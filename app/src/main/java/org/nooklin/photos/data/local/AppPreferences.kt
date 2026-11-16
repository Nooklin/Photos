package org.nooklin.photos.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.annotation.StringRes
import org.nooklin.photos.R
import org.nooklin.photos.appString

private val Context.appSettingsDataStore by preferencesDataStore(name = "nooklin_app_settings")

enum class AppThemeMode { LIGHT, DARK, BLACK, SYSTEM }
enum class GridStyle { ORIGINAL, SQUARE }
enum class GroupBy { NONE, DAY, WEEK, MONTH, YEAR }

const val MIN_GRID_COLUMNS = 1
const val MAX_GRID_COLUMNS = 6
const val MIN_LIBRARY_PREVIEW_ROWS = 1
const val MAX_LIBRARY_PREVIEW_ROWS = 8

// storageKey, so renaming the enum can't corrupt a saved order
enum class HomeSection(val storageKey: String, @param:StringRes val labelRes: Int) {
    LIBRARY("library", R.string.common_library),
    ALBUMS("albums", R.string.common_albums),
    DISCOVERED("discovered", R.string.common_discovered),
    MEDIA_TYPES("media_types", R.string.common_media_types),
    UTILITIES("utilities", R.string.common_utilities),
    SETTINGS("settings", R.string.common_settings);
    val label: String get() = appString(labelRes)
    companion object {
        val DEFAULT_ORDER = listOf(LIBRARY, ALBUMS, DISCOVERED, MEDIA_TYPES, UTILITIES, SETTINGS)
        val HIDEABLE = listOf(ALBUMS, DISCOVERED, MEDIA_TYPES, UTILITIES)
        fun fromKey(key: String): HomeSection? = entries.find { it.storageKey == key }
    }
}

enum class ContrastColor(val storageKey: String, @param:StringRes val labelRes: Int, val color: Color?) {
    DEFAULT("default", R.string.contrast_color_default, null),
    BLUE("blue", R.string.contrast_color_blue, Color(0xFF0A84FF)),
    RED("red", R.string.contrast_color_red, Color(0xFFFF3B30)),
    ORANGE("orange", R.string.contrast_color_orange, Color(0xFFFF9500)),
    YELLOW("yellow", R.string.contrast_color_yellow, Color(0xFFFFCC00)),
    GREEN("green", R.string.contrast_color_green, Color(0xFF34C759)),
    TEAL("teal", R.string.contrast_color_teal, Color(0xFF30B0C7)),
    PURPLE("purple", R.string.contrast_color_purple, Color(0xFFAF52DE)),
    PINK("pink", R.string.contrast_color_pink, Color(0xFFFF2D55));
    val label: String get() = appString(labelRes)
    companion object {
        fun fromKey(key: String): ContrastColor? = entries.find { it.storageKey == key }
    }
}

// Two fonts only: Helvetica has no free license to bundle
enum class AppFont(val storageKey: String, @param:StringRes val labelRes: Int) {
    INTER("inter", R.string.app_font_inter),
    SYSTEM_DEFAULT("system_default", R.string.app_font_stock_android_font),
    WORK_SANS("work_sans", R.string.app_font_work_sans);
    val label: String get() = appString(labelRes)
    companion object {
        fun fromKey(key: String): AppFont? = entries.find { it.storageKey == key }
    }
}

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val CONTRAST_COLOR_KEY = stringPreferencesKey("contrast_color")
// v2: Inter became the default for everyone, so the earlier "app_font" choice is no longer read
private val APP_FONT_KEY = stringPreferencesKey("app_font_v2")

private val GRID_STYLE_KEY = stringPreferencesKey("grid_style")
private val GRID_COLUMNS_KEY = intPreferencesKey("grid_columns")
private val HOME_MEDIA_TYPES_ORDER_KEY = stringPreferencesKey("home_media_types_order")
private val HOME_UTILITIES_ORDER_KEY = stringPreferencesKey("home_utilities_order")
private val HOME_HIDDEN_SECTIONS_KEY = stringPreferencesKey("home_hidden_sections")
private val HIDDEN_HOME_HIDDEN_SECTIONS_KEY = stringPreferencesKey("hidden_home_hidden_sections")
private val LIBRARY_PREVIEW_ROWS_KEY = intPreferencesKey("library_preview_rows")
private val GROUP_BY_KEY = stringPreferencesKey("group_by")

