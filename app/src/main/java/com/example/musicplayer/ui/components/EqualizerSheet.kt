package com.example.musicplayer.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicplayer.audio.AudioEffectsController
import com.example.musicplayer.audio.EqFilterType
import com.example.musicplayer.audio.EqUiMode
import com.example.musicplayer.audio.ParametricBand
import com.example.musicplayer.audio.ParametricEqController
import java.util.Locale
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val LOG_MIN_FREQ = log10(20f)
private val LOG_MAX_FREQ = log10(20000f)

private fun freqToNorm(freqHz: Float): Float {
    val logF = log10(freqHz.coerceIn(20f, 20000f))
    return ((logF - LOG_MIN_FREQ) / (LOG_MAX_FREQ - LOG_MIN_FREQ)).coerceIn(0f, 1f)
}

private fun normToFreq(norm: Float): Float {
    val exp = LOG_MIN_FREQ + norm.coerceIn(0f, 1f) * (LOG_MAX_FREQ - LOG_MIN_FREQ)
    return 10f.pow(exp).coerceIn(20f, 20000f)
}

private fun formatFreqLabel(freqHz: Float): String {
    return if (freqHz >= 1000f) {
        val khz = freqHz / 1000f
        if (khz >= 10f) String.format(Locale.US, "%.1f кГц", khz)
        else String.format(Locale.US, "%.2f кГц", khz)
    } else {
        "${freqHz.roundToInt()} Гц"
    }
}

