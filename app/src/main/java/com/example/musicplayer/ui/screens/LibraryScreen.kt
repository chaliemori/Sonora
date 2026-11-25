package com.example.musicplayer.ui.screens

import android.content.res.Configuration
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.data.LocalTrack
import com.example.musicplayer.data.UserPlaylist
import com.example.musicplayer.ui.theme.MonetMode
import java.io.File
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<LocalTrack>,
    playlists: List<UserPlaylist>,
    favoriteIds: Set<Long>,
    selectedFolderUris: List<Uri>,
    isScanning: Boolean,
    scannedFilesCount: Int,
    scanStatusText: String,
    currentTrack: LocalTrack?,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    darkTheme: Boolean,
    amoledTheme: Boolean,
    landscapeNavOnRight: Boolean,
    hideDuplicates: Boolean = true,
    ignoreJunkFolders: Boolean = true,
    autoRefreshLibrary: Boolean = true,
    minDurationSec: Int = 15,
    monetMode: MonetMode,
    customSeedColor: Color,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetAmoledTheme: (Boolean) -> Unit,
    onSetLandscapeNavOnRight: (Boolean) -> Unit,
    onSetHideDuplicates: (Boolean) -> Unit = {},
    onSetIgnoreJunkFolders: (Boolean) -> Unit = {},
    onSetAutoRefreshLibrary: (Boolean) -> Unit = {},
    onSetMinDurationSec: (Int) -> Unit = {},
    onSelectMonetMode: (MonetMode) -> Unit,
    onSelectCustomColor: (Color) -> Unit,
    onCycleMonetMode: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onClearCoverCache: () -> Unit,
    onAddFolderClick: () -> Unit,
    onRemoveFolderClick: (Uri) -> Unit,
    onSelectTrack: (Int, List<LocalTrack>) -> Unit,
    onShuffleAll: (List<LocalTrack>) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onRenamePlaylistClick: (UserPlaylist) -> Unit,
    onDeletePlaylistClick: (UserPlaylist) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit = {},
    onPreviousTrack: () -> Unit = {},
    onPlayNext: (LocalTrack) -> Unit = {},
    onAddToQueue: (LocalTrack) -> Unit = {},
    onRemoveTrackFromPlaylist: (LocalTrack, String) -> Unit = { _, _ -> },
    onDeleteTrackFromDevice: (LocalTrack) -> Unit = {},
    onToggleTrackFavorite: (LocalTrack) -> Unit = {},
    onAddTrackToPlaylist: (LocalTrack) -> Unit = {},
    onOpenNowPlaying: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var trackMenuTarget by remember { mutableStateOf<LocalTrack?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableIntStateOf(0) } // 0 = А–Я, 1 = Я–А, 2 = Новые, 3 = Исполн.
    var selectedTab by remember { mutableIntStateOf(0) }
    var activePlaylistId by remember { mutableStateOf<String?>(null) }
    var activePlaylistFilterTitle by remember { mutableStateOf<String?>(null) }
    var cacheRefreshTick by remember { mutableIntStateOf(0) }

    val coverCacheSizeMb = remember(tracks.size, cacheRefreshTick) {
        val dir = File(context.cacheDir, "covers")
        val bytes = dir.listFiles()?.sumOf { it.length() } ?: 0L
        String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024.0))
    }

    val refreshRotation by if (isScanning) {
        val transition = rememberInfiniteTransition(label = "scanSpin")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scanAngle"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val folderGroups = remember(tracks) {
        tracks.groupBy { it.folderName }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    val activePlaylistQueue = remember(activePlaylistId, tracks, favoriteIds, playlists, folderGroups) {
        when {
            activePlaylistId == null -> emptyList()
            activePlaylistId == "__FAVORITES__" -> tracks.filter { it.id in favoriteIds }
            activePlaylistId == "__LOSSLESS__" -> tracks.filter {
                it.formatLabel.equals("FLAC", true) ||
                    it.formatLabel.equals("WAV", true) ||
                    it.formatLabel.equals("ALAC", true) ||
                    it.formatLabel.equals("DSF", true) ||
                    it.formatLabel.equals("DFF", true)
            }
            activePlaylistId!!.startsWith("__FOLDER__:") -> {
                val folderName = activePlaylistId!!.removePrefix("__FOLDER__:")
                folderGroups[folderName].orEmpty()
            }
            else -> {
                val pl = playlists.firstOrNull { it.id == activePlaylistId }
                if (pl != null) tracks.filter { it.id in pl.trackIds } else emptyList()
            }
        }
    }

    val filteredTracks = remember(tracks, searchQuery, sortMode, activePlaylistId, activePlaylistQueue) {
        val sourceList = if (activePlaylistId != null) activePlaylistQueue else tracks
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        val base = if (q.isBlank()) {
            sourceList
        } else {
            sourceList.filter {
                it.title.lowercase(Locale.getDefault()).contains(q) ||
                    it.artist.lowercase(Locale.getDefault()).contains(q) ||
                    it.folderName.lowercase(Locale.getDefault()).contains(q)
            }
        }
        when (sortMode) {
            1 -> base.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            2 -> base.sortedByDescending { it.dateAddedSec }
            3 -> base.sortedBy { it.artist.lowercase(Locale.getDefault()) }
            else -> base.sortedBy { it.title.lowercase(Locale.getDefault()) }
        }
    }

    val topBarTitle = when {
        activePlaylistFilterTitle != null -> activePlaylistFilterTitle!!
        selectedTab == 3 -> "Настройки"
        else -> "Медиатека"
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showSearchRow = selectedTab == 0 || activePlaylistFilterTitle != null

    // Нижний отступ контента, чтобы списки прокручивались под парящими пилюлями и не перекрывались в конце
    val listBottomPadding = when {
        isLandscape && currentTrack != null -> 94.dp
        isLandscape -> 20.dp
        currentTrack != null -> 188.dp
        else -> 114.dp
    }

    val searchAndSortRow: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Поиск по трекам...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Очистить поиск"
                            )
                        }
                    }
                } else null,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Surface(
                modifier = Modifier
                    .height(if (isLandscape) 48.dp else 52.dp)
                    .clip(CircleShape)
                    .clickable { sortMode = (sortMode + 1) % 4 },
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = if (isLandscape) 14.dp else 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (sortMode) {
                            1 -> "Я–А"
                            2 -> "Новые"
                            3 -> "Исполн."
                            else -> "А–Я"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    val landscapeRail: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    start = if (landscapeNavOnRight) 0.dp else 12.dp,
                    end = if (landscapeNavOnRight) 12.dp else 0.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavBarItem(
                        selected = selectedTab == 0 && activePlaylistFilterTitle == null,
                        icon = Icons.Rounded.MusicNote,
                        label = "Треки",
                        onClick = {
                            activePlaylistId = null
                            activePlaylistFilterTitle = null
                            selectedTab = 0
                        }
                    )
                    NavBarItem(
                        selected = selectedTab == 1 && activePlaylistFilterTitle == null,
                        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        label = "Плейлисты",
                        onClick = {
                            activePlaylistId = null
                            activePlaylistFilterTitle = null
                            selectedTab = 1
                        }
                    )
                    NavBarItem(
                        selected = selectedTab == 2 && activePlaylistFilterTitle == null,
                        icon = Icons.Rounded.Folder,
                        label = "Папки",
                        onClick = {
                            activePlaylistId = null
                            activePlaylistFilterTitle = null
                            selectedTab = 2
                        }
                    )
                    NavBarItem(
                        selected = selectedTab == 3 && activePlaylistFilterTitle == null,
                        icon = Icons.Rounded.Settings,
                        label = "Настройки",
                        onClick = {
                            activePlaylistId = null
                            activePlaylistFilterTitle = null
                            selectedTab = 3
                        }
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .let { if (isLandscape) it.safeDrawingPadding() else it }
    ) {
        if (isLandscape && !landscapeNavOnRight) {
            landscapeRail()
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (!isLandscape) it.statusBarsPadding() else it }
            ) {
                // Верхний заголовок (в горизонтальном режиме поиск встроен в ту же строку)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isLandscape) 16.dp else 20.dp,
                            vertical = if (isLandscape) 8.dp else 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = topBarTitle,
                        style = if (isLandscape) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (isLandscape && showSearchRow) Modifier else Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (isLandscape && showSearchRow) {
                        searchAndSortRow(Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (activePlaylistFilterTitle != null) {
                            FilledTonalIconButton(
                                onClick = {
                                    activePlaylistId = null
                                    activePlaylistFilterTitle = null
                                },
                                modifier = Modifier.size(if (isLandscape) 40.dp else 44.dp),
                                shape = CircleShape
                            ) {
                                Text("Все", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        FilledTonalIconButton(
                            onClick = onRefreshLibrary,
                            modifier = Modifier.size(if (isLandscape) 40.dp else 44.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Обновить библиотеку",
                                modifier = Modifier.graphicsLayer { rotationZ = refreshRotation }
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onCycleMonetMode,
                            modifier = Modifier.size(if (isLandscape) 40.dp else 44.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = "Monet: ${monetMode.title}"
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { onSetDarkTheme(!darkTheme) },
                            modifier = Modifier.size(if (isLandscape) 40.dp else 44.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Тема"
                            )
                        }
                    }
                }

                if (!isLandscape && showSearchRow) {
                    searchAndSortRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

            AnimatedContent(
                targetState = if (activePlaylistFilterTitle != null) 0 else selectedTab,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 28 })
                        .togetherWith(fadeOut(tween(140)))
                },
                label = "libraryTabContent"
            ) { tabIndex ->
                when (tabIndex) {
                    1 -> {
                        val favoritesList = remember(tracks, favoriteIds) {
                            tracks.filter { it.id in favoriteIds }
                        }
                        val losslessList = remember(tracks) {
                            tracks.filter {
                                it.formatLabel.equals("FLAC", true) ||
                                    it.formatLabel.equals("WAV", true) ||
                                    it.formatLabel.equals("ALAC", true) ||
                                    it.formatLabel.equals("DSF", true) ||
                                    it.formatLabel.equals("DFF", true)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 4.dp,
                                bottom = listBottomPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Плейлисты",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Button(
                                        onClick = onCreatePlaylistClick,
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Создать")
                                    }
                                }
                            }

                            item {
                                PlaylistSummaryCard(
                                    title = "Избранное",
                                    subtitle = "${favoritesList.size} треков",
                                    coverUri = favoritesList.firstOrNull()?.let { it.artworkUri ?: it.contentUri },
                                    onClick = {
                                        activePlaylistId = "__FAVORITES__"
                                        activePlaylistFilterTitle = "Избранное"
                                    }
                                )
                            }

                            item {
                                PlaylistSummaryCard(
                                    title = "Lossless / FLAC",
                                    subtitle = "${losslessList.size} треков высокого качества",
                                    coverUri = losslessList.firstOrNull()?.let { it.artworkUri ?: it.contentUri },
                                    onClick = {
                                        activePlaylistId = "__LOSSLESS__"
                                        activePlaylistFilterTitle = "Lossless / FLAC"
                                    }
                                )
                            }

                            items(playlists, key = { it.id }) { playlist ->
                                val pTracks = remember(tracks, playlist.trackIds) {
                                    tracks.filter { it.id in playlist.trackIds }
                                }
                                PlaylistSummaryCard(
                                    title = playlist.name,
                                    subtitle = "${pTracks.size} треков",
                                    coverUri = pTracks.firstOrNull()?.let { it.artworkUri ?: it.contentUri },
                                    onClick = {
                                        activePlaylistId = playlist.id
                                        activePlaylistFilterTitle = playlist.name
                                    },
                                    onRename = { onRenamePlaylistClick(playlist) },
                                    onDelete = { onDeletePlaylistClick(playlist) }
                                )
                            }
                        }
                    }

                    2 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 4.dp,
                                bottom = listBottomPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Папки с музыкой",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = onAddFolderClick,
                                            modifier = Modifier.weight(1f),
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CreateNewFolder,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Добавить папку", maxLines = 1)
                                        }

                                        FilledTonalButton(
                                            onClick = onRefreshLibrary,
                                            modifier = Modifier.weight(1f),
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .graphicsLayer { rotationZ = refreshRotation }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Обновить", maxLines = 1)
                                        }
                                    }
                                }
                            }

                            if (selectedFolderUris.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Выбранные директории (${selectedFolderUris.size})",
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
                                            Icon(
                                                imageVector = Icons.Rounded.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = readablePath.substringAfterLast('/'),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
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
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Удалить папку",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (folderGroups.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Папки с треками",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                items(folderGroups.keys.toList(), key = { it }) { folder ->
                                    val folderTracks = folderGroups[folder].orEmpty()
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                activePlaylistId = "__FOLDER__:$folder"
                                                activePlaylistFilterTitle = folder
                                            },
                                        color = MaterialTheme.colorScheme.surfaceContainerLow
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                                    text = folder,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${folderTracks.size} треков",
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

                    3 -> {
                        SettingsScreen(
                            darkTheme = darkTheme,
                            amoledTheme = amoledTheme,
                            landscapeNavOnRight = landscapeNavOnRight,
                            hideDuplicates = hideDuplicates,
                            ignoreJunkFolders = ignoreJunkFolders,
                            autoRefreshLibrary = autoRefreshLibrary,
                            minDurationSec = minDurationSec,
                            monetMode = monetMode,
                            customSeedColor = customSeedColor,
                            tracksCount = tracks.size,
                            coverCacheSizeMb = coverCacheSizeMb,
                            bottomPaddingDp = listBottomPadding.value.toInt(),
                            onToggleDarkTheme = onSetDarkTheme,
                            onToggleAmoledTheme = onSetAmoledTheme,
                            onToggleLandscapeNavOnRight = onSetLandscapeNavOnRight,
                            onToggleHideDuplicates = onSetHideDuplicates,
                            onToggleIgnoreJunkFolders = onSetIgnoreJunkFolders,
                            onToggleAutoRefreshLibrary = onSetAutoRefreshLibrary,
                            onSelectMinDurationSec = onSetMinDurationSec,
                            onSelectMonetMode = onSelectMonetMode,
                            onSelectCustomColor = onSelectCustomColor,
                            onRefreshLibrary = onRefreshLibrary,
                            onClearCoverCache = {
                                onClearCoverCache()
                                cacheRefreshTick++
                            }
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 2.dp,
                                bottom = listBottomPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activePlaylistFilterTitle == null) {
                                item(key = "library_header_card", contentType = "header") {
                                    // Компактная и ровная плашка состояния библиотеки
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                                                        text = if (isScanning) {
                                                            "Сканирование: $scannedFilesCount"
                                                        } else {
                                                            "${filteredTracks.size} треков"
                                                        },
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = scanStatusText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = onAddFolderClick,
                                                    modifier = Modifier.weight(1f),
                                                    shape = CircleShape,
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.CreateNewFolder,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = "Папка",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1
                                                    )
                                                }

                                                FilledTonalButton(
                                                    onClick = onRefreshLibrary,
                                                    modifier = Modifier.weight(1f),
                                                    shape = CircleShape,
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Refresh,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .graphicsLayer { rotationZ = refreshRotation }
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = "Обновить",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1
                                                    )
                                                }

                                                if (filteredTracks.isNotEmpty()) {
                                                    FilledTonalButton(
                                                        onClick = { onShuffleAll(filteredTracks) },
                                                        modifier = Modifier.weight(1f),
                                                        shape = CircleShape,
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Shuffle,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(
                                                            text = "Микс",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            itemsIndexed(
                                items = filteredTracks,
                                key = { _, track -> track.id },
                                contentType = { _, _ -> "track_row" }
                            ) { index, track ->
                                val isSelected = currentTrack?.id == track.id
                                val thumbRequest = remember(track.id) {
                                    ImageRequest.Builder(context)
                                        .data(track.artworkUri ?: track.contentUri)
                                        .size(128)
                                        .build()
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .combinedClickable(
                                            onClick = { onSelectTrack(index, filteredTracks) },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                trackMenuTarget = track
                                            }
                                        ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            AsyncImage(
                                                model = thumbRequest,
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (track.durationMs > 0L) {
                                                    "${track.artist} • ${formatDuration(track.durationMs)}"
                                                } else {
                                                    track.artist
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        if (isSelected) {
                                            MiniEqualizerBars(
                                                isPlaying = isPlaying,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Text(
                                                text = track.formatLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
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
        }

            // Парящий прозрачный нижний блок: (в режиме одной руки) поиск + пилюля трека + пилюля хотбара
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .let { if (!isLandscape) it.navigationBarsPadding() else it }
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (isLandscape) 10.dp else 14.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentTrack != null) {
                    val miniCoverRequest = remember(currentTrack.id) {
                        ImageRequest.Builder(context)
                            .data(currentTrack.artworkUri ?: currentTrack.contentUri)
                            .size(128)
                            .build()
                    }
                    val miniDragX = remember { Animatable(0f) }
                    var miniTransitionDir by remember { mutableIntStateOf(1) }
                    val miniSwipeThresholdPx = with(density) { 44.dp.toPx() }
                    val miniMaxDragPx = with(density) { 100.dp.toPx() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            miniDragX.snapTo(
                                                (miniDragX.value + dragAmount * 0.75f).coerceIn(-miniMaxDragPx, miniMaxDragPx)
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        when {
                                            miniDragX.value <= -miniSwipeThresholdPx -> {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                miniTransitionDir = 1
                                                onNextTrack()
                                            }
                                            miniDragX.value >= miniSwipeThresholdPx -> {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                miniTransitionDir = -1
                                                onPreviousTrack()
                                            }
                                        }
                                        scope.launch {
                                            miniDragX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            miniDragX.animateTo(0f)
                                        }
                                    }
                                )
                            }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationX = miniDragX.value }
                                .clip(CircleShape)
                                .clickable(onClick = onOpenNowPlaying),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 6.dp
                        ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    AsyncImage(
                                        model = miniCoverRequest,
                                        contentDescription = currentTrack.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                AnimatedContent(
                                    targetState = currentTrack,
                                    modifier = Modifier.weight(1f),
                                    transitionSpec = {
                                        val dir = miniTransitionDir
                                        (slideInHorizontally(tween(240)) { (it * 0.3f * dir).toInt() } + fadeIn(tween(200)))
                                            .togetherWith(
                                                slideOutHorizontally(tween(200)) { (-it * 0.3f * dir).toInt() } + fadeOut(tween(150))
                                            )
                                    },
                                    label = "miniTrackSlide"
                                ) { trackItem ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = trackItem.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${trackItem.artist} • ${trackItem.formatLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                FilledIconButton(
                                    onClick = onTogglePlayPause,
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Пауза" else "Воспроизвести"
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .height(2.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(playbackProgress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                    }
                }

                if (!isLandscape) {
                    // Хотбар в виде парящей пилюли без сплошного фона под ней
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavBarItem(
                                selected = selectedTab == 0 && activePlaylistFilterTitle == null,
                                icon = Icons.Rounded.MusicNote,
                                label = "Треки",
                                onClick = {
                                    activePlaylistFilterTitle = null
                                    selectedTab = 0
                                }
                            )
                            NavBarItem(
                                selected = selectedTab == 1 && activePlaylistFilterTitle == null,
                                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                label = "Плейлисты",
                                onClick = {
                                    activePlaylistFilterTitle = null
                                    selectedTab = 1
                                }
                            )
                            NavBarItem(
                                selected = selectedTab == 2 && activePlaylistFilterTitle == null,
                                icon = Icons.Rounded.Folder,
                                label = "Папки",
                                onClick = {
                                    activePlaylistFilterTitle = null
                                    selectedTab = 2
                                }
                            )
                            NavBarItem(
                                selected = selectedTab == 3 && activePlaylistFilterTitle == null,
                                icon = Icons.Rounded.Settings,
                                label = "Настройки",
                                onClick = {
                                    activePlaylistFilterTitle = null
                                    selectedTab = 3
                                }
                            )
                        }
                    }
                }
            }
        }

        if (isLandscape && landscapeNavOnRight) {
            landscapeRail()
        }
    }

    val menuTrack = trackMenuTarget
    if (menuTrack != null) {
        val isFav = menuTrack.id in favoriteIds
        val srLabel = when {
            menuTrack.sampleRateHz >= 1_000_000 ->
                String.format(Locale.US, "%.1f MHz", menuTrack.sampleRateHz / 1_000_000.0)
            menuTrack.sampleRateHz % 1000 == 0 ->
                "${menuTrack.sampleRateHz / 1000} kHz"
            else ->
                String.format(Locale.US, "%.1f kHz", menuTrack.sampleRateHz / 1000.0)
        }
        ModalBottomSheet(
            onDismissRequest = { trackMenuTarget = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = menuTrack.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${menuTrack.artist} • ${menuTrack.formatLabel} • ${menuTrack.bitrateKbps} kbps • $srLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            onPlayNext(menuTrack)
                            trackMenuTarget = null
                        },
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueuePlayNext,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Воспроизвести следующим",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            onAddToQueue(menuTrack)
                            trackMenuTarget = null
                        },
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Добавить в очередь",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                val currentCustomPlaylist = playlists.firstOrNull { it.id == activePlaylistId }
                if (currentCustomPlaylist != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onRemoveTrackFromPlaylist(menuTrack, currentCustomPlaylist.id)
                                trackMenuTarget = null
                            },
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistRemove,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Убрать из плейлиста «${currentCustomPlaylist.name}»",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            onToggleTrackFavorite(menuTrack)
                            trackMenuTarget = null
                        },
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (isFav) "Убрать из избранного" else "Добавить в избранное",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            onAddTrackToPlaylist(menuTrack)
                            trackMenuTarget = null
                        },
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Добавить в плейлист",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            activePlaylistId = "__FOLDER__:${menuTrack.folderName}"
                            activePlaylistFilterTitle = menuTrack.folderName
                            trackMenuTarget = null
                        },
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Открыть папку «${menuTrack.folderName}»",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            onDeleteTrackFromDevice(menuTrack)
                            trackMenuTarget = null
                        },
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Удалить файл с устройства",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistSummaryCard(
    title: String,
    subtitle: String,
    coverUri: Uri?,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val request = remember(coverUri) {
        coverUri?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(160)
                .build()
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (request != null) {
                    AsyncImage(
                        model = request,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onRename != null) {
                IconButton(onClick = onRename) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Переименовать",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val pillScale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "navPillSpring"
    )

    Column(
        modifier = Modifier
            .widthIn(min = 72.dp)
            .graphicsLayer {
                scaleX = pillScale
                scaleY = pillScale
            }
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun MiniEqualizerBars(
    isPlaying: Boolean,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "miniEq")
    val bar1 by if (isPlaying) {
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
            label = "b1"
        )
    } else {
        remember { mutableFloatStateOf(0.35f) }
    }
    val bar2 by if (isPlaying) {
        transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
            label = "b2"
        )
    } else {
        remember { mutableFloatStateOf(0.55f) }
    }
    val bar3 by if (isPlaying) {
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(360), RepeatMode.Reverse),
            label = "b3"
        )
    } else {
        remember { mutableFloatStateOf(0.4f) }
    }

    Row(
        modifier = Modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3).forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(frac)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
