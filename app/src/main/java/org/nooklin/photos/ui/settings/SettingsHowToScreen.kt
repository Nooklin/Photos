package org.nooklin.photos.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nooklin.photos.data.local.MlTag
import org.nooklin.photos.data.local.ShareStripOptions
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.R
import org.nooklin.photos.appString

// The settings that change what the app does, read live so the How To always matches this device
data class HowToState(
    val hiddenSectionEnabled: Boolean,
    val hiddenPinSet: Boolean,
    val hiddenAlbumsEnabled: Boolean,
    val hiddenImageSharingEnabled: Boolean,
    val sharingDisabled: Boolean,
    val removeExifOnShare: Boolean,
    val appLockEnabled: Boolean,
    val disableScreenshots: Boolean,
    val mlEnabled: Boolean,
    val mlEnabledTags: Set<String>,
    val scanCompleteNotificationEnabled: Boolean,
    val stripOptions: ShareStripOptions = ShareStripOptions()
)

private class HowToLink(val label: String, val route: String, val on: Boolean? = null)

private class HowToTopic(
    val title: String,
    val text: String,
    val now: String? = null,
    val links: List<HowToLink> = emptyList()
)

// The kinds of data that are removed, in the words of Settings, Sharing
private fun stripNames(options: ShareStripOptions): String = listOfNotNull(
    if (options.location) appString(R.string.settings_exif_strip_location) else null,
    if (options.camera) appString(R.string.settings_exif_strip_camera) else null,
    if (options.dateTime) appString(R.string.settings_exif_strip_date_time) else null,
    if (options.description) appString(R.string.settings_exif_strip_description) else null
).joinToString(", ")

