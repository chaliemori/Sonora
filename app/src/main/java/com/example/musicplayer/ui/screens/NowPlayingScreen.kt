package com.example.musicplayer.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.audio.AudioEffectsController
import com.example.musicplayer.data.LocalTrack
import com.example.musicplayer.data.LyricsLookupResult
import com.example.musicplayer.data.LyricsRepository
import com.example.musicplayer.data.TrackLyrics
import com.example.musicplayer.ui.components.EqualizerBottomSheet
import com.example.musicplayer.ui.theme.MonetMode
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    track: LocalTrack,
    queue: List<LocalTrack>,
    isPlaying: Boolean,
    isFavorite: Boolean,
    sleepTimerMinutes: Int?,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    monetMode: MonetMode,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onRemoveQueueIndex: (Int) -> Unit = {},
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekBy: (Long) -> Unit = {},
    onSeekToMs: (Long) -> Unit = {},
    onSeekToFraction: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onCycleMonetMode: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsState by remember(track.id) { mutableStateOf<TrackLyrics?>(null) }
    var lyricsNetworkError by remember(track.id) { mutableStateOf(false) }
    var isLoadingLyrics by remember(track.id) { mutableStateOf(false) }
    var hasTriedLyricsForTrack by remember(track.id) { mutableStateOf(false) }
    var showLyricsSearchDialog by remember { mutableStateOf(false) }
    var manualLyricsQuery by remember { mutableStateOf("") }
    var transitionDir by remember { mutableIntStateOf(1) } // 1 = следующий (влево уходит, справа летит), -1 = предыдущий

    LaunchedEffect(track.id) {
        isLoadingLyrics = true
        val res = LyricsRepository.searchLyrics(context, track)
        lyricsState = res.lyrics
        lyricsNetworkError = res.hadNetworkError
        hasTriedLyricsForTrack = true
        isLoadingLyrics = false
    }

    LaunchedEffect(showLyrics, track.id) {
        if (showLyrics && !isLoadingLyrics && lyricsState == null && (!hasTriedLyricsForTrack || lyricsNetworkError)) {
            isLoadingLyrics = true
            val res = LyricsRepository.searchLyrics(context, track, forceRefresh = true)
            lyricsState = res.lyrics
            lyricsNetworkError = res.hadNetworkError
            hasTriedLyricsForTrack = true
            isLoadingLyrics = false
        }
    }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val playbackFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayedFraction = if (isDraggingSlider) dragFraction else playbackFraction

    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coverScale"
    )

    val playButtonCorner by animateFloatAsState(
        targetValue = if (isPlaying) 26f else 42f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "playCornerMorph"
    )

    val shuffleTint by animateColorAsState(
        targetValue = if (shuffleEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "shuffleTint"
    )

    val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
    val repeatTint by animateColorAsState(
        targetValue = if (isRepeatActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "repeatTint"
    )

    val coverRequest = remember(track.artworkUri, track.contentUri) {
        ImageRequest.Builder(context)
            .data(track.artworkUri ?: track.contentUri)
            .size(720)
            .build()
    }

    val sampleRateKhz = remember(track.sampleRateHz) {
        when {
            track.sampleRateHz >= 1_000_000 ->
                String.format(Locale.US, "%.1f MHz", track.sampleRateHz / 1_000_000.0)
            track.sampleRateHz % 1000 == 0 ->
                "${track.sampleRateHz / 1000} kHz"
            else ->
                String.format(Locale.US, "%.1f kHz", track.sampleRateHz / 1000.0)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isCompactPortrait = !isLandscape && (configuration.screenHeightDp < 740 || configuration.screenWidthDp < 370)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dismissThresholdPx = with(density) { 120.dp.toPx() }
    val dismissFlingVelocityPx = with(density) { 800.dp.toPx() }

    val dismissOffsetY = remember { Animatable(0f) }
    val dismissProgress = (dismissOffsetY.value / dismissThresholdPx).coerceIn(0f, 1f)

    val safeQueue = remember(queue, track) { if (queue.isNotEmpty()) queue else listOf(track) }
    val currentQueueIdx = remember(safeQueue, track.id) {
        safeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = currentQueueIdx,
        pageCount = { safeQueue.size }
    )
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(track.id, safeQueue.size) {
        if (currentQueueIdx != pagerState.currentPage) {
            isProgrammaticScroll = true
            try {
                pagerState.animateScrollToPage(
                    page = currentQueueIdx,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val settled = pagerState.settledPage
        if (!isProgrammaticScroll && settled != currentQueueIdx && settled in safeQueue.indices) {
            transitionDir = if (settled > currentQueueIdx) 1 else -1
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectQueueIndex(settled)
        }
    }

    // Внешний неподвижный Box принимает жесты, чтобы смещение Surface не сбивало координаты
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        scope.launch {
                            dismissOffsetY.snapTo((dismissOffsetY.value + dragAmount).coerceAtLeast(0f))
                        }
                    },
                    onDragEnd = {
                        val velocityY = velocityTracker.calculateVelocity().y
                        if (dismissOffsetY.value >= dismissThresholdPx || velocityY >= dismissFlingVelocityPx) {
                            onClose()
                        } else {
                            scope.launch {
                                dismissOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dismissOffsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                )
            }
    ) {
        val glowColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.primary.copy(alpha = if (isPlaying) 0.16f else 0.08f),
            animationSpec = tween(durationMillis = 600),
            label = "ambientGlow"
        )
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dismissOffsetY.value
                    val scale = 1f - dismissProgress * 0.04f
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - dismissProgress * 0.18f
                },
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = Offset(540f, 420f),
                            radius = 950f
                        )
                    )
            ) {
        val closeButton: @Composable () -> Unit = {
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier.size(if (isLandscape) 42.dp else 46.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Закрыть плеер",
                    modifier = Modifier.size(if (isLandscape) 20.dp else 22.dp)
                )
            }
        }

        val paletteButton: @Composable () -> Unit = {
            FilledTonalIconButton(
                onClick = onCycleMonetMode,
                modifier = Modifier.size(if (isLandscape) 42.dp else 46.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = "Monet: ${monetMode.title}",
                    modifier = Modifier.size(if (isLandscape) 20.dp else 22.dp)
                )
            }
        }

        val formatBadge: @Composable (Modifier) -> Unit = { badgeMod ->
            Box(
                modifier = badgeMod,
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showEqSheet = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "${track.formatLabel} • ${track.bitrateKbps} kbps • $sampleRateKhz",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        val topBar: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                closeButton()
                formatBadge(Modifier.weight(1f))
                paletteButton()
            }
        }

        val coverOrLyrics: @Composable (Modifier) -> Unit = { boxModifier ->
            AnimatedContent(
                targetState = showLyrics,
                modifier = boxModifier,
                transitionSpec = {
                    (fadeIn(tween(240)) + scaleIn(initialScale = 0.96f, animationSpec = tween(240)))
                        .togetherWith(fadeOut(tween(160)))
                },
                label = "coverLyricsSwitch"
            ) { lyricsVisible ->
                if (!lyricsVisible) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        pageSpacing = 16.dp,
                        beyondViewportPageCount = 1
                    ) { page ->
                        val pageTrack = safeQueue[page]
                        val pageRequest = remember(pageTrack.artworkUri, pageTrack.contentUri) {
                            ImageRequest.Builder(context)
                                .data(pageTrack.artworkUri ?: pageTrack.contentUri)
                                .size(720)
                                .build()
                        }
                        val pageOffset = kotlin.math.abs(
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).coerceIn(0f, 1f)
                        val pageScale = 1f - pageOffset * 0.08f
                        val activeScale = if (page == currentQueueIdx) coverScale else 1f

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = activeScale * pageScale
                                    scaleY = activeScale * pageScale
                                    alpha = 1f - pageOffset * 0.25f
                                }
                                .clip(RoundedCornerShape(if (isLandscape) 26.dp else 32.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { offset ->
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (offset.x < size.width / 2f) {
                                                onSeekBy(-10_000L)
                                            } else {
                                                onSeekBy(10_000L)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(if (isLandscape) 64.dp else 88.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            AsyncImage(
                                model = pageRequest,
                                contentDescription = pageTrack.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(if (isLandscape) 26.dp else 32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when {
                                        isLoadingLyrics -> "Ищем..."
                                        lyricsState?.isSynced == true -> "Текст • ${lyricsState?.source} (синхр.)"
                                        lyricsState != null -> "Текст • ${lyricsState?.source}"
                                        lyricsNetworkError -> "Нет связи с серверами текста"
                                        else -> "Текст не найден"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val cleanArtist = track.artist
                                                .takeUnless { it.equals("Неизвестный исполнитель", ignoreCase = true) }
                                                .orEmpty()
                                            manualLyricsQuery = listOf(cleanArtist, track.title)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" - ")
                                            showLyricsSearchDialog = true
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = "Найти текст вручную",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                isLoadingLyrics = true
                                                val res = LyricsRepository.searchLyrics(context, track, forceRefresh = true)
                                                lyricsState = res.lyrics
                                                lyricsNetworkError = res.hadNetworkError
                                                hasTriedLyricsForTrack = true
                                                isLoadingLyrics = false
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Обновить текст",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val currentLyrics = lyricsState
                            when {
                                isLoadingLyrics -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Ищем...",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                currentLyrics == null || currentLyrics.isEmpty -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (lyricsNetworkError) {
                                                "Нет связи с серверами текста.\nПроверьте интернет или VPN."
                                            } else {
                                                "Текст не найден в LRCMux, LRCLIB и KuGou"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilledTonalButton(
                                                onClick = {
                                                    scope.launch {
                                                        isLoadingLyrics = true
                                                        val res = LyricsRepository.searchLyrics(context, track, forceRefresh = true)
                                                        lyricsState = res.lyrics
                                                        lyricsNetworkError = res.hadNetworkError
                                                        hasTriedLyricsForTrack = true
                                                        isLoadingLyrics = false
                                                    }
                                                },
                                                shape = CircleShape
                                            ) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Повторить")
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    val cleanArtist = track.artist
                                                        .takeUnless { it.equals("Неизвестный исполнитель", ignoreCase = true) }
                                                        .orEmpty()
                                                    manualLyricsQuery = listOf(cleanArtist, track.title)
                                                        .filter { it.isNotBlank() }
                                                        .joinToString(" - ")
                                                    showLyricsSearchDialog = true
                                                },
                                                shape = CircleShape
                                            ) {
                                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Вручную")
                                            }
                                        }
                                    }
                                }
                                currentLyrics.isSynced -> {
                                    val synced = currentLyrics.synced
                                    val activeLineIdx = remember(synced, positionMs) {
                                        synced.indexOfLast { it.timeMs <= positionMs + 150L }
                                    }
                                    val lyricsListState = rememberLazyListState()
                                    LaunchedEffect(activeLineIdx) {
                                        if (activeLineIdx >= 0) {
                                            lyricsListState.animateScrollToItem(
                                                index = (activeLineIdx - 2).coerceAtLeast(0)
                                            )
                                        }
                                    }
                                    LazyColumn(
                                        state = lyricsListState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(vertical = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        itemsIndexed(synced, key = { idx, item -> "${idx}_${item.timeMs}" }) { index, line ->
                                            val isActive = index == activeLineIdx
                                            val lineAlpha by animateFloatAsState(
                                                targetValue = if (isActive) 1f else 0.42f,
                                                animationSpec = tween(220),
                                                label = "lyricAlpha"
                                            )
                                            val lineScale by animateFloatAsState(
                                                targetValue = if (isActive) 1.03f else 1f,
                                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 360f),
                                                label = "lyricScale"
                                            )
                                            Text(
                                                text = line.text,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isActive) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onSeekToMs(line.timeMs)
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                                    .graphicsLayer {
                                                        alpha = lineAlpha
                                                        scaleX = lineScale
                                                        scaleY = lineScale
                                                    }
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Text(
                                        text = currentLyrics.plain.orEmpty(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val trackInfoRow: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = track.id,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val dir = transitionDir
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            initialOffsetX = { (it * 0.25f * dir).toInt() }
                        ) + fadeIn(tween(220)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                    targetOffsetX = { (-it * 0.25f * dir).toInt() }
                                ) + fadeOut(tween(160))
                            )
                    },
                    label = "trackTextSlide"
                ) { _ ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = track.title,
                            style = if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${track.artist} • ${track.folderName}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isLandscape) {
                        paletteButton()
                    }
                    FilledTonalIconButton(
                        onClick = { showLyrics = !showLyrics },
                        modifier = Modifier.size(if (isLandscape) 42.dp else 46.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (showLyrics) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (showLyrics) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Текст песни",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    val eqActive = AudioEffectsController.eqEnabled
                    FilledTonalIconButton(
                        onClick = { showEqSheet = true },
                        modifier = Modifier.size(if (isLandscape) 42.dp else 46.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (eqActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (eqActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Эквалайзер",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    val heartScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "heartBounce"
                    )
                    FilledTonalIconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(if (isLandscape) 42.dp else 46.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Избранное",
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = heartScale
                                    scaleY = heartScale
                                }
                        )
                    }
                }
            }
        }

        val seekSection: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SquigglyProgressBar(
                    progress = displayedFraction,
                    isPlaying = isPlaying,
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    onSeekStart = { fraction ->
                        isDraggingSlider = true
                        dragFraction = fraction
                    },
                    onSeekChange = { fraction ->
                        dragFraction = fraction
                    },
                    onSeekEnd = {
                        onSeekToFraction(dragFraction)
                        isDraggingSlider = false
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val shownMs = if (isDraggingSlider) {
                        (safeDuration * dragFraction).toLong()
                    } else {
                        positionMs
                    }
                    Text(
                        text = formatDuration(shownMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val playbackControlsRow: @Composable () -> Unit = {
            val prevNextSize = if (isLandscape || isCompactPortrait) 50.dp else 58.dp
            val playSize = if (isLandscape || isCompactPortrait) 68.dp else 80.dp
            val playCorner = if (isLandscape || isCompactPortrait) (playButtonCorner * 0.84f) else playButtonCorner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Перемешать",
                        tint = shuffleTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isLandscape || isCompactPortrait) 12.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val prevInteraction = remember { MutableInteractionSource() }
                    val prevPressed by prevInteraction.collectIsPressedAsState()
                    val prevScale by animateFloatAsState(
                        targetValue = if (prevPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                        label = "prevBounce"
                    )
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            transitionDir = -1
                            onPrevious()
                        },
                        interactionSource = prevInteraction,
                        modifier = Modifier
                            .size(prevNextSize)
                            .graphicsLayer { scaleX = prevScale; scaleY = prevScale },
                        shape = RoundedCornerShape(if (isLandscape || isCompactPortrait) 18.dp else 22.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Предыдущий",
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    val playInteraction = remember { MutableInteractionSource() }
                    val playPressed by playInteraction.collectIsPressedAsState()
                    val playScale by animateFloatAsState(
                        targetValue = if (playPressed) 0.90f else 1f,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = 450f),
                        label = "playBounce"
                    )
                    FilledIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePlayPause()
                        },
                        interactionSource = playInteraction,
                        modifier = Modifier
                            .size(playSize)
                            .graphicsLayer { scaleX = playScale; scaleY = playScale },
                        shape = RoundedCornerShape(playCorner.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.65f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f)) + fadeIn(tween(150)))
                                    .togetherWith(scaleOut(targetScale = 0.65f, animationSpec = tween(120)) + fadeOut(tween(120)))
                            },
                            label = "playPauseIconMorph"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Пауза" else "Играть",
                                modifier = Modifier.size(if (isLandscape || isCompactPortrait) 32.dp else 36.dp)
                            )
                        }
                    }

                    val nextInteraction = remember { MutableInteractionSource() }
                    val nextPressed by nextInteraction.collectIsPressedAsState()
                    val nextScale by animateFloatAsState(
                        targetValue = if (nextPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                        label = "nextBounce"
                    )
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            transitionDir = 1
                            onNext()
                        },
                        interactionSource = nextInteraction,
                        modifier = Modifier
                            .size(prevNextSize)
                            .graphicsLayer { scaleX = nextScale; scaleY = nextScale },
                        shape = RoundedCornerShape(if (isLandscape || isCompactPortrait) 18.dp else 22.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Следующий",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onCycleRepeatMode,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = "Повтор",
                        tint = repeatTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        val actionPillsRow: @Composable () -> Unit = {
            val pillHeight = if (isLandscape) 42.dp else 48.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isLandscape) 8.dp else 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier
                        .weight(1f)
                        .height(pillHeight),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Плейлист",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                val timerActive = sleepTimerMinutes != null
                FilledTonalButton(
                    onClick = onCycleSleepTimer,
                    modifier = Modifier
                        .weight(1f)
                        .height(pillHeight),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = if (timerActive) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sleepTimerMinutes != null) "${sleepTimerMinutes} мин" else "Таймер",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                FilledTonalIconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.size(pillHeight),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Очередь воспроизведения",
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isLandscape) {
                    closeButton()
                }
            }
        }

        if (isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                    ) {
                        formatBadge(Modifier.fillMaxWidth())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            contentAlignment = Alignment.Center
                        ) {
                            coverOrLyrics(
                                Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        trackInfoRow()
                        seekSection()
                        playbackControlsRow()
                        actionPillsRow()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = if (isCompactPortrait) 18.dp else 22.dp,
                        vertical = if (isCompactPortrait) 8.dp else 12.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isCompactPortrait) 6.dp else 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                    topBar()
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = if (isCompactPortrait) 8.dp else 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val coverSize = minOf(maxWidth, maxHeight)
                    coverOrLyrics(Modifier.size(coverSize))
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactPortrait) 10.dp else 14.dp)
                ) {
                    trackInfoRow()
                    seekSection()
                    playbackControlsRow()
                    actionPillsRow()
                }
            }
        }
            }
        }
    }

    if (showEqSheet) {
        EqualizerBottomSheet(onDismiss = { showEqSheet = false })
    }

    if (showLyricsSearchDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsSearchDialog = false },
            title = { Text("Поиск текста") },
            text = {
                OutlinedTextField(
                    value = manualLyricsQuery,
                    onValueChange = { manualLyricsQuery = it },
                    singleLine = true,
                    placeholder = { Text("Исполнитель - Название") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val q = manualLyricsQuery.trim()
                        showLyricsSearchDialog = false
                        if (q.isNotEmpty()) {
                            scope.launch {
                                isLoadingLyrics = true
                                val res = LyricsRepository.searchLyrics(
                                    context = context,
                                    track = track,
                                    forceRefresh = true,
                                    customQuery = q
                                )
                                lyricsState = res.lyrics
                                lyricsNetworkError = res.hadNetworkError
                                hasTriedLyricsForTrack = true
                                isLoadingLyrics = false
                            }
                        }
                    }
                ) {
                    Text("Найти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLyricsSearchDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showQueueSheet) {
        val queueListState = rememberLazyListState()
        val activeIndex = remember(queue, track.id) {
            queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        }
        LaunchedEffect(showQueueSheet) {
            if (activeIndex > 0) {
                queueListState.scrollToItem(activeIndex)
            }
        }
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Очередь (${queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    state = queueListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(queue, key = { idx, item -> "${item.id}_$idx" }) { index, item ->
                        val active = item.id == track.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    onSelectQueueIndex(index)
                                    showQueueSheet = false
                                },
                            color = if (active) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (active) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatDuration(item.durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!active) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onRemoveQueueIndex(index) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Убрать из очереди",
                                            modifier = Modifier.size(16.dp),
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

@Composable
private fun SquigglyProgressBar(
    progress: Float,
    isPlaying: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onSeekStart: (Float) -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekEnd: () -> Unit
) {
    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying) 4.5f else 0f,
        animationSpec = spring(stiffness = 220f),
        label = "waveAmp"
    )

    val phase by if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "wavePhase")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavePhaseValue"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onSeekStart(fraction)
                    onSeekEnd()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val fraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onSeekStart(fraction)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onSeekChange(fraction)
                    },
                    onDragEnd = { onSeekEnd() },
                    onDragCancel = { onSeekEnd() }
                )
            }
    ) {
        val w = size.width
        val centerY = size.height / 2f
        val thumbX = (w * progress.coerceIn(0f, 1f))
        val strokePx = 3.2.dp.toPx()
        val ampPx = amplitude.dp.toPx()
        val wavelengthPx = 28.dp.toPx()

        if (thumbX < w) {
            drawLine(
                color = inactiveColor,
                start = Offset(thumbX, centerY),
                end = Offset(w, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }

        if (thumbX > 0f) {
            val path = Path()
            var x = 0f
            val step = 2.dp.toPx()
            while (x <= thumbX) {
                val angle = (x / wavelengthPx) * (2f * Math.PI).toFloat() + phase
                val y = centerY + sin(angle) * ampPx
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += step
            }
            val endAngle = (thumbX / wavelengthPx) * (2f * Math.PI).toFloat() + phase
            path.lineTo(thumbX, centerY + sin(endAngle) * ampPx)

            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        val thumbWidth = 4.dp.toPx()
        val thumbHeight = 16.dp.toPx()
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(
                (thumbX - thumbWidth / 2f).coerceIn(0f, w - thumbWidth),
                centerY - thumbHeight / 2f
            ),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
        )
    }
}
