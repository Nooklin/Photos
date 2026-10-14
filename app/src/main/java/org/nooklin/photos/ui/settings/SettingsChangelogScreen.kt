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

private class ChangelogCategory(@param:StringRes val title: Int, val items: List<Int>)

// What is new in 1.1.0, grouped by the part of the app
private val changelog110 = listOf(
    ChangelogCategory(R.string.changelog_cat_library, listOf(R.string.changelog_110_1, R.string.changelog_110_2)),
    ChangelogCategory(R.string.changelog_cat_selecting, listOf(R.string.changelog_110_4, R.string.changelog_110_7)),
    ChangelogCategory(R.string.common_sharing, listOf(R.string.changelog_110_5, R.string.changelog_110_6)),
    ChangelogCategory(R.string.common_machine_learning, listOf(R.string.changelog_110_3)),
    ChangelogCategory(R.string.common_hidden_section, listOf(R.string.changelog_110_11)),
    ChangelogCategory(
        R.string.changelog_cat_general,
        listOf(
            R.string.changelog_110_8, R.string.changelog_110_9, R.string.changelog_110_10,
            R.string.changelog_110_13, R.string.changelog_110_12
        )
    )
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
            changelog110.forEach { category ->
                SettingsGroup(
                    appString(category.title),
                    category.items.map { id ->
                        @Composable {
                            Text(
                                appString(id),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
