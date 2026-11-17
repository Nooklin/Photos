package org.nooklin.photos.ui.editor

import androidx.compose.ui.text.font.FontWeight
import android.app.Activity
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Deblur
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vignette
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.filled.WbSunny
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import coil3.imageLoader
import coil3.memory.MemoryCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.MaterialTheme
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.EditorDestructivePill
import org.nooklin.photos.ui.common.EditorModeBar
import org.nooklin.photos.ui.common.EditorModeItem
import org.nooklin.photos.ui.theme.AppColors
import org.nooklin.photos.ui.common.detectPinchPanGestures
import org.nooklin.photos.data.OriginalsStore
import android.widget.Toast
import org.nooklin.photos.data.MediaStoreRepository
import android.content.ContentUris
import org.nooklin.photos.data.local.LocalMetadataStore
import org.nooklin.photos.R
import org.nooklin.photos.appString
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.Role
import org.nooklin.photos.ui.theme.Spacing
import org.nooklin.photos.ui.theme.AppText

// Bottom bar (Styles, Adjust, Crop), same image box in every mode so the photo doesn't move
enum class EditorMode { STYLES, ADJUST, CROP }

private val CropHandleInset = 24.dp
enum class AdjustmentType(@param:StringRes val labelRes: Int, val icon: ImageVector) {
    AUTO_ENHANCE(R.string.photo_editor_auto, Icons.Filled.AutoFixHigh),
    EXPOSURE(R.string.photo_editor_exposure, Icons.Filled.Exposure),
    BRIGHTNESS(R.string.photo_editor_brightness, Icons.Filled.WbSunny),
    HIGHLIGHTS(R.string.photo_editor_highlights, Icons.Filled.Highlight),
    SHADOWS(R.string.photo_editor_shadows, Icons.Filled.DarkMode),
    CONTRAST(R.string.photo_editor_contrast, Icons.Filled.Contrast),
    BLACK_POINT(R.string.photo_editor_black_point, Icons.Filled.Tonality),
    SATURATION(R.string.photo_editor_saturation, Icons.Filled.InvertColors),
    VIBRANCE(R.string.photo_editor_vibrance, Icons.Filled.Palette),
    WARMTH(R.string.photo_editor_warmth, Icons.Filled.WbIncandescent),
    TINT(R.string.photo_editor_tint, Icons.Filled.Colorize),
    SHARPNESS(R.string.photo_editor_sharpness, Icons.Filled.Deblur),
    DEFINITION(R.string.photo_editor_definition, Icons.Filled.Texture),
    NOISE_REDUCTION(R.string.photo_editor_noise_reduction, Icons.Filled.BlurOn),
    VIGNETTE(R.string.photo_editor_vignette, Icons.Filled.Vignette);
    val label: String get() = appString(labelRes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    target: EditTarget,
    darkTheme: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    // a deleted edit leaves nothing to show, so the caller can leave the viewer too instead of landing on it.
    onDeleted: () -> Unit = onSaved,
    onHiddenSizeChanged: (width: Int, height: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val screenBackground = if (darkTheme) Color.Black else MaterialTheme.colorScheme.background
    val chromeColor = if (darkTheme) Color.White else Color.Black
    val pillBackground = if (darkTheme) GlassPill.fixedGlassBackground else GlassPill.adaptiveGlassBackground
    val pillBorder = if (darkTheme) null else GlassPill.adaptiveBorder
    val popupBackground = if (darkTheme) GlassPill.fixedPopupBackground else GlassPill.adaptivePopupBackground
    val coroutineScope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var autoEnhanceTargets by remember { mutableStateOf<AutoEnhanceTargets?>(null) }
    var editorMode by rememberSaveable { mutableStateOf(EditorMode.STYLES) }
    var rotationDegrees by rememberSaveable { mutableStateOf(0f) }
    var straightenDegrees by rememberSaveable { mutableStateOf(0f) }
    var perspectiveH by rememberSaveable { mutableStateOf(0f) }
    var perspectiveV by rememberSaveable { mutableStateOf(0f) }
    var flipHorizontal by rememberSaveable { mutableStateOf(false) }
    var flipVertical by rememberSaveable { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf(PhotoFilter.NONE) }
    var adjustments by remember { mutableStateOf(Adjustments()) }
    var selectedAdjustmentType by rememberSaveable { mutableStateOf(AdjustmentType.BRIGHTNESS) }
    var cropRect by remember { mutableStateOf(CropRect()) }
    var selectedAspectPreset by rememberSaveable { mutableStateOf(AspectPreset.entries.first()) }
    var isSaving by remember { mutableStateOf(false) }
    var imageSizePx by remember { mutableStateOf(IntSize.Zero) }
    var userZoomScale by remember { mutableStateOf(1f) }
    var userZoomOffset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(editorMode) {
        if (editorMode == EditorMode.CROP) {
            userZoomScale = 1f
            userZoomOffset = Offset.Zero
        }
    }
    fun clampUserZoomOffset(newScale: Float, candidate: Offset): Offset {
        val maxX = (imageSizePx.width * (newScale - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (imageSizePx.height * (newScale - 1f) / 2f).coerceAtLeast(0f)
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val originals = remember { OriginalsStore(context) }
    val originalKey = remember(target) {
        when (target) {
            is EditTarget.MediaStoreTarget -> OriginalsStore.mediaKey(target.item.id)
            is EditTarget.HiddenFileTarget -> OriginalsStore.hiddenKey(target.file.name)
        }
    }
    var hasOriginal by remember(originalKey) { mutableStateOf(originals.has(originalKey)) }
    var isEditedCopy by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        (target as? EditTarget.MediaStoreTarget)?.let { isEditedCopy = LocalMetadataStore(context).isEditedCopy(it.item.id) }
    }
    var showRevertConfirm by remember { mutableStateOf(false) }
    var isReverting by remember { mutableStateOf(false) }

    val hasUnsavedChanges = rotationDegrees != 0f || straightenDegrees != 0f ||
        perspectiveH != 0f || perspectiveV != 0f || flipHorizontal || flipVertical ||
        selectedFilter != PhotoFilter.NONE || !adjustments.isNeutral ||
        !(cropRect.left <= 0f && cropRect.top <= 0f && cropRect.right >= 1f && cropRect.bottom >= 1f)

    fun requestBack() {
        if (hasUnsavedChanges) showDiscardConfirm = true else onBack()
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardConfirm = true
    }

    fun clearImageCaches(keys: List<String>) {
        val loader = context.imageLoader
        for (key in keys) {
            loader.memoryCache?.remove(MemoryCache.Key(key))
            loader.diskCache?.remove(key)
        }
    }

    val revertWriteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val mediaTarget = target as? EditTarget.MediaStoreTarget
        if (result.resultCode == Activity.RESULT_OK && mediaTarget != null) {
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(mediaTarget.item.uri, "rwt")?.use { output ->
                            originals.copyTo(originalKey, output)
                        } ?: false
                    } catch (e: Exception) {
                        false
                    }
                }
                isReverting = false
                if (success) {
                    originals.delete(originalKey)
                    hasOriginal = false
                    clearImageCaches(listOf(mediaTarget.item.uri.toString()))
                    onSaved()
                }
            }
        } else {
            isReverting = false
        }
    }

