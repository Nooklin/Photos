package org.nooklin.photos.ui

import org.nooklin.photos.data.HiddenItem

import org.nooklin.photos.ui.calendar.CalendarScreen
import org.nooklin.photos.ui.calendar.HiddenCalendarScreen
import org.nooklin.photos.ui.duplicates.DuplicatesScreen
import org.nooklin.photos.ui.common.GroupActions
import org.nooklin.photos.ui.common.ZoomReturnOverlay

import org.nooklin.photos.ui.common.ZoomSources

import org.nooklin.photos.ui.common.ZoomLaunch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.nooklin.photos.data.local.LocalMetadataStore
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.MediaStoreRepository
import org.nooklin.photos.data.createExifStrippedShareUri
import org.nooklin.photos.appString
import kotlinx.coroutines.launch
import org.nooklin.photos.data.MediaCategory
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.settings.SettingsHiddenImportScreen
import org.nooklin.photos.ui.settings.LibraryTransferViewModel
import org.nooklin.photos.ui.settings.SettingsBackupImportScreen
import org.nooklin.photos.ui.settings.SettingsLibraryBackupScreen
import org.nooklin.photos.toMediaStoreId
import org.nooklin.photos.data.local.AppThemeMode
import org.nooklin.photos.ui.albums.AlbumDetailScreen
import org.nooklin.photos.ui.albums.AlbumPhotoPickerScreen
import org.nooklin.photos.ui.albums.AlbumMergeScreen
import org.nooklin.photos.ui.albums.AlbumViewModel
import org.nooklin.photos.ui.common.BackupNoticeDialog
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.albums.AlbumsScreen
import org.nooklin.photos.ui.category.CategoryScreen
import org.nooklin.photos.ui.discovered.DiscoveredAlbum
import org.nooklin.photos.ui.discovered.DiscoveredDetailScreen
import org.nooklin.photos.ui.discovered.DiscoveredScreen
import org.nooklin.photos.ui.discovered.DiscoveredViewModel
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.GroupBy
import org.nooklin.photos.data.local.MlTag
import org.nooklin.photos.ui.editor.EditTarget
import org.nooklin.photos.ui.editor.PhotoEditorScreen
import org.nooklin.photos.ui.gallery.GalleryScreen
import org.nooklin.photos.ui.hidden.HiddenAlbumDetailScreen
import org.nooklin.photos.ui.hidden.HiddenAlbumPhotoPickerScreen
import org.nooklin.photos.ui.hidden.HiddenAlbumsScreen
import org.nooklin.photos.ui.hidden.HiddenFolderGate
import org.nooklin.photos.ui.hidden.HiddenGalleryScreen
import org.nooklin.photos.ui.hidden.HiddenHomeScreen
import org.nooklin.photos.ui.hidden.HiddenPhotoViewerScreen
import org.nooklin.photos.ui.hidden.HiddenPinGate
import org.nooklin.photos.ui.hidden.HiddenViewModel
import org.nooklin.photos.ui.home.HomeScreen
import org.nooklin.photos.ui.home.HiddenHomeCustomizeScreen
import org.nooklin.photos.ui.home.ReorderHomeScreen
import org.nooklin.photos.ui.people.PeopleScreen
import org.nooklin.photos.ui.people.PeopleViewModel
import org.nooklin.photos.ui.people.PersonDetailScreen
import org.nooklin.photos.ui.people.PersonPickerEntry
import org.nooklin.photos.ui.people.PersonWithPreview
import org.nooklin.photos.ui.pets.PetDetailScreen
import org.nooklin.photos.ui.pets.PetViewModel
import org.nooklin.photos.ui.pets.PetWithPreview
import org.nooklin.photos.ui.pets.PetsScreen
import org.nooklin.photos.ui.recyclebin.RecycleBinScreen
import org.nooklin.photos.ui.recyclebin.RecycleBinViewerScreen
import org.nooklin.photos.ui.search.SearchScreen
import org.nooklin.photos.ui.settings.SettingsHiddenBackupScreen
import org.nooklin.photos.ui.settings.SettingsHiddenBackupSelectScreen
import org.nooklin.photos.ui.settings.SettingsViewModel
import org.nooklin.photos.ui.settings.settingsNavGraph
import org.nooklin.photos.ui.theme.DesignStyle
import org.nooklin.photos.ui.theme.NooklinTheme
import org.nooklin.photos.ui.viewer.PhotoViewerScreen
import org.nooklin.photos.ui.editor.VideoTrimScreen
import org.nooklin.photos.R
import org.nooklin.photos.ui.theme.Motion

// READ_MEDIA_VIDEO must be requested too, else MediaStore silently hides every video
private fun mediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private const val ROUTE_HOME = "home"
private const val ROUTE_HOME_REORDER = "home/reorder"
private const val ROUTE_GALLERY = "gallery"
private const val ROUTE_ALBUMS = "albums"
private const val ROUTE_RECYCLE_BIN = "recyclebin"
private const val ROUTE_HIDDEN = "hidden"
private const val ROUTE_HIDDEN_LIBRARY = "hiddenlibrary"
private const val ROUTE_HIDDEN_CUSTOMIZE = "hiddencustomize"
private const val ROUTE_HIDDEN_ALBUMS = "hiddenalbums"
private const val ROUTE_HIDDEN_FAVORITES = "hiddenfavorites"
private const val ROUTE_HIDDEN_RECYCLE_BIN = "hiddenrecyclebin"
private const val ROUTE_HIDDEN_VIDEOS = "hiddenvideos"
private const val ROUTE_HIDDEN_SLOW_MOTION = "hiddenslowmotion"
private const val ROUTE_HIDDEN_RAW = "hiddenraw"
private const val ROUTE_HIDDEN_SCREENSHOTS = "hiddenscreenshots"
private const val ROUTE_HIDDEN_PANORAMAS = "hiddenpanoramas"
private const val ROUTE_HIDDEN_SCREEN_RECORDINGS = "hiddenscreenrecordings"
private const val ROUTE_HIDDEN_RECENTLY_SAVED = "hiddenrecentlysaved"
private const val ROUTE_HIDDEN_CALENDAR = "hiddencalendar"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_CALENDAR = "calendar"
private const val ROUTE_DUPLICATES = "duplicates"
private const val ROUTE_DISCOVERED = "discovered"
private const val ROUTE_PEOPLE = "people"
private const val ROUTE_PETS = "pets"

