package com.humanics.exampleapplication.component

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()

    val itemHeight = 36.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }

    /** 최적화 1: onGloballyPositioned 제거 — LazyListState의 layoutInfo에서 뷰포트 중앙에 가장 가까운 아이템을 파생합니다. */
    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset +
                (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
            layoutInfo.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2f) - viewportCenter)
            }?.index ?: 0
        }
    }

    /** selectedIndex 변경 시 콜백을 호출합니다. snapshotFlow + distinctUntilChanged로 중복 호출을 방지합니다. */
    LaunchedEffect(items) {
        snapshotFlow { selectedIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index in items.indices && items[index].isNotEmpty()) {
                    onItemSelected(index, items[index])
                }
            }
    }

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
            scrollState.scrollToItem(safeTargetIndex)
        }

        val pickerHeightDp = with(density) { currentPickerHeightPx.toDp() }
        val fadeHeightDp = with(density) { ((currentPickerHeightPx - itemHeightPx) / 2f).toDp() }

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

                    /** 최적화 2: 아이템별 derivedStateOf — selectedIndex가 변경되어도 실제로 isSelected 값이 바뀐 아이템(이전/현재 2개)만 리컴포지션됩니다. */
                    val isSelected by remember {
                        derivedStateOf { selectedIndex == i }
                    }

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
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        content(item, isSelected)
                    }
                }
            )
        }
    }
}
