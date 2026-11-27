package com.example.musicplayer.audio

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class EqUiMode {
    GRAPHIC,
    PARAMETRIC
}

enum class EqFilterType(val title: String, val shortLabel: String, val usesGain: Boolean) {
    PEAKING("Пиковый (Bell)", "Peak", true),
    LOW_SHELF("Низкий шельф", "LowShelf", true),
    HIGH_SHELF("Высокий шельф", "HighShelf", true),
    HIGH_PASS("ФВЧ (срез низа)", "HPF", false),
    LOW_PASS("ФНЧ (срез верха)", "LPF", false),
    NOTCH("Режекторный", "Notch", false)
}

data class ParametricBand(
    val index: Int,
    val freqHz: Float,
    val gainDb: Float,
    val q: Float,
    val filterType: EqFilterType,
    val enabled: Boolean = true
)

data class ParametricPreset(
    val name: String,
    val preampDb: Float,
    val bands: List<ParametricBand>,
    val isCustom: Boolean = false
)

internal data class BiquadCoeffs(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double,
    val isIdentity: Boolean
)

internal data class ParametricSnapshot(
    val version: Int,
    val enabled: Boolean,
    val preampLinear: Double,
    val bands: List<ParametricBand>,
    val channelBalance: Float = 0f,
    val stereoWidth: Float = 1f,
    val monoAudio: Boolean = false
)

object ParametricEqController {
    private const val PREFS_NAME = "music_player_prefs"
    private const val KEY_ENABLED = "peq_enabled"
    private const val KEY_UI_MODE = "peq_ui_mode"
    private const val KEY_PREAMP = "peq_preamp_db"
    private const val KEY_PRESET = "peq_preset_name"
    private const val KEY_BANDS = "peq_bands_v2"
    private const val KEY_BANDS_V1 = "peq_bands_v1"
    private const val KEY_USER_PRESETS = "peq_user_presets_v1"
    private const val KEY_BALANCE = "dsp_channel_balance"
    private const val KEY_STEREO_WIDTH = "dsp_stereo_width"
    private const val KEY_MONO = "dsp_mono_audio"

    const val MIN_BANDS = 3
    const val MAX_BANDS = 10

    var enabled by mutableStateOf(false)
        private set

    var uiMode by mutableStateOf(EqUiMode.GRAPHIC)
        private set

    var preampDb by mutableFloatStateOf(0f)
        private set

    var selectedPresetName by mutableStateOf("Ровная (Flat)")
        private set

    var bands by mutableStateOf(defaultBands())
        private set

    var userPresets by mutableStateOf<List<ParametricPreset>>(emptyList())
        private set

    var channelBalance by mutableFloatStateOf(0f)
        private set

    var stereoWidth by mutableFloatStateOf(1f)
        private set

    var monoAudio by mutableStateOf(false)
        private set

    var revision by mutableIntStateOf(0)
        private set

    @Volatile
    private var loaded = false

    @Volatile
    internal var snapshot = ParametricSnapshot(
        version = 0,
        enabled = false,
        preampLinear = 1.0,
        bands = defaultBands()
    )
        private set

