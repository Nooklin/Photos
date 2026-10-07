package org.nooklin.photos.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nooklin_photos_prefs")

// Delimited strings in DataStore, simple on purpose
private object Keys {
    val FAVORITE_IDS = stringSetPreferencesKey("favorite_ids")
    val RECYCLE_BIN_ENTRIES = stringSetPreferencesKey("recycle_bin_entries") // "id|uri|deletedAt"
    val SORT_DATES = stringSetPreferencesKey("sort_date_overrides") // "mediaId|dateAddedSeconds"
    val EDITED_COPIES = stringSetPreferencesKey("edited_copies") // mediaId of each copy saved by the editor
}

class LocalMetadataStore(private val context: Context) {

    data class RecycleBinEntry(
        val mediaId: Long,
        val uri: String,
        val deletedAt: Long,
        val wasHidden: Boolean = false,
        // Orientation-corrected, captured when moved to the recycle bin
        val width: Int? = null,
        val height: Int? = null
    )

    // Edited copies get their original's date
    val sortDates: Flow<Map<Long, Long>> = context.dataStore.data.map { prefs ->
        prefs[Keys.SORT_DATES]?.mapNotNull { raw ->
            val parts = raw.split("|")
            val id = parts.getOrNull(0)?.toLongOrNull()
            val date = parts.getOrNull(1)?.toLongOrNull()
            if (id != null && date != null) id to date else null
        }?.toMap() ?: emptyMap()
    }

    suspend fun addEditedCopy(mediaId: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EDITED_COPIES] = (prefs[Keys.EDITED_COPIES] ?: emptySet()) + mediaId.toString()
        }
    }

    suspend fun removeEditedCopy(mediaId: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EDITED_COPIES] = (prefs[Keys.EDITED_COPIES] ?: emptySet()) - mediaId.toString()
        }
    }

    suspend fun isEditedCopy(mediaId: Long): Boolean =
        context.dataStore.data.first()[Keys.EDITED_COPIES]?.contains(mediaId.toString()) == true

    suspend fun setSortDate(mediaId: Long, dateAddedSeconds: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SORT_DATES]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$mediaId|") }
            current.add("$mediaId|$dateAddedSeconds")
            prefs[Keys.SORT_DATES] = current
        }
    }

    val favoriteIds: Flow<Set<Long>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_IDS]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    val recycleBinEntries: Flow<List<RecycleBinEntry>> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECYCLE_BIN_ENTRIES]?.mapNotNull { raw ->
            val parts = raw.split("|")
            val id = parts.getOrNull(0)?.toLongOrNull()
            val uri = parts.getOrNull(1)
            val deletedAt = parts.getOrNull(2)?.toLongOrNull()
            // Older entries lack this field, default false
            val wasHidden = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: false
            // Older entries have no size, null
            val width = parts.getOrNull(4)?.toIntOrNull()
            val height = parts.getOrNull(5)?.toIntOrNull()
            if (id != null && uri != null && deletedAt != null) {
                RecycleBinEntry(id, uri, deletedAt, wasHidden, width, height)
            } else null
        } ?: emptyList()
    }

    suspend fun setFavorite(mediaId: Long, favorite: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_IDS]?.toMutableSet() ?: mutableSetOf()
            if (favorite) current.add(mediaId.toString()) else current.remove(mediaId.toString())
            prefs[Keys.FAVORITE_IDS] = current
        }
    }

    suspend fun moveToRecycleBin(
        mediaId: Long,
        uri: String,
        wasHidden: Boolean = false,
        width: Int? = null,
        height: Int? = null
    ) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECYCLE_BIN_ENTRIES]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$mediaId|") }
            current.add("$mediaId|$uri|${System.currentTimeMillis()}|$wasHidden|$width|$height")
            prefs[Keys.RECYCLE_BIN_ENTRIES] = current
        }
    }

    suspend fun restoreFromRecycleBin(mediaId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECYCLE_BIN_ENTRIES]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$mediaId|") }
            prefs[Keys.RECYCLE_BIN_ENTRIES] = current
        }
    }
}
