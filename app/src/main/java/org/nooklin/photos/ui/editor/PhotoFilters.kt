package org.nooklin.photos.ui.editor
import androidx.annotation.StringRes
import org.nooklin.photos.R
import org.nooklin.photos.appString

// ColorMatrix filters (4x5), the same array drives the preview and the save
enum class PhotoFilter(@StringRes val labelRes: Int, val matrix: FloatArray) {
    NONE(
        R.string.settings_gallery_none,
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    GRAYSCALE(
        R.string.photo_filters_b_w,
        floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    SEPIA(
        R.string.photo_filters_sepia,
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VIVID(
        R.string.photo_filters_vivid,
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
        R.string.photo_filters_cool,
        floatArrayOf(
            0.9f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1.15f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    WARM(
        R.string.photo_filters_warm,
        floatArrayOf(
            1.15f, 0f, 0f, 0f, 10f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    );
    val label: String get() = appString(labelRes)
}
