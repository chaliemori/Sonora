package com.example.musicplayer.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicplayer.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var fadeOutPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var crossfadeMonitorJob: Job? = null
    private var activeCrossfadeJob: Job? = null

    @Volatile
    private var crossfadeSec: Int = 0
    private var prefs: SharedPreferences? = null
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        if (key == "crossfade_sec") {
            crossfadeSec = sp.getInt("crossfade_sec", 0).coerceIn(0, 12)
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val sp = getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
        prefs = sp
        crossfadeSec = sp.getInt("crossfade_sec", 0).coerceIn(0, 12)
        sp.registerOnSharedPreferenceChangeListener(prefsListener)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DsdAwareDataSourceFactory(this)))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exoPlayer
        if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && exoPlayer.audioSessionId != 0) {
            AudioEffectsController.attachToSession(applicationContext, exoPlayer.audioSessionId)
        }
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    AudioEffectsController.attachToSession(applicationContext, audioSessionId)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && activeCrossfadeJob?.isActive == true) {
                    cancelActiveCrossfade(exoPlayer)
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK && activeCrossfadeJob?.isActive == true) {
                    // Keep crossfade running only when initiated by our crossfade seek
                    if (!isInternalCrossfadeSeek) {
                        cancelActiveCrossfade(exoPlayer)
                    }
                }
            }
        })

        startCrossfadeMonitor(exoPlayer)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val wrappedPlayer = object : ForwardingPlayer(exoPlayer) {
            override fun isCommandAvailable(command: Int): Boolean {
                if (command == COMMAND_SEEK_TO_NEXT || command == COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
                    return mediaItemCount > 0
                }
                return super.isCommandAvailable(command)
            }

            override fun getAvailableCommands(): Player.Commands {
                val base = super.getAvailableCommands()
                return if (mediaItemCount > 0) {
                    base.buildUpon()
                        .add(COMMAND_SEEK_TO_NEXT)
                        .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .build()
                } else {
                    base
                }
            }

            override fun hasNextMediaItem(): Boolean = mediaItemCount > 0

            override fun seekToNext() {
                cancelActiveCrossfade(exoPlayer)
                if (super.hasNextMediaItem()) {
                    super.seekToNext()
                } else if (mediaItemCount > 0) {
                    val timeline = currentTimeline
                    val firstIndex = if (!timeline.isEmpty) {
                        timeline.getFirstWindowIndex(shuffleModeEnabled)
                    } else {
                        0
                    }
                    seekTo(firstIndex, 0L)
                    if (playbackState == STATE_IDLE || playbackState == STATE_ENDED) {
                        prepare()
                    }
                    play()
                }
            }

            override fun seekToNextMediaItem() {
                seekToNext()
            }
        }

        mediaSession = MediaSession.Builder(this, wrappedPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    @Volatile
    private var isInternalCrossfadeSeek: Boolean = false

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun ensureFadeOutPlayer(): ExoPlayer {
        fadeOutPlayer?.let { return it }
        return ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DsdAwareDataSourceFactory(this)))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false
            )
            .build()
            .also { fadeOutPlayer = it }
    }

    private fun startCrossfadeMonitor(mainPlayer: ExoPlayer) {
        crossfadeMonitorJob?.cancel()
        crossfadeMonitorJob = serviceScope.launch {
            while (isActive) {
                delay(120L)
                val sec = crossfadeSec
                if (sec <= 0 || activeCrossfadeJob?.isActive == true) continue
                if (!mainPlayer.isPlaying || mainPlayer.mediaItemCount <= 1) continue
                if (mainPlayer.repeatMode == Player.REPEAT_MODE_ONE) continue

                val dur = mainPlayer.duration
                val pos = mainPlayer.currentPosition
                val fadeMs = sec * 1000L
                if (dur <= fadeMs * 2L + 2000L || pos < 0L) continue

                val remaining = dur - pos
                if (remaining in 250L..fadeMs) {
                    val timeline = mainPlayer.currentTimeline
                    if (timeline.isEmpty) continue
                    val nextIdx = timeline.getNextWindowIndex(
                        mainPlayer.currentMediaItemIndex,
                        mainPlayer.repeatMode,
                        mainPlayer.shuffleModeEnabled
                    )
                    if (nextIdx == C.INDEX_UNSET || nextIdx == mainPlayer.currentMediaItemIndex) continue

                    val outgoingItem = mainPlayer.currentMediaItem ?: continue
                    val outHandoffPos = (pos + 80L).coerceAtMost(dur - 50L)
                    triggerCrossfade(mainPlayer, outgoingItem, outHandoffPos, nextIdx, remaining.coerceIn(800L, fadeMs))
                }
            }
        }
    }

    private fun triggerCrossfade(
        mainPlayer: ExoPlayer,
        outgoingItem: androidx.media3.common.MediaItem,
        outgoingStartMs: Long,
        nextIndex: Int,
        fadeDurationMs: Long
    ) {
        activeCrossfadeJob?.cancel()
        activeCrossfadeJob = serviceScope.launch {
            val deckOut = ensureFadeOutPlayer()
            try {
                deckOut.volume = mainPlayer.volume
                deckOut.setMediaItem(outgoingItem, outgoingStartMs)
                deckOut.prepare()
                deckOut.play()

                var waited = 0L
                while (waited < 320L && deckOut.playbackState == Player.STATE_BUFFERING) {
                    delay(20L)
                    waited += 20L
                }
                if (!mainPlayer.isPlaying) {
                    deckOut.pause()
                    deckOut.clearMediaItems()
                    return@launch
                }

                isInternalCrossfadeSeek = true
                mainPlayer.volume = 0f
                mainPlayer.seekTo(nextIndex, 0L)
                if (mainPlayer.playbackState == Player.STATE_IDLE || mainPlayer.playbackState == Player.STATE_ENDED) {
                    mainPlayer.prepare()
                }
                mainPlayer.play()
                isInternalCrossfadeSeek = false

                val startTime = System.currentTimeMillis()
                while (isActive) {
                    if (!mainPlayer.isPlaying || mainPlayer.currentMediaItemIndex != nextIndex) break
                    val elapsed = System.currentTimeMillis() - startTime
                    val t = (elapsed.toFloat() / fadeDurationMs.toFloat()).coerceIn(0f, 1f)
                    val angle = t * (Math.PI / 2.0)
                    deckOut.volume = cos(angle).toFloat().coerceIn(0f, 1f)
                    mainPlayer.volume = sin(angle).toFloat().coerceIn(0f, 1f)
                    if (t >= 1f) break
                    delay(32L)
                }
            } finally {
                isInternalCrossfadeSeek = false
                mainPlayer.volume = 1f
                try {
                    deckOut.pause()
                    deckOut.clearMediaItems()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun cancelActiveCrossfade(mainPlayer: ExoPlayer) {
        activeCrossfadeJob?.cancel()
        activeCrossfadeJob = null
        mainPlayer.volume = 1f
        try {
            fadeOutPlayer?.pause()
            fadeOutPlayer?.clearMediaItems()
        } catch (_: Exception) {
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        prefs?.unregisterOnSharedPreferenceChangeListener(prefsListener)
        crossfadeMonitorJob?.cancel()
        activeCrossfadeJob?.cancel()
        serviceScope.cancel()
        fadeOutPlayer?.run {
            release()
            fadeOutPlayer = null
        }
        AudioEffectsController.release()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        player?.run {
            release()
            player = null
        }
        super.onDestroy()
    }
}
