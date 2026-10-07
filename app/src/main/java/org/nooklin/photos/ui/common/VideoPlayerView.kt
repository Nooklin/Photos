package org.nooklin.photos.ui.common

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import android.view.LayoutInflater
import org.nooklin.photos.R
import org.nooklin.photos.data.VideoTrims

// ExoPlayer playback for a pager page
internal fun videoUriOf(model: Any): Uri = when (model) {
    is Uri -> model
    is File -> Uri.fromFile(model)
    else -> Uri.parse(model.toString())
}

// A trim saved with "Save Video" plays as a clip of the original file.
private fun videoMediaItem(uri: Uri): Media3MediaItem {
    val trim = VideoTrims.get(uri) ?: return Media3MediaItem.fromUri(uri)
    return Media3MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(
            Media3MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(trim.startMs)
                .setEndPositionMs(trim.endMs)
                .build()
        )
        .build()
}

private fun createVideoPlayer(context: Context, uri: Uri): ExoPlayer =
    ExoPlayer.Builder(context.applicationContext).build().apply {
        setMediaItem(videoMediaItem(uri))
        // Loops rather than stopping on the last frame, a short clip ending mid-view reads as a stalled player.
        repeatMode = Player.REPEAT_MODE_ONE
        prepare()
        playWhenReady = false
    }

// Prepared paused players for neighbouring videos, so a swipe only attaches a surface.
// Capped, released when no longer a neighbour.
internal object VideoPrewarm {
    private const val MAX_PLAYERS = 2
    private val players = LinkedHashMap<Uri, ExoPlayer>()

    fun update(context: Context, warm: Set<Uri>, keep: Set<Uri>) {
        val stale = players.keys.filter { it !in keep }
        stale.forEach { players.remove(it)?.release() }
        for (uri in warm) {
            if (uri in players) continue
            if (players.size >= MAX_PLAYERS) break
            players[uri] = createVideoPlayer(context, uri)
        }
    }

    fun take(uri: Uri): ExoPlayer? = players.remove(uri)

    fun create(context: Context, uri: Uri): ExoPlayer = createVideoPlayer(context, uri)

    fun clear() {
        players.values.forEach { it.release() }
        players.clear()
    }
}

@Composable
fun VideoPlayerView(
    model: Any,
    modifier: Modifier = Modifier,
    // Publishes position and play state to the control bar and turns off the built-in controller. Null keeps it.
    controlState: VideoControlState? = null,
    onTap: () -> Unit = {},
    // False while the pager is still swiping, so playback does not begin until the swipe has finished.
    playEnabled: Boolean = true,
    // Poster over the player until the first frame is on screen, avoids a flicker at the end of the open animation
    poster: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val uri = remember(model) { videoUriOf(model) }
    // A player prepared ahead of time when one exists, so playback starts without waiting for the decoder.
    val exoPlayer = remember(uri) { VideoPrewarm.take(uri) ?: VideoPrewarm.create(context, uri) }
    var hasStarted by remember(exoPlayer) { mutableStateOf(false) }
    LaunchedEffect(exoPlayer, playEnabled) {
        if (playEnabled && !hasStarted) {
            hasStarted = true
            exoPlayer.playWhenReady = true
        }
    }
    // Pause on background, release only on leaving composition
    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }
    DisposableEffect(lifecycleOwner, exoPlayer) {
        // Don't resume a video the user paused
        var resumeOnForeground = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    resumeOnForeground = exoPlayer.playWhenReady
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (resumeOnForeground) exoPlayer.play()
                    resumeOnForeground = false
                }
                else -> {}
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            exoPlayer.release()
        }
    }
    var firstFrameRendered by remember(exoPlayer) { mutableStateOf(false) }
    // If the first-frame event is missed, don't keep the poster forever
    LaunchedEffect(exoPlayer, hasStarted) {
        if (hasStarted) {
            delay(1500)
            firstFrameRendered = true
        }
    }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }
    // Publishes the player to the control bar
    DisposableEffect(exoPlayer, controlState) {
        val state = controlState
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                state?.isPlaying = playWhenReady
            }
        }
        if (state != null) {
            state.player = exoPlayer
            state.isPlaying = exoPlayer.playWhenReady
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            // Only clear if still this player: the next one may have registered already
            if (state != null && state.player === exoPlayer) {
                state.player = null
                state.isPlaying = false
                state.positionMs = 0L
                state.durationMs = 0L
            }
        }
    }
    // ExoPlayer has no position callback, poll on the main thread
    LaunchedEffect(exoPlayer, controlState) {
        val state = controlState ?: return@LaunchedEffect
        while (true) {
            state.positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            state.durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(200)
        }
    }
    val muted = controlState?.isMuted == true
    LaunchedEffect(exoPlayer, muted) {
        exoPlayer.volume = if (muted) 0f else 1f
    }
    val posterAlpha by animateFloatAsState(
        targetValue = if (firstFrameRendered) 0f else 1f,
        animationSpec = tween(150),
        label = "videoPosterAlpha"
    )
    val currentOnTap by rememberUpdatedState(onTap)
    val videoAlpha by animateFloatAsState(
        targetValue = if (firstFrameRendered) 1f else 0f,
        animationSpec = tween(150),
        label = "videoAlpha"
    )
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { currentOnTap() }) }
    ) {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.video_player_texture, null) as PlayerView).apply {
                    // Built-in controller only without a control bar. Set before attaching, else a paused player flashes center buttons.
                    useController = controlState == null
                    if (controlState != null) hideController()
                    // The default shutter is opaque black until the first frame, which would hide the still image.
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    player = exoPlayer
                }
            },
            // Invisible until the first video frame, then fades in over the still image, which is the same frame.
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = videoAlpha }
        )
        // Removed, not just invisible: stops holding the image
        if (posterAlpha > 0f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = posterAlpha }) {
                poster()
            }
        }
    }
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}
