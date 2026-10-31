package org.nooklin.photos.ui.albums

import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.common.heroImageRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import org.nooklin.photos.ui.common.MenuIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GlassPillTextField
import androidx.compose.ui.semantics.Role
import org.nooklin.photos.ui.theme.Motion
import org.nooklin.photos.ui.theme.Spacing
import org.nooklin.photos.ui.theme.AppText

private val FolderThumbSize = 84.dp

// One folder in the list at the top of an album, for both the normal and the Hidden albums
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderRow(
    name: String,
    itemCount: Int,
    onClick: () -> Unit,
    thumbnail: @Composable (Dp) -> Unit,
    // A long press shows the menu that the caller puts in [menu]
    onLongClick: () -> Unit = {},
    menu: @Composable () -> Unit = {}
) {
    Box {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(FolderThumbSize)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Gray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            // The folder icon stays visible behind an empty folder
            Icon(Icons.Filled.Folder, contentDescription = null, tint = Color.Gray)
            thumbnail(FolderThumbSize)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                fontSize = AppText.BodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                itemCount.toString(),
                color = Color.Gray,
                fontSize = AppText.BodyLarge,
                maxLines = 1
            )
        }
    }
    menu()
    }
}

// The big photo at the top of an album screen, with the title, the item count and a play button for the slideshow
@Composable
fun AlbumHero(
    title: String,
    itemCount: Int,
    showPlay: Boolean,
    onPlay: () -> Unit,
    image: @Composable () -> Unit
) {
    Box(Modifier.fillMaxWidth().aspectRatio(0.9f)) {
        image()
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(170.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 88.dp, bottom = 16.dp)
        ) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = AppText.Headline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(appString(R.string.common_items, itemCount), color = Color.White.copy(alpha = 0.85f), fontSize = AppText.BodyLarge)
        }
        if (showPlay) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(role = Role.Button, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = appString(R.string.slideshow_play), tint = Color.White)
            }
        }
    }
}

// One photo of the slideshow. [model] is the full photo, [thumbnail] and [freshness] give the small picture in the
// strip at the bottom, [aspect] is width / height when known.
class SlideshowPhoto(val model: Any, val thumbnail: Any, val freshness: Any, val aspect: Float?)

private const val SLIDE_MS = 3500
private const val GROUP_MS = 3000
private const val ZOOM = 0.05f

// How the photos of one scene are placed: one photo, or two photos above each other (top share first)
private enum class SlideLayout(val top: Float, val bottom: Float) {
    SINGLE(1f, 0f), STACK_60(60f, 40f), STACK_40(40f, 60f), STACK_50(50f, 50f)
}

// A scene shows [count] photos starting at [first]
private class SlideScene(val first: Int, val count: Int, val layout: SlideLayout) {
    operator fun contains(photo: Int) = photo >= first && photo < first + count
}

private val LAYOUT_CYCLE = listOf(
    SlideLayout.SINGLE, SlideLayout.STACK_60, SlideLayout.SINGLE, SlideLayout.STACK_40,
    SlideLayout.SINGLE, SlideLayout.STACK_50
)

// Single photos alternate with two photos at once. The last photo is single when only one is left.
private fun buildScenes(total: Int): List<SlideScene> {
    val scenes = mutableListOf<SlideScene>()
    var next = 0
    var step = 0
    while (next < total) {
        val wanted = LAYOUT_CYCLE[step % LAYOUT_CYCLE.size]
        val layout = if (wanted != SlideLayout.SINGLE && total - next < 2) SlideLayout.SINGLE else wanted
        val count = if (layout == SlideLayout.SINGLE) 1 else 2
        scenes.add(SlideScene(next, count, layout))
        next += count
        step += 1
    }
    return scenes
}

// Slow zoom of one scene: even scenes zoom in, odd scenes zoom out (like the iOS slideshow)
private fun slideScale(i: Int, progress: Float): Float =
    if (i % 2 == 0) 1f + ZOOM * progress else 1f + ZOOM * (1f - progress)

