package org.nooklin.photos.ui.discovered

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.nooklin.photos.data.AnalysisRepository
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.local.PhotoTagEntity
import org.nooklin.photos.ui.common.throttleLatest

class DiscoveredViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnalysisRepository(application)

    val tags: StateFlow<List<PhotoTagEntity>> = repository.allTags
        .throttleLatest(500)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearCategories(tagIds: Set<String>) {
        viewModelScope.launch { repository.clearCategories(tagIds) }
    }
}

// Computed smart album, one per tag in photo_tags. Not an AlbumWithPreview: no database row, tagId identifies it.
data class DiscoveredAlbum(
    val tagId: String,
    val label: String,
    val itemCount: Int,
    val previewItems: List<MediaItem>
)