private fun howToTopics(s: HowToState): List<HowToTopic> {
    val mlTags = MlTag.entries.filter { it.id in s.mlEnabledTags }.joinToString { it.label }
    val duplicatesOn = MlTag.DUPLICATES.id in s.mlEnabledTags
    val removedNames = stripNames(s.stripOptions)

    return listOf(
        HowToTopic(
            appString(R.string.common_library),
            appString(R.string.settings_how_to_all_photos_and_videos_on),
            links = listOf(
                HowToLink(appString(R.string.settings_how_to_gallery_grid_size_ratio_grouping), "settings/gallery"),
                HowToLink(appString(R.string.settings_how_to_appearance_theme_color_font), "settings/appearance")
            )
        ),
        HowToTopic(
            appString(R.string.common_albums),
            appString(R.string.settings_how_to_create_an_album_with_the)
        ),
        HowToTopic(
            appString(R.string.settings_how_to_favorites_recently_saved_and_recently),
            appString(R.string.settings_how_to_tap_the_heart_in_the)
        ),
        HowToTopic(
            appString(R.string.settings_how_to_media_types_and_your_home),
            appString(R.string.settings_how_to_home_lists_videos_panoramas_screenshot)
        ),
        HowToTopic(
            appString(R.string.settings_how_to_timeline_and_calendar),
            appString(R.string.settings_how_to_while_you_scroll_the_library)
        ),
        HowToTopic(
            appString(R.string.settings_how_to_selecting_items),
            appString(R.string.settings_how_to_tap_select_or_press_and)
        ),
        HowToTopic(
            appString(R.string.settings_how_to_editing),
            appString(R.string.settings_how_to_tap_the_edit_button_in)
        ),
        HowToTopic(
            appString(R.string.common_sharing),
            appString(R.string.settings_how_to_share_from_the_viewer_s),
            now = if (s.sharingDisabled) {
                appString(R.string.settings_how_to_sharing_is_off_so_there)
            } else if (s.removeExifOnShare) {
                if (s.stripOptions.any) appString(R.string.settings_how_to_sharing_is_on_removing, removedNames)
                else appString(R.string.settings_how_to_sharing_is_on_nothing_selected)
            } else {
                appString(R.string.settings_how_to_sharing_is_on_photos_are)
            },
            links = listOf(
                HowToLink(appString(R.string.settings_how_to_sharing_enabled), "settings/exif", !s.sharingDisabled),
                HowToLink(appString(R.string.settings_how_to_remove_exif_data_when_sharing), "settings/exif", s.removeExifOnShare)
            ) + if (s.removeExifOnShare) listOf(
                HowToLink(appString(R.string.settings_exif_strip_location), "settings/exif", s.stripOptions.location),
                HowToLink(appString(R.string.settings_exif_strip_camera), "settings/exif", s.stripOptions.camera),
                HowToLink(appString(R.string.settings_exif_strip_date_time), "settings/exif", s.stripOptions.dateTime),
                HowToLink(appString(R.string.settings_exif_strip_description), "settings/exif", s.stripOptions.description)
            ) else emptyList()
        ),
        HowToTopic(
            appString(R.string.common_hidden_section),
            appString(R.string.settings_how_to_a_private_place_for_photos),
            now = if (!s.hiddenSectionEnabled) {
                appString(R.string.settings_how_to_the_hidden_section_is_off)
            } else {
                buildString {
                    append(appString(R.string.settings_how_to_the_hidden_section_is_on))
                    append(if (s.hiddenPinSet) appString(R.string.settings_how_to_an_extra_pin_is_asked) else appString(R.string.settings_how_to_no_extra_pin_is_set))
                    append(if (s.hiddenAlbumsEnabled) appString(R.string.settings_how_to_hidden_albums_are_shown) else appString(R.string.settings_how_to_hidden_albums_are_not_shown))
                    if (s.hiddenImageSharingEnabled && !s.sharingDisabled) {
                        append(appString(R.string.settings_how_to_hidden_items_can_be_shared))
                    } else if (s.sharingDisabled) {
                        append(appString(R.string.settings_how_to_hidden_items_cannot_be_shared))
                    } else {
                        append(appString(R.string.settings_how_to_hidden_items_cannot_be_shared_2))
                    }
                }
            },
            links = if (!s.hiddenSectionEnabled) {
                listOf(HowToLink(appString(R.string.common_hidden_section), "settings/hiddensection", false))
            } else {
                listOf(
                    HowToLink(appString(R.string.common_hidden_section), "settings/hiddensection", true),
                    HowToLink(appString(R.string.common_extra_pin), "settings/hiddensection/hidden_pin", s.hiddenPinSet),
                    HowToLink(appString(R.string.common_show_hidden_albums), "settings/hiddenalbums", s.hiddenAlbumsEnabled),
                    HowToLink(appString(R.string.settings_how_to_allow_sharing_hidden_images), "settings/hiddensharing", s.hiddenImageSharingEnabled)
                )
            }
        ),
        HowToTopic(
            appString(R.string.settings_how_to_people_pets_cars_motorcycles_and),
            appString(R.string.settings_how_to_these_run_on_your_device),
            now = if (!s.mlEnabled) {
                appString(R.string.settings_how_to_machine_learning_is_off_so)
            } else if (mlTags.isEmpty()) {
                appString(R.string.settings_how_to_machine_learning_is_on_but)
            } else {
                appString(R.string.settings_how_to_machine_learning_is_on_a, mlTags) +
                    if (duplicatesOn) "" else appString(R.string.settings_how_to_duplicates_is_off_so_it)
            },
            links = listOf(
                HowToLink(appString(R.string.common_machine_learning), "settings/machinelearning", s.mlEnabled),
                HowToLink(appString(R.string.settings_how_to_scan_finished_notification), "settings/notifications", s.scanCompleteNotificationEnabled)
            )
        ),
        HowToTopic(
            appString(R.string.common_privacy),
            appString(R.string.settings_how_to_app_lock_asks_for_your),
            links = listOf(
                HowToLink(appString(R.string.common_app_lock), "settings/security", s.appLockEnabled),
                HowToLink(appString(R.string.common_disable_screenshots), "settings/security", s.disableScreenshots)
            )
        )
    )
}

