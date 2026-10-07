package org.nooklin.photos.ui.editor

// ColorMatrix filters (4x5), the same array drives the preview and the save
enum class PhotoFilter(val label: String, val matrix: FloatArray) {
    NONE(
        "None",
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    GRAYSCALE(
        "B&W",
        floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    SEPIA(
        "Sepia",
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VIVID(
        "Vivid",
        run {
            // Standard luminance-preserving saturation matrix
            val sat = 1.4f
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            floatArrayOf(
                lumR * (1 - sat) + sat, lumG * (1 - sat), lumB * (1 - sat), 0f, 0f,
                lumR * (1 - sat), lumG * (1 - sat) + sat, lumB * (1 - sat), 0f, 0f,
                lumR * (1 - sat), lumG * (1 - sat), lumB * (1 - sat) + sat, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        }
    ),
    COOL(
        "Cool",
        floatArrayOf(
            0.9f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1.15f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    WARM(
        "Warm",
        floatArrayOf(
            1.15f, 0f, 0f, 0f, 10f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}
