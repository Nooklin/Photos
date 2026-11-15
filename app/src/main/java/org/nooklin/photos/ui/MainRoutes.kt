package org.nooklin.photos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.MediaCategory
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.MediaStoreRepository
import org.nooklin.photos.data.createExifStrippedShareUri
import org.nooklin.photos.data.local.AlbumEntity
import org.nooklin.photos.data.local.AlbumMembershipEntity
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.GroupBy
import org.nooklin.photos.data.local.HomeSection
import org.nooklin.photos.ui.albums.AlbumDetailScreen
import org.nooklin.photos.ui.albums.AlbumMergeScreen
import org.nooklin.photos.ui.albums.AlbumPhotoPickerScreen
import org.nooklin.photos.ui.albums.AlbumViewModel
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.albums.AlbumsScreen
import org.nooklin.photos.ui.common.GroupActions
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.common.ZoomLaunch
import org.nooklin.photos.ui.common.ZoomSources
import org.nooklin.photos.ui.discovered.DiscoveredAlbum
import org.nooklin.photos.ui.gallery.GalleryScreen
import org.nooklin.photos.ui.hidden.HiddenViewModel
import org.nooklin.photos.ui.home.HiddenHomeCustomizeScreen
import org.nooklin.photos.ui.home.HomeScreen
import org.nooklin.photos.ui.home.ReorderHomeScreen
import org.nooklin.photos.ui.settings.SettingsViewModel
import org.nooklin.photos.ui.theme.Motion

