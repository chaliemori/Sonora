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

enum class MonetMode(val title: String, val subtitle: String, val previewColor: Color) {
    COVER_AUTO("Из обложки", "Динамический акцент текущего трека", Color(0xFF6750A4)),
    SYSTEM_DYNAMIC("Системный Monet", "Цвета обоев системы (Android 12+)", Color(0xFF4C662B)),
    LAVENDER("Лаванда", "Мягкий фиолетовый акцент", Color(0xFF6750A4)),
    EMERALD("Изумруд", "Глубокий хвойно-зелёный оттенок", Color(0xFF2E7D32)),
    OCEAN("Океан", "Спокойный сине-голубой акцент", Color(0xFF0277BD)),
    SUNSET("Закат", "Тёплый терракотово-янтарный тон", Color(0xFFEF6C00)),
    ROSE("Кварц", "Приглушённый малиново-розовый", Color(0xFFC2185B)),
    CUSTOM("Свой цвет", "Палитра, выбранная вручную", Color(0xFF3F51B5))
}

enum class PaletteStyle(
    val title: String,
    val description: String,
    val satMultiplier: Float,
    val bgSatMultiplier: Float,
    val tertiaryHueShift: Float
) {
    TONAL_SPOT("Tonal Spot", "Balanced and calm.", 1.0f, 1.0f, 42f),
    VIBRANT("Vibrant", "Rich and saturated album tones.", 1.32f, 1.25f, 65f),
    EXPRESSIVE("Expressive", "Bold secondary and playful accents.", 1.18f, 1.15f, 135f),
    NEUTRAL("Neutral", "Soft, muted low-chroma surfaces.", 0.58f, 0.50f, 30f),
    MONOCHROME("Monochrome", "Pure grayscale without color tint.", 0.0f, 0.0f, 0f)
}

enum class NowPlayingStyle(val title: String, val subtitle: String) {
    FULL_BLEED("На весь экран", "Полноэкранная обложка с блюром и переходом"),
    CARD("Карточка (классика)", "Квадратная обложка с кнопками снизу"),
    EXPRESSIVE("Expressive (новый)", "Обложка с инфо внутри, ±10с и нижними плашками")
}

enum class LyricsStyle(val title: String, val subtitle: String) {
    APPLE_MUSIC("Fluid", "Крупный живой текст с пословной подсветкой"),
    CLASSIC("Классический", "Обычное окно текста вместо обложки")
}

val MonetPresetSwatches: List<Pair<String, Color>> = listOf(
    "Фиолетовый" to Color(0xFF6750A4),
    "Королевский синий" to Color(0xFF2962FF),
    "Индиго" to Color(0xFF3F51B5),
    "Небесный" to Color(0xFF0288D1),
    "Бирюза" to Color(0xFF00897B),
    "Мята" to Color(0xFF00BFA5),
    "Изумруд" to Color(0xFF2E7D32),
    "Олива" to Color(0xFF689F38),
    "Золото" to Color(0xFFF9A825),
    "Янтарь" to Color(0xFFEF6C00),
    "Коралл" to Color(0xFFD84315),
    "Рубин" to Color(0xFFD32F2F),
    "Малина" to Color(0xFFC2185B),
    "Сакура" to Color(0xFFE91E63),
    "Слива" to Color(0xFF7B1FA2),
    "Графит" to Color(0xFF546E7A)
)

@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean,
    amoledTheme: Boolean = false,
    monetMode: MonetMode = MonetMode.COVER_AUTO,
    paletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
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
                generateSeedColorScheme(seed = Color(0xFF6750A4), darkTheme = darkTheme, style = paletteStyle)
            }
        }
        MonetMode.COVER_AUTO -> generateSeedColorScheme(seed = coverAccent, darkTheme = darkTheme, style = paletteStyle)
        MonetMode.LAVENDER -> generateSeedColorScheme(seed = Color(0xFF6750A4), darkTheme = darkTheme, style = paletteStyle)
        MonetMode.EMERALD -> generateSeedColorScheme(seed = Color(0xFF2E7D32), darkTheme = darkTheme, style = paletteStyle)
        MonetMode.OCEAN -> generateSeedColorScheme(seed = Color(0xFF0277BD), darkTheme = darkTheme, style = paletteStyle)
        MonetMode.SUNSET -> generateSeedColorScheme(seed = Color(0xFFEF6C00), darkTheme = darkTheme, style = paletteStyle)
        MonetMode.ROSE -> generateSeedColorScheme(seed = Color(0xFFC2185B), darkTheme = darkTheme, style = paletteStyle)
        MonetMode.CUSTOM -> generateSeedColorScheme(seed = customSeedColor, darkTheme = darkTheme, style = paletteStyle)
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

