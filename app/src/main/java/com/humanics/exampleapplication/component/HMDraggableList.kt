package com.humanics.exampleapplication.component

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.humanics.exampleapplication.model.Draggable
import kotlinx.coroutines.isActive
import kotlin.math.abs

/**
 * HMDraggableList - Humania-android의 프로덕션 DnD 컴포넌트
 *
 * iOS HMDraggableScrollView와 동일한 인터페이스를 제공하는 재사용 가능한 드래그 앤 드롭 리스트 컴포넌트
 *
 * === 레거시 코드 대비 개선점 ===
 *
 * [개선 1] 재사용 가능한 제네릭 컴포넌트
 * - 기존: RoutineDetailView에 DnD 로직이 직접 구현되어 있었음
 * - 개선: 제네릭 타입 T를 사용하여 어떤 데이터 타입에도 적용 가능
 *
 * [개선 2] 햅틱 피드백 최적화
 * - 기존: 햅틱 피드백 없음
 * - 개선: targetedDropIndex 변경 시에만 햅틱 피드백 (prevTargetedDropIndex 비교)
 *
 * [개선 3] 프레임 동기화 자동 스크롤
 * - 기존: delay(16)으로 약 60fps 업데이트 (정확하지 않음)
 * - 개선: withFrameNanos 사용으로 정확한 프레임 동기화
 *
 * [개선 4] 드래그 중인 아이템 추적
 * - 기존: 드래그 중인 아이템 시각적 구분 없음
 * - 개선: draggingItemId로 추적하여 opacity 0.8 적용
 *
 * [개선 6] Header/Footer 지원
 * - 기존: 리스트 아이템만 표시
 * - 개선: header, footer 컴포저블 슬롯 제공
 *
 * [개선 7] 외부에서 주입 가능한 파라미터들
 * - rowHeight: 각 행의 높이
 * - headerOffset: 드롭 인덱스 계산 시 헤더 오프셋
 * - listState: LazyListState 외부 주입 가능
 *
 * @param items 리스트에 표시할 아이템 목록
 * @param rowHeight 각 행의 고정 높이
 * @param isDragEnabled 드래그 기능 활성화 여부
 * @param onReorder 아이템 재정렬 완료 시 호출되는 콜백
 * @param onTapRow 행 탭 시 호출되는 콜백
 * @param onMoveItem 아이템 이동 시 호출되는 콜백 (item, targetIndex)
 * @param itemContent 각 아이템의 컨텐츠를 생성하는 composable (item, isDragging)
 * @param header 리스트 상단에 표시할 헤더 컴포저블
 * @param footer 리스트 하단에 표시할 푸터 컴포저블
 * @param listState LazyListState (외부에서 주입 가능)
 * @param modifier Modifier
 */
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
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    // 드롭 타겟 인덱스 (시각적 피드백용)
    var targetedDropIndex by remember { mutableStateOf<Int?>(null) }

    // [개선] 이전 드롭 인덱스 (햅틱 피드백 최적화용)
    var prevTargetedDropIndex by remember { mutableStateOf<Int?>(null) }

    // 자동 스크롤 속도
    var autoScrollVelocity by remember { mutableFloatStateOf(0f) }

    // 마지막 드래그 Y 좌표
    var lastDragY by remember { mutableStateOf<Float?>(null) }

    // [개선] 드래그 중인 아이템 ID 추적 (시각적 피드백용)
    var draggingItemId by remember { mutableStateOf<Int?>(null) }

    // [개선] 컨테이너의 Window 기준 Y 오프셋 측정
    // DragEvent.y는 Window 기준 좌표이므로, Box의 위치를 빼야 로컬 좌표가 됨
    var containerTopOffset by remember { mutableFloatStateOf(0f) }

    // ========================================
    // 자동 스크롤 속도 계산용 DnD 상태
    // ========================================
    val dndState = rememberDragAndDropListState(lazyListState = listState) { _, _ ->
        val y = lastDragY
        if (y == null) {
            autoScrollVelocity = 0f
            return@rememberDragAndDropListState
        }
        val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()

        val toTop = y - viewportStart
        val toBottom = viewportEnd - y

        // 상수로 분리 (레거시의 매직 넘버 문제 해결)
        val scrollThresholdPx = with(density) { SCROLL_THRESHOLD_DP.toPx() }
        val headerThresholdPx = with(density) { HEADER_THRESHOLD_DP.toPx() }

        autoScrollVelocity = when {
            toTop < (scrollThresholdPx + headerThresholdPx) -> -SCROLL_VELOCITY
            toBottom < scrollThresholdPx -> SCROLL_VELOCITY
            else -> 0f
        }
    }

    // ========================================
    // [개선] 프레임 동기화 자동 스크롤 루프
    // 레거시: delay(16)으로 약 60fps (부정확)
    // 개선: withFrameNanos로 정확한 프레임 동기화
    // ========================================
    LaunchedEffect(listState) {
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
                val consumed = listState.scrollBy(delta)
                // 스크롤 끝에 도달하면 속도 리셋
                if (abs(consumed) < 0.5f) {
                    autoScrollVelocity = 0f
                }
            }
        }
    }

    // ========================================
    // [개선] targetedDropIndex 변경 시 햅틱 피드백
    // 레거시: 햅틱 피드백 없음
    // 개선: 인덱스 변경 시에만 피드백 (중복 방지)
    // ========================================
    LaunchedEffect(targetedDropIndex) {
        if (prevTargetedDropIndex != targetedDropIndex && targetedDropIndex != null) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        prevTargetedDropIndex = targetedDropIndex
    }

    // ========================================
    // 드롭 타겟 인덱스 계산 함수
    //
    // [핵심] DragEvent.y는 Window 기준 좌표
    // - containerTopOffset을 빼서 Box 로컬 좌표로 변환
    // - visibleItemsInfo.offset은 LazyColumn 내부 좌표 (Box와 동일)
    //
    // [개선] rowHeight를 활용한 중간점 계산
    // ========================================
    fun computeTargetIndex(y: Float): Int {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 0

        // Window 좌표 → Box 로컬 좌표 변환
        val localY = y - containerTopOffset

        // 각 아이템의 중간점과 비교하여 타겟 인덱스 결정
        for (info in visibleItems) {
            val mid = info.offset + (info.size / 2f)
            if (localY < mid) return info.index
        }

        // 모든 가시 아이템 아래에 있는 경우
        return layoutInfo.totalItemsCount
    }

    fun moveItem(item: T, targetLazyIndex: Int) {
        val headerCount = if (header != null) 1 else 0
        val currentIndex = items.indexOfFirst { it.id == item.id }
        if (currentIndex == -1) return

        // LazyIndex를 items 리스트 상대 인덱스로 변환
        val targetItemsIndex = (targetLazyIndex - headerCount).coerceIn(0, items.size)

        // 이동 후의 최종 인덱스 계산
        // - targetItemsIndex가 currentIndex보다 크면, 아이템이 빠지면서 하나씩 당겨지므로 -1
        // - 예: [A, B, C]에서 A(0)를 C(2) 위치로 옮기면, A 삭제([B, C]) 후 index 1에 삽입([B, A, C])
        val insertIndex = if (targetItemsIndex > currentIndex) targetItemsIndex - 1 else targetItemsIndex

        if (insertIndex == currentIndex) return

        onReorder(item, insertIndex)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerTopOffset = coordinates.positionInWindow().y
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
                            dndState.onDragStart(Offset(0f, y))
                            targetedDropIndex = computeTargetIndex(y)
                        }

                        override fun onMoved(event: DragAndDropEvent) {
                            val y = event.toAndroidDragEvent().y
                            val last = lastDragY
                            if (last != null) {
                                val dy = y - last
                                dndState.onDrag(Offset(0f, dy))
                            }
                            lastDragY = y
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
                            dndState.onDragInterrupted()
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            header?.let { item(key = "header") { it() } }

            // Items with drop indicators
            // iOS HMDraggableScrollView와 동일한 로직:
            // - 각 아이템의 위/아래에 인디케이터 표시 가능
            // - targetedDropIndex == index: 아이템 위에 표시
            // - targetedDropIndex == index + 1: 아이템 아래에 표시 (마지막 포함)
            itemsIndexed(
                items = items,
                key = { _, item -> item.id }
            ) { index, item ->
                val itemId = item.id
                val isDragging = draggingItemId == itemId
                val isFirstItem = index == 0
                val isLastItem = index == items.size - 1

                Column(Modifier.animateItem()) {
                    // 아이템 위쪽 인디케이터
                    // [개선] targetedDropIndex(LazyColumn index)를 items 리스트 상대 인덱스로 변환
                    val adjustedDropIndex = targetedDropIndex?.let {
                        (it - if (header != null) 1 else 0).coerceIn(0, items.size)
                    }

                    if (adjustedDropIndex == index) {
                        DropIndicator(
                            isTopRounded = isFirstItem,
                            isBottomRounded = false
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(rowHeight)
                            .then(
                                if (isDragEnabled) {
                                    Modifier.dragAndDropSource {
                                        detectTapGestures(
                                            onTap = {
                                                onTapRow(item)
                                            },
                                            onLongPress = {
                                                draggingItemId = itemId
                                                startTransfer(
                                                    transferData = DragAndDropTransferData(
                                                        clipData = ClipData.newPlainText(
                                                            "draggable-item-id",
                                                            itemId.toString()
                                                        )
                                                    )
                                                )
                                            }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        itemContent(item, isDragging)
                    }

                    // 아이템 아래쪽 인디케이터 (마지막 아이템 뒤 포함)
                    if (adjustedDropIndex == index + 1) {
                        DropIndicator(
                            isTopRounded = false,
                            isBottomRounded = isLastItem
                        )
                    }
                }
            }

            footer?.let { item(key = "footer") { it() } }
        }
    }
}

/**
 * 드롭 위치를 나타내는 인디케이터
 *
 * iOS HMDraggableScrollView의 UnevenRoundedRectangle과 동일한 모서리 처리:
 * - 첫 번째 아이템 위: 상단 모서리 4dp, 하단 모서리 4dp
 * - 마지막 아이템 아래: 상단 모서리 4dp, 하단 모서리 4dp
 * - 중간: 상단 0dp, 하단 4dp (위쪽) / 상단 4dp, 하단 0dp (아래쪽)
 *
 * @param isTopRounded 상단 모서리를 둥글게 할지 여부
 * @param isBottomRounded 하단 모서리를 둥글게 할지 여부
 */
@Composable
private fun DropIndicator(
    isTopRounded: Boolean = false,
    isBottomRounded: Boolean = false
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(12.dp)
            .background(
                color = Color.Blue,
                shape = RoundedCornerShape(
                    topStart = if (isTopRounded) 4.dp else 0.dp,
                    topEnd = if (isTopRounded) 4.dp else 0.dp,
                    bottomStart = if (isBottomRounded) 4.dp else 0.dp,
                    bottomEnd = if (isBottomRounded) 4.dp else 0.dp
                )
            )
    )
}

// ========================================
// [개선] 상수 분리 (레거시의 매직 넘버 문제 해결)
// ========================================
private val SCROLL_THRESHOLD_DP = 96.dp
private val HEADER_THRESHOLD_DP = 64.dp
private const val SCROLL_VELOCITY = 1200f
