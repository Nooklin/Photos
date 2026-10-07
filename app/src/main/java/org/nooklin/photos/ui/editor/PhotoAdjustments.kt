package org.nooklin.photos.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader

// ColorMatrix generators for the sliders (4x5 row-major, same as PhotoFilters)
data class Adjustments(
    val brightness: Float = 0f,  // -1..1, 0 = neutral
    val contrast: Float = 0f,    // -1..1, 0 = neutral
    val saturation: Float = 0f,  // -1..1, 0 = neutral
    val warmth: Float = 0f,      // -1..1, 0 = neutral
    val exposure: Float = 0f,    // -1..1, 0 = neutral
    val blackPoint: Float = 0f,  // -1..1, 0 = neutral (positive crushes shadows, negative lifts/fades them)
    val tint: Float = 0f,        // -1..1, 0 = neutral (green/magenta axis, mirrors warmth's red/blue axis)
    val vignette: Float = 0f,    // -1..1, 0 = neutral (positive darkens edges, negative lightens them)
    val autoEnhance: Float = 0f, // 0..1, 0 = off (strength applied to the 8 fields AutoEnhanceTargets computes — see this file's own top doc comment)
    // Not ColorMatrix-expressible, applied through the AGSL shaders in PhotoShaders.kt
    val highlights: Float = 0f, // -1..1, 0 = neutral (positive brightens bright areas, negative recovers/darkens them)
    val shadows: Float = 0f,    // -1..1, 0 = neutral (positive lifts dark areas, negative deepens them)
    val vibrance: Float = 0f,   // -1..1, 0 = neutral (boosts low-saturation pixels more than already-saturated ones)
    val sharpness: Float = 0f,  // -1..1, 0 = neutral (unsharp-mask edge enhancement; negative softens)
    // Same AGSL pipeline
    val definition: Float = 0f, // -1..1, 0 = neutral (wider-radius local-contrast enhancement; negative softens broadly)
    val noiseReduction: Float = 0f // 0..1 only, no sensible negative direction
) {
    val isNeutral: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f && warmth == 0f &&
            exposure == 0f && blackPoint == 0f && tint == 0f && vignette == 0f && autoEnhance == 0f &&
            highlights == 0f && shadows == 0f && vibrance == 0f && sharpness == 0f &&
            definition == 0f && noiseReduction == 0f
}

