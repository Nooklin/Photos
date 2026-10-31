package org.nooklin.photos.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.nooklin.photos.data.ImportEntry
import org.nooklin.photos.data.ImportProgress
import org.nooklin.photos.data.ImportResult
import org.nooklin.photos.data.ImportSource
import org.nooklin.photos.data.LibraryImportSink
import org.nooklin.photos.data.exportLibraryZip
import org.nooklin.photos.data.runImport
import org.nooklin.photos.data.scanImportFolder
import org.nooklin.photos.data.scanImportZip
import org.nooklin.photos.ui.hidden.HiddenExportProgress

// Backup (zip) and import of the normal library
class LibraryTransferViewModel(application: Application) : AndroidViewModel(application) {
    private val _exportProgress = MutableStateFlow<HiddenExportProgress?>(null)
    val exportProgress: StateFlow<HiddenExportProgress?> = _exportProgress

    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress
    private var importJob: Job? = null

    fun exportZip(destination: Uri, organized: Boolean, onResult: (Boolean) -> Unit) {
        _exportProgress.value = HiddenExportProgress(0, 0)
        viewModelScope.launch {
            val success = exportLibraryZip(getApplication(), destination, organized) { done, total ->
                _exportProgress.value = HiddenExportProgress(done, total)
            }
            _exportProgress.value = null
            onResult(success)
        }
    }

    fun scanImport(source: ImportSource, onResult: (List<ImportEntry>?, Set<String>) -> Unit) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            val entries = try {
                if (source.isZip) scanImportZip(getApplication(), source.uri) else scanImportFolder(getApplication(), source.uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            onResult(entries, LibraryImportSink(getApplication()).topLevelAlbumNames())
        }
    }

    fun startImport(
        source: ImportSource,
        entries: List<ImportEntry>,
        merge: Boolean,
        deleteAfter: Boolean,
        onResult: (ImportResult?) -> Unit
    ) {
        _importProgress.value = ImportProgress(true, 0, 0)
        importJob = viewModelScope.launch {
            val result = try {
                runImport(getApplication(), LibraryImportSink(getApplication()), source, entries, merge, deleteAfter) {
                    _importProgress.value = it
                }
            } catch (e: CancellationException) {
                null
            } catch (e: Exception) {
                null
            }
            _importProgress.value = null
            onResult(result)
        }
    }

    // Files that were imported before Cancel stay
    fun cancelImport() {
        importJob?.cancel()
    }
}
