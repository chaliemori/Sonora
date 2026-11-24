package com.example.musicplayer.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils

enum class MonetMode(val title: String, val subtitle: String) {
    COVER_AUTO("Из обложки", "Динамический акцент текущего трека"),
    SYSTEM_DYNAMIC("Системный Monet", "Цвета обоев системы (Android 12+)"),
    LAVENDER("Лаванда", "Мягкий фиолетовый Material 3"),
    EMERALD("Изумруд", "Глубокий хвойно-зелёный оттенок"),
    OCEAN("Океан", "Спокойный сине-голубой акцент"),
    SUNSET("Закат", "Тёплый терракотово-янтарный тон"),
    ROSE("Кварц", "Приглушённый малиново-розовый"),
    CUSTOM("Свой цвет", "Выбранный вручную акцент")
}

val MonetPresetSwatches: List<Pair<String, Color>> = listOf(
    "Фиолетовый" to Color(0xFF6750A4),
    "Индиго" to Color(0xFF3F51B5),
    "Небесный" to Color(0xFF0277BD),
    "Бирюза" to Color(0xFF00796B),
    "Изумруд" to Color(0xFF2E7D32),
    "Олива" to Color(0xFF689F38),
    "Янтарь" to Color(0xFFEF6C00),
    "Коралл" to Color(0xFFD84315),
    "Малина" to Color(0xFFC2185B),
    "Графит" to Color(0xFF546E7A)
)

@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean,
    amoledTheme: Boolean = false,
    monetMode: MonetMode = MonetMode.COVER_AUTO,
    coverAccent: Color = Color(0xFF6750A4),
    customSeedColor: Color = Color(0xFF6750A4),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val baseScheme: ColorScheme = when (monetMode) {
        MonetMode.SYSTEM_DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                generateSeedColorScheme(seed = Color(0xFF6750A4), darkTheme = darkTheme)
            }
        }
        MonetMode.COVER_AUTO -> generateSeedColorScheme(seed = coverAccent, darkTheme = darkTheme)
        MonetMode.LAVENDER -> generateSeedColorScheme(seed = Color(0xFF6750A4), darkTheme = darkTheme)
        MonetMode.EMERALD -> generateSeedColorScheme(seed = Color(0xFF2E7D32), darkTheme = darkTheme)
        MonetMode.OCEAN -> generateSeedColorScheme(seed = Color(0xFF0277BD), darkTheme = darkTheme)
        MonetMode.SUNSET -> generateSeedColorScheme(seed = Color(0xFFEF6C00), darkTheme = darkTheme)
        MonetMode.ROSE -> generateSeedColorScheme(seed = Color(0xFFC2185B), darkTheme = darkTheme)
        MonetMode.CUSTOM -> generateSeedColorScheme(seed = customSeedColor, darkTheme = darkTheme)
    }

    val finalScheme = if (darkTheme && amoledTheme) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0B0B0D),
            surfaceContainer = Color(0xFF121215),
            surfaceContainerHigh = Color(0xFF1A1A1E),
            surfaceContainerHighest = Color(0xFF242429)
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        content = content
    )
}