private fun brightnessMatrix(value: Float): FloatArray {
    val b = value * 255f
    return floatArrayOf(
        1f, 0f, 0f, 0f, b,
        0f, 1f, 0f, 0f, b,
        0f, 0f, 1f, 0f, b,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun contrastMatrix(value: Float): FloatArray {
    val c = value + 1f
    val t = (1f - c) * 128f
    return floatArrayOf(
        c, 0f, 0f, 0f, t,
        0f, c, 0f, 0f, t,
        0f, 0f, c, 0f, t,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun saturationMatrix(value: Float): FloatArray {
    val sat = value + 1f
    val lumR = 0.213f
    val lumG = 0.715f
    val lumB = 0.072f
    return floatArrayOf(
        lumR * (1 - sat) + sat, lumG * (1 - sat), lumB * (1 - sat), 0f, 0f,
        lumR * (1 - sat), lumG * (1 - sat) + sat, lumB * (1 - sat), 0f, 0f,
        lumR * (1 - sat), lumG * (1 - sat), lumB * (1 - sat) + sat, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun warmthMatrix(value: Float): FloatArray {
    val r = 1f + value * 0.15f
    val b = 1f - value * 0.15f
    return floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

// Multiplicative gain, unlike brightness' additive shift
private fun exposureMatrix(value: Float): FloatArray {
    val scale = 1f + value
    return floatArrayOf(
        scale, 0f, 0f, 0f, 0f,
        0f, scale, 0f, 0f, 0f,
        0f, 0f, scale, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

// Levels-style shadow floor: positive crushes, negative fades. Symmetric like the other sliders.
private fun blackPointMatrix(value: Float): FloatArray {
    val shift = value * 80f
    val scale = 255f / (255f - shift).coerceAtLeast(1f)
    val offset = -shift * scale
    return floatArrayOf(
        scale, 0f, 0f, 0f, offset,
        0f, scale, 0f, 0f, offset,
        0f, 0f, scale, 0f, offset,
        0f, 0f, 0f, 1f, 0f
    )
}

// Green/magenta axis, like warmth's red/blue
private fun tintMatrix(value: Float): FloatArray {
    val g = 1f - value * 0.15f
    val rb = 1f + value * 0.075f
    return floatArrayOf(
        rb, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, rb, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

// 4x5 composition: 4x4 multiplied, 5th column added (what ColorMatrix.preConcat does)
private fun multiplyColorMatrices(a: FloatArray, b: FloatArray): FloatArray {
    val result = FloatArray(20)
    for (row in 0 until 4) {
        for (col in 0 until 4) {
            var sum = 0f
            for (k in 0 until 4) {
                sum += a[row * 5 + k] * b[k * 5 + col]
            }
            result[row * 5 + col] = sum
        }
        var translation = a[row * 5 + 4]
        for (k in 0 until 4) {
            translation += a[row * 5 + k] * b[k * 5 + 4]
        }
        result[row * 5 + 4] = translation
    }
    return result
}

// Filter plus all sliders in one matrix. Auto Enhance isn't here, it sets the sliders themselves.
fun combinedColorMatrix(
    filterMatrix: FloatArray,
    adjustments: Adjustments
): FloatArray {
    var result = filterMatrix
    if (adjustments.exposure != 0f) result = multiplyColorMatrices(exposureMatrix(adjustments.exposure), result)
    if (adjustments.brightness != 0f) result = multiplyColorMatrices(brightnessMatrix(adjustments.brightness), result)
    if (adjustments.blackPoint != 0f) result = multiplyColorMatrices(blackPointMatrix(adjustments.blackPoint), result)
    if (adjustments.contrast != 0f) result = multiplyColorMatrices(contrastMatrix(adjustments.contrast), result)
    if (adjustments.saturation != 0f) result = multiplyColorMatrices(saturationMatrix(adjustments.saturation), result)
    if (adjustments.warmth != 0f) result = multiplyColorMatrices(warmthMatrix(adjustments.warmth), result)
    if (adjustments.tint != 0f) result = multiplyColorMatrices(tintMatrix(adjustments.tint), result)
    return result
}

// Suggested values for one photo, scaled by one 0..1 strength into Adjustments
data class AutoEnhanceTargets(
    val exposure: Float,
    val contrast: Float,
    val blackPoint: Float,
    val highlights: Float,
    val shadows: Float,
    val vibrance: Float,
    val warmth: Float,
    val tint: Float
)

// Analyzes a 128px copy and derives a conservative correction per field, once per photo
fun computeAutoEnhanceTargets(source: Bitmap): AutoEnhanceTargets {
    val maxDimension = 128
    val longerSide = maxOf(source.width, source.height)
    val scale = maxDimension.toFloat() / longerSide
    val sample = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else {
        source
    }

    val pixelCount = sample.width * sample.height
    val pixels = IntArray(pixelCount)
    sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
    if (sample !== source) sample.recycle()

    val lumaHist = IntArray(256)
    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    var sumSaturation = 0.0
    for (p in pixels) {
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        sumR += r
        sumG += g
        sumB += b
        val luma = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
        lumaHist[luma]++
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        sumSaturation += if (maxC == 0) 0.0 else (maxC - minC).toDouble() / maxC
    }

    val avgR = sumR.toFloat() / pixelCount
    val avgG = sumG.toFloat() / pixelCount
    val avgB = sumB.toFloat() / pixelCount
    val avgLuminance = (0.299f * avgR + 0.587f * avgG + 0.114f * avgB) / 255f
    val avgSaturation = (sumSaturation / pixelCount).toFloat()

    fun percentile(fraction: Float): Int {
        val target = (pixelCount * fraction).toInt()
        var cumulative = 0
        for (i in 0..255) {
            cumulative += lumaHist[i]
            if (cumulative >= target) return i
        }
        return 255
    }

    val p01 = percentile(0.01f)
    val p05 = percentile(0.05f)
    val p95 = percentile(0.95f)
    val p99 = percentile(0.99f)

    // Exposure: nudge toward a midtone, partially (snow or night scenes stay)
    val exposureTarget = ((0.48f - avgLuminance) * 1.2f).coerceIn(-0.35f, 0.35f)

    // Contrast: only added, for flat photos
    val spread = (p95 - p05) / 255f
    val contrastTarget = ((0.78f - spread) * 0.6f).coerceIn(0f, 0.3f)

    // Black point: only crushed, when the shadow floor is above true black
    val blackPointTarget = ((p01 / 255f) * 0.7f).coerceIn(0f, 0.25f)

    // Highlights: only recovered, when clipping
    val highlightsTarget = if (p99 >= 253) -0.25f else 0f

    // Shadows: only lifted, when crushed
    val shadowsTarget = if (p01 <= 2) 0.25f else 0f

    // Vibrance: only added, for flat photos
    val vibranceTarget = ((0.32f - avgSaturation) * 0.9f).coerceIn(0f, 0.3f)

    // Warmth and tint: gray-world white balance, corrected gently, never a full snap (skin tones)
    val warmthTarget = (-(avgR - avgB) / 255f * 0.6f).coerceIn(-0.2f, 0.2f)
    val tintTarget = (-(avgG - (avgR + avgB) / 2f) / 255f * 0.6f).coerceIn(-0.2f, 0.2f)

    return AutoEnhanceTargets(
        exposure = exposureTarget,
        contrast = contrastTarget,
        blackPoint = blackPointTarget,
        highlights = highlightsTarget,
        shadows = shadowsTarget,
        vibrance = vibranceTarget,
        warmth = warmthTarget,
        tint = tintTarget
    )
}

// Vignette is a separate Canvas step after the color pass: it varies by position, not color
fun applyVignette(source: Bitmap, strength: Float): Bitmap {
    if (strength == 0f) return source
    val output = source.copy(Bitmap.Config.ARGB_8888, true) ?: source
    val canvas = Canvas(output)
    val darken = strength > 0f
    val amount = kotlin.math.abs(strength).coerceIn(0f, 1f)
    val cx = output.width / 2f
    val cy = output.height / 2f
    val radius = maxOf(output.width, output.height) * 0.75f
    val edgeAlpha = (amount * if (darken) 180f else 130f).toInt().coerceIn(0, 255)
    val edgeColor = if (darken) {
        Color.argb(edgeAlpha, 0, 0, 0)
    } else {
        Color.argb(edgeAlpha, 255, 255, 255)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(cx, cy, radius, Color.TRANSPARENT, edgeColor, Shader.TileMode.CLAMP)
        xfermode = PorterDuffXfermode(if (darken) PorterDuff.Mode.MULTIPLY else PorterDuff.Mode.SCREEN)
    }
    canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), paint)
    return output
}
