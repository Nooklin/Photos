package org.nooklin.photos.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import org.nooklin.photos.data.MediaCategory
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.LocalMetadataStore
import org.nooklin.photos.ui.common.GroupActions
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.gallery.CalendarScreen
import org.nooklin.photos.ui.gallery.CategoryScreen
import org.nooklin.photos.ui.gallery.DuplicatesScreen
import org.nooklin.photos.ui.gallery.SearchScreen
import org.nooklin.photos.ui.hidden.HiddenFolderGate
import org.nooklin.photos.ui.hidden.HiddenPinGate
import org.nooklin.photos.ui.hidden.HiddenUiState
import org.nooklin.photos.ui.hidden.HiddenViewModel
import org.nooklin.photos.ui.settings.LibraryTransferViewModel
import org.nooklin.photos.ui.settings.SettingsBackupImportScreen
import org.nooklin.photos.ui.settings.SettingsHiddenBackupScreen
import org.nooklin.photos.ui.settings.SettingsHiddenBackupSelectScreen
import org.nooklin.photos.ui.settings.SettingsHiddenImportScreen
import org.nooklin.photos.ui.settings.SettingsLibraryBackupScreen
import org.nooklin.photos.ui.settings.SettingsViewModel

internal fun NavGraphBuilder.utilityRoutes(
    context: Context,
    viewModel: GalleryViewModel,
    hiddenViewModel: HiddenViewModel,
    settingsViewModel: SettingsViewModel,
    libraryTransferViewModel: LibraryTransferViewModel,
    gridStyleState: State<GridStyle>,
    gridColumnsState: State<Int>,
    hiddenPinSetState: State<Boolean?>,
    uiStateHolder: State<GalleryUiState>,
    hiddenUiStateHolder: State<HiddenUiState>,
    coroutineScope: CoroutineScope,
    groupActions: GroupActions,
    navController: NavHostController,
    viewerItemsOverrideState: MutableState<StableList<MediaItem>?>,
    openViewerZoomed: (Any, String) -> Unit
) {
    val gridStyle by gridStyleState
    val gridColumns by gridColumnsState
    val hiddenPinSet by hiddenPinSetState
    val uiState by uiStateHolder
    val hiddenUiState by hiddenUiStateHolder
    var viewerItemsOverride by viewerItemsOverrideState
    composable("settings/hiddenbackup") {
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
}
