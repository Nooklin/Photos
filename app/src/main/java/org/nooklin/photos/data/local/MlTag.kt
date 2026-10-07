package org.nooklin.photos.data.local

// id is written to photo_tags.tag, never change it once scanned
enum class MlTag(val id: String, val label: String) {
    PEOPLE("person", "People"),
    PETS("pet", "Pets"),
    CARS("car", "Cars"),
    MOTORCYCLES("motorcycle", "Motorcycles"),
    DUPLICATES("duplicate", "Duplicates");

    companion object {
        fun fromId(id: String): MlTag? = entries.find { it.id == id }
    }
}