private fun howItWorksTopics(s: HowToState): List<HowToTopic> = listOf(
    HowToTopic(
        appString(R.string.settings_how_to_what_happens_when_you_hide),
        appString(R.string.settings_how_to_the_file_is_copied_into),
        now = if (s.hiddenSectionEnabled) null else appString(R.string.settings_how_to_hide_is_only_available_while)
    ),
    HowToTopic(
        appString(R.string.settings_how_to_what_happens_when_you_unhide),
        appString(R.string.settings_how_to_the_item_is_written_back)
    ),
    HowToTopic(
        appString(R.string.settings_how_to_what_happens_when_you_delete),
        appString(R.string.settings_how_to_the_file_stays_private_inside)
    ),
    HowToTopic(
        appString(R.string.settings_how_to_what_happens_when_you_delete_2),
        appString(R.string.settings_how_to_the_file_itself_is_not)
    ),
    HowToTopic(
        appString(R.string.settings_how_to_what_happens_when_you_edit),
        appString(R.string.settings_how_to_saving_an_edited_photo_creates)
    ),
    HowToTopic(
        appString(R.string.settings_how_to_what_duplicate_does),
        appString(R.string.settings_how_to_duplicate_in_the_viewer_s)
    ),
    HowToTopic(
        appString(R.string.settings_how_to_where_the_app_keeps_your),
        appString(R.string.settings_how_to_favorites_albums_recently_deleted_entr)
    ),
    HowToTopic(
        appString(R.string.settings_how_to_what_a_machine_learning_scan),
        appString(R.string.settings_how_to_a_scan_reads_each_photo)
    ),
    HowToTopic(
        appString(R.string.settings_how_to_what_happens_when_you_share),
        appString(R.string.settings_how_to_the_app_makes_a_temporary),
        now = if (s.sharingDisabled) {
            appString(R.string.settings_how_to_sharing_is_off_so_nothing)
        } else if (s.removeExifOnShare && s.stripOptions.any) {
            appString(R.string.settings_how_to_removed_before_sharing, stripNames(s.stripOptions))
        } else {
            appString(R.string.settings_how_to_exif_data_is_kept_when)
        }
    ),
    HowToTopic(
        appString(R.string.settings_how_to_how_the_hidden_section_is),
        appString(R.string.settings_how_to_opening_the_hidden_section_asks),
        now = if (s.hiddenPinSet) appString(R.string.settings_how_to_an_extra_pin_is_set) else appString(R.string.settings_how_to_no_extra_pin_is_set_2)
    ),
    HowToTopic(
        appString(R.string.common_hidden_photos_backup),
        appString(R.string.settings_how_to_hidden_files_are_deleted_when)
    )
)

// A group in the tree. A group with one topic shows that topic directly.
private class HowToGroup(val title: String, val topics: List<HowToTopic>)

private val sentenceSplit = Regex("(?<=[.!?])\\s+(?=[\\p{Lu}0-9])")

// Names of screens and buttons, taken from the app's own labels so they match the language in use
@Composable
private fun boldTerms(): List<String> = listOf(
    R.string.common_library, R.string.common_albums, R.string.common_favorites, R.string.common_recently_saved,
    R.string.common_recently_deleted, R.string.common_hidden_section, R.string.common_sharing,
    R.string.share_sheet_print, R.string.share_sheet_wallpaper, R.string.share_sheet_save_to_folder,
    R.string.share_sheet_export_original, R.string.share_sheet_copy,
    R.string.common_machine_learning, R.string.common_privacy, R.string.common_share, R.string.common_hide,
    R.string.common_select, R.string.common_add_to_album, R.string.common_app_lock, R.string.common_extra_pin,
    R.string.common_hidden_photos_backup, R.string.common_notifications, R.string.common_appearance,
    R.string.common_gallery, R.string.common_settings, R.string.common_videos
).map { appString(it) }.filter { it.length >= 4 }.distinct().sortedByDescending { it.length }