private val ML_ENABLED_KEY = booleanPreferencesKey("ml_enabled")
private val ML_ENABLED_TAGS_KEY = stringSetPreferencesKey("ml_enabled_tags")

private val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
private val DISABLE_SCREENSHOTS_KEY = booleanPreferencesKey("disable_screenshots")
private val HIDDEN_PIN_HASH_KEY = stringPreferencesKey("hidden_pin_hash")
private val PIN_FAILED_ATTEMPTS_KEY = intPreferencesKey("pin_failed_attempts")
private val PIN_LOCKED_UNTIL_KEY = longPreferencesKey("pin_locked_until")
private val HIDDEN_PIN_RECOVERY_HASH_KEY = stringPreferencesKey("hidden_pin_recovery_hash")

private val REMOVE_EXIF_ON_SHARE_KEY = booleanPreferencesKey("remove_exif_on_share")
private val SHARING_DISABLED_KEY = booleanPreferencesKey("sharing_disabled")
private val COLORFUL_KEY = booleanPreferencesKey("colorful")
private val SHOW_SCREENSHOTS_IN_LIBRARY_KEY = booleanPreferencesKey("show_screenshots_in_library")
private val STRIP_LOCATION_KEY = booleanPreferencesKey("strip_location")
private val STRIP_CAMERA_KEY = booleanPreferencesKey("strip_camera")
private val STRIP_DATE_TIME_KEY = booleanPreferencesKey("strip_date_time")
private val STRIP_DESCRIPTION_KEY = booleanPreferencesKey("strip_description")

private val HIDDEN_SECTION_ENABLED_KEY = booleanPreferencesKey("hidden_section_enabled")
private val HIDDEN_ALBUMS_ENABLED_KEY = booleanPreferencesKey("hidden_albums_enabled")
private val HIDDEN_IMAGE_SHARING_KEY = booleanPreferencesKey("hidden_image_sharing_enabled")
private val HIDDEN_BACKUP_NOTICE_ACKNOWLEDGED_KEY = booleanPreferencesKey("hidden_backup_notice_acknowledged")

private val SCAN_COMPLETE_NOTIFICATION_KEY = booleanPreferencesKey("scan_complete_notification_enabled")

private val HOME_SECTION_ORDER_KEY = stringPreferencesKey("home_section_order")
private val SHARING_NOTICE_HIDDEN_KEY = booleanPreferencesKey("sharing_notice_hidden")
private val AUTO_ALBUMS_KEY = stringSetPreferencesKey("auto_albums")
private val HIDDEN_AUTO_ALBUMS_KEY = stringSetPreferencesKey("hidden_auto_albums")
private val VIDEO_TRIMS_KEY = stringSetPreferencesKey("video_trims")
private val HAPTICS_KEY = booleanPreferencesKey("haptics_enabled")
private val HIDDEN_ALBUM_PREVIEWS_OFF_KEY = booleanPreferencesKey("hidden_album_previews_off")

private fun albumSortOf(value: String?, fallback: AlbumSort): AlbumSort =
    AlbumSort.entries.firstOrNull { it.name == value } ?: fallback

private fun readAlbumOrder(prefs: Preferences, hidden: Boolean): AlbumOrder {
    val p = if (hidden) "hidden_" else ""
    return AlbumOrder(
        sort = albumSortOf(prefs[stringPreferencesKey(p + "album_sort")], AlbumSort.NAME),
        folderSort = albumSortOf(prefs[stringPreferencesKey(p + "folder_sort")], AlbumSort.NAME),
        manual = prefs[stringPreferencesKey(p + "album_manual_order")]
            ?.split(',')?.mapNotNull { it.toLongOrNull() } ?: emptyList(),
        homeSort = albumSortOf(prefs[stringPreferencesKey(p + "home_album_sort")], AlbumSort.SIZE),
        homeRows = (prefs[intPreferencesKey(p + "home_album_rows")] ?: 1).coerceIn(1, 2)
    )
}

