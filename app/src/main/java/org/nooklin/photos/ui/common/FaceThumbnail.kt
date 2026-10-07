package org.nooklin.photos.ui.common

import android.net.Uri
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size

@Composable
fun FaceThumbnail(
    photoUri: Uri,
    boxXMin: Float,
    boxYMin: Float,
    boxXMax: Float,
    boxYMax: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(photoUri)
            // Original size: skips the grid's own thumbnail decode (the face was black with it) and gives the crop real detail.
            .size(Size.ORIGINAL)
            .transformations(FaceCropTransformation(boxXMin, boxYMin, boxXMax, boxYMax))
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CircleShape)
    )
}
