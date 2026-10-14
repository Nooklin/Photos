package org.nooklin.photos.ui.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nooklin.photos.data.PhotoMetadata
import org.nooklin.photos.data.formatFileSize
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Only fields with real data: no lens name (Apple infers it), no geocoded location (needs network)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoInfoSheet(metadata: PhotoMetadata, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                appString(R.string.photo_info_sheet_u, metadata.weekdayDateLabel, metadata.timeLabel),
                fontWeight = FontWeight.Medium
            )
            Text(metadata.name, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 2.dp) {
                Column {
                    val deviceLabel = listOfNotNull(metadata.cameraMake, metadata.cameraModel)
                        .joinToString(" ")
                        .ifBlank { null }

                    if (deviceLabel != null || metadata.format != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                deviceLabel ?: appString(R.string.photo_info_sheet_unknown_device),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            metadata.format?.let { format ->
                                AssistChip(onClick = {}, label = { Text(format) })
                            }
                        }
                        HorizontalDivider()
                    }

                    val cameraLine = listOfNotNull(metadata.focalLength, metadata.aperture)
                        .joinToString(" ")
                        .ifBlank { null }
                    if (cameraLine != null) {
                        Text(
                            cameraLine,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    val detailsLine = listOfNotNull(
                        metadata.megapixels,
                        metadata.dimensions,
                        formatFileSize(metadata.sizeBytes)
                    ).joinToString(" \u2022 ")
                    Text(
                        detailsLine,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    val technicalLine = listOfNotNull(
                        metadata.iso,
                        metadata.shutterSpeed
                    ).joinToString("   \u2502   ")
                    if (technicalLine.isNotBlank()) {
                        HorizontalDivider()
                        Text(
                            technicalLine,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            metadata.gpsCoordinates?.let { coords ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 20.dp)
                ) {
                    Text(coords, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
