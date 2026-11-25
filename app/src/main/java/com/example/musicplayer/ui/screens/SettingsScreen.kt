package com.example.musicplayer.ui.screens

import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicplayer.audio.AudioEffectsController
import com.example.musicplayer.data.LocalTrack
import com.example.musicplayer.ui.components.EqualizerBottomSheet
import com.example.musicplayer.ui.theme.LyricsStyle
import com.example.musicplayer.ui.theme.MonetMode
import com.example.musicplayer.ui.theme.MonetPresetSwatches
import com.example.musicplayer.ui.theme.NowPlayingStyle

private enum class SettingsSection(val title: String) {
    MAIN("Настройки"),
    APPEARANCE("Темы и палитры"),
    PLAYER_STYLE("Экран «Сейчас играет»"),
    SOUND("Звук, эквалайзер и кроссфейд"),
    FOLDERS("Папки с музыкой"),
    LIBRARY("Библиотека и фильтры"),
    SYSTEM("Интерфейс и память")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    darkTheme: Boolean,
    amoledTheme: Boolean,
    playerStyle: NowPlayingStyle = NowPlayingStyle.CARD,
    lyricsStyle: LyricsStyle = LyricsStyle.APPLE_MUSIC,
    crossfadeSec: Int = 0,
    landscapeNavOnRight: Boolean,
    hideDuplicates: Boolean = true,
    ignoreJunkFolders: Boolean = true,
    autoRefreshLibrary: Boolean = true,
    minDurationSec: Int = 15,
    monetMode: MonetMode,
    customSeedColor: Color,
    tracksCount: Int,
    coverCacheSizeMb: String,
    selectedFolderUris: List<Uri> = emptyList(),
    folderGroups: Map<String, List<LocalTrack>> = emptyMap(),
    bottomPaddingDp: Int,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleAmoledTheme: (Boolean) -> Unit,
    onSelectPlayerStyle: (NowPlayingStyle) -> Unit = {},
    onSelectLyricsStyle: (LyricsStyle) -> Unit = {},
    onSelectCrossfadeSec: (Int) -> Unit = {},
    onToggleLandscapeNavOnRight: (Boolean) -> Unit,
    onToggleHideDuplicates: (Boolean) -> Unit = {},
    onToggleIgnoreJunkFolders: (Boolean) -> Unit = {},
    onToggleAutoRefreshLibrary: (Boolean) -> Unit = {},
    onSelectMinDurationSec: (Int) -> Unit = {},
    onSelectMonetMode: (MonetMode) -> Unit,
    onSelectCustomColor: (Color) -> Unit,
    onAddFolderClick: () -> Unit = {},
    onRemoveFolderClick: (Uri) -> Unit = {},
    onOpenFolderTracks: (String, List<LocalTrack>) -> Unit = { _, _ -> },
    onRefreshLibrary: () -> Unit,
    onClearCoverCache: () -> Unit
) {
    var currentSection by remember { mutableStateOf(SettingsSection.MAIN) }
    var showEqSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = currentSection != SettingsSection.MAIN) {
        currentSection = SettingsSection.MAIN
    }

    val cardShape = RoundedCornerShape(24.dp)
    val cardColor = MaterialTheme.colorScheme.surfaceContainerLow

    AnimatedContent(
        targetState = currentSection,
        transitionSpec = {
            val forward = targetState != SettingsSection.MAIN
            val dir = if (forward) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                initialOffsetX = { (it * 0.18f * dir).toInt() }
            ) + fadeIn(tween(200))) togetherWith (slideOutHorizontally(
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                targetOffsetX = { (-it * 0.12f * dir).toInt() }
            ) + fadeOut(tween(150)))
        },
        label = "settingsSubMenu"
    ) { section ->
        Column(modifier = Modifier.fillMaxSize()) {
            if (section != SettingsSection.MAIN) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentSection = SettingsSection.MAIN }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 6.dp,
                    bottom = bottomPaddingDp.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (section) {
                    SettingsSection.MAIN -> {
                        item(key = "menu_appearance") {
                            SettingsCategoryCard(
                                title = "Темы и палитры",
                                subtitle = "${monetMode.title} • ${if (darkTheme) if (amoledTheme) "AMOLED" else "Тёмная" else "Светлая"}",
                                icon = Icons.Rounded.Palette,
                                badgeColor = Color(0xFF7C4DFF),
                                onClick = { currentSection = SettingsSection.APPEARANCE }
                            )
                        }
                        item(key = "menu_player_style") {
                            SettingsCategoryCard(
                                title = "Экран «Сейчас играет»",
                                subtitle = "${playerStyle.title} • Текст: ${lyricsStyle.title}",
                                icon = Icons.Rounded.Album,
                                badgeColor = Color(0xFF0288D1),
                                onClick = { currentSection = SettingsSection.PLAYER_STYLE }
                            )
                        }
                        item(key = "menu_folders") {
                            SettingsCategoryCard(
                                title = "Папки с музыкой",
                                subtitle = "Выбрано: ${selectedFolderUris.size} • Найдено: ${folderGroups.size}",
                                icon = Icons.Rounded.Folder,
                                badgeColor = Color(0xFF2E7D32),
                                onClick = { currentSection = SettingsSection.FOLDERS }
                            )
                        }
                        item(key = "menu_library") {
                            SettingsCategoryCard(
                                title = "Библиотека и фильтры",
                                subtitle = "Треков: $tracksCount • Дубликаты, мусор, порог ${minDurationSec}с",
                                icon = Icons.Rounded.FilterList,
                                badgeColor = Color(0xFFEF6C00),
                                onClick = { currentSection = SettingsSection.LIBRARY }
                            )
                        }
                        item(key = "menu_eq") {
                            val eqPart = if (AudioEffectsController.eqEnabled) {
                                "Эквалайзер: ${AudioEffectsController.selectedPresetName}"
                            } else {
                                "Встроенный и системный EQ"
                            }
                            val cfPart = if (crossfadeSec > 0) "Кроссфейд ${crossfadeSec}с" else "Кроссфейд выкл."
                            SettingsCategoryCard(
                                title = "Звук, эквалайзер и кроссфейд",
                                subtitle = "$eqPart • $cfPart",
                                icon = Icons.Rounded.GraphicEq,
                                badgeColor = Color(0xFFC2185B),
                                onClick = { currentSection = SettingsSection.SOUND }
                            )
                        }
                        item(key = "menu_system") {
                            SettingsCategoryCard(
                                title = "Интерфейс и память",
                                subtitle = "Кэш обложек: $coverCacheSizeMb МБ • Sonora v1.2",
                                icon = Icons.Rounded.Info,
                                badgeColor = Color(0xFF546E7A),
                                onClick = { currentSection = SettingsSection.SYSTEM }
                            )
                        }
                    }

                    SettingsSection.APPEARANCE -> {
                        item(key = "theme_switches") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                color = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Режимы темы",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    SettingsSwitchRow(
                                        icon = Icons.Rounded.DarkMode,
                                        title = "Тёмная тема",
                                        subtitle = if (darkTheme) "Включена" else "Светлый режим",
                                        checked = darkTheme,
                                        onCheckedChange = onToggleDarkTheme
                                    )

                                    SettingsSwitchRow(
                                        icon = Icons.Rounded.Contrast,
                                        title = "AMOLED чёрный фон",
                                        subtitle = "Чистый #000000 фон для OLED-экранов",
                                        checked = darkTheme && amoledTheme,
                                        onCheckedChange = { enabled ->
                                            if (enabled && !darkTheme) onToggleDarkTheme(true)
                                            onToggleAmoledTheme(enabled)
                                        }
                                    )
                                }
                            }
                        }

                        item(key = "palettes_card") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                color = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Цветовые палитры",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "Primary",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Container",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "Secondary",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                            )
                                        }
                                    }

                                    MonetMode.entries.forEach { mode ->
                                        val selected = monetMode == mode
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                                    else Color.Transparent
                                                )
                                                .clickable { onSelectMonetMode(mode) }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(mode.previewColor)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = mode.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = mode.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            RadioButton(
                                                selected = selected,
                                                onClick = { onSelectMonetMode(mode) }
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Готовые оттенки акцента",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        MonetPresetSwatches.forEach { (name, swatchColor) ->
                                            val isChosen = monetMode == MonetMode.CUSTOM &&
                                                customSeedColor.toArgb() == swatchColor.toArgb()
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(swatchColor)
                                                    .then(
                                                        if (isChosen) {
                                                            Modifier.border(
                                                                width = 3.dp,
                                                                color = MaterialTheme.colorScheme.onBackground,
                                                                shape = CircleShape
                                                            )
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .clickable {
                                                        onSelectCustomColor(swatchColor)
                                                        onSelectMonetMode(MonetMode.CUSTOM)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isChosen) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = name,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.PLAYER_STYLE -> {
                        item(key = "player_style_options") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                color = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Оформление плеера",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    NowPlayingStyle.entries.forEach { style ->
                                        val selected = playerStyle == style
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                                    else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
                                                )
                                                .border(
                                                    width = if (selected) 1.5.dp else 0.dp,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                                .clickable { onSelectPlayerStyle(style) }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 64.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                                contentAlignment = Alignment.TopCenter
                                            ) {
                                                if (style == NowPlayingStyle.FULL_BLEED) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(40.dp)
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    listOf(
                                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                                                    )
                                                                )
                                                            )
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 8.dp)
                                                            .size(28.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = style.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = style.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            RadioButton(
                                                selected = selected,
                                                onClick = { onSelectPlayerStyle(style) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "lyrics_style_options") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                color = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Стиль текста песни",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    LyricsStyle.entries.forEach { lStyle ->
                                        val selected = lyricsStyle == lStyle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                                    else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
                                                )
                                                .border(
                                                    width = if (selected) 1.5.dp else 0.dp,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                                .clickable { onSelectLyricsStyle(lStyle) }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = lStyle.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = lStyle.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            RadioButton(
                                                selected = selected,
                                                onClick = { onSelectLyricsStyle(lStyle) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.SOUND -> {
                        item(key = "eq_card") {
                            val ctx = LocalContext.current
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                color = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Эквалайзер",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Sonora передаёт ID аудиосессии системе — сторонние эквалайзеры (Wavelet, Poweramp EQ, системный DSP) подхватывают звук автоматически.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { showEqSheet = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (AudioEffectsController.eqEnabled) {
                                                "Встроенный эквалайзер (${AudioEffectsController.selectedPresetName})"
                                            } else {
                                                "Открыть встроенный эквалайзер"
                                            }
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            if (!AudioEffectsController.openSystemEqualizer(ctx)) {
                                                Toast.makeText(
                                                    ctx,
                                                    "Системный эквалайзер не найден на устройстве",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Открыть системный / внешний эквалайзер")
                                    }
                                }
                            }
                        }

                        item(key = "crossfade_card") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                color = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Кроссфейд",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Плавное наложение конца текущего трека на начало следующего без пауз.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(0, 2, 4, 6, 8, 10).forEach { sec ->
                                            FilterChip(
                                                selected = crossfadeSec == sec,
                                                onClick = { onSelectCrossfadeSec(sec) },
                                                label = {
                                                    Text(if (sec == 0) "Выкл" else "${sec}с")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.FOLDERS -> {
                        item(key = "folders_actions") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onAddFolderClick,
                                    modifier = Modifier.weight(1f),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Добавить папку", maxLines = 1)
                                }
                                FilledTonalButton(
                                    onClick = onRefreshLibrary,
                                    modifier = Modifier.weight(1f),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Обновить", maxLines = 1)
                                }
                            }
                        }

                        if (selectedFolderUris.isNotEmpty()) {
                            item(key = "selected_folders_header") {
                                Text(
                                    text = "Добавленные вручную (${selectedFolderUris.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(selectedFolderUris, key = { it.toString() }) { folderUri ->
                                val readablePath = remember(folderUri) {
                                    try {
                                        DocumentsContract.getTreeDocumentId(folderUri)
                                            .substringAfter(':')
                                            .ifBlank { "Внутренняя память" }
                                    } catch (_: Exception) {
                                        folderUri.lastPathSegment ?: "Папка"
                                    }
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Folder, contentDescription = null)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = readablePath.substringAfterLast('/'),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = readablePath,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                        IconButton(onClick = { onRemoveFolderClick(folderUri) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить папку")
                                        }
                                    }
                                }
                            }
                        }

                        if (folderGroups.isNotEmpty()) {
                            item(key = "detected_folders_header") {
                                Text(
                                    text = "Папки с треками (${folderGroups.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(folderGroups.keys.toList(), key = { "fg_$it" }) { folderName ->
                                val folderTracks = folderGroups[folderName].orEmpty()
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { onOpenFolderTracks(folderName, folderTracks) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = cardColor
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folderName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${folderTracks.size} треков • нажми чтобы открыть",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.LIBRARY -> {
                        item(key = "library_filters_card") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                color = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Сканирование и фильтры",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Треков в базе: $tracksCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    SettingsSwitchRow(
                                        icon = Icons.Rounded.Refresh,
                                        title = "Автообновление медиатеки",
                                        subtitle = "Подхватывать новые треки автоматически",
                                        checked = autoRefreshLibrary,
                                        onCheckedChange = onToggleAutoRefreshLibrary
                                    )

                                    SettingsSwitchRow(
                                        icon = Icons.Rounded.FilterList,
                                        title = "Скрывать дубликаты",
                                        subtitle = "Оставлять версию лучшего качества",
                                        checked = hideDuplicates,
                                        onCheckedChange = onToggleHideDuplicates
                                    )

                                    SettingsSwitchRow(
                                        icon = Icons.Rounded.Folder,
                                        title = "Скрывать системные папки",
                                        subtitle = "Игнорировать диктофон, рингтоны и мессенджеры",
                                        checked = ignoreJunkFolders,
                                        onCheckedChange = onToggleIgnoreJunkFolders
                                    )

                                    Text(
                                        text = "Минимальная длительность трека",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(3, 15, 30, 60).forEach { sec ->
                                            FilterChip(
                                                selected = minDurationSec == sec,
                                                onClick = { onSelectMinDurationSec(sec) },
                                                label = { Text("от ${sec}с") }
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = onRefreshLibrary,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Пересканировать медиатеку")
                                    }
                                }
                            }
                        }
                    }

                    SettingsSection.SYSTEM -> {
                        item(key = "interface_memory_card") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                color = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Интерфейс и кэш",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    SettingsSwitchRow(
                                        icon = Icons.Rounded.SwapHoriz,
                                        title = "Панель навигации справа",
                                        subtitle = if (landscapeNavOnRight) "В альбомном режиме справа" else "В альбомном режиме слева",
                                        checked = landscapeNavOnRight,
                                        onCheckedChange = onToggleLandscapeNavOnRight
                                    )

                                    Text(
                                        text = "Размер кэша обложек: $coverCacheSizeMb МБ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    FilledTonalButton(
                                        onClick = onClearCoverCache,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Очистить кэш обложек")
                                    }
                                }
                            }
                        }

                        item(key = "about_card") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                color = cardColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Sonora v1.2",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Плеер с поддержкой Hi-Res / DSD, эквалайзером и автопоиском синхронизированных текстов (LRCMux • LRCLIB • KuGou).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEqSheet) {
        EqualizerBottomSheet(onDismiss = { showEqSheet = false })
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
