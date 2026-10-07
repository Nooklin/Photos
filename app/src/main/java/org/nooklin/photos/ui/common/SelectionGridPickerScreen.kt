package org.nooklin.photos.ui.common

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

// Shared by both pickers, identity is MediaItem.id or HiddenItem.file.name
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T, K : Any> SelectionGridPickerScreen(
    title: String,
    items: List<T>,
    key: (T) -> K,
    thumbnailModel: (T) -> Any,
    excludeKeys: Set<K>,
    gridColumns: Int,
    emptyMessage: String,
    onConfirm: (List<K>) -> Unit,
    onBack: () -> Unit
) {
    val pickable = remember(items, excludeKeys) { items.filter { key(it) !in excludeKeys } }
    var selectedKeys by remember { mutableStateOf(setOf<K>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        enabled = selectedKeys.isNotEmpty(),
                        onClick = { onConfirm(selectedKeys.toList()) }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Add ${selectedKeys.size} photo(s)")
                    }
                }
            )
        }
    ) { padding ->
        if (pickable.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(emptyMessage)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(pickable.size, key = { key(pickable[it]) }) { index ->
                    val item = pickable[index]
                    val itemKey = key(item)
                    val selected = itemKey in selectedKeys
                    Box {
                        AsyncImage(
                            model = thumbnailModel(item),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedKeys = if (selected) selectedKeys - itemKey else selectedKeys + itemKey
                                }
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
