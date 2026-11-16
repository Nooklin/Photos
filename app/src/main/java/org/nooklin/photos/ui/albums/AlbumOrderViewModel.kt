package org.nooklin.photos.ui.albums

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.nooklin.photos.data.local.AlbumOrder
import org.nooklin.photos.data.local.AppPreferences

class AlbumOrderViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)

    val library: StateFlow<AlbumOrder> =
        preferences.albumOrder(false).stateIn(viewModelScope, SharingStarted.Eagerly, AlbumOrder())

    val hidden: StateFlow<AlbumOrder> =
        preferences.albumOrder(true).stateIn(viewModelScope, SharingStarted.Eagerly, AlbumOrder())

    val hiddenPreviewsOff: StateFlow<Boolean> =
        preferences.hiddenAlbumPreviewsOff.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun update(hidden: Boolean, change: (AlbumOrder) -> AlbumOrder) {
        viewModelScope.launch { preferences.updateAlbumOrder(hidden, change) }
    }

    fun setHiddenPreviewsOff(off: Boolean) {
        viewModelScope.launch { preferences.setHiddenAlbumPreviewsOff(off) }
    }
}
