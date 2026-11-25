package com.example.musicplayer.ui.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicplayer.audio.AudioEffectsController
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val enabled = AudioEffectsController.eqEnabled
    val bands = AudioEffectsController.bands
    val minMb = AudioEffectsController.minLevelMb
    val maxMb = AudioEffectsController.maxLevelMb
    val selectedPreset = AudioEffectsController.selectedPresetName
    val bassBoost = AudioEffectsController.bassBoostStrength
    val isSupported = AudioEffectsController.isSupported

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            text = if (isSupported) {
                                if (enabled) "Активен • $selectedPreset" else "Выключен"
                            } else {
                                "Включите трек для активации"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { AudioEffectsController.setEqEnabled(context, it) }
                )
            }

            // Пресеты
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioEffectsController.builtInPresets.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset.name && enabled,
                        onClick = { AudioEffectsController.applyPreset(context, preset) },
                        label = {
                            Text(
                                text = preset.name,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Полосы частот
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
                                    color = if (enabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )

                                VerticalEqSlider(
                                    fraction = fraction,
                                    enabled = enabled,
                                    onFractionChange = { newFrac ->
                                        if (!enabled) AudioEffectsController.setEqEnabled(context, true)
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
        label = "eqBarColor"
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
        val thumbY = h * (1f - fraction.coerceIn(0f, 1f))

        // Фоновая дорожка
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(centerX - trackW / 2f, 0f),
            size = Size(trackW, h),
            cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
        )

        // Линия от центра (0 dB) к ползунку
        val midY = h / 2f
        val topY = minOf(midY, thumbY)
        val barH = kotlin.math.abs(thumbY - midY).coerceAtLeast(2.dp.toPx())
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(centerX - trackW / 2f, topY),
            size = Size(trackW, barH),
            cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
        )

        // Ползунок
        drawCircle(
            color = activeColor,
            radius = 8.dp.toPx(),
            center = Offset(centerX, thumbY.coerceIn(8.dp.toPx(), h - 8.dp.toPx()))
        )
    }
}
