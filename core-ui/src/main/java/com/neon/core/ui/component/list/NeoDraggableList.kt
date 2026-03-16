package com.neon.core.ui.component.list

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray25
import com.neon.core.ui.theme.Primary10
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 드래그 앤 드롭으로 아이템 순서를 변경할 수 있는 리스트 컴포넌트.
 *
 * Column + verticalScroll 구조를 사용하여 LazyColumn의 key 기반 스크롤 추적 문제를
 * 근본적으로 해결합니다. ScrollState는 pixel offset 기반이므로 아이템 재정렬 시
 * 스크롤 위치가 자동으로 유지됩니다.
 *
 * 주요 기능:
 * - Android의 Drag and Drop API를 사용하여 네이티브 드래그 동작을 지원합니다.
 * - 드래그 시 해당 행이 [Primary10] 배경으로 하이라이트되고, 햅틱 피드백이 제공됩니다.
 * - 드래그 중 리스트 상하단에 도달하면 자동 스크롤이 활성화됩니다.
 * - 아이템 순서 변경 시 spring 애니메이션으로 부드러운 전환 효과가 적용됩니다.
 * - [isDragEnabled]가 false이면 드래그가 비활성화되고 탭만 동작합니다.
 *
 * 아이템은 [Draggable] 인터페이스를 구현해야 하며, [id] 프로퍼티로 각 아이템을 식별합니다.
 *
 * @param T [Draggable]을 구현하는 아이템 타입.
 * @param modifier 리스트에 적용할 Modifier.
 * @param items 리스트에 표시할 아이템 목록.
 * @param rowHeight 각 행의 고정 높이. 드래그 위치 계산에 사용됩니다.
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
fun <T : Draggable> NeoDraggableList(
    modifier: Modifier = Modifier,
    items: List<T>,
    rowHeight: Dp,
    isDragEnabled: Boolean,
    onReorder: (T, Int) -> Unit,
    onTapRow: (T) -> Unit,
    itemContent: @Composable (T, Boolean) -> Unit,
    header: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val overallHeightPx = with(density) { (rowHeight + DROP_INDICATOR_HEIGHT + DROP_INDICATOR_ROW_GAP).toPx() }

    // remember {} 내부 콜백에서 항상 최신 값을 참조하기 위한 State wrapper.
    // rememberUpdatedState는 동일한 State 객체를 유지하면서 .value만 매 recomposition마다 갱신하므로,
    // remember {} 에 캡처된 stale closure에서도 .value 읽기 시 최신 값을 반환합니다.
    val currentItems by rememberUpdatedState(items)
    val currentOnReorder by rememberUpdatedState(onReorder)

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
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevTargetedDropIndex = targetedDropIndex
    }

    // ========================================
    // 드롭 타겟 인덱스 계산 (items 리스트 기준 인덱스 반환)
    //
    // Window Y → 컨테이너 로컬 Y → 스크롤 보정한 절대 Y → 헤더 제외 → rowHeight로 인덱스 계산
    // ========================================
    fun computeTargetIndex(y: Float): Int {
        return DraggableListLogic.computeTargetSlot(
            windowY = y,
            containerTopOffset = containerTopOffset,
            scrollOffset = scrollState.value,
            headerHeightPx = headerHeightPx,
            rowSlotHeightPx = overallHeightPx,
            itemCount = currentItems.size
        )
    }

    fun moveItem(item: T, targetItemsIndex: Int) {
        val currentIndex = currentItems.indexOfFirst { it.id == item.id }
        if (currentIndex == -1) return
        val insertIndex = DraggableListLogic.computeInsertIndex(
            currentIndex = currentIndex,
            targetSlot = targetItemsIndex,
            itemCount = currentItems.size
        )
        currentOnReorder(item, insertIndex)
    }

    // region SubComponent
    @Composable fun dropIndicator() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DROP_INDICATOR_HEIGHT)
                .background(Primary10, RoundedCornerShape(4.dp))
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerTopOffset = coordinates.positionInRoot().y
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
                            val droppedItem = currentItems.firstOrNull { it.id == draggedId } ?: return false

                            val dropSlot = targetedDropIndex ?: computeTargetIndex(event.toAndroidDragEvent().y)
                            moveItem(droppedItem, dropSlot)
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

                    // 눌림 효과를 위한 scale 애니메이션
                    val scale = remember { Animatable(1f) }
                    // Press 시 배경 alpha 애니메이션
                    val backgroundAlpha = remember { Animatable(0f) }
                    val coroutineScope = rememberCoroutineScope()

                    Column(
                        Modifier
                            .height(rowHeight+ DROP_INDICATOR_HEIGHT + DROP_INDICATOR_ROW_GAP +
                                    (if (isLast) (DROP_INDICATOR_HEIGHT + DROP_INDICATOR_ROW_GAP) else 0.dp)
                            ) // 순수 Row Height + DropIndicator
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                Gray25.copy(alpha = backgroundAlpha.value),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp)
                            .graphicsLayer {
                                alpha = 1f - backgroundAlpha.value
                                translationY = offsetY.value
                                scaleX = scale.value
                                scaleY = scale.value
                            }
                            .then(
                                if (isDragEnabled) {
                                    Modifier
                                        .dragAndDropSource {
                                            detectTapGestures(
                                                onPress = {
                                                    // Press 시작 시 축소 및 배경 표시
                                                    coroutineScope.launch {
                                                        scale.animateTo(
                                                            targetValue = 0.97f,
                                                            animationSpec = tween(100)
                                                        )
                                                    }
                                                    coroutineScope.launch {
                                                        backgroundAlpha.animateTo(
                                                            targetValue = 0.3f,
                                                            animationSpec = tween(100)
                                                        )
                                                    }
                                                    // Press가 끝날 때까지 대기
                                                    tryAwaitRelease()
                                                    // Release 시 원래 크기로 복원 및 배경 숨김
                                                    coroutineScope.launch {
                                                        scale.animateTo(
                                                            targetValue = 1f,
                                                            animationSpec = tween(400)
                                                        )
                                                    }
                                                    coroutineScope.launch {
                                                        backgroundAlpha.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = tween(400)
                                                        )
                                                    }
                                                },
                                                onTap = { onTapRow(item) },
                                                onLongPress = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
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

/**
 * DnD 재정렬 로직의 순수 계산 함수.
 * Composable 내부 로컬 함수와 동일한 로직을 외부에서 테스트할 수 있도록 분리.
 */
internal object DraggableListLogic {

    /**
     * Window Y 좌표 → 드롭 슬롯 인덱스 (0..itemCount) 변환.
     * Composable 내부 computeTargetIndex와 동일한 로직.
     */
    fun computeTargetSlot(
        windowY: Float,
        containerTopOffset: Float,
        scrollOffset: Int,
        headerHeightPx: Float,
        rowSlotHeightPx: Float,
        itemCount: Int
    ): Int {
        val localY = windowY - containerTopOffset
        val absoluteY = localY + scrollOffset
        val relativeY = absoluteY - headerHeightPx
        if (relativeY <= 0f) return 0

        val index = (relativeY / rowSlotHeightPx).toInt()
        val midOffset = relativeY - (index * rowSlotHeightPx)
        val adjustedIndex = if (midOffset > rowSlotHeightPx / 2f) index + 1 else index
        return adjustedIndex.coerceIn(0, itemCount)
    }

    /**
     * 드롭 슬롯 → 실제 삽입 인덱스 변환.
     * Composable 내부 moveItem과 동일한 로직.
     */
    fun computeInsertIndex(
        currentIndex: Int,
        targetSlot: Int,
        itemCount: Int
    ): Int {
        val coercedTarget = targetSlot.coerceIn(0, itemCount)
        return if (coercedTarget > currentIndex) coercedTarget - 1 else coercedTarget
    }
}
