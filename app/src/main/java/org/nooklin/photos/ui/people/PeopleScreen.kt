package org.nooklin.photos.ui.people

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nooklin.photos.ui.common.FaceThumbnail
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GlassPillTextField
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect

// Grid of detected people (named and "Unknown N"), each card shows a cropped face
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    people: List<PersonWithPreview>,
    gridColumns: Int,
    onPersonClick: (Long) -> Unit,
    onAddName: (personId: Long, name: String) -> Unit,
    onPreviewCombine: suspend (Float) -> Int,
    onCombine: (Float, (Int) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var namingPersonId by remember { mutableStateOf<Long?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showCombineDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("People", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = GlassPill.shape,
                        containerColor = GlassPill.adaptivePopupBackground
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                            DropdownMenuItem(
                                text = { Text("Combine similar people") },
                                onClick = {
                                    menuExpanded = false
                                    showCombineDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (people.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No people detected yet. Run a scan from Settings > Machine Learning.",
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(people, key = { it.id }) { person ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPersonClick(person.id) }
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                            FaceThumbnail(
                                photoUri = person.representativePhotoUri,
                                boxXMin = person.boxXMin,
                                boxYMin = person.boxYMin,
                                boxXMax = person.boxXMax,
                                boxYMax = person.boxYMax,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (!person.isNamed) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { namingPersonId = person.id },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add name",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            person.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            "${person.itemCount} items",
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

    val namingPerson = people.find { it.id == namingPersonId }
    if (namingPerson != null) {
        var name by remember(namingPerson.id) { mutableStateOf("") }
        val existingNames = remember(people) {
            people.filter { it.isNamed }.map { it.displayName }.distinct().sortedBy { it.lowercase() }
        }
        AlertDialog(
            onDismissRequest = { namingPersonId = null },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Who is this?") },
            text = {
                Column {
                    GlassPillTextField(
                        value = name,
                        onValueChange = { name = it }
                    )
                    if (existingNames.isNotEmpty()) {
                        Text(
                            "Or pick an existing person",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                            existingNames.forEach { existingName ->
                                Text(
                                    existingName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { name = existingName }
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onAddName(namingPerson.id, name)
                        namingPersonId = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { namingPersonId = null }) { Text("Cancel") }
            }
        )
    }

    if (showCombineDialog) {
        // The same person can be split into several when their faces differ in angle or light. Each setting shows how
        // many people it would combine, so the choice can be made before anything changes.
        val options = listOf("Careful" to 0.6f, "Balanced" to 0.75f, "Loose" to 0.9f)
        var counts by remember { mutableStateOf<Map<Float, Int>>(emptyMap()) }
        LaunchedEffect(Unit) {
            counts = options.associate { (_, threshold) -> threshold to onPreviewCombine(threshold) }
        }
        AlertDialog(
            onDismissRequest = { showCombineDialog = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            title = { Text("Combine similar people") },
            text = {
                Column {
                    Text("Pick how strict to be. Looser settings combine more, and can join different people who look alike.")
                    options.forEach { (label, threshold) ->
                        val count = counts[threshold]
                        TextButton(
                            enabled = count != null && count > 0,
                            onClick = {
                                showCombineDialog = false
                                onCombine(threshold) { combined ->
                                    Toast.makeText(context, "Combined $combined people.", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Text(
                                when {
                                    count == null -> "$label: checking..."
                                    count == 0 -> "$label: nothing to combine"
                                    else -> "$label: combine $count"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCombineDialog = false }) { Text("Cancel") } }
        )
    }
}
