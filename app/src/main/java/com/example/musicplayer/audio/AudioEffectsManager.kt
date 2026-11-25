package com.example.musicplayer.audio

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class EqBandUi(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int
)

data class EqPreset(
    val name: String,
    val levelsMb: List<Int>
)

/**
 * Управляет встроенным Equalizer/BassBoost и уведомляет системные/сторонние эквалайзеры
 * (Wavelet, Poweramp EQ, системный DSP) об активной audioSessionId ExoPlayer.
 */
object AudioEffectsController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var appContext: Context? = null
    private var currentSessionId: Int = 0

    var isSupported by mutableStateOf(false)
        private set
    var eqEnabled by mutableStateOf(false)
        private set
    var bands by mutableStateOf<List<EqBandUi>>(emptyList())
        private set
    var minLevelMb by mutableIntStateOf(-1500)
        private set
    var maxLevelMb by mutableIntStateOf(1500)
        private set
    var selectedPresetName by mutableStateOf("Обычный")
        private set
    var bassBoostStrength by mutableIntStateOf(0)
        private set

    val activeSessionId: Int get() = currentSessionId

    val builtInPresets = listOf(
        EqPreset("Обычный", listOf(0, 0, 0, 0, 0)),
        EqPreset("Бас", listOf(550, 350, 0, -100, 100)),
        EqPreset("Рок", listOf(450, 200, -150, 250, 450)),
        EqPreset("Поп", listOf(-100, 250, 400, 200, -100)),
        EqPreset("Электро", listOf(500, 250, -100, 300, 500)),
        EqPreset("Вокал", listOf(-250, -100, 400, 350, 100))
    )

    fun attachToSession(context: Context, audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) return
        release()
        appContext = context.applicationContext
        currentSessionId = audioSessionId

        broadcastSessionControl(context.applicationContext, AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION, audioSessionId)

        val prefs = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
        eqEnabled = prefs.getBoolean("eq_enabled", false)
        selectedPresetName = prefs.getString("eq_preset", "Обычный") ?: "Обычный"
        bassBoostStrength = prefs.getInt("eq_bass_boost", 0).coerceIn(0, 1000)

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            val range = eq.bandLevelRange
            minLevelMb = range[0].toInt()
            maxLevelMb = range[1].toInt()

            val bandCount = eq.numberOfBands.toInt().coerceAtLeast(0)
            val savedRaw = prefs.getString("eq_bands_mb", null)
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                .orEmpty()

            val loadedBands = (0 until bandCount).map { idx ->
                val freqHz = (eq.getCenterFreq(idx.toShort()) / 1000)
                val level = savedRaw.getOrNull(idx)?.coerceIn(minLevelMb, maxLevelMb) ?: 0
                eq.setBandLevel(idx.toShort(), level.toShort())
                EqBandUi(index = idx, centerFreqHz = freqHz, levelMb = level)
            }
            bands = loadedBands
            eq.enabled = eqEnabled
            isSupported = bandCount > 0
        } catch (_: Exception) {
            isSupported = false
            equalizer = null
        }

        try {
            val bb = BassBoost(0, audioSessionId)
            bassBoost = bb
            if (bb.strengthSupported) {
                bb.setStrength(bassBoostStrength.toShort())
            }
            bb.enabled = eqEnabled && bassBoostStrength > 0
        } catch (_: Exception) {
            bassBoost = null
        }
    }

    fun hasSystemEqualizer(context: Context): Boolean {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        return context.packageManager.resolveActivity(intent, 0) != null
    }

    fun openSystemEqualizer(context: Context): Boolean {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            if (currentSessionId != 0) {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, currentSessionId)
            }
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                if (eqEnabled) {
                    setEqEnabled(context, false)
                }
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun broadcastSessionControl(context: Context, action: String, sessionId: Int) {
        if (sessionId == 0) return
        try {
            val intent = Intent(action).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }

    fun setEqEnabled(context: Context, enabled: Boolean) {
        eqEnabled = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled && bassBoostStrength > 0
        } catch (_: Exception) {
        }
        context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("eq_enabled", enabled)
            .apply()
    }

    fun setBandLevel(context: Context, bandIndex: Int, levelMb: Int) {
        val clamped = levelMb.coerceIn(minLevelMb, maxLevelMb)
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
        } catch (_: Exception) {
        }
        val updated = bands.map {
            if (it.index == bandIndex) it.copy(levelMb = clamped) else it
        }
        bands = updated
        selectedPresetName = "Свой"
        persistBandsAndPreset(context, updated, "Свой")
    }

    fun applyPreset(context: Context, preset: EqPreset) {
        if (!eqEnabled) {
            setEqEnabled(context, true)
        }
        val updated = bands.mapIndexed { idx, band ->
            val target = preset.levelsMb.getOrElse(idx) { 0 }.coerceIn(minLevelMb, maxLevelMb)
            try {
                equalizer?.setBandLevel(idx.toShort(), target.toShort())
            } catch (_: Exception) {
            }
            band.copy(levelMb = target)
        }
        bands = updated
        selectedPresetName = preset.name
        persistBandsAndPreset(context, updated, preset.name)
    }

    fun setBassBoost(context: Context, strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        bassBoostStrength = clamped
        if (clamped > 0 && !eqEnabled) {
            setEqEnabled(context, true)
        }
        try {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.setStrength(clamped.toShort())
                }
                bb.enabled = eqEnabled && clamped > 0
            }
        } catch (_: Exception) {
        }
        context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("eq_bass_boost", clamped)
            .apply()
    }

    private fun persistBandsAndPreset(context: Context, list: List<EqBandUi>, presetName: String) {
        val serialized = list.joinToString(",") { it.levelMb.toString() }
        context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("eq_bands_mb", serialized)
            .putString("eq_preset", presetName)
            .apply()
    }

    fun release() {
        val ctx = appContext
        val oldSession = currentSessionId
        if (ctx != null && oldSession != 0) {
            broadcastSessionControl(ctx, AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION, oldSession)
        }
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
        currentSessionId = 0
    }
}