private fun formatFreqShort(freqHz: Float): String {
    return if (freqHz >= 1000f) {
        val khz = freqHz / 1000f
        if (khz >= 10f) String.format(Locale.US, "%.0fk", khz)
        else String.format(Locale.US, "%.1fk", khz)
    } else {
        "${freqHz.roundToInt()}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ParametricEqController.ensureLoaded(context)
    }

    val uiMode = ParametricEqController.uiMode
    val graphicEnabled = AudioEffectsController.eqEnabled
    val peqEnabled = ParametricEqController.enabled
    val bands = AudioEffectsController.bands
    val minMb = AudioEffectsController.minLevelMb
    val maxMb = AudioEffectsController.maxLevelMb
    val selectedPreset = AudioEffectsController.selectedPresetName
    val bassBoost = AudioEffectsController.bassBoostStrength
    val isSupported = AudioEffectsController.isSupported

    val peqBands = ParametricEqController.bands
    val peqPreamp = ParametricEqController.preampDb
    val peqPresetName = ParametricEqController.selectedPresetName
    val userPresets = ParametricEqController.userPresets
    var selectedBandIndex by remember { mutableIntStateOf(0) }
    val safeBandIndex = selectedBandIndex.coerceIn(0, (peqBands.size - 1).coerceAtLeast(0))

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    val activeSwitchChecked = if (uiMode == EqUiMode.PARAMETRIC) peqEnabled else graphicEnabled
    val subtitleText = if (uiMode == EqUiMode.PARAMETRIC) {
        if (peqEnabled) "Параметрический (${peqBands.size} полос) • $peqPresetName" else "Параметрический (${peqBands.size} полос) • Выключен"
    } else {
        if (isSupported) {
            if (graphicEnabled) "Графический • $selectedPreset" else "Графический • Выключен"
        } else {
            "Включите трек для графического EQ"
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Сохранить пресет EQ") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    singleLine = true,
                    label = { Text("Название пресета") },
                    placeholder = { Text("Например: Мои наушники") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetNameInput.isNotBlank()) {
                            ParametricEqController.saveUserPreset(context, presetNameInput)
                            presetNameInput = ""
                            showSavePresetDialog = false
                            Toast.makeText(context, "Пресет сохранён", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Эквалайзер",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Switch(
                    checked = activeSwitchChecked,
                    onCheckedChange = { checked ->
                        if (uiMode == EqUiMode.PARAMETRIC) {
                            ParametricEqController.setEnabled(context, checked)
                        } else {
                            if (checked && ParametricEqController.enabled) {
                                ParametricEqController.setEnabled(context, false)
                            }
                            AudioEffectsController.setEqEnabled(context, checked)
                        }
                    }
                )
            }

            // Segmented Mode Toggle: Графический / Параметрический
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        EqUiMode.GRAPHIC to "Графический (5 полос)",
                        EqUiMode.PARAMETRIC to "Параметрический (до 10)"
                    ).forEach { (mode, title) ->
                        val selected = uiMode == mode
                        val tabBgColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            animationSpec = tween(200),
                            label = "eqModeTabBg"
                        )
                        val tabTextColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "eqModeTabText"
                        )
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { ParametricEqController.setUiMode(context, mode) },
                            shape = CircleShape,
                            color = tabBgColor
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = tabTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = uiMode,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "eqUiModeContent"
            ) { animatedUiMode ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                if (animatedUiMode == EqUiMode.PARAMETRIC) {
                // Parametric EQ Presets (User + Built-in) + Save + Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        userPresets.forEach { preset ->
                            FilterChip(
                                selected = peqPresetName == preset.name && peqEnabled,
                                onClick = {
                                    ParametricEqController.applyPreset(context, preset)
                                    selectedBandIndex = 0
                                },
                                label = {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Удалить пресет",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                ParametricEqController.deleteUserPreset(context, preset.name)
                                            }
                                    )
                                }
                            )
                        }
                        ParametricEqController.builtInPresets.forEach { preset ->
                            FilterChip(
                                selected = peqPresetName == preset.name && peqEnabled,
                                onClick = {
                                    ParametricEqController.applyPreset(context, preset)
                                    selectedBandIndex = 0
                                },
                                label = {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            presetNameInput = ""
                            showSavePresetDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkAdd,
                            contentDescription = "Сохранить свой пресет"
                        )
                    }
                    IconButton(
                        onClick = {
                            ParametricEqController.resetFlat(context)
                            selectedBandIndex = 0
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Сбросить кривую"
                        )
                    }
                }

                // Live Frequency Response Curve + Individual Band Overlay
                ParametricCurveCard(
                    bands = peqBands,
                    preampDb = peqPreamp,
                    enabled = peqEnabled,
                    selectedBandIndex = safeBandIndex,
                    onSelectBand = { selectedBandIndex = it },
                    onDragBand = { idx, freqHz, gainDb ->
                        selectedBandIndex = idx
                        ParametricEqController.updateBand(
                            context = context,
                            bandIndex = idx,
                            freqHz = freqHz,
                            gainDb = gainDb
                        )
                    }
                )

                // Band selector strip + Add Band chip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    peqBands.forEach { band ->
                        val isSelected = band.index == safeBandIndex
                        val badgeValue = when {
                            !band.enabled -> "Выкл"
                            !band.filterType.usesGain -> band.filterType.shortLabel
                            band.gainDb >= 0f -> String.format(Locale.US, "+%.1f дБ", band.gainDb)
                            else -> String.format(Locale.US, "%.1f дБ", band.gainDb)
                        }
                        val bandChipScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.04f else 1f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 460f),
                            label = "peqBandScale"
                        )
                        val bandBorderWidth by animateDpAsState(
                            targetValue = if (isSelected) 1.5.dp else 1.dp,
                            animationSpec = tween(180),
                            label = "peqBandBorderW"
                        )
                        val bandBorderColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            animationSpec = tween(200),
                            label = "peqBandBorderColor"
                        )
                        val bandBgColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            animationSpec = tween(200),
                            label = "peqBandBgColor"
                        )
                        val bandBadgeColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "peqBandBadgeColor"
                        )
                        Surface(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = bandChipScale
                                    scaleY = bandChipScale
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = bandBorderWidth,
                                    color = bandBorderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedBandIndex = band.index },
                            shape = RoundedCornerShape(14.dp),
                            color = bandBgColor
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "B${band.index + 1} • ${formatFreqShort(band.freqHz)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (band.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = badgeValue,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = bandBadgeColor
                                )
                            }
                        }
                    }

                    if (peqBands.size < ParametricEqController.MAX_BANDS) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedBandIndex = ParametricEqController.addBand(context)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Добавить полосу",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Полоса",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Selected Band Fine Controls
                val activeBand = peqBands.getOrNull(safeBandIndex) ?: peqBands.first()
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Полоса ${activeBand.index + 1} • ${activeBand.filterType.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${formatFreqLabel(activeBand.freqHz)}  •  Q ${String.format(Locale.US, "%.2f", activeBand.q)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (peqBands.size > ParametricEqController.MIN_BANDS) {
                                    IconButton(
                                        onClick = {
                                            selectedBandIndex = ParametricEqController.removeBand(context, activeBand.index)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Удалить полосу",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Switch(
                                    checked = activeBand.enabled,
                                    onCheckedChange = { bandOn ->
                                        ParametricEqController.updateBand(
                                            context = context,
                                            bandIndex = activeBand.index,
                                            bandEnabled = bandOn
                                        )
                                    }
                                )
                            }
                        }

                        // Filter type chips (Peak, LowShelf, HighShelf, HPF, LPF, Notch)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EqFilterType.entries.forEach { fType ->
                                FilterChip(
                                    selected = activeBand.filterType == fType,
                                    onClick = {
                                        ParametricEqController.updateBand(
                                            context = context,
                                            bandIndex = activeBand.index,
                                            filterType = fType
                                        )
                                    },
                                    label = { Text(fType.shortLabel, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        // Frequency slider (logarithmic 20 Hz .. 20 kHz)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Частота", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = formatFreqLabel(activeBand.freqHz),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = freqToNorm(activeBand.freqHz),
                                onValueChange = { norm ->
                                    ParametricEqController.updateBand(
                                        context = context,
                                        bandIndex = activeBand.index,
                                        freqHz = normToFreq(norm)
                                    )
                                },
                                valueRange = 0f..1f
                            )
                        }

                        // Gain slider (shown for Peak / LowShelf / HighShelf)
                        if (activeBand.filterType.usesGain) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Усиление", style = MaterialTheme.typography.labelMedium)
                                    val gText = if (activeBand.gainDb >= 0f) {
                                        String.format(Locale.US, "+%.1f дБ", activeBand.gainDb)
                                    } else {
                                        String.format(Locale.US, "%.1f дБ", activeBand.gainDb)
                                    }
                                    Text(
                                        text = gText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = activeBand.gainDb,
                                    onValueChange = { g ->
                                        val snapped = (g * 2f).roundToInt() / 2f
                                        ParametricEqController.updateBand(
                                            context = context,
                                            bandIndex = activeBand.index,
                                            gainDb = snapped
                                        )
                                    },
                                    valueRange = -12f..12f
                                )
                            }
                        }

                        // Q-factor slider (0.40 .. 8.00)
                        val qMinLog = ln(0.4f)
                        val qMaxLog = ln(8.0f)
                        val qNorm = ((ln(activeBand.q.coerceIn(0.4f, 8.0f)) - qMinLog) / (qMaxLog - qMinLog)).coerceIn(0f, 1f)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Добротность (Q)", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = String.format(Locale.US, "%.2f", activeBand.q),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = qNorm,
                                onValueChange = { norm ->
                                    val qVal = kotlin.math.exp(qMinLog + norm * (qMaxLog - qMinLog)).coerceIn(0.4f, 8.0f)
                                    val roundedQ = (qVal * 20f).roundToInt() / 20f
                                    ParametricEqController.updateBand(
                                        context = context,
                                        bandIndex = activeBand.index,
                                        q = roundedQ
                                    )
                                },
                                valueRange = 0f..1f
                            )
                        }
                    }
                }

                // Pre-amp headroom card + Auto-headroom button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Преамп (запас громкости)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val pText = if (peqPreamp >= 0f) {
                                    String.format(Locale.US, "+%.1f дБ", peqPreamp)
                                } else {
                                    String.format(Locale.US, "%.1f дБ", peqPreamp)
                                }
                                Text(
                                    text = pText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { ParametricEqController.autoAdjustPreampHeadroom(context) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "Авто-запас",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Slider(
                            value = peqPreamp,
                            onValueChange = { v ->
                                val snapped = (v * 2f).roundToInt() / 2f
                                ParametricEqController.setPreamp(context, snapped)
                            },
                            valueRange = -12f..6f
                        )
                    }
                }
            } else {
                // Classic 5-band Graphic Equalizer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioEffectsController.builtInPresets.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset.name && graphicEnabled,
                            onClick = {
                                if (ParametricEqController.enabled) {
                                    ParametricEqController.setEnabled(context, false)
                                }
                                AudioEffectsController.applyPreset(context, preset)
                            },
                            label = {
                                Text(
                                    text = preset.name,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                if (bands.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bands.forEach { band ->
                                val freqLabel = if (band.centerFreqHz >= 1000) {
                                    val khz = band.centerFreqHz / 1000.0
                                    if (band.centerFreqHz % 1000 == 0) "${band.centerFreqHz / 1000}k" else String.format("%.1fk", khz)
                                } else {
                                    "${band.centerFreqHz}"
                                }
                                val dbVal = (band.levelMb / 100f).roundToInt()
                                val dbText = if (dbVal > 0) "+$dbVal" else "$dbVal"
                                val fraction = ((band.levelMb - minMb).toFloat() / (maxMb - minMb).coerceAtLeast(1).toFloat())
                                    .coerceIn(0f, 1f)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = dbText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (graphicEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )

                                    VerticalEqSlider(
                                        fraction = fraction,
                                        enabled = graphicEnabled,
                                        onFractionChange = { newFrac ->
                                            if (ParametricEqController.enabled) {
                                                ParametricEqController.setEnabled(context, false)
                                            }
                                            if (!graphicEnabled) AudioEffectsController.setEqEnabled(context, true)
                                            val newMb = (minMb + newFrac * (maxMb - minMb)).roundToInt()
                                            AudioEffectsController.setBandLevel(context, band.index, newMb)
                                        }
                                    )

                                    Text(
                                        text = freqLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Bass Boost
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Усиление баса (Bass Boost)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${(bassBoost / 10)}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { AudioEffectsController.setBassBoost(context, it.roundToInt()) },
                            valueRange = 0f..1000f
                        )
                    }
                }
            }
                }
            }

            // Spatial & Channel DSP Card: Loudness, Stereo Balance, Stereo Width (M/S), Mono
            val dspLoudnessMb = AudioEffectsController.loudnessGainMb
            val dspBalance = ParametricEqController.channelBalance
            val dspWidth = ParametricEqController.stereoWidth
            val dspMono = ParametricEqController.monoAudio
            val hasCustomStage = dspLoudnessMb > 0 || kotlin.math.abs(dspBalance) > 0.02f ||
                kotlin.math.abs(dspWidth - 1f) > 0.02f || dspMono

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Сцена, баланс и усиление",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Работает в реальном времени поверх звука плеера",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (hasCustomStage) {
                            TextButton(
                                onClick = {
                                    AudioEffectsController.setLoudnessGain(context, 0)
                                    ParametricEqController.resetAudioStage(context)
                                }
                            ) {
                                Text("Сброс", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Loudness Enhancer (0 .. +10 dB)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Усиление громкости", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(
                            text = String.format(Locale.US, "+%.1f дБ", dspLoudnessMb / 100f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = dspLoudnessMb.toFloat(),
                        onValueChange = { AudioEffectsController.setLoudnessGain(context, it.roundToInt()) },
                        valueRange = 0f..1000f
                    )

                    // Stereo Width (M/S)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ширина стерео-сцены", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (dspMono) "Моно" else "${(dspWidth * 100f).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = dspWidth,
                        enabled = !dspMono,
                        onValueChange = {
                            val snapped = ((it * 20f).roundToInt() / 20f).let { v ->
                                if (kotlin.math.abs(v - 1f) < 0.06f) 1f else v
                            }
                            ParametricEqController.setStereoWidth(context, snapped)
                        },
                        valueRange = 0.2f..1.8f
                    )

                    // Channel Balance (L - Center - R)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Баланс каналов (L / R)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        val balPct = (dspBalance * 100f).roundToInt()
                        val balLabel = when {
                            balPct < -2 -> "L ${-balPct}%"
                            balPct > 2 -> "R $balPct%"
                            else -> "Центр"
                        }
                        Text(
                            text = balLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = dspBalance,
                        onValueChange = {
                            val snapped = if (kotlin.math.abs(it) < 0.06f) 0f else it
                            ParametricEqController.setChannelBalance(context, snapped)
                        },
                        valueRange = -1f..1f
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { ParametricEqController.setMonoAudio(context, !dspMono) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Моно-режим (L + R)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Смешать каналы в один для одного наушника",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dspMono,
                            onCheckedChange = { ParametricEqController.setMonoAudio(context, it) }
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = {
                    val opened = AudioEffectsController.openSystemEqualizer(context)
                    if (!opened) {
                        Toast.makeText(
                            context,
                            "Системный эквалайзер на устройстве не найден",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Системный / внешний эквалайзер")
            }
        }
    }
}

@Composable
private fun ParametricCurveCard(
    bands: List<ParametricBand>,
    preampDb: Float,
    enabled: Boolean,
    selectedBandIndex: Int,
    onSelectBand: (Int) -> Unit,
    onDragBand: (bandIndex: Int, freqHz: Float, gainDb: Float) -> Unit
) {
    val curveColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(220),
        label = "peqCurveColor"
    )
    val bandOverlayColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.tertiary.copy(alpha = if (enabled) 0.5f else 0.25f),
        animationSpec = tween(220),
        label = "peqBandOverlayColor"
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val zeroLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer

    val sampledCurve = remember(bands, preampDb) {
        val count = 84
        FloatArray(count) { i ->
            val norm = i / (count - 1).toFloat()
            val freq = normToFreq(norm)
            ParametricEqController.computeMagnitudeDbAt(
                freqHz = freq,
                bandList = bands,
                preamp = preampDb
            )
        }
    }

    val selectedBand = bands.getOrNull(selectedBandIndex)
    val selectedBandCurve = remember(selectedBand) {
        if (selectedBand == null) FloatArray(0)
        else {
            val count = 84
            val singleList = listOf(selectedBand)
            FloatArray(count) { i ->
                val norm = i / (count - 1).toFloat()
                val freq = normToFreq(norm)
                ParametricEqController.computeMagnitudeDbAt(
                    freqHz = freq,
                    bandList = singleList,
                    preamp = 0f
                )
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = surfaceColor
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .pointerInput(bands) {
                            detectTapGestures { offset ->
                                val w = size.width.coerceAtLeast(1).toFloat()
                                val h = size.height.coerceAtLeast(1).toFloat()
                                val nearest = bands.minByOrNull { band ->
                                    val bx = freqToNorm(band.freqHz) * w
                                    val bandYGain = if (band.filterType.usesGain) band.gainDb else 0f
                                    val by = (0.5f - (bandYGain / 24f)) * h
                                    val dx = offset.x - bx
                                    val dy = offset.y - by
                                    dx * dx + dy * dy
                                }
                                if (nearest != null) {
                                    onSelectBand(nearest.index)
                                }
                            }
                        }
                        .pointerInput(bands, selectedBandIndex) {
                            var draggingIdx = selectedBandIndex
                            detectDragGestures(
                                onDragStart = { start ->
                                    val w = size.width.coerceAtLeast(1).toFloat()
                                    val h = size.height.coerceAtLeast(1).toFloat()
                                    val nearest = bands.minByOrNull { band ->
                                        val bx = freqToNorm(band.freqHz) * w
                                        val bandYGain = if (band.filterType.usesGain) band.gainDb else 0f
                                        val by = (0.5f - (bandYGain / 24f)) * h
                                        val dx = start.x - bx
                                        val dy = start.y - by
                                        dx * dx + dy * dy
                                    }
                                    if (nearest != null) {
                                        draggingIdx = nearest.index
                                        onSelectBand(draggingIdx)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val w = size.width.coerceAtLeast(1).toFloat()
                                    val h = size.height.coerceAtLeast(1).toFloat()
                                    val normX = (change.position.x / w).coerceIn(0f, 1f)
                                    val gain = ((0.5f - (change.position.y / h)) * 24f).coerceIn(-12f, 12f)
                                    val snappedGain = (gain * 2f).roundToInt() / 2f
                                    val targetBand = bands.getOrNull(draggingIdx)
                                    onDragBand(
                                        draggingIdx,
                                        normToFreq(normX),
                                        if (targetBand?.filterType?.usesGain == false) 0f else snappedGain
                                    )
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    listOf(60f, 250f, 1000f, 4000f, 16000f).forEach { freq ->
                        val x = freqToNorm(freq) * w
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    drawLine(
                        color = gridColor,
                        start = Offset(0f, h * 0.25f),
                        end = Offset(w, h * 0.25f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, h * 0.75f),
                        end = Offset(w, h * 0.75f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = zeroLineColor,
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1.2.dp.toPx()
                    )

                    // Individual selected band response overlay
                    if (selectedBandCurve.isNotEmpty()) {
                        val bandPath = Path()
                        val bLast = (selectedBandCurve.size - 1).coerceAtLeast(1)
                        selectedBandCurve.forEachIndexed { i, db ->
                            val x = (i / bLast.toFloat()) * w
                            val y = (0.5f - (db.coerceIn(-15f, 15f) / 24f)) * h
                            if (i == 0) bandPath.moveTo(x, y) else bandPath.lineTo(x, y)
                        }
                        drawPath(
                            path = bandPath,
                            color = bandOverlayColor,
                            style = Stroke(
                                width = 1.6.dp.toPx(),
                                pathEffect = dashEffect,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // Combined EQ response curve
                    val strokePath = Path()
                    val fillPath = Path()
                    val lastIdx = (sampledCurve.size - 1).coerceAtLeast(1)
                    sampledCurve.forEachIndexed { i, db ->
                        val x = (i / lastIdx.toFloat()) * w
                        val y = (0.5f - (db.coerceIn(-15f, 15f) / 24f)) * h
                        if (i == 0) {
                            strokePath.moveTo(x, y)
                            fillPath.moveTo(x, midY)
                            fillPath.lineTo(x, y)
                        } else {
                            strokePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }
                    fillPath.lineTo(w, midY)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                curveColor.copy(alpha = 0.28f),
                                curveColor.copy(alpha = 0.04f)
                            )
                        )
                    )
                    drawPath(
                        path = strokePath,
                        color = curveColor,
                        style = Stroke(
                            width = 2.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    bands.forEach { band ->
                        val bx = freqToNorm(band.freqHz) * w
                        val bandYGain = if (band.filterType.usesGain) band.gainDb else 0f
                        val by = (0.5f - (bandYGain / 24f)) * h
                        val isSel = band.index == selectedBandIndex
                        val dotColor = when {
                            !band.enabled -> gridColor
                            isSel -> curveColor
                            else -> secondaryColor
                        }
                        if (isSel) {
                            drawCircle(
                                color = dotColor.copy(alpha = 0.26f),
                                radius = 12.dp.toPx(),
                                center = Offset(bx, by)
                            )
                        }
                        drawCircle(
                            color = surfaceColor,
                            radius = if (isSel) 7.dp.toPx() else 5.5.dp.toPx(),
                            center = Offset(bx, by)
                        )
                        drawCircle(
                            color = dotColor,
                            radius = if (isSel) 5.5.dp.toPx() else 4.dp.toPx(),
                            center = Offset(bx, by)
                        )
                    }
                }

                // Scale markers (+12, 0, -12 dB)
                Column(
                    modifier = Modifier
                        .height(172.dp)
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("+12", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text("0 дБ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text("-12", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("20 Гц", "250 Гц", "1 кГц", "4 кГц", "20 кГц").forEach { lbl ->
                    Text(
                        text = lbl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalEqSlider(
    fraction: Float,
    enabled: Boolean,
    onFractionChange: (Float) -> Unit
) {
    val activeColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        },
        animationSpec = tween(220),
        label = "eqBarColor"
    )
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 750f),
        label = "eqBarFraction"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(
        modifier = Modifier
            .width(36.dp)
            .height(140.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newFrac = 1f - (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onFractionChange(newFrac)
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val newFrac = 1f - (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onFractionChange(newFrac)
                }
            }
    ) {
        val trackW = 6.dp.toPx()
        val centerX = size.width / 2f
        val h = size.height
        val thumbY = h * (1f - animatedFraction)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(centerX - trackW / 2f, 0f),
            size = Size(trackW, h),
            cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
        )

        val midY = h / 2f
        val topY = minOf(midY, thumbY)
        val barH = kotlin.math.abs(thumbY - midY).coerceAtLeast(2.dp.toPx())
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(centerX - trackW / 2f, topY),
            size = Size(trackW, barH),
            cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
        )

        drawCircle(
            color = activeColor,
            radius = 8.dp.toPx(),
            center = Offset(centerX, thumbY.coerceIn(8.dp.toPx(), h - 8.dp.toPx()))
        )
    }
}
