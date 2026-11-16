package org.nooklin.photos.ui

import androidx.compose.ui.graphics.toArgb
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem

import org.nooklin.photos.ui.common.GroupActions
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.ZoomReturnOverlay

import org.nooklin.photos.ui.common.ZoomSources

import org.nooklin.photos.ui.common.ZoomLaunch

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.nooklin.photos.data.local.LocalMetadataStore
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.navigation.compose.rememberNavController
import org.nooklin.photos.appString
import kotlinx.coroutines.launch
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.createExifStrippedShareUri
import org.nooklin.photos.data.MediaStoreRepository
import org.nooklin.photos.ui.settings.LocalColorful
import org.nooklin.photos.ui.settings.LibraryTransferViewModel
import org.nooklin.photos.toMediaStoreId
import org.nooklin.photos.data.local.AppThemeMode
import org.nooklin.photos.ui.albums.AlbumViewModel
import org.nooklin.photos.ui.common.BackupNoticeDialog
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.discovered.DiscoveredAlbum
import org.nooklin.photos.ui.discovered.DiscoveredViewModel
import org.nooklin.photos.data.local.MlTag
import org.nooklin.photos.ui.hidden.HiddenFolderGate
import org.nooklin.photos.ui.hidden.HiddenViewModel
import org.nooklin.photos.ui.people.PeopleViewModel
import org.nooklin.photos.ui.people.PersonWithPreview
import org.nooklin.photos.ui.pets.PetViewModel
import org.nooklin.photos.ui.pets.PetWithPreview
import org.nooklin.photos.ui.settings.SettingsViewModel
import org.nooklin.photos.ui.theme.DesignStyle
import org.nooklin.photos.ui.theme.NooklinTheme
import org.nooklin.photos.ui.viewer.ExternalPickScreen
import org.nooklin.photos.ui.viewer.ExternalViewScreen
import org.nooklin.photos.R
import org.nooklin.photos.ui.theme.Motion

