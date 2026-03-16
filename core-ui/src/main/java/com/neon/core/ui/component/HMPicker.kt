package com.neon.core.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray20
import kotlinx.coroutines.launch

/**
 * iOS 스타일의 Wheel 피커 컴포넌트.
 *
 * LazyColumn 기반의 스냅 스크롤로 구현되어, 사용자가 스크롤하면 가장 가까운 아이템에
 * 자동으로 스냅됩니다. 상하 가장자리에는 페이드 그라데이션이 적용되고,
 * 선택된 아이템 영역에는 [Gray20] 배경의 하이라이트가 표시됩니다.
 *
 * [content] 람다를 통해 각 아이템의 렌더링 방식을 커스터마이징할 수 있으며,
 * 선택 여부(Boolean)가 두 번째 파라미터로 전달됩니다.
 *
 * 초기 스크롤 위치는 [initialItem]과 일치하는 아이템으로 자동 설정됩니다.
 * 스크롤이 멈출 때마다 [onItemSelected]가 호출되어 선택된 아이템의 인덱스와 값이 전달됩니다.
 *
 * @param modifier 피커에 적용할 Modifier.
 * @param items 피커에 표시할 문자열 아이템 목록.
 * @param initialItem 초기 선택 아이템. items 목록에 포함되어야 합니다.
 * @param onItemSelected 아이템 선택 시 호출되는 콜백. (인덱스, 값) 쌍이 전달됩니다.
 * @param content 각 아이템을 렌더링하는 컴포저블. (아이템 문자열, 선택 여부)가 전달됩니다.
 */
@Composable
fun HMPicker(
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

    val itemHeight = 36.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }

    BoxWithConstraints(
        modifier
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
            },
        contentAlignment = Alignment.Center
    ) {
        val availableHeight = this.constraints.maxHeight.toFloat()

        /** 부모 뷰로부터 할당받은 영역을 최대한 활용토록 하되, 220.dp를 상한선으로 설정합니다. */
        val currentPickerHeightPx = if (availableHeight == Constraints.Infinity.toFloat()) {
            with(density) { 220.dp.toPx() }
        } else {
            availableHeight
        }

        /** 초기 스크롤 위치를 계산하여 수행합니다. */
        LaunchedEffect(currentPickerHeightPx) {
            val targetIndex = items.indexOf(initialItem)

            /** initialItem이 items 내부에 존재하지 않아 targetIndex가 정수로 반환되지 않을 경우, 안전하게 0을 반환토록 설계합니다. */
            val safeTargetIndex = if (targetIndex >= 0) targetIndex else 0

            lastSelectedIndex = safeTargetIndex
            scrollState.scrollToItem(safeTargetIndex)
        }

        val pickerHeightDp = with(density) { currentPickerHeightPx.toDp() }
        val fadeHeightDp = with(density) { ((currentPickerHeightPx - itemHeightPx) / 2f).toDp() }

        // 리스트 본문
        LazyColumn(
            Modifier.fillMaxWidth().height(pickerHeightDp),
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
                            .height(itemHeight)
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
                                val parentHalfHeight = (currentPickerHeightPx / 2f)
                                val isCurrentlySelected =
                                    kotlin.math.abs(parentHalfHeight - y) <= (itemHeightPx / 2f)

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
