package com.humanics.exampleapplication

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.humanics.exampleapplication.component.DemoItemRow
import com.humanics.exampleapplication.model.DemoItem
import com.humanics.exampleapplication.model.generateSampleItems
import kotlinx.coroutines.delay

/**
 * DragAndDropDemoView (Column 버전)
 *
 * LazyColumn 대신 스크롤 가능한 Column을 사용한 DnD 데모
 *
 * === LazyColumn → Column 변경으로 간소화된 부분 ===
 *
 * [간소화 1] computeTargetIndex가 단순 수식으로 변경
 * - 기존: visibleItemsInfo 순회하며 각 아이템의 offset/size 비교
 * - 변경: (y + scrollOffset) / rowHeight 로 단순 계산
 *
 * [간소화 2] DragAndDropListState 헬퍼 제거
 * - 기존: LazyListState 기반의 복잡한 상태 관리
 * - 변경: ScrollState만으로 충분
 *
 * [간소화 3] 좌표계 단순화
 * - 기존: viewportStartOffset, visibleItemsInfo 등 복잡한 좌표 변환
 * - 변경: scrollState.value + localY로 단순화
 *
 * === 주의사항 ===
 * - 아이템 수가 많으면 성능 저하 (모든 아이템이 렌더링됨)
 * - animateItemPlacement() 사용 불가
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DragAndDropDemoView() {
    val rowHeight: Dp = 72.dp
    val density = LocalDensity.current
    val rowHeightPx = with(density) { rowHeight.toPx() }

    var items by remember { mutableStateOf(generateSampleItems()) }

    val scrollState = rememberScrollState()
    // 드롭 타겟 인덱스
    var targetedDropIndex by remember { mutableStateOf<Int?>(null) }
    var autoScrollVelocity by remember { mutableFloatStateOf(0f) }

    // 마지막 드래그 Y 좌표 (Box 로컬 좌표)
    var lastDragY by remember { mutableStateOf<Float?>(null) }

    // Box의 Window 기준 Y 오프셋
    var containerTopOffset by remember { mutableFloatStateOf(0f) }
    val currentContainerTopOffset by rememberUpdatedState(containerTopOffset)

    // Box의 높이 (자동 스크롤 임계값 계산용)
    var containerHeight by remember { mutableFloatStateOf(0f) }
    val currentContainerHeight by rememberUpdatedState(containerHeight)

    // 현재 스크롤 위치 (remember 블록 내에서 최신 값 참조용)
    val currentScrollOffset by rememberUpdatedState(scrollState.value)

    // 아이템 개수 (remember 블록 내에서 최신 값 참조용)
    val currentItemCount by rememberUpdatedState(items.size)

    /**
     * 자동 스크롤 속도 계산
     * Column에서는 scrollState.value와 containerHeight를 기준으로 계산
     */
    fun updateAutoScrollVelocity(localY: Float) {
        val scrollThresholdPx = with(density) { 96.dp.toPx() }

        val toTop = localY
        val toBottom = currentContainerHeight - localY

        autoScrollVelocity = when {
            toTop < scrollThresholdPx -> -1200f  // 위로 스크롤
            toBottom < scrollThresholdPx -> 1200f  // 아래로 스크롤
            else -> 0f
        }
    }

    /**
     * 드롭 타겟 인덱스 계산 (간소화된 버전)
     *
     * [개선] visibleItemsInfo 순회 대신 단순 수식 사용
     * - scrollState.value: 현재 스크롤 오프셋
     * - localY: Box 내 로컬 Y 좌표
     * - (scrollOffset + localY) / rowHeight = 절대 위치 기준 인덱스
     */
    fun computeTargetIndex(localY: Float): Int {
        val absoluteY = currentScrollOffset + localY
        val index = (absoluteY / rowHeightPx).toInt()
        return index.coerceIn(0, currentItemCount)
    }

    /**
     * 자동 스크롤 루프
     * ScrollState.scrollBy() 사용
     */
    LaunchedEffect(Unit) {
        snapshotFlow { autoScrollVelocity }.collect { velocity ->
            while (autoScrollVelocity != 0f) {
                val pixels = (velocity / 60f)
                scrollState.scrollBy(pixels)
                delay(16)
            }
        }
    }

    Box(
        modifier = Modifier
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
                            val localY = event.toAndroidDragEvent().y - currentContainerTopOffset
                            lastDragY = localY
                            updateAutoScrollVelocity(localY)
                            targetedDropIndex = computeTargetIndex(localY)
                        }

                        override fun onMoved(event: DragAndDropEvent) {
                            val localY = event.toAndroidDragEvent().y - currentContainerTopOffset
                            lastDragY = localY
                            updateAutoScrollVelocity(localY)
                            targetedDropIndex = computeTargetIndex(localY)
                        }

                        override fun onExited(event: DragAndDropEvent) {
                            autoScrollVelocity = 0f
                            lastDragY = null
                            targetedDropIndex = null
                        }

                        override fun onEnded(event: DragAndDropEvent) {
                            autoScrollVelocity = 0f
                            lastDragY = null
                            targetedDropIndex = null
                        }

                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val text = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                            val draggedId = text?.toIntOrNull() ?: return false
                            val droppedItem = items.firstOrNull { it.id == draggedId } ?: return false
                            val localY = event.toAndroidDragEvent().y - currentContainerTopOffset
                            val targetIndex = computeTargetIndex(localY)

                            items = moveItem(items, droppedItem, targetIndex)
                            targetedDropIndex = null
                            return true
                        }
                    }
                }
            )
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            items.forEachIndexed { index, item ->
                if (targetedDropIndex == index) {
                    DropIndicator()
                }
                key(item.id) {
                    DemoItemRow(
                        modifier = Modifier
                            .height(rowHeight)
                            .dragAndDropSource {
                                detectTapGestures(
                                    onTap = { },
                                    onLongPress = {
                                        startTransfer(
                                            transferData = DragAndDropTransferData(
                                                clipData = ClipData.newPlainText(
                                                    "demo/item-id",
                                                    item.id.toString()
                                                )
                                            )
                                        )
                                    }
                                )
                            },
                        item = item,
                        isEditMode = true
                    )
                }

                // 마지막 아이템 뒤 드롭 인디케이터
                if (index == items.size - 1 && targetedDropIndex == items.size) {
                    DropIndicator()
                }
            }
        }
    }
}

/**
 * 드롭 위치 인디케이터
 */
@Composable
private fun DropIndicator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(12.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(2.dp)
            )
    )
}

/**
 * 아이템 이동 함수
 */
private fun moveItem(
    items: List<DemoItem>,
    item: DemoItem,
    to: Int
): List<DemoItem> {
    val currentIndex = items.indexOfFirst { it.id == item.id }
    if (currentIndex == -1 || currentIndex == to) return items

    val mutableList = items.toMutableList()
    val movedItem = mutableList.removeAt(currentIndex)
    val insertIndex = if (to > currentIndex) to - 1 else to
    mutableList.add(insertIndex, movedItem)

    return mutableList
}

@Preview(showBackground = true)
@Composable
private fun DragAndDropDemoViewPreview() {
    MaterialTheme {
        DragAndDropDemoView()
    }
}
