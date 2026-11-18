package org.nooklin.photos.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.albums.AlbumViewModel
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.common.ZoomLaunch
import org.nooklin.photos.ui.editor.EditTarget
import org.nooklin.photos.ui.editor.PhotoEditorScreen
import org.nooklin.photos.ui.editor.VideoTrimScreen
import org.nooklin.photos.ui.hidden.HiddenPhotoViewerScreen
import org.nooklin.photos.ui.hidden.HiddenUiState
import org.nooklin.photos.ui.hidden.HiddenViewModel
import org.nooklin.photos.ui.theme.Motion
import org.nooklin.photos.ui.viewer.PhotoViewerScreen

internal fun NavGraphBuilder.viewerRoutes(
    onExternalFinish: (List<Uri>?) -> Unit,
    viewModel: GalleryViewModel,
    hiddenViewModel: HiddenViewModel,
    albumViewModel: AlbumViewModel,
    removeExifOnShareState: State<Boolean>,
    sharingDisabledState: State<Boolean>,
    hiddenSectionEnabledState: State<Boolean>,
    hiddenImageSharingEnabledState: State<Boolean>,
    resolvedDarkTheme: Boolean,
    uiStateHolder: State<GalleryUiState>,
    externalItemState: MutableState<MediaItem?>,
    hiddenUiStateHolder: State<HiddenUiState>,
    hiddenTargets: List<HiddenAlbumWithPreview>,
    albumTargets: List<AlbumWithPreview>,
    hideItem: (MediaItem) -> Unit,
    navController: NavHostController,
    capturedZoomLaunchState: MutableState<ZoomLaunch?>,
    viewerItemsOverrideState: MutableState<StableList<MediaItem>?>,
    hiddenViewerItemsOverrideState: MutableState<List<HiddenItem>?>
) {
    val removeExifOnShare by removeExifOnShareState
    val sharingDisabled by sharingDisabledState
    val hiddenSectionEnabled by hiddenSectionEnabledState
    val hiddenImageSharingEnabled by hiddenImageSharingEnabledState
    val uiState by uiStateHolder
    val externalItem by externalItemState
    val hiddenUiState by hiddenUiStateHolder
    val capturedZoomLaunch by capturedZoomLaunchState
    val viewerItemsOverride by viewerItemsOverrideState
    val hiddenViewerItemsOverride by hiddenViewerItemsOverrideState
    composable(
        "viewer/{index}",
        enterTransition = {
            if (isZoomPair(initialState.destination.route, targetState.destination.route)) {
                EnterTransition.None
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(Motion.Medium))
            }
        },
        popExitTransition = {
            if (isZoomPair(initialState.destination.route, targetState.destination.route)) {
                ExitTransition.None
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(Motion.Medium))
            }
        }
    ) { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        // Memoized in a StableList so the viewer can skip recomposition (visibleItems re-filters on every read)
        val libraryItems = remember(uiState.allItems, uiState.filter, uiState.hideScreenshots, uiState.sortOrder) {
            StableList(uiState.visibleItems)
        }
        // override lists are snapshots, take the favorite state from the live library
        val viewerItems = remember(viewerItemsOverride, uiState.allItems, libraryItems) {
            val override = viewerItemsOverride
            if (override == null) libraryItems else {
                val live = uiState.allItems.associate { it.id to it.isFavorite }
                StableList(override.value.map { live[it.id]?.let { f -> if (f == it.isFavorite) it else it.copy(isFavorite = f) } ?: it })
            }
        }
        PhotoViewerScreen(
            items = viewerItems,
            zoomLaunch = capturedZoomLaunch,
            darkTheme = resolvedDarkTheme,
            startIndex = index,
            existingAlbums = albumTargets,
            removeExifOnShare = removeExifOnShare,
            shareEnabled = !sharingDisabled,
            onBack = { if (externalItem != null) onExternalFinish(null) else navController.popBackStack() },
            onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
            onMoveToRecycleBin = { item -> viewModel.moveToRecycleBin(item) },
            hideEnabled = hiddenSectionEnabled,
            onHide = { item -> hideItem(item) },
            onEdit = { item ->
                navController.navigate("editor/${item.id}")
            },
            onAddToAlbum = { albumId, item -> albumViewModel.addToAlbum(albumId, item.id) },
            onCreateAlbumAndAdd = { name, item ->
                albumViewModel.createAlbum(name) { newAlbumId ->
                    albumViewModel.addToAlbum(newAlbumId, item.id)
                }
            },
            onLibraryChanged = { viewModel.load() }
        )
    }
    composable("hiddenviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        HiddenPhotoViewerScreen(
            items = remember(hiddenViewerItemsOverride, hiddenUiState.items) {
                val override = hiddenViewerItemsOverride
                if (override == null) StableList(hiddenUiState.items) else {
                    val live = hiddenUiState.items.associate { it.file.name to it.isFavorite }
                    StableList(override.map { live[it.file.name]?.let { f -> if (f == it.isFavorite) it else it.copy(isFavorite = f) } ?: it })
                }
            },
            zoomLaunch = capturedZoomLaunch,
            darkTheme = resolvedDarkTheme,
            startIndex = index,
            existingAlbums = hiddenTargets,
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            onBack = { navController.popBackStack() },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) { viewModel.load() } },
            onTrash = { item -> hiddenViewModel.trash(item.file.name) },
            onEdit = { item ->
                val editIndex = hiddenUiState.items.indexOf(item)
                navController.navigate("hiddeneditor/$editIndex")
            },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
        )
    }
    composable("hiddenlibraryviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        HiddenPhotoViewerScreen(
            items = StableList(hiddenUiState.visibleItems),
            zoomLaunch = capturedZoomLaunch,
            darkTheme = resolvedDarkTheme,
            startIndex = index,
            existingAlbums = hiddenTargets,
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            onBack = { navController.popBackStack() },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) { viewModel.load() } },
            onTrash = { item -> hiddenViewModel.trash(item.file.name) },
            onEdit = { item ->
                val editIndex = hiddenUiState.items.indexOf(item)
                navController.navigate("hiddeneditor/$editIndex")
            },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
        )
    }
    composable("editor/{mediaId}") { entry ->
        val mediaId = entry.arguments?.getString("mediaId")?.toLongOrNull()
        // The last found item is kept: a deleted edit leaves the list while this screen is still animating out
        val lastItem = remember(mediaId) { arrayOfNulls<MediaItem>(1) }
        val foundItem = uiState.allItems.find { it.id == mediaId }
        if (foundItem != null) lastItem[0] = foundItem
        val item = foundItem ?: lastItem[0]
        if (item != null && item.isVideo) {
            VideoTrimScreen(
                item = item,
                darkTheme = resolvedDarkTheme,
                onBack = { navController.popBackStack() },
                onSaved = {
                    viewModel.load()
                    navController.popBackStack()
                }
            )
        } else if (item != null) {
            PhotoEditorScreen(
                target = EditTarget.MediaStoreTarget(item),
                darkTheme = resolvedDarkTheme,
                onBack = { navController.popBackStack() },
                onSaved = {
                    viewModel.load()
                    navController.popBackStack()
                },
                onDeleted = {
                    viewModel.removeItem(item.id)
                    navController.popBackStack()
                    navController.popBackStack()
                }
            )
        } else {
            LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }
    composable("hiddeneditor/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val item = hiddenUiState.items.getOrNull(index)
        if (item != null) {
            PhotoEditorScreen(
                target = EditTarget.HiddenFileTarget(item.file, item.thumbnailFile, item.isVideo),
                darkTheme = resolvedDarkTheme,
                onHiddenSizeChanged = { w, h -> hiddenViewModel.updateSize(item.file.name, w, h) },
                onHiddenEditSaved = { newName, w, h -> hiddenViewModel.registerEditedCopy(item.file.name, newName, w, h) },
                onBack = { navController.popBackStack() },
                onSaved = {
                    hiddenViewModel.refresh()
                    navController.popBackStack()
                }
            )
        } else {
            LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }
}
