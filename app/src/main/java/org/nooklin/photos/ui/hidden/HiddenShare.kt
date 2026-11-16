package org.nooklin.photos.ui.hidden

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.createExifStrippedShareUriFromFile
import org.nooklin.photos.ui.common.SharePreparingDialog

// Shares hidden photos and videos from their private files, with the Cancel dialog while they are prepared.
// A file that cannot be prepared is skipped, never shared with its metadata.
@Composable
fun rememberHiddenShare(removeExifOnShare: Boolean): (List<HiddenItem>, () -> Unit) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    if (job != null) {
        SharePreparingDialog(onCancel = {
            job?.cancel()
            job = null
        })
    }
    return { chosen, onDone ->
        job = scope.launch {
            val prepared = withContext(Dispatchers.IO) {
                val running = coroutineContext[Job]
                chosen.mapNotNull { item ->
                    createExifStrippedShareUriFromFile(context, item.file, item.isVideo, removeExifOnShare, item.mimeType) { running?.isActive == false }
                        ?.let { item to it }
                }
            }
            job = null
            val skipped = chosen.size - prepared.size
            if (skipped > 0) Toast.makeText(context, appString(R.string.share_sheet_skipped, skipped), Toast.LENGTH_SHORT).show()
            if (prepared.isEmpty()) return@launch
            val uris = prepared.map { it.second }
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            intent.type = when {
                prepared.all { it.first.isVideo } -> "video/*"
                prepared.none { it.first.isVideo } -> "image/*"
                else -> "*/*"
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, null))
            onDone()
        }
    }
}
