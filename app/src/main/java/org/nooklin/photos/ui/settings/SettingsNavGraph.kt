package org.nooklin.photos.ui.settings

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.nooklin.photos.ui.GalleryViewModel

// settings/hiddenbackup is defined in NooklinPhotosApp.kt, not here
fun NavGraphBuilder.settingsNavGraph(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    galleryViewModel: GalleryViewModel,
    onOpenHiddenBackup: () -> Unit
) {
    composable(ROUTE_SETTINGS) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onNavigate = { sub -> navController.navigate("settings/$sub") }
        )
    }

    composable("settings/appearance") {
        val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
        val contrastColor by settingsViewModel.contrastColor.collectAsStateWithLifecycle()
        val appFont by settingsViewModel.appFont.collectAsStateWithLifecycle()
        SettingsAppearanceScreen(
            themeMode = themeMode,
            onThemeModeChange = settingsViewModel::setThemeMode,
            contrastColor = contrastColor,
            onContrastColorChange = settingsViewModel::setContrastColor,
            appFont = appFont,
            onAppFontChange = settingsViewModel::setAppFont,
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/gallery") {
        val gridStyle by settingsViewModel.gridStyle.collectAsStateWithLifecycle()
        val gridColumns by settingsViewModel.gridColumns.collectAsStateWithLifecycle()
        val groupBy by settingsViewModel.groupBy.collectAsStateWithLifecycle()
        val libraryPreviewRows by settingsViewModel.libraryPreviewRows.collectAsStateWithLifecycle()
        SettingsGalleryScreen(
            gridStyle = gridStyle,
            onGridStyleChange = settingsViewModel::setGridStyle,
            gridColumns = gridColumns,
            onGridColumnsChange = settingsViewModel::setGridColumns,
            groupBy = groupBy,
            onGroupByChange = settingsViewModel::setGroupBy,
            libraryPreviewRows = libraryPreviewRows,
            onLibraryPreviewRowsChange = settingsViewModel::setLibraryPreviewRows,
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/machinelearning") {
        val mlEnabled by settingsViewModel.mlEnabled.collectAsStateWithLifecycle()
        val mlEnabledTags by settingsViewModel.mlEnabledTags.collectAsStateWithLifecycle()
        val scanProgress by settingsViewModel.scanProgress.collectAsStateWithLifecycle()
        val uiState by galleryViewModel.uiState.collectAsStateWithLifecycle()
        SettingsMachineLearningScreen(
            mlEnabled = mlEnabled,
            onMlEnabledChange = settingsViewModel::setMlEnabled,
            enabledTagIds = mlEnabledTags,
            onTagToggle = { tag, enabled -> settingsViewModel.setTagEnabled(tag, enabled) },
            scanProgress = scanProgress,
            onScanLibrary = { settingsViewModel.scanLibrary(uiState.allItems) },
            onClearAnalysisData = { settingsViewModel.clearAnalysisData() },
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/security") {
        // ?: false only for this row's display, unlike the app lock gate which keeps null distinct
        val appLockEnabled by settingsViewModel.appLockEnabled.collectAsStateWithLifecycle()
        val disableScreenshots by settingsViewModel.disableScreenshots.collectAsStateWithLifecycle()
        SettingsSecurityScreen(
            appLockEnabled = appLockEnabled ?: false,
            onAppLockEnabledChange = settingsViewModel::setAppLockEnabled,
            disableScreenshots = disableScreenshots,
            onDisableScreenshotsChange = settingsViewModel::setDisableScreenshots,
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/exif") {
        val removeExifOnShare by settingsViewModel.removeExifOnShare.collectAsStateWithLifecycle()
        val sharingDisabled by settingsViewModel.sharingDisabled.collectAsStateWithLifecycle()
        SettingsExifScreen(
            removeExifOnShare = removeExifOnShare,
            onRemoveExifOnShareChange = settingsViewModel::setRemoveExifOnShare,
            sharingDisabled = sharingDisabled,
            onSharingDisabledChange = settingsViewModel::setSharingDisabled,
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/hiddensection") {
        val hiddenPinSet by settingsViewModel.hiddenPinSet.collectAsStateWithLifecycle()
        val hiddenSectionEnabled by settingsViewModel.hiddenSectionEnabled.collectAsStateWithLifecycle()
        SettingsHiddenSectionScreen(
            hiddenSectionEnabled = hiddenSectionEnabled,
            onHiddenSectionEnabledChange = settingsViewModel::setHiddenSectionEnabled,
            hiddenPinSet = hiddenPinSet ?: false,
            onNavigate = { sub ->
                if (sub == "hiddenbackup") {
                    onOpenHiddenBackup()
                } else {
                    navController.navigate("settings/$sub")
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
    composable("settings/hiddenalbums") {
        val hiddenAlbumsEnabled by settingsViewModel.hiddenAlbumsEnabled.collectAsStateWithLifecycle()
        SettingsHiddenAlbumsScreen(
            hiddenAlbumsEnabled = hiddenAlbumsEnabled,
            onHiddenAlbumsEnabledChange = settingsViewModel::setHiddenAlbumsEnabled,
            onBack = { navController.popBackStack() }
        )
    }
    composable("settings/hiddensharing") {
        val hiddenImageSharingEnabled by settingsViewModel.hiddenImageSharingEnabled.collectAsStateWithLifecycle()
        val sharingDisabled by settingsViewModel.sharingDisabled.collectAsStateWithLifecycle()
        SettingsHiddenSharingScreen(
            hiddenImageSharingEnabled = hiddenImageSharingEnabled,
            onHiddenImageSharingEnabledChange = settingsViewModel::setHiddenImageSharingEnabled,
            sharingDisabled = sharingDisabled,
            onBack = { navController.popBackStack() }
        )
    }
    composable("settings/hiddensection/hidden_pin") {
        val hiddenPinSet by settingsViewModel.hiddenPinSet.collectAsStateWithLifecycle()
        SettingsHiddenPinScreen(
            pinSet = hiddenPinSet ?: false,
            verifyPin = settingsViewModel::verifyHiddenPin,
            verifyRecoveryCode = settingsViewModel::verifyHiddenPinRecoveryCode,
            setNewPin = settingsViewModel::setHiddenPin,
            onClearPin = settingsViewModel::clearHiddenPin,
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/notifications") {
        val scanCompleteNotificationEnabled by settingsViewModel.scanCompleteNotificationEnabled.collectAsStateWithLifecycle()
        SettingsNotificationsScreen(
            scanCompleteNotificationEnabled = scanCompleteNotificationEnabled,
            onScanCompleteNotificationEnabledChange = settingsViewModel::setScanCompleteNotificationEnabled,
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/help") {
        SettingsHelpScreen(onBack = { navController.popBackStack() })
    }

    composable("settings/about") {
        SettingsAboutScreen(onBack = { navController.popBackStack() })
    }
}

internal const val ROUTE_SETTINGS = "settings"
