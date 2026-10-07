package org.nooklin.photos.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.ui.graphics.asComposeRenderEffect
import android.graphics.RenderEffect as AndroidRenderEffect
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect

// AGSL shaders for the sliders a ColorMatrix can't express: highlights, shadows, vibrance (per-pixel awareness),
// sharpness, definition, noise reduction (neighbor sampling)
private const val TONE_SHADER_SRC = """
    uniform shader inputShader;
    uniform float highlights;
    uniform float shadows;
    uniform float vibrance;

    half4 main(float2 coord) {
        half4 c = inputShader.eval(coord);
        half luminance = dot(c.rgb, half3(0.299, 0.587, 0.114));

        // Highlights: only affects the bright end (smoothstep mask rising
        // from mid-tones to white), positive brightens further, negative
        // recovers/darkens blown highlights.
        half highlightMask = smoothstep(0.3, 1.0, luminance);
        c.rgb += half(highlights) * highlightMask * 0.45;

        // Shadows: mirror of the above, masked toward the dark end,
        // positive lifts/opens shadows, negative deepens them.
        half shadowMask = smoothstep(0.7, 0.0, luminance);
        c.rgb += half(shadows) * shadowMask * 0.45;

        // Vibrance: extrapolate away from (positive) or toward (negative)
        // this pixel's own gray point, scaled by how UNsaturated it
        // already is (1.0 - sat), an already-vivid pixel barely moves,
        // a near-gray pixel moves the most. This is the actual
        // distinction between Vibrance and a flat Saturation boost.
        half maxC = max(c.r, max(c.g, c.b));
        half minC = min(c.r, min(c.g, c.b));
        half sat = maxC - minC;
        half3 gray = half3(luminance);
        half boost = half(vibrance) * (1.0 - sat);
        c.rgb = gray + (c.rgb - gray) * (1.0 + boost);

        c.rgb = clamp(c.rgb, 0.0, 1.0);
        return c;
    }
"""

// Unsharp mask: subtract a 4-tap neighbor blur to isolate fine detail, add it back by strength
private const val SHARPEN_SHADER_SRC = """
    uniform shader inputShader;
    uniform float sharpness;
    uniform float2 texel;

    half4 main(float2 coord) {
        half4 center = inputShader.eval(coord);
        half4 sum = half4(0.0);
        sum += inputShader.eval(coord + float2(-texel.x, 0.0));
        sum += inputShader.eval(coord + float2(texel.x, 0.0));
        sum += inputShader.eval(coord + float2(0.0, -texel.y));
        sum += inputShader.eval(coord + float2(0.0, texel.y));
        half4 blurred = sum / 4.0;
        half4 result = center + (center - blurred) * half(sharpness) * 2.0;
        result.rgb = clamp(result.rgb, 0.0, 1.0);
        result.a = center.a;
        return result;
    }
"""

// Same as sharpen but 4 texels away: broader local contrast (clarity)
private const val DEFINITION_SHADER_SRC = """
    uniform shader inputShader;
    uniform float definition;
    uniform float2 texel;

    half4 main(float2 coord) {
        half4 center = inputShader.eval(coord);
        float radius = 4.0;
        half4 sum = half4(0.0);
        sum += inputShader.eval(coord + float2(-texel.x * radius, 0.0));
        sum += inputShader.eval(coord + float2(texel.x * radius, 0.0));
        sum += inputShader.eval(coord + float2(0.0, -texel.y * radius));
        sum += inputShader.eval(coord + float2(0.0, texel.y * radius));
        half4 blurred = sum / 4.0;
        half4 result = center + (center - blurred) * half(definition) * 1.5;
        result.rgb = clamp(result.rgb, 0.0, 1.0);
        result.a = center.a;
        return result;
    }
"""

// Blend toward a 9-tap box blur: a cheap noise reduction that also softens edges
private const val NOISE_REDUCTION_SHADER_SRC = """
    uniform shader inputShader;
    uniform float noiseReduction;
    uniform float2 texel;

    half4 main(float2 coord) {
        half4 center = inputShader.eval(coord);
        half4 sum = center;
        sum += inputShader.eval(coord + float2(-texel.x, -texel.y));
        sum += inputShader.eval(coord + float2(texel.x, -texel.y));
        sum += inputShader.eval(coord + float2(-texel.x, texel.y));
        sum += inputShader.eval(coord + float2(texel.x, texel.y));
        sum += inputShader.eval(coord + float2(-texel.x, 0.0));
        sum += inputShader.eval(coord + float2(texel.x, 0.0));
        sum += inputShader.eval(coord + float2(0.0, -texel.y));
        sum += inputShader.eval(coord + float2(0.0, texel.y));
        half4 blurred = sum / 9.0;
        half4 result = mix(center, blurred, half(noiseReduction));
        result.a = center.a;
        return result;
    }
"""

