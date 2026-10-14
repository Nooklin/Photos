package org.nooklin.photos.ui.duplicates

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.MediaCategory
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GridThumbnail

// The photo to keep by default: most pixels, then the biggest file
private fun bestOf(group: List<MediaItem>): MediaItem =
    group.maxWith(
        compareBy<MediaItem>({ (it.width ?: 0).toLong() * (it.height ?: 0) }, { it.sizeBytes ?: 0L })
    )

// Review of photos that look the same. Per group one photo is kept, the others go to Recently Deleted.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    groups: List<List<MediaItem>>,
    onBack: () -> Unit,
    onOpenGroup: (group: List<MediaItem>, startIndex: Int) -> Unit,
    onMoveToRecycleBin: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    // The photo chosen to keep, per group (key is the smallest id of the group)
    val keepChoice = remember { mutableStateMapOf<Long, Long>() }
    var confirmAll by remember { mutableStateOf(false) }

    fun keepOf(group: List<MediaItem>): MediaItem =
        group.find { it.id == keepChoice[group.minOf { item -> item.id }] } ?: bestOf(group)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(MediaCategory.DUPLICATES.label, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    if (groups.isNotEmpty()) {
                        TextButton(onClick = { confirmAll = true }) { Text(appString(R.string.duplicates_clean_all)) }
                    }
                }
            )
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(appString(R.string.category_nothing_here_yet))
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(groups, key = { group -> group.minOf { it.id } }) { group ->
                val groupKey = group.minOf { it.id }
                val keep = keepOf(group)
                val best = bestOf(group)
                Surface(
                    color = GlassPill.adaptiveGlassBackground,
                    shape = RoundedCornerShape(20.dp),
                    border = GlassPill.adaptiveBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            appString(R.string.duplicates_group_count, group.size),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(group, key = { it.id }) { item ->
                                Column(Modifier.size(width = 132.dp, height = 172.dp)) {
                                    Box(
                                        Modifier
                                            .size(132.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onOpenGroup(group, group.indexOf(item)) }
                                    ) {
                                        GridThumbnail(uri = item.uri, dateModified = item.dateModified, modifier = Modifier.fillMaxSize())
                                        val isKept = item.id == keep.id
                                        Icon(
                                            if (isKept) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isKept) MaterialTheme.colorScheme.primary else Color.White,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                                                .clickable { keepChoice[groupKey] = item.id }
                                                .padding(2.dp)
                                        )
                                        if (item.id == best.id) {
                                            Text(
                                                appString(R.string.duplicates_best),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    val sizeText = item.sizeBytes?.let { Formatter.formatShortFileSize(context, it) }
                                    val resolution = if (item.width != null && item.height != null) "${item.width} x ${item.height}" else null
                                    Text(
                                        listOfNotNull(resolution, sizeText).joinToString(" · "),
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = { group.filter { it.id != keep.id }.forEach(onMoveToRecycleBin) },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(appString(R.string.duplicates_keep_selected, group.size - 1), color = GlassPill.destructiveText)
                        }
                    }
                }
            }
        }
    }

    if (confirmAll) {
        val removeCount = groups.sumOf { it.size - 1 }
        ConfirmDeleteDialog(
            title = appString(R.string.duplicates_confirm_title),
            text = appString(R.string.duplicates_confirm_text, removeCount, groups.size),
            confirmLabel = appString(R.string.duplicates_clean_all),
            onConfirm = {
                confirmAll = false
                groups.forEach { group ->
                    val keep = keepOf(group)
                    group.filter { it.id != keep.id }.forEach(onMoveToRecycleBin)
                }
            },
            onDismiss = { confirmAll = false }
        )
    }
}
