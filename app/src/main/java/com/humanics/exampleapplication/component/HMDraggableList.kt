package com.humanics.exampleapplication.component

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.abs

/**
 * HMDraggableList - Column 기반 드래그 앤 드롭 리스트 컴포넌트
 *
 * Column + verticalScroll 구조를 사용하여 key 기반 스크롤 추적 문제를 근본적으로 해결.
 * ScrollState는 pixel offset 기반이므로 아이템 재정렬 시 스크롤 위치가 자동으로 유지됨.
 *
 * @param items 리스트에 표시할 아이템 목록
 * @param rowHeight 각 행의 고정 높이
 * @param isDragEnabled 드래그 기능 활성화 여부
 * @param onReorder 아이템 재정렬 완료 시 호출되는 콜백 (item, targetIndex)
 * @param onTapRow 행 탭 시 호출되는 콜백
 * @param itemContent 각 아이템의 컨텐츠를 생성하는 composable (item, isDragging)
 * @param header 리스트 상단에 표시할 헤더 컴포저블
 * @param footer 리스트 하단에 표시할 푸터 컴포저블
 * @param scrollState ScrollState (외부에서 주입 가능)
 * @param modifier Modifier
 */
interface Draggable {
    val id: Int
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : Draggable> HMDraggableList(
    items: List<T>,
    rowHeight: Dp,
    isDragEnabled: Boolean,
    onReorder: (T, Int) -> Unit,
    onTapRow: (T) -> Unit,
    itemContent: @Composable (T, Boolean) -> Unit,
    header: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val overallHeightPx = with(density) { (rowHeight + DROP_INDICATOR_HEIGHT + DROP_INDICATOR_ROW_GAP).toPx() }

    // 드롭 타겟 인덱스 (items 리스트 기준, 0..items.size)
    var targetedDropIndex by remember { mutableStateOf<Int?>(null) }
    var prevTargetedDropIndex by remember { mutableStateOf<Int?>(null) }

    // 자동 스크롤 속도
    var autoScrollVelocity by remember { mutableFloatStateOf(0f) }

    // 마지막 드래그 Y 좌표 (Window 기준)
    var lastDragY by remember { mutableStateOf<Float?>(null) }

    // 드래그 중인 아이템 ID 추적 (시각적 피드백용)
    var draggingItemId by remember { mutableStateOf<Int?>(null) }

    // 컨테이너의 Window 기준 Y 오프셋 및 높이
    var containerTopOffset by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }

    // 헤더 높이 (computeTargetIndex에서 사용)
    var headerHeightPx by remember { mutableFloatStateOf(0f) }

    // ========================================
    // 자동 스크롤 속도 계산
    // ========================================
    fun updateAutoScrollVelocity() {
        val y = lastDragY
        if (y == null) {
            autoScrollVelocity = 0f
            return
        }
        val toTop = y - containerTopOffset
        val toBottom = (containerTopOffset + containerHeight) - y

        val scrollThresholdPx = with(density) { SCROLL_THRESHOLD_DP.toPx() }
        val headerThresholdPx = with(density) { HEADER_THRESHOLD_DP.toPx() }

        autoScrollVelocity = when {
            toTop < (scrollThresholdPx + headerThresholdPx) -> -SCROLL_VELOCITY
            toBottom < scrollThresholdPx -> SCROLL_VELOCITY
            else -> 0f
        }
    }

    // ========================================
    // 프레임 동기화 자동 스크롤 루프
    // ========================================
    LaunchedEffect(scrollState) {
        var lastTime = 0L
        while (isActive) {
            val dtNanos = withFrameNanos { now ->
                val dt = if (lastTime == 0L) 0L else now - lastTime
                lastTime = now
                dt
            }
            val v = autoScrollVelocity
            if (v != 0f && dtNanos > 0L) {
                val dtSec = dtNanos / 1_000_000_000f
                val delta = v * dtSec
                val consumed = scrollState.scrollBy(delta)
                if (abs(consumed) < 0.5f) {
                    autoScrollVelocity = 0f
                }
            }
        }
    }

    LaunchedEffect(targetedDropIndex) {
        if (prevTargetedDropIndex != targetedDropIndex && targetedDropIndex != null) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        prevTargetedDropIndex = targetedDropIndex
    }

    // ========================================
    // 드롭 타겟 인덱스 계산 (items 리스트 기준 인덱스 반환)
    //
    // Window Y → 컨테이너 로컬 Y → 스크롤 보정한 절대 Y → 헤더 제외 → rowHeight로 인덱스 계산
    // ========================================
    fun computeTargetIndex(y: Float): Int {
        val localY = y - containerTopOffset
        val absoluteY = localY + scrollState.value
        val relativeY = absoluteY - headerHeightPx
        if (relativeY <= 0f) return 0

        val index = (relativeY / overallHeightPx).toInt()
        val midOffset = relativeY - (index * overallHeightPx)
        val adjustedIndex = if (midOffset > overallHeightPx / 2f) index + 1 else index
        return adjustedIndex.coerceIn(0, items.size)
    }