// The save path runs on a background thread, so it gets its own shader objects instead of sharing the preview ones.
private val saveToneShader by lazy { RuntimeShader(TONE_SHADER_SRC) }
private val saveSharpenShader by lazy { RuntimeShader(SHARPEN_SHADER_SRC) }
private val saveDefinitionShader by lazy { RuntimeShader(DEFINITION_SHADER_SRC) }
private val saveNoiseReductionShader by lazy { RuntimeShader(NOISE_REDUCTION_SHADER_SRC) }
private val toneShader by lazy { RuntimeShader(TONE_SHADER_SRC) }
private val sharpenShader by lazy { RuntimeShader(SHARPEN_SHADER_SRC) }
private val definitionShader by lazy { RuntimeShader(DEFINITION_SHADER_SRC) }
private val noiseReductionShader by lazy { RuntimeShader(NOISE_REDUCTION_SHADER_SRC) }

// True when any shader adjustment is non-neutral, lets callers skip the effect chain
fun Adjustments.needsShaderPass(): Boolean =
    highlights != 0f || shadows != 0f || vibrance != 0f ||
        sharpness != 0f || definition != 0f || noiseReduction != 0f

// Chains RenderEffects, the first argument applies first (createChainEffect(outer, inner))
private fun chainInOrder(vararg effects: AndroidRenderEffect?): AndroidRenderEffect? {
    var result: AndroidRenderEffect? = null
    for (effect in effects) {
        if (effect == null) continue
        result = if (result == null) effect else AndroidRenderEffect.createChainEffect(effect, result)
    }
    return result
}

// Live preview chain. Needs the real on-screen pixel size for the texel-size uniform.
fun buildPreviewRenderEffect(adjustments: Adjustments, widthPx: Int, heightPx: Int): ComposeRenderEffect? {
    if (!adjustments.needsShaderPass() || widthPx <= 0 || heightPx <= 0) return null
    val texelX = 1f / widthPx
    val texelY = 1f / heightPx

    val toneEffect = if (adjustments.highlights != 0f || adjustments.shadows != 0f || adjustments.vibrance != 0f) {
        toneShader.setFloatUniform("highlights", adjustments.highlights)
        toneShader.setFloatUniform("shadows", adjustments.shadows)
        toneShader.setFloatUniform("vibrance", adjustments.vibrance)
        AndroidRenderEffect.createRuntimeShaderEffect(toneShader, "inputShader")
    } else null

    val noiseReductionEffect = if (adjustments.noiseReduction != 0f) {
        noiseReductionShader.setFloatUniform("noiseReduction", adjustments.noiseReduction)
        noiseReductionShader.setFloatUniform("texel", texelX, texelY)
        AndroidRenderEffect.createRuntimeShaderEffect(noiseReductionShader, "inputShader")
    } else null

    val definitionEffect = if (adjustments.definition != 0f) {
        definitionShader.setFloatUniform("definition", adjustments.definition)
        definitionShader.setFloatUniform("texel", texelX, texelY)
        AndroidRenderEffect.createRuntimeShaderEffect(definitionShader, "inputShader")
    } else null

    val sharpenEffect = if (adjustments.sharpness != 0f) {
        sharpenShader.setFloatUniform("sharpness", adjustments.sharpness)
        sharpenShader.setFloatUniform("texel", texelX, texelY)
        AndroidRenderEffect.createRuntimeShaderEffect(sharpenShader, "inputShader")
    } else null

    return chainInOrder(toneEffect, noiseReductionEffect, definitionEffect, sharpenEffect)?.asComposeRenderEffect()
}

// Save-step counterpart: same shaders and order on a real Bitmap, using its full-res size
fun applyShaderAdjustments(source: Bitmap, adjustments: Adjustments): Bitmap {
    if (!adjustments.needsShaderPass()) return source
    var current = source

    fun applyPass(shader: RuntimeShader, setUniforms: (RuntimeShader) -> Unit): Bitmap {
        shader.setInputShader("inputShader", BitmapShader(current, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        setUniforms(shader)
        val output = Bitmap.createBitmap(current.width, current.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = shader
        Canvas(output).drawRect(0f, 0f, current.width.toFloat(), current.height.toFloat(), paint)
        // Free the previous pass output, never the caller's source bitmap.
        if (current !== source) current.recycle()
        return output
    }

    if (adjustments.highlights != 0f || adjustments.shadows != 0f || adjustments.vibrance != 0f) {
        current = applyPass(saveToneShader) {
            it.setFloatUniform("highlights", adjustments.highlights)
            it.setFloatUniform("shadows", adjustments.shadows)
            it.setFloatUniform("vibrance", adjustments.vibrance)
        }
    }

    if (adjustments.noiseReduction != 0f) {
        current = applyPass(saveNoiseReductionShader) {
            it.setFloatUniform("noiseReduction", adjustments.noiseReduction)
            it.setFloatUniform("texel", 1f / current.width, 1f / current.height)
        }
    }

    if (adjustments.definition != 0f) {
        current = applyPass(saveDefinitionShader) {
            it.setFloatUniform("definition", adjustments.definition)
            it.setFloatUniform("texel", 1f / current.width, 1f / current.height)
        }
    }

    if (adjustments.sharpness != 0f) {
        current = applyPass(saveSharpenShader) {
            it.setFloatUniform("sharpness", adjustments.sharpness)
            it.setFloatUniform("texel", 1f / current.width, 1f / current.height)
        }
    }

    return current
}
