package com.example.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
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
import com.example.musicplayer.ui.theme.MonetMode
import com.example.musicplayer.ui.theme.MusicPlayerTheme
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

            var playlists by remember {
                mutableStateOf(loadPlaylistsFromPrefs(prefs.getString("playlists_json", null)))
            }

            var showCreatePlaylistDialog by remember { mutableStateOf(false) }
            var newPlaylistName by remember { mutableStateOf("") }
            var renamingPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
            var renameText by remember { mutableStateOf("") }
            var showAddToPlaylistDialog by remember { mutableStateOf(false) }
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

            fun triggerScan(folders: List<Uri> = selectedFolderUris, forceClearCovers: Boolean = false) {
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
                            scanner.scanTracks(folders) { count, msg ->
                                scannedFilesCount = count
                                scanStatusText = msg
                            }
                        }
                        if (result != null) {
                            tracks = result.tracks
                            if (activeQueue.isEmpty()) activeQueue = result.tracks
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
                    }
                }
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
                            currentTrack = tracks.firstOrNull { it.id == activeId } ?: currentTrack
                        }
                    }

                    mc.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                            if (playing && mc.duration > 0L) {
                                durationMs = mc.duration
                            }
                            positionMs = mc.currentPosition.coerceAtLeast(0L)
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            val activeId = mediaItem?.mediaId?.toLongOrNull()
                            if (activeId != null) {
                                currentTrack = tracks.firstOrNull { it.id == activeId } ?: currentTrack
                            }
                            durationMs = mc.duration.coerceAtLeast(0L)
                            positionMs = mc.currentPosition.coerceAtLeast(0L)
                        }

                        override fun onTracksChanged(playerTracks: Tracks) {
                            val active = currentTrack ?: return
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

                    syncPlayerState()
                }, MoreExecutors.directExecutor())
            }

            LaunchedEffect(isPlaying, isNowPlayingOpen, controller) {
                while (isPlaying && isNowPlayingOpen) {
                    val mc = controller
                    if (mc != null) {
                        positionMs = mc.currentPosition.coerceAtLeast(0L)
                        if (mc.duration > 0L) {
                            durationMs = mc.duration
                        }
                    }
                    delay(400L)
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
                val items = queue.map { track ->
                    MediaItem.Builder()
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

            BackHandler(enabled = isNowPlayingOpen) {
                isNowPlayingOpen = false
            }

            MusicPlayerTheme(
                darkTheme = darkTheme,
                amoledTheme = amoledTheme,
                monetMode = monetMode,
                coverAccent = coverAccent,
                customSeedColor = customSeedColor
            ) {
                val activeTrack = currentTrack
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = isNowPlayingOpen && activeTrack != null,
                        modifier = Modifier.zIndex(1f),
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeIn(animationSpec = tween(durationMillis = 220)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(durationMillis = 260)
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
                        onClose = { isNowPlayingOpen = false },
                        onTogglePlayPause = {
                            controller?.let { mc ->
                                if (mc.isPlaying) mc.pause() else mc.play()
                            }
                        },
                        onToggleFavorite = {
                            val updated = if (activeTrack.id in favoriteIds) {
                                favoriteIds - activeTrack.id
                            } else {
                                favoriteIds + activeTrack.id
                            }
                            saveFavorites(updated)
                        },
                        onAddToPlaylist = {
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
                        onPrevious = { controller?.seekToPrevious() },
                        onNext = { controller?.seekToNext() },
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
                        darkTheme = darkTheme,
                        amoledTheme = amoledTheme,
                        landscapeNavOnRight = landscapeNavOnRight,
                        monetMode = monetMode,
                        customSeedColor = customSeedColor,
                        onSetDarkTheme = ::saveThemePref,
                        onSetAmoledTheme = ::saveAmoledPref,
                        onSetLandscapeNavOnRight = ::saveLandscapeNavRightPref,
                        onSelectMonetMode = ::saveMonetPref,
                        onSelectCustomColor = ::saveCustomColorPref,
                        onCycleMonetMode = ::cycleMonet,
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
                                        val initialIds = activeTrack?.id?.let { listOf(it) } ?: emptyList()
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

                if (showAddToPlaylistDialog && activeTrack != null) {
                    AlertDialog(
                        onDismissRequest = { showAddToPlaylistDialog = false },
                        title = { Text("Добавить в плейлист") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                playlists.forEach { playlist ->
                                    val alreadyIn = activeTrack.id in playlist.trackIds
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                val updatedList = playlists.map { item ->
                                                    if (item.id == playlist.id) {
                                                        val newIds = if (alreadyIn) {
                                                            item.trackIds - activeTrack.id
                                                        } else {
                                                            item.trackIds + activeTrack.id
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
                        sampleRateHz = obj.optInt("sampleRateHz", 44100)
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
