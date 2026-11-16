package org.nooklin.photos.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.folderDepth
import org.nooklin.photos.data.MAX_FOLDER_DEPTH
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.local.LocalMetadataStore
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.common.ZoomLaunch
import org.nooklin.photos.ui.albums.AlbumOrderViewModel
import org.nooklin.photos.data.local.sortedAlbums
import org.nooklin.photos.ui.hidden.HiddenAlbumDetailScreen
import org.nooklin.photos.ui.hidden.HiddenAlbumPhotoPickerScreen
import org.nooklin.photos.ui.hidden.HiddenAlbumsScreen
import org.nooklin.photos.ui.hidden.HiddenUiState
import org.nooklin.photos.ui.hidden.HiddenViewModel
import org.nooklin.photos.ui.recyclebin.RecycleBinScreen
import org.nooklin.photos.ui.recyclebin.RecycleBinViewerScreen
import org.nooklin.photos.ui.settings.SettingsViewModel
import org.nooklin.photos.ui.settings.settingsNavGraph

internal fun NavGraphBuilder.hiddenAlbumRoutes(
    viewModel: GalleryViewModel,
    hiddenViewModel: HiddenViewModel,
    albumOrderViewModel: AlbumOrderViewModel,
    settingsViewModel: SettingsViewModel,
    gridColumnsState: State<Int>,
    hiddenUiStateHolder: State<HiddenUiState>,
    hiddenTopAlbums: List<HiddenAlbumWithPreview>,
    hiddenTargets: List<HiddenAlbumWithPreview>,
    hiddenRecycleBinEntries: List<LocalMetadataStore.RecycleBinEntry>,
    hiddenFileNamesFor: (Collection<Long>) -> List<String>,
    navController: NavHostController,
    capturedZoomLaunchState: MutableState<ZoomLaunch?>,
    hiddenViewerItemsOverrideState: MutableState<List<HiddenItem>?>,
    openViewerZoomed: (Any, String) -> Unit,
    removeExifOnShareState: State<Boolean>,
    hiddenImageSharingEnabledState: State<Boolean>
) {
    val removeExifOnShare by removeExifOnShareState
    val hiddenImageSharingEnabled by hiddenImageSharingEnabledState
    val gridColumns by gridColumnsState
    val hiddenUiState by hiddenUiStateHolder
    val capturedZoomLaunch by capturedZoomLaunchState
    var hiddenViewerItemsOverride by hiddenViewerItemsOverrideState
    composable(ROUTE_HIDDEN_RECYCLE_BIN) {
        RecycleBinScreen(
            entries = hiddenRecycleBinEntries,
            gridColumns = gridColumns,
            onRestore = { mediaId -> hiddenViewModel.restoreDeleted(hiddenFileNamesFor(listOf(mediaId))) },
            onPurgeExpired = { ids -> hiddenViewModel.purgeDeleted(hiddenFileNamesFor(ids)) },
            onItemClick = { index -> openViewerZoomed(hiddenRecycleBinEntries[index].mediaId, "hiddenrecyclebinviewer/$index") },
            onBack = { navController.popBackStack() },
            privateFiles = true
        )
    }
    composable("hiddenrecyclebinviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        RecycleBinViewerScreen(
            entries = StableList(hiddenRecycleBinEntries),
            zoomLaunch = capturedZoomLaunch,
            startIndex = index,
            onBack = { navController.popBackStack() },
            onRestore = { mediaId -> hiddenViewModel.restoreDeleted(hiddenFileNamesFor(listOf(mediaId))) }
        )
    }
    composable(ROUTE_HIDDEN_ALBUMS) {
        val albumOrder by albumOrderViewModel.hidden.collectAsStateWithLifecycle()
        val previewsOff by albumOrderViewModel.hiddenPreviewsOff.collectAsStateWithLifecycle()
        HiddenAlbumsScreen(
            albums = hiddenTopAlbums,
            order = albumOrder,
            onOrderChange = { change -> albumOrderViewModel.update(true, change) },
            previewsOff = previewsOff,
            gridColumns = gridColumns,
            onAlbumClick = { albumId -> navController.navigate("hiddenalbums/$albumId") },
            onCreateAlbum = { name -> hiddenViewModel.createAlbum(name) },
            onDeleteAlbums = { ids -> ids.forEach { hiddenViewModel.deleteAlbum(it) } },
            onRenameAlbum = { id, name -> hiddenViewModel.renameAlbum(id, name) },
            onCreateFolder = { parentId, name -> hiddenViewModel.createFolder(parentId, name) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("hiddenalbums/{albumId}") { entry ->
        val albumId = entry.arguments?.getString("albumId")?.toLongOrNull() ?: -1L
        val album = hiddenUiState.albums.find { it.id == albumId }
        val memberFileNames = remember(hiddenUiState.albumMembership, albumId) {
            hiddenUiState.albumMembership.filter { it.first == albumId }.map { it.second }.toSet()
        }
        val albumItems = remember(hiddenUiState.items, memberFileNames) {
            hiddenUiState.items.filter { it.file.name in memberFileNames }
        }
        val albumOrder by albumOrderViewModel.hidden.collectAsStateWithLifecycle()
        val previewsOff by albumOrderViewModel.hiddenPreviewsOff.collectAsStateWithLifecycle()
        val albumFolders = remember(hiddenUiState.albums, albumId, albumOrder.folderSort) {
            hiddenUiState.albums.filter { it.parentId == albumId }
                .sortedAlbums(albumOrder.folderSort, emptyList(), { it.id }, { it.name }, { it.itemCount })
        }
        HiddenAlbumDetailScreen(
            albumName = album?.name ?: appString(R.string.nooklin_photos_app_album),
            items = albumItems,
            totalCount = album?.itemCount ?: albumItems.size,
            gridColumns = gridColumns,
            existingAlbums = hiddenTargets.filter { it.id != albumId },
            onItemClick = { index ->
                hiddenViewerItemsOverride = albumItems
                openViewerZoomed(albumItems[index].file.absolutePath, "hiddenviewer/$index")
            },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { targetAlbumId, item -> hiddenViewModel.addToAlbum(targetAlbumId, item.file.name) },
            onRemoveFromAlbum = { item -> hiddenViewModel.removeFromAlbum(albumId, item.file.name) },
            onRenameAlbum = { newName -> hiddenViewModel.renameAlbum(albumId, newName) },
            onDeleteAlbum = {
                val isTopLevel = album?.parentId == null
                val othersLeft = hiddenUiState.albums.any { it.parentId == null && it.id != albumId }
                hiddenViewModel.deleteAlbum(albumId)
                when {
                    !isTopLevel -> navController.popBackStack()
                    !othersLeft -> navController.popBackStack(ROUTE_HIDDEN, inclusive = false)
                    !navController.popBackStack(ROUTE_HIDDEN_ALBUMS, inclusive = false) -> navController.popBackStack()
                }
            },
            onAddPhotos = { navController.navigate("hiddenalbums/$albumId/pick") },
            onBack = { navController.popBackStack() },
            isFolder = album?.parentId != null,
            canCreateFolder = folderDepth(albumId) { id -> hiddenUiState.albums.find { it.id == id }?.parentId } < MAX_FOLDER_DEPTH,
            folders = albumFolders,
            folderSort = albumOrder.folderSort,
            onFolderSort = { mode -> albumOrderViewModel.update(true) { it.copy(folderSort = mode) } },
            previewsOff = previewsOff,
            onFolderClick = { folderId -> navController.navigate("hiddenalbums/$folderId") },
            onRenameFolder = { id, name -> hiddenViewModel.renameAlbum(id, name) },
            onDeleteFolder = { id -> hiddenViewModel.deleteAlbum(id) },
            onCreateFolder = { name -> hiddenViewModel.createFolder(albumId, name) },
            coverFileName = album?.coverFileName,
            onSetCover = { fileName -> hiddenViewModel.setAlbumCover(albumId, fileName) },
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare
        )
    }
    composable("hiddenalbums/{albumId}/pick") { entry ->
        val albumId = entry.arguments?.getString("albumId")?.toLongOrNull() ?: -1L
        val album = hiddenUiState.albums.find { it.id == albumId }
        val memberFileNames = remember(hiddenUiState.albumMembership, albumId) {
            hiddenUiState.albumMembership.filter { it.first == albumId }.map { it.second }.toSet()
        }
        HiddenAlbumPhotoPickerScreen(
            albumName = album?.name ?: appString(R.string.nooklin_photos_app_album),
            items = hiddenUiState.items,
            alreadyInAlbum = memberFileNames,
            gridColumns = gridColumns,
            onConfirm = { selectedNames ->
                selectedNames.forEach { fileName -> hiddenViewModel.addToAlbum(albumId, fileName) }
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }
    settingsNavGraph(
        navController = navController,
        settingsViewModel = settingsViewModel,
        galleryViewModel = viewModel,
        onOpenHiddenBackup = {
            hiddenViewModel.lock()
            navController.navigate("settings/hiddenbackup")
        },
        onOpenHiddenImport = {
            hiddenViewModel.lock()
            navController.navigate("settings/hiddenimport")
        }
    )
}