private fun writeAlbumOrder(prefs: MutablePreferences, hidden: Boolean, order: AlbumOrder) {
    val p = if (hidden) "hidden_" else ""
    prefs[stringPreferencesKey(p + "album_sort")] = order.sort.name
    prefs[stringPreferencesKey(p + "folder_sort")] = order.folderSort.name
    prefs[stringPreferencesKey(p + "album_manual_order")] = order.manual.joinToString(",")
    prefs[stringPreferencesKey(p + "home_album_sort")] = order.homeSort.name
    prefs[intPreferencesKey(p + "home_album_rows")] = order.homeRows.coerceIn(1, 2)
}

class AppPreferences(private val context: Context) {

    val themeMode: Flow<AppThemeMode> = context.appSettingsDataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            "light" -> AppThemeMode.LIGHT
            "dark" -> AppThemeMode.DARK
            "black" -> AppThemeMode.BLACK
            else -> AppThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name.lowercase()
        }
    }

    val colorful: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs -> prefs[COLORFUL_KEY] ?: true }

    suspend fun setColorful(colorful: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[COLORFUL_KEY] = colorful }
    }

    val contrastColor: Flow<ContrastColor> = context.appSettingsDataStore.data.map { prefs ->
        prefs[CONTRAST_COLOR_KEY]?.let { ContrastColor.fromKey(it) } ?: ContrastColor.DEFAULT
    }

    suspend fun setContrastColor(color: ContrastColor) {
        context.appSettingsDataStore.edit { prefs -> prefs[CONTRAST_COLOR_KEY] = color.storageKey }
    }

    val appFont: Flow<AppFont> = context.appSettingsDataStore.data.map { prefs ->
        prefs[APP_FONT_KEY]?.let { AppFont.fromKey(it) } ?: AppFont.INTER
    }

    suspend fun setAppFont(font: AppFont) {
        context.appSettingsDataStore.edit { prefs -> prefs[APP_FONT_KEY] = font.storageKey }
    }

    val gridStyle: Flow<GridStyle> = context.appSettingsDataStore.data.map { prefs ->
        when (prefs[GRID_STYLE_KEY]) {
            "original" -> GridStyle.ORIGINAL
            else -> GridStyle.SQUARE
        }
    }

    suspend fun setGridStyle(style: GridStyle) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[GRID_STYLE_KEY] = style.name.lowercase()
        }
    }

    val gridColumns: Flow<Int> = context.appSettingsDataStore.data.map { prefs ->
        (prefs[GRID_COLUMNS_KEY] ?: 4).coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }

    suspend fun setGridColumns(count: Int) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[GRID_COLUMNS_KEY] = count.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
        }
    }

    fun screenGridColumns(key: String): Flow<Int?> = context.appSettingsDataStore.data.map { prefs ->
        prefs[intPreferencesKey("grid_columns_$key")]?.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }

    suspend fun setScreenGridColumns(key: String, count: Int) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[intPreferencesKey("grid_columns_$key")] = count.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
        }
    }

    val libraryPreviewRows: Flow<Int> = context.appSettingsDataStore.data.map { prefs ->
        (prefs[LIBRARY_PREVIEW_ROWS_KEY] ?: 2).coerceIn(MIN_LIBRARY_PREVIEW_ROWS, MAX_LIBRARY_PREVIEW_ROWS)
    }

    suspend fun setLibraryPreviewRows(count: Int) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[LIBRARY_PREVIEW_ROWS_KEY] = count.coerceIn(MIN_LIBRARY_PREVIEW_ROWS, MAX_LIBRARY_PREVIEW_ROWS)
        }
    }

    val groupBy: Flow<GroupBy> = context.appSettingsDataStore.data.map { prefs ->
        when (prefs[GROUP_BY_KEY]) {
            "day" -> GroupBy.DAY
            "week" -> GroupBy.WEEK
            "month" -> GroupBy.MONTH
            "year" -> GroupBy.YEAR
            else -> GroupBy.NONE
        }
    }

    suspend fun setGroupBy(groupBy: GroupBy) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[GROUP_BY_KEY] = groupBy.name.lowercase()
        }
    }

    val mlEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[ML_ENABLED_KEY] ?: false
    }

    suspend fun setMlEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[ML_ENABLED_KEY] = enabled }
    }

    val mlEnabledTags: Flow<Set<String>> = context.appSettingsDataStore.data.map { prefs ->
        prefs[ML_ENABLED_TAGS_KEY] ?: emptySet()
    }

    suspend fun setMlEnabledTags(tagIds: Set<String>) {
        context.appSettingsDataStore.edit { prefs -> prefs[ML_ENABLED_TAGS_KEY] = tagIds }
    }

    val appLockEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[APP_LOCK_ENABLED_KEY] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[APP_LOCK_ENABLED_KEY] = enabled }
    }

    val pinFailedAttempts: Flow<Int> = context.appSettingsDataStore.data.map { prefs ->
        prefs[PIN_FAILED_ATTEMPTS_KEY] ?: 0
    }

    val pinLockedUntil: Flow<Long> = context.appSettingsDataStore.data.map { prefs ->
        prefs[PIN_LOCKED_UNTIL_KEY] ?: 0L
    }

    suspend fun setPinAttemptState(failedAttempts: Int, lockedUntil: Long) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[PIN_FAILED_ATTEMPTS_KEY] = failedAttempts
            prefs[PIN_LOCKED_UNTIL_KEY] = lockedUntil
        }
    }

    val hiddenPinHash: Flow<String?> = context.appSettingsDataStore.data.map { prefs ->
        prefs[HIDDEN_PIN_HASH_KEY]
    }

    suspend fun setHiddenPinHash(hash: String?) {
        context.appSettingsDataStore.edit { prefs ->
            if (hash == null) prefs.remove(HIDDEN_PIN_HASH_KEY) else prefs[HIDDEN_PIN_HASH_KEY] = hash
        }
    }

    val hiddenPinRecoveryHash: Flow<String?> = context.appSettingsDataStore.data.map { prefs ->
        prefs[HIDDEN_PIN_RECOVERY_HASH_KEY]
    }

    suspend fun setHiddenPinRecoveryHash(hash: String?) {
        context.appSettingsDataStore.edit { prefs ->
            if (hash == null) prefs.remove(HIDDEN_PIN_RECOVERY_HASH_KEY) else prefs[HIDDEN_PIN_RECOVERY_HASH_KEY] = hash
        }
    }

    val disableScreenshots: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[DISABLE_SCREENSHOTS_KEY] ?: false
    }

    suspend fun setDisableScreenshots(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[DISABLE_SCREENSHOTS_KEY] = enabled }
    }

    val removeExifOnShare: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[REMOVE_EXIF_ON_SHARE_KEY] ?: true
    }

    suspend fun setRemoveExifOnShare(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[REMOVE_EXIF_ON_SHARE_KEY] = enabled }
    }

    val shareStripOptions: Flow<ShareStripOptions> = context.appSettingsDataStore.data.map { prefs ->
        ShareStripOptions(
            location = prefs[STRIP_LOCATION_KEY] ?: true,
            camera = prefs[STRIP_CAMERA_KEY] ?: true,
            dateTime = prefs[STRIP_DATE_TIME_KEY] ?: true,
            description = prefs[STRIP_DESCRIPTION_KEY] ?: true
        )
    }

    suspend fun setShareStripOption(category: StripCategory, enabled: Boolean) {
        val key = when (category) {
            StripCategory.LOCATION -> STRIP_LOCATION_KEY
            StripCategory.CAMERA -> STRIP_CAMERA_KEY
            StripCategory.DATE_TIME -> STRIP_DATE_TIME_KEY
            StripCategory.DESCRIPTION -> STRIP_DESCRIPTION_KEY
        }
        context.appSettingsDataStore.edit { prefs -> prefs[key] = enabled }
    }

    val showScreenshotsInLibrary: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[SHOW_SCREENSHOTS_IN_LIBRARY_KEY] ?: true
    }

    suspend fun setShowScreenshotsInLibrary(show: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[SHOW_SCREENSHOTS_IN_LIBRARY_KEY] = show }
    }

    val sharingNoticeHidden: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[SHARING_NOTICE_HIDDEN_KEY] ?: false
    }

    suspend fun setSharingNoticeHidden(hidden: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[SHARING_NOTICE_HIDDEN_KEY] = hidden }
    }

    val sharingDisabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[SHARING_DISABLED_KEY] ?: true
    }

    suspend fun setSharingDisabled(disabled: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[SHARING_DISABLED_KEY] = disabled }
    }

    // Off by default. Display only: turning it off keeps every hidden file and database row
    val hiddenSectionEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[HIDDEN_SECTION_ENABLED_KEY] ?: false
    }

    suspend fun setHiddenSectionEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[HIDDEN_SECTION_ENABLED_KEY] = enabled }
    }

    // Display only: hidden albums stay in the database when switched off
    val hiddenAlbumsEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[HIDDEN_ALBUMS_ENABLED_KEY] ?: true
    }

    suspend fun setHiddenAlbumsEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[HIDDEN_ALBUMS_ENABLED_KEY] = enabled }
    }

    val hiddenImageSharingEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[HIDDEN_IMAGE_SHARING_KEY] ?: false
    }

    suspend fun setHiddenImageSharingEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[HIDDEN_IMAGE_SHARING_KEY] = enabled }
    }

    val hiddenBackupNoticeAcknowledged: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[HIDDEN_BACKUP_NOTICE_ACKNOWLEDGED_KEY] ?: false
    }

    suspend fun setHiddenBackupNoticeAcknowledged(acknowledged: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[HIDDEN_BACKUP_NOTICE_ACKNOWLEDGED_KEY] = acknowledged }
    }

    val scanCompleteNotificationEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[SCAN_COMPLETE_NOTIFICATION_KEY] ?: false
    }

    suspend fun setScanCompleteNotificationEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[SCAN_COMPLETE_NOTIFICATION_KEY] = enabled }
    }

    val homeSectionOrder: Flow<List<HomeSection>> = context.appSettingsDataStore.data.map { prefs ->
        val stored = prefs[HOME_SECTION_ORDER_KEY]
        val parsed = stored?.split(",")?.mapNotNull { HomeSection.fromKey(it) }.orEmpty()
        val missing = HomeSection.DEFAULT_ORDER.filter { it !in parsed }
        (parsed + missing).ifEmpty { HomeSection.DEFAULT_ORDER }
    }

    suspend fun setHomeSectionOrder(order: List<HomeSection>) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[HOME_SECTION_ORDER_KEY] = order.joinToString(",") { it.storageKey }
        }
    }

    val homeMediaTypesOrder: Flow<List<String>> = context.appSettingsDataStore.data.map { prefs ->
        HomeItems.ordered(HomeItems.MEDIA_TYPES, prefs[HOME_MEDIA_TYPES_ORDER_KEY]?.split(",").orEmpty())
    }

    val homeUtilitiesOrder: Flow<List<String>> = context.appSettingsDataStore.data.map { prefs ->
        HomeItems.ordered(HomeItems.UTILITIES, prefs[HOME_UTILITIES_ORDER_KEY]?.split(",").orEmpty())
    }

    suspend fun setHomeMediaTypesOrder(order: List<String>) {
        context.appSettingsDataStore.edit { prefs -> prefs[HOME_MEDIA_TYPES_ORDER_KEY] = order.joinToString(",") }
    }

    suspend fun setHomeUtilitiesOrder(order: List<String>) {
        context.appSettingsDataStore.edit { prefs -> prefs[HOME_UTILITIES_ORDER_KEY] = order.joinToString(",") }
    }

    val homeHiddenSections: Flow<Set<HomeSection>> = context.appSettingsDataStore.data.map { prefs ->
        parseHiddenSections(prefs[HOME_HIDDEN_SECTIONS_KEY])
    }

    val hiddenHomeHiddenSections: Flow<Set<HomeSection>> = context.appSettingsDataStore.data.map { prefs ->
        parseHiddenSections(prefs[HIDDEN_HOME_HIDDEN_SECTIONS_KEY])
    }

    suspend fun setHomeHiddenSections(sections: Set<HomeSection>) {
        context.appSettingsDataStore.edit { prefs -> prefs[HOME_HIDDEN_SECTIONS_KEY] = sections.joinToString(",") { it.storageKey } }
    }

    suspend fun setHiddenHomeHiddenSections(sections: Set<HomeSection>) {
        context.appSettingsDataStore.edit { prefs -> prefs[HIDDEN_HOME_HIDDEN_SECTIONS_KEY] = sections.joinToString(",") { it.storageKey } }
    }

    // Only the 4 hideable sections, Library and Settings stay
    private fun parseHiddenSections(stored: String?): Set<HomeSection> =
        stored?.split(",")?.mapNotNull { HomeSection.fromKey(it) }.orEmpty().filter { it in HomeSection.HIDEABLE }.toSet()

    // "rule:albumId", the album each automatic folder album (Camera, Screenshots, ...) was created or adopted as
    val autoAlbums: Flow<Set<String>> = context.appSettingsDataStore.data.map { prefs ->
        prefs[AUTO_ALBUMS_KEY] ?: emptySet()
    }

    suspend fun setAutoAlbums(entries: Set<String>) {
        context.appSettingsDataStore.edit { prefs -> prefs[AUTO_ALBUMS_KEY] = entries }
    }

    // rule keys of automatic albums the user hides from the album lists
    val hiddenAutoAlbums: Flow<Set<String>> = context.appSettingsDataStore.data.map { prefs ->
        prefs[HIDDEN_AUTO_ALBUMS_KEY] ?: emptySet()
    }

    suspend fun setAutoAlbumHidden(rule: String, hidden: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            val current = prefs[HIDDEN_AUTO_ALBUMS_KEY] ?: emptySet()
            prefs[HIDDEN_AUTO_ALBUMS_KEY] = if (hidden) current + rule else current - rule
        }
    }

    fun albumOrder(hidden: Boolean): Flow<AlbumOrder> =
        context.appSettingsDataStore.data.map { prefs -> readAlbumOrder(prefs, hidden) }

    suspend fun updateAlbumOrder(hidden: Boolean, change: (AlbumOrder) -> AlbumOrder) {
        context.appSettingsDataStore.edit { prefs ->
            writeAlbumOrder(prefs, hidden, change(readAlbumOrder(prefs, hidden)))
        }
    }

    // Hidden albums and folders show a placeholder instead of their photos
    val hiddenAlbumPreviewsOff: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[HIDDEN_ALBUM_PREVIEWS_OFF_KEY] ?: false
    }

    suspend fun setHiddenAlbumPreviewsOff(off: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[HIDDEN_ALBUM_PREVIEWS_OFF_KEY] = off }
    }

    val hapticsEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[HAPTICS_KEY] ?: true
    }

    suspend fun setHapticsEnabled(on: Boolean) {
        context.appSettingsDataStore.edit { prefs -> prefs[HAPTICS_KEY] = on }
    }

    val videoTrims: Flow<Set<String>> = context.appSettingsDataStore.data.map { prefs ->
        prefs[VIDEO_TRIMS_KEY] ?: emptySet()
    }

    suspend fun setVideoTrims(values: Set<String>) {
        context.appSettingsDataStore.edit { prefs -> prefs[VIDEO_TRIMS_KEY] = values }
    }
}