    fun onRevertConfirmed() {
        isReverting = true
        when (target) {
            is EditTarget.MediaStoreTarget -> if (isEditedCopy) {
                coroutineScope.launch {
                    val deleted = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.delete(target.item.uri, null, null) > 0
                        } catch (e: SecurityException) {
                            false
                        }
                    }
                    isReverting = false
                    if (deleted) {
                        LocalMetadataStore(context).removeEditedCopy(target.item.id)
                        clearImageCaches(listOf(target.item.uri.toString()))
                        onDeleted()
                    } else {
                        Toast.makeText(context, appString(R.string.photo_editor_could_not_delete_the_edited), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val pendingIntent = MediaStore.createWriteRequest(
                    context.contentResolver,
                    listOf(target.item.uri)
                )
                revertWriteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            }
            is EditTarget.HiddenFileTarget -> {
                coroutineScope.launch {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            val restored = target.file.outputStream().use { out -> originals.copyTo(originalKey, out) }
                            restored && rewriteHiddenThumbnail(target.file, target.thumbnailFile)
                        } catch (e: Exception) {
                            false
                        }
                    }
                    isReverting = false
                    if (success) {
                        originals.delete(originalKey)
                        hasOriginal = false
                        clearImageCaches(listOf(target.file.absolutePath, target.thumbnailFile.absolutePath))
                        withContext(Dispatchers.IO) { imageSizeOf(target.file) }?.let { onHiddenSizeChanged(it.first, it.second) }
                        onSaved()
                    }
                }
            }
        }
    }

    LaunchedEffect(target) {
        loadFailed = false
        autoEnhanceTargets = null
        val bitmap = withContext(Dispatchers.IO) {
            when (target) {
                is EditTarget.MediaStoreTarget -> decodePreviewBitmap(context, target.item.uri)
                is EditTarget.HiddenFileTarget -> decodePreviewBitmapFromFile(target.file)
            }
        }
        sourceBitmap = bitmap
        loadFailed = bitmap == null
        if (bitmap != null) {
            autoEnhanceTargets = withContext(Dispatchers.Default) { computeAutoEnhanceTargets(bitmap) }
        }
    }

    fun onSaveClicked() {
        isSaving = true
        coroutineScope.launch {
            val transformed = withContext(Dispatchers.Default) {
                try {
                    val fullRes = when (target) {
                        is EditTarget.MediaStoreTarget -> decodeFullResolutionBitmap(context, target.item.uri)
                        is EditTarget.HiddenFileTarget -> decodeFullResolutionBitmapFromFile(target.file)
                    } ?: return@withContext null
                    // each stage returns a new bitmap or its input, so the replaced input is freed (low peak memory)
                    var current: Bitmap = fullRes
                    current = applyPerspective(current, perspectiveH, perspectiveV).also { if (it !== current) current.recycle() }
                    current = applyGeometricTransform(current, rotationDegrees, straightenDegrees, flipHorizontal, flipVertical)
                        .also { if (it !== current) current.recycle() }
                    current = cropBitmap(current, cropRect).also { if (it !== current) current.recycle() }
                    val combinedMatrix = combinedColorMatrix(selectedFilter.matrix, adjustments)
                    current = applyColorMatrix(current, combinedMatrix).also { if (it !== current) current.recycle() }
                    current = applyShaderAdjustments(current, adjustments).also { if (it !== current) current.recycle() }
                    applyVignette(current, adjustments.vignette).also { if (it !== current) current.recycle() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                } catch (e: OutOfMemoryError) {
                    null
                }
            }
            if (transformed == null) {
                isSaving = false
                return@launch
            }

            when (target) {
                is EditTarget.MediaStoreTarget -> {
                    val copyUri = MediaStoreRepository(context).saveEditedCopy(transformed)
                    isSaving = false
                    if (copyUri != null) {
                        // Sorted just before the original (+1 second) so the copy sits beside it
                        LocalMetadataStore(context).setSortDate(ContentUris.parseId(copyUri), target.item.sortDate + 1)
                        LocalMetadataStore(context).addEditedCopy(ContentUris.parseId(copyUri))
                        Toast.makeText(context, appString(R.string.photo_editor_saved_as_a_new_photo), Toast.LENGTH_LONG).show()
                        onSaved()
                    } else {
                        Toast.makeText(context, appString(R.string.photo_editor_could_not_save_the_edit), Toast.LENGTH_LONG).show()
                    }
                }
                is EditTarget.HiddenFileTarget -> {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            if (!originals.stashIfAbsent(originalKey) { target.file.inputStream() }) {
                                return@withContext false
                            }
                            val format = compressFormatFor(target.file.extension)
                            target.file.outputStream().use { output ->
                                transformed.compress(format, 95, output)
                            } && run {
                                val longerSide = maxOf(transformed.width, transformed.height)
                                val scale = 512f / longerSide
                                val thumbBitmap = if (scale < 1f) {
                                    Bitmap.createScaledBitmap(
                                        transformed,
                                        (transformed.width * scale).toInt().coerceAtLeast(1),
                                        (transformed.height * scale).toInt().coerceAtLeast(1),
                                        true
                                    )
                                } else {
                                    transformed
                                }
                                val thumbSaved = target.thumbnailFile.outputStream().use { output ->
                                    thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                                }
                                if (thumbBitmap !== transformed) thumbBitmap.recycle()
                                thumbSaved
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }
                    isSaving = false
                    if (success) {
                        val loader = context.imageLoader
                        for (path in listOf(target.file.absolutePath, target.thumbnailFile.absolutePath)) {
                            loader.memoryCache?.remove(MemoryCache.Key(path))
                            loader.diskCache?.remove(path)
                        }
                        onHiddenSizeChanged(transformed.width, transformed.height)
                        onSaved()
                    }
                }
            }
        }
    }

    fun adjustmentValue(type: AdjustmentType): Float = when (type) {
        AdjustmentType.AUTO_ENHANCE -> adjustments.autoEnhance
        AdjustmentType.EXPOSURE -> adjustments.exposure
        AdjustmentType.BRIGHTNESS -> adjustments.brightness
        AdjustmentType.HIGHLIGHTS -> adjustments.highlights
        AdjustmentType.SHADOWS -> adjustments.shadows
        AdjustmentType.CONTRAST -> adjustments.contrast
        AdjustmentType.BLACK_POINT -> adjustments.blackPoint
        AdjustmentType.SATURATION -> adjustments.saturation
        AdjustmentType.VIBRANCE -> adjustments.vibrance
        AdjustmentType.WARMTH -> adjustments.warmth
        AdjustmentType.TINT -> adjustments.tint
        AdjustmentType.SHARPNESS -> adjustments.sharpness
        AdjustmentType.DEFINITION -> adjustments.definition
        AdjustmentType.NOISE_REDUCTION -> adjustments.noiseReduction
        AdjustmentType.VIGNETTE -> adjustments.vignette
    }

    fun setAdjustmentValue(type: AdjustmentType, value: Float) {
        adjustments = when (type) {
            AdjustmentType.AUTO_ENHANCE -> adjustments.copy(autoEnhance = value)
            AdjustmentType.EXPOSURE -> adjustments.copy(exposure = value)
            AdjustmentType.BRIGHTNESS -> adjustments.copy(brightness = value)
            AdjustmentType.HIGHLIGHTS -> adjustments.copy(highlights = value)
            AdjustmentType.SHADOWS -> adjustments.copy(shadows = value)
            AdjustmentType.CONTRAST -> adjustments.copy(contrast = value)
            AdjustmentType.BLACK_POINT -> adjustments.copy(blackPoint = value)
            AdjustmentType.SATURATION -> adjustments.copy(saturation = value)
            AdjustmentType.VIBRANCE -> adjustments.copy(vibrance = value)
            AdjustmentType.WARMTH -> adjustments.copy(warmth = value)
            AdjustmentType.TINT -> adjustments.copy(tint = value)
            AdjustmentType.SHARPNESS -> adjustments.copy(sharpness = value)
            AdjustmentType.DEFINITION -> adjustments.copy(definition = value)
            AdjustmentType.NOISE_REDUCTION -> adjustments.copy(noiseReduction = value)
            AdjustmentType.VIGNETTE -> adjustments.copy(vignette = value)
        }
    }

    // Auto Enhance writes eight other fields at once, so it isn't routed through setAdjustmentValue
    fun applyAutoEnhance(strength: Float) {
        val targets = autoEnhanceTargets ?: return
        adjustments = adjustments.copy(
            autoEnhance = strength,
            exposure = targets.exposure * strength,
            contrast = targets.contrast * strength,
            blackPoint = targets.blackPoint * strength,
            highlights = targets.highlights * strength,
            shadows = targets.shadows * strength,
            vibrance = targets.vibrance * strength,
            warmth = targets.warmth * strength,
            tint = targets.tint * strength
        )
    }

    Scaffold(
        containerColor = screenBackground,
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_edit), color = chromeColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { requestBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_cancel), tint = chromeColor)
                    }
                },
                actions = {
                    if (hasOriginal || isEditedCopy) {
                        EditorDestructivePill(
                            text = if (isEditedCopy) appString(R.string.photo_editor_delete_edit) else appString(R.string.photo_editor_revert),
                            enabled = !isSaving && !isReverting
                        ) { showRevertConfirm = true }
                    }
                    IconButton(
                        enabled = sourceBitmap != null && !isSaving && !isReverting,
                        onClick = { onSaveClicked() }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = appString(R.string.common_save), tint = chromeColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBackground)
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .background(screenBackground)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 8.dp)
            ) {
                when (editorMode) {
                    EditorMode.STYLES -> {
                        val bitmap = sourceBitmap
                        if (bitmap != null) {
                            val filterThumbnail = remember(bitmap) { filterThumbnailOf(bitmap) }
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                                items(PhotoFilter.entries, key = { it.name }) { filter ->
                                    val selected = filter == selectedFilter
                                    Column(
                                        Modifier
                                            .padding(end = 12.dp)
                                            .clickable(role = Role.Button) { selectedFilter = filter },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Image(
                                            bitmap = filterThumbnail,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            colorFilter = ColorFilter.colorMatrix(ComposeColorMatrix(filter.matrix)),
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .then(
                                                    if (selected) {
                                                        Modifier.border(2.dp, AppColors.SelectedYellow, RoundedCornerShape(10.dp))
                                                    } else Modifier
                                                )
                                        )
                                        Text(
                                            filter.label,
                                            color = if (selected) AppColors.SelectedYellow else chromeColor.copy(alpha = 0.7f),
                                            fontSize = AppText.Caption,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    EditorMode.ADJUST -> {
                        // LazyRow: 9 categories don't fit evenly on a phone, it scrolls like iOS
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Lg)
                        ) {
                            items(AdjustmentType.entries, key = { it.name }) { type ->
                                val selected = type == selectedAdjustmentType
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(chromeColor.copy(alpha = 0.1f))
                                            .then(
                                                if (selected) {
                                                    Modifier.border(2.dp, AppColors.SelectedYellow, CircleShape)
                                                } else Modifier
                                            )
                                            .clickable(role = Role.Button) {
                                                selectedAdjustmentType = type
                                                // a toggle: at 0 it jumps to 100% (the full computed correction), again resets all eight fields to 0
                                                if (type == AdjustmentType.AUTO_ENHANCE) {
                                                    applyAutoEnhance(if (adjustments.autoEnhance == 0f) 1f else 0f)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val value = adjustmentValue(type)
                                        if (value != 0f) {
                                            val counterValue = (value * 100).toInt()
                                            Text(
                                                text = if (counterValue > 0) "+$counterValue" else "$counterValue",
                                                color = chromeColor,
                                                fontSize = AppText.Body,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Icon(type.icon, contentDescription = type.label, tint = chromeColor)
                                        }
                                    }
                                    Text(
                                        type.label,
                                        color = if (selected) AppColors.SelectedYellow else chromeColor.copy(alpha = 0.7f),
                                        fontSize = AppText.Caption,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                        Slider(
                            value = adjustmentValue(selectedAdjustmentType),
                            onValueChange = {
                                if (selectedAdjustmentType == AdjustmentType.AUTO_ENHANCE) {
                                    applyAutoEnhance(it)
                                } else {
                                    setAdjustmentValue(selectedAdjustmentType, it)
                                }
                            },
                            valueRange = if (
                                selectedAdjustmentType == AdjustmentType.AUTO_ENHANCE ||
                                selectedAdjustmentType == AdjustmentType.NOISE_REDUCTION
                            ) {
                                0f..1f
                            } else {
                                -1f..1f
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        )
                    }
                    EditorMode.CROP -> {
                        GeometryTools(
                            chromeColor = chromeColor,
                            tools = GeometryTool.entries,
                            values = mapOf(
                                GeometryTool.STRAIGHTEN to straightenDegrees,
                                GeometryTool.VERTICAL to perspectiveV,
                                GeometryTool.HORIZONTAL to perspectiveH
                            ),
                            onValueChange = { tool, newValue ->
                                when (tool) {
                                    GeometryTool.STRAIGHTEN -> straightenDegrees = newValue
                                    GeometryTool.VERTICAL -> perspectiveV = newValue
                                    GeometryTool.HORIZONTAL -> perspectiveH = newValue
                                }
                            }
                        )
                    }
                }

                EditorModeBar(
                    items = listOf(
                        EditorModeItem(EditorMode.STYLES, Icons.Filled.AutoAwesome, appString(R.string.photo_editor_styles)),
                        EditorModeItem(EditorMode.ADJUST, Icons.Filled.Tune, appString(R.string.photo_editor_adjust)),
                        EditorModeItem(EditorMode.CROP, Icons.Filled.Crop, appString(R.string.photo_editor_crop))
                    ),
                    selected = editorMode,
                    chromeColor = chromeColor,
                    pillBackground = pillBackground,
                    pillBorder = pillBorder
                ) { editorMode = it }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        if (editorMode == EditorMode.CROP && sourceBitmap != null) {
            CropTopRow(
                chromeColor = chromeColor,
                pillBackground = pillBackground,
                pillBorder = pillBorder,
                selectedPreset = selectedAspectPreset,
                onSelectPreset = { preset ->
                    selectedAspectPreset = preset
                    val bitmap = sourceBitmap
                    if (bitmap != null) {
                        val swapped = rotationDegrees % 180f != 0f
                        val imageAspect = if (swapped) {
                            bitmap.height.toFloat() / bitmap.width.toFloat()
                        } else {
                            bitmap.width.toFloat() / bitmap.height.toFloat()
                        }
                        cropRect = if (preset.ratio == null) CropRect() else cropRectForAspect(preset.ratio, imageAspect)
                    }
                },
                onRotate = {
                    rotationDegrees = (rotationDegrees + 90f) % 360f
                    cropRect = CropRect()
                },
                onFlipHorizontal = { flipHorizontal = !flipHorizontal },
                onFlipVertical = { flipVertical = !flipVertical },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (editorMode == EditorMode.CROP) CropTopRowHeight else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = sourceBitmap
            when {
                bitmap != null -> {
                    val combinedMatrix = combinedColorMatrix(selectedFilter.matrix, adjustments)
                    val previewRenderEffect = remember(adjustments, imageSizePx) {
                        buildPreviewRenderEffect(adjustments, imageSizePx.width, imageSizePx.height)
                    }
                    val swapped = rotationDegrees % 180f != 0f
                    val imageAspect = if (swapped) {
                        bitmap.height.toFloat() / bitmap.width.toFloat()
                    } else {
                        bitmap.width.toFloat() / bitmap.height.toFloat()
                    }
                    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val cropInset = if (editorMode == EditorMode.CROP) CropHandleInset else 0.dp
                    val availableWidth = maxWidth - cropInset * 2
                    val availableHeight = maxHeight - cropInset * 2
                    val photoWidth = if (availableWidth / availableHeight < imageAspect) availableWidth else availableHeight * imageAspect
                    val photoHeight = if (availableWidth / availableHeight < imageAspect) availableWidth / imageAspect else availableHeight
                    Box(Modifier.size(photoWidth + cropInset * 2, photoHeight + cropInset * 2), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(photoWidth, photoHeight)) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(ComposeColorMatrix(combinedMatrix)),
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { imageSizePx = it }
                                .then(
                                    if (editorMode != EditorMode.CROP) {
                                        Modifier.pointerInput(editorMode) {
                                            detectPinchPanGestures(
                                                getScale = { userZoomScale },
                                                onGesture = { pan, zoom ->
                                                    val newScale = (userZoomScale * zoom).coerceIn(1f, 6f)
                                                    userZoomScale = newScale
                                                    userZoomOffset = if (newScale <= 1f) {
                                                        Offset.Zero
                                                    } else {
                                                        clampUserZoomOffset(newScale, userZoomOffset + pan)
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .graphicsLayer {
                                    rotationZ = rotationDegrees + straightenDegrees
                                    val fillScale = if (straightenDegrees != 0f) {
                                        val theta = Math.toRadians(straightenDegrees.toDouble())
                                        (
                                            kotlin.math.abs(kotlin.math.cos(theta)) +
                                                kotlin.math.abs(kotlin.math.sin(theta)) * (size.width / size.height)
                                            ).toFloat()
                                    } else {
                                        1f
                                    }
                                    scaleX = (if (flipHorizontal) -1f else 1f) * fillScale * userZoomScale
                                    scaleY = (if (flipVertical) -1f else 1f) * fillScale * userZoomScale
                                    translationX = userZoomOffset.x
                                    translationY = userZoomOffset.y
                                    // Perspective preview is a 3D tilt (rotationX/Y), graphicsLayer has no projective transform
                                    if (perspectiveV != 0f || perspectiveH != 0f) {
                                        cameraDistance = 12f * density
                                        rotationX = -perspectiveV * 15f
                                        rotationY = perspectiveH * 15f
                                    }
                                    renderEffect = previewRenderEffect
                                }
                        )
                        // Live vignette is a radial gradient overlay, a bitmap redraw per frame would make dragging choppy
                        if (adjustments.vignette != 0f) {
                            val strength = adjustments.vignette
                            val darken = strength > 0f
                            val amount = kotlin.math.abs(strength).coerceIn(0f, 1f)
                            ComposeCanvas(Modifier.fillMaxSize()) {
                                val edgeColor = if (darken) {
                                    Color.Black.copy(alpha = amount * 0.7f)
                                } else {
                                    Color.White.copy(alpha = amount * 0.5f)
                                }
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.Transparent, edgeColor),
                                        center = center,
                                        radius = size.maxDimension * 0.75f
                                    ),
                                    blendMode = if (darken) BlendMode.Multiply else BlendMode.Screen
                                )
                            }
                        }
                    }
                    if (editorMode == EditorMode.CROP) {
                        CropOverlay(
                            cropRect = cropRect,
                            onCropRectChange = { cropRect = it },
                            inset = cropInset
                        )
                    }
                    }
                    }
                }
                loadFailed -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val isHiddenVideo = target is EditTarget.HiddenFileTarget && target.isVideo
                        Text(if (isHiddenVideo) appString(R.string.photo_editor_videos_in_hidden_can_t) else appString(R.string.photo_editor_this_photo_can_t_be), color = chromeColor)
                        Text(
                            if (isHiddenVideo) {
                                appString(R.string.photo_editor_move_the_video_out_of)
                            } else {
                                appString(R.string.photo_editor_the_format_isn_t_supported)
                            },
                            color = chromeColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
                        )
                    }
                }
                else -> CircularProgressIndicator(color = chromeColor)
            }

            if (isSaving) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
        }
    }

    if (showRevertConfirm) {
        AlertDialog(
            onDismissRequest = { showRevertConfirm = false },
            shape = GlassPill.shape,
            containerColor = popupBackground,
            titleContentColor = chromeColor,
            textContentColor = chromeColor,
            title = { Text(if (isEditedCopy) appString(R.string.photo_editor_delete_this_edit) else appString(R.string.photo_editor_revert_to_original)) },
            text = {
                Text(
                    if (isEditedCopy) appString(R.string.photo_editor_this_deletes_the_edited_copy)
                    else appString(R.string.photo_editor_this_restores_the_photo_to)
                )
            },
            confirmButton = {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Red.copy(alpha = 0.15f))
                        .clickable(role = Role.Button) {
                            showRevertConfirm = false
                            onRevertConfirmed()
                        }
                        .padding(horizontal = Spacing.Lg, vertical = Spacing.Md)
                ) {
                    Text(if (isEditedCopy) appString(R.string.photo_editor_delete_edit) else appString(R.string.photo_editor_revert_to_original_2), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertConfirm = false }) {
                    Text(appString(R.string.common_cancel))
                }
            }
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            shape = GlassPill.shape,
            containerColor = popupBackground,
            titleContentColor = chromeColor,
            textContentColor = chromeColor,
            title = { Text(appString(R.string.photo_editor_discard_changes)) },
            text = { Text(appString(R.string.photo_editor_are_you_sure_you_want)) },
            confirmButton = {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Red.copy(alpha = 0.15f))
                        .clickable(role = Role.Button) {
                            showDiscardConfirm = false
                            onBack()
                        }
                        .padding(horizontal = Spacing.Lg, vertical = Spacing.Md)
                ) {
                    Text(appString(R.string.photo_editor_discard_changes_2), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text(appString(R.string.common_cancel))
                }
            }
        )
    }
}