private fun generateSeedColorScheme(seed: Color, darkTheme: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.28f, 0.78f)

    fun tone(h: Float, s: Float, l: Float): Color {
        return Color(ColorUtils.HSLToColor(floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))
    }

    return if (darkTheme) {
        val primary = tone(hue, sat, 0.78f)
        val onPrimary = if (primary.luminance() > 0.45f) Color(0xFF141218) else Color.White
        val primaryContainer = tone(hue, sat * 0.85f, 0.28f)
        val onPrimaryContainer = tone(hue, sat * 0.65f, 0.92f)
        val secondary = tone(hue, sat * 0.45f, 0.76f)
        val secondaryContainer = tone(hue, sat * 0.42f, 0.24f)
        val onSecondaryContainer = tone(hue, sat * 0.35f, 0.90f)
        val tertiaryHue = (hue + 42f) % 360f
        val tertiary = tone(tertiaryHue, sat * 0.55f, 0.78f)
        val tertiaryContainer = tone(tertiaryHue, sat * 0.45f, 0.26f)

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = Color(0xFF1A191E),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            tertiaryContainer = tertiaryContainer,
            background = tone(hue, sat * 0.14f, 0.07f),
            onBackground = tone(hue, sat * 0.08f, 0.93f),
            surface = tone(hue, sat * 0.14f, 0.07f),
            onSurface = tone(hue, sat * 0.08f, 0.93f),
            surfaceVariant = tone(hue, sat * 0.18f, 0.22f),
            onSurfaceVariant = tone(hue, sat * 0.14f, 0.76f),
            surfaceContainerLowest = tone(hue, sat * 0.14f, 0.05f),
            surfaceContainerLow = tone(hue, sat * 0.15f, 0.10f),
            surfaceContainer = tone(hue, sat * 0.16f, 0.13f),
            surfaceContainerHigh = tone(hue, sat * 0.17f, 0.17f),
            surfaceContainerHighest = tone(hue, sat * 0.18f, 0.22f),
            outline = tone(hue, sat * 0.15f, 0.55f),
            outlineVariant = tone(hue, sat * 0.16f, 0.26f)
        )
    } else {
        val primary = tone(hue, sat, 0.40f)
        val onPrimary = Color.White
        val primaryContainer = tone(hue, sat * 0.72f, 0.90f)
        val onPrimaryContainer = tone(hue, sat * 0.90f, 0.16f)
        val secondary = tone(hue, sat * 0.40f, 0.42f)
        val secondaryContainer = tone(hue, sat * 0.45f, 0.91f)
        val onSecondaryContainer = tone(hue, sat * 0.55f, 0.17f)
        val tertiaryHue = (hue + 42f) % 360f
        val tertiary = tone(tertiaryHue, sat * 0.55f, 0.42f)
        val tertiaryContainer = tone(tertiaryHue, sat * 0.55f, 0.90f)

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            tertiaryContainer = tertiaryContainer,
            background = tone(hue, sat * 0.12f, 0.98f),
            onBackground = tone(hue, sat * 0.12f, 0.11f),
            surface = tone(hue, sat * 0.12f, 0.98f),
            onSurface = tone(hue, sat * 0.12f, 0.11f),
            surfaceVariant = tone(hue, sat * 0.18f, 0.90f),
            onSurfaceVariant = tone(hue, sat * 0.16f, 0.32f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = tone(hue, sat * 0.14f, 0.95f),
            surfaceContainer = tone(hue, sat * 0.16f, 0.93f),
            surfaceContainerHigh = tone(hue, sat * 0.18f, 0.90f),
            surfaceContainerHighest = tone(hue, sat * 0.20f, 0.87f),
            outline = tone(hue, sat * 0.16f, 0.48f),
            outlineVariant = tone(hue, sat * 0.16f, 0.80f)
        )
    }
}

fun extractDominantColorFromBitmap(bitmap: Bitmap?, fallback: Color): Color {
    if (bitmap == null) return fallback
    return try {
        val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        var bestScore = -1f
        var bestColor = fallback.toArgb()
        val hsl = FloatArray(3)

        for (y in 0 until scaled.height) {
            for (x in 0 until scaled.width) {
                val pixel = scaled.getPixel(x, y)
                ColorUtils.colorToHSL(pixel, hsl)
                val saturation = hsl[1]
                val lightness = hsl[2]
                if (lightness in 0.16f..0.84f) {
                    val score = saturation * 2.2f + (1f - kotlin.math.abs(lightness - 0.5f))
                    if (score > bestScore) {
                        bestScore = score
                        bestColor = pixel
                    }
                }
            }
        }
        if (scaled != bitmap) scaled.recycle()
        Color(bestColor)
    } catch (_: Exception) {
        fallback
    }
}
