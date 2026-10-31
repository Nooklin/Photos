@file:OptIn(UnstableApi::class)

package org.nooklin.photos.ui.editor

import android.media.MediaMetadataRetriever
import android.app.Activity
import java.io.File
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import coil3.imageLoader
import coil3.memory.MemoryCache
import org.nooklin.photos.data.OriginalsStore
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.VideoClipExporter
import org.nooklin.photos.data.VideoTrims
import org.nooklin.photos.data.local.AppPreferences
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.EditorDestructivePill
import org.nooklin.photos.ui.common.EditorModeBar
import org.nooklin.photos.ui.common.EditorModeItem
import org.nooklin.photos.ui.common.GlassPillContainer
import org.nooklin.photos.ui.theme.AppColors
import org.nooklin.photos.ui.common.VideoPrewarm
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.compose.ui.semantics.Role
import org.nooklin.photos.ui.theme.Spacing
import org.nooklin.photos.ui.theme.AppText

private const val MIN_CLIP_MS = 500f
private const val FILM_FRAMES = 10

private enum class VideoEditorMode { TRIM, STYLES, CROP }

// Trim, Styles (the photo filters) and Crop (like the photo editor).
// "Save Video" keeps the file and remembers the trim, "Save as New Clip" cuts a new file.
// With a filter or a crop only a new clip is possible: the video is encoded again.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimScreen(item: MediaItem, darkTheme: Boolean, onBack: () -> Unit, onSaved: () -> Unit) {
    // Same colors as the video viewer: black only in dark mode, the app background in light mode.
    val screenBackground = if (darkTheme) Color.Black else MaterialTheme.colorScheme.background
    val chromeColor = if (darkTheme) Color.White else Color.Black
    val pillBackground = if (darkTheme) GlassPill.fixedGlassBackground else GlassPill.adaptiveGlassBackground
    val pillBorder = if (darkTheme) null else GlassPill.adaptiveBorder
    val popupBackground = if (darkTheme) GlassPill.fixedPopupBackground else GlassPill.adaptivePopupBackground
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }
    val savedTrim = remember(item.uri) { VideoTrims.get(item.uri) }

    // Always the whole video, so the trim handles can reach any part of it.
    val player = remember(item.uri) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaItem(Media3MediaItem.fromUri(item.uri))
            repeatMode = Player.REPEAT_MODE_OFF
            // Not prepared yet: the video effects are set first, once the picture size is known, so the player builds
            // its video pipeline with them (an effect list set on a running player did not change the picture)
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    var durationMs by remember { mutableLongStateOf(0L) }
    var range by remember { mutableStateOf(0f..1f) }
    var ready by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var loadFailed by remember { mutableStateOf(false) }

    var mode by rememberSaveable { mutableStateOf(VideoEditorMode.TRIM) }
    var filter by rememberSaveable { mutableStateOf(PhotoFilter.NONE) }
    var rotationDegrees by rememberSaveable { mutableStateOf(0f) }
    var straightenDegrees by rememberSaveable { mutableStateOf(0f) }
    var flipHorizontal by rememberSaveable { mutableStateOf(false) }
    var flipVertical by rememberSaveable { mutableStateOf(false) }
    var cropRect by remember { mutableStateOf(CropRect()) }
    var selectedAspectPreset by rememberSaveable { mutableStateOf(AspectPreset.entries.first()) }

    // Picture size as shown (rotation applied) and a row of frames for the trim bar and the filter thumbnails
    var displayWidth by remember { mutableStateOf(0) }
    var displayHeight by remember { mutableStateOf(0) }
    var metadataDone by remember { mutableStateOf(false) }
    val filmFrames = remember(item.uri) {
        mutableStateListOf<ImageBitmap?>().apply { repeat(FILM_FRAMES) { add(null) } }
    }

    // null: not exporting. A negative value: exporting without a progress figure (the fast cut).
    var exportProgress by remember { mutableStateOf<Float?>(null) }
    var exportJob by remember { mutableStateOf<Job?>(null) }
    var showSaveMenu by remember { mutableStateOf(false) }

    val edits = VideoEdits(filter, rotationDegrees, straightenDegrees, flipHorizontal, flipVertical, cropRect)
    // The preview effects read the latest values from here on every frame
    val liveEdits = remember { AtomicReference(edits) }
    liveEdits.set(edits)
    val saving = exportProgress != null

    // Saving over the original: the untouched original is kept aside first, for Revert (like the photo editor)
    val originals = remember { OriginalsStore(context) }
    val originalKey = remember(item.id) { OriginalsStore.mediaKey(item.id) }
    var hasOriginal by remember(originalKey) { mutableStateOf(originals.has(originalKey)) }
    var pendingOverwrite by remember { mutableStateOf<File?>(null) }
    var showRevertConfirm by remember { mutableStateOf(false) }

    fun clearVideoCaches() {
        val key = item.uri.toString()
        context.imageLoader.memoryCache?.remove(MemoryCache.Key(key))
        context.imageLoader.diskCache?.remove(key)
        VideoPrewarm.clear()
    }

    val overwriteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val file = pendingOverwrite
        pendingOverwrite = null
        if (result.resultCode == Activity.RESULT_OK && file != null) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    try {
                        // The original goes aside BEFORE the write truncates the file. If that fails, nothing is overwritten.
                        val kept = originals.stashIfAbsent(originalKey) { context.contentResolver.openInputStream(item.uri) }
                        if (!kept) {
                            false
                        } else {
                            context.contentResolver.openOutputStream(item.uri, "rwt")?.use { out ->
                                file.inputStream().use { it.copyTo(out) }
                            } != null
                        }
                    } catch (e: Exception) {
                        Log.e("VideoTrimScreen", "Save over the original failed", e)
                        false
                    } finally {
                        file.delete()
                    }
                }
                exportProgress = null
                if (success) {
                    hasOriginal = true
                    VideoTrims.set(preferences, item.uri, null)
                    clearVideoCaches()
                    Toast.makeText(context, appString(R.string.video_editor_saved_over_original), Toast.LENGTH_LONG).show()
                    onSaved()
                } else {
                    Toast.makeText(context, appString(R.string.video_editor_could_not_overwrite), Toast.LENGTH_LONG).show()
                }
            }
        } else {
            file?.delete()
            exportProgress = null
        }
    }

    val revertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(item.uri, "rwt")?.use { out -> originals.copyTo(originalKey, out) } ?: false
                    } catch (e: Exception) {
                        Log.e("VideoTrimScreen", "Revert failed", e)
                        false
                    }
                }
                exportProgress = null
                if (success) {
                    originals.delete(originalKey)
                    hasOriginal = false
                    VideoTrims.set(preferences, item.uri, null)
                    clearVideoCaches()
                    Toast.makeText(context, appString(R.string.video_editor_reverted), Toast.LENGTH_LONG).show()
                    onSaved()
                } else {
                    Toast.makeText(context, appString(R.string.video_editor_could_not_revert), Toast.LENGTH_LONG).show()
                }
            }
        } else {
            exportProgress = null
        }
    }

    fun saveOverOriginal() {
        player.pause()
        exportProgress = 0f
        exportJob = scope.launch {
            val file = try {
                VideoEditExporter.exportToFile(
                    context, item.uri, range.start.toLong(), range.endInclusive.toLong(), edits, displayWidth, displayHeight
                ) { exportProgress = it }
            } finally {
                exportJob = null
            }
            if (file == null) {
                exportProgress = null
                Toast.makeText(context, appString(R.string.video_trim_could_not_save_the_clip), Toast.LENGTH_LONG).show()
            } else {
                // Stays "saving" (no progress figure) until the system consent and the write are done
                exportProgress = -1f
                pendingOverwrite = file
                try {
                    val request = MediaStore.createWriteRequest(context.contentResolver, listOf(item.uri))
                    overwriteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                } catch (e: Exception) {
                    Log.e("VideoTrimScreen", "No write request", e)
                    pendingOverwrite = null
                    file.delete()
                    exportProgress = null
                    Toast.makeText(context, appString(R.string.video_editor_could_not_overwrite), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun revertToOriginal() {
        exportProgress = -1f
        try {
            val request = MediaStore.createWriteRequest(context.contentResolver, listOf(item.uri))
            revertLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } catch (e: Exception) {
            Log.e("VideoTrimScreen", "No write request", e)
            exportProgress = null
        }
    }

    LaunchedEffect(item.uri) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, item.uri)
                fun metadata(key: Int) = retriever.extractMetadata(key)?.toLongOrNull() ?: 0L
                val width = metadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
                val height = metadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
                val rotation = metadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
                val duration = metadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val turned = rotation == 90 || rotation == 270
                val shownWidth = if (turned) height else width
                val shownHeight = if (turned) width else height
                displayWidth = shownWidth
                displayHeight = shownHeight
                // The player can prepare now; the strip of frames below fills in while it loads
                metadataDone = true
                if (shownWidth > 0 && shownHeight > 0 && duration > 0L) {
                    val frameHeight = 120
                    val frameWidth = (frameHeight.toFloat() * shownWidth / shownHeight).toInt().coerceAtLeast(1)
                    for (index in 0 until FILM_FRAMES) {
                        val timeUs = duration * 1000L * (2 * index + 1) / (2 * FILM_FRAMES)
                        val frame = retriever.getScaledFrameAtTime(
                            timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, frameWidth, frameHeight
                        )
                        filmFrames[index] = frame?.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                Log.w("VideoTrimScreen", "Could not read the video frames", e)
            } finally {
                retriever.release()
            }
        }
        metadataDone = true
    }

    // The first time the player is prepared here, with the effects already set. A rotate tap builds a new effect list:
    // the player is stopped, gets the list and is prepared again at the same place. The other edits are read live.
    LaunchedEffect(rotationDegrees, displayWidth, displayHeight, metadataDone) {
        if (!metadataDone) return@LaunchedEffect
        val idle = player.playbackState == Player.STATE_IDLE
        if (displayWidth > 0 && displayHeight > 0) {
            val effects = buildVideoEffects(liveEdits, displayWidth, displayHeight, forExport = false)
            Log.d("VideoTrimScreen", "Preview effects set: ${effects.size} effects, rotation $rotationDegrees, size ${displayWidth}x$displayHeight, prepared before: ${!idle}")
            val position = player.currentPosition
            if (!idle) {
                player.pause()
                player.stop()
            }
            player.setVideoEffects(effects)
            player.setSeekParameters(SeekParameters.EXACT)
            if (!idle) player.seekTo(position)
            player.prepare()
        } else {
            Log.w("VideoTrimScreen", "Preview without effects, picture size unknown: ${displayWidth}x$displayHeight")
            if (idle) player.prepare()
        }
    }
    // A paused player draws no new frame when an edit changes. So while paused, the frame at the current place is
    // grabbed once and shown as a picture with the edits applied on it (live while dragging, the video does not move).
    // While playing, the player shows the edits itself.
    var stillFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var stillPositionMs by remember { mutableLongStateOf(-1L) }
    LaunchedEffect(isPlaying, ready) {
        if (!ready || isPlaying) return@LaunchedEffect
        while (true) {
            val position = player.currentPosition
            if (position != stillPositionMs) stillPositionMs = position
            delay(100)
        }
    }
    val stillWanted = ready && !isPlaying && !edits.isNeutral && !(edits.hasCrop &&
        edits.filter == PhotoFilter.NONE && edits.rotationDegrees == 0f && edits.straightenDegrees == 0f &&
        !edits.flipHorizontal && !edits.flipVertical)
    LaunchedEffect(stillPositionMs, stillWanted) {
        if (!stillWanted || stillPositionMs < 0L) return@LaunchedEffect
        val frame = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, item.uri)
                val timeUs = stillPositionMs * 1000L
                val high = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val wide = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val turned = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0) % 180 != 0
                val sourceHigh = if (turned) wide else high
                val sourceWide = if (turned) high else wide
                if (sourceHigh <= 0 || sourceWide <= 0) {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                } else {
                    val targetHigh = minOf(sourceHigh, 1080)
                    val targetWide = (sourceWide.toLong() * targetHigh / sourceHigh).toInt().coerceAtLeast(1)
                    retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, targetWide, targetHigh)
                }
            } catch (e: Exception) {
                Log.w("VideoTrimScreen", "Could not grab the still frame", e)
                null
            } finally {
                retriever.release()
            }
        }
        // On failure the old frame stays; if there is none the player is shown
        if (frame != null) stillFrame = frame.asImageBitmap()
    }

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

            // The player reports the size it really shows: with a working rotate effect it swaps after a rotate tap.
            // Also the way to learn the picture size when the metadata did not give it.
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                Log.d("VideoTrimScreen", "Video size ${videoSize.width}x${videoSize.height}, unapplied rotation ${videoSize.unappliedRotationDegrees}")
                if (displayWidth == 0 && videoSize.width > 0 && videoSize.height > 0) {
                    val turned = videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270
                    displayWidth = if (turned) videoSize.height else videoSize.width
                    displayHeight = if (turned) videoSize.width else videoSize.height
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("VideoTrimScreen", "Player error ${error.errorCodeName}", error)
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
            positionMs = player.currentPosition
            if (player.currentPosition >= range.endInclusive) {
                player.pause()
                player.seekTo(range.start.toLong())
            }
            delay(50)
        }
        positionMs = player.currentPosition
    }

    val frameIsTurned = ((rotationDegrees % 180f) + 180f) % 180f == 90f
    val frameAspect = if (displayWidth > 0 && displayHeight > 0) {
        val shown = displayWidth.toFloat() / displayHeight.toFloat()
        if (frameIsTurned) 1f / shown else shown
    } else {
        16f / 9f
    }

    fun saveAsNewClip() {
        player.pause()
        val fastCut = edits.isNeutral
        exportProgress = if (fastCut) -1f else 0f
        exportJob = scope.launch {
            val start = range.start.toLong()
            val end = range.endInclusive.toLong()
            val clip = try {
                if (fastCut) {
                    VideoClipExporter.export(context, item.uri, start, end)
                } else {
                    VideoEditExporter.export(context, item.uri, start, end, edits, displayWidth, displayHeight) {
                        exportProgress = it
                    }
                }
            } finally {
                exportProgress = null
                exportJob = null
            }
            if (clip != null) {
                val message = if (fastCut) R.string.video_trim_saved_as_a_new_clip else R.string.video_editor_saved_new_clip
                Toast.makeText(context, appString(message), Toast.LENGTH_LONG).show()
                onSaved()
            } else {
                Toast.makeText(context, appString(R.string.video_trim_could_not_save_the_clip), Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler(enabled = saving) { exportJob?.cancel() }

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
                    // One red button that takes the file back to the original. Without a stored original it only
                    // clears a trim saved by an older version.
                    if (hasOriginal || savedTrim != null) {
                        EditorDestructivePill(text = appString(R.string.photo_editor_revert), enabled = !saving) {
                            if (hasOriginal) {
                                showRevertConfirm = true
                            } else {
                                scope.launch {
                                    VideoTrims.set(preferences, item.uri, null)
                                    VideoPrewarm.clear()
                                    Toast.makeText(context, appString(R.string.video_trim_trim_reset_the_whole_video), Toast.LENGTH_LONG).show()
                                    onSaved()
                                }
                            }
                        }
                    }
                    Box {
                        IconButton(enabled = ready && !saving, onClick = { showSaveMenu = true }) {
                            Icon(Icons.Filled.Check, contentDescription = appString(R.string.common_save), tint = chromeColor)
                        }
                        DropdownMenu(
                            expanded = showSaveMenu,
                            onDismissRequest = { showSaveMenu = false },
                            shape = GlassPill.shape,
                            containerColor = popupBackground
                        ) {
                            DropdownMenuItem(
                                enabled = !(edits.isNeutral && range.start <= 0f && range.endInclusive >= durationMs - 1f),
                                text = {
                                    Column {
                                        Text(appString(R.string.video_trim_save_video), color = chromeColor)
                                        Text(
                                            appString(R.string.video_editor_save_over_original_hint),
                                            color = chromeColor.copy(alpha = 0.7f),
                                            fontSize = AppText.Caption
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null, tint = chromeColor) },
                                onClick = {
                                    showSaveMenu = false
                                    saveOverOriginal()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(appString(R.string.video_trim_save_video_as_new_clip), color = chromeColor) },
                                leadingIcon = { Icon(Icons.Filled.ContentCut, contentDescription = null, tint = chromeColor) },
                                onClick = {
                                    showSaveMenu = false
                                    saveAsNewClip()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBackground)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(screenBackground)
            ) {
                if (mode == VideoEditorMode.CROP) {
                    CropTopRow(
                        chromeColor = chromeColor,
                        pillBackground = pillBackground,
                        pillBorder = pillBorder,
                        selectedPreset = selectedAspectPreset,
                        onSelectPreset = { preset ->
                            selectedAspectPreset = preset
                            cropRect = if (preset.ratio == null) CropRect() else cropRectForAspect(preset.ratio, frameAspect)
                        },
                        onRotate = {
                            rotationDegrees = (rotationDegrees + 90f) % 360f
                            cropRect = CropRect()
                        },
                        onFlipHorizontal = { flipHorizontal = !flipHorizontal },
                        onFlipVertical = { flipVertical = !flipVertical }
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // The box has the shape of the picture after rotating, so the crop overlay lies exactly on it
                    Box(Modifier.aspectRatio(frameAspect)) {
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
                        val still = stillFrame
                        if (stillWanted && still != null) {
                            // Same order as the export: rotate, flip, straighten (with fill scale), filter
                            val theta = Math.toRadians(straightenDegrees.toDouble())
                            val fill = (abs(cos(theta)) + abs(sin(theta)) * frameAspect).toFloat()
                            BoxWithConstraints(Modifier.fillMaxSize()) {
                                val boxWidth = maxWidth
                                val boxHeight = maxHeight
                                Image(
                                    bitmap = still,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    colorFilter = if (filter == PhotoFilter.NONE) null else ColorFilter.colorMatrix(ComposeColorMatrix(filter.matrix)),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            rotationZ = straightenDegrees
                                            scaleX = fill
                                            scaleY = fill
                                        }
                                        .graphicsLayer {
                                            scaleX = if (flipHorizontal) -1f else 1f
                                            scaleY = if (flipVertical) -1f else 1f
                                        }
                                        .requiredSize(if (frameIsTurned) boxHeight else boxWidth, if (frameIsTurned) boxWidth else boxHeight)
                                        .graphicsLayer { rotationZ = rotationDegrees }
                                )
                            }
                        }
                        if (mode == VideoEditorMode.CROP && ready) {
                            CropOverlay(
                                cropRect = cropRect,
                                onCropRectChange = { cropRect = it }
                            )
                        }
                    }
                    if (mode != VideoEditorMode.CROP) {
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(pillBackground)
                                .clickable(enabled = ready && !saving, role = Role.Button) {
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
                }

                if (!ready) {
                    if (loadFailed) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(appString(R.string.video_trim_this_video_can_t_be), color = chromeColor, textAlign = TextAlign.Center)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = chromeColor)
                        }
                    }
                } else {
                    when (mode) {
                        VideoEditorMode.TRIM -> TrimPill(pillBackground, pillBorder) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                TrimRangeBar(
                                    range = range,
                                    durationMs = durationMs.toFloat(),
                                    positionMs = positionMs.toFloat(),
                                    minGapMs = MIN_CLIP_MS,
                                    enabled = !saving,
                                    color = chromeColor,
                                    frames = filmFrames,
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
                                    Text(appString(R.string.video_trim_start, formatClock(range.start.toLong())), color = chromeColor.copy(alpha = 0.7f), fontSize = AppText.Body)
                                    Text(appString(R.string.video_trim_length, formatClock((range.endInclusive - range.start).toLong())), color = chromeColor.copy(alpha = 0.7f), fontSize = AppText.Body)
                                    Text(appString(R.string.video_trim_end, formatClock(range.endInclusive.toLong())), color = chromeColor.copy(alpha = 0.7f), fontSize = AppText.Body)
                                }
                            }
                        }

                        VideoEditorMode.STYLES -> {
                            val thumbnail = filmFrames.firstOrNull { it != null }
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                                items(PhotoFilter.entries, key = { it.name }) { option ->
                                    val selected = option == filter
                                    Column(
                                        Modifier
                                            .padding(end = 12.dp)
                                            .clickable(role = Role.Button) { filter = option },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val shape = RoundedCornerShape(10.dp)
                                        val thumbnailModifier = Modifier
                                            .size(56.dp)
                                            .clip(shape)
                                            .then(if (selected) Modifier.border(2.dp, AppColors.SelectedYellow, shape) else Modifier)
                                        if (thumbnail != null) {
                                            Image(
                                                bitmap = thumbnail,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                colorFilter = ColorFilter.colorMatrix(ComposeColorMatrix(option.matrix)),
                                                modifier = thumbnailModifier
                                            )
                                        } else {
                                            Box(thumbnailModifier.background(chromeColor.copy(alpha = 0.1f)))
                                        }
                                        Text(
                                            option.label,
                                            color = if (selected) AppColors.SelectedYellow else chromeColor.copy(alpha = 0.7f),
                                            fontSize = AppText.Caption,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Straighten only: the video effects have no perspective warp
                        VideoEditorMode.CROP -> GeometryTools(
                            chromeColor = chromeColor,
                            tools = listOf(GeometryTool.STRAIGHTEN),
                            values = mapOf(GeometryTool.STRAIGHTEN to straightenDegrees),
                            onValueChange = { _, newValue -> straightenDegrees = newValue }
                        )
                    }
                }

                EditorModeBar(
                    items = listOf(
                        EditorModeItem(VideoEditorMode.TRIM, Icons.Filled.ContentCut, appString(R.string.video_editor_trim)),
                        EditorModeItem(VideoEditorMode.STYLES, Icons.Filled.AutoAwesome, appString(R.string.photo_editor_styles)),
                        EditorModeItem(VideoEditorMode.CROP, Icons.Filled.Crop, appString(R.string.photo_editor_crop))
                    ),
                    selected = mode,
                    chromeColor = chromeColor,
                    pillBackground = pillBackground,
                    pillBorder = pillBorder,
                    enabled = !saving
                ) { mode = it }
            }

            if (showRevertConfirm) {
                AlertDialog(
                    onDismissRequest = { showRevertConfirm = false },
                    shape = GlassPill.shape,
                    containerColor = popupBackground,
                    titleContentColor = chromeColor,
                    textContentColor = chromeColor,
                    title = { Text(appString(R.string.video_editor_revert_title)) },
                    text = { Text(appString(R.string.video_editor_revert_text)) },
                    confirmButton = {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Red.copy(alpha = 0.15f))
                                .clickable(role = Role.Button) {
                                    showRevertConfirm = false
                                    revertToOriginal()
                                }
                                .padding(horizontal = Spacing.Lg, vertical = Spacing.Md)
                        ) {
                            Text(appString(R.string.photo_editor_revert_to_original_2), color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRevertConfirm = false }) {
                            Text(appString(R.string.common_cancel))
                        }
                    }
                )
            }

            if (saving) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = true, role = Role.Button) { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val progress = exportProgress ?: 0f
                        if (progress < 0f) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            CircularProgressIndicator(progress = { progress }, color = Color.White)
                            Text(
                                appString(R.string.video_editor_exporting, (progress * 100).toInt()),
                                color = Color.White,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                        TextButton(onClick = { exportJob?.cancel() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text(appString(R.string.common_cancel), color = Color.White)
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = Spacing.Sm)
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
    }
}

// Same bar as the photo editor: Trim, Styles and Crop
// A strip of frames with a handle at each end. A drag moves the closer handle. The part outside the chosen range is
// dimmed and a thin line shows where the player is.
@Composable
private fun TrimRangeBar(
    range: ClosedFloatingPointRange<Float>,
    durationMs: Float,
    positionMs: Float,
    minGapMs: Float,
    enabled: Boolean,
    color: Color,
    frames: List<ImageBitmap?>,
    onChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onDragEnd: (startMoved: Boolean) -> Unit
) {
    val currentRange by rememberUpdatedState(range)
    val currentOnChange by rememberUpdatedState(onChange)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    var movingStart by remember { mutableStateOf(true) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            frames.forEach { frame ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color.copy(alpha = 0.1f))
                ) {
                    if (frame != null) {
                        Image(
                            bitmap = frame,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(enabled, durationMs) {
                    if (!enabled || durationMs <= 0f) return@pointerInput
                    val inset = 12.dp.toPx()
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
            val inset = 12.dp.toPx()
            val left = inset
            val right = size.width - inset
            val barHeight = size.height
            fun xOf(v: Float) = if (durationMs <= 0f) left else left + (right - left) * (v / durationMs)
            val startX = xOf(range.start)
            val endX = xOf(range.endInclusive)
            val dim = Color.Black.copy(alpha = 0.55f)
            drawRect(dim, topLeft = Offset(left, 0f), size = Size((startX - left).coerceAtLeast(0f), barHeight))
            drawRect(dim, topLeft = Offset(endX, 0f), size = Size((right - endX).coerceAtLeast(0f), barHeight))
            val border = 3.dp.toPx()
            drawRect(
                AppColors.SelectedYellow,
                topLeft = Offset(startX, border / 2f),
                size = Size((endX - startX).coerceAtLeast(0f), barHeight - border),
                style = Stroke(width = border)
            )
            val radius = CornerRadius(6.dp.toPx())
            drawRoundRect(AppColors.SelectedYellow, topLeft = Offset(startX - inset, 0f), size = Size(inset, barHeight), cornerRadius = radius)
            drawRoundRect(AppColors.SelectedYellow, topLeft = Offset(endX, 0f), size = Size(inset, barHeight), cornerRadius = radius)
            val notch = Color.Black.copy(alpha = 0.6f)
            drawLine(notch, Offset(startX - inset / 2f, barHeight * 0.3f), Offset(startX - inset / 2f, barHeight * 0.7f), 2.dp.toPx())
            drawLine(notch, Offset(endX + inset / 2f, barHeight * 0.3f), Offset(endX + inset / 2f, barHeight * 0.7f), 2.dp.toPx())
            val playX = xOf(positionMs.coerceIn(range.start, range.endInclusive))
            drawLine(Color.White, Offset(playX, 0f), Offset(playX, barHeight), 2.dp.toPx())
        }
    }
}