private fun styled(text: String, terms: List<String>): AnnotatedString {
    if (terms.isEmpty()) return AnnotatedString(text)
    val regex = Regex(terms.joinToString("|", "(?<![\\p{L}])(?:", ")(?![\\p{L}])") { Regex.escape(it) })
    return buildAnnotatedString {
        var last = 0
        for (match in regex.findAll(text)) {
            append(text.substring(last, match.range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.value) }
            last = match.range.last + 1
        }
        append(text.substring(last))
    }
}

// Numbered steps stay one line each, other text is cut into short paragraphs of two sentences
private fun paragraphs(text: String): List<String> =
    text.split("\n").flatMap { line ->
        if (line.firstOrNull()?.isDigit() == true) listOf(line) else line.split(sentenceSplit).chunked(2).map { it.joinToString(" ") }
    }.map { it.trim() }.filter { it.isNotEmpty() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHowToScreen(
    state: HowToState,
    onOpenSetting: (String) -> Unit,
    onBack: () -> Unit
) {
    val topics = remember(state) { howToTopics(state) }
    val howItWorks = remember(state) { howItWorksTopics(state) }
    // Same order as the Settings menu. Library, Albums and so on come first, they have no row in Settings.
    val browsingTitle = appString(R.string.settings_how_to_group_browsing)
    val howItWorksTitle = appString(R.string.settings_how_to_how_it_works)
    val groups = remember(topics, howItWorks, browsingTitle, howItWorksTitle) {
        listOf(
            HowToGroup(browsingTitle, topics.subList(0, 5)),
            HowToGroup(topics[8].title, listOf(topics[8])),
            HowToGroup(topics[5].title, listOf(topics[5])),
            HowToGroup(topics[7].title, listOf(topics[7])),
            HowToGroup(topics[6].title, listOf(topics[6])),
            HowToGroup(howItWorksTitle, howItWorks)
        )
    }
    // Everything starts closed
    val open = remember { mutableStateMapOf<String, Boolean>() }
    val terms = boldTerms()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_how_to_use), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            groups.forEach { group ->
                Surface(
                    color = GlassPill.adaptiveGlassBackground,
                    shape = RoundedCornerShape(20.dp),
                    border = GlassPill.adaptiveBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (group.topics.size == 1) {
                            val topic = group.topics.first()
                            val key = "g:${group.title}"
                            TreeRow(topic.title, open[key] == true, 18, 16) { open[key] = open[key] != true }
                            if (open[key] == true) TopicBody(topic, terms, onOpenSetting)
                        } else {
                            val groupKey = "g:${group.title}"
                            TreeRow(group.title, open[groupKey] == true, 18, 16) { open[groupKey] = open[groupKey] != true }
                            if (open[groupKey] == true) {
                                group.topics.forEach { topic ->
                                    val key = "t:${group.title}:${topic.title}"
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                    TreeRow(topic.title, open[key] == true, 15, 28) { open[key] = open[key] != true }
                                    if (open[key] == true) TopicBody(topic, terms, onOpenSetting)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TreeRow(title: String, expanded: Boolean, fontSize: Int, startPadding: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = startPadding.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null
        )
    }
}

@Composable
private fun TopicBody(topic: HowToTopic, terms: List<String>, onOpenSetting: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = 28.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        paragraphs(topic.text).forEach { paragraph -> Text(styled(paragraph, terms)) }
        if (topic.now != null) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    styled(appString(R.string.settings_how_to_right_now, topic.now), terms),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        topic.links.forEach { link ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(link.label, fontWeight = FontWeight.SemiBold)
                    if (link.on != null) {
                        Text(
                            if (link.on) appString(R.string.settings_how_to_on) else appString(R.string.settings_how_to_off),
                            color = if (link.on) MaterialTheme.colorScheme.primary else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(onClick = { onOpenSetting(link.route) }) { Text(appString(R.string.settings_how_to_open)) }
            }
        }
    }
}
