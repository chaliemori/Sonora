package com.example.musicplayer.ui.screens

import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay10
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.audio.AudioEffectsController
import com.example.musicplayer.data.LocalTrack
import com.example.musicplayer.data.LyricLine
import com.example.musicplayer.data.LyricsLookupResult
import com.example.musicplayer.data.LyricsRepository
import com.example.musicplayer.data.TrackLyrics
import com.example.musicplayer.ui.components.EqualizerBottomSheet
import com.example.musicplayer.ui.theme.LyricsStyle
import com.example.musicplayer.ui.theme.MonetMode
import com.example.musicplayer.ui.theme.NowPlayingStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    playerStyle: NowPlayingStyle = NowPlayingStyle.CARD,
    lyricsStyle: LyricsStyle = LyricsStyle.APPLE_MUSIC,
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
    var lyricAlternatives by remember(track.id) { mutableStateOf<List<TrackLyrics>>(emptyList()) }
    var activeAltIndex by remember(track.id) { mutableIntStateOf(0) }
    var lyricsNetworkError by remember(track.id) { mutableStateOf(false) }
    var isLoadingLyrics by remember(track.id) { mutableStateOf(false) }
    var hasTriedLyricsForTrack by remember(track.id) { mutableStateOf(false) }
    var showLyricsSearchDialog by remember { mutableStateOf(false) }
    var manualLyricsQuery by remember { mutableStateOf("") }
    var transitionDir by remember { mutableIntStateOf(1) } // 1 = следующий (влево уходит, справа летит), -1 = предыдущий

    suspend fun loadLyrics(forceRefresh: Boolean, customQuery: String? = null) {
        isLoadingLyrics = true
        val res = LyricsRepository.searchLyrics(
            context = context,
            track = track,
            forceRefresh = forceRefresh,
            customQuery = customQuery
        )
        lyricsState = res.lyrics
        lyricAlternatives = res.alternatives
        activeAltIndex = 0
        lyricsNetworkError = res.hadNetworkError
        hasTriedLyricsForTrack = true
        isLoadingLyrics = false
    }

    LaunchedEffect(track.id) {
        loadLyrics(forceRefresh = false)
    }

    LaunchedEffect(showLyrics, track.id) {
        if (showLyrics && !isLoadingLyrics && lyricsState == null && (!hasTriedLyricsForTrack || lyricsNetworkError)) {
            loadLyrics(forceRefresh = true)
        }
    }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val playbackFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayedFraction = if (isDraggingSlider) dragFraction else playbackFraction
    val latestPositionState = rememberUpdatedState(positionMs)

    BackHandler(enabled = showLyrics) {
        showLyrics = false
    }

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
            val isFullBleed = playerStyle == NowPlayingStyle.FULL_BLEED && !isLandscape
            val bgColor = MaterialTheme.colorScheme.background
            val isDarkBg = bgColor.luminance() < 0.45f

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
                if (isFullBleed) {
                    AsyncImage(
                        model = coverRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) it.blur(64.dp) else it }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor.copy(alpha = if (isDarkBg) 0.72f else 0.78f))
                    )
                }

                if (isFullBleed && !showLyrics) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.66f)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.0f to Color.Black,
                                            0.42f to Color.Black,
                                            0.76f to Color.Black.copy(alpha = 0.28f),
                                            1.0f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                },
                            pageSpacing = 0.dp,
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
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .graphicsLayer { alpha = 1f - pageOffset * 0.25f }
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
                                    modifier = Modifier.size(96.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                                AsyncImage(
                                    model = pageRequest,
                                    contentDescription = pageTrack.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Лёгкий верхний градиент, чтобы верхние кнопки читались на светлых обложках
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            bgColor.copy(alpha = if (isDarkBg) 0.46f else 0.36f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

        val chromeFill = if (isFullBleed) {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

        val closeButton: @Composable () -> Unit = {
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier.size(if (isLandscape) 42.dp else 46.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = chromeFill,
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
                    containerColor = chromeFill,
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
                    color = chromeFill,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    val isAppleLyrics = lyricsStyle == LyricsStyle.APPLE_MUSIC
                    val lyricsShape = RoundedCornerShape(if (isLandscape) 26.dp else 32.dp)
                    val lyricsCardColor = when {
                        isAppleLyrics && isFullBleed -> Color.Transparent
                        isAppleLyrics -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.56f)
                        isFullBleed -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f)
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = lyricsShape,
                        color = lyricsCardColor,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (isAppleLyrics) 14.dp else 16.dp, vertical = 10.dp)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!isLoadingLyrics && lyricAlternatives.size > 1) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    val nextIdx = (activeAltIndex + 1) % lyricAlternatives.size
                                                    activeAltIndex = nextIdx
                                                    val chosen = lyricAlternatives[nextIdx]
                                                    lyricsState = chosen
                                                    scope.launch {
                                                        LyricsRepository.cacheSelection(context, track, chosen)
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = "${activeAltIndex + 1}/${lyricAlternatives.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
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
                                            scope.launch { loadLyrics(forceRefresh = true) }
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
                                                    scope.launch { loadLyrics(forceRefresh = true) }
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
                                    val isAppleLyrics = lyricsStyle == LyricsStyle.APPLE_MUSIC
                                    val activeLineIdx = remember(synced, positionMs) {
                                        synced.indexOfLast { it.timeMs <= positionMs + 180L }
                                    }
                                    val lyricsListState = rememberLazyListState()
                                    LaunchedEffect(activeLineIdx, isAppleLyrics) {
                                        if (activeLineIdx >= 0) {
                                            if (isAppleLyrics) {
                                                val layoutInfo = lyricsListState.layoutInfo
                                                val targetInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == activeLineIdx }
                                                val desiredTopPx = (layoutInfo.viewportSize.height * 0.22f).toInt()
                                                if (targetInfo != null) {
                                                    val delta = (targetInfo.offset - desiredTopPx).toFloat()
                                                    if (abs(delta) > 4f) {
                                                        lyricsListState.animateScrollBy(
                                                            value = delta,
                                                            animationSpec = spring(
                                                                dampingRatio = 0.84f,
                                                                stiffness = 210f
                                                            )
                                                        )
                                                    }
                                                } else {
                                                    lyricsListState.animateScrollToItem(
                                                        index = (activeLineIdx - 1).coerceAtLeast(0)
                                                    )
                                                }
                                            } else {
                                                lyricsListState.animateScrollToItem(
                                                    index = (activeLineIdx - 2).coerceAtLeast(0)
                                                )
                                            }
                                        }
                                    }

                                    val listModifier = if (isAppleLyrics) {
                                        Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                            .drawWithContent {
                                                drawContent()
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        0.0f to Color.Transparent,
                                                        0.07f to Color.Black,
                                                        0.88f to Color.Black,
                                                        1.0f to Color.Transparent
                                                    ),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            }
                                    } else {
                                        Modifier.fillMaxSize()
                                    }

                                    LazyColumn(
                                        state = lyricsListState,
                                        modifier = listModifier,
                                        contentPadding = PaddingValues(
                                            top = if (isAppleLyrics) 24.dp else 24.dp,
                                            bottom = if (isAppleLyrics) 72.dp else 24.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(if (isAppleLyrics) 18.dp else 12.dp)
                                    ) {
                                        itemsIndexed(synced, key = { idx, item -> "${idx}_${item.timeMs}" }) { index, line ->
                                            val isActive = index == activeLineIdx
                                            val dist = if (activeLineIdx >= 0) abs(index - activeLineIdx) else 0
                                            val staggerDelay = if (isAppleLyrics && activeLineIdx >= 0 && index > activeLineIdx) {
                                                dist.coerceIn(0, 4) * 32
                                            } else {
                                                0
                                            }
                                            val targetAlpha = when {
                                                isActive -> 1f
                                                isAppleLyrics && dist == 1 -> 0.38f
                                                isAppleLyrics && dist == 2 -> 0.25f
                                                isAppleLyrics -> 0.16f
                                                else -> 0.42f
                                            }
                                            val lineAlpha by animateFloatAsState(
                                                targetValue = targetAlpha,
                                                animationSpec = tween(
                                                    durationMillis = if (isAppleLyrics) 340 else 220,
                                                    delayMillis = staggerDelay,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                label = "lyricAlpha"
                                            )
                                            val lineScale by animateFloatAsState(
                                                targetValue = if (isActive) {
                                                    if (isAppleLyrics) 1.04f else 1.03f
                                                } else if (isAppleLyrics) {
                                                    0.96f
                                                } else {
                                                    1f
                                                },
                                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f),
                                                label = "lyricScale"
                                            )
                                            val targetShiftPx = if (!isAppleLyrics || activeLineIdx < 0) {
                                                0f
                                            } else when {
                                                isActive -> 0f
                                                index > activeLineIdx -> 12f
                                                else -> -6f
                                            }
                                            val lineShiftPx by animateFloatAsState(
                                                targetValue = targetShiftPx,
                                                animationSpec = tween(
                                                    durationMillis = 360,
                                                    delayMillis = staggerDelay,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                label = "lyricShift"
                                            )

                                            val litColor = if (isAppleLyrics) {
                                                if (isDarkBg) Color.White else MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                            val unlitColor = if (isAppleLyrics) {
                                                litColor.copy(alpha = 0.34f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                            }

                                            val fontSizeSp = when {
                                                isAppleLyrics && isLandscape -> 19.sp
                                                isAppleLyrics -> 22.sp
                                                else -> 16.sp
                                            }
                                            val lineHeightSp = when {
                                                isAppleLyrics && isLandscape -> 25.sp
                                                isAppleLyrics -> 29.sp
                                                else -> 22.sp
                                            }
                                            val itemWeight = if (isActive) {
                                                FontWeight.ExtraBold
                                            } else if (isAppleLyrics) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.SemiBold
                                            }

                                            Box(
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
                                                        translationY = lineShiftPx
                                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                                        scaleX = lineScale
                                                        scaleY = lineScale
                                                    }
                                            ) {
                                                if (isActive && line.words.isNotEmpty()) {
                                                    ActiveKaraokeLyricText(
                                                        line = line,
                                                        isPlaying = isPlaying,
                                                        positionProvider = { latestPositionState.value },
                                                        litColor = litColor,
                                                        unlitColor = unlitColor,
                                                        fontSize = fontSizeSp,
                                                        lineHeight = lineHeightSp,
                                                        fontWeight = itemWeight
                                                    )
                                                } else {
                                                    Text(
                                                        text = line.text,
                                                        fontSize = fontSizeSp,
                                                        lineHeight = lineHeightSp,
                                                        fontWeight = itemWeight,
                                                        color = if (isActive) litColor else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
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

                Row(horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 6.dp)) {
                    if (isLandscape) {
                        paletteButton()
                    }
                    val smallBtnSize = if (isLandscape || isCompactPortrait) 40.dp else 42.dp
                    val smallInactiveFill = MaterialTheme.colorScheme.surfaceContainerHigh

                    FilledTonalIconButton(
                        onClick = { showLyrics = !showLyrics },
                        modifier = Modifier.size(smallBtnSize),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (showLyrics) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                smallInactiveFill
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
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    val eqActive = AudioEffectsController.eqEnabled || com.example.musicplayer.audio.ParametricEqController.enabled
                    FilledTonalIconButton(
                        onClick = { showEqSheet = true },
                        modifier = Modifier.size(smallBtnSize),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (eqActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                smallInactiveFill
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
                            modifier = Modifier.size(19.dp)
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
                        modifier = Modifier.size(smallBtnSize),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = smallInactiveFill,
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
                                .size(20.dp)
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
                    val prevShape = RoundedCornerShape(if (isLandscape || isCompactPortrait) 18.dp else 22.dp)
                    val sideControlFill = MaterialTheme.colorScheme.surfaceContainerHigh
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
                        shape = prevShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = sideControlFill,
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
                    val playShape = RoundedCornerShape(playCorner.dp)
                    FilledIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePlayPause()
                        },
                        interactionSource = playInteraction,
                        modifier = Modifier
                            .size(playSize)
                            .graphicsLayer { scaleX = playScale; scaleY = playScale },
                        shape = playShape,
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
                        shape = prevShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = sideControlFill,
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
            val pillHeight = if (isLandscape) 42.dp else 46.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isLandscape) 4.dp else 0.dp),
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
        } else if (playerStyle == NowPlayingStyle.EXPRESSIVE) {
            val nextTrackTitle = safeQueue.getOrNull((currentQueueIdx + 1) % safeQueue.size)?.title
            val screenBg = MaterialTheme.colorScheme.background
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Верхняя панель
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    closeButton()
                    Text(
                        text = "Player",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = track.folderName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (nextTrackTitle != null && safeQueue.size > 1) {
                            Text(
                                text = nextTrackTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    paletteButton()
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Высокая обложка на всё свободное пространство (без пустых дыр)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showLyrics) {
                        coverOrLyrics(Modifier.fillMaxSize())
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        ) {
                            coverOrLyrics(Modifier.fillMaxSize())

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.Transparent,
                                            0.45f to screenBg.copy(alpha = 0.65f),
                                            0.82f to screenBg.copy(alpha = 0.94f),
                                            1.0f to screenBg
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { showEqSheet = true },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tune,
                                                contentDescription = "Эквалайзер",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { showEqSheet = true },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${track.formatLabel} • ${track.bitrateKbps} kbps • $sampleRateKhz",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { onToggleFavorite() },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = "Избранное",
                                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Плотно сгруппированный центральный блок: тайминг + пилюля управления + 3 кнопки
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val shownMs = if (isDraggingSlider) (safeDuration * dragFraction).toLong() else positionMs
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = formatDuration(shownMs),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SquigglyProgressBar(
                                progress = displayedFraction,
                                isPlaying = isPlaying,
                                activeColor = MaterialTheme.colorScheme.primary,
                                inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                onSeekStart = { f -> isDraggingSlider = true; dragFraction = f },
                                onSeekChange = { f -> dragFraction = f },
                                onSeekEnd = { onSeekToFraction(dragFraction); isDraggingSlider = false }
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = formatDuration(durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    transitionDir = -1
                                    onPrevious()
                                },
                                modifier = Modifier.size(50.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Предыдущий", modifier = Modifier.size(25.dp))
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeekBy(-10_000L)
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(Icons.Rounded.Replay10, contentDescription = "-10 сек", modifier = Modifier.size(23.dp))
                            }

                            FilledIconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTogglePlayPause()
                                },
                                modifier = Modifier.size(width = 72.dp, height = 52.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Пауза" else "Играть",
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeekBy(10_000L)
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(Icons.Rounded.Forward10, contentDescription = "+10 сек", modifier = Modifier.size(23.dp))
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    transitionDir = 1
                                    onNext()
                                },
                                modifier = Modifier.size(50.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Следующий", modifier = Modifier.size(25.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = if (shuffleEnabled) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            }
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = { showLyrics = !showLyrics },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = if (showLyrics) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            }
                        ) {
                            Icon(Icons.Rounded.Lyrics, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lyrics", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = onCycleRepeatMode,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = if (isRepeatActive) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            }
                        ) {
                            Icon(
                                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Repeat", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Две симметричные нижние плашки без обрезки текста (долгое нажатие на Плейлист включает таймер)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = { onAddToPlaylist() },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCycleSleepTimer()
                                }
                            ),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Плейлист",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (sleepTimerMinutes != null) "Таймер: ${sleepTimerMinutes}м" else "Таймер (удерж.)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(CircleShape)
                            .clickable { showQueueSheet = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Очередь",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${currentQueueIdx + 1} из ${safeQueue.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                    maxLines = 1
                                )
                            }
                        }
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
                        .padding(vertical = if (isCompactPortrait) 4.dp else 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFullBleed) {
                        if (showLyrics) {
                            coverOrLyrics(Modifier.fillMaxSize())
                        }
                    } else {
                        val coverSize = minOf(maxWidth, maxHeight) * 0.94f
                        coverOrLyrics(Modifier.size(coverSize))
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactPortrait) 10.dp else 13.dp)
                ) {
                    trackInfoRow()
                    seekSection()
                    playbackControlsRow()
                    actionPillsRow()
                }
                Spacer(modifier = Modifier.height(if (isCompactPortrait) 18.dp else 30.dp))
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
                            scope.launch { loadLyrics(forceRefresh = true, customQuery = q) }
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

@Composable
private fun ActiveKaraokeLyricText(
    line: LyricLine,
    isPlaying: Boolean,
    positionProvider: () -> Long,
    litColor: Color,
    unlitColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight
) {
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    var framePosMs by remember(line.timeMs) { mutableLongStateOf(positionProvider()) }

    LaunchedEffect(line.timeMs, isPlaying) {
        var lastPolled = positionProvider()
        var anchorNs = System.nanoTime()
        framePosMs = lastPolled
        if (!isPlaying) return@LaunchedEffect

        while (latestIsPlaying) {
            withFrameMillis {
                val polled = positionProvider()
                val nowNs = System.nanoTime()
                if (polled != lastPolled) {
                    lastPolled = polled
                    anchorNs = nowNs
                }
                val elapsed = ((nowNs - anchorNs) / 1_000_000L).coerceIn(0L, 180L)
                framePosMs = lastPolled + elapsed
            }
        }
    }

    val sweptText = buildCharacterSweepText(
        line = line,
        nowMs = framePosMs,
        litColor = litColor,
        unlitColor = unlitColor
    )

    Text(
        text = sweptText,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight
    )
}

private fun buildCharacterSweepText(
    line: LyricLine,
    nowMs: Long,
    litColor: Color,
    unlitColor: Color
): AnnotatedString {
    if (line.words.isEmpty()) return AnnotatedString(line.text)
    return buildAnnotatedString {
        for (w in line.words) {
            when {
                nowMs >= w.endMs -> {
                    withStyle(SpanStyle(color = litColor)) {
                        append(w.text)
                    }
                }
                nowMs <= w.startMs -> {
                    withStyle(SpanStyle(color = unlitColor)) {
                        append(w.text)
                    }
                }
                else -> {
                    val wordDur = (w.endMs - w.startMs).coerceAtLeast(1L).toFloat()
                    val wordProg = ((nowMs - w.startMs).toFloat() / wordDur).coerceIn(0f, 1f)
                    val len = w.text.length
                    if (len <= 1) {
                        withStyle(SpanStyle(color = lerp(unlitColor, litColor, wordProg))) {
                            append(w.text)
                        }
                    } else {
                        val feather = 1.6f
                        val sweepFront = wordProg * (len + feather)
                        for (i in 0 until len) {
                            val charProgress = ((sweepFront - i) / feather).coerceIn(0f, 1f)
                            withStyle(SpanStyle(color = lerp(unlitColor, litColor, charProgress))) {
                                append(w.text[i])
                            }
                        }
                    }
                }
            }
        }
    }
}