// Home is the single entry, no bottom nav.
// App lock: one biometric check per cold start. Disable Screenshots sets FLAG_SECURE.
@Composable
fun NooklinPhotosApp(
    externalRequest: org.nooklin.photos.ExternalRequest? = null,
    onExternalFinish: (List<Uri>?) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GalleryViewModel = viewModel()
    val hiddenViewModel: HiddenViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val albumViewModel: AlbumViewModel = viewModel()
    val libraryTransferViewModel: LibraryTransferViewModel = viewModel()
    val discoveredViewModel: DiscoveredViewModel = viewModel()
    val petViewModel: PetViewModel = viewModel()
    val peopleViewModel: PeopleViewModel = viewModel()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val contrastColor by settingsViewModel.contrastColor.collectAsStateWithLifecycle()
    val appFont by settingsViewModel.appFont.collectAsStateWithLifecycle()
    val gridStyle by settingsViewModel.gridStyle.collectAsStateWithLifecycle()
    val gridColumns by settingsViewModel.gridColumns.collectAsStateWithLifecycle()
    val libraryPreviewRows by settingsViewModel.libraryPreviewRows.collectAsStateWithLifecycle()
    val groupBy by settingsViewModel.groupBy.collectAsStateWithLifecycle()
    val mlEnabled by settingsViewModel.mlEnabled.collectAsStateWithLifecycle()
    val mlEnabledTags by settingsViewModel.mlEnabledTags.collectAsStateWithLifecycle()
    val scanProgress by settingsViewModel.scanProgress.collectAsStateWithLifecycle()
    val appLockEnabled by settingsViewModel.appLockEnabled.collectAsStateWithLifecycle()
    val hiddenPinSet by settingsViewModel.hiddenPinSet.collectAsStateWithLifecycle()
    val disableScreenshots by settingsViewModel.disableScreenshots.collectAsStateWithLifecycle()
    val removeExifOnShare by settingsViewModel.removeExifOnShare.collectAsStateWithLifecycle()
    val sharingDisabled by settingsViewModel.sharingDisabled.collectAsStateWithLifecycle()
    val hiddenSectionEnabled by settingsViewModel.hiddenSectionEnabled.collectAsStateWithLifecycle()
    val hiddenAlbumsEnabled by settingsViewModel.hiddenAlbumsEnabled.collectAsStateWithLifecycle()
    val hiddenImageSharingEnabled by settingsViewModel.hiddenImageSharingEnabled.collectAsStateWithLifecycle()
    val hiddenBackupNoticeAcknowledged by settingsViewModel.hiddenBackupNoticeAcknowledged.collectAsStateWithLifecycle()
    val scanCompleteNotificationEnabled by settingsViewModel.scanCompleteNotificationEnabled.collectAsStateWithLifecycle()
    val homeSectionOrder by settingsViewModel.homeSectionOrder.collectAsStateWithLifecycle()
    val homeMediaTypesOrder by settingsViewModel.homeMediaTypesOrder.collectAsStateWithLifecycle()
    val homeUtilitiesOrder by settingsViewModel.homeUtilitiesOrder.collectAsStateWithLifecycle()
    val visibleUtilitiesOrder = if (hiddenSectionEnabled) homeUtilitiesOrder else homeUtilitiesOrder - "HIDDEN"
    val homeHiddenSections by settingsViewModel.homeHiddenSections.collectAsStateWithLifecycle()
    val hiddenHomeHiddenSections by settingsViewModel.hiddenHomeHiddenSections.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()
    val resolvedDarkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.BLACK -> true
        AppThemeMode.SYSTEM -> systemIsDark
    }
    val isBlackMode = themeMode == AppThemeMode.BLACK

    // Status bar icons follow our theme, not the system one
    val view = LocalView.current
    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        // Dark icons for a light background, the inverse of resolvedDark
        insetsController.isAppearanceLightStatusBars = !resolvedDarkTheme
        insetsController.isAppearanceLightNavigationBars = !resolvedDarkTheme
    }

    val appUnlocked by settingsViewModel.appUnlocked.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            mediaPermissions().all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Set when the permission dialog was answered, so a file opened from another app knows whether to wait or fall back
    var permissionAnswered by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = mediaPermissions().all { results[it] == true }
        permissionAnswered = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(mediaPermissions())
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.load()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // A file opened from another app (for example the camera): find it in the library once that is loaded,
    // so it opens in the normal viewer. Not found, or no permission: the single file screen is used.
    val externalView = externalRequest as? org.nooklin.photos.ExternalRequest.View
    var externalItem by remember(externalView?.uri) { mutableStateOf<MediaItem?>(null) }
    var externalLookupDone by remember(externalView?.uri) { mutableStateOf(false) }
    LaunchedEffect(externalView?.uri, hasPermission, uiState.isLoading) {
        if (externalView == null || !hasPermission || uiState.isLoading) return@LaunchedEffect
        val id = externalView.uri.toMediaStoreId()
        externalItem = if (id == null) null else uiState.allItems.find { it.id == id } ?: viewModel.findMediaItemById(id)
        externalLookupDone = true
    }
    val recycleBinEntries by viewModel.recycleBinEntries.collectAsStateWithLifecycle()
    val hiddenUiState by hiddenViewModel.uiState.collectAsStateWithLifecycle()
    // Hidden albums without their folders, and the add-to list with the folders named Album / Folder
    val hiddenTopAlbums = remember(hiddenUiState.albums) { hiddenUiState.albums.filter { it.parentId == null } }
    val hiddenTargets = remember(hiddenUiState.albums) {
        hiddenTopAlbums.flatMap { album ->
            listOf(album) + hiddenUiState.albums.filter { it.parentId == album.id }
                .map { folder -> folder.copy(name = album.name + " / " + folder.name) }
        }
    }

    // Hidden content must stay out of recents and screen capture even when the optional setting is off
    val secureWindow = disableScreenshots || hiddenUiState.isAuthenticated
    LaunchedEffect(secureWindow) {
        (context as? Activity)?.window?.let { window ->
            if (secureWindow) {
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()

    val albumEntities by albumViewModel.albums.collectAsStateWithLifecycle()
    val albumMemberships by albumViewModel.memberships.collectAsStateWithLifecycle()
    val petEntities by petViewModel.pets.collectAsStateWithLifecycle()
    val petMemberships by petViewModel.memberships.collectAsStateWithLifecycle()

    // Albums and their folders in one list, a folder has a parentId
    val allCollections = remember(albumEntities, albumMemberships, uiState.allItems) {
        val membershipsByAlbum = albumMemberships.groupBy { it.albumId }
        val folderCounts = albumEntities.mapNotNull { it.parentId }.groupingBy { it }.eachCount()
        val idsByAlbum = albumEntities.associate { album ->
            album.id to membershipsByAlbum[album.id].orEmpty().map { it.mediaId }.toSet()
        }
        val foldersByParent = albumEntities.filter { it.parentId != null }.groupBy { it.parentId }
        fun frontOf(album: org.nooklin.photos.data.local.AlbumEntity, memberItems: List<org.nooklin.photos.data.MediaItem>) =
            memberItems.find { it.id == album.coverMediaId } ?: memberItems.firstOrNull()
        albumEntities.map { album ->
            val memberIds = idsByAlbum[album.id].orEmpty()
            // Filter the sorted items so the first 4 are the newest
            val memberItems = uiState.allItems.filter { it.id in memberIds }
            // The chosen cover comes first in the preview
            val cover = memberItems.find { it.id == album.coverMediaId }
            val folders = foldersByParent[album.id].orEmpty()
            // An album with folders shows the front of its first 4 folders, and counts the photos of its folders too
            val folderFronts = folders.mapNotNull { folder ->
                frontOf(folder, uiState.allItems.filter { it.id in idsByAlbum[folder.id].orEmpty() })
            }.take(4)
            val totalIds = memberIds + folders.flatMap { idsByAlbum[it.id].orEmpty() }
            AlbumWithPreview(
                id = album.id,
                name = album.name,
                itemCount = if (totalIds.size == memberIds.size) memberItems.size else uiState.allItems.count { it.id in totalIds },
                previewItems = when {
                    folderFronts.isNotEmpty() -> folderFronts
                    cover != null -> listOf(cover) + memberItems.filter { it !== cover }.take(3)
                    else -> memberItems.take(4)
                },
                parentId = album.parentId,
                folderCount = folders.size
            )
        }
    }
    // Only albums: Albums screen, Home and Merge
    val albumsWithPreview = remember(allCollections) { allCollections.filter { it.parentId == null } }
    // Every place where a photo is added to an album also offers the folders, named Album / Folder
    val albumTargets = remember(allCollections) {
        val topLevel = allCollections.filter { it.parentId == null }
        topLevel.flatMap { album ->
            listOf(album) + allCollections.filter { it.parentId == album.id }
                .map { folder -> folder.copy(name = album.name + " / " + folder.name) }
        }
    }

    // One set of selection actions for every group screen (Media Types, albums, Search, pets)
    val groupActions = GroupActions(
        albums = albumTargets,
        removeExifOnShare = removeExifOnShare,
        onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
        onMoveToRecycleBin = { item -> viewModel.moveToRecycleBin(item) },
        onAddToAlbum = { albumId, item -> albumViewModel.addToAlbum(albumId, item.id) }
    )

    val petsWithPreview = remember(petEntities, petMemberships, uiState.allItems) {
        val membershipsByPet = petMemberships.groupBy { it.petId }
        petEntities.map { pet ->
            val memberIds = membershipsByPet[pet.id].orEmpty().map { it.mediaId }.toSet()
            val memberItems = uiState.allItems.filter { it.id in memberIds }
            PetWithPreview(
                id = pet.id,
                name = pet.name,
                itemCount = memberItems.size,
                previewItems = memberItems.take(4)
            )
        }
    }

    // Orphans only appear when items disappear, so pruning waits for the first load or a smaller library
    val lastPrunedTotal = remember { intArrayOf(-1) }
    LaunchedEffect(uiState.allItems, recycleBinEntries) {
        albumViewModel.seedIfNeeded(uiState.allItems)
        if (hasPermission && uiState.allItems.isNotEmpty()) {
            val total = uiState.allItems.size + recycleBinEntries.size
            if (lastPrunedTotal[0] < 0 || total < lastPrunedTotal[0]) {
                // Recycled items are not in allItems but must keep their memberships for Restore
                val validIds = uiState.allItems.map { it.id }.toSet() + recycleBinEntries.map { it.mediaId }
                albumViewModel.pruneOrphanedMemberships(validIds)
                petViewModel.pruneOrphanedMemberships(validIds)
                settingsViewModel.pruneOrphanedAnalysisData(validIds)
            }
            lastPrunedTotal[0] = total
        }
    }

    // Built once per library change and shared by the Discovered and People previews
    val itemsById = remember(uiState.allItems) { uiState.allItems.associateBy { it.id } }
    val analysisTags by discoveredViewModel.tags.collectAsStateWithLifecycle()

    // One album per tag present in photo_tags, empty ones are dropped
    val discoveredAlbums = remember(analysisTags, itemsById) {
        analysisTags.filter { it.tag != MlTag.PETS.id }.groupBy { it.tag }.mapNotNull { (tagId, rows) ->
            val label = MlTag.fromId(tagId)?.label ?: tagId.replaceFirstChar { it.uppercase() }
            val matchedItems = rows.mapNotNull { itemsById[it.mediaId] }
            if (matchedItems.isEmpty()) {
                null
            } else {
                DiscoveredAlbum(
                    tagId = tagId,
                    label = label,
                    itemCount = matchedItems.size,
                    previewItems = matchedItems.take(4)
                )
            }
        }.sortedByDescending { it.itemCount }
    }

    val people by peopleViewModel.people.collectAsStateWithLifecycle()
    val faces by peopleViewModel.faces.collectAsStateWithLifecycle()

    // Named people first, then by item count. "Unknown N" comes from id order at display time.
    val peopleWithPreview = remember(people, faces, itemsById) {
        val facesByPerson = faces.groupBy { it.personId }
        val unnamedLabels = people.filter { it.name == null }
            .sortedBy { it.id }
            .mapIndexed { index, p -> p.id to appString(R.string.nooklin_photos_app_unknown, index + 1) }
            .toMap()
        people.mapNotNull { person ->
            val personFaces = facesByPerson[person.id].orEmpty()
            val mediaIds = personFaces.map { it.mediaId }.distinct()
            val items = mediaIds.mapNotNull { itemsById[it] }
            // Highest confidence gives the cleanest crop
            val representative = personFaces.maxByOrNull { it.confidence }
            val representativeItem = representative?.let { itemsById[it.mediaId] }
            if (items.isEmpty() || representative == null || representativeItem == null) {
                null
            } else {
                PersonWithPreview(
                    id = person.id,
                    displayName = person.name ?: unnamedLabels[person.id] ?: appString(R.string.nooklin_photos_app_unknown_2),
                    isNamed = person.name != null,
                    itemCount = items.size,
                    representativePhotoUri = representativeItem.uri,
                    boxXMin = representative.boxXMin,
                    boxYMin = representative.boxYMin,
                    boxXMax = representative.boxXMax,
                    boxYMax = representative.boxYMax
                )
            }
        }.sortedWith(compareBy({ !it.isNamed }, { -it.itemCount }))
    }

    val peopleByDisplayName = remember(peopleWithPreview) {
        peopleWithPreview.associateBy({ it.id }, { it.displayName })
    }

    // One place decides where a Discovered tap goes (People opens its own list)

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        hiddenViewModel.finishHide(result.resultCode == Activity.RESULT_OK)
        viewModel.load()
        hiddenViewModel.refresh()
    }

    fun hideItem(item: MediaItem) {
        coroutineScope.launch {
            // Null: the private copy failed, never delete the original without a backup
            val pendingIntent = hiddenViewModel.beginHide(item) ?: return@launch
            deleteRequestLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        }
    }

    // Bulk hide shares deleteRequestLauncher
    fun hideItems(items: List<MediaItem>) {
        coroutineScope.launch {
            val pendingIntent = hiddenViewModel.beginHideMultiple(items) ?: return@launch
            deleteRequestLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        }
    }

    // Hidden Recently Deleted comes from the Hidden database, the files never leave the app. Stable id per file name.
    val hiddenRecycleBinEntries = hiddenUiState.deleted.map {
        LocalMetadataStore.RecycleBinEntry(
            mediaId = it.file.name.hashCode().toLong(),
            uri = Uri.fromFile(it.file).toString(),
            deletedAt = it.deletedAt ?: 0L,
            wasHidden = true,
            width = it.width,
            height = it.height,
            isVideo = it.isVideo
        )
    }
    fun hiddenFileNamesFor(mediaIds: Collection<Long>): List<String> =
        hiddenUiState.deleted.map { it.file.name }.filter { it.hashCode().toLong() in mediaIds }

    NooklinTheme(
        style = DesignStyle.IOS,
        darkTheme = resolvedDarkTheme,
        contrastColor = contrastColor,
        appFont = appFont,
        blackMode = isBlackMode
    ) {
        // Null until DataStore has loaded: show nothing so no lock screen flashes
        if (appLockEnabled == null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            return@NooklinTheme
        }
        if (appLockEnabled == true && !appUnlocked) {
            HiddenFolderGate(
                title = appString(R.string.nooklin_photos_app_unlock_nooklin_photos),
                onAuthenticated = { settingsViewModel.unlockApp() },
                onCancelled = { /* stays locked, retry happens on next recomposition */ }
            )
            return@NooklinTheme
        }

        // After the app lock, so a locked app does not open files for other apps
        if (externalView != null) {
            if (!hasPermission && !permissionAnswered) {
                // The permission dialog is open
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                return@NooklinTheme
            }
            if (hasPermission && !externalLookupDone) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@NooklinTheme
            }
            if (!hasPermission || externalItem == null) {
                org.nooklin.photos.ui.external.ExternalViewScreen(externalView, onClose = { onExternalFinish(null) })
                return@NooklinTheme
            }
        }

        if (!hasPermission) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(appString(R.string.nooklin_photos_app_nooklin_photos_needs_access_to))
                Button(onClick = { launcher.launch(mediaPermissions()) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(appString(R.string.nooklin_photos_app_grant_access))
                }
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                }) {
                    Text(appString(R.string.nooklin_photos_app_open_app_settings))
                }
            }
            return@NooklinTheme
        }

        if (externalRequest is org.nooklin.photos.ExternalRequest.Pick) {
            org.nooklin.photos.ui.external.ExternalPickScreen(
                items = uiState.visibleItems.filter { if (it.isVideo) externalRequest.videos else externalRequest.images },
                allowMultiple = externalRequest.allowMultiple,
                columns = gridColumns.coerceAtLeast(3),
                onResult = onExternalFinish
            )
            return@NooklinTheme
        }

        val navController = rememberNavController()
        // A re-lock after a long absence must not leave a Hidden screen open: go back to the gate
        LaunchedEffect(hiddenUiState.isAuthenticated) {
            if (!hiddenUiState.isAuthenticated) {
                val route = navController.currentDestination?.route.orEmpty()
                if (route.startsWith("hidden") && route != ROUTE_HIDDEN) {
                    navController.popBackStack(ROUTE_HIDDEN, inclusive = false)
                }
            }
        }
        // Tapped cell bounds, hoisted so they survive the navigation.
        // Entry points without bounds clear it so the viewer doesn't replay an old rect.
        var capturedZoomLaunch by remember { mutableStateOf<ZoomLaunch?>(null) }
        // Pages through only these photos (Album, Category), not the whole library
        // A file opened from another app that the library list does not show (a hidden screenshot, for example) pages through itself only
        val externalStartIndex = remember { externalItem?.let { item -> uiState.visibleItems.indexOfFirst { it.id == item.id } } ?: -1 }
        var viewerItemsOverride by remember {
            mutableStateOf<StableList<MediaItem>?>(
                externalItem?.takeIf { externalStartIndex < 0 }?.let { StableList(listOf(it)) }
            )
        }
        var hiddenViewerItemsOverride by remember { mutableStateOf<List<HiddenItem>?>(null) }
        // For grids that don't capture their own bounds
        fun openViewerZoomed(key: Any, route: String) {
            coroutineScope.launch {
                capturedZoomLaunch = ZoomSources.capture(key)
                navController.navigate(route)
            }
        }

        if (!hiddenBackupNoticeAcknowledged) {
            BackupNoticeDialog(onAcknowledge = { settingsViewModel.acknowledgeHiddenBackupNotice() })
        }

        fun navigateToDiscoveredAlbum(tagId: String) {
            if (tagId == MlTag.PEOPLE.id) {
                navController.navigate(ROUTE_PEOPLE)
            } else {
                navController.navigate("discovered/$tagId")
            }
        }

        Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            // Opened from another app: straight into the viewer, back closes this screen and returns to that app
            startDestination = if (externalItem != null) "viewer/${externalStartIndex.coerceAtLeast(0)}" else ROUTE_HOME,
            // iOS-style 300ms push instead of the 700ms default fade.
            // Grid and viewer pairs are instant, the viewer animates itself.
            enterTransition = {
                if (isZoomPair(initialState.destination.route, targetState.destination.route)) EnterTransition.None
                else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(Motion.Medium))
            },
            exitTransition = {
                if (isZoomPair(initialState.destination.route, targetState.destination.route)) ExitTransition.None
                else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(Motion.Medium))
            },
            popEnterTransition = {
                if (isZoomPair(initialState.destination.route, targetState.destination.route)) EnterTransition.None
                else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(Motion.Medium))
            },
            popExitTransition = {
                if (isZoomPair(initialState.destination.route, targetState.destination.route)) ExitTransition.None
                else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(Motion.Medium))
            }
        ) {
            composable(
                ROUTE_HOME,
                // Instant like ROUTE_GALLERY, Home's preview row captures cell bounds
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
                        // Duplicates has its own review screen
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
                // Instant: a fade keeps both screens composed, the bounds animation already covers it
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
                    hideScreenshots = uiState.hideScreenshots,
                    // One column keeps each photo's own shape and drops the group headers
                    gridStyle = if (gridColumns == 1) GridStyle.ORIGINAL else gridStyle,
                    gridColumns = gridColumns,
                    onGridColumnsChange = settingsViewModel::setGridColumns,
                    groupBy = if (gridColumns == 1) GroupBy.NONE else groupBy,
                    existingAlbums = albumTargets,
                    onFilterChange = viewModel::setFilter,
                    onHideScreenshotsChange = viewModel::setHideScreenshots,
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
                            if (MediaStoreRepository(context).duplicate(item)) viewModel.load()
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
                // Instant like ROUTE_GALLERY, the album grid captures cell bounds
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
                    onSetCover = { mediaId -> albumViewModel.setCover(albumId, mediaId) }
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
            composable(ROUTE_DISCOVERED) {
                DiscoveredScreen(
                    albums = discoveredAlbums,
                    gridColumns = gridColumns,
                    onAlbumClick = { tagId -> navigateToDiscoveredAlbum(tagId) },
                    onDeleteCategories = { tagIds ->
                        discoveredViewModel.clearCategories(tagIds)
                        // Tag and face data come from one scan, delete both or People shows clusters without a tag
                        if (MlTag.PEOPLE.id in tagIds) {
                            peopleViewModel.clearAll()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("discovered/{tagId}") { entry ->
                val tagId = entry.arguments?.getString("tagId") ?: ""
                val album = discoveredAlbums.find { it.tagId == tagId }
                val photos = remember(analysisTags, uiState.allItems, tagId) {
                    val matchedIds = analysisTags.filter { it.tag == tagId }.map { it.mediaId }.toSet()
                    uiState.allItems.filter { it.id in matchedIds }
                }
                DiscoveredDetailScreen(
                    label = album?.label ?: MlTag.fromId(tagId)?.label ?: tagId,
                    photos = photos,
                    gridColumns = gridColumns,
                    existingAlbums = albumTargets,
                    isPetsCategory = tagId == MlTag.PETS.id,
                    existingPets = petsWithPreview,
                    onAddToPet = { petId, item -> petViewModel.addToPet(petId, item.id) },
                    onCreatePetAndAdd = { name, items ->
                        petViewModel.createPet(name) { petId ->
                            items.forEach { item -> petViewModel.addToPet(petId, item.id) }
                        }
                    },
                    onOpenPets = { navController.navigate(ROUTE_PETS) },
                    onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                    onMoveToRecycleBin = { item -> viewModel.moveToRecycleBin(item) },
                    onAddToAlbum = { albumId, item -> albumViewModel.addToAlbum(albumId, item.id) },
                    onBack = { navController.popBackStack() },
                    onItemClick = { item, discoveredPhotos ->
                        viewerItemsOverride = StableList(discoveredPhotos)
                        openViewerZoomed(item.id, "viewer/${discoveredPhotos.indexOf(item)}")
                    }
                )
            }
            composable(ROUTE_PETS) {
                PetsScreen(
                    pets = petsWithPreview,
                    gridColumns = gridColumns,
                    onPetClick = { petId -> navController.navigate("pets/$petId") },
                    onCreatePet = { name -> petViewModel.createPet(name) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("pets/{petId}") { entry ->
                val petId = entry.arguments?.getString("petId")?.toLongOrNull() ?: -1L
                val pet = petsWithPreview.find { it.id == petId }
                val memberIds = remember(petMemberships, petId) {
                    petMemberships.filter { it.petId == petId }.map { it.mediaId }.toSet()
                }
                val petPhotos = remember(uiState.allItems, memberIds) {
                    uiState.allItems.filter { it.id in memberIds }
                }
                PetDetailScreen(
                    petName = pet?.name ?: appString(R.string.nooklin_photos_app_pet),
                    photos = petPhotos,
                    gridColumns = gridColumns,
                    groupActions = groupActions,
                    onBack = { navController.popBackStack() },
                    onItemClick = { item, petPhotoList ->
                        viewerItemsOverride = StableList(petPhotoList)
                        openViewerZoomed(item.id, "viewer/${petPhotoList.indexOf(item)}")
                    },
                    onAddPhotos = { navController.navigate("pets/$petId/pick") },
                    onRename = { newName -> petViewModel.renamePet(petId, newName) },
                    onDeletePet = { petViewModel.deletePet(petId) }
                )
            }
            composable("pets/{petId}/pick") { entry ->
                val petId = entry.arguments?.getString("petId")?.toLongOrNull() ?: -1L
                val pet = petsWithPreview.find { it.id == petId }
                val memberIds = remember(petMemberships, petId) {
                    petMemberships.filter { it.petId == petId }.map { it.mediaId }.toSet()
                }
                // Pick from Pets-tagged photos only
                val petsTaggedItems = remember(analysisTags, uiState.allItems) {
                    val taggedIds = analysisTags.filter { it.tag == MlTag.PETS.id }.map { it.mediaId }.toSet()
                    uiState.allItems.filter { it.id in taggedIds }
                }
                AlbumPhotoPickerScreen(
                    albumName = pet?.name ?: appString(R.string.nooklin_photos_app_pet),
                    items = petsTaggedItems,
                    alreadyInAlbum = memberIds,
                    gridColumns = gridColumns,
                    onConfirm = { selectedIds ->
                        selectedIds.forEach { mediaId -> petViewModel.addToPet(petId, mediaId) }
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(ROUTE_PEOPLE) {
                PeopleScreen(
                    people = peopleWithPreview,
                    gridColumns = gridColumns,
                    onPersonClick = { personId -> navController.navigate("people/$personId") },
                    onAddName = { personId, name -> peopleViewModel.rename(personId, name) },
                    onPreviewCombine = { threshold -> peopleViewModel.previewCombine(threshold) },
                    onCombine = { threshold, onDone -> peopleViewModel.combineSimilar(threshold, onDone) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("people/{personId}") { entry ->
                val personId = entry.arguments?.getString("personId")?.toLongOrNull() ?: -1L

                // A rename can merge this person away, leave the page
                LaunchedEffect(people, personId) {
                    if (people.isNotEmpty() && people.none { it.id == personId }) {
                        navController.popBackStack()
                    }
                }

                val displayName = peopleByDisplayName[personId] ?: appString(R.string.nooklin_photos_app_unknown_2)
                val personFaces = remember(faces, personId) { faces.filter { it.personId == personId } }
                val facesByMediaId = remember(personFaces) {
                    // Last face wins, removing needs only one valid face id
                    personFaces.groupBy({ it.mediaId }, { it.id })
                }
                val photos = remember(personFaces, uiState.allItems) {
                    val mediaIds = personFaces.map { it.mediaId }.distinct().toSet()
                    uiState.allItems.filter { it.id in mediaIds }
                }
                val otherPeople = remember(peopleWithPreview, personId) {
                    peopleWithPreview.filter { it.id != personId }
                        .map { PersonPickerEntry(it.id, it.displayName) }
                }
                PersonDetailScreen(
                    displayName = displayName,
                    isNamed = people.find { it.id == personId }?.name != null,
                    photos = photos,
                    facesByMediaId = facesByMediaId,
                    gridColumns = gridColumns,
                    existingAlbums = albumTargets,
                    otherPeople = otherPeople,
                    onRename = { newName -> peopleViewModel.rename(personId, newName) },
                    onMergeWith = { otherId -> peopleViewModel.merge(personId, otherId) },
                    onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                    onMoveToRecycleBin = { item -> viewModel.moveToRecycleBin(item) },
                    onAddToAlbum = { albumId, item -> albumViewModel.addToAlbum(albumId, item.id) },
                    onSplitOutFaces = { faceIds -> peopleViewModel.splitOutFaces(faceIds) },
                    onBack = { navController.popBackStack() },
                    onItemClick = { item, personPhotos ->
                        viewerItemsOverride = StableList(personPhotos)
                        openViewerZoomed(item.id, "viewer/${personPhotos.indexOf(item)}")
                    }
                )
            }
            composable(ROUTE_RECYCLE_BIN) {
                // Older versions put deleted hidden items in this store too, they are plain library files and show here
                val normalRecycleBinEntries = recycleBinEntries
                RecycleBinScreen(
                    entries = normalRecycleBinEntries,
                    gridColumns = gridColumns,
                    onRestore = viewModel::restoreFromRecycleBin,
                    onPurgeExpired = viewModel::purgeExpiredFromRecycleBin,
                    onItemClick = { index -> openViewerZoomed(normalRecycleBinEntries[index].mediaId, "recyclebinviewer/$index") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("recyclebinviewer/{index}") { entry ->
                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                val normalRecycleBinEntries = recycleBinEntries
                RecycleBinViewerScreen(
                    entries = StableList(normalRecycleBinEntries),
                    zoomLaunch = capturedZoomLaunch,
                    startIndex = index,
                    onBack = { navController.popBackStack() },
                    onRestore = viewModel::restoreFromRecycleBin
                )
            }
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
                    items = hiddenUiState.visibleItems,
                    gridColumns = gridColumns,
                    onGridColumnsChange = settingsViewModel::setGridColumns,
                    existingAlbums = hiddenTargets,
                    onItemClick = { index -> openViewerZoomed(hiddenUiState.visibleItems[index].file.absolutePath, "hiddenlibraryviewer/$index") },
                    onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
                    onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
                    onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
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
                    groupBy = groupBy
                )
            }
            composable(ROUTE_HIDDEN_FAVORITES) {
                val hiddenFavorites = hiddenUiState.items.filter { it.isFavorite }
                val hiddenFavoritesCover by remember { LocalMetadataStore(context).hiddenFavoritesCover }.collectAsStateWithLifecycle(initialValue = null)
                HiddenGalleryScreen(
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
                        // Resolve against the full list, the editor route keys off it
                        val editIndex = hiddenUiState.items.indexOf(item)
                        navController.navigate("hiddeneditor/$editIndex")
                    },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }
            composable(ROUTE_HIDDEN_VIDEOS) {
                val hiddenVideos = hiddenUiState.items.filter { it.isVideo }
                HiddenGalleryScreen(
                    items = hiddenVideos,
                    gridColumns = gridColumns,
                    existingAlbums = hiddenTargets,
                    title = appString(R.string.common_videos),
                    onItemClick = { index -> openViewerZoomed(hiddenVideos[index].file.absolutePath, "hiddenvideosviewer/$index") },
                    onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
                    onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
                    onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
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
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }

            // Slow Motion / RAW / Screenshots: flags captured at hide-time
            composable(ROUTE_HIDDEN_SLOW_MOTION) {
                val hiddenSlowMotion = hiddenUiState.items.filter { it.isSlowMotion }
                HiddenGalleryScreen(
                    items = hiddenSlowMotion,
                    gridColumns = gridColumns,
                    existingAlbums = hiddenTargets,
                    title = appString(R.string.common_slow_motion),
                    onItemClick = { index -> openViewerZoomed(hiddenSlowMotion[index].file.absolutePath, "hiddenslowmotionviewer/$index") },
                    onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
                    onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
                    onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
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
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }

            composable(ROUTE_HIDDEN_RAW) {
                val hiddenRaw = hiddenUiState.items.filter { it.isRaw }
                HiddenGalleryScreen(
                    items = hiddenRaw,
                    gridColumns = gridColumns,
                    existingAlbums = hiddenTargets,
                    title = appString(R.string.common_raw),
                    onItemClick = { index -> openViewerZoomed(hiddenRaw[index].file.absolutePath, "hiddenrawviewer/$index") },
                    onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
                    onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
                    onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
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
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }

            composable(ROUTE_HIDDEN_SCREENSHOTS) {
                val hiddenScreenshots = hiddenUiState.items.filter { it.isScreenshot }
                HiddenGalleryScreen(
                    items = hiddenScreenshots,
                    gridColumns = gridColumns,
                    existingAlbums = hiddenTargets,
                    title = appString(R.string.common_screenshots),
                    onItemClick = { index -> openViewerZoomed(hiddenScreenshots[index].file.absolutePath, "hiddenscreenshotsviewer/$index") },
                    onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
                    onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
                    onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
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
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }

            composable(ROUTE_HIDDEN_PANORAMAS) {
                val hiddenPanoramas = hiddenUiState.items.filter { it.isPanorama }
                HiddenGalleryScreen(
                    items = hiddenPanoramas,
                    gridColumns = gridColumns,
                    existingAlbums = hiddenTargets,
                    title = appString(R.string.common_panoramas),
                    onItemClick = { index -> openViewerZoomed(hiddenPanoramas[index].file.absolutePath, "hiddenpanoramasviewer/$index") },
                    onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
                    onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
                    onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
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
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }

            composable(ROUTE_HIDDEN_SCREEN_RECORDINGS) {
                val hiddenScreenRecordings = hiddenUiState.items.filter { it.isScreenRecording }
                HiddenGalleryScreen(
                    items = hiddenScreenRecordings,
                    gridColumns = gridColumns,
                    existingAlbums = hiddenTargets,
                    title = appString(R.string.common_screen_recordings),
                    onItemClick = { index -> openViewerZoomed(hiddenScreenRecordings[index].file.absolutePath, "hiddenscreenrecordingsviewer/$index") },
                    onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
                    onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
                    onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
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
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }

            composable(ROUTE_HIDDEN_RECENTLY_SAVED) {
                // 30 days, same window as MediaCategory.RECENTLY_SAVED but on hiddenAt
                val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                val hiddenRecentlySaved = hiddenUiState.items
                    .filter { it.hiddenAt >= cutoff }
                    .sortedByDescending { it.hiddenAt }
                HiddenGalleryScreen(
                    items = hiddenRecentlySaved,
                    gridColumns = gridColumns,
                    existingAlbums = hiddenTargets,
                    title = appString(R.string.common_recently_saved),
                    onItemClick = { index -> openViewerZoomed(hiddenRecentlySaved[index].file.absolutePath, "hiddenrecentlysavedviewer/$index") },
                    onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
                    onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
                    onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
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
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }
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
                HiddenAlbumsScreen(
                    albums = hiddenTopAlbums,
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
                HiddenAlbumDetailScreen(
                    albumName = album?.name ?: appString(R.string.nooklin_photos_app_album),
                    items = albumItems,
                    totalCount = album?.itemCount ?: albumItems.size,
                    gridColumns = gridColumns,
                    existingAlbums = hiddenTargets.filter { it.id != albumId },
                    onItemClick = { index ->
                        // The viewer pages through only this album's photos
                        hiddenViewerItemsOverride = albumItems
                        openViewerZoomed(albumItems[index].file.absolutePath, "hiddenviewer/$index")
                    },
                    onToggleFavorite = { item -> hiddenViewModel.toggleFavorite(item) },
                    onUnhide = { item -> hiddenViewModel.unhide(item.file.name) },
                    onMoveToRecycleBin = { item -> hiddenViewModel.trash(item.file.name) },
                    onAddToAlbum = { targetAlbumId, item -> hiddenViewModel.addToAlbum(targetAlbumId, item.file.name) },
                    onRemoveFromAlbum = { item -> hiddenViewModel.removeFromAlbum(albumId, item.file.name) },
                    onRenameAlbum = { newName -> hiddenViewModel.renameAlbum(albumId, newName) },
                    // Leaves the screen itself. A folder goes back to its album, an album to the albums overview,
                    // or to Hidden Home when it was the last album.
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
                    folders = hiddenUiState.albums.filter { it.parentId == albumId },
                    onFolderClick = { folderId -> navController.navigate("hiddenalbums/$folderId") },
                    onRenameFolder = { id, name -> hiddenViewModel.renameAlbum(id, name) },
                    onDeleteFolder = { id -> hiddenViewModel.deleteAlbum(id) },
                    onCreateFolder = { name -> hiddenViewModel.createFolder(albumId, name) },
                    coverFileName = album?.coverFileName,
                    onSetCover = { fileName -> hiddenViewModel.setAlbumCover(albumId, fileName) }
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
                    // Re-lock first, the backup exports real hidden photos
                    hiddenViewModel.lock()
                    navController.navigate("settings/hiddenbackup")
                },
                onOpenHiddenImport = {
                    // Re-lock first, the import writes into the Hidden Section
                    hiddenViewModel.lock()
                    navController.navigate("settings/hiddenimport")
                }
            )
            composable("settings/hiddenbackup") {
                // Same three step gate as ROUTE_HIDDEN, the backup exports hidden content
                when {
                    !hiddenUiState.isAuthenticated -> HiddenFolderGate(
                        onAuthenticated = { hiddenViewModel.onAuthenticated() },
                        onCancelled = { navController.popBackStack() }
                    )
                    hiddenPinSet == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    hiddenPinSet == true && !hiddenUiState.pinVerified -> HiddenPinGate(
                        verifyPin = settingsViewModel::verifyHiddenPin,
                        verifyRecoveryCode = settingsViewModel::verifyHiddenPinRecoveryCode,
                        setNewPin = settingsViewModel::setHiddenPin,
                        onUnlocked = { hiddenViewModel.onPinVerified() },
                        onCancel = { navController.popBackStack() }
                    )
                    else -> {
                        val exportProgress by hiddenViewModel.exportProgress.collectAsStateWithLifecycle()
                        SettingsHiddenBackupScreen(
                            progress = exportProgress,
                            onExportFlat = { uri, onResult -> hiddenViewModel.exportFlatZip(uri, onResult) },
                            onExportOrganized = { uri, onResult -> hiddenViewModel.exportOrganizedZip(uri, onResult) },
                            onNavigateToSelectExport = { navController.navigate("settings/hiddenbackup/select") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
            composable("settings/backupimport") {
                SettingsBackupImportScreen(
                    onNavigate = { navController.navigate("settings/$it") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings/librarybackup") {
                val exportProgress by libraryTransferViewModel.exportProgress.collectAsStateWithLifecycle()
                SettingsLibraryBackupScreen(
                    progress = exportProgress,
                    onExport = { uri, organized, onResult -> libraryTransferViewModel.exportZip(uri, organized, onResult) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings/libraryimport") {
                val importProgress by libraryTransferViewModel.importProgress.collectAsStateWithLifecycle()
                SettingsHiddenImportScreen(
                    progress = importProgress,
                    onScan = { source, onResult -> libraryTransferViewModel.scanImport(source, onResult) },
                    onImport = { source, entries, merge, deleteAfter, onResult ->
                        libraryTransferViewModel.startImport(source, entries, merge, deleteAfter, onResult)
                    },
                    onCancel = { libraryTransferViewModel.cancelImport() },
                    onBack = { navController.popBackStack() },
                    hidden = false
                )
            }
            composable("settings/hiddenimport") {
                // Same three step gate as the backup
                when {
                    !hiddenUiState.isAuthenticated -> HiddenFolderGate(
                        onAuthenticated = { hiddenViewModel.onAuthenticated() },
                        onCancelled = { navController.popBackStack() }
                    )
                    hiddenPinSet == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    hiddenPinSet == true && !hiddenUiState.pinVerified -> HiddenPinGate(
                        verifyPin = settingsViewModel::verifyHiddenPin,
                        verifyRecoveryCode = settingsViewModel::verifyHiddenPinRecoveryCode,
                        setNewPin = settingsViewModel::setHiddenPin,
                        onUnlocked = { hiddenViewModel.onPinVerified() },
                        onCancel = { navController.popBackStack() }
                    )
                    else -> {
                        val importProgress by hiddenViewModel.importProgress.collectAsStateWithLifecycle()
                        SettingsHiddenImportScreen(
                            progress = importProgress,
                            onScan = { source, onResult -> hiddenViewModel.scanImport(source, onResult) },
                            onImport = { source, entries, merge, deleteAfter, onResult ->
                                hiddenViewModel.startImport(source, entries, merge, deleteAfter, onResult)
                            },
                            onCancel = { hiddenViewModel.cancelImport() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
            composable("settings/hiddenbackup/select") {
                // No gate: only reachable from the gated backup screen
                SettingsHiddenBackupSelectScreen(
                    items = hiddenUiState.items,
                    gridColumns = gridColumns,
                    onExportSelected = { fileNames, uri, onResult ->
                        hiddenViewModel.exportSelectedZip(fileNames, uri, onResult)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(ROUTE_DUPLICATES) {
                var groupIds by remember { mutableStateOf(emptyList<List<Long>>()) }
                LaunchedEffect(uiState.allItems) {
                    val validIds = uiState.allItems.map { it.id }.toSet()
                    settingsViewModel.findDuplicateGroups(validIds) { groupIds = it }
                }
                // Items already moved to Recently Deleted drop out at once, groups of one are no group
                val groups = remember(groupIds, uiState.allItems) {
                    val byId = uiState.allItems.associateBy { it.id }
                    groupIds.map { ids -> ids.mapNotNull { byId[it] } }.filter { it.size >= 2 }
                }
                DuplicatesScreen(
                    groups = groups,
                    onBack = { navController.popBackStack() },
                    onOpenGroup = { group, startIndex ->
                        viewerItemsOverride = StableList(group)
                        openViewerZoomed(group[startIndex].id, "viewer/$startIndex")
                    },
                    onMoveToRecycleBin = { item -> viewModel.moveToRecycleBin(item) }
                )
            }
            composable(ROUTE_CALENDAR) {
                CalendarScreen(
                    items = uiState.visibleItems,
                    onBack = { navController.popBackStack() },
                    onOpenDay = { dayItems ->
                        viewerItemsOverride = StableList(dayItems)
                        openViewerZoomed(dayItems.first().id, "viewer/0")
                    }
                )
            }
            composable(ROUTE_SEARCH) {
                SearchScreen(
                    items = uiState.allItems,
                    gridColumns = gridColumns,
                    groupActions = groupActions,
                    onBack = { navController.popBackStack() },
                    onItemClick = { item, searchResults ->
                        viewerItemsOverride = StableList(searchResults)
                        openViewerZoomed(item.id, "viewer/${searchResults.indexOf(item)}")
                    }
                )
            }
            composable("category/{name}") { entry ->
                val name = entry.arguments?.getString("name")
                val category = MediaCategory.entries.find { it.name == name }
                if (category != null) {
                    var duplicateMediaIds by remember { mutableStateOf(emptySet<Long>()) }
                    val favoritesCoverId by remember { LocalMetadataStore(context).favoritesCover }.collectAsStateWithLifecycle(initialValue = null)
                    LaunchedEffect(category, uiState.allItems) {
                        if (category == MediaCategory.DUPLICATES) {
                            val validIds = uiState.allItems.map { it.id }.toSet()
                            settingsViewModel.findDuplicateMediaIds(validIds) { duplicateMediaIds = it }
                        }
                    }
                    CategoryScreen(
                        category = category,
                        allItems = uiState.allItems,
                        groupActions = groupActions,
                        gridStyleSetting = gridStyle,
                        gridColumns = gridColumns,
                        duplicateMediaIds = duplicateMediaIds,
                        heroCoverId = favoritesCoverId,
                        onSetHeroCover = { id -> coroutineScope.launch { LocalMetadataStore(context).setFavoritesCover(id) } },
                        onBack = { navController.popBackStack() },
                        onItemClick = { item, categoryItems ->
                            viewerItemsOverride = StableList(categoryItems)
                            openViewerZoomed(item.id, "viewer/${categoryItems.indexOf(item)}")
                        }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
            composable(
                "viewer/{index}",
                // Same instant transition as ROUTE_GALLERY
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
                val viewerItems = viewerItemsOverride ?: libraryItems
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
                    items = StableList(hiddenViewerItemsOverride ?: hiddenUiState.items),
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
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }
            // Own viewer for the Hidden Library: its click index is a position in visibleItems
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
                        // Resolve against the full list, the editor route keys off it
                        val editIndex = hiddenUiState.items.indexOf(item)
                        navController.navigate("hiddeneditor/$editIndex")
                    },
                    onAddToAlbum = { albumId, item -> hiddenViewModel.addToAlbum(albumId, item.file.name) },
                    onCreateAlbum = { name, onCreated -> hiddenViewModel.createAlbum(name, onCreated) }
                )
            }
            composable("editor/{mediaId}") { entry ->
                val mediaId = entry.arguments?.getString("mediaId")?.toLongOrNull()
                // The last found item is kept: a deleted edit leaves the list while this screen is still animating out,
                // and a missing item would make the else branch below pop one screen too many.
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
                            // Same exit as deleting from the viewer: the item is dropped at once and both the editor and the
                            // viewer close, so the viewer never shows or animates to a photo that no longer exists.
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
            // Return zoom for photos that weren't on screen in the grid at tap time.
            ZoomReturnOverlay()
        }
    }
}

// Grid <-> viewer route pairs that swap instantly (the viewer's own zoom is the animation).
private val ZOOM_VIEWER_ROUTES = setOf(
    "viewer/{index}", "hiddenviewer/{index}", "hiddenlibraryviewer/{index}",
    "hiddenfavoritesviewer/{index}", "hiddenvideosviewer/{index}", "hiddenslowmotionviewer/{index}",
    "hiddenrawviewer/{index}", "hiddenscreenshotsviewer/{index}", "hiddenrecentlysavedviewer/{index}",
    "hiddenpanoramasviewer/{index}", "hiddenscreenrecordingsviewer/{index}",
    "recyclebinviewer/{index}", "hiddenrecyclebinviewer/{index}"
)
private val ZOOM_GRID_ROUTES = setOf(
    ROUTE_HOME, ROUTE_GALLERY, "album/{id}", "category/{name}", ROUTE_RECYCLE_BIN,
    ROUTE_HIDDEN, ROUTE_HIDDEN_LIBRARY, ROUTE_HIDDEN_FAVORITES, ROUTE_HIDDEN_VIDEOS,
    ROUTE_HIDDEN_SLOW_MOTION, ROUTE_HIDDEN_RAW, ROUTE_HIDDEN_SCREENSHOTS,
    ROUTE_HIDDEN_PANORAMAS, ROUTE_HIDDEN_SCREEN_RECORDINGS,
    ROUTE_HIDDEN_RECENTLY_SAVED, ROUTE_HIDDEN_RECYCLE_BIN, "hiddenalbums/{albumId}",
    "discovered/{tagId}", "pets/{petId}", "people/{personId}", ROUTE_SEARCH, ROUTE_CALENDAR
)

private fun isZoomPair(a: String?, b: String?): Boolean =
    (a in ZOOM_VIEWER_ROUTES && b in ZOOM_GRID_ROUTES) || (b in ZOOM_VIEWER_ROUTES && a in ZOOM_GRID_ROUTES)