    val builtInPresets: List<ParametricPreset> = listOf(
        ParametricPreset(
            name = "Ровная (Flat)",
            preampDb = 0f,
            bands = defaultBands()
        ),
        ParametricPreset(
            name = "Harman АЧХ",
            preampDb = -3.0f,
            bands = listOf(
                ParametricBand(0, 45f, 3.5f, 0.71f, EqFilterType.LOW_SHELF, true),
                ParametricBand(1, 105f, 2.0f, 1.0f, EqFilterType.PEAKING, true),
                ParametricBand(2, 240f, -1.5f, 1.15f, EqFilterType.PEAKING, true),
                ParametricBand(3, 800f, -0.5f, 1.2f, EqFilterType.PEAKING, true),
                ParametricBand(4, 1800f, 1.0f, 1.3f, EqFilterType.PEAKING, true),
                ParametricBand(5, 3400f, 3.0f, 1.5f, EqFilterType.PEAKING, true),
                ParametricBand(6, 7000f, -1.5f, 2.2f, EqFilterType.PEAKING, true),
                ParametricBand(7, 12000f, 2.0f, 0.71f, EqFilterType.HIGH_SHELF, true)
            )
        ),
        ParametricPreset(
            name = "Глубокий саб-бас",
            preampDb = -4.5f,
            bands = listOf(
                ParametricBand(0, 24f, 0f, 0.71f, EqFilterType.HIGH_PASS, true),
                ParametricBand(1, 55f, 5.5f, 0.75f, EqFilterType.LOW_SHELF, true),
                ParametricBand(2, 130f, 2.5f, 1.1f, EqFilterType.PEAKING, true),
                ParametricBand(3, 350f, -1.8f, 1.3f, EqFilterType.PEAKING, true),
                ParametricBand(4, 1000f, 0f, 1.2f, EqFilterType.PEAKING, true),
                ParametricBand(5, 2800f, 1.5f, 1.4f, EqFilterType.PEAKING, true),
                ParametricBand(6, 6500f, 0.5f, 1.6f, EqFilterType.PEAKING, true),
                ParametricBand(7, 12000f, 2.0f, 0.71f, EqFilterType.HIGH_SHELF, true)
            )
        ),
        ParametricPreset(
            name = "Вокал и детали",
            preampDb = -2.5f,
            bands = listOf(
                ParametricBand(0, 40f, -1.5f, 0.71f, EqFilterType.LOW_SHELF, true),
                ParametricBand(1, 120f, -1.0f, 1.1f, EqFilterType.PEAKING, true),
                ParametricBand(2, 260f, -1.5f, 1.2f, EqFilterType.PEAKING, true),
                ParametricBand(3, 1400f, 2.5f, 1.25f, EqFilterType.PEAKING, true),
                ParametricBand(4, 3400f, 3.5f, 1.45f, EqFilterType.PEAKING, true),
                ParametricBand(5, 5800f, 1.5f, 1.6f, EqFilterType.PEAKING, true),
                ParametricBand(6, 8500f, 1.0f, 1.8f, EqFilterType.PEAKING, true),
                ParametricBand(7, 13500f, 2.5f, 0.71f, EqFilterType.HIGH_SHELF, true)
            )
        ),
        ParametricPreset(
            name = "Тёплый винил",
            preampDb = -2.5f,
            bands = listOf(
                ParametricBand(0, 50f, 2.5f, 0.65f, EqFilterType.LOW_SHELF, true),
                ParametricBand(1, 140f, 2.0f, 0.9f, EqFilterType.PEAKING, true),
                ParametricBand(2, 340f, 1.5f, 1.0f, EqFilterType.PEAKING, true),
                ParametricBand(3, 1100f, 0.5f, 1.2f, EqFilterType.PEAKING, true),
                ParametricBand(4, 3200f, -1.5f, 1.3f, EqFilterType.PEAKING, true),
                ParametricBand(5, 6200f, -2.5f, 1.5f, EqFilterType.PEAKING, true),
                ParametricBand(6, 10000f, -3.0f, 1.4f, EqFilterType.PEAKING, true),
                ParametricBand(7, 15000f, -3.5f, 0.71f, EqFilterType.HIGH_SHELF, true)
            )
        ),
        ParametricPreset(
            name = "V-Shape Hi-Fi",
            preampDb = -4.0f,
            bands = listOf(
                ParametricBand(0, 55f, 4.5f, 0.71f, EqFilterType.LOW_SHELF, true),
                ParametricBand(1, 130f, 2.0f, 1.1f, EqFilterType.PEAKING, true),
                ParametricBand(2, 420f, -2.0f, 1.1f, EqFilterType.PEAKING, true),
                ParametricBand(3, 1100f, -2.5f, 1.0f, EqFilterType.PEAKING, true),
                ParametricBand(4, 2800f, 1.5f, 1.4f, EqFilterType.PEAKING, true),
                ParametricBand(5, 5500f, 2.5f, 1.5f, EqFilterType.PEAKING, true),
                ParametricBand(6, 9000f, 3.5f, 1.6f, EqFilterType.PEAKING, true),
                ParametricBand(7, 14000f, 4.0f, 0.71f, EqFilterType.HIGH_SHELF, true)
            )
        ),
        ParametricPreset(
            name = "Электроника / EDM",
            preampDb = -4.0f,
            bands = listOf(
                ParametricBand(0, 26f, 0f, 0.71f, EqFilterType.HIGH_PASS, true),
                ParametricBand(1, 52f, 5.0f, 0.75f, EqFilterType.LOW_SHELF, true),
                ParametricBand(2, 110f, 3.0f, 1.3f, EqFilterType.PEAKING, true),
                ParametricBand(3, 380f, -2.0f, 1.2f, EqFilterType.PEAKING, true),
                ParametricBand(4, 1500f, 0.5f, 1.4f, EqFilterType.PEAKING, true),
                ParametricBand(5, 4200f, 2.5f, 1.5f, EqFilterType.PEAKING, true),
                ParametricBand(6, 8500f, 3.0f, 1.5f, EqFilterType.PEAKING, true),
                ParametricBand(7, 14000f, 3.5f, 0.71f, EqFilterType.HIGH_SHELF, true)
            )
        ),
        ParametricPreset(
            name = "Рок и металл",
            preampDb = -3.5f,
            bands = listOf(
                ParametricBand(0, 65f, 3.5f, 0.75f, EqFilterType.LOW_SHELF, true),
                ParametricBand(1, 140f, 1.5f, 1.2f, EqFilterType.PEAKING, true),
                ParametricBand(2, 400f, -2.5f, 1.3f, EqFilterType.PEAKING, true),
                ParametricBand(3, 950f, -1.0f, 1.4f, EqFilterType.PEAKING, true),
                ParametricBand(4, 2400f, 3.0f, 1.5f, EqFilterType.PEAKING, true),
                ParametricBand(5, 4800f, 2.0f, 1.7f, EqFilterType.PEAKING, true),
                ParametricBand(6, 7500f, -1.0f, 2.5f, EqFilterType.PEAKING, true),
                ParametricBand(7, 12000f, 2.5f, 0.71f, EqFilterType.HIGH_SHELF, true)
            )
        ),
        ParametricPreset(
            name = "Ночной / Мягкий",
            preampDb = -2.5f,
            bands = listOf(
                ParametricBand(0, 60f, 3.5f, 0.65f, EqFilterType.LOW_SHELF, true),
                ParametricBand(1, 150f, 1.5f, 1.0f, EqFilterType.PEAKING, true),
                ParametricBand(2, 500f, -0.5f, 1.2f, EqFilterType.PEAKING, true),
                ParametricBand(3, 1800f, 0.5f, 1.3f, EqFilterType.PEAKING, true),
                ParametricBand(4, 4200f, -1.5f, 1.4f, EqFilterType.PEAKING, true),
                ParametricBand(5, 7000f, -2.5f, 1.5f, EqFilterType.PEAKING, true),
                ParametricBand(6, 11000f, 1.0f, 0.8f, EqFilterType.HIGH_SHELF, true),
                ParametricBand(7, 16500f, 0f, 0.71f, EqFilterType.LOW_PASS, true)
            )
        ),
        ParametricPreset(
            name = "Студийный контроль",
            preampDb = -1.5f,
            bands = listOf(
                ParametricBand(0, 28f, 0f, 0.71f, EqFilterType.HIGH_PASS, true),
                ParametricBand(1, 80f, 0.5f, 1.2f, EqFilterType.PEAKING, true),
                ParametricBand(2, 220f, -1.0f, 1.5f, EqFilterType.PEAKING, true),
                ParametricBand(3, 650f, 0f, 1.4f, EqFilterType.PEAKING, true),
                ParametricBand(4, 2200f, 1.0f, 1.6f, EqFilterType.PEAKING, true),
                ParametricBand(5, 4800f, 0.5f, 1.6f, EqFilterType.PEAKING, true),
                ParametricBand(6, 8200f, -1.0f, 2.8f, EqFilterType.PEAKING, true),
                ParametricBand(7, 13000f, 1.5f, 0.71f, EqFilterType.HIGH_SHELF, true)
            )
        )
    )

