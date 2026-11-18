package org.nooklin.photos.ui

import org.nooklin.photos.ui.gallery.OnThisDayScreen
import org.nooklin.photos.ui.gallery.DuplicatesScreen
import org.nooklin.photos.ui.gallery.DuplicateInfo
import org.nooklin.photos.ui.common.freshImageRequest
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import org.nooklin.photos.ui.hidden.HiddenSearchScreen
import org.nooklin.photos.ui.albums.AlbumOrderViewModel
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.GroupBy
import org.nooklin.photos.data.local.HomeSection
import org.nooklin.photos.data.local.LocalMetadataStore
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.common.ZoomLaunch
import org.nooklin.photos.ui.gallery.HiddenCalendarScreen
import org.nooklin.photos.ui.hidden.HiddenFolderGate
import org.nooklin.photos.ui.hidden.HiddenGalleryScreen
import org.nooklin.photos.ui.hidden.HiddenHomeScreen
import org.nooklin.photos.ui.hidden.HiddenPhotoViewerScreen
import org.nooklin.photos.ui.hidden.HiddenPinGate
import org.nooklin.photos.ui.hidden.HiddenUiState
import org.nooklin.photos.ui.hidden.HiddenViewModel
import org.nooklin.photos.ui.settings.SettingsViewModel
import org.nooklin.photos.ui.theme.Motion