// One photo that fills its cell and zooms slowly inside it. Always loaded at screen size (own cache key), so a small
// cell is as sharp as a full screen photo.
@Composable
private fun SlideCell(photo: SlideshowPhoto, scale: Float, fit: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val request = remember(photo) { heroImageRequest(context, photo.model, photo.freshness) }
    Box(modifier.clip(RoundedCornerShape(0.dp))) {
        Box(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }) {
            if (fit) {
                // A landscape photo is shown whole on a blurred copy of itself
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(60.dp)
                )
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = if (fit) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SceneView(scene: SlideScene, photos: List<SlideshowPhoto>, scale: Float) {
    val first = photos.getOrNull(scene.first) ?: return
    if (scene.layout == SlideLayout.SINGLE) {
        val landscape = first.aspect != null && first.aspect >= 1f
        SlideCell(first, scale, landscape, Modifier.fillMaxSize())
        return
    }
    val second = photos.getOrNull(scene.first + 1) ?: return
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        SlideCell(first, scale, false, Modifier.fillMaxWidth().weight(scene.layout.top))
        Spacer(Modifier.height(2.dp))
        SlideCell(second, scale, false, Modifier.fillMaxWidth().weight(scene.layout.bottom))
    }
}

// Full screen slideshow after the iOS one: photos fill the screen (a landscape photo is shown whole on a blurred copy),
// a slow zoom in or out, a dissolve to the next photo, the album name fading away at the start. A tap shows the
// controls: Back, Pause and Play, and a strip with all photos in order (tap one to jump to it). After the last photo
// the picture dims and Replay appears. System Back also closes it.
@Composable
fun SlideshowDialog(photos: List<SlideshowPhoto>, title: String, secure: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var index by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(true) }
    val progress = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }
    val strip = rememberLazyListState()
    // index is the scene, a scene shows one or two photos
    val scenes = remember(photos) { buildScenes(photos.size) }
    var shown by remember { mutableIntStateOf(0) }
    var previous by remember { mutableIntStateOf(-1) }
    val transition = remember { Animatable(1f) }

    LaunchedEffect(Unit) { open.animateTo(1f, tween(Motion.Medium)) }
    LaunchedEffect(Unit) {
        delay(4000)
        showTitle = false
    }
    // Runs the zoom of the current photo, then goes to the next. Pause keeps the progress and resumes from there.
    LaunchedEffect(index, paused, finished) {
        if (paused || finished) return@LaunchedEffect
        val duration = if (scenes.getOrNull(index)?.layout == SlideLayout.SINGLE) SLIDE_MS else GROUP_MS
        progress.animateTo(1f, tween(((1f - progress.value) * duration).toInt().coerceAtLeast(1), easing = LinearEasing))
        if (index < scenes.lastIndex) {
            progress.snapTo(0f)
            index += 1
        } else {
            finished = true
        }
    }
    LaunchedEffect(index) {
        strip.animateScrollToItem(((scenes.getOrNull(index)?.first ?: 0) - 4).coerceAtLeast(0))
    }
    // A new scene covers the old one: two photos slide in from the right, a single photo dissolves
    LaunchedEffect(index) {
        if (index != shown) {
            previous = shown
            shown = index
            transition.snapTo(0f)
            transition.animateTo(1f, tween(700))
            previous = -1
        }
    }

    fun jumpTo(photo: Int) {
        val target = scenes.indexOfFirst { photo in it }.coerceAtLeast(0)
        // The current scene is already running, a restart would stop it
        if (target == index && !finished) return
        scope.launch {
            progress.snapTo(0f)
            finished = false
            index = target
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // The photo also fills the area behind the status and navigation bar
            decorFitsSystemWindows = false,
            // Hidden photos must stay out of screenshots, also in this window
            securePolicy = if (secure) SecureFlagPolicy.SecureOn else SecureFlagPolicy.Inherit
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = open.value))
        ) {
            // The slideshow opens as a card that grows to full size, with rounded corners that disappear
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val o = open.value
                        scaleX = 0.92f + 0.08f * o
                        scaleY = 0.92f + 0.08f * o
                        alpha = o
                        shape = RoundedCornerShape((44f * (1f - o)).dp)
                        clip = true
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        controls = !controls
                    }
            ) {
                val old = scenes.getOrNull(previous)
                val current = scenes.getOrNull(shown)
                if (old != null) {
                    // The scene that is covered keeps the zoom it ended with
                    Box(Modifier.fillMaxSize()) { SceneView(old, photos, slideScale(previous, 1f)) }
                }
                if (current != null) {
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            if (old != null) {
                                if (current.layout == SlideLayout.SINGLE) alpha = transition.value
                                else translationX = size.width * (1f - transition.value)
                            }
                        }
                    ) {
                        SceneView(current, photos, slideScale(shown, if (shown == index) progress.value else 1f))
                    }
                }
                // After the last photo
                AnimatedVisibility(visible = finished, enter = fadeIn(tween(600)), exit = fadeOut()) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
                }
                AnimatedVisibility(
                    visible = showTitle && !controls && !finished,
                    enter = fadeIn(),
                    exit = fadeOut(tween(600)),
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp)
                ) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = AppText.Headline)
                }
                AnimatedVisibility(
                    visible = finished,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = AppText.Headline)
                        Spacer(Modifier.height(12.dp))
                        CircleIconButton(
                            icon = Icons.Filled.Replay,
                            contentDescription = appString(R.string.slideshow_replay),
                            onClick = { jumpTo(0) }
                        )
                    }
                }
                // Back is visible after a tap and after the last photo
                AnimatedVisibility(
                    visible = controls || finished,
                    enter = fadeIn(tween(Motion.Medium)),
                    exit = fadeOut(tween(Motion.Medium)),
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp)
                ) {
                    CircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = appString(R.string.common_back),
                        onClick = onClose
                    )
                }
                AnimatedVisibility(
                    visible = controls && !finished,
                    enter = fadeIn(tween(Motion.Medium)),
                    exit = fadeOut(tween(Motion.Medium)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable(role = Role.Button) { paused = !paused },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = appString(if (paused) R.string.slideshow_play else R.string.slideshow_pause),
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                // All photos in order, the current one is larger with a white border
                AnimatedVisibility(
                    visible = controls && !finished,
                    enter = fadeIn(tween(Motion.Medium)),
                    exit = fadeOut(tween(Motion.Medium)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                            .navigationBarsPadding()
                            .padding(top = 24.dp, bottom = 16.dp)
                    ) {
                        LazyRow(
                            state = strip,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            items(photos.size) { i ->
                                val selected = scenes.getOrNull(index)?.contains(i) == true
                                val width by animateDpAsState(if (selected) 40.dp else 26.dp, label = "thumbWidth")
                                val height by animateDpAsState(if (selected) 56.dp else 46.dp, label = "thumbHeight")
                                AsyncImage(
                                    model = freshImageRequest(context, photos[i].thumbnail, photos[i].freshness),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width, height)
                                        .clip(RoundedCornerShape(4.dp))
                                        .then(if (selected) Modifier.border(2.dp, Color.White, RoundedCornerShape(4.dp)) else Modifier)
                                        .clickable(role = Role.Button) { jumpTo(i) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Pick the cover photo of an album from its photos, or go back to the newest photo
@Composable
fun CoverPickerDialog(
    count: Int,
    key: (Int) -> Any,
    thumbnail: @Composable (Int) -> Unit,
    onPick: (Int) -> Unit,
    onUseNewest: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = GlassPill.shape,
        containerColor = GlassPill.adaptivePopupBackground,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(appString(R.string.album_set_cover)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.heightIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(count, key = { key(it) }) { index ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { onPick(index) }
                    ) {
                        thumbnail(index)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUseNewest) { Text(appString(R.string.album_cover_newest)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(appString(R.string.common_cancel)) }
        }
    )
}

@Composable
fun NewFolderDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = GlassPill.shape,
        containerColor = GlassPill.adaptivePopupBackground,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(appString(R.string.folder_new)) },
        text = {
            GlassPillTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = appString(R.string.folder_name)
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onCreate(name.trim())
                    onDismiss()
                }
            ) {
                Text(appString(R.string.common_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(appString(R.string.common_cancel)) }
        }
    )
}

// Menu of a long press on an album in an overview. Share and Merge are null where the screen has none.
@Composable
fun AlbumContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)?,
    onRename: () -> Unit,
    onNewFolder: (() -> Unit)?,
    onMerge: (() -> Unit)?,
    onDelete: () -> Unit,
    isFolder: Boolean = false
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = GlassPill.shape,
        containerColor = GlassPill.adaptivePopupBackground
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (onShare != null) {
                DropdownMenuItem(
                    text = { Text(appString(R.string.common_share)) },
                    leadingIcon = { MenuIcon(Icons.Filled.IosShare) },
                    onClick = { onDismiss(); onShare() }
                )
            }
            DropdownMenuItem(
                text = { Text(appString(if (isFolder) R.string.folder_rename else R.string.album_rename)) },
                leadingIcon = { MenuIcon(Icons.Filled.Edit) },
                onClick = { onDismiss(); onRename() }
            )
            if (onNewFolder != null) {
                DropdownMenuItem(
                    text = { Text(appString(R.string.folder_new)) },
                    leadingIcon = { MenuIcon(Icons.Filled.CreateNewFolder) },
                    onClick = { onDismiss(); onNewFolder() }
                )
            }
            if (onMerge != null) {
                DropdownMenuItem(
                    text = { Text(appString(R.string.common_merge_into)) },
                    leadingIcon = { MenuIcon(Icons.Filled.CallMerge) },
                    onClick = { onDismiss(); onMerge() }
                )
            }
            DropdownMenuItem(
                text = { Text(appString(if (isFolder) R.string.folder_delete else R.string.common_delete_album), color = GlassPill.destructiveText) },
                leadingIcon = { MenuIcon(Icons.Filled.Delete) },
                onClick = { onDismiss(); onDelete() }
            )
        }
    }
}

@Composable
fun RenameAlbumDialog(current: String, onRename: (String) -> Unit, onDismiss: () -> Unit, isFolder: Boolean = false) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = GlassPill.shape,
        containerColor = GlassPill.adaptivePopupBackground,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(appString(if (isFolder) R.string.folder_rename else R.string.album_rename)) },
        text = {
            GlassPillTextField(value = name, onValueChange = { name = it }, placeholder = appString(R.string.common_album_name))
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onRename(name.trim())
                    onDismiss()
                }
            ) { Text(appString(R.string.common_rename)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(appString(R.string.common_cancel)) } }
    )
}
