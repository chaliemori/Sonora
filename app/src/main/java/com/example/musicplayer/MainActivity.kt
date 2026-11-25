package com.example.musicplayer

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.Coil
import coil.ImageLoader
import com.example.musicplayer.audio.PlaybackService
import com.example.musicplayer.data.LocalMusicScanner
import com.example.musicplayer.data.LocalTrack
import com.example.musicplayer.data.UserPlaylist
import com.example.musicplayer.ui.components.AudioCoverFetcher
import com.example.musicplayer.ui.screens.LibraryScreen
import com.example.musicplayer.ui.screens.NowPlayingScreen
import com.example.musicplayer.ui.theme.LyricsStyle
import com.example.musicplayer.ui.theme.MonetMode
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import com.example.musicplayer.ui.theme.NowPlayingStyle
import com.example.musicplayer.ui.theme.PaletteStyle
import com.example.musicplayer.ui.theme.extractDominantColorFromBitmap
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var externalOpenUri by mutableStateOf<Uri?>(null)

    private fun extractExternalAudioUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        return intent.data ?: intent.clipData?.getItemAt(0)?.uri
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractExternalAudioUri(intent)?.let { uri ->
            externalOpenUri = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.musicplayer.audio.ParametricEqController.ensureLoaded(applicationContext)
        com.example.musicplayer.data.LibraryFilterPrefs.ensureLoaded(applicationContext)
        externalOpenUri = extractExternalAudioUri(intent)

        val customImageLoader = ImageLoader.Builder(applicationContext)
            .components {
                add(AudioCoverFetcher.Factory(applicationContext))
            }
            .crossfade(false)
            .build()
        Coil.setImageLoader(customImageLoader)

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val systemDark = isSystemInDarkTheme()
            val prefs = remember {
                context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            }

            var darkTheme by remember {
                mutableStateOf(
                    if (prefs.contains("dark_theme")) {
                        prefs.getBoolean("dark_theme", systemDark)
                    } else {
                        systemDark
                    }
                )
            }
            var amoledTheme by remember {
                mutableStateOf(prefs.getBoolean("amoled_theme", false))
            }
            var landscapeNavOnRight by remember {
                mutableStateOf(prefs.getBoolean("landscape_nav_right", true))
            }
            var monetMode by remember {
                mutableStateOf(
                    prefs.getString("monet_mode", null)?.let { saved ->
                        MonetMode.entries.firstOrNull { it.name == saved }
                    } ?: MonetMode.COVER_AUTO
                )
            }
            var paletteStyle by remember {
                mutableStateOf(
                    prefs.getString("palette_style", null)?.let { saved ->
                        PaletteStyle.entries.firstOrNull { it.name == saved }
                    } ?: PaletteStyle.TONAL_SPOT
                )
            }
            var customSeedColor by remember {
                mutableStateOf(
                    Color(prefs.getInt("custom_seed_argb", Color(0xFF6750A4).toArgb()))
                )
            }

            val initialCachedTracks = remember { loadCachedTracksFromDisk(context) }
            var tracks by remember { mutableStateOf(initialCachedTracks) }
            var activeQueue by remember { mutableStateOf(initialCachedTracks) }
            var isScanning by remember { mutableStateOf(false) }
            var scannedFilesCount by remember { mutableIntStateOf(initialCachedTracks.size) }
            var scanStatusText by remember {
                mutableStateOf(
                    if (initialCachedTracks.isNotEmpty()) {
                        "Загружено из кэша (${initialCachedTracks.size} треков)"
                    } else {
                        "Ожидание запуска сканирования"
                    }
                )
            }
            var scanJob by remember { mutableStateOf<Job?>(null) }
            var currentTrack by remember { mutableStateOf<LocalTrack?>(null) }
            var isNowPlayingOpen by remember { mutableStateOf(false) }

            var selectedFolderUris by remember {
                mutableStateOf(
                    prefs.getStringSet("folder_uris", emptySet())
                        ?.map { Uri.parse(it) }
                        ?.sortedBy { it.toString() }
                        ?: emptyList()
                )
            }

            var favoriteIds by remember {
                mutableStateOf(
                    prefs.getStringSet("favorite_ids", emptySet())
                        ?.mapNotNull { it.toLongOrNull() }
                        ?.toSet()
                        ?: emptySet()
                )
            }

            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                    if (key == "favorite_ids") {
                        favoriteIds = sp.getStringSet("favorite_ids", emptySet())
                            ?.mapNotNull { it.toLongOrNull() }
                            ?.toSet()
                            ?: emptySet()
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            var playlists by remember {
                mutableStateOf(loadPlaylistsFromPrefs(prefs.getString("playlists_json", null)))
            }

            var showCreatePlaylistDialog by remember { mutableStateOf(false) }
            var newPlaylistName by remember { mutableStateOf("") }
            var renamingPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
            var renameText by remember { mutableStateOf("") }
            var showAddToPlaylistDialog by remember { mutableStateOf(false) }
            var playlistTargetTrack by remember { mutableStateOf<LocalTrack?>(null) }
            var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }

            var isPlaying by remember { mutableStateOf(false) }
            var positionMs by remember { mutableLongStateOf(0L) }
            var durationMs by remember { mutableLongStateOf(0L) }
            var shuffleEnabled by remember { mutableStateOf(prefs.getBoolean("shuffle_enabled", false)) }
            var repeatMode by remember {
                mutableIntStateOf(
                    prefs.getInt("repeat_mode_int", Player.REPEAT_MODE_OFF)
                )
            }
            var controller by remember { mutableStateOf<MediaController?>(null) }
            var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }

            var hideDuplicates by remember {
                mutableStateOf(prefs.getBoolean("hide_duplicates", true))
            }
            var ignoreJunkFolders by remember {
                mutableStateOf(prefs.getBoolean("ignore_junk_folders", true))
            }
            var autoRefreshLibrary by remember {
                mutableStateOf(prefs.getBoolean("auto_refresh_library", true))
            }
            var minDurationSec by remember {
                mutableIntStateOf(prefs.getInt("min_duration_sec", 15))
            }
            var playerStyle by remember {
                mutableStateOf(
                    prefs.getString("player_style", null)?.let { saved ->
                        NowPlayingStyle.entries.firstOrNull { it.name == saved }
                    } ?: NowPlayingStyle.CARD
                )
            }
            var lyricsStyle by remember {
                mutableStateOf(
                    prefs.getString("lyrics_style", null)?.let { saved ->
                        LyricsStyle.entries.firstOrNull { it.name == saved }
                    } ?: LyricsStyle.APPLE_MUSIC
                )
            }
            var crossfadeSec by remember {
                mutableIntStateOf(prefs.getInt("crossfade_sec", 0).coerceIn(0, 12))
            }
            var lastScanFinishedAtMs by remember { mutableLongStateOf(0L) }
            var mediaStoreDirty by remember { mutableStateOf(false) }
            var pendingDeleteTrack by remember { mutableStateOf<LocalTrack?>(null) }
            val lifecycleOwner = LocalLifecycleOwner.current

            fun savePlayerStylePref(style: NowPlayingStyle) {
                playerStyle = style
                prefs.edit().putString("player_style", style.name).commit()
            }

            fun saveLyricsStylePref(style: LyricsStyle) {
                lyricsStyle = style
                prefs.edit().putString("lyrics_style", style.name).commit()
            }

            fun saveCrossfadePref(sec: Int) {
                crossfadeSec = sec
                prefs.edit().putInt("crossfade_sec", sec).commit()
            }

            fun saveThemePref(isDark: Boolean) {
                darkTheme = isDark
                prefs.edit().putBoolean("dark_theme", isDark).commit()
            }

            fun saveAmoledPref(isAmoled: Boolean) {
                amoledTheme = isAmoled
                prefs.edit().putBoolean("amoled_theme", isAmoled).commit()
            }

            fun saveLandscapeNavRightPref(onRight: Boolean) {
                landscapeNavOnRight = onRight
                prefs.edit().putBoolean("landscape_nav_right", onRight).commit()
            }

            fun saveMonetPref(mode: MonetMode) {
                monetMode = mode
                prefs.edit().putString("monet_mode", mode.name).commit()
            }

            fun savePaletteStylePref(style: PaletteStyle) {
                paletteStyle = style
                prefs.edit().putString("palette_style", style.name).commit()
            }

            fun saveCustomColorPref(color: Color) {
                customSeedColor = color
                prefs.edit().putInt("custom_seed_argb", color.toArgb()).commit()
            }

            fun saveFolders(updated: List<Uri>) {
                val distinct = updated.distinctBy { it.toString() }
                selectedFolderUris = distinct
                prefs.edit().putStringSet("folder_uris", distinct.map { it.toString() }.toSet()).commit()
            }

            fun saveFavorites(updated: Set<Long>) {
                favoriteIds = updated
                prefs.edit().putStringSet("favorite_ids", updated.map { it.toString() }.toSet()).commit()
            }

            fun savePlaylists(updated: List<UserPlaylist>) {
                playlists = updated
                prefs.edit().putString("playlists_json", encodePlaylistsToJson(updated)).commit()
            }

            val scanner = remember { LocalMusicScanner(context) }

            fun savePlaybackState(
                track: LocalTrack? = currentTrack,
                posMs: Long = controller?.currentPosition?.coerceAtLeast(0L) ?: positionMs,
                queue: List<LocalTrack> = activeQueue
            ) {
                val tId = track?.id ?: -1L
                val qIds = queue.take(500).joinToString(",") { it.id.toString() }
                prefs.edit()
                    .putLong("last_track_id", tId)
                    .putLong("last_position_ms", posMs.coerceAtLeast(0L))
                    .putString("last_queue_ids", qIds)
                    .apply()
            }

            fun triggerScan(
                folders: List<Uri> = selectedFolderUris,
                forceClearCovers: Boolean = false,
                durSec: Int = minDurationSec,
                dedup: Boolean = hideDuplicates,
                ignoreJunk: Boolean = ignoreJunkFolders
            ) {
                scanJob?.cancel()
                scanJob = scope.launch {
                    isScanning = true
                    scannedFilesCount = 0
                    scanStatusText = "Запуск сканирования..."
                    try {
                        val result = withTimeoutOrNull(35_000L) {
                            if (forceClearCovers) {
                                scanner.clearCoverCache()
                            }
                            scanner.scanTracks(
                                folderUris = folders,
                                minDurationMs = durSec * 1000L,
                                hideDuplicates = dedup,
                                ignoreJunkFolders = ignoreJunk
                            ) { count, msg ->
                                scannedFilesCount = count
                                scanStatusText = msg
                            }
                        }
                        if (result != null) {
                            tracks = result.tracks
                            if (activeQueue.isEmpty() && (controller?.mediaItemCount ?: 0) == 0) {
                                activeQueue = result.tracks
                            }
                            scannedFilesCount = result.scannedFilesCount
                            scanStatusText = result.statusText
                            val activeId = currentTrack?.id
                            if (activeId != null) {
                                tracks.firstOrNull { it.id == activeId }?.let { refreshed ->
                                    currentTrack = refreshed
                                }
                            }
                            withContext(Dispatchers.IO) {
                                saveCachedTracksToDisk(context, result.tracks)
                            }
                        } else {
                            scanStatusText = "Таймаут сканирования (проверено $scannedFilesCount файлов)"
                        }
                    } catch (e: Exception) {
                        scanStatusText = "Сбой: ${e.message ?: "ошибка"}"
                    } finally {
                        isScanning = false
                        mediaStoreDirty = false
                        lastScanFinishedAtMs = System.currentTimeMillis()
                    }
                }
            }

            fun saveHideDuplicatesPref(enabled: Boolean) {
                hideDuplicates = enabled
                prefs.edit().putBoolean("hide_duplicates", enabled).commit()
                triggerScan(dedup = enabled)
            }

            fun saveIgnoreJunkFoldersPref(enabled: Boolean) {
                ignoreJunkFolders = enabled
                prefs.edit().putBoolean("ignore_junk_folders", enabled).commit()
                triggerScan(ignoreJunk = enabled)
            }

            fun saveAutoRefreshLibraryPref(enabled: Boolean) {
                autoRefreshLibrary = enabled
                prefs.edit().putBoolean("auto_refresh_library", enabled).commit()
            }

            fun saveMinDurationSecPref(sec: Int) {
                minDurationSec = sec
                prefs.edit().putInt("min_duration_sec", sec).commit()
                triggerScan(durSec = sec)
            }

            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { treeUri ->
                if (treeUri != null) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {
                    }
                    val updated = (selectedFolderUris + treeUri).distinctBy { it.toString() }
                    saveFolders(updated)
                    triggerScan(folders = updated, forceClearCovers = false)
                }
            }

            fun buildMediaItem(track: LocalTrack): MediaItem {
                return MediaItem.Builder()
                    .setMediaId(track.id.toString())
                    .setUri(track.contentUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .setArtworkUri(track.artworkUri)
                            .build()
                    )
                    .build()
            }

            fun cleanupDeletedTrack(target: LocalTrack) {
                val updatedTracks = tracks.filterNot { it.id == target.id && it.contentUri == target.contentUri }
                tracks = updatedTracks
                if (target.id in favoriteIds) {
                    saveFavorites(favoriteIds - target.id)
                }
                if (playlists.any { target.id in it.trackIds }) {
                    savePlaylists(playlists.map { it.copy(trackIds = it.trackIds - target.id) })
                }
                val mc = controller
                val removeIndices = activeQueue.mapIndexedNotNull { idx, item ->
                    if (item.id == target.id && item.contentUri == target.contentUri) idx else null
                }.sortedDescending()
                removeIndices.forEach { idx ->
                    if (mc != null && idx in 0 until mc.mediaItemCount) {
                        mc.removeMediaItem(idx)
                    }
                }
                val newQueue = activeQueue.filterNot { it.id == target.id && it.contentUri == target.contentUri }
                activeQueue = newQueue
                if (currentTrack?.id == target.id && currentTrack?.contentUri == target.contentUri) {
                    val activeId = mc?.currentMediaItem?.mediaId?.toLongOrNull()
                    currentTrack = activeId?.let { id -> newQueue.firstOrNull { it.id == id } }
                        ?: newQueue.firstOrNull()
                    if (currentTrack == null) {
                        isPlaying = false
                        positionMs = 0L
                        durationMs = 0L
                    }
                }
                scope.launch(Dispatchers.IO) {
                    saveCachedTracksToDisk(context, updatedTracks)
                }
                Toast.makeText(context, "Файл удалён", Toast.LENGTH_SHORT).show()
            }

            val deleteLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                val target = pendingDeleteTrack
                pendingDeleteTrack = null
                if (result.resultCode == Activity.RESULT_OK && target != null) {
                    cleanupDeletedTrack(target)
                }
            }

            val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted && tracks.isEmpty()) {
                    triggerScan(folders = selectedFolderUris, forceClearCovers = false)
                }
            }

            LaunchedEffect(Unit) {
                val hasPerm = ContextCompat.checkSelfPermission(context, audioPermission) ==
                    PackageManager.PERMISSION_GRANTED
                if (!hasPerm && selectedFolderUris.isEmpty()) {
                    permissionLauncher.launch(audioPermission)
                } else if (tracks.isEmpty()) {
                    triggerScan(folders = selectedFolderUris, forceClearCovers = false)
                }
            }

            DisposableEffect(autoRefreshLibrary) {
                if (!autoRefreshLibrary) {
                    onDispose { }
                } else {
                    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            mediaStoreDirty = true
                        }
                    }
                    try {
                        context.contentResolver.registerContentObserver(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            true,
                            observer
                        )
                    } catch (_: Exception) {
                    }
                    onDispose {
                        try {
                            context.contentResolver.unregisterContentObserver(observer)
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            LaunchedEffect(mediaStoreDirty, autoRefreshLibrary, isScanning) {
                if (mediaStoreDirty && autoRefreshLibrary && !isScanning) {
                    delay(1800L)
                    val hasPerm = ContextCompat.checkSelfPermission(context, audioPermission) ==
                        PackageManager.PERMISSION_GRANTED
                    if (mediaStoreDirty && autoRefreshLibrary && !isScanning && (hasPerm || selectedFolderUris.isNotEmpty())) {
                        triggerScan(folders = selectedFolderUris, forceClearCovers = false)
                    }
                }
            }

            DisposableEffect(lifecycleOwner, autoRefreshLibrary) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            savePlaybackState()
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            val hasPerm = ContextCompat.checkSelfPermission(context, audioPermission) ==
                                PackageManager.PERMISSION_GRANTED
                            val stale = System.currentTimeMillis() - lastScanFinishedAtMs > 12_000L
                            if (autoRefreshLibrary && !isScanning && (hasPerm || selectedFolderUris.isNotEmpty()) && (mediaStoreDirty || stale)) {
                                triggerScan(folders = selectedFolderUris, forceClearCovers = false)
                            }
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, PlaybackService::class.java)
                )
                val future = MediaController.Builder(context, sessionToken).buildAsync()
                future.addListener({
                    val mc = future.get()
                    controller = mc

                    mc.shuffleModeEnabled = shuffleEnabled
                    mc.repeatMode = repeatMode

                    val syncPlayerState = {
                        isPlaying = mc.isPlaying
                        durationMs = mc.duration.coerceAtLeast(0L)
                        positionMs = mc.currentPosition.coerceAtLeast(0L)

                        val activeId = mc.currentMediaItem?.mediaId?.toLongOrNull()
                        if (activeId != null) {
                            currentTrack = tracks.firstOrNull { it.id == activeId }
                                ?: activeQueue.firstOrNull { it.id == activeId }
                                ?: currentTrack
                        }
                    }

                    mc.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                            if (playing && mc.duration > 0L) {
                                durationMs = mc.duration
                            }
                            positionMs = mc.currentPosition.coerceAtLeast(0L)
                            if (!playing) {
                                savePlaybackState()
                            }
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            val activeId = mediaItem?.mediaId?.toLongOrNull()
                            if (activeId != null) {
                                currentTrack = tracks.firstOrNull { it.id == activeId }
                                    ?: activeQueue.firstOrNull { it.id == activeId }
                                    ?: currentTrack
                            }
                            durationMs = mc.duration.coerceAtLeast(0L)
                            positionMs = mc.currentPosition.coerceAtLeast(0L)
                            savePlaybackState()
                        }

                        override fun onTracksChanged(playerTracks: Tracks) {
                            val active = currentTrack ?: return
                            if (active.formatLabel == "DSF" || active.formatLabel == "DFF") return
                            for (group in playerTracks.groups) {
                                if (group.isSelected && group.length > 0) {
                                    val fmt = group.getTrackFormat(0)
                                    val liveSr = fmt.sampleRate
                                    val liveBr = if (fmt.bitrate > 0) fmt.bitrate / 1000 else 0
                                    if (liveSr in 8000..384000 || liveBr in 32..6000) {
                                        currentTrack = active.copy(
                                            sampleRateHz = if (liveSr in 8000..384000) liveSr else active.sampleRateHz,
                                            bitrateKbps = if (liveBr in 32..6000) liveBr else active.bitrateKbps
                                        )
                                    }
                                    break
                                }
                            }
                        }

                        override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                            shuffleEnabled = enabled
                            prefs.edit().putBoolean("shuffle_enabled", enabled).commit()
                        }

                        override fun onRepeatModeChanged(newMode: Int) {
                            repeatMode = newMode
                            prefs.edit().putInt("repeat_mode_int", newMode).commit()
                        }
                    })

                    if (mc.mediaItemCount == 0 && tracks.isNotEmpty()) {
                        val savedTrackId = prefs.getLong("last_track_id", -1L)
                        if (savedTrackId != -1L) {
                            val savedPos = prefs.getLong("last_position_ms", 0L).coerceAtLeast(0L)
                            val savedQueueIds = prefs.getString("last_queue_ids", null)
                                ?.split(",")
                                ?.mapNotNull { it.toLongOrNull() }
                                .orEmpty()
                            val trackMap = tracks.associateBy { it.id }
                            val restoredQueue = savedQueueIds.mapNotNull { trackMap[it] }
                                .ifEmpty { tracks }
                            val restoredIndex = restoredQueue.indexOfFirst { it.id == savedTrackId }
                            if (restoredIndex >= 0) {
                                val restoredTrack = restoredQueue[restoredIndex]
                                activeQueue = restoredQueue
                                currentTrack = restoredTrack
                                durationMs = restoredTrack.durationMs
                                positionMs = savedPos.coerceIn(0L, restoredTrack.durationMs.coerceAtLeast(1L))
                                mc.setMediaItems(
                                    restoredQueue.map(::buildMediaItem),
                                    restoredIndex,
                                    positionMs
                                )
                                mc.prepare()
                            }
                        }
                    } else {
                        syncPlayerState()
                    }
                }, MoreExecutors.directExecutor())
            }

            LaunchedEffect(isPlaying, isNowPlayingOpen, controller) {
                while (isPlaying) {
                    val mc = controller
                    if (mc != null) {
                        positionMs = mc.currentPosition.coerceAtLeast(0L)
                        if (mc.duration > 0L) {
                            durationMs = mc.duration
                        }
                    }
                    delay(if (isNowPlayingOpen) 90L else 500L)
                }
            }

            LaunchedEffect(sleepTimerMinutes) {
                val mins = sleepTimerMinutes ?: return@LaunchedEffect
                delay(mins * 60_000L)
                controller?.pause()
                sleepTimerMinutes = null
            }

            LaunchedEffect(currentTrack?.id, currentTrack?.contentUri) {
                val track = currentTrack ?: run {
                    coverBitmap = null
                    return@LaunchedEffect
                }
                val artworkInfo = withContext(Dispatchers.IO) {
                    extractArtworkBytesAndBitmap(context, track)
                }
                coverBitmap = artworkInfo?.second

                val mc = controller
                val currentItem = mc?.currentMediaItem
                if (mc != null && currentItem != null && currentItem.mediaId == track.id.toString()) {
                    val bytes = artworkInfo?.first
                    val cachedUri = artworkInfo?.third
                    val updatedMetadata = currentItem.mediaMetadata.buildUpon()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .apply {
                            if (cachedUri != null) {
                                setArtworkUri(cachedUri)
                            }
                            if (bytes != null) {
                                setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            }
                        }
                        .build()

                    val index = mc.currentMediaItemIndex
                    if (index in 0 until mc.mediaItemCount) {
                        val updatedItem = currentItem.buildUpon()
                            .setMediaMetadata(updatedMetadata)
                            .build()
                        mc.replaceMediaItem(index, updatedItem)
                    }
                }
            }

            val coverAccent = extractDominantColorFromBitmap(
                bitmap = coverBitmap,
                fallback = Color(0xFF6750A4)
            )

            fun playQueue(queue: List<LocalTrack>, startIndex: Int, shuffle: Boolean) {
                val mc = controller ?: return
                if (queue.isEmpty()) return

                activeQueue = queue
                val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
                val items = queue.map(::buildMediaItem)

                shuffleEnabled = shuffle
                prefs.edit().putBoolean("shuffle_enabled", shuffle).commit()

                mc.setMediaItems(items, safeIndex, 0L)
                mc.shuffleModeEnabled = shuffle
                mc.repeatMode = repeatMode
                mc.prepare()
                mc.play()

                currentTrack = queue[safeIndex]
                isPlaying = true
                durationMs = queue[safeIndex].durationMs
                positionMs = 0L
                savePlaybackState(track = queue[safeIndex], posMs = 0L, queue = queue)
            }

            fun playNext(track: LocalTrack) {
                val mc = controller
                if (mc == null || mc.mediaItemCount == 0 || activeQueue.isEmpty()) {
                    playQueue(listOf(track), 0, false)
                    isNowPlayingOpen = true
                    return
                }
                val insertIdx = (mc.currentMediaItemIndex + 1).coerceIn(0, mc.mediaItemCount)
                mc.addMediaItem(insertIdx, buildMediaItem(track))
                val updated = activeQueue.toMutableList()
                updated.add(insertIdx.coerceIn(0, updated.size), track)
                activeQueue = updated
                savePlaybackState(queue = updated)
                Toast.makeText(context, "Сыграет следующим: ${track.title}", Toast.LENGTH_SHORT).show()
            }

            fun addToQueue(track: LocalTrack) {
                val mc = controller
                if (mc == null || mc.mediaItemCount == 0 || activeQueue.isEmpty()) {
                    playQueue(listOf(track), 0, false)
                    isNowPlayingOpen = true
                    return
                }
                mc.addMediaItem(buildMediaItem(track))
                val updated = activeQueue + track
                activeQueue = updated
                savePlaybackState(queue = updated)
                Toast.makeText(context, "Добавлено в очередь: ${track.title}", Toast.LENGTH_SHORT).show()
            }

            fun removeFromQueue(index: Int) {
                val mc = controller ?: return
                if (index !in activeQueue.indices) return
                if (index in 0 until mc.mediaItemCount) {
                    mc.removeMediaItem(index)
                }
                val updated = activeQueue.toMutableList().also { it.removeAt(index) }
                activeQueue = updated
                if (updated.isEmpty() || mc.mediaItemCount == 0) {
                    currentTrack = null
                    isPlaying = false
                    positionMs = 0L
                    durationMs = 0L
                } else {
                    val activeId = mc.currentMediaItem?.mediaId?.toLongOrNull()
                    if (activeId != null) {
                        currentTrack = tracks.firstOrNull { it.id == activeId }
                            ?: updated.firstOrNull { it.id == activeId }
                            ?: currentTrack
                    }
                }
                savePlaybackState(queue = updated)
            }

            fun removeTrackFromPlaylist(track: LocalTrack, playlistId: String) {
                val updatedPlaylists = playlists.map { pl ->
                    if (pl.id == playlistId) pl.copy(trackIds = pl.trackIds - track.id) else pl
                }
                savePlaylists(updatedPlaylists)
                Toast.makeText(context, "Удалено из плейлиста", Toast.LENGTH_SHORT).show()
            }

            fun deleteTrackFromDevice(track: LocalTrack) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(track.contentUri))
                        pendingDeleteTrack = track
                        deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                        return
                    } catch (_: Exception) {
                        pendingDeleteTrack = null
                    }
                }
                val deletedDirect = try {
                    context.contentResolver.delete(track.contentUri, null, null) > 0
                } catch (_: Exception) {
                    try {
                        DocumentFile.fromSingleUri(context, track.contentUri)?.delete() == true
                    } catch (_: Exception) {
                        false
                    }
                }
                if (deletedDirect) {
                    cleanupDeletedTrack(track)
                } else {
                    Toast.makeText(context, "Не удалось удалить файл со смартфона", Toast.LENGTH_SHORT).show()
                }
            }

            LaunchedEffect(externalOpenUri, controller) {
                val extUri = externalOpenUri ?: return@LaunchedEffect
                if (controller == null) return@LaunchedEffect
                externalOpenUri = null
                val existingIdx = tracks.indexOfFirst { it.contentUri == extUri }
                if (existingIdx >= 0) {
                    playQueue(tracks, existingIdx, shuffle = false)
                    isNowPlayingOpen = true
                    return@LaunchedEffect
                }
                val resolvedTrack = withContext(Dispatchers.IO) {
                    resolveExternalTrack(context, extUri)
                }
                if (resolvedTrack != null) {
                    playQueue(listOf(resolvedTrack), 0, shuffle = false)
                    isNowPlayingOpen = true
                } else {
                    Toast.makeText(context, "Не удалось открыть аудиофайл", Toast.LENGTH_SHORT).show()
                }
            }

            fun cycleMonet() {
                val values = MonetMode.entries
                saveMonetPref(values[(monetMode.ordinal + 1) % values.size])
            }

            fun cycleSleepTimer() {
                sleepTimerMinutes = when (sleepTimerMinutes) {
                    null -> 15
                    15 -> 30
                    30 -> 60
                    else -> null
                }
            }

            fun skipPrevious() {
                controller?.seekToPrevious()
            }

            fun skipNext() {
                val mc = controller ?: return
                if (mc.mediaItemCount > 0) {
                    if (mc.hasNextMediaItem()) {
                        mc.seekToNext()
                    } else {
                        val timeline = mc.currentTimeline
                        val firstIndex = if (!timeline.isEmpty) {
                            timeline.getFirstWindowIndex(mc.shuffleModeEnabled)
                        } else {
                            0
                        }
                        mc.seekTo(firstIndex, 0L)
                        if (mc.playbackState == Player.STATE_IDLE || mc.playbackState == Player.STATE_ENDED) {
                            mc.prepare()
                        }
                        mc.play()
                        activeQueue.getOrNull(firstIndex)?.let { selected ->
                            currentTrack = selected
                            durationMs = selected.durationMs
                            positionMs = 0L
                        }
                    }
                }
            }

            fun toggleTrackFavorite(target: LocalTrack) {
                val updated = if (target.id in favoriteIds) {
                    favoriteIds - target.id
                } else {
                    favoriteIds + target.id
                }
                saveFavorites(updated)
            }

            BackHandler(enabled = isNowPlayingOpen) {
                isNowPlayingOpen = false
            }

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                }
            }

            MusicPlayerTheme(
                darkTheme = darkTheme,
                amoledTheme = amoledTheme,
                monetMode = monetMode,
                paletteStyle = paletteStyle,
                coverAccent = coverAccent,
                customSeedColor = customSeedColor
            ) {
                val activeTrack = currentTrack
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            visible = isNowPlayingOpen && activeTrack != null,
                            modifier = Modifier.zIndex(1f),
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(
                                    dampingRatio = 0.88f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(animationSpec = tween(durationMillis = 220)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = spring(
                                    dampingRatio = 0.92f,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(animationSpec = tween(durationMillis = 180))
                        ) {
                            if (activeTrack != null) {
                                NowPlayingScreen(
                                    track = activeTrack,
                                    queue = activeQueue,
                                    isPlaying = isPlaying,
                                    isFavorite = activeTrack.id in favoriteIds,
                                    sleepTimerMinutes = sleepTimerMinutes,
                                    positionMs = positionMs,
                                    durationMs = if (durationMs > 0L) durationMs else activeTrack.durationMs,
                                    shuffleEnabled = shuffleEnabled,
                                    repeatMode = repeatMode,
                                    monetMode = monetMode,
                                    playerStyle = playerStyle,
                                    lyricsStyle = lyricsStyle,
                                    onClose = { isNowPlayingOpen = false },
                                    onTogglePlayPause = {
                                        controller?.let { mc ->
                                            if (mc.isPlaying) mc.pause() else mc.play()
                                        }
                                    },
                                    onToggleFavorite = { toggleTrackFavorite(activeTrack) },
                                    onAddToPlaylist = {
                                        playlistTargetTrack = activeTrack
                                        if (playlists.isEmpty()) {
                                            showCreatePlaylistDialog = true
                                        } else {
                                            showAddToPlaylistDialog = true
                                        }
                                    },
                                    onCycleSleepTimer = ::cycleSleepTimer,
                                    onSelectQueueIndex = { index ->
                                        val mc = controller ?: return@NowPlayingScreen
                                        if (index in 0 until mc.mediaItemCount) {
                                            mc.seekToDefaultPosition(index)
                                            mc.play()
                                            activeQueue.getOrNull(index)?.let { selected ->
                                                currentTrack = selected
                                                durationMs = selected.durationMs
                                                positionMs = 0L
                                            }
                                        }
                                    },
                                    onRemoveQueueIndex = ::removeFromQueue,
                                    onPrevious = ::skipPrevious,
                                    onNext = ::skipNext,
                                    onSeekBy = { deltaMs ->
                                        val mc = controller ?: return@NowPlayingScreen
                                        val total = if (mc.duration > 0L) mc.duration else activeTrack.durationMs
                                        val targetPos = (mc.currentPosition + deltaMs).coerceIn(0L, total.coerceAtLeast(0L))
                                        mc.seekTo(targetPos)
                                        positionMs = targetPos
                                    },
                                    onSeekToMs = { targetMs ->
                                        val mc = controller ?: return@NowPlayingScreen
                                        val total = if (mc.duration > 0L) mc.duration else activeTrack.durationMs
                                        val clamped = targetMs.coerceIn(0L, total.coerceAtLeast(0L))
                                        mc.seekTo(clamped)
                                        positionMs = clamped
                                    },
                                    onSeekToFraction = { fraction ->
                                        val mc = controller ?: return@NowPlayingScreen
                                        val targetDuration = if (mc.duration > 0L) mc.duration else activeTrack.durationMs
                                        val targetPos = (targetDuration * fraction).toLong()
                                        mc.seekTo(targetPos)
                                        positionMs = targetPos
                                    },
                                    onToggleShuffle = {
                                        val nextState = !shuffleEnabled
                                        shuffleEnabled = nextState
                                        prefs.edit().putBoolean("shuffle_enabled", nextState).commit()
                                        controller?.shuffleModeEnabled = nextState
                                    },
                                    onCycleRepeatMode = {
                                        val nextMode = when (repeatMode) {
                                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                            else -> Player.REPEAT_MODE_OFF
                                        }
                                        repeatMode = nextMode
                                        prefs.edit().putInt("repeat_mode_int", nextMode).commit()
                                        controller?.repeatMode = nextMode
                                    },
                                    onCycleMonetMode = ::cycleMonet
                                )
                            }
                        }

                        LibraryScreen(
                            tracks = tracks,
                            playlists = playlists,
                            favoriteIds = favoriteIds,
                            selectedFolderUris = selectedFolderUris,
                            isScanning = isScanning,
                            scannedFilesCount = scannedFilesCount,
                            scanStatusText = scanStatusText,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            playbackProgress = if ((if (durationMs > 0L) durationMs else (currentTrack?.durationMs ?: 0L)) > 0L) {
                                (positionMs.toFloat() / (if (durationMs > 0L) durationMs else currentTrack!!.durationMs).toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                            darkTheme = darkTheme,
                            amoledTheme = amoledTheme,
                            playerStyle = playerStyle,
                            lyricsStyle = lyricsStyle,
                            crossfadeSec = crossfadeSec,
                            landscapeNavOnRight = landscapeNavOnRight,
                            monetMode = monetMode,
                            paletteStyle = paletteStyle,
                            coverAccent = coverAccent,
                            customSeedColor = customSeedColor,
                            hideDuplicates = hideDuplicates,
                            ignoreJunkFolders = ignoreJunkFolders,
                            autoRefreshLibrary = autoRefreshLibrary,
                            minDurationSec = minDurationSec,
                            onSetDarkTheme = ::saveThemePref,
                            onSetAmoledTheme = ::saveAmoledPref,
                            onSelectPlayerStyle = ::savePlayerStylePref,
                            onSelectLyricsStyle = ::saveLyricsStylePref,
                            onSelectCrossfadeSec = ::saveCrossfadePref,
                            onSetLandscapeNavOnRight = ::saveLandscapeNavRightPref,
                            onSelectMonetMode = ::saveMonetPref,
                            onSelectPaletteStyle = ::savePaletteStylePref,
                            onSelectCustomColor = ::saveCustomColorPref,
                            onCycleMonetMode = ::cycleMonet,
                            onSetHideDuplicates = ::saveHideDuplicatesPref,
                            onSetIgnoreJunkFolders = ::saveIgnoreJunkFoldersPref,
                            onSetAutoRefreshLibrary = ::saveAutoRefreshLibraryPref,
                            onSetMinDurationSec = ::saveMinDurationSecPref,
                            onRefreshLibrary = {
                                triggerScan(folders = selectedFolderUris, forceClearCovers = true)
                            },
                            onClearCoverCache = {
                                scanner.clearCoverCache()
                            },
                            onAddFolderClick = { folderPickerLauncher.launch(null) },
                            onRemoveFolderClick = { uriToRemove ->
                                val updated = selectedFolderUris.filterNot { it == uriToRemove }
                                saveFolders(updated)
                                triggerScan(folders = updated, forceClearCovers = false)
                            },
                            onSelectTrack = { index, list ->
                                playQueue(queue = list, startIndex = index, shuffle = shuffleEnabled)
                                isNowPlayingOpen = true
                            },
                            onShuffleAll = { list ->
                                if (list.isNotEmpty()) {
                                    val randomIndex = list.indices.random()
                                    playQueue(queue = list, startIndex = randomIndex, shuffle = true)
                                    isNowPlayingOpen = true
                                }
                            },
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                            onRenamePlaylistClick = { playlist ->
                                renamingPlaylist = playlist
                                renameText = playlist.name
                            },
                            onDeletePlaylistClick = { playlist ->
                                savePlaylists(playlists.filterNot { it.id == playlist.id })
                            },
                            onTogglePlayPause = {
                                controller?.let { mc ->
                                    if (mc.isPlaying) mc.pause() else mc.play()
                                }
                            },
                            onNextTrack = ::skipNext,
                            onPreviousTrack = ::skipPrevious,
                            onToggleTrackFavorite = ::toggleTrackFavorite,
                            onAddTrackToPlaylist = { target ->
                                playlistTargetTrack = target
                                if (playlists.isEmpty()) {
                                    showCreatePlaylistDialog = true
                                } else {
                                    showAddToPlaylistDialog = true
                                }
                            },
                            onPlayNext = ::playNext,
                            onAddToQueue = ::addToQueue,
                            onRemoveTrackFromPlaylist = ::removeTrackFromPlaylist,
                            onDeleteTrackFromDevice = ::deleteTrackFromDevice,
                            onOpenNowPlaying = {
                                if (currentTrack != null) {
                                    isNowPlayingOpen = true
                                }
                            }
                        )
                    }

                    if (showCreatePlaylistDialog) {
                        AlertDialog(
                            onDismissRequest = { showCreatePlaylistDialog = false },
                            title = { Text("Новый плейлист") },
                            text = {
                                OutlinedTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    singleLine = true,
                                    placeholder = { Text("Название плейлиста") }
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val cleanName = newPlaylistName.trim()
                                        if (cleanName.isNotEmpty()) {
                                            val seedTrack = playlistTargetTrack ?: activeTrack
                                            val initialIds = seedTrack?.id?.let { listOf(it) } ?: emptyList()
                                            val created = UserPlaylist(
                                                id = UUID.randomUUID().toString(),
                                                name = cleanName,
                                                trackIds = initialIds
                                            )
                                            savePlaylists(playlists + created)
                                            newPlaylistName = ""
                                        }
                                        showCreatePlaylistDialog = false
                                    }
                                ) {
                                    Text("Создать")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }

                    val targetRename = renamingPlaylist
                    if (targetRename != null) {
                        AlertDialog(
                            onDismissRequest = { renamingPlaylist = null },
                            title = { Text("Переименовать плейлист") },
                            text = {
                                OutlinedTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it },
                                    singleLine = true,
                                    placeholder = { Text("Новое название") }
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val clean = renameText.trim()
                                        if (clean.isNotEmpty()) {
                                            savePlaylists(
                                                playlists.map {
                                                    if (it.id == targetRename.id) it.copy(name = clean) else it
                                                }
                                            )
                                        }
                                        renamingPlaylist = null
                                    }
                                ) {
                                    Text("Сохранить")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { renamingPlaylist = null }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }

                    val targetPlaylistTrack = playlistTargetTrack ?: activeTrack
                    if (showAddToPlaylistDialog && targetPlaylistTrack != null) {
                        AlertDialog(
                            onDismissRequest = { showAddToPlaylistDialog = false },
                            title = { Text("Добавить в плейлист") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    playlists.forEach { playlist ->
                                        val alreadyIn = targetPlaylistTrack.id in playlist.trackIds
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    val updatedList = playlists.map { item ->
                                                        if (item.id == playlist.id) {
                                                            val newIds = if (alreadyIn) {
                                                                item.trackIds - targetPlaylistTrack.id
                                                            } else {
                                                                item.trackIds + targetPlaylistTrack.id
                                                            }
                                                            item.copy(trackIds = newIds)
                                                        } else {
                                                            item
                                                        }
                                                    }
                                                    savePlaylists(updatedList)
                                                    showAddToPlaylistDialog = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(playlist.name)
                                                Text(if (alreadyIn) "✓" else "+")
                                            }
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            showAddToPlaylistDialog = false
                                            showCreatePlaylistDialog = true
                                        }
                                    ) {
                                        Text("+ Новый плейлист")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                                    Text("Закрыть")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun saveCachedTracksToDisk(context: Context, list: List<LocalTrack>) {
    try {
        val arr = JSONArray()
        list.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("artist", t.artist)
            obj.put("album", t.album)
            obj.put("durationMs", t.durationMs)
            obj.put("contentUri", t.contentUri.toString())
            obj.put("artworkUri", t.artworkUri?.toString() ?: "")
            obj.put("folderName", t.folderName)
            obj.put("formatLabel", t.formatLabel)
            obj.put("bitrateKbps", t.bitrateKbps)
            obj.put("sampleRateHz", t.sampleRateHz)
            obj.put("dateAddedSec", t.dateAddedSec)
            arr.put(obj)
        }
        File(context.filesDir, "library_cache.json").writeText(arr.toString())
    } catch (_: Exception) {
    }
}

private fun loadCachedTracksFromDisk(context: Context): List<LocalTrack> {
    val file = File(context.filesDir, "library_cache.json")
    if (!file.exists()) return emptyList()
    return try {
        val arr = JSONArray(file.readText())
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val artRaw = obj.optString("artworkUri", "")
                add(
                    LocalTrack(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.getString("album"),
                        durationMs = obj.getLong("durationMs"),
                        contentUri = Uri.parse(obj.getString("contentUri")),
                        artworkUri = if (artRaw.isNotBlank()) Uri.parse(artRaw) else null,
                        folderName = obj.getString("folderName"),
                        formatLabel = obj.getString("formatLabel"),
                        bitrateKbps = obj.optInt("bitrateKbps", 320),
                        sampleRateHz = obj.optInt("sampleRateHz", 44100),
                        dateAddedSec = obj.optLong("dateAddedSec", 0L)
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun extractArtworkBytesAndBitmap(
    context: Context,
    track: LocalTrack
): Triple<ByteArray, Bitmap, Uri>? {
    val coverDir = File(context.cacheDir, "covers").apply { mkdirs() }
    val cacheKey = track.contentUri.toString().hashCode().toString()
    val cachedFile = File(coverDir, "art_$cacheKey.jpg")

    if (cachedFile.exists() && cachedFile.length() > 0L) {
        val bytes = try {
            cachedFile.readBytes()
        } catch (_: Exception) {
            null
        }
        val bmp = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
        if (bytes != null && bmp != null) {
            return Triple(bytes, bmp, Uri.fromFile(cachedFile))
        }
    }

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, track.contentUri)
        val rawBytes = retriever.embeddedPicture ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        val sampleSize = if (maxSide > 720) maxSide / 720 else 1
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize.coerceAtLeast(1) }
        val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null

        val outStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
        val jpegBytes = outStream.toByteArray()
        try {
            FileOutputStream(cachedFile).use { it.write(jpegBytes) }
        } catch (_: Exception) {
        }
        Triple(jpegBytes, bmp, Uri.fromFile(cachedFile))
    } catch (_: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

private fun loadPlaylistsFromPrefs(rawJson: String?): List<UserPlaylist> {
    if (rawJson.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(rawJson)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val idsArr = obj.optJSONArray("trackIds") ?: JSONArray()
                val ids = buildList {
                    for (j in 0 until idsArr.length()) {
                        add(idsArr.getLong(j))
                    }
                }
                add(
                    UserPlaylist(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Плейлист"),
                        trackIds = ids
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun encodePlaylistsToJson(list: List<UserPlaylist>): String {
    val arr = JSONArray()
    list.forEach { playlist ->
        val obj = JSONObject()
        obj.put("id", playlist.id)
        obj.put("name", playlist.name)
        val idsArr = JSONArray()
        playlist.trackIds.forEach { idsArr.put(it) }
        obj.put("trackIds", idsArr)
        arr.put(obj)
    }
    return arr.toString()
}

private fun resolveExternalTrack(context: Context, uri: Uri): LocalTrack? {
    var displayName: String? = null
    try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) displayName = cursor.getString(idx)
                }
            }
    } catch (_: Exception) {
    }

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val rawDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val rawBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.div(1000) ?: 0
        val rawSampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 44100
        } else {
            44100
        }
        val ext = (displayName ?: uri.lastPathSegment ?: "").substringAfterLast('.', "").uppercase()
        val format = when (ext) {
            "FLAC", "MP3", "WAV", "M4A", "AAC", "OGG", "OPUS", "DSF", "DFF", "ALAC", "APE", "WV" -> ext
            else -> "AUDIO"
        }
        val fallbackName = (displayName ?: uri.lastPathSegment ?: "Аудиофайл").substringBeforeLast('.')
        LocalTrack(
            id = -(uri.toString().hashCode().toLong() and 0x7FFFFFFFL).coerceAtLeast(1L),
            title = rawTitle?.takeIf { it.isNotBlank() } ?: fallbackName,
            artist = rawArtist?.takeIf { it.isNotBlank() } ?: "Неизвестный исполнитель",
            album = rawAlbum?.takeIf { it.isNotBlank() } ?: "Внешний файл",
            durationMs = rawDuration.coerceAtLeast(0L),
            contentUri = uri,
            artworkUri = null,
            folderName = "Внешние",
            formatLabel = format,
            bitrateKbps = rawBitrate,
            sampleRateHz = rawSampleRate,
            dateAddedSec = System.currentTimeMillis() / 1000L
        )
    } catch (_: Exception) {
        val fallbackName = (displayName ?: uri.lastPathSegment ?: "Аудиофайл").substringBeforeLast('.')
        LocalTrack(
            id = -(uri.toString().hashCode().toLong() and 0x7FFFFFFFL).coerceAtLeast(1L),
            title = fallbackName,
            artist = "Внешний файл",
            album = "Sonora",
            durationMs = 0L,
            contentUri = uri,
            artworkUri = null,
            folderName = "Внешние",
            formatLabel = "AUDIO",
            bitrateKbps = 0,
            sampleRateHz = 44100,
            dateAddedSec = System.currentTimeMillis() / 1000L
        )
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}
