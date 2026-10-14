package org.nooklin.photos.data.local

import androidx.annotation.StringRes
import org.nooklin.photos.R
import org.nooklin.photos.appString

// id is written to photo_tags.tag, never change it once scanned
enum class MlTag(val id: String, @StringRes val labelRes: Int) {
    PEOPLE("person", R.string.people_people),
    PETS("pet", R.string.pets_pets),
    CARS("car", R.string.ml_tag_cars),
    MOTORCYCLES("motorcycle", R.string.ml_tag_motorcycles),
    DUPLICATES("duplicate", R.string.media_category_duplicates);
    val label: String get() = appString(labelRes)
    companion object {
        fun fromId(id: String): MlTag? = entries.find { it.id == id }
    }
}
