package org.nooklin.photos.ui.pets

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nooklin.photos.ui.common.AlbumThumbnail
import org.nooklin.photos.ui.common.CircleIconButton
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GlassPillTextField
import org.nooklin.photos.R
import org.nooklin.photos.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetsScreen(
    pets: List<PetWithPreview>,
    gridColumns: Int,
    onPetClick: (Long) -> Unit,
    onCreatePet: (String) -> Unit,
    onBack: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.pets_pets), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    CircleIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = appString(R.string.pets_new_pet),
                        onClick = { showCreateDialog = true }
                    )
                }
            )
        }
    ) { padding ->
        if (pets.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(appString(R.string.pets_no_pets_named_yet_tap), modifier = Modifier.padding(32.dp))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(pets, key = { it.id }) { pet ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPetClick(pet.id) }
                    ) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            AlbumThumbnail(pet.previewItems, size = maxWidth)
                        }
                        Text(
                            pet.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            appString(R.string.common_items, pet.itemCount),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var newPetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.pets_new_pet_2)) },
            text = {
                GlassPillTextField(
                    value = newPetName,
                    onValueChange = { newPetName = it },
                    placeholder = appString(R.string.pets_pet_name)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newPetName.isNotBlank(),
                    onClick = {
                        onCreatePet(newPetName)
                        showCreateDialog = false
                    }
                ) {
                    Text(appString(R.string.common_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(appString(R.string.common_cancel)) }
            }
        )
    }
}
