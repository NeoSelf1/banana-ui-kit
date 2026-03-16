package com.neon.sample.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HMPickerLegacy(
    modifier: Modifier = Modifier,
    items: List<String>,
    initialItem: String,
    onItemSelected: (Int, String) -> Unit = { _, _ -> },
    content: @Composable ((String, Boolean) -> Unit)
) {
    val density = LocalDensity.current
    val scrollState = rememberLazyListState(0)
    var lastSelectedIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    var itemHeightPx by remember { mutableStateOf(0f) }
    var itemHeightDp by remember { mutableStateOf(0.dp) }
    var pickerHeightPx by remember { mutableStateOf(0f) }

    /** SubcomposeLayout으로 아이템 높이를 측정합니다. */
    SubcomposeLayout { constraints ->
        if (itemHeightPx == 0f) {
            val sampleItem = items.firstOrNull() ?: ""
            val measurables = subcompose("sample") {
                content(sampleItem, false)
            }
            val placeables = measurables.map { it.measure(constraints) }
            val heightPx = placeables.firstOrNull()?.height?.toFloat() ?: 0f
            itemHeightPx = heightPx
            itemHeightDp = with(density) { heightPx.toDp() }
        }

        layout(0, 0) {}
    }

    /** 초기 스크롤 위치를 계산하여 수행합니다. */
    LaunchedEffect(pickerHeightPx) {
        if (pickerHeightPx <= 0f) return@LaunchedEffect

        val targetIndex = items.indexOf(initialItem)

        /** initialItem이 items 내부에 존재하지 않아 targetIndex가 정수로 반환되지 않을 경우, 안전하게 0을 반환토록 설계합니다. */
        val safeTargetIndex = if (targetIndex >= 0) targetIndex else 0

        lastSelectedIndex = safeTargetIndex
        scrollState.scrollToItem(safeTargetIndex)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val centerY = size.height / 2f
                val rectTop = centerY - (itemHeightPx / 2f)

                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Transparent,
                        0.3f to Black,
                        0.7f to Black,
                        1.0f to Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )

                drawRoundRect(
                    color = Gray.copy(alpha = 0.2f),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    blendMode = BlendMode.Multiply,
                    topLeft = Offset(0f, rectTop),
                    size = Size(size.width, itemHeightPx)
                )
            }
            .onGloballyPositioned { coordinates ->
                val actualHeightPx = coordinates.size.height.toFloat()
                if (actualHeightPx > 0f && pickerHeightPx != actualHeightPx) {
                    pickerHeightPx = actualHeightPx
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (itemHeightPx > 0f && pickerHeightPx > 0f) {
            val fadeHeightDp = with(density) { ((pickerHeightPx - itemHeightPx) / 2f).toDp() }

            LazyColumn(
                Modifier.fillMaxSize(),
                state = scrollState,
                flingBehavior = rememberSnapFlingBehavior(scrollState),
                /** 양끝 값 또한 스크롤하여 선택할 수 있도록 상 하단에 (전체높이 - 단일 아이템 높이)/2 만큼 높이 패딩을 줍니다. */
                contentPadding = PaddingValues(vertical = fadeHeightDp)
            ) {
                items(
                    count = items.size,
                    itemContent = { i ->
                        val item = items[i]

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(itemHeightDp)
                                .pointerInput(i) {
                                    detectTapGestures(
                                        onTap = {
                                            coroutineScope.launch {
                                                scrollState.animateScrollToItem(i)
                                            }
                                        }
                                    )
                                }
                                .onGloballyPositioned { coordinates ->
                                    /** 아래 상황에서 호출됩니다.
                                     * 1. 초기 레이아웃 배치 시, 모든 아이템들의 위치 계산되며 호출
                                     * 2. 위치나 크기가 변경될 때 (스크롤로 위치 변경됨)
                                     * 3. 재평가 후 실제 레이아웃이 변경된 경우,
                                     * 4. scrollToItem 실행하며, 아이템들의 위치가 변경될 때.*/
                                    val y = (coordinates.positionInParent().y) + (itemHeightPx / 2f)
                                    val parentHalfHeight = (pickerHeightPx / 2f)
                                    val isCurrentlySelected =
                                        abs(parentHalfHeight - y) <= (itemHeightPx / 2f)

                                    if (isCurrentlySelected && lastSelectedIndex != i && item.isNotEmpty()) {
                                        onItemSelected(i, item)
                                        lastSelectedIndex = i
                                    }
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            content(item, lastSelectedIndex == i)
                        }
                    }
                )
            }
        }
    }
}