    fun defaultBands(): List<ParametricBand> = listOf(
        ParametricBand(0, 35f, 0f, 0.71f, EqFilterType.LOW_SHELF, true),
        ParametricBand(1, 80f, 0f, 1.2f, EqFilterType.PEAKING, true),
        ParametricBand(2, 180f, 0f, 1.3f, EqFilterType.PEAKING, true),
        ParametricBand(3, 500f, 0f, 1.4f, EqFilterType.PEAKING, true),
        ParametricBand(4, 1400f, 0f, 1.4f, EqFilterType.PEAKING, true),
        ParametricBand(5, 3400f, 0f, 1.5f, EqFilterType.PEAKING, true),
        ParametricBand(6, 7500f, 0f, 1.6f, EqFilterType.PEAKING, true),
        ParametricBand(7, 13500f, 0f, 0.71f, EqFilterType.HIGH_SHELF, true)
    )

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            enabled = prefs.getBoolean(KEY_ENABLED, false)
            uiMode = runCatching {
                EqUiMode.valueOf(prefs.getString(KEY_UI_MODE, EqUiMode.GRAPHIC.name) ?: EqUiMode.GRAPHIC.name)
            }.getOrDefault(EqUiMode.GRAPHIC)
            preampDb = prefs.getFloat(KEY_PREAMP, 0f).coerceIn(-12f, 6f)
            selectedPresetName = prefs.getString(KEY_PRESET, "Ровная (Flat)") ?: "Ровная (Flat)"
            val serialized = prefs.getString(KEY_BANDS, null) ?: prefs.getString(KEY_BANDS_V1, null)
            bands = parseBands(serialized) ?: defaultBands()
            userPresets = parseUserPresets(prefs.getString(KEY_USER_PRESETS, null))
            channelBalance = prefs.getFloat(KEY_BALANCE, 0f).coerceIn(-1f, 1f)
            stereoWidth = prefs.getFloat(KEY_STEREO_WIDTH, 1f).coerceIn(0f, 1.8f)
            monoAudio = prefs.getBoolean(KEY_MONO, false)
            loaded = true
            publishSnapshot()
        }
    }

    fun setChannelBalance(context: Context, balance: Float) {
        ensureLoaded(context)
        channelBalance = balance.coerceIn(-1f, 1f)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_BALANCE, channelBalance).apply()
        publishSnapshot()
    }

    fun setStereoWidth(context: Context, width: Float) {
        ensureLoaded(context)
        stereoWidth = width.coerceIn(0f, 1.8f)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_STEREO_WIDTH, stereoWidth).apply()
        publishSnapshot()
    }

    fun setMonoAudio(context: Context, mono: Boolean) {
        ensureLoaded(context)
        monoAudio = mono
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MONO, mono).apply()
        publishSnapshot()
    }

    fun resetAudioStage(context: Context) {
        ensureLoaded(context)
        channelBalance = 0f
        stereoWidth = 1f
        monoAudio = false
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_BALANCE, 0f)
            .putFloat(KEY_STEREO_WIDTH, 1f)
            .putBoolean(KEY_MONO, false)
            .apply()
        publishSnapshot()
    }

    fun setUiMode(context: Context, mode: EqUiMode) {
        ensureLoaded(context)
        uiMode = mode
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_UI_MODE, mode.name).apply()
    }

    fun setEnabled(context: Context, isEnabled: Boolean) {
        ensureLoaded(context)
        enabled = isEnabled
        if (isEnabled && AudioEffectsController.eqEnabled) {
            AudioEffectsController.setEqEnabled(context, false)
        }
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, isEnabled).apply()
        publishSnapshot()
    }

    fun setPreamp(context: Context, db: Float) {
        ensureLoaded(context)
        preampDb = db.coerceIn(-12f, 6f)
        selectedPresetName = "Свой"
        saveState(context)
        publishSnapshot()
    }

    fun autoAdjustPreampHeadroom(context: Context) {
        ensureLoaded(context)
        var maxBoost = 0f
        for (i in 0 until 72) {
            val f = 20f * 1000f.pow(i / 71f)
            val db = computeMagnitudeDbAt(f, bands, preamp = 0f)
            if (db > maxBoost) maxBoost = db
        }
        val targetPreamp = if (maxBoost > 0.25f) {
            (-(maxBoost + 0.5f)).coerceIn(-12f, 0f)
        } else {
            0f
        }
        val snapped = (targetPreamp * 2f).toInt() / 2f
        preampDb = snapped
        saveState(context)
        publishSnapshot()
    }

    fun addBand(context: Context): Int {
        ensureLoaded(context)
        if (bands.size >= MAX_BANDS) return bands.lastIndex
        val newFreq = suggestNewBandFrequency(bands)
        val updated = (bands + ParametricBand(
            index = bands.size,
            freqHz = newFreq,
            gainDb = 0f,
            q = 1.4f,
            filterType = EqFilterType.PEAKING,
            enabled = true
        )).sortedBy { it.freqHz }.mapIndexed { idx, b -> b.copy(index = idx) }
        val addedIdx = updated.indexOfFirst { abs(it.freqHz - newFreq) < 1f }.coerceAtLeast(0)
        bands = updated
        selectedPresetName = "Свой"
        saveState(context)
        publishSnapshot()
        return addedIdx
    }

    fun removeBand(context: Context, bandIndex: Int): Int {
        ensureLoaded(context)
        if (bands.size <= MIN_BANDS) return bandIndex.coerceIn(0, bands.lastIndex)
        val filtered = bands.filterNot { it.index == bandIndex }
            .mapIndexed { idx, b -> b.copy(index = idx) }
        bands = filtered
        selectedPresetName = "Свой"
        saveState(context)
        publishSnapshot()
        return (bandIndex - 1).coerceIn(0, filtered.lastIndex)
    }

    private fun suggestNewBandFrequency(existing: List<ParametricBand>): Float {
        if (existing.isEmpty()) return 1000f
        val sorted = existing.map { log10(it.freqHz.coerceIn(20f, 20000f)) }.sorted()
        var bestMid = log10(1000f)
        var bestGap = -1f
        for (i in 0 until sorted.lastIndex) {
            val gap = sorted[i + 1] - sorted[i]
            if (gap > bestGap) {
                bestGap = gap
                bestMid = (sorted[i] + sorted[i + 1]) / 2f
            }
        }
        return 10f.pow(bestMid).coerceIn(20f, 20000f)
    }

    fun updateBand(
        context: Context,
        bandIndex: Int,
        freqHz: Float? = null,
        gainDb: Float? = null,
        q: Float? = null,
        filterType: EqFilterType? = null,
        bandEnabled: Boolean? = null
    ) {
        ensureLoaded(context)
        bands = bands.map { band ->
            if (band.index != bandIndex) band
            else band.copy(
                freqHz = (freqHz ?: band.freqHz).coerceIn(20f, 20000f),
                gainDb = (gainDb ?: band.gainDb).coerceIn(-12f, 12f),
                q = (q ?: band.q).coerceIn(0.4f, 8.0f),
                filterType = filterType ?: band.filterType,
                enabled = bandEnabled ?: band.enabled
            )
        }
        selectedPresetName = "Свой"
        if (!enabled) {
            enabled = true
            if (AudioEffectsController.eqEnabled) {
                AudioEffectsController.setEqEnabled(context, false)
            }
        }
        saveState(context)
        publishSnapshot()
    }

    fun applyPreset(context: Context, preset: ParametricPreset) {
        ensureLoaded(context)
        bands = preset.bands.mapIndexed { idx, b -> b.copy(index = idx) }
        preampDb = preset.preampDb.coerceIn(-12f, 6f)
        selectedPresetName = preset.name
        if (!enabled) {
            enabled = true
            if (AudioEffectsController.eqEnabled) {
                AudioEffectsController.setEqEnabled(context, false)
            }
        }
        saveState(context)
        publishSnapshot()
    }

    fun saveUserPreset(context: Context, rawName: String) {
        ensureLoaded(context)
        val cleanName = rawName.trim()
        if (cleanName.isBlank()) return
        val newPreset = ParametricPreset(
            name = cleanName,
            preampDb = preampDb,
            bands = bands.mapIndexed { idx, b -> b.copy(index = idx) },
            isCustom = true
        )
        val updated = userPresets.filterNot { it.name.equals(cleanName, ignoreCase = true) } + newPreset
        userPresets = updated
        selectedPresetName = cleanName
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_PRESETS, serializeUserPresets(updated))
            .putString(KEY_PRESET, cleanName)
            .apply()
    }

    fun deleteUserPreset(context: Context, presetName: String) {
        ensureLoaded(context)
        val updated = userPresets.filterNot { it.name == presetName }
        userPresets = updated
        if (selectedPresetName == presetName) {
            selectedPresetName = "Свой"
        }
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_PRESETS, serializeUserPresets(updated))
            .putString(KEY_PRESET, selectedPresetName)
            .apply()
    }

    fun resetFlat(context: Context) {
        applyPreset(context, builtInPresets.first())
    }

    private fun saveState(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putFloat(KEY_PREAMP, preampDb)
            .putString(KEY_PRESET, selectedPresetName)
            .putString(KEY_BANDS, serializeBands(bands))
            .apply()
    }

    private fun serializeBands(list: List<ParametricBand>): String {
        return list.joinToString(";") { b ->
            "${b.index},${b.freqHz},${b.gainDb},${b.q},${b.filterType.name},${if (b.enabled) 1 else 0}"
        }
    }

    private fun parseBands(raw: String?): List<ParametricBand>? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val parsed = raw.split(";").mapIndexedNotNull { idx, token ->
                val parts = token.split(",")
                if (parts.size < 6) return@mapIndexedNotNull null
                ParametricBand(
                    index = idx,
                    freqHz = parts[1].toFloat().coerceIn(20f, 20000f),
                    gainDb = parts[2].toFloat().coerceIn(-12f, 12f),
                    q = parts[3].toFloat().coerceIn(0.4f, 8.0f),
                    filterType = EqFilterType.valueOf(parts[4]),
                    enabled = parts[5] == "1"
                )
            }
            if (parsed.size in MIN_BANDS..MAX_BANDS) parsed else null
        }.getOrNull()
    }

    private fun serializeUserPresets(presets: List<ParametricPreset>): String {
        val arr = JSONArray()
        presets.forEach { p ->
            val obj = JSONObject()
            obj.put("name", p.name)
            obj.put("preamp", p.preampDb.toDouble())
            obj.put("bands", serializeBands(p.bands))
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseUserPresets(raw: String?): List<ParametricPreset> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name").trim()
                    val preamp = obj.optDouble("preamp", 0.0).toFloat().coerceIn(-12f, 6f)
                    val bList = parseBands(obj.optString("bands")) ?: continue
                    if (name.isNotBlank()) {
                        add(ParametricPreset(name = name, preampDb = preamp, bands = bList, isCustom = true))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun publishSnapshot() {
        val nextVersion = revision + 1
        revision = nextVersion
        snapshot = ParametricSnapshot(
            version = nextVersion,
            enabled = enabled,
            preampLinear = 10.0.pow(preampDb / 20.0),
            bands = bands.toList(),
            channelBalance = channelBalance,
            stereoWidth = stereoWidth,
            monoAudio = monoAudio
        )
    }

    internal fun computeBiquadCoeffs(band: ParametricBand, sampleRate: Int): BiquadCoeffs {
        if (!band.enabled || sampleRate <= 0) {
            return BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0, isIdentity = true)
        }
        if (band.filterType.usesGain && abs(band.gainDb) < 0.05f) {
            return BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0, isIdentity = true)
        }

        val fs = sampleRate.toDouble()
        val f0 = band.freqHz.toDouble().coerceIn(20.0, fs * 0.45)
        val gainDb = band.gainDb.toDouble().coerceIn(-15.0, 15.0)
        val q = band.q.toDouble().coerceIn(0.4, 8.0)

        val w0 = 2.0 * PI * f0 / fs
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val amp = 10.0.pow(gainDb / 40.0)
        val alpha = sinW0 / (2.0 * q)

        val b0: Double
        val b1: Double
        val b2: Double
        val a0: Double
        val a1: Double
        val a2: Double

        when (band.filterType) {
            EqFilterType.PEAKING -> {
                b0 = 1.0 + alpha * amp
                b1 = -2.0 * cosW0
                b2 = 1.0 - alpha * amp
                a0 = 1.0 + alpha / amp
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha / amp
            }
            EqFilterType.LOW_SHELF -> {
                val twoSqrtAAlpha = 2.0 * sqrt(amp) * alpha
                b0 = amp * ((amp + 1.0) - (amp - 1.0) * cosW0 + twoSqrtAAlpha)
                b1 = 2.0 * amp * ((amp - 1.0) - (amp + 1.0) * cosW0)
                b2 = amp * ((amp + 1.0) - (amp - 1.0) * cosW0 - twoSqrtAAlpha)
                a0 = (amp + 1.0) + (amp - 1.0) * cosW0 + twoSqrtAAlpha
                a1 = -2.0 * ((amp - 1.0) + (amp + 1.0) * cosW0)
                a2 = (amp + 1.0) + (amp - 1.0) * cosW0 - twoSqrtAAlpha
            }
            EqFilterType.HIGH_SHELF -> {
                val twoSqrtAAlpha = 2.0 * sqrt(amp) * alpha
                b0 = amp * ((amp + 1.0) + (amp - 1.0) * cosW0 + twoSqrtAAlpha)
                b1 = -2.0 * amp * ((amp - 1.0) + (amp + 1.0) * cosW0)
                b2 = amp * ((amp + 1.0) + (amp - 1.0) * cosW0 - twoSqrtAAlpha)
                a0 = (amp + 1.0) - (amp - 1.0) * cosW0 + twoSqrtAAlpha
                a1 = 2.0 * ((amp - 1.0) - (amp + 1.0) * cosW0)
                a2 = (amp + 1.0) - (amp - 1.0) * cosW0 - twoSqrtAAlpha
            }
            EqFilterType.HIGH_PASS -> {
                b0 = (1.0 + cosW0) / 2.0
                b1 = -(1.0 + cosW0)
                b2 = (1.0 + cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            EqFilterType.LOW_PASS -> {
                b0 = (1.0 - cosW0) / 2.0
                b1 = 1.0 - cosW0
                b2 = (1.0 - cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            EqFilterType.NOTCH -> {
                b0 = 1.0
                b1 = -2.0 * cosW0
                b2 = 1.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
        }

        return BiquadCoeffs(
            b0 = b0 / a0,
            b1 = b1 / a0,
            b2 = b2 / a0,
            a1 = a1 / a0,
            a2 = a2 / a0,
            isIdentity = false
        )
    }

    fun computeMagnitudeDbAt(
        freqHz: Float,
        bandList: List<ParametricBand> = bands,
        preamp: Float = preampDb,
        sampleRate: Int = 48000
    ): Float {
        val w = 2.0 * PI * freqHz.toDouble().coerceIn(20.0, sampleRate * 0.45) / sampleRate.toDouble()
        val cosW = cos(w)
        val sinW = sin(w)
        val cos2W = cos(2.0 * w)
        val sin2W = sin(2.0 * w)

        var magSqProduct = 1.0
        for (band in bandList) {
            val c = computeBiquadCoeffs(band, sampleRate)
            if (c.isIdentity) continue
            val nr = c.b0 + c.b1 * cosW + c.b2 * cos2W
            val ni = -c.b1 * sinW - c.b2 * sin2W
            val dr = 1.0 + c.a1 * cosW + c.a2 * cos2W
            val di = -c.a1 * sinW - c.a2 * sin2W
            val num = nr * nr + ni * ni
            val den = max(dr * dr + di * di, 1e-20)
            magSqProduct *= (num / den)
        }
        return (preamp + 10.0 * log10(max(magSqProduct, 1e-12))).toFloat()
    }
}

@UnstableApi
class ParametricEqAudioProcessor : BaseAudioProcessor() {

    private var appliedVersion = -1
    private var coeffsList: Array<BiquadCoeffs> = emptyArray()
    private var z1: DoubleArray = DoubleArray(0)
    private var z2: DoubleArray = DoubleArray(0)
    private var lastChannelCount = 0
    private var lastSampleRate = 0

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        z1.fill(0.0)
        z2.fill(0.0)
    }

    override fun onReset() {
        coeffsList = emptyArray()
        z1 = DoubleArray(0)
        z2 = DoubleArray(0)
        appliedVersion = -1
        lastChannelCount = 0
        lastSampleRate = 0
    }

    private fun syncCoefficients(snap: ParametricSnapshot, sampleRate: Int, channelCount: Int) {
        val bandCount = snap.bands.size
        val neededStates = bandCount * channelCount
        if (lastChannelCount != channelCount || z1.size != neededStates) {
            z1 = DoubleArray(neededStates)
            z2 = DoubleArray(neededStates)
            lastChannelCount = channelCount
        }
        if (appliedVersion != snap.version || lastSampleRate != sampleRate || coeffsList.size != bandCount) {
            coeffsList = Array(bandCount) { idx ->
                ParametricEqController.computeBiquadCoeffs(snap.bands[idx], sampleRate)
            }
            appliedVersion = snap.version
            lastSampleRate = sampleRate
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val snap = ParametricEqController.snapshot
        val channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        val sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000)

        val hasSpatialDsp = channelCount == 2 && (
            snap.monoAudio ||
                abs(snap.channelBalance) > 0.01f ||
                abs(snap.stereoWidth - 1f) > 0.01f
            )

        if (!snap.enabled && !hasSpatialDsp) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        val activeStages: Array<BiquadCoeffs>
        val preamp: Double
        val runEq: Boolean
        if (snap.enabled) {
            syncCoefficients(snap, sampleRate, channelCount)
            activeStages = coeffsList
            preamp = snap.preampLinear
            runEq = !(abs(preamp - 1.0) < 1e-4 && activeStages.all { it.isIdentity })
        } else {
            activeStages = emptyArray()
            preamp = 1.0
            runEq = false
        }

        if (!runEq && !hasSpatialDsp) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        val inBuf = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val out = replaceOutputBuffer(remaining).order(ByteOrder.LITTLE_ENDIAN)
        val bal = snap.channelBalance.coerceIn(-1f, 1f).toDouble()
        val leftGain = (1.0 - bal).coerceIn(0.0, 1.0)
        val rightGain = (1.0 + bal).coerceIn(0.0, 1.0)
        val width = snap.stereoWidth.coerceIn(0f, 1.8f).toDouble()
        val isMono = snap.monoAudio

        if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            if (channelCount == 2 && hasSpatialDsp) {
                val frames = remaining / 8
                for (f in 0 until frames) {
                    var l = inBuf.float.toDouble()
                    var r = inBuf.float.toDouble()
                    if (runEq) {
                        l *= preamp
                        r *= preamp
                        for (b in activeStages.indices) {
                            val c = activeStages[b]
                            if (c.isIdentity) continue
                            val idxL = b * 2
                            val yl = c.b0 * l + z1[idxL]
                            z1[idxL] = c.b1 * l - c.a1 * yl + z2[idxL]
                            z2[idxL] = c.b2 * l - c.a2 * yl
                            l = yl

                            val idxR = idxL + 1
                            val yr = c.b0 * r + z1[idxR]
                            z1[idxR] = c.b1 * r - c.a1 * yr + z2[idxR]
                            z2[idxR] = c.b2 * r - c.a2 * yr
                            r = yr
                        }
                    }
                    if (isMono) {
                        val m = (l + r) * 0.5
                        l = m
                        r = m
                    } else if (abs(width - 1.0) > 0.01) {
                        val mid = (l + r) * 0.5
                        val side = (l - r) * 0.5 * width
                        l = mid + side
                        r = mid - side
                    }
                    l *= leftGain
                    r *= rightGain
                    out.putFloat(l.coerceIn(-1.0, 1.0).toFloat())
                    out.putFloat(r.coerceIn(-1.0, 1.0).toFloat())
                }
            } else {
                val totalSamples = remaining / 4
                var ch = 0
                for (i in 0 until totalSamples) {
                    var x = inBuf.float.toDouble() * preamp
                    for (b in activeStages.indices) {
                        val c = activeStages[b]
                        if (c.isIdentity) continue
                        val idx = b * channelCount + ch
                        val y = c.b0 * x + z1[idx]
                        z1[idx] = c.b1 * x - c.a1 * y + z2[idx]
                        z2[idx] = c.b2 * x - c.a2 * y
                        x = y
                    }
                    out.putFloat(x.coerceIn(-1.0, 1.0).toFloat())
                    ch++
                    if (ch >= channelCount) ch = 0
                }
            }
        } else {
            if (channelCount == 2 && hasSpatialDsp) {
                val frames = remaining / 4
                for (f in 0 until frames) {
                    var l = inBuf.short.toDouble() / 32768.0
                    var r = inBuf.short.toDouble() / 32768.0
                    if (runEq) {
                        l *= preamp
                        r *= preamp
                        for (b in activeStages.indices) {
                            val c = activeStages[b]
                            if (c.isIdentity) continue
                            val idxL = b * 2
                            val yl = c.b0 * l + z1[idxL]
                            z1[idxL] = c.b1 * l - c.a1 * yl + z2[idxL]
                            z2[idxL] = c.b2 * l - c.a2 * yl
                            l = yl

                            val idxR = idxL + 1
                            val yr = c.b0 * r + z1[idxR]
                            z1[idxR] = c.b1 * r - c.a1 * yr + z2[idxR]
                            z2[idxR] = c.b2 * r - c.a2 * yr
                            r = yr
                        }
                    }
                    if (isMono) {
                        val m = (l + r) * 0.5
                        l = m
                        r = m
                    } else if (abs(width - 1.0) > 0.01) {
                        val mid = (l + r) * 0.5
                        val side = (l - r) * 0.5 * width
                        l = mid + side
                        r = mid - side
                    }
                    l *= leftGain
                    r *= rightGain
                    out.putShort((l * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                    out.putShort((r * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort())
                }
            } else {
                val totalSamples = remaining / 2
                var ch = 0
                for (i in 0 until totalSamples) {
                    var x = (inBuf.short.toDouble() / 32768.0) * preamp
                    for (b in activeStages.indices) {
                        val c = activeStages[b]
                        if (c.isIdentity) continue
                        val idx = b * channelCount + ch
                        val y = c.b0 * x + z1[idx]
                        z1[idx] = c.b1 * x - c.a1 * y + z2[idx]
                        z2[idx] = c.b2 * x - c.a2 * y
                        x = y
                    }
                    val pcm = (x * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    out.putShort(pcm)
                    ch++
                    if (ch >= channelCount) ch = 0
                }
            }
        }

        out.flip()
    }
}
