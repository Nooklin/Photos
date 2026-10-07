package org.nooklin.photos.ml

import org.nooklin.photos.data.local.MlTag

// COCO index to MlTag, classes without a tag are ignored
object CocoClass {
    const val PERSON = 0
    const val CAR = 2
    const val MOTORCYCLE = 3
    // Indices follow the 91-line label file minus 1, gaps kept (not 0-79).
    // CAT and DOG sit after the first gap.
    const val CAT = 16
    const val DOG = 17

    private val TAG_BY_CLASS: Map<Int, MlTag> = mapOf(
        CAR to MlTag.CARS,
        MOTORCYCLE to MlTag.MOTORCYCLES,
        CAT to MlTag.PETS,
        DOG to MlTag.PETS
    )

    fun tagFor(cocoClassIndex: Int): MlTag? = TAG_BY_CLASS[cocoClassIndex]
}
