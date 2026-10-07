package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nooklin.photos.data.local.MlTag
import org.nooklin.photos.ml.PhotoAnalysisScanner
import org.nooklin.photos.ui.common.ConfirmDeleteDialog

// Content scrolls: with all tag rows it overflows a typical screen
private fun iconFor(tag: MlTag): ImageVector = when (tag) {
    MlTag.PEOPLE -> Icons.Filled.Person
    MlTag.PETS -> Icons.Filled.Pets
    MlTag.CARS -> Icons.Filled.DirectionsCar
    MlTag.MOTORCYCLES -> Icons.Filled.TwoWheeler
    MlTag.DUPLICATES -> Icons.Filled.ContentCopy
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMachineLearningScreen(
    mlEnabled: Boolean,
    onMlEnabledChange: (Boolean) -> Unit,
    enabledTagIds: Set<String>,
    onTagToggle: (MlTag, Boolean) -> Unit,
    scanProgress: PhotoAnalysisScanner.ScanProgress?,
    onScanLibrary: () -> Unit,
    onClearAnalysisData: () -> Unit,
    onBack: () -> Unit
) {
    val scanning = scanProgress != null && !scanProgress.done
    val hasScannableTag = enabledTagIds.any { id -> MlTag.fromId(id) != null }
    var showDisableConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Machine Learning", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsGroup(
                "Machine Learning",
                listOf({
                    SettingsToggleRow(
                        label = "Use machine learning",
                        checked = mlEnabled,
                        icon = Icons.Filled.Psychology,
                        onCheckedChange = { turningOn ->
                            if (!turningOn && mlEnabled) {
                                // Confirm first, disabling deletes the analysis data
                                showDisableConfirm = true
                            } else {
                                onMlEnabledChange(turningOn)
                            }
                        }
                    )
                })
            )
            Text(
                "Machine learning takes place entirely on your device. No data is ever shared.",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            if (mlEnabled) {
                SettingsGroup(
                    "Tags to scan",
                    MlTag.entries.filter { it != MlTag.PETS }.map { tag ->
                        {
                            Column {
                                SettingsToggleRow(
                                    label = tag.label,
                                    checked = tag.id in enabledTagIds,
                                    icon = iconFor(tag),
                                    onCheckedChange = { checked -> onTagToggle(tag, checked) }
                                )
                            }
                        }
                    }
                )

                SettingsSectionTitle("Scan")
                Text(
                    "Scans your library on this device only, checking each photo for the tags turned on above. Already-analyzed photos are skipped unless they've changed.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                Button(
                    onClick = onScanLibrary,
                    enabled = hasScannableTag && !scanning,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text(if (scanning) "Scanning..." else "Scan Library")
                }
                if (!hasScannableTag) {
                    Text(
                        "Turn on at least one tag above to scan.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    )
                }
                if (scanProgress != null && scanning && scanProgress.total > 0) {
                    LinearProgressIndicator(
                        progress = { scanProgress.processed / scanProgress.total.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    Text(
                        "${scanProgress.processed} / ${scanProgress.total}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                if (scanProgress != null && scanProgress.done && scanProgress.total > 0) {
                    Text(
                        "Scan complete.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                }

                SettingsSectionTitle("Reset")
                Text(
                    "Clears everything detected so far on this device. Use this if a previous scan tagged things incorrectly, then run Scan Library again.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    enabled = !scanning,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text("Clear analysis data")
                }
            }

            Column(Modifier.height(48.dp).fillMaxWidth()) {}
        }
    }

    if (showDisableConfirm) {
        ConfirmDeleteDialog(
            title = "Turn off machine learning?",
            text = "Everything detected so far will be permanently deleted from this device, " +
                "not just hidden. You'll need to scan again if you turn this back on.",
            confirmLabel = "Turn Off",
            onConfirm = {
                showDisableConfirm = false
                // Delete before the toggle flips, so ML never reads "off" while stale tags remain
                onClearAnalysisData()
                onMlEnabledChange(false)
            },
            onDismiss = { showDisableConfirm = false }
        )
    }

    if (showClearConfirm) {
        ConfirmDeleteDialog(
            title = "Clear analysis data?",
            text = "Everything detected so far will be permanently deleted from this device. You'll need to scan again afterward.",
            confirmLabel = "Clear",
            onConfirm = {
                showClearConfirm = false
                onClearAnalysisData()
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}