// READ_MEDIA_VIDEO must be requested too, else MediaStore silently hides every video
private fun mediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }


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
    val colorful by settingsViewModel.colorful.collectAsStateWithLifecycle()
    val appFont by settingsViewModel.appFont.collectAsStateWithLifecycle()
    val gridStyleState = settingsViewModel.gridStyle.collectAsStateWithLifecycle()
    val gridStyle by gridStyleState
    val gridColumnsState = settingsViewModel.gridColumns.collectAsStateWithLifecycle()
    val gridColumns by gridColumnsState
    val libraryPreviewRowsState = settingsViewModel.libraryPreviewRows.collectAsStateWithLifecycle()
    val libraryPreviewRows by libraryPreviewRowsState
    val groupByState = settingsViewModel.groupBy.collectAsStateWithLifecycle()
    val groupBy by groupByState
    val mlEnabled by settingsViewModel.mlEnabled.collectAsStateWithLifecycle()
    val mlEnabledTagsState = settingsViewModel.mlEnabledTags.collectAsStateWithLifecycle()
    val mlEnabledTags by mlEnabledTagsState
    val scanProgress by settingsViewModel.scanProgress.collectAsStateWithLifecycle()
    val appLockEnabled by settingsViewModel.appLockEnabled.collectAsStateWithLifecycle()
    val hiddenPinSetState = settingsViewModel.hiddenPinSet.collectAsStateWithLifecycle()
    val hiddenPinSet by hiddenPinSetState
    val disableScreenshots by settingsViewModel.disableScreenshots.collectAsStateWithLifecycle()
    val removeExifOnShareState = settingsViewModel.removeExifOnShare.collectAsStateWithLifecycle()
    val removeExifOnShare by removeExifOnShareState
    val sharingDisabledState = settingsViewModel.sharingDisabled.collectAsStateWithLifecycle()
    val sharingDisabled by sharingDisabledState
    val hiddenSectionEnabledState = settingsViewModel.hiddenSectionEnabled.collectAsStateWithLifecycle()
    val hiddenSectionEnabled by hiddenSectionEnabledState
    val hiddenAlbumsEnabledState = settingsViewModel.hiddenAlbumsEnabled.collectAsStateWithLifecycle()
    val hiddenAlbumsEnabled by hiddenAlbumsEnabledState
    val hiddenImageSharingEnabledState = settingsViewModel.hiddenImageSharingEnabled.collectAsStateWithLifecycle()
    val hiddenImageSharingEnabled by hiddenImageSharingEnabledState
    val hiddenBackupNoticeAcknowledged by settingsViewModel.hiddenBackupNoticeAcknowledged.collectAsStateWithLifecycle()
    val scanCompleteNotificationEnabled by settingsViewModel.scanCompleteNotificationEnabled.collectAsStateWithLifecycle()
    val homeSectionOrderState = settingsViewModel.homeSectionOrder.collectAsStateWithLifecycle()
    val homeSectionOrder by homeSectionOrderState
    val homeMediaTypesOrderState = settingsViewModel.homeMediaTypesOrder.collectAsStateWithLifecycle()
    val homeMediaTypesOrder by homeMediaTypesOrderState
    val homeUtilitiesOrderState = settingsViewModel.homeUtilitiesOrder.collectAsStateWithLifecycle()
    val homeUtilitiesOrder by homeUtilitiesOrderState
    val visibleUtilitiesOrder = if (hiddenSectionEnabled) homeUtilitiesOrder else homeUtilitiesOrder - "HIDDEN"
    val homeHiddenSectionsState = settingsViewModel.homeHiddenSections.collectAsStateWithLifecycle()
    val homeHiddenSections by homeHiddenSectionsState
    val hiddenHomeHiddenSectionsState = settingsViewModel.hiddenHomeHiddenSections.collectAsStateWithLifecycle()
    val hiddenHomeHiddenSections by hiddenHomeHiddenSectionsState
    val systemIsDark = isSystemInDarkTheme()
    val resolvedDarkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.BLACK -> true
        AppThemeMode.SYSTEM -> systemIsDark
    }
    val isBlackMode = themeMode == AppThemeMode.BLACK

    val view = LocalView.current
    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
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

    val uiStateHolder = viewModel.uiState.collectAsStateWithLifecycle()
    val uiState by uiStateHolder

    // a file opened from another app (for example the camera): find it in the library once that is loaded, so it opens in the normal viewer
    val externalView = externalRequest as? org.nooklin.photos.ExternalRequest.View
    val externalItemState = remember(externalView?.uri) { mutableStateOf<MediaItem?>(null) }
    var externalItem by externalItemState
    var externalLookupDone by remember(externalView?.uri) { mutableStateOf(false) }
    LaunchedEffect(externalView?.uri, hasPermission, uiState.isLoading) {
        if (externalView == null || !hasPermission || uiState.isLoading) return@LaunchedEffect
        val id = externalView.uri.toMediaStoreId()
        externalItem = if (id == null) null else uiState.allItems.find { it.id == id } ?: viewModel.findMediaItemById(id)
        externalLookupDone = true
    }
    val recycleBinEntriesState = viewModel.recycleBinEntries.collectAsStateWithLifecycle()
    val recycleBinEntries by recycleBinEntriesState
    val hiddenUiStateHolder = hiddenViewModel.uiState.collectAsStateWithLifecycle()
    val hiddenUiState by hiddenUiStateHolder
    val hiddenTopAlbums = remember(hiddenUiState.albums) { hiddenUiState.albums.filter { it.parentId == null } }
    val hiddenTargets = remember(hiddenUiState.albums) {
        fun withFolders(album: HiddenAlbumWithPreview): List<HiddenAlbumWithPreview> =
            listOf(album) + hiddenUiState.albums.filter { it.parentId == album.id }.flatMap { folder ->
                withFolders(folder.copy(name = album.name + " / " + folder.name))
            }
        hiddenTopAlbums.flatMap { withFolders(it) }
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

    val albumEntitiesState = albumViewModel.albums.collectAsStateWithLifecycle()
    val albumEntities by albumEntitiesState
    val albumMembershipsState = albumViewModel.memberships.collectAsStateWithLifecycle()
    val albumMemberships by albumMembershipsState
    val petEntities by petViewModel.pets.collectAsStateWithLifecycle()
    val petMembershipsState = petViewModel.memberships.collectAsStateWithLifecycle()
    val petMemberships by petMembershipsState

    val allCollections by produceState(emptyList<AlbumWithPreview>(), albumEntities, albumMemberships, uiState.allItems) {
        val allItems = uiState.allItems
        value = withContext(Dispatchers.Default) {
            // one pass over the library: bucket items per album, newest first like allItems
            val albumsByMedia = HashMap<Long, MutableList<Long>>()
            albumMemberships.forEach { albumsByMedia.getOrPut(it.mediaId) { mutableListOf() }.add(it.albumId) }
            val itemsByAlbum = HashMap<Long, MutableList<org.nooklin.photos.data.MediaItem>>()
            for (item in allItems) {
                albumsByMedia[item.id]?.forEach { id -> itemsByAlbum.getOrPut(id) { mutableListOf() }.add(item) }
            }
            val foldersByParent = albumEntities.filter { it.parentId != null }.groupBy { it.parentId }
            fun membersOf(id: Long): List<org.nooklin.photos.data.MediaItem> = itemsByAlbum[id].orEmpty()
            fun treeMembers(id: Long): List<org.nooklin.photos.data.MediaItem> =
                (membersOf(id) + foldersByParent[id].orEmpty().flatMap { treeMembers(it.id) }).distinctBy { it.id }
            fun frontOf(album: org.nooklin.photos.data.local.AlbumEntity, memberItems: List<org.nooklin.photos.data.MediaItem>) =
                memberItems.find { it.id == album.coverMediaId } ?: memberItems.firstOrNull()
            albumEntities.map { album ->
                val memberItems = membersOf(album.id)
                val cover = memberItems.find { it.id == album.coverMediaId }
                val folders = foldersByParent[album.id].orEmpty()
                val folderFronts = folders.mapNotNull { folder -> frontOf(folder, treeMembers(folder.id)) }.take(4)
                val itemCount = if (folders.isEmpty()) memberItems.size else treeMembers(album.id).size
                AlbumWithPreview(
                    id = album.id,
                    name = album.name,
                    itemCount = itemCount,
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
    }
    val hiddenAutoAlbumIds by albumViewModel.hiddenAutoAlbumIds.collectAsStateWithLifecycle()
    val albumsWithPreview = remember(allCollections, hiddenAutoAlbumIds) {
        allCollections.filter { it.parentId == null && it.id !in hiddenAutoAlbumIds }
    }
    val albumTargets = remember(allCollections, hiddenAutoAlbumIds) {
        val topLevel = allCollections.filter { it.parentId == null && it.id !in hiddenAutoAlbumIds }
        fun withFolders(album: AlbumWithPreview): List<AlbumWithPreview> =
            listOf(album) + allCollections.filter { it.parentId == album.id }.flatMap { folder ->
                withFolders(folder.copy(name = album.name + " / " + folder.name))
            }
        topLevel.flatMap { withFolders(it) }
    }

    val petsWithPreview by produceState(emptyList<PetWithPreview>(), petEntities, petMemberships, uiState.allItems) {
        val allItems = uiState.allItems
        value = withContext(Dispatchers.Default) {
            val petsByMedia = HashMap<Long, MutableList<Long>>()
            petMemberships.forEach { petsByMedia.getOrPut(it.mediaId) { mutableListOf() }.add(it.petId) }
            val itemsByPet = HashMap<Long, MutableList<org.nooklin.photos.data.MediaItem>>()
            for (item in allItems) {
                petsByMedia[item.id]?.forEach { id -> itemsByPet.getOrPut(id) { mutableListOf() }.add(item) }
            }
            petEntities.map { pet ->
                val memberItems = itemsByPet[pet.id].orEmpty()
                PetWithPreview(id = pet.id, name = pet.name, itemCount = memberItems.size, previewItems = memberItems.take(4))
            }
        }
    }

    // Orphans only appear when items disappear, so pruning waits for the first load or a smaller library
    val lastPrunedTotal = remember { intArrayOf(-1) }
    LaunchedEffect(uiState.allItems, recycleBinEntries) {
        albumViewModel.syncAutoAlbums(uiState.allItems)
        if (hasPermission && uiState.allItems.isNotEmpty()) {
            val total = uiState.allItems.size + recycleBinEntries.size
            if (lastPrunedTotal[0] < 0 || total < lastPrunedTotal[0]) {
                // Recycled items are not in allItems but must keep their memberships for Restore
                val allItems = uiState.allItems
                val validIds = withContext(Dispatchers.Default) { allItems.map { it.id }.toSet() + recycleBinEntries.map { it.mediaId } }
                albumViewModel.pruneOrphanedMemberships(validIds)
                petViewModel.pruneOrphanedMemberships(validIds)
                settingsViewModel.pruneOrphanedAnalysisData(validIds)
            }
            lastPrunedTotal[0] = total
        }
    }

    val itemsById by produceState(emptyMap<Long, MediaItem>(), uiState.allItems) {
        val allItems = uiState.allItems
        value = withContext(Dispatchers.Default) { allItems.associateBy { it.id } }
    }
    val analysisTagsState = discoveredViewModel.tags.collectAsStateWithLifecycle()
    val analysisTags by analysisTagsState

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

    val peopleState = peopleViewModel.people.collectAsStateWithLifecycle()
    val people by peopleState
    val facesState = peopleViewModel.faces.collectAsStateWithLifecycle()
    val faces by facesState

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

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        hiddenViewModel.finishHide(result.resultCode == Activity.RESULT_OK)
        viewModel.load()
        hiddenViewModel.refresh()
    }

    fun startHide(items: List<MediaItem>) {
        coroutineScope.launch {
            // Null: the private copy failed, never delete the original without a backup
            val pendingIntent = hiddenViewModel.beginHideMultiple(items) ?: return@launch
            deleteRequestLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        }
    }

    var hideAfterAccess by remember { mutableStateOf<List<MediaItem>?>(null) }
    var manageMediaAsked by remember { mutableStateOf(false) }
    val manageMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val items = hideAfterAccess
        hideAfterAccess = null
        if (items != null) startHide(items)
    }

    fun hideItems(items: List<MediaItem>) {
        if (!MediaStore.canManageMedia(context) && !manageMediaAsked) {
            manageMediaAsked = true
            hideAfterAccess = items
        } else {
            startHide(items)
        }
    }

    fun hideItem(item: MediaItem) = hideItems(listOf(item))

    hideAfterAccess?.let { items ->
        AlertDialog(
            onDismissRequest = { hideAfterAccess = null; startHide(items) },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.hide_access_title)) },
            text = { Text(appString(R.string.hide_access_text)) },
            confirmButton = {
                TextButton(onClick = {
                    manageMediaLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA, Uri.parse("package:${context.packageName}"))
                    )
                }) { Text(appString(R.string.hide_access_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { hideAfterAccess = null; startHide(items) }) { Text(appString(R.string.hide_access_not_now)) }
            }
        )
    }

    val groupActions = GroupActions(
        albums = albumTargets,
        removeExifOnShare = removeExifOnShare,
        onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
        onMoveToRecycleBin = { item -> viewModel.moveToRecycleBin(item) },
        onAddToAlbum = { albumId, item -> albumViewModel.addToAlbum(albumId, item.id) },
        shareEnabled = !sharingDisabled,
        onCopy = { item ->
            coroutineScope.launch {
                val copyUri = withContext(Dispatchers.IO) { createExifStrippedShareUri(context, item, removeExifOnShare) }
                if (copyUri != null) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, appString(R.string.photo_viewer_photo), copyUri))
                }
            }
        },
        onDuplicate = { item ->
            coroutineScope.launch {
                if (MediaStoreRepository(context).duplicate(item) != null) viewModel.load()
            }
        },
        onHide = if (hiddenSectionEnabled) ({ item -> hideItem(item) }) else null
    )

    // Hidden Recently Deleted comes from the Hidden database, the files never leave the app.
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
      val windowBackground = MaterialTheme.colorScheme.background
      val hostView = LocalView.current
      SideEffect {
          (hostView.context as? Activity)?.window?.setBackgroundDrawable(
              android.graphics.drawable.ColorDrawable(windowBackground.toArgb())
          )
      }
      CompositionLocalProvider(LocalColorful provides colorful) {
        // Null until DataStore has loaded: show nothing so no lock screen flashes
        if (appLockEnabled == null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            return@CompositionLocalProvider
        }
        if (appLockEnabled == true && !appUnlocked) {
            HiddenFolderGate(
                title = appString(R.string.nooklin_photos_app_unlock_nooklin_photos),
                onAuthenticated = { settingsViewModel.unlockApp() },
                onCancelled = { /* stays locked, retry happens on next recomposition */ }
            )
            return@CompositionLocalProvider
        }

        // After the app lock, so a locked app does not open files for other apps
        if (externalView != null) {
            if (!hasPermission && !permissionAnswered) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                return@CompositionLocalProvider
            }
            if (hasPermission && !externalLookupDone) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@CompositionLocalProvider
            }
            if (!hasPermission || externalItem == null) {
                ExternalViewScreen(externalView, onClose = { onExternalFinish(null) })
                return@CompositionLocalProvider
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
            return@CompositionLocalProvider
        }

        if (externalRequest is org.nooklin.photos.ExternalRequest.Pick) {
            ExternalPickScreen(
                items = uiState.visibleItems.filter { if (it.isVideo) externalRequest.videos else externalRequest.images },
                allowMultiple = externalRequest.allowMultiple,
                columns = gridColumns.coerceAtLeast(3),
                onResult = onExternalFinish
            )
            return@CompositionLocalProvider
        }

        val navController = rememberNavController()
        // a re-lock after a long absence must not leave a Hidden screen open: go back to the gate
        LaunchedEffect(hiddenUiState.isAuthenticated) {
            if (!hiddenUiState.isAuthenticated) {
                val route = navController.currentDestination?.route.orEmpty()
                if (route.startsWith("hidden") && route != ROUTE_HIDDEN) {
                    navController.popBackStack(ROUTE_HIDDEN, inclusive = false)
                }
            }
        }
        // Tapped cell bounds, hoisted so they survive the navigation
        val capturedZoomLaunchState = remember { mutableStateOf<ZoomLaunch?>(null) }
        var capturedZoomLaunch by capturedZoomLaunchState
        val externalStartIndex = remember { externalItem?.let { item -> uiState.visibleItems.indexOfFirst { it.id == item.id } } ?: -1 }
        val viewerItemsOverrideState = remember {
            mutableStateOf<StableList<MediaItem>?>(
                externalItem?.takeIf { externalStartIndex < 0 }?.let { StableList(listOf(it)) }
            )
        }
        var viewerItemsOverride by viewerItemsOverrideState
        val hiddenViewerItemsOverrideState = remember { mutableStateOf<List<HiddenItem>?>(null) }
        var hiddenViewerItemsOverride by hiddenViewerItemsOverrideState
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
            startDestination = if (externalItem != null) "viewer/${externalStartIndex.coerceAtLeast(0)}" else ROUTE_HOME,
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
            mainRoutes(
                context = context,
                viewModel = viewModel,
                hiddenViewModel = hiddenViewModel,
                settingsViewModel = settingsViewModel,
                albumViewModel = albumViewModel,
                gridStyleState = gridStyleState,
                gridColumnsState = gridColumnsState,
                libraryPreviewRowsState = libraryPreviewRowsState,
                groupByState = groupByState,
                mlEnabledTagsState = mlEnabledTagsState,
                removeExifOnShareState = removeExifOnShareState,
                sharingDisabledState = sharingDisabledState,
                hiddenSectionEnabledState = hiddenSectionEnabledState,
                homeSectionOrderState = homeSectionOrderState,
                homeMediaTypesOrderState = homeMediaTypesOrderState,
                homeUtilitiesOrderState = homeUtilitiesOrderState,
                visibleUtilitiesOrder = visibleUtilitiesOrder,
                homeHiddenSectionsState = homeHiddenSectionsState,
                hiddenHomeHiddenSectionsState = hiddenHomeHiddenSectionsState,
                uiStateHolder = uiStateHolder,
                coroutineScope = coroutineScope,
                albumEntitiesState = albumEntitiesState,
                albumMembershipsState = albumMembershipsState,
                allCollections = allCollections,
                albumsWithPreview = albumsWithPreview,
                albumTargets = albumTargets,
                groupActions = groupActions,
                discoveredAlbums = discoveredAlbums,
                hideItems = ::hideItems,
                navController = navController,
                capturedZoomLaunchState = capturedZoomLaunchState,
                viewerItemsOverrideState = viewerItemsOverrideState,
                navigateToDiscoveredAlbum = ::navigateToDiscoveredAlbum
            )
            collectionRoutes(
                viewModel = viewModel,
                albumViewModel = albumViewModel,
                discoveredViewModel = discoveredViewModel,
                petViewModel = petViewModel,
                peopleViewModel = peopleViewModel,
                gridColumnsState = gridColumnsState,
                uiStateHolder = uiStateHolder,
                recycleBinEntriesState = recycleBinEntriesState,
                petMembershipsState = petMembershipsState,
                albumTargets = albumTargets,
                groupActions = groupActions,
                petsWithPreview = petsWithPreview,
                analysisTagsState = analysisTagsState,
                discoveredAlbums = discoveredAlbums,
                peopleState = peopleState,
                facesState = facesState,
                peopleWithPreview = peopleWithPreview,
                peopleByDisplayName = peopleByDisplayName,
                navController = navController,
                capturedZoomLaunchState = capturedZoomLaunchState,
                viewerItemsOverrideState = viewerItemsOverrideState,
                openViewerZoomed = ::openViewerZoomed,
                navigateToDiscoveredAlbum = ::navigateToDiscoveredAlbum
            )
            hiddenBrowseRoutes(
                context = context,
                viewModel = viewModel,
                hiddenViewModel = hiddenViewModel,
                settingsViewModel = settingsViewModel,
                gridStyleState = gridStyleState,
                gridColumnsState = gridColumnsState,
                groupByState = groupByState,
                hiddenPinSetState = hiddenPinSetState,
                removeExifOnShareState = removeExifOnShareState,
                hiddenAlbumsEnabledState = hiddenAlbumsEnabledState,
                hiddenImageSharingEnabledState = hiddenImageSharingEnabledState,
                homeMediaTypesOrderState = homeMediaTypesOrderState,
                homeUtilitiesOrderState = homeUtilitiesOrderState,
                hiddenHomeHiddenSectionsState = hiddenHomeHiddenSectionsState,
                resolvedDarkTheme = resolvedDarkTheme,
                hiddenUiStateHolder = hiddenUiStateHolder,
                hiddenTopAlbums = hiddenTopAlbums,
                hiddenTargets = hiddenTargets,
                coroutineScope = coroutineScope,
                navController = navController,
                capturedZoomLaunchState = capturedZoomLaunchState,
                hiddenViewerItemsOverrideState = hiddenViewerItemsOverrideState,
                openViewerZoomed = ::openViewerZoomed
            )
            hiddenAlbumRoutes(
                viewModel = viewModel,
                hiddenViewModel = hiddenViewModel,
                settingsViewModel = settingsViewModel,
                gridColumnsState = gridColumnsState,
                hiddenUiStateHolder = hiddenUiStateHolder,
                hiddenTopAlbums = hiddenTopAlbums,
                hiddenTargets = hiddenTargets,
                hiddenRecycleBinEntries = hiddenRecycleBinEntries,
                hiddenFileNamesFor = ::hiddenFileNamesFor,
                navController = navController,
                capturedZoomLaunchState = capturedZoomLaunchState,
                hiddenViewerItemsOverrideState = hiddenViewerItemsOverrideState,
                openViewerZoomed = ::openViewerZoomed,
                removeExifOnShareState = removeExifOnShareState,
                hiddenImageSharingEnabledState = hiddenImageSharingEnabledState
            )
            utilityRoutes(
                context = context,
                viewModel = viewModel,
                hiddenViewModel = hiddenViewModel,
                settingsViewModel = settingsViewModel,
                libraryTransferViewModel = libraryTransferViewModel,
                gridStyleState = gridStyleState,
                gridColumnsState = gridColumnsState,
                hiddenPinSetState = hiddenPinSetState,
                uiStateHolder = uiStateHolder,
                hiddenUiStateHolder = hiddenUiStateHolder,
                coroutineScope = coroutineScope,
                groupActions = groupActions,
                navController = navController,
                viewerItemsOverrideState = viewerItemsOverrideState,
                openViewerZoomed = ::openViewerZoomed
            )
            viewerRoutes(
                onExternalFinish = onExternalFinish,
                viewModel = viewModel,
                hiddenViewModel = hiddenViewModel,
                albumViewModel = albumViewModel,
                removeExifOnShareState = removeExifOnShareState,
                sharingDisabledState = sharingDisabledState,
                hiddenSectionEnabledState = hiddenSectionEnabledState,
                hiddenImageSharingEnabledState = hiddenImageSharingEnabledState,
                resolvedDarkTheme = resolvedDarkTheme,
                uiStateHolder = uiStateHolder,
                externalItemState = externalItemState,
                hiddenUiStateHolder = hiddenUiStateHolder,
                hiddenTargets = hiddenTargets,
                albumTargets = albumTargets,
                hideItem = ::hideItem,
                navController = navController,
                capturedZoomLaunchState = capturedZoomLaunchState,
                viewerItemsOverrideState = viewerItemsOverrideState,
                hiddenViewerItemsOverrideState = hiddenViewerItemsOverrideState
            )
        }
            ZoomReturnOverlay()
        }
    }
  }
    }