internal fun NavGraphBuilder.hiddenBrowseRoutes(
    context: Context,
    viewModel: GalleryViewModel,
    hiddenViewModel: HiddenViewModel,
    albumOrderViewModel: AlbumOrderViewModel,
    settingsViewModel: SettingsViewModel,
    gridStyleState: State<GridStyle>,
    gridColumnsState: State<Int>,
    groupByState: State<GroupBy>,
    hiddenPinSetState: State<Boolean?>,
    removeExifOnShareState: State<Boolean>,
    hiddenAlbumsEnabledState: State<Boolean>,
    hiddenImageSharingEnabledState: State<Boolean>,
    homeMediaTypesOrderState: State<List<String>>,
    homeUtilitiesOrderState: State<List<String>>,
    hiddenHomeHiddenSectionsState: State<Set<HomeSection>>,
    resolvedDarkTheme: Boolean,
    hiddenUiStateHolder: State<HiddenUiState>,
    hiddenTopAlbums: List<HiddenAlbumWithPreview>,
    hiddenTargets: List<HiddenAlbumWithPreview>,
    coroutineScope: CoroutineScope,
    navController: NavHostController,
    capturedZoomLaunchState: MutableState<ZoomLaunch?>,
    hiddenViewerItemsOverrideState: MutableState<List<HiddenItem>?>,
    openViewerZoomed: (Any, String) -> Unit
) {
    val gridStyle by gridStyleState
    val gridColumns by gridColumnsState
    val groupBy by groupByState
    val hiddenPinSet by hiddenPinSetState
    val removeExifOnShare by removeExifOnShareState
    val hiddenAlbumsEnabled by hiddenAlbumsEnabledState
    val hiddenImageSharingEnabled by hiddenImageSharingEnabledState
    val homeMediaTypesOrder by homeMediaTypesOrderState
    val homeUtilitiesOrder by homeUtilitiesOrderState
    val hiddenHomeHiddenSections by hiddenHomeHiddenSectionsState
    val hiddenUiState by hiddenUiStateHolder
    val capturedZoomLaunch by capturedZoomLaunchState
    var hiddenViewerItemsOverride by hiddenViewerItemsOverrideState
    composable(ROUTE_HIDDEN) {
        when {
            !hiddenUiState.isAuthenticated -> HiddenFolderGate(
                onAuthenticated = { hiddenViewModel.onAuthenticated() },
                onCancelled = { navController.popBackStack() }
            )
            // Wait for the confirmed value, never fall through on a null
            hiddenPinSet == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            hiddenPinSet == true && !hiddenUiState.pinVerified -> HiddenPinGate(
                verifyPin = settingsViewModel::verifyHiddenPin,
                verifyRecoveryCode = settingsViewModel::verifyHiddenPinRecoveryCode,
                setNewPin = settingsViewModel::setHiddenPin,
                onUnlocked = { hiddenViewModel.onPinVerified() },
                onCancel = { navController.popBackStack() }
            )
            else -> HiddenHomeScreen(
                items = hiddenUiState.items,
                albums = hiddenTopAlbums,
                albumOrder = albumOrderViewModel.hidden.collectAsStateWithLifecycle().value,
                previewsOff = albumOrderViewModel.hiddenPreviewsOff.collectAsStateWithLifecycle().value,
                hiddenAlbumsEnabled = hiddenAlbumsEnabled,
                gridColumns = gridColumns,
                hiddenSections = hiddenHomeHiddenSections,
                mediaTypesOrder = homeMediaTypesOrder,
                utilitiesOrder = homeUtilitiesOrder,
                onOpenCustomize = { navController.navigate(ROUTE_HIDDEN_CUSTOMIZE) },
                onOpenLibrary = { navController.navigate(ROUTE_HIDDEN_LIBRARY) },
                onOpenAlbums = { navController.navigate(ROUTE_HIDDEN_ALBUMS) },
                onOpenAlbumDetail = { albumId -> navController.navigate("hiddenalbums/$albumId") },
                onOpenVideos = { navController.navigate(ROUTE_HIDDEN_VIDEOS) },
                onOpenSlowMotion = { navController.navigate(ROUTE_HIDDEN_SLOW_MOTION) },
                onOpenRaw = { navController.navigate(ROUTE_HIDDEN_RAW) },
                onOpenScreenshots = { navController.navigate(ROUTE_HIDDEN_SCREENSHOTS) },
                onOpenPanoramas = { navController.navigate(ROUTE_HIDDEN_PANORAMAS) },
                onOpenScreenRecordings = { navController.navigate(ROUTE_HIDDEN_SCREEN_RECORDINGS) },
                onOpenFavorites = { navController.navigate(ROUTE_HIDDEN_FAVORITES) },
                onOpenRecentlyDeleted = { navController.navigate(ROUTE_HIDDEN_RECYCLE_BIN) },
                onOpenRecentlySaved = { navController.navigate(ROUTE_HIDDEN_RECENTLY_SAVED) },
                onOpenCalendar = { navController.navigate(ROUTE_HIDDEN_CALENDAR) },
                onOpenDuplicates = { navController.navigate(ROUTE_HIDDEN_DUPLICATES) },
                onOpenOnThisDay = { navController.navigate(ROUTE_HIDDEN_ON_THIS_DAY) },
                onItemClick = { index -> hiddenViewerItemsOverride = null; openViewerZoomed(hiddenUiState.items[index].file.absolutePath, "hiddenviewer/$index") },
                onBack = { navController.popBackStack() }
            )
        }
    }
    composable(ROUTE_HIDDEN_CALENDAR) {
        HiddenCalendarScreen(
            items = hiddenUiState.items,
            onBack = { navController.popBackStack() },
            onOpenDay = { dayItems ->
                hiddenViewerItemsOverride = dayItems
                openViewerZoomed(dayItems.first().file.absolutePath, "hiddenviewer/0")
            }
        )
    }
    composable(ROUTE_HIDDEN_LIBRARY) {
        HiddenGalleryScreen(
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            items = hiddenUiState.visibleItems,
            gridColumns = gridColumns,
            onGridColumnsChange = settingsViewModel::setGridColumns,
            existingAlbums = hiddenTargets,
            onItemClick = { index -> openViewerZoomed(hiddenUiState.visibleItems[index].file.absolutePath, "hiddenlibraryviewer/$index") },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) },
            onBack = { navController.popBackStack() },
            showFilterMenu = true,
            filter = hiddenUiState.filter,
            onFilterChange = hiddenViewModel::setFilter,
            hideScreenshots = hiddenUiState.hideScreenshots,
            onHideScreenshotsChange = hiddenViewModel::setHideScreenshots,
            sortOrder = hiddenUiState.sortOrder,
            onSortOrderChange = hiddenViewModel::setSortOrder,
            gridStyle = gridStyle,
            groupBy = groupBy,
            onOpenSearch = { navController.navigate(ROUTE_HIDDEN_SEARCH) }
        )
    }
    composable(ROUTE_HIDDEN_DUPLICATES) {
        var groups by remember { mutableStateOf(emptyList<List<HiddenItem>>()) }
        LaunchedEffect(hiddenUiState.items) {
            hiddenViewModel.findDuplicateGroups(hiddenUiState.items) { found -> groups = found }
        }
        val liveGroups = remember(groups, hiddenUiState.items) {
            val names = hiddenUiState.items.map { it.file.name }.toSet()
            groups.map { group -> group.filter { it.file.name in names } }.filter { it.size >= 2 }
        }
        DuplicatesScreen(
            groups = liveGroups,
            info = { item ->
                DuplicateInfo(item.file.name, item.width, item.height, item.file.length()) { modifier ->
                    AsyncImage(
                        model = freshImageRequest(context, item.thumbnailFile, item.thumbnailFile.lastModified()),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = modifier
                    )
                }
            },
            onBack = { navController.popBackStack() },
            onOpenGroup = { group, startIndex ->
                hiddenViewerItemsOverride = group
                openViewerZoomed(group[startIndex].file.absolutePath, "hiddenviewer/$startIndex")
            },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) }
        )
    }
    composable(ROUTE_HIDDEN_ON_THIS_DAY) {
        OnThisDayScreen(
            items = hiddenUiState.items,
            timeMillis = { it.sortDate },
            zoomKey = { it.file.absolutePath },
            gridColumns = gridColumns,
            onBack = { navController.popBackStack() },
            onItemClick = { all, index ->
                hiddenViewerItemsOverride = all
                openViewerZoomed(all[index].file.absolutePath, "hiddenviewer/$index")
            }
        ) { item, modifier ->
            AsyncImage(
                model = freshImageRequest(context, item.thumbnailFile, item.thumbnailFile.lastModified()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
    }
    composable(ROUTE_HIDDEN_SEARCH) {
        HiddenSearchScreen(
            items = hiddenUiState.items,
            gridColumns = gridColumns,
            onBack = { navController.popBackStack() },
            onItemClick = { item, results ->
                hiddenViewerItemsOverride = results
                openViewerZoomed(item.file.absolutePath, "hiddenviewer/${results.indexOf(item)}")
            }
        )
    }
    composable(ROUTE_HIDDEN_FAVORITES) {
        val hiddenFavorites = hiddenUiState.items.filter { it.isFavorite }
        val hiddenFavoritesCover by remember { LocalMetadataStore(context).hiddenFavoritesCover }.collectAsStateWithLifecycle(initialValue = null)
        HiddenGalleryScreen(
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            items = hiddenFavorites,
            gridColumns = gridColumns,
            existingAlbums = hiddenTargets,
            title = appString(R.string.common_favorites),
            showHero = true,
            heroCoverName = hiddenFavoritesCover,
            onSetHeroCover = { name -> coroutineScope.launch { LocalMetadataStore(context).setHiddenFavoritesCover(name) } },
            onItemClick = { index -> openViewerZoomed(hiddenFavorites[index].file.absolutePath, "hiddenfavoritesviewer/$index") },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("hiddenfavoritesviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val hiddenFavorites = hiddenUiState.items.filter { it.isFavorite }
        HiddenPhotoViewerScreen(
            items = StableList(hiddenFavorites),
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
    composable(ROUTE_HIDDEN_VIDEOS) {
        val hiddenVideos = hiddenUiState.items.filter { it.isVideo }
        HiddenGalleryScreen(
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            items = hiddenVideos,
            gridColumns = gridColumns,
            existingAlbums = hiddenTargets,
            title = appString(R.string.common_videos),
            onItemClick = { index -> openViewerZoomed(hiddenVideos[index].file.absolutePath, "hiddenvideosviewer/$index") },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("hiddenvideosviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val hiddenVideos = hiddenUiState.items.filter { it.isVideo }
        HiddenPhotoViewerScreen(
            items = StableList(hiddenVideos),
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

    // Slow Motion / RAW / Screenshots: flags captured at hide-time
    composable(ROUTE_HIDDEN_SLOW_MOTION) {
        val hiddenSlowMotion = hiddenUiState.items.filter { it.isSlowMotion }
        HiddenGalleryScreen(
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            items = hiddenSlowMotion,
            gridColumns = gridColumns,
            existingAlbums = hiddenTargets,
            title = appString(R.string.common_slow_motion),
            onItemClick = { index -> openViewerZoomed(hiddenSlowMotion[index].file.absolutePath, "hiddenslowmotionviewer/$index") },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("hiddenslowmotionviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val hiddenSlowMotion = hiddenUiState.items.filter { it.isSlowMotion }
        HiddenPhotoViewerScreen(
            items = StableList(hiddenSlowMotion),
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

    composable(ROUTE_HIDDEN_RAW) {
        val hiddenRaw = hiddenUiState.items.filter { it.isRaw }
        HiddenGalleryScreen(
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            items = hiddenRaw,
            gridColumns = gridColumns,
            existingAlbums = hiddenTargets,
            title = appString(R.string.common_raw),
            onItemClick = { index -> openViewerZoomed(hiddenRaw[index].file.absolutePath, "hiddenrawviewer/$index") },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("hiddenrawviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val hiddenRaw = hiddenUiState.items.filter { it.isRaw }
        HiddenPhotoViewerScreen(
            items = StableList(hiddenRaw),
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

    composable(ROUTE_HIDDEN_SCREENSHOTS) {
        val hiddenScreenshots = hiddenUiState.items.filter { it.isScreenshot }
        HiddenGalleryScreen(
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            items = hiddenScreenshots,
            gridColumns = gridColumns,
            existingAlbums = hiddenTargets,
            title = appString(R.string.common_screenshots),
            onItemClick = { index -> openViewerZoomed(hiddenScreenshots[index].file.absolutePath, "hiddenscreenshotsviewer/$index") },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("hiddenscreenshotsviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val hiddenScreenshots = hiddenUiState.items.filter { it.isScreenshot }
        HiddenPhotoViewerScreen(
            items = StableList(hiddenScreenshots),
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

    composable(ROUTE_HIDDEN_PANORAMAS) {
        val hiddenPanoramas = hiddenUiState.items.filter { it.isPanorama }
        HiddenGalleryScreen(
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            items = hiddenPanoramas,
            gridColumns = gridColumns,
            existingAlbums = hiddenTargets,
            title = appString(R.string.common_panoramas),
            onItemClick = { index -> openViewerZoomed(hiddenPanoramas[index].file.absolutePath, "hiddenpanoramasviewer/$index") },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("hiddenpanoramasviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val hiddenPanoramas = hiddenUiState.items.filter { it.isPanorama }
        HiddenPhotoViewerScreen(
            items = StableList(hiddenPanoramas),
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

    composable(ROUTE_HIDDEN_SCREEN_RECORDINGS) {
        val hiddenScreenRecordings = hiddenUiState.items.filter { it.isScreenRecording }
        HiddenGalleryScreen(
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            items = hiddenScreenRecordings,
            gridColumns = gridColumns,
            existingAlbums = hiddenTargets,
            title = appString(R.string.common_screen_recordings),
            onItemClick = { index -> openViewerZoomed(hiddenScreenRecordings[index].file.absolutePath, "hiddenscreenrecordingsviewer/$index") },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("hiddenscreenrecordingsviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val hiddenScreenRecordings = hiddenUiState.items.filter { it.isScreenRecording }
        HiddenPhotoViewerScreen(
            items = StableList(hiddenScreenRecordings),
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

    composable(ROUTE_HIDDEN_RECENTLY_SAVED) {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val hiddenRecentlySaved = hiddenUiState.items
            .filter { it.hiddenAt >= cutoff }
            .sortedByDescending { it.hiddenAt }
        HiddenGalleryScreen(
            shareEnabled = hiddenImageSharingEnabled,
            removeExifOnShare = removeExifOnShare,
            items = hiddenRecentlySaved,
            gridColumns = gridColumns,
            existingAlbums = hiddenTargets,
            title = appString(R.string.common_recently_saved),
            onItemClick = { index -> openViewerZoomed(hiddenRecentlySaved[index].file.absolutePath, "hiddenrecentlysavedviewer/$index") },
            onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
            onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
            onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
            onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
            onDuplicate = { item -> hiddenViewModel.duplicate(item) },
            onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("hiddenrecentlysavedviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val hiddenRecentlySaved = hiddenUiState.items
            .filter { it.hiddenAt >= cutoff }
            .sortedByDescending { it.hiddenAt }
        HiddenPhotoViewerScreen(
            items = StableList(hiddenRecentlySaved),
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
}
