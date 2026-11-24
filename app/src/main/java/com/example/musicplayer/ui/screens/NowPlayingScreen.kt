package com.example.musicplayer.ui.screens

import android.content.res.Configuration
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.data.LocalTrack
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
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekToFraction: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onCycleMonetMode: () -> Unit
) {
    val context = LocalContext.current
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var showQueueSheet by remember { mutableStateOf(false) }

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
        if (track.sampleRateHz % 1000 == 0) {
            "${track.sampleRateHz / 1000} kHz"
        } else {
            String.format(Locale.US, "%.1f kHz", track.sampleRateHz / 1000.0)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dismissThresholdPx = with(density) { 130.dp.toPx() }
    val dismissFlingVelocityPx = with(density) { 900.dp.toPx() }
    val coverSwipeThresholdPx = with(density) { 64.dp.toPx() }

    val dismissOffsetY = remember { Animatable(0f) }
    val dismissProgress = (dismissOffsetY.value / dismissThresholdPx).coerceIn(0f, 1f)

    val coverDragX = remember { Animatable(0f) }

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
        val topBar: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalIconButton(
                    onClick = onClose,
                    modifier = Modifier.size(if (isLandscape) 42.dp else 48.dp),
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

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "${track.formatLabel} • ${track.bitrateKbps} kbps • $sampleRateKhz",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onCycleMonetMode,
                    modifier = Modifier.size(if (isLandscape) 42.dp else 48.dp),
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
        }

        val coverArt: @Composable (Modifier) -> Unit = { boxModifier ->
            // Неподвижный контейнер для жеста, чтобы translationX не вызывал дёрганье
            Box(
                modifier = boxModifier.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                coverDragX.snapTo((coverDragX.value + dragAmount * 0.65f).coerceIn(-160f, 160f))
                            }
                        },
                        onDragEnd = {
                            when {
                                coverDragX.value <= -coverSwipeThresholdPx -> onNext()
                                coverDragX.value >= coverSwipeThresholdPx -> onPrevious()
                            }
                            scope.launch {
                                coverDragX.animateTo(
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
                                coverDragX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    )
                },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = coverScale
                            scaleY = coverScale
                            translationX = coverDragX.value
                        }
                        .clip(RoundedCornerShape(if (isLandscape) 26.dp else 32.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(if (isLandscape) 64.dp else 88.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    AsyncImage(
                        model = coverRequest,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        val trackInfoRow: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                Spacer(modifier = Modifier.width(12.dp))

                FilledTonalIconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(if (isLandscape) 44.dp else 48.dp),
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
                        modifier = Modifier.size(22.dp)
                    )
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
            val prevNextSize = if (isLandscape) 50.dp else 58.dp
            val playSize = if (isLandscape) 66.dp else 80.dp
            val playCorner = if (isLandscape) (playButtonCorner * 0.82f) else playButtonCorner
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
                    horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(prevNextSize),
                        shape = RoundedCornerShape(if (isLandscape) 18.dp else 22.dp),
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

                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(playSize),
                        shape = RoundedCornerShape(playCorner.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Пауза" else "Играть",
                            modifier = Modifier.size(if (isLandscape) 32.dp else 36.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onNext,
                        modifier = Modifier.size(prevNextSize),
                        shape = RoundedCornerShape(if (isLandscape) 18.dp else 22.dp),
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
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Плейлист",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                FilledTonalButton(
                    onClick = onCycleSleepTimer,
                    modifier = Modifier
                        .weight(1f)
                        .height(pillHeight),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
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
            }
        }

        if (isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    Box(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        coverArt(
                            Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        topBar()
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
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

                coverArt(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp)
                        .aspectRatio(1f)
                )

                trackInfoRow()
                seekSection()
                playbackControlsRow()
                actionPillsRow()
            }
        }
        }
    }

    if (showQueueSheet) {
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