    fun moveItem(item: T, targetItemsIndex: Int) {
        val currentIndex = items.indexOfFirst { it.id == item.id }
        if (currentIndex == -1) return

        val coercedTarget = targetItemsIndex.coerceIn(0, items.size)
        val insertIndex = if (coercedTarget > currentIndex) coercedTarget - 1 else coercedTarget
        onReorder(item, insertIndex)
    }

    // region SubComponent
    @Composable fun dropIndicator() {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(DROP_INDICATOR_HEIGHT)
                .background(Color.Blue, RoundedCornerShape(4.dp))
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerTopOffset = coordinates.positionInWindow().y
                containerHeight = coordinates.size.height.toFloat()
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = remember {
                    object : DragAndDropTarget {
                        override fun onEntered(event: DragAndDropEvent) {
                            val y = event.toAndroidDragEvent().y
                            lastDragY = y
                            targetedDropIndex = computeTargetIndex(y)
                        }

                        override fun onMoved(event: DragAndDropEvent) {
                            val y = event.toAndroidDragEvent().y
                            lastDragY = y
                            updateAutoScrollVelocity()
                            targetedDropIndex = computeTargetIndex(y)
                        }

                        override fun onExited(event: DragAndDropEvent) {
                            autoScrollVelocity = 0f
                            lastDragY = null
                            targetedDropIndex = null
                            draggingItemId = null
                        }

                        override fun onEnded(event: DragAndDropEvent) {
                            autoScrollVelocity = 0f
                            lastDragY = null
                            targetedDropIndex = null
                            draggingItemId = null
                        }

                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val text = event.toAndroidDragEvent().clipData
                                ?.getItemAt(0)?.text?.toString()
                            val draggedId = text?.toIntOrNull() ?: return false
                            val droppedItem = items.firstOrNull { it.id == draggedId }
                                ?: return false

                            val y = event.toAndroidDragEvent().y
                            moveItem(droppedItem, computeTargetIndex(y))
                            targetedDropIndex = null
                            draggingItemId = null
                            return true
                        }
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            header?.let {
                Box(Modifier.onGloballyPositioned { coordinates ->
                    headerHeightPx = coordinates.size.height.toFloat()
                }
                ) { it() }
            }

            // Items with drop indicators + reorder animation
            items.forEachIndexed { index, item ->
                key(item.id) {
                    // 리오더 애니메이션: 인덱스 변경 시 이전 위치에서 새 위치로 슬라이드
                    var previousIndex by remember { mutableIntStateOf(index) }
                    val offsetY = remember { Animatable(0f) }

                    LaunchedEffect(index) {
                        if (previousIndex != index) {
                            // index * Row 높이로 하여 애니메이션이 진행될 offsetPx을 계산
                            val delta = (previousIndex - index) * overallHeightPx
                            // 기존 변경된 위치에서,
                            offsetY.snapTo(delta)
                            // 새로 변경된 위치로 애니메이션 진행
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = 300f
                                )
                            )
                            previousIndex = index
                        }
                    }

                    val isLast = index==(items.size-1)
                    Column(
                        Modifier
                            .height(rowHeight+ DROP_INDICATOR_HEIGHT + DROP_INDICATOR_ROW_GAP +
                                    (if (isLast) (DROP_INDICATOR_HEIGHT + DROP_INDICATOR_ROW_GAP) else 0.dp)
                            ) // 순수 Row Height + DropIndicator
                            .fillMaxWidth()
                            .graphicsLayer { translationY = offsetY.value }
                            .then(
                                if (isDragEnabled) {
                                    Modifier.dragAndDropSource {
                                        detectTapGestures(
                                            onTap = { onTapRow(item) },
                                            onLongPress = {
                                                draggingItemId = item.id
                                                startTransfer(
                                                    transferData = DragAndDropTransferData(
                                                        clipData = ClipData.newPlainText(
                                                            "draggable-item-id",
                                                            item.id.toString()
                                                        )
                                                    )
                                                )
                                            }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        verticalArrangement = Arrangement.spacedBy(DROP_INDICATOR_ROW_GAP)
                    ) {
                        val transitionDuration = 120
                        AnimatedVisibility(
                            visible = targetedDropIndex == index,
                            enter = expandVertically(tween(transitionDuration)),
                            exit = shrinkVertically(tween(transitionDuration)),
                        ) {
                            dropIndicator()
                        }
                        Column(Modifier.height(rowHeight)) {
                            itemContent(item, draggingItemId == item.id)
                        }
                        AnimatedVisibility(
                            visible = isLast && targetedDropIndex == index + 1,
                            enter = expandVertically(tween(transitionDuration)),
                            exit = shrinkVertically(tween(transitionDuration)),
                        ) {
                            dropIndicator()
                        }
                    }
                }
            }
            footer?.let { it() }
        }
    }
}

private val SCROLL_THRESHOLD_DP = 96.dp
private val HEADER_THRESHOLD_DP = 64.dp
private val DROP_INDICATOR_HEIGHT = 12.dp
private val DROP_INDICATOR_ROW_GAP = 4.dp
private const val SCROLL_VELOCITY = 1200f
