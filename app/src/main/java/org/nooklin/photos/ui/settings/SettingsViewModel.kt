package org.nooklin.photos.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.nooklin.photos.data.AnalysisRepository
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.PersonRepository
import org.nooklin.photos.data.local.AppPreferences
import org.nooklin.photos.data.local.AppFont
import org.nooklin.photos.data.local.AppThemeMode
import org.nooklin.photos.data.local.ContrastColor
import org.nooklin.photos.data.local.GridStyle
import org.nooklin.photos.data.local.GroupBy
import org.nooklin.photos.data.local.HomeItems
import org.nooklin.photos.data.local.HomeSection
import org.nooklin.photos.data.local.MlTag
import org.nooklin.photos.data.local.PinHashing
import org.nooklin.photos.ml.PhotoAnalysisScanner
import org.nooklin.photos.notifications.ScanCompleteNotifier
import org.nooklin.photos.data.local.PinAttemptGuard

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)
    private val analysisRepository = AnalysisRepository(application)
    private val personRepository = PersonRepository(application)
    private val scanner = PhotoAnalysisScanner(application, analysisRepository, personRepository)

    val themeMode: StateFlow<AppThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    val contrastColor: StateFlow<ContrastColor> = preferences.contrastColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContrastColor.DEFAULT)

    fun setContrastColor(color: ContrastColor) {
        viewModelScope.launch { preferences.setContrastColor(color) }
    }

    val appFont: StateFlow<AppFont> = preferences.appFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppFont.SYSTEM_DEFAULT)

    fun setAppFont(font: AppFont) {
        viewModelScope.launch { preferences.setAppFont(font) }
    }

    val gridStyle: StateFlow<GridStyle> = preferences.gridStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GridStyle.SQUARE)

    fun setGridStyle(style: GridStyle) {
        viewModelScope.launch { preferences.setGridStyle(style) }
    }

    val gridColumns: StateFlow<Int> = preferences.gridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    fun setGridColumns(count: Int) {
        viewModelScope.launch { preferences.setGridColumns(count) }
    }

    val libraryPreviewRows: StateFlow<Int> = preferences.libraryPreviewRows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    fun setLibraryPreviewRows(count: Int) {
        viewModelScope.launch { preferences.setLibraryPreviewRows(count) }
    }

    val groupBy: StateFlow<GroupBy> = preferences.groupBy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupBy.NONE)

    fun setGroupBy(groupBy: GroupBy) {
        viewModelScope.launch { preferences.setGroupBy(groupBy) }
    }

    val mlEnabled: StateFlow<Boolean> = preferences.mlEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setMlEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setMlEnabled(enabled) }
    }

    val mlEnabledTags: StateFlow<Set<String>> = preferences.mlEnabledTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setTagEnabled(tag: MlTag, enabled: Boolean) {
        viewModelScope.launch {
            val current = preferences.mlEnabledTags.first()
            val updated = if (enabled) current + tag.id else current - tag.id
            preferences.setMlEnabledTags(updated)
        }
    }

    // Lives here, not in a composable, so rotation does not re-lock the app, yet a new process starts locked.
    private val _appUnlocked = MutableStateFlow(false)
    val appUnlocked: StateFlow<Boolean> = _appUnlocked

    fun unlockApp() {
        _appUnlocked.value = true
    }

    private val _scanProgress = MutableStateFlow<PhotoAnalysisScanner.ScanProgress?>(null)
    val scanProgress: StateFlow<PhotoAnalysisScanner.ScanProgress?> = _scanProgress

    // No-op while a scan runs. Reads the enabled tags fresh, not from a stale StateFlow snapshot.
    fun scanLibrary(items: List<MediaItem>) {
        if (_scanProgress.value?.done == false) return
        viewModelScope.launch {
            val enabledIds = preferences.mlEnabledTags.first()
            val enabledTags = MlTag.entries.filter { it.id in enabledIds && it != MlTag.PETS }.toSet()
            _scanProgress.value = PhotoAnalysisScanner.ScanProgress(0, 0)
            try {
                scanner.scan(items, enabledTags) { progress -> _scanProgress.value = progress }
                if (preferences.scanCompleteNotificationEnabled.first()) {
                    ScanCompleteNotifier.notifyScanComplete(getApplication(), items.size)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed scan must not crash the app or leave the progress stuck, which blocks later scans.
                _scanProgress.value = PhotoAnalysisScanner.ScanProgress(0, 0, done = true)
            }
        }
    }

    // needsAnalysis only checks dateModified, so after a detector change a rescan would skip everything already analyzed
    fun clearAnalysisData() {
        viewModelScope.launch {
            analysisRepository.clearAll()
            // Face data derives from the same analysis state, leaving it would show People with no tags behind them
            personRepository.clearAll()
        }
    }

    // Same as pruneOrphanedMemberships in AlbumViewModel/PetViewModel, for faces and tags
    fun pruneOrphanedAnalysisData(validMediaIds: Set<Long>) {
        viewModelScope.launch {
            personRepository.pruneOrphanedFaces(validMediaIds)
            analysisRepository.pruneOrphanedTags(validMediaIds)
        }
    }

    // One-shot fetch, only needed when the Duplicates screen opens
    fun findDuplicateMediaIds(validMediaIds: Set<Long>, onResult: (Set<Long>) -> Unit) {
        viewModelScope.launch {
            onResult(analysisRepository.findDuplicateMediaIds(validMediaIds))
        }
    }

    // null means "not loaded yet", distinct from a confirmed false
    val appLockEnabled: StateFlow<Boolean?> = preferences.appLockEnabled
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAppLockEnabled(enabled) }
    }

    // null means "not loaded yet", the entry gate must never skip the PIN check before the value arrives
    val hiddenPinSet: StateFlow<Boolean?> = preferences.hiddenPinHash
        .map<String?, Boolean?> { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun verifyHiddenPin(pin: String): Boolean {
        // While locked out the guess is not even checked, so a correct one cannot get through early.
        if (PinAttemptGuard.remainingMs() > 0L) return false
        val hash = preferences.hiddenPinHash.first() ?: return false
        return recordAttempt(PinHashing.matches(pin, hash))
    }

    suspend fun verifyHiddenPinRecoveryCode(code: String): Boolean {
        if (PinAttemptGuard.remainingMs() > 0L) return false
        val hash = preferences.hiddenPinRecoveryHash.first() ?: return false
        return recordAttempt(PinHashing.matches(code, hash))
    }

    private suspend fun recordAttempt(correct: Boolean): Boolean {
        if (correct) PinAttemptGuard.registerSuccess(preferences) else PinAttemptGuard.registerFailure(preferences)
        return correct
    }

    // New PIN and new recovery code together, the old code is invalidated so it can't work twice
    suspend fun setHiddenPin(pin: String): String {
        val recoveryCode = PinHashing.generateRecoveryCode()
        preferences.setHiddenPinHash(PinHashing.hashWithNewSalt(pin))
        preferences.setHiddenPinRecoveryHash(PinHashing.hashWithNewSalt(recoveryCode.replace("-", "")))
        return recoveryCode
    }

    suspend fun clearHiddenPin() {
        preferences.setHiddenPinHash(null)
        preferences.setHiddenPinRecoveryHash(null)
    }

    val disableScreenshots: StateFlow<Boolean> = preferences.disableScreenshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDisableScreenshots(enabled: Boolean) {
        viewModelScope.launch { preferences.setDisableScreenshots(enabled) }
    }

    val removeExifOnShare: StateFlow<Boolean> = preferences.removeExifOnShare
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setRemoveExifOnShare(enabled: Boolean) {
        viewModelScope.launch { preferences.setRemoveExifOnShare(enabled) }
    }

    val sharingDisabled: StateFlow<Boolean> = preferences.sharingDisabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Disable Sharing ON also forces hiddenImageSharingEnabled off, the whole-app switch overrides it
    fun setSharingDisabled(disabled: Boolean) {
        viewModelScope.launch {
            preferences.setSharingDisabled(disabled)
            if (disabled) preferences.setHiddenImageSharingEnabled(false)
        }
    }

    val hiddenSectionEnabled: StateFlow<Boolean> = preferences.hiddenSectionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setHiddenSectionEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setHiddenSectionEnabled(enabled) }
    }

    val hiddenAlbumsEnabled: StateFlow<Boolean> = preferences.hiddenAlbumsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setHiddenAlbumsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setHiddenAlbumsEnabled(enabled) }
    }

    val hiddenImageSharingEnabled: StateFlow<Boolean> = preferences.hiddenImageSharingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setHiddenImageSharingEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setHiddenImageSharingEnabled(enabled) }
    }

    val hiddenBackupNoticeAcknowledged: StateFlow<Boolean> = preferences.hiddenBackupNoticeAcknowledged
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Defaults to true: WhileSubscribed shows the initial value briefly and it must not flash the dialog
    fun acknowledgeHiddenBackupNotice() {
        viewModelScope.launch { preferences.setHiddenBackupNoticeAcknowledged(true) }
    }

    val scanCompleteNotificationEnabled: StateFlow<Boolean> = preferences.scanCompleteNotificationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setScanCompleteNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setScanCompleteNotificationEnabled(enabled) }
    }

    val homeSectionOrder: StateFlow<List<HomeSection>> = preferences.homeSectionOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeSection.DEFAULT_ORDER)

    fun setHomeSectionOrder(order: List<HomeSection>) {
        viewModelScope.launch { preferences.setHomeSectionOrder(order) }
    }

    val homeMediaTypesOrder: StateFlow<List<String>> = preferences.homeMediaTypesOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeItems.MEDIA_TYPES)

    val homeUtilitiesOrder: StateFlow<List<String>> = preferences.homeUtilitiesOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeItems.UTILITIES)

    fun setHomeMediaTypesOrder(order: List<String>) {
        viewModelScope.launch { preferences.setHomeMediaTypesOrder(order) }
    }

    fun setHomeUtilitiesOrder(order: List<String>) {
        viewModelScope.launch { preferences.setHomeUtilitiesOrder(order) }
    }

    val homeHiddenSections: StateFlow<Set<HomeSection>> = preferences.homeHiddenSections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val hiddenHomeHiddenSections: StateFlow<Set<HomeSection>> = preferences.hiddenHomeHiddenSections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setHomeHiddenSections(sections: Set<HomeSection>) {
        viewModelScope.launch { preferences.setHomeHiddenSections(sections) }
    }

    fun setHiddenHomeHiddenSections(sections: Set<HomeSection>) {
        viewModelScope.launch { preferences.setHiddenHomeHiddenSections(sections) }
    }
}
