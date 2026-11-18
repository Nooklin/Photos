package org.nooklin.photos.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.ui.theme.Spacing

private class ChangelogCategory(@param:StringRes val title: Int, val items: List<Int>)

private val changelog136 = listOf(
    ChangelogCategory(R.string.common_hidden_section, listOf(R.string.changelog_136_1)),
    ChangelogCategory(R.string.changelog_cat_library, listOf(R.string.changelog_136_2, R.string.changelog_136_3, R.string.changelog_136_4, R.string.changelog_136_5, R.string.changelog_136_6, R.string.changelog_136_7, R.string.changelog_136_8))
)

private val changelog134 = listOf(
    ChangelogCategory(R.string.changelog_cat_albums, listOf(R.string.changelog_134_1, R.string.changelog_134_2)),
    ChangelogCategory(R.string.common_hidden_section, listOf(R.string.changelog_134_3, R.string.changelog_134_4, R.string.changelog_134_5, R.string.changelog_134_6, R.string.changelog_134_7, R.string.changelog_134_8, R.string.changelog_134_9)),
    ChangelogCategory(R.string.changelog_cat_library, listOf(R.string.changelog_134_10, R.string.changelog_134_11))
)

private val changelog133 = listOf(
    ChangelogCategory(R.string.changelog_cat_albums, listOf(R.string.changelog_133_1)),
    ChangelogCategory(R.string.common_hidden_section, listOf(R.string.changelog_133_2))
)

private val changelog132 = listOf(
    ChangelogCategory(R.string.changelog_cat_albums, listOf(R.string.changelog_132_1)),
    ChangelogCategory(R.string.changelog_cat_home, listOf(R.string.changelog_132_2)),
    ChangelogCategory(R.string.changelog_cat_general, listOf(R.string.changelog_132_5)),
    ChangelogCategory(R.string.common_hidden_section, listOf(R.string.changelog_132_3, R.string.changelog_132_4))
)

private val changelog131 = listOf(
    ChangelogCategory(R.string.changelog_cat_home, listOf(R.string.changelog_131_5, R.string.changelog_131_6)),
    ChangelogCategory(R.string.changelog_cat_folders, listOf(R.string.changelog_131_1)),
    ChangelogCategory(R.string.changelog_cat_swipe, listOf(R.string.changelog_131_2)),
    ChangelogCategory(R.string.common_hidden_section, listOf(R.string.changelog_131_3)),
    ChangelogCategory(R.string.changelog_cat_recent, listOf(R.string.changelog_131_4))
)

private val changelog130 = listOf(
    ChangelogCategory(R.string.changelog_cat_home, listOf(R.string.changelog_130_1)),
    ChangelogCategory(R.string.changelog_cat_panoramas, listOf(R.string.changelog_130_2, R.string.changelog_130_12)),
    ChangelogCategory(R.string.changelog_cat_auto_albums, listOf(R.string.changelog_130_3)),
    ChangelogCategory(R.string.changelog_cat_duplicate, listOf(R.string.changelog_130_4)),
    ChangelogCategory(R.string.changelog_cat_longpress, listOf(R.string.changelog_130_5, R.string.changelog_130_8)),
    ChangelogCategory(R.string.changelog_cat_filter, listOf(R.string.changelog_130_6)),
    ChangelogCategory(R.string.common_sharing, listOf(R.string.changelog_130_7)),
    ChangelogCategory(R.string.common_hidden_section, listOf(R.string.changelog_130_9, R.string.changelog_130_10, R.string.changelog_130_13, R.string.changelog_130_14)),
    ChangelogCategory(R.string.changelog_cat_appearance, listOf(R.string.changelog_130_11)),
    ChangelogCategory(R.string.changelog_cat_privacy, listOf(R.string.changelog_130_15, R.string.changelog_130_18)),
    ChangelogCategory(R.string.changelog_cat_smooth, listOf(R.string.changelog_130_16, R.string.changelog_130_17))
)

private val changelog120 = listOf(
    ChangelogCategory(R.string.changelog_cat_video_editor, listOf(R.string.changelog_120_1, R.string.changelog_120_2, R.string.changelog_120_3, R.string.changelog_120_25, R.string.changelog_120_24, R.string.changelog_120_23, R.string.changelog_120_22, R.string.changelog_120_21)),
    ChangelogCategory(R.string.changelog_cat_photo_editor, listOf(R.string.changelog_120_4, R.string.changelog_120_26, R.string.changelog_120_30)),
    ChangelogCategory(R.string.changelog_cat_other_apps, listOf(R.string.changelog_120_5)),
    ChangelogCategory(R.string.changelog_cat_albums, listOf(R.string.changelog_120_6, R.string.changelog_120_7, R.string.changelog_120_8, R.string.changelog_120_10, R.string.changelog_120_15)),
    ChangelogCategory(R.string.changelog_cat_selecting, listOf(R.string.changelog_120_11, R.string.changelog_120_32)),
    ChangelogCategory(R.string.changelog_cat_backup, listOf(R.string.changelog_120_12, R.string.changelog_120_13, R.string.changelog_120_18, R.string.changelog_120_9)),
    ChangelogCategory(R.string.common_hidden_section, listOf(R.string.changelog_120_14, R.string.changelog_120_19, R.string.changelog_120_20)),
    ChangelogCategory(R.string.changelog_cat_longpress, listOf(R.string.changelog_120_28)),
    ChangelogCategory(R.string.changelog_cat_library, listOf(R.string.changelog_120_29)),
    ChangelogCategory(R.string.changelog_cat_viewer, listOf(R.string.changelog_120_33)),
    ChangelogCategory(R.string.changelog_cat_settings_design, listOf(R.string.changelog_120_16, R.string.changelog_120_17, R.string.changelog_120_27, R.string.changelog_120_31))
)

private val changelog110 = listOf(
    ChangelogCategory(R.string.changelog_cat_library, listOf(R.string.changelog_110_1, R.string.changelog_110_2)),
    ChangelogCategory(R.string.common_machine_learning, listOf(R.string.changelog_110_3)),
    ChangelogCategory(R.string.changelog_cat_selecting, listOf(R.string.changelog_110_4, R.string.changelog_110_7)),
    ChangelogCategory(R.string.common_sharing, listOf(R.string.changelog_110_5, R.string.changelog_110_6)),
    ChangelogCategory(R.string.common_hidden_section, listOf(R.string.changelog_110_11)),
    ChangelogCategory(R.string.changelog_cat_other_apps, listOf(R.string.changelog_110_8)),
    ChangelogCategory(R.string.changelog_cat_general, listOf(R.string.changelog_110_9, R.string.changelog_110_10, R.string.changelog_110_13, R.string.changelog_110_12))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsChangelogScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.changelog_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            listOf(
                R.string.changelog_version_136 to changelog136,
                R.string.changelog_version_134 to changelog134,
                R.string.changelog_version_133 to changelog133,
                R.string.changelog_version_132 to changelog132,
                R.string.changelog_version_131 to changelog131,
                R.string.changelog_version_130 to changelog130,
                R.string.changelog_version_120 to changelog120,
                R.string.changelog_version_110 to changelog110
            ).forEach { (versionTitle, categories) ->
                Text(
                    appString(versionTitle),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                categories.forEach { category ->
                    SettingsGroup(
                        appString(category.title),
                        category.items.map { id ->
                            @Composable {
                                Text(
                                    appString(id),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = Spacing.Md)
                                )
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
