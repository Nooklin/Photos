package org.nooklin.photos.data

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.nooklin.photos.data.local.AppPreferences

// The trim is stored, the file is untouched, reset brings back the whole video
object VideoTrims {
    data class Trim(val startMs: Long, val endMs: Long)

    private val trims = MutableStateFlow<Map<String, Trim>>(emptyMap())

    fun get(uri: Any): Trim? = trims.value[uri.toString()]

    suspend fun load(preferences: AppPreferences) {
        val parsed = HashMap<String, Trim>()
        for (entry in preferences.videoTrims.first()) {
            val parts = entry.split("|")
            val start = parts.getOrNull(1)?.toLongOrNull() ?: continue
            val end = parts.getOrNull(2)?.toLongOrNull() ?: continue
            parsed[parts[0]] = Trim(start, end)
        }
        trims.value = parsed
    }

    suspend fun set(preferences: AppPreferences, uri: Uri, trim: Trim?) {
        val updated = trims.value.toMutableMap()
        if (trim == null) updated.remove(uri.toString()) else updated[uri.toString()] = trim
        trims.value = updated
        preferences.setVideoTrims(updated.map { "${it.key}|${it.value.startMs}|${it.value.endMs}" }.toSet())
    }
}
