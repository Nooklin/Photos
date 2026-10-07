package org.nooklin.photos.ui.people

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.nooklin.photos.data.PersonRepository
import org.nooklin.photos.data.local.FaceSummary
import org.nooklin.photos.ui.common.throttleLatest
import org.nooklin.photos.data.local.PersonEntity

class PeopleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PersonRepository(application)

    val people: StateFlow<List<PersonEntity>> = repository.allPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val faces: StateFlow<List<FaceSummary>> = repository.allFaces
        .throttleLatest(500)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun rename(personId: Long, name: String?) {
        viewModelScope.launch { repository.rename(personId, name) }
    }

    fun merge(intoPersonId: Long, fromPersonId: Long) {
        viewModelScope.launch { repository.merge(intoPersonId, fromPersonId) }
    }

    fun splitOutFaces(faceIds: List<Long>) {
        viewModelScope.launch { repository.splitOut(faceIds) }
    }

    /** How many people would be combined at this threshold, without changing anything. */
    suspend fun previewCombine(threshold: Float): Int = repository.combineSimilarPeople(threshold, apply = false)

    fun combineSimilar(threshold: Float, onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(repository.combineSimilarPeople(threshold, apply = true)) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}

data class PersonWithPreview(
    val id: Long,
    val displayName: String,
    val isNamed: Boolean,
    val itemCount: Int,
    val representativePhotoUri: Uri,
    val boxXMin: Float,
    val boxYMin: Float,
    val boxXMax: Float,
    val boxYMax: Float
)

data class PersonPickerEntry(val id: Long, val label: String)
