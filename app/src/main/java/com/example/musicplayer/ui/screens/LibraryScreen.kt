package com.example.musicplayer.ui.screens

import android.content.res.Configuration
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.musicplayer.data.LibraryFilterPrefs
import com.example.musicplayer.data.LibraryGroupMode
import com.example.musicplayer.data.LibraryQualityFilter
import com.example.musicplayer.data.LibrarySortMode
import com.example.musicplayer.data.LocalTrack
import com.example.musicplayer.data.UserPlaylist
import com.example.musicplayer.ui.theme.LyricsStyle
import com.example.musicplayer.ui.theme.MonetMode
import com.example.musicplayer.ui.theme.PaletteStyle
import com.example.musicplayer.ui.theme.NowPlayingStyle
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
    playerStyle: NowPlayingStyle = NowPlayingStyle.CARD,
    lyricsStyle: LyricsStyle = LyricsStyle.APPLE_MUSIC,
    crossfadeSec: Int = 0,
    landscapeNavOnRight: Boolean,
    hideDuplicates: Boolean = true,
    ignoreJunkFolders: Boolean = true,
    autoRefreshLibrary: Boolean = true,
    minDurationSec: Int = 15,
    monetMode: MonetMode,
    paletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
    coverAccent: Color = Color(0xFF6750A4),
    customSeedColor: Color,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetAmoledTheme: (Boolean) -> Unit,
    onSelectPlayerStyle: (NowPlayingStyle) -> Unit = {},
    onSelectLyricsStyle: (LyricsStyle) -> Unit = {},
    onSelectCrossfadeSec: (Int) -> Unit = {},
    onSetLandscapeNavOnRight: (Boolean) -> Unit,
    onSetHideDuplicates: (Boolean) -> Unit = {},
    onSetIgnoreJunkFolders: (Boolean) -> Unit = {},
    onSetAutoRefreshLibrary: (Boolean) -> Unit = {},
    onSetMinDurationSec: (Int) -> Unit = {},
    onSelectMonetMode: (MonetMode) -> Unit,
    onSelectPaletteStyle: (PaletteStyle) -> Unit = {},
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
    LaunchedEffect(Unit) {
        LibraryFilterPrefs.ensureLoaded(context)
    }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var trackMenuTarget by remember { mutableStateOf<LocalTrack?>(null) }
    var pendingDeleteConfirmTrack by remember { mutableStateOf<LocalTrack?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(LibrarySortMode.TITLE_ASC) }
    var groupMode by remember { mutableStateOf(LibraryGroupMode.TRACKS) }
    var qualityFilter by remember { mutableStateOf(LibraryQualityFilter.ALL) }
    var drillArtist by remember { mutableStateOf<String?>(null) }
    var drillAlbum by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showPlaylistDisplayConfigSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var activePlaylistId by remember { mutableStateOf<String?>(null) }
    var activePlaylistFilterTitle by remember { mutableStateOf<String?>(null) }
    var cacheRefreshTick by remember { mutableIntStateOf(0) }

    BackHandler(enabled = drillAlbum != null || drillArtist != null || activePlaylistId != null) {
        when {
            drillAlbum != null -> drillAlbum = null
            drillArtist != null -> drillArtist = null
            else -> {
                activePlaylistId = null
                activePlaylistFilterTitle = null
                groupMode = LibraryGroupMode.TRACKS
                qualityFilter = LibraryQualityFilter.ALL
            }
        }
    }

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

    val tracksById = remember(tracks) {
        tracks.associateBy { it.id }
    }

    val folderGroups = remember(tracks) {
        tracks.groupBy { it.folderName }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    val allArtistCount = remember(tracks) {
        tracks.map { it.artist.trim().ifBlank { "Неизвестный исполнитель" } }.distinct().size
    }

    val allAlbumCount = remember(tracks) {
        tracks.map { it.album.trim().ifBlank { "Без альбома" } }.distinct().size
    }

    val activePlaylistQueue = remember(activePlaylistId, tracks, tracksById, favoriteIds, playlists, folderGroups) {
        when {
            activePlaylistId == null -> emptyList()
            activePlaylistId == "__ALL_ARTISTS__" ||
                activePlaylistId == "__ALL_ALBUMS__" ||
                activePlaylistId == "__ALL_ARTISTS_ALBUMS__" -> tracks
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
                if (pl != null) pl.trackIds.mapNotNull { tracksById[it] } else emptyList()
            }
        }
    }

    val baseFilteredTracks = remember(
        tracks,
        searchQuery,
        qualityFilter,
        favoriteIds,
        activePlaylistId,
        activePlaylistQueue
    ) {
        val sourceList = if (activePlaylistId != null) activePlaylistQueue else tracks
        val qFiltered = when (qualityFilter) {
            LibraryQualityFilter.ALL -> sourceList
            LibraryQualityFilter.LOSSLESS -> sourceList.filter {
                it.formatLabel.equals("FLAC", true) ||
                    it.formatLabel.equals("WAV", true) ||
                    it.formatLabel.equals("ALAC", true) ||
                    it.formatLabel.equals("DSF", true) ||
                    it.formatLabel.equals("DFF", true)
            }
            LibraryQualityFilter.FLAC -> sourceList.filter { it.formatLabel.equals("FLAC", true) }
            LibraryQualityFilter.MP3 -> sourceList.filter { it.formatLabel.equals("MP3", true) }
            LibraryQualityFilter.FAVORITES -> sourceList.filter { it.id in favoriteIds }
        }
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) {
            qFiltered
        } else {
            qFiltered.filter {
                it.title.lowercase(Locale.getDefault()).contains(q) ||
                    it.artist.lowercase(Locale.getDefault()).contains(q) ||
                    it.album.lowercase(Locale.getDefault()).contains(q) ||
                    it.folderName.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    val artistGroups = remember(baseFilteredTracks) {
        baseFilteredTracks
            .groupBy { it.artist.trim().ifBlank { "Неизвестный исполнитель" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    val albumGroups = remember(baseFilteredTracks) {
        baseFilteredTracks
            .groupBy { it.album.trim().ifBlank { "Без альбома" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    val filteredTracks = remember(baseFilteredTracks, sortMode, drillArtist, drillAlbum) {
        val drilled = baseFilteredTracks.filter { track ->
            val tArtist = track.artist.trim().ifBlank { "Неизвестный исполнитель" }
            val tAlbum = track.album.trim().ifBlank { "Без альбома" }
            (drillArtist == null || tArtist.equals(drillArtist, ignoreCase = true)) &&
                (drillAlbum == null || tAlbum.equals(drillAlbum, ignoreCase = true))
        }
        when (sortMode) {
            LibrarySortMode.TITLE_DESC -> drilled.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            LibrarySortMode.NEWEST -> drilled.sortedByDescending { it.dateAddedSec }
            LibrarySortMode.ARTIST -> drilled.sortedBy { it.artist.lowercase(Locale.getDefault()) }
            LibrarySortMode.ALBUM -> drilled.sortedBy { it.album.lowercase(Locale.getDefault()) }
            LibrarySortMode.DURATION -> drilled.sortedByDescending { it.durationMs }
            LibrarySortMode.CUSTOM_ORDER -> drilled
            LibrarySortMode.TITLE_ASC -> drilled.sortedBy { it.title.lowercase(Locale.getDefault()) }
        }
    }

    val topBarTitle = when {
        activePlaylistFilterTitle != null -> activePlaylistFilterTitle!!
        selectedTab == 2 -> "Настройки"
        selectedTab == 1 -> "Плейлисты"
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

            Box {
                Surface(
                    modifier = Modifier
                        .height(if (isLandscape) 48.dp else 52.dp)
                        .clip(CircleShape)
                        .clickable { showSortMenu = true },
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = if (isLandscape) 14.dp else 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sortMode.shortLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    LibrarySortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.title) },
                            leadingIcon = if (sortMode == mode) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else null,
                            onClick = {
                                sortMode = mode
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }
    }

    val filterChipsRow: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(
            modifier = rowModifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = groupMode == LibraryGroupMode.TRACKS,
                onClick = {
                    groupMode = LibraryGroupMode.TRACKS
                    drillArtist = null
                    drillAlbum = null
                },
                label = { Text("Треки") }
            )
            if (LibraryFilterPrefs.showArtistsFilter) {
                FilterChip(
                    selected = groupMode == LibraryGroupMode.ARTISTS,
                    onClick = {
                        groupMode = LibraryGroupMode.ARTISTS
                        drillArtist = null
                        drillAlbum = null
                    },
                    label = { Text("Артисты (${artistGroups.size})") }
                )
            }
            if (LibraryFilterPrefs.showAlbumsFilter) {
                FilterChip(
                    selected = groupMode == LibraryGroupMode.ALBUMS,
                    onClick = {
                        groupMode = LibraryGroupMode.ALBUMS
                        drillArtist = null
                        drillAlbum = null
                    },
                    label = { Text("Альбомы (${albumGroups.size})") }
                )
            }
            if (LibraryFilterPrefs.showArtistsAlbumsFilter) {
                FilterChip(
                    selected = groupMode == LibraryGroupMode.ARTISTS_ALBUMS,
                    onClick = {
                        groupMode = LibraryGroupMode.ARTISTS_ALBUMS
                        drillArtist = null
                        drillAlbum = null
                    },
                    label = { Text("Артисты и альбомы") }
                )
            }
            Box {
                FilterChip(
                    selected = qualityFilter != LibraryQualityFilter.ALL,
                    onClick = { showQualityMenu = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (qualityFilter == LibraryQualityFilter.ALL) "Фильтр"
                            else qualityFilter.title
                        )
                    }
                )
                DropdownMenu(
                    expanded = showQualityMenu,
                    onDismissRequest = { showQualityMenu = false }
                ) {
                    LibraryQualityFilter.entries.forEach { qf ->
                        DropdownMenuItem(
                            text = { Text(qf.title) },
                            leadingIcon = if (qualityFilter == qf) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else null,
                            onClick = {
                                qualityFilter = qf
                                showQualityMenu = false
                            }
                        )
                    }
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
                            drillArtist = null
                            drillAlbum = null
                            groupMode = LibraryGroupMode.TRACKS
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
                            drillArtist = null
                            drillAlbum = null
                            groupMode = LibraryGroupMode.TRACKS
                            selectedTab = 1
                        }
                    )
                    NavBarItem(
                        selected = selectedTab == 2 && activePlaylistFilterTitle == null,
                        icon = Icons.Rounded.Settings,
                        label = "Настройки",
                        onClick = {
                            activePlaylistId = null
                            activePlaylistFilterTitle = null
                            drillArtist = null
                            drillAlbum = null
                            selectedTab = 2
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
                                    drillArtist = null
                                    drillAlbum = null
                                    groupMode = LibraryGroupMode.TRACKS
                                    qualityFilter = LibraryQualityFilter.ALL
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

                AnimatedVisibility(
                    visible = !isLandscape && showSearchRow,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        searchAndSortRow(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        )
                        AnimatedVisibility(
                            visible = LibraryFilterPrefs.showFilterBar,
                            enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(140)) + shrinkVertically(tween(170))
                        ) {
                            filterChipsRow(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .padding(horizontal = 20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                AnimatedVisibility(
                    visible = isLandscape && showSearchRow && LibraryFilterPrefs.showFilterBar,
                    enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(170))
                ) {
                    filterChipsRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 6.dp)
                    )
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = { showPlaylistDisplayConfigSheet = true },
                                            shape = CircleShape
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tune,
                                                contentDescription = "Настроить фильтры и отображение"
                                            )
                                        }
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
                            }

                            if (LibraryFilterPrefs.showArtistsFilter ||
                                LibraryFilterPrefs.showAlbumsFilter ||
                                LibraryFilterPrefs.showArtistsAlbumsFilter
                            ) {
                                item(key = "playlist_catalog_filters") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Фильтры и каталоги",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (LibraryFilterPrefs.showArtistsFilter) {
                                                PlaylistQuickFilterCard(
                                                    icon = Icons.Rounded.Person,
                                                    title = "По артистам",
                                                    subtitle = "$allArtistCount исполн.",
                                                    onClick = {
                                                        activePlaylistId = "__ALL_ARTISTS__"
                                                        activePlaylistFilterTitle = "Артисты"
                                                        groupMode = LibraryGroupMode.ARTISTS
                                                        drillArtist = null
                                                        drillAlbum = null
                                                    }
                                                )
                                            }
                                            if (LibraryFilterPrefs.showAlbumsFilter) {
                                                PlaylistQuickFilterCard(
                                                    icon = Icons.Rounded.Album,
                                                    title = "По альбомам",
                                                    subtitle = "$allAlbumCount альб.",
                                                    onClick = {
                                                        activePlaylistId = "__ALL_ALBUMS__"
                                                        activePlaylistFilterTitle = "Альбомы"
                                                        groupMode = LibraryGroupMode.ALBUMS
                                                        drillArtist = null
                                                        drillAlbum = null
                                                    }
                                                )
                                            }
                                            if (LibraryFilterPrefs.showArtistsAlbumsFilter) {
                                                PlaylistQuickFilterCard(
                                                    icon = Icons.Rounded.LibraryMusic,
                                                    title = "Артисты и альбомы",
                                                    subtitle = "Иерархия",
                                                    onClick = {
                                                        activePlaylistId = "__ALL_ARTISTS_ALBUMS__"
                                                        activePlaylistFilterTitle = "Артисты и альбомы"
                                                        groupMode = LibraryGroupMode.ARTISTS_ALBUMS
                                                        drillArtist = null
                                                        drillAlbum = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (LibraryFilterPrefs.showFoldersInPlaylists && folderGroups.isNotEmpty()) {
                                item(key = "playlist_folders_strip") {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Папки",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(folderGroups.keys.toList(), key = { "folder_chip_$it" }) { folderName ->
                                                val count = folderGroups[folderName]?.size ?: 0
                                                FilterChip(
                                                    selected = false,
                                                    onClick = {
                                                        activePlaylistId = "__FOLDER__:$folderName"
                                                        activePlaylistFilterTitle = folderName
                                                        groupMode = LibraryGroupMode.TRACKS
                                                        drillArtist = null
                                                        drillAlbum = null
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Folder,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    },
                                                    label = { Text("$folderName ($count)") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (LibraryFilterPrefs.showFavoritesPlaylist) {
                                item {
                                    PlaylistSummaryCard(
                                        title = "Избранное",
                                        subtitle = "${favoritesList.size} треков",
                                        coverUri = favoritesList.firstOrNull()?.let { it.artworkUri ?: it.contentUri },
                                        onClick = {
                                            activePlaylistId = "__FAVORITES__"
                                            activePlaylistFilterTitle = "Избранное"
                                            groupMode = LibraryGroupMode.TRACKS
                                            drillArtist = null
                                            drillAlbum = null
                                        }
                                    )
                                }
                            }

                            if (LibraryFilterPrefs.showLosslessPlaylist) {
                                item {
                                    PlaylistSummaryCard(
                                        title = "Lossless / FLAC",
                                        subtitle = "${losslessList.size} треков высокого качества",
                                        coverUri = losslessList.firstOrNull()?.let { it.artworkUri ?: it.contentUri },
                                        onClick = {
                                            activePlaylistId = "__LOSSLESS__"
                                            activePlaylistFilterTitle = "Lossless / FLAC"
                                            groupMode = LibraryGroupMode.TRACKS
                                            drillArtist = null
                                            drillAlbum = null
                                        }
                                    )
                                }
                            }

                            items(playlists, key = { it.id }) { playlist ->
                                val pTracks = remember(tracksById, playlist.trackIds) {
                                    playlist.trackIds.mapNotNull { tracksById[it] }
                                }
                                PlaylistSummaryCard(
                                    title = playlist.name,
                                    subtitle = "${pTracks.size} треков",
                                    coverUri = pTracks.firstOrNull()?.let { it.artworkUri ?: it.contentUri },
                                    onClick = {
                                        activePlaylistId = playlist.id
                                        activePlaylistFilterTitle = playlist.name
                                        groupMode = LibraryGroupMode.TRACKS
                                        drillArtist = null
                                        drillAlbum = null
                                    },
                                    onRename = { onRenamePlaylistClick(playlist) },
                                    onDelete = { onDeletePlaylistClick(playlist) }
                                )
                            }
                        }
                    }

                    2 -> {
                        SettingsScreen(
                            darkTheme = darkTheme,
                            amoledTheme = amoledTheme,
                            playerStyle = playerStyle,
                            lyricsStyle = lyricsStyle,
                            crossfadeSec = crossfadeSec,
                            landscapeNavOnRight = landscapeNavOnRight,
                            hideDuplicates = hideDuplicates,
                            ignoreJunkFolders = ignoreJunkFolders,
                            autoRefreshLibrary = autoRefreshLibrary,
                            minDurationSec = minDurationSec,
                            monetMode = monetMode,
                            paletteStyle = paletteStyle,
                            coverAccent = coverAccent,
                            customSeedColor = customSeedColor,
                            tracksCount = tracks.size,
                            coverCacheSizeMb = coverCacheSizeMb,
                            selectedFolderUris = selectedFolderUris,
                            folderGroups = folderGroups,
                            bottomPaddingDp = listBottomPadding.value.toInt(),
                            onToggleDarkTheme = onSetDarkTheme,
                            onToggleAmoledTheme = onSetAmoledTheme,
                            onSelectPlayerStyle = onSelectPlayerStyle,
                            onSelectLyricsStyle = onSelectLyricsStyle,
                            onSelectCrossfadeSec = onSelectCrossfadeSec,
                            onToggleLandscapeNavOnRight = onSetLandscapeNavOnRight,
                            onToggleHideDuplicates = onSetHideDuplicates,
                            onToggleIgnoreJunkFolders = onSetIgnoreJunkFolders,
                            onToggleAutoRefreshLibrary = onSetAutoRefreshLibrary,
                            onSelectMinDurationSec = onSetMinDurationSec,
                            onSelectMonetMode = onSelectMonetMode,
                        onSelectPaletteStyle = onSelectPaletteStyle,
                            onSelectCustomColor = onSelectCustomColor,
                            onAddFolderClick = onAddFolderClick,
                            onRemoveFolderClick = onRemoveFolderClick,
                            onOpenFolderTracks = { folderName, _ ->
                                activePlaylistId = "__FOLDER__:$folderName"
                                activePlaylistFilterTitle = folderName
                            },
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
                                                FilledTonalButton(
                                                    onClick = onRefreshLibrary,
                                                    modifier = Modifier.weight(1f),
                                                    shape = CircleShape,
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Refresh,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .graphicsLayer { rotationZ = refreshRotation }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Обновить",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1
                                                    )
                                                }

                                                if (filteredTracks.isNotEmpty()) {
                                                    Button(
                                                        onClick = { onShuffleAll(filteredTracks) },
                                                        modifier = Modifier.weight(1f),
                                                        shape = CircleShape,
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Shuffle,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Перемешать все",
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

                            if (drillArtist != null || drillAlbum != null) {
                                item(key = "drill_breadcrumb_card") {
                                    val crumbLabel = when {
                                        drillArtist != null && drillAlbum != null -> "$drillArtist • $drillAlbum"
                                        drillArtist != null -> "Артист: $drillArtist"
                                        else -> "Альбом: $drillAlbum"
                                    }
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (drillAlbum != null && groupMode == LibraryGroupMode.ARTISTS_ALBUMS) {
                                                        drillAlbum = null
                                                        drillArtist = null
                                                    } else if (drillAlbum != null) {
                                                        drillAlbum = null
                                                    } else {
                                                        drillArtist = null
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                    contentDescription = "Назад",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = crumbLabel,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = "${filteredTracks.size} треков",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                            if (filteredTracks.isNotEmpty()) {
                                                FilledTonalButton(
                                                    onClick = { onShuffleAll(filteredTracks) },
                                                    shape = CircleShape,
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Микс", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (activePlaylistFilterTitle != null && filteredTracks.isNotEmpty() && groupMode == LibraryGroupMode.TRACKS) {
                                item(key = "playlist_shuffle_bar") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${filteredTracks.size} треков",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        FilledTonalButton(
                                            onClick = { onShuffleAll(filteredTracks) },
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Перемешать", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            when {
                                groupMode == LibraryGroupMode.ARTISTS && drillArtist == null -> {
                                    items(artistGroups.entries.toList(), key = { "artist_${it.key}" }) { (artistName, artistTracks) ->
                                        val albumCount = artistTracks.map { it.album.trim().ifBlank { "Без альбома" } }.distinct().size
                                        GroupBrowseCard(
                                            title = artistName,
                                            subtitle = "${artistTracks.size} треков • $albumCount альб.",
                                            coverUri = artistTracks.firstOrNull()?.let { it.artworkUri ?: it.contentUri },
                                            fallbackIcon = Icons.Rounded.Person,
                                            onClick = {
                                                drillArtist = artistName
                                                drillAlbum = null
                                            },
                                            onShuffle = { onShuffleAll(artistTracks) }
                                        )
                                    }
                                }

                                groupMode == LibraryGroupMode.ALBUMS && drillAlbum == null -> {
                                    items(albumGroups.entries.toList(), key = { "album_${it.key}" }) { (albumName, albumTracks) ->
                                        val leadArtist = albumTracks.firstOrNull()?.artist?.takeIf { it.isNotBlank() } ?: "Разные исполнители"
                                        val totalDur = albumTracks.sumOf { it.durationMs }
                                        GroupBrowseCard(
                                            title = albumName,
                                            subtitle = "$leadArtist • ${albumTracks.size} треков • ${formatDuration(totalDur)}",
                                            coverUri = albumTracks.firstOrNull()?.let { it.artworkUri ?: it.contentUri },
                                            fallbackIcon = Icons.Rounded.Album,
                                            onClick = {
                                                drillAlbum = albumName
                                                drillArtist = null
                                            },
                                            onShuffle = { onShuffleAll(albumTracks) }
                                        )
                                    }
                                }

                                groupMode == LibraryGroupMode.ARTISTS_ALBUMS && drillArtist == null && drillAlbum == null -> {
                                    items(artistGroups.entries.toList(), key = { "artist_tree_${it.key}" }) { (artistName, artistTracks) ->
                                        ArtistAlbumsTreeCard(
                                            artistName = artistName,
                                            artistTracks = artistTracks,
                                            onOpenAllArtistTracks = {
                                                drillArtist = artistName
                                                drillAlbum = null
                                            },
                                            onOpenAlbum = { albumName ->
                                                drillArtist = artistName
                                                drillAlbum = albumName
                                            }
                                        )
                                    }
                                }

                                else -> {
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

                                        val trackInteractionSource = remember { MutableInteractionSource() }
                                        val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                                        val trackPressScale by animateFloatAsState(
                                            targetValue = if (isTrackPressed) 0.978f else 1f,
                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 540f),
                                            label = "trackRowPressScale"
                                        )
                                        val trackContainerColor by animateColorAsState(
                                            targetValue = if (isSelected) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                            },
                                            animationSpec = tween(220),
                                            label = "trackRowBgColor"
                                        )
                                        val trackTitleColor by animateColorAsState(
                                            targetValue = if (isSelected) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            animationSpec = tween(200),
                                            label = "trackRowTitleColor"
                                        )

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    scaleX = trackPressScale
                                                    scaleY = trackPressScale
                                                }
                                                .clip(RoundedCornerShape(20.dp))
                                                .combinedClickable(
                                                    interactionSource = trackInteractionSource,
                                                    indication = LocalIndication.current,
                                                    onClick = { onSelectTrack(index, filteredTracks) },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        trackMenuTarget = track
                                                    }
                                                ),
                                            color = trackContainerColor
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
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
                                                        color = trackTitleColor
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val metaText = buildString {
                                                        append(track.artist)
                                                        if (LibraryFilterPrefs.showAlbumSubtitle && track.album.isNotBlank()) {
                                                            append(" • ")
                                                            append(track.album)
                                                        }
                                                        if (track.durationMs > 0L) {
                                                            append(" • ")
                                                            append(formatDuration(track.durationMs))
                                                        }
                                                    }
                                                    Text(
                                                        text = metaText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                AnimatedVisibility(
                                                    visible = isSelected,
                                                    enter = fadeIn(tween(180)) + scaleIn(
                                                        initialScale = 0.72f,
                                                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f)
                                                    ),
                                                    exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.72f)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        MiniEqualizerBars(
                                                            isPlaying = isPlaying,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
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

                                                IconButton(
                                                    onClick = { trackMenuTarget = track },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.MoreVert,
                                                        contentDescription = "Действия с треком",
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                AnimatedContent(
                                    targetState = miniCoverRequest to currentTrack.id,
                                    transitionSpec = { fadeIn(tween(260)).togetherWith(fadeOut(tween(180))) },
                                    label = "miniCoverFade"
                                ) { (req, _) ->
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
                                            model = req,
                                            contentDescription = currentTrack.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                AnimatedContent(
                                    targetState = currentTrack,
                                    contentKey = { it.id },
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

                                val miniPlayCorner by animateFloatAsState(
                                    targetValue = if (isPlaying) 14f else 22f,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
                                    label = "miniPlayCorner"
                                )
                                val miniPlayInteraction = remember { MutableInteractionSource() }
                                val miniPlayPressed by miniPlayInteraction.collectIsPressedAsState()
                                val miniPlayScale by animateFloatAsState(
                                    targetValue = if (miniPlayPressed) 0.88f else 1f,
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 460f),
                                    label = "miniPlayBounce"
                                )
                                FilledIconButton(
                                    onClick = onTogglePlayPause,
                                    interactionSource = miniPlayInteraction,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .graphicsLayer { scaleX = miniPlayScale; scaleY = miniPlayScale },
                                    shape = RoundedCornerShape(miniPlayCorner.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    AnimatedContent(
                                        targetState = isPlaying,
                                        transitionSpec = {
                                            (scaleIn(initialScale = 0.65f, animationSpec = tween(160)) + fadeIn(tween(150)))
                                                .togetherWith(scaleOut(targetScale = 0.65f, animationSpec = tween(120)) + fadeOut(tween(120)))
                                        },
                                        label = "miniPlayIconMorph"
                                    ) { playing ->
                                        Icon(
                                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (playing) "Пауза" else "Воспроизвести"
                                        )
                                    }
                                }
                            }

                            val smoothMiniProgress by animateFloatAsState(
                                targetValue = playbackProgress.coerceIn(0f, 1f),
                                animationSpec = tween(durationMillis = 120, easing = LinearEasing),
                                label = "miniProgressSmooth"
                            )
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
                                        .fillMaxWidth(smoothMiniProgress)
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
                                    activePlaylistId = null
                                    activePlaylistFilterTitle = null
                                    drillArtist = null
                                    drillAlbum = null
                                    groupMode = LibraryGroupMode.TRACKS
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
                                    drillArtist = null
                                    drillAlbum = null
                                    groupMode = LibraryGroupMode.TRACKS
                                    selectedTab = 1
                                }
                            )
                            NavBarItem(
                                selected = selectedTab == 2 && activePlaylistFilterTitle == null,
                                icon = Icons.Rounded.Settings,
                                label = "Настройки",
                                onClick = {
                                    activePlaylistId = null
                                    activePlaylistFilterTitle = null
                                    drillArtist = null
                                    drillAlbum = null
                                    selectedTab = 2
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
                val memberPlaylists = playlists.filter { menuTrack.id in it.trackIds }
                if (currentCustomPlaylist != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onRemoveTrackFromPlaylist(menuTrack, currentCustomPlaylist.id)
                                trackMenuTarget = null
                            },
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistRemove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Убрать из плейлиста «${currentCustomPlaylist.name}»",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Файл останется на устройстве",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                                )
                            }
                        }
                    }
                } else if (memberPlaylists.isNotEmpty()) {
                    memberPlaylists.forEach { pl ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    onRemoveTrackFromPlaylist(menuTrack, pl.id)
                                    trackMenuTarget = null
                                },
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlaylistRemove,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Убрать из плейлиста «${pl.name}»",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Без удаления файла с устройства",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFav) "Убрать из избранного" else "Добавить в избранное",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isFav) {
                                Text(
                                    text = "Файл останется на устройстве",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                            pendingDeleteConfirmTrack = menuTrack
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

    val confirmDeleteTrack = pendingDeleteConfirmTrack
    if (confirmDeleteTrack != null) {
        val activeCustom = playlists.firstOrNull { it.id == activePlaylistId }
        AlertDialog(
            onDismissRequest = { pendingDeleteConfirmTrack = null },
            title = { Text("Удалить файл с устройства?") },
            text = {
                Text(
                    "Трек «${confirmDeleteTrack.title}» будет безвозвратно удалён из памяти смартфона.\n" +
                        if (activeCustom != null) "Чтобы убрать его только из «${activeCustom.name}», нажмите «Только из плейлиста»."
                        else "Чтобы не удалять файл с устройства, используйте «Убрать из плейлиста» или «Убрать из избранного»."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTrackFromDevice(confirmDeleteTrack)
                        pendingDeleteConfirmTrack = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Удалить файл")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeCustom != null) {
                        TextButton(
                            onClick = {
                                onRemoveTrackFromPlaylist(confirmDeleteTrack, activeCustom.id)
                                pendingDeleteConfirmTrack = null
                            }
                        ) {
                            Text("Только из плейлиста")
                        }
                    }
                    TextButton(onClick = { pendingDeleteConfirmTrack = null }) {
                        Text("Отмена")
                    }
                }
            }
        )
    }

    if (showPlaylistDisplayConfigSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistDisplayConfigSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Фильтры и отображение",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Настройте какие группировки и разделы видны в Плейлистах и внутри списков.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PlaylistConfigSwitchRow(
                    title = "Панель фильтров в списках",
                    subtitle = "Чипы по артистам, альбомам и форматам",
                    checked = LibraryFilterPrefs.showFilterBar,
                    onCheckedChange = { LibraryFilterPrefs.setShowFilterBar(context, it) }
                )
                PlaylistConfigSwitchRow(
                    title = "По артистам",
                    checked = LibraryFilterPrefs.showArtistsFilter,
                    onCheckedChange = { LibraryFilterPrefs.setShowArtistsFilter(context, it) }
                )
                PlaylistConfigSwitchRow(
                    title = "По альбомам",
                    checked = LibraryFilterPrefs.showAlbumsFilter,
                    onCheckedChange = { LibraryFilterPrefs.setShowAlbumsFilter(context, it) }
                )
                PlaylistConfigSwitchRow(
                    title = "Артисты и их альбомы",
                    checked = LibraryFilterPrefs.showArtistsAlbumsFilter,
                    onCheckedChange = { LibraryFilterPrefs.setShowArtistsAlbumsFilter(context, it) }
                )
                PlaylistConfigSwitchRow(
                    title = "Папки на экране плейлистов",
                    checked = LibraryFilterPrefs.showFoldersInPlaylists,
                    onCheckedChange = { LibraryFilterPrefs.setShowFoldersInPlaylists(context, it) }
                )
                PlaylistConfigSwitchRow(
                    title = "Показывать «Избранное»",
                    checked = LibraryFilterPrefs.showFavoritesPlaylist,
                    onCheckedChange = { LibraryFilterPrefs.setShowFavoritesPlaylist(context, it) }
                )
                PlaylistConfigSwitchRow(
                    title = "Показывать «Lossless / FLAC»",
                    checked = LibraryFilterPrefs.showLosslessPlaylist,
                    onCheckedChange = { LibraryFilterPrefs.setShowLosslessPlaylist(context, it) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistQuickFilterCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 500f),
        label = "quickFilterCardScale"
    )
    Surface(
        modifier = Modifier
            .widthIn(min = 128.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GroupBrowseCard(
    title: String,
    subtitle: String,
    coverUri: Uri?,
    fallbackIcon: ImageVector,
    onClick: () -> Unit,
    onShuffle: () -> Unit
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "groupBrowseCardScale"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fallbackIcon,
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onShuffle) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Перемешать",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ArtistAlbumsTreeCard(
    artistName: String,
    artistTracks: List<LocalTrack>,
    onOpenAllArtistTracks: () -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    val context = LocalContext.current
    val albumsMap = remember(artistTracks) {
        artistTracks
            .groupBy { it.album.trim().ifBlank { "Без альбома" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${albumsMap.size} альб. • ${artistTracks.size} треков",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onOpenAllArtistTracks,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Все треки", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                albumsMap.entries.forEach { (albumName, albumTracks) ->
                    val coverUri = albumTracks.firstOrNull()?.let { it.artworkUri ?: it.contentUri }
                    val request = remember(coverUri) {
                        coverUri?.let {
                            ImageRequest.Builder(context).data(it).size(180).build()
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .width(126.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenAlbum(albumName) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Album,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (request != null) {
                                    AsyncImage(
                                        model = request,
                                        contentDescription = albumName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Text(
                                text = albumName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${albumTracks.size} треков",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistConfigSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "playlistSummaryCardScale"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
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
    val pillBgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = tween(200),
        label = "navPillBg"
    )
    val contentTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(180),
        label = "navPillTint"
    )

    Column(
        modifier = Modifier
            .widthIn(min = 72.dp)
            .graphicsLayer {
                scaleX = pillScale
                scaleY = pillScale
            }
            .clip(CircleShape)
            .background(pillBgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = contentTint
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            color = contentTint
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
