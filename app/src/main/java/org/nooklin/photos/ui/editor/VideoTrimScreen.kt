package org.nooklin.photos.ui.editor

import android.view.LayoutInflater
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.PlaybackException
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nooklin.photos.R
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.VideoClipExporter
import org.nooklin.photos.data.VideoTrims
import org.nooklin.photos.data.local.AppPreferences
import org.nooklin.photos.ui.common.VideoPrewarm
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.TopAppBarDefaults
import org.nooklin.photos.ui.common.GlassPill
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import org.nooklin.photos.appString

private const val MIN_CLIP_MS = 500f

// "Save Video" keeps the file and remembers the trim, "Save as New Clip" cuts a new file
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimScreen(item: MediaItem, darkTheme: Boolean, onBack: () -> Unit, onSaved: () -> Unit) {
    // Same colors as the video viewer: black only in dark mode, the app background in light mode.
    val screenBackground = if (darkTheme) Color.Black else MaterialTheme.colorScheme.background
    val chromeColor = if (darkTheme) Color.White else Color.Black
    val pillBackground = if (darkTheme) GlassPill.fixedGlassBackground else GlassPill.adaptiveGlassBackground
    val pillBorder = if (darkTheme) null else GlassPill.adaptiveBorder
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }
    val savedTrim = remember(item.uri) { VideoTrims.get(item.uri) }

    // Always the whole video, so the trim handles can reach any part of it.
    val player = remember(item.uri) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaItem(Media3MediaItem.fromUri(item.uri))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    var durationMs by remember { mutableLongStateOf(0L) }
    var range by remember { mutableStateOf(0f..1f) }
    var ready by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        // Called from every signal that can bring the duration. Runs once, when the duration is known.
        fun tryBecomeReady() {
            if (ready) return
            if (player.playbackState != Player.STATE_READY) return
            val duration = player.duration
            if (duration == C.TIME_UNSET || duration <= 0L) return
            durationMs = duration
            val start = (savedTrim?.startMs ?: 0L).coerceIn(0L, duration)
            val end = (savedTrim?.endMs ?: duration).coerceIn(start, duration)
            range = start.toFloat()..end.toFloat()
            ready = true
            player.seekTo(start)
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) = tryBecomeReady()

            override fun onTimelineChanged(timeline: Timeline, reason: Int) = tryBecomeReady()

            override fun onPlayerError(error: PlaybackException) {
                loadFailed = true
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        tryBecomeReady()
        onDispose { player.removeListener(listener) }
    }

    // Preview stops at the chosen end and returns to the chosen start.
    LaunchedEffect(isPlaying, range) {
        while (isPlaying) {
            if (player.currentPosition >= range.endInclusive) {
                player.pause()
                player.seekTo(range.start.toLong())
            }
            delay(50)
        }
    }

    Scaffold(
        containerColor = screenBackground,
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_edit), color = chromeColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_cancel), tint = chromeColor)
                    }
                },
                actions = {
                    if (savedTrim != null) {
                        Box(
                            Modifier
                                .padding(end = 4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                                .clickable(enabled = !saving) {
                                    scope.launch {
                                        VideoTrims.set(preferences, item.uri, null)
                                        VideoPrewarm.clear()
                                        Toast.makeText(context, appString(R.string.video_trim_trim_reset_the_whole_video), Toast.LENGTH_LONG).show()
                                        onSaved()
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(appString(R.string.common_reset), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBackground)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(screenBackground)
                .padding(padding)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Named apart from PlayerView's own "player" property, which would win inside apply.
                val playerToShow = player
                AndroidView(
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.video_player_texture, null) as PlayerView).apply {
                            useController = false
                            this.player = playerToShow
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(pillBackground)
                        .clickable(enabled = ready && !saving) {
                            if (isPlaying) {
                                player.pause()
                            } else {
                                if (player.currentPosition >= range.endInclusive - 50f || player.currentPosition < range.start) {
                                    player.seekTo(range.start.toLong())
                                }
                                player.play()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) appString(R.string.common_pause) else appString(R.string.common_play),
                        tint = chromeColor
                    )
                }
            }

            if (ready) {
                TrimPill(pillBackground, pillBorder) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        TrimRangeBar(
                            range = range,
                            durationMs = durationMs.toFloat(),
                            minGapMs = MIN_CLIP_MS.toFloat(),
                            enabled = !saving,
                            color = chromeColor,
                            onChange = { new ->
                                val startMoved = new.start != range.start
                                range = new
                                player.pause()
                                // While dragging, jump to the nearest keyframe: fast enough for the picture to follow the thumb live.
                                player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                                player.seekTo((if (startMoved) new.start else new.endInclusive).toLong())
                            },
                            onDragEnd = { startMoved ->
                                // On release, settle on the exact frame.
                                player.setSeekParameters(SeekParameters.EXACT)
                                player.seekTo((if (startMoved) range.start else range.endInclusive).toLong())
                            }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(appString(R.string.video_trim_start, formatClock(range.start.toLong())), color = chromeColor.copy(alpha = 0.7f), fontSize = 13.sp)
                            Text(appString(R.string.video_trim_length, formatClock((range.endInclusive - range.start).toLong())), color = chromeColor.copy(alpha = 0.7f), fontSize = 13.sp)
                            Text(appString(R.string.video_trim_end, formatClock(range.endInclusive.toLong())), color = chromeColor.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                }
            } else if (loadFailed) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(appString(R.string.video_trim_this_video_can_t_be), color = chromeColor, textAlign = TextAlign.Center)
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = chromeColor)
                }
            }

            TrimPill(pillBackground, pillBorder) {
                if (saving) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = chromeColor)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(
                            Triple(Icons.Filled.Check, appString(R.string.video_trim_save_video), false),
                            Triple(Icons.Filled.ContentCut, appString(R.string.video_trim_save_video_as_new_clip), true)
                        ).forEach { (icon, label, asNewClip) ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clickable(enabled = ready) {
                                        if (asNewClip) {
                                            saving = true
                                            player.pause()
                                            scope.launch {
                                                val clip = VideoClipExporter.export(context, item.uri, range.start.toLong(), range.endInclusive.toLong())
                                                saving = false
                                                if (clip != null) {
                                                    Toast.makeText(context, appString(R.string.video_trim_saved_as_a_new_clip), Toast.LENGTH_LONG).show()
                                                    onSaved()
                                                } else {
                                                    Toast.makeText(context, appString(R.string.video_trim_could_not_save_the_clip), Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            scope.launch {
                                                val isWhole = range.start <= 0f && range.endInclusive >= durationMs - 1f
                                                val trim = if (isWhole) null else VideoTrims.Trim(range.start.toLong(), range.endInclusive.toLong())
                                                VideoTrims.set(preferences, item.uri, trim)
                                                VideoPrewarm.clear()
                                                Toast.makeText(
                                                    context,
                                                    if (trim == null) appString(R.string.video_trim_video_saved) else appString(R.string.video_trim_video_saved_the_file_is),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onSaved()
                                            }
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(icon, contentDescription = label, tint = chromeColor)
                                Text(
                                    label,
                                    color = chromeColor,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatClock(ms: Long): String {
    val totalTenths = ms / 100
    val minutes = totalTenths / 600
    val seconds = (totalTenths % 600) / 10
    val tenths = totalTenths % 10
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}

// Same pill as the viewer's video control bar
@Composable
private fun TrimPill(background: Color, border: BorderStroke?, content: @Composable () -> Unit) {
    Surface(
        color = background,
        shape = GlassPill.shape,
        border = border,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
    }
}

// Range bar like the viewer's seek bar with a thumb at each end, a drag moves the closer one
@Composable
private fun TrimRangeBar(
    range: ClosedFloatingPointRange<Float>,
    durationMs: Float,
    minGapMs: Float,
    enabled: Boolean,
    color: Color,
    onChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onDragEnd: (startMoved: Boolean) -> Unit
) {
    val currentRange by rememberUpdatedState(range)
    val currentOnChange by rememberUpdatedState(onChange)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    var movingStart by remember { mutableStateOf(true) }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(enabled, durationMs) {
                if (!enabled || durationMs <= 0f) return@pointerInput
                val inset = 7.dp.toPx()
                fun valueAt(x: Float): Float {
                    val span = size.width - 2f * inset
                    return if (span <= 0f) 0f else ((x - inset) / span).coerceIn(0f, 1f) * durationMs
                }
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val v = valueAt(offset.x)
                        movingStart = abs(v - currentRange.start) <= abs(v - currentRange.endInclusive)
                    },
                    onHorizontalDrag = { change, _ ->
                        val v = valueAt(change.position.x)
                        val r = currentRange
                        val new = if (movingStart) {
                            v.coerceAtMost(r.endInclusive - minGapMs).coerceAtLeast(0f)..r.endInclusive
                        } else {
                            r.start..v.coerceAtLeast(r.start + minGapMs).coerceAtMost(durationMs)
                        }
                        currentOnChange(new)
                    },
                    onDragEnd = { currentOnDragEnd(movingStart) },
                    onDragCancel = { currentOnDragEnd(movingStart) }
                )
            }
    ) {
        val inset = 7.dp.toPx()
        val centerY = size.height / 2f
        val left = inset
        val right = size.width - inset
        val strokeWidth = 4.dp.toPx()
        fun xOf(v: Float) = if (durationMs <= 0f) left else left + (right - left) * (v / durationMs)
        drawLine(color.copy(alpha = 0.3f), Offset(left, centerY), Offset(right, centerY), strokeWidth, StrokeCap.Round)
        val startX = xOf(range.start)
        val endX = xOf(range.endInclusive)
        drawLine(color, Offset(startX, centerY), Offset(endX, centerY), strokeWidth, StrokeCap.Round)
        drawCircle(color, radius = inset, center = Offset(startX, centerY))
        drawCircle(color, radius = inset, center = Offset(endX, centerY))
    }
}
