package org.nooklin.photos.ui.viewer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.GlassPill
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.getValue

val ViewerPillHeight = 48.dp

val ViewerTopBarHeight = ViewerPillHeight + 8.dp

val FilmstripHeight = 68.dp

// While hidden, a tap shows the chrome and nothing underneath reacts, so invisible buttons can't fire
fun Modifier.chromeGate(visible: Boolean, onShow: () -> Unit): Modifier =
    if (visible) {
        this
    } else {
        this.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                down.consume()
                val up = waitForUpOrCancellation(PointerEventPass.Initial)
                if (up != null) {
                    up.consume()
                    onShow()
                }
            }
        }
    }

// Constant, so swiping between photos and videos never resizes the pager
@Composable
fun videoFramePadding(scaffold: PaddingValues, topBarHeight: Dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues.Absolute(
        left = scaffold.calculateLeftPadding(direction),
        top = (scaffold.calculateTopPadding() - topBarHeight / 2).coerceAtLeast(0.dp),
        right = scaffold.calculateRightPadding(direction),
        bottom = scaffold.calculateBottomPadding()
    )
}

fun photoFrameInset(topBarHeight: Dp, extraBottom: Dp): PaddingValues =
    PaddingValues(top = topBarHeight / 2, bottom = extraBottom)

@Composable
fun DateTimePill(
    dateLabel: String,
    timeLabel: String,
    backgroundColor: Color = Color.Black.copy(alpha = 0.6f),
    border: BorderStroke? = null,
    contentColor: Color = Color.White
) {
    Surface(
        color = backgroundColor,
        shape = GlassPill.shape,
        border = border
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.height(ViewerPillHeight).padding(horizontal = 16.dp)
        ) {
            // Explicit line heights: the inherited 24sp line height made the two lines far taller than the text.
            Text(dateLabel, color = contentColor, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
            Text(timeLabel, color = contentColor.copy(alpha = 0.8f), fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun PhotoFilmstrip(
    thumbnails: List<Any>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    selectedBorderColor: Color = Color.White
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        if (thumbnails.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex.coerceIn(0, thumbnails.lastIndex))
        }
    }
    LazyRow(
        state = listState,
        modifier = Modifier.height(FilmstripHeight),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(thumbnails, key = { _, model -> model.toString() }) { index, model ->
            val selected = index == currentIndex
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(if (selected) 52.dp else 44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (selected) {
                            Modifier.border(2.dp, selectedBorderColor, RoundedCornerShape(6.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(index) }
            )
        }
    }
}