fun generateSeedColorScheme(
    seed: Color,
    darkTheme: Boolean,
    style: PaletteStyle = PaletteStyle.TONAL_SPOT
): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val hue = hsl[0]
    val baseSat = hsl[1].coerceIn(0.28f, 0.78f)
    val sat = if (style == PaletteStyle.MONOCHROME) 0f else (baseSat * style.satMultiplier).coerceIn(0.12f, 0.92f)
    val bgSat = if (style == PaletteStyle.MONOCHROME) 0f else (baseSat * style.bgSatMultiplier).coerceIn(0.05f, 0.85f)
    val secHue = if (style == PaletteStyle.EXPRESSIVE) (hue + 28f) % 360f else hue
    val tertiaryHue = (hue + style.tertiaryHueShift) % 360f

    fun tone(h: Float, s: Float, l: Float): Color {
        val finalS = if (style == PaletteStyle.MONOCHROME) 0f else s.coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(floatArrayOf(h, finalS, l.coerceIn(0f, 1f))))
    }

    return if (darkTheme) {
        val primary = tone(hue, sat, 0.78f)
        val onPrimary = if (primary.luminance() > 0.45f) Color(0xFF141218) else Color.White
        val primaryContainer = tone(hue, sat * 0.85f, 0.28f)
        val onPrimaryContainer = tone(hue, sat * 0.65f, 0.92f)
        val secondary = tone(secHue, sat * 0.52f, 0.76f)
        val secondaryContainer = tone(secHue, sat * 0.45f, 0.24f)
        val onSecondaryContainer = tone(secHue, sat * 0.35f, 0.90f)
        val tertiary = tone(tertiaryHue, sat * 0.65f, 0.80f)
        val tertiaryContainer = tone(tertiaryHue, sat * 0.50f, 0.28f)

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
            background = tone(hue, bgSat * 0.14f, 0.07f),
            onBackground = tone(hue, bgSat * 0.08f, 0.93f),
            surface = tone(hue, bgSat * 0.14f, 0.07f),
            onSurface = tone(hue, bgSat * 0.08f, 0.93f),
            surfaceVariant = tone(hue, bgSat * 0.18f, 0.22f),
            onSurfaceVariant = tone(hue, bgSat * 0.14f, 0.76f),
            surfaceContainerLowest = tone(hue, bgSat * 0.14f, 0.05f),
            surfaceContainerLow = tone(hue, bgSat * 0.15f, 0.10f),
            surfaceContainer = tone(hue, bgSat * 0.16f, 0.13f),
            surfaceContainerHigh = tone(hue, bgSat * 0.17f, 0.17f),
            surfaceContainerHighest = tone(hue, bgSat * 0.18f, 0.22f),
            outline = tone(hue, bgSat * 0.15f, 0.55f),
            outlineVariant = tone(hue, bgSat * 0.16f, 0.26f)
        )
    } else {
        val primary = tone(hue, sat, 0.40f)
        val onPrimary = Color.White
        val primaryContainer = tone(hue, sat * 0.72f, 0.90f)
        val onPrimaryContainer = tone(hue, sat * 0.90f, 0.16f)
        val secondary = tone(secHue, sat * 0.45f, 0.42f)
        val secondaryContainer = tone(secHue, sat * 0.48f, 0.91f)
        val onSecondaryContainer = tone(secHue, sat * 0.55f, 0.17f)
        val tertiary = tone(tertiaryHue, sat * 0.60f, 0.42f)
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
            background = tone(hue, bgSat * 0.12f, 0.98f),
            onBackground = tone(hue, bgSat * 0.12f, 0.11f),
            surface = tone(hue, bgSat * 0.12f, 0.98f),
            onSurface = tone(hue, bgSat * 0.12f, 0.11f),
            surfaceVariant = tone(hue, bgSat * 0.18f, 0.90f),
            onSurfaceVariant = tone(hue, bgSat * 0.16f, 0.32f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = tone(hue, bgSat * 0.14f, 0.95f),
            surfaceContainer = tone(hue, bgSat * 0.16f, 0.93f),
            surfaceContainerHigh = tone(hue, bgSat * 0.18f, 0.90f),
            surfaceContainerHighest = tone(hue, bgSat * 0.20f, 0.87f),
            outline = tone(hue, bgSat * 0.16f, 0.48f),
            outlineVariant = tone(hue, bgSat * 0.16f, 0.80f)
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
