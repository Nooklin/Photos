package org.nooklin.photos.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.ui.albums.AlbumOrderViewModel

@Composable
fun AlbumHomeSettings(hidden: Boolean, extra: List<@Composable () -> Unit> = emptyList()) {
    val vm: AlbumOrderViewModel = viewModel()
    val order by (if (hidden) vm.hidden else vm.library).collectAsStateWithLifecycle()
    SettingsGroup(
        null,
        listOf<@Composable () -> Unit>(
            {
                SettingsChoiceRow(
                    appString(R.string.album_home_rows),
                    listOf(1 to appString(R.string.album_rows_one), 2 to appString(R.string.album_rows_two)),
                    order.homeRows
                ) { rows -> vm.update(hidden) { it.copy(homeRows = rows) } }
            }
        ) + extra
    )
}
