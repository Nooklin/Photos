package org.nooklin.photos.data.local

// What is removed from a photo or video when sharing, while Remove Exif data is on (Settings, Sharing)
enum class StripCategory { LOCATION, CAMERA, DATE_TIME, DESCRIPTION }

data class ShareStripOptions(
    val location: Boolean = true,
    val camera: Boolean = true,
    val dateTime: Boolean = true,
    val description: Boolean = true
) {
    val any: Boolean get() = location || camera || dateTime || description
    val all: Boolean get() = location && camera && dateTime && description

    fun isOn(category: StripCategory): Boolean = when (category) {
        StripCategory.LOCATION -> location
        StripCategory.CAMERA -> camera
        StripCategory.DATE_TIME -> dateTime
        StripCategory.DESCRIPTION -> description
    }
}