internal fun NavGraphBuilder.mainRoutes(
    context: Context,
    viewModel: GalleryViewModel,
    hiddenViewModel: HiddenViewModel,
    settingsViewModel: SettingsViewModel,
    albumViewModel: AlbumViewModel,
    gridStyleState: State<GridStyle>,
    gridColumnsState: State<Int>,
    libraryPreviewRowsState: State<Int>,
    groupByState: State<GroupBy>,
    mlEnabledTagsState: State<Set<String>>,
    removeExifOnShareState: State<Boolean>,
    sharingDisabledState: State<Boolean>,
    hiddenSectionEnabledState: State<Boolean>,
    homeSectionOrderState: State<List<HomeSection>>,
    homeMediaTypesOrderState: State<List<String>>,
    homeUtilitiesOrderState: State<List<String>>,
    visibleUtilitiesOrder: List<String>,
    homeHiddenSectionsState: State<Set<HomeSection>>,
    hiddenHomeHiddenSectionsState: State<Set<HomeSection>>,
    uiStateHolder: State<GalleryUiState>,
    coroutineScope: CoroutineScope,
    albumEntitiesState: State<List<AlbumEntity>>,
    albumMembershipsState: State<List<AlbumMembershipEntity>>,
    allCollections: List<AlbumWithPreview>,
    albumsWithPreview: List<AlbumWithPreview>,
    albumTargets: List<AlbumWithPreview>,
    groupActions: GroupActions,
    discoveredAlbums: List<DiscoveredAlbum>,
    hideItems: (List<MediaItem>) -> Unit,
    navController: NavHostController,
    capturedZoomLaunchState: MutableState<ZoomLaunch?>,
    viewerItemsOverrideState: MutableState<StableList<MediaItem>?>,
    navigateToDiscoveredAlbum: (String) -> Unit
) {
    val gridStyle by gridStyleState
    val gridColumns by gridColumnsState
    val libraryPreviewRows by libraryPreviewRowsState
    val groupBy by groupByState
    val mlEnabledTags by mlEnabledTagsState
    val removeExifOnShare by removeExifOnShareState
    val sharingDisabled by sharingDisabledState
    val hiddenSectionEnabled by hiddenSectionEnabledState
    val homeSectionOrder by homeSectionOrderState
    val homeMediaTypesOrder by homeMediaTypesOrderState
    val homeUtilitiesOrder by homeUtilitiesOrderState
    val homeHiddenSections by homeHiddenSectionsState
    val hiddenHomeHiddenSections by hiddenHomeHiddenSectionsState
    val uiState by uiStateHolder
    val albumEntities by albumEntitiesState
    val albumMemberships by albumMembershipsState
    var capturedZoomLaunch by capturedZoomLaunchState
    var viewerItemsOverride by viewerItemsOverrideState
    composable(
        ROUTE_HOME,
        exitTransition = {
            if (targetState.destination.route == "viewer/{index}") {
                ExitTransition.None
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(Motion.Medium))
            }
        },
        popEnterTransition = {
            if (initialState.destination.route == "viewer/{index}") {
                EnterTransition.None
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(Motion.Medium))
            }
        }
    ) {
        HomeScreen(
            items = uiState.visibleItems,
            gridColumns = gridColumns,
            libraryPreviewRows = libraryPreviewRows,
            albums = albumsWithPreview,
            discoveredAlbums = discoveredAlbums,
            sectionOrder = homeSectionOrder,
            hiddenSections = homeHiddenSections,
            mediaTypesOrder = homeMediaTypesOrder,
            utilitiesOrder = visibleUtilitiesOrder,
            onSeeFullLibrary = { navController.navigate(ROUTE_GALLERY) },
            onOpenAlbums = { navController.navigate(ROUTE_ALBUMS) },
            onOpenAlbumDetail = { albumId -> navController.navigate("album/$albumId") },
            onOpenDiscovered = { navController.navigate(ROUTE_DISCOVERED) },
            onOpenDiscoveredDetail = { tagId -> navigateToDiscoveredAlbum(tagId) },
            onOpenHidden = {
                hiddenViewModel.lock()
                navController.navigate(ROUTE_HIDDEN)
            },
            onOpenRecentlyDeleted = { navController.navigate(ROUTE_RECYCLE_BIN) },
            onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            onOpenReorderHome = { navController.navigate(ROUTE_HOME_REORDER) },
            onOpenCategory = { category ->
                navController.navigate(if (category == MediaCategory.DUPLICATES) ROUTE_DUPLICATES else "category/${category.name}")
            },
            onOpenCalendar = { navController.navigate(ROUTE_CALENDAR) },
            onItemClick = { index, bounds, backdrop ->
                capturedZoomLaunch = ZoomLaunch(bounds, backdrop, ZoomSources.snapshotBounds())
                viewerItemsOverride = null
                navController.navigate("viewer/$index")
            },
            duplicatesMlEnabled = "duplicate" in mlEnabledTags
        )
    }
    composable(ROUTE_HOME_REORDER) {
        ReorderHomeScreen(
            initialOrder = homeSectionOrder,
            mediaTypesOrder = homeMediaTypesOrder,
            utilitiesOrder = visibleUtilitiesOrder,
            hiddenSections = homeHiddenSections,
            onReorder = { order -> settingsViewModel.setHomeSectionOrder(order) },
            onMediaTypesReorder = settingsViewModel::setHomeMediaTypesOrder,
            onUtilitiesReorder = { order ->
                // Keep the Hidden tile at its place while it is not shown in this list
                val hiddenIndex = homeUtilitiesOrder.indexOf("HIDDEN")
                val full = if (hiddenSectionEnabled || hiddenIndex < 0) order
                else order.toMutableList().also { it.add(hiddenIndex.coerceAtMost(it.size), "HIDDEN") }
                settingsViewModel.setHomeUtilitiesOrder(full)
            },
            onHiddenSectionsChange = settingsViewModel::setHomeHiddenSections,
            onBack = { navController.popBackStack() }
        )
    }
    composable(ROUTE_HIDDEN_CUSTOMIZE) {
        HiddenHomeCustomizeScreen(
            hiddenSections = hiddenHomeHiddenSections,
            onHiddenSectionsChange = settingsViewModel::setHiddenHomeHiddenSections,
            onBack = { navController.popBackStack() }
        )
    }
    composable(
        ROUTE_GALLERY,
        exitTransition = {
            if (targetState.destination.route == "viewer/{index}") {
                ExitTransition.None
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(Motion.Medium))
            }
        },
        popEnterTransition = {
            if (initialState.destination.route == "viewer/{index}") {
                EnterTransition.None
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(Motion.Medium))
            }
        }
    ) {
        GalleryScreen(
            items = uiState.visibleItems,
            filter = uiState.filter,
            gridStyle = if (gridColumns == 1) GridStyle.ORIGINAL else gridStyle,
            gridColumns = gridColumns,
            onGridColumnsChange = settingsViewModel::setGridColumns,
            groupBy = if (gridColumns == 1) GroupBy.NONE else groupBy,
            existingAlbums = albumTargets,
            onFilterChange = viewModel::setFilter,
            sortOrder = uiState.sortOrder,
            onSortOrderChange = viewModel::setSortOrder,
            onItemClick = { index, bounds, backdrop ->
                capturedZoomLaunch = ZoomLaunch(bounds, backdrop, ZoomSources.snapshotBounds())
                viewerItemsOverride = null
                navController.navigate("viewer/$index")
            },
            onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
            onMoveToRecycleBin = { item -> viewModel.moveToRecycleBin(item) },
            hideEnabled = hiddenSectionEnabled,
            onHideSelected = { items -> hideItems(items) },
            onAddToAlbum = { albumId, item -> albumViewModel.addToAlbum(albumId, item.id) },
            onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
            onDuplicate = { item ->
                coroutineScope.launch {
                    if (MediaStoreRepository(context).duplicate(item) != null) viewModel.load()
                }
            },
            onCopy = { item ->
                coroutineScope.launch {
                    val copyUri = withContext(Dispatchers.IO) {
                        createExifStrippedShareUri(context, item, removeExifOnShare)
                    }
                    if (copyUri != null) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, appString(R.string.photo_viewer_photo), copyUri))
                    }
                }
            },
            shareEnabled = !sharingDisabled,
            removeExifOnShare = removeExifOnShare,
            onBack = { navController.popBackStack() }
        )
    }
    composable(ROUTE_ALBUMS) {
        AlbumsScreen(
            albums = albumsWithPreview,
            gridColumns = gridColumns,
            onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
            onCreateAlbum = { name -> albumViewModel.createAlbum(name) },
            onDeleteAlbums = { ids -> ids.forEach { albumViewModel.deleteAlbum(it) } },
            onRenameAlbum = { id, name -> albumViewModel.renameAlbum(id, name) },
            onCreateFolder = { parentId, name -> albumViewModel.createFolder(parentId, name) },
            onMergeAlbum = { id -> navController.navigate("album/$id/merge") },
            itemsOfAlbums = { ids ->
                val allIds = ids + albumEntities.filter { it.parentId in ids }.map { it.id }
                val memberIds = albumMemberships.filter { it.albumId in allIds }.map { it.mediaId }.toSet()
                uiState.allItems.filter { it.id in memberIds }
            },
            removeExifOnShare = removeExifOnShare,
            onBack = { navController.popBackStack() }
        )
    }
    composable(
        "album/{id}",
        exitTransition = {
            if (targetState.destination.route == "viewer/{index}") {
                ExitTransition.None
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(Motion.Medium))
            }
        },
        popEnterTransition = {
            if (initialState.destination.route == "viewer/{index}") {
                EnterTransition.None
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(Motion.Medium))
            }
        }
    ) { entry ->
        val albumId = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
        val autoAlbumIds by albumViewModel.autoAlbums.collectAsStateWithLifecycle()
        val album = allCollections.find { it.id == albumId }
        val albumFolders = remember(allCollections, albumId) { allCollections.filter { it.parentId == albumId } }
        val memberIds = remember(albumMemberships, albumId) {
            albumMemberships.filter { it.albumId == albumId }.map { it.mediaId }.toSet()
        }
        val albumPhotos = remember(uiState.allItems, memberIds) {
            uiState.allItems.filter { it.id in memberIds }
        }
        AlbumDetailScreen(
            albumName = album?.name ?: appString(R.string.nooklin_photos_app_album),
            photos = albumPhotos,
            gridColumns = gridColumns,
            groupActions = groupActions,
            onBack = { navController.popBackStack() },
            onItemClick = { item, bounds, backdrop ->
                val index = albumPhotos.indexOf(item)
                capturedZoomLaunch = ZoomLaunch(bounds, backdrop, ZoomSources.snapshotBounds())
                viewerItemsOverride = StableList(albumPhotos)
                navController.navigate("viewer/$index")
            },
            onRename = { newName -> albumViewModel.renameAlbum(albumId, newName) },
            onMergeRequested = { navController.navigate("album/$albumId/merge") },
            onDeleteAlbum = { albumViewModel.deleteAlbum(albumId) { navController.popBackStack() } },
            onAddPhotos = { navController.navigate("album/$albumId/pick") },
            isFolder = album?.parentId != null,
            folders = albumFolders,
            totalCount = album?.itemCount ?: albumPhotos.size,
            onFolderClick = { folderId -> navController.navigate("album/$folderId") },
            onRenameFolder = { id, name -> albumViewModel.renameAlbum(id, name) },
            onDeleteFolder = { id -> albumViewModel.deleteAlbum(id) },
            onCreateFolder = { name -> albumViewModel.createFolder(albumId, name) },
            coverMediaId = albumEntities.find { it.id == albumId }?.coverMediaId,
            onSetCover = { mediaId -> albumViewModel.setCover(albumId, mediaId) },
            onRemoveFromAlbum = { item: MediaItem -> albumViewModel.removeFromAlbum(albumId, item.id) }.takeUnless { albumId in autoAlbumIds.values }
        )
    }
    composable("album/{id}/pick") { entry ->
        val albumId = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
        val album = allCollections.find { it.id == albumId }
        val memberIds = remember(albumMemberships, albumId) {
            albumMemberships.filter { it.albumId == albumId }.map { it.mediaId }.toSet()
        }
        AlbumPhotoPickerScreen(
            albumName = album?.name ?: appString(R.string.nooklin_photos_app_album),
            items = uiState.allItems,
            alreadyInAlbum = memberIds,
            gridColumns = gridColumns,
            onConfirm = { selectedIds ->
                selectedIds.forEach { mediaId -> albumViewModel.addToAlbum(albumId, mediaId) }
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }
    composable("album/{id}/merge") { entry ->
        val albumId = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
        val album = albumsWithPreview.find { it.id == albumId }
        AlbumMergeScreen(
            sourceAlbumId = albumId,
            sourceAlbumName = album?.name ?: appString(R.string.nooklin_photos_app_album),
            candidates = albumsWithPreview,
            onMerge = { targetAlbumId ->
                albumViewModel.mergeAlbums(albumId, targetAlbumId) {
                    navController.popBackStack(ROUTE_ALBUMS, inclusive = false)
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}
