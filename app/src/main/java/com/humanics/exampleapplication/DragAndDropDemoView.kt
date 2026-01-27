package com.humanics.exampleapplication

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.humanics.exampleapplication.component.DemoItemRow
import com.humanics.exampleapplication.component.rememberDragAndDropListState
import com.humanics.exampleapplication.model.DemoItem
import com.humanics.exampleapplication.model.generateSampleItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * DragAndDropDemoView
 *
 * Humania-android RoutineDetailView의 DnD 로직을 그대로 가져온 데모 뷰
 * 개발 블로그에서 DnD 구현 과정을 설명하기 위한 레거시 코드
 *
 * === 전체적인 문제점 ===
 *
 * [아키텍처 문제점 1] DnD 상태 관리가 View 레벨에 분산되어 있음
 * - targetedDropIndex, lastDragY, desiredVelocity 등이 View에서 직접 관리됨
 * - ViewModel로 분리하면 테스트하기 쉽고 재사용성이 높아짐
 *
 * [아키텍처 문제점 2] 자동 스크롤 로직이 View에 직접 구현되어 있음
 * - 프레임 기반 업데이트 루프가 Composable 내부에 있어 관심사가 혼재됨
 * - 별도 컴포넌트나 유틸리티로 분리하는 것이 좋음
 *
 * [UX 문제점 1] 드래그 중 원본 아이템이 그대로 표시됨
 * - 사용자가 어떤 아이템을 드래그하고 있는지 시각적으로 구분하기 어려움
 * - 원본 아이템에 opacity를 적용하거나 placeholder로 대체하는 것이 좋음
 *
 * [UX 문제점 2] 드래그 프리뷰가 기본 시스템 스타일
 * - 커스텀 프리뷰를 제공하면 더 나은 UX 제공 가능
 *
 * [UX 문제점 3] 드롭 시 haptic feedback이 없음
 * - 드롭 완료 시 진동 피드백을 추가하면 사용자 경험 향상
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DragAndDropDemoView() {
    // 데모 아이템 리스트
    var items by remember { mutableStateOf(generateSampleItems()) }
    var isEditMode by remember { mutableStateOf(true) }

    /**
     * 현재 드롭 타겟 인덱스
     * Humania-android의 RoutineDetailViewModel.targetedDropIndex와 동일
     *
     * [문제점] 이 상태가 View에서 관리되고 있어
     * 테스트하기 어렵고 로직이 View에 종속됨
     */
    var targetedDropIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    /**
     * 자동 스크롤 관련 상태
     *
     * [문제점 1] desiredVelocity가 픽셀 단위의 하드코딩된 값
     * -> 다양한 화면 밀도에서 일관되지 않은 스크롤 속도
     *
     * [문제점 2] lastDragY를 nullable Float으로 관리
     * -> 매번 null 체크가 필요해 코드가 복잡해짐
     */
    var desiredVelocity by remember { mutableFloatStateOf(0f) }
    var lastDragY by remember { mutableStateOf<Float?>(null) }
    val density = LocalDensity.current

    /**
     * DragAndDropListState 헬퍼
     *
     * [문제점] 이 헬퍼가 실제로 하는 일이 거의 없음
     * 자동 스크롤 속도 계산 트리거로만 사용되고 있어 불필요하게 복잡함
     */
    val dndState = rememberDragAndDropListState(lazyListState = listState) { _, _ ->
        val y = lastDragY
        if (y == null) {
            desiredVelocity = 0f
            return@rememberDragAndDropListState
        }
        val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()

        val toTop = y - viewportStart
        val toBottom = viewportEnd - y

        /**
         * [문제점] 96.dp, 64.dp, 1200f 등 하드코딩된 매직 넘버
         * -> 상수로 분리하거나 설정 가능하게 만들어야 함
         * -> 다양한 화면 크기에서 테스트 필요
         */
        val thresholdPx = with(density) { 96.dp.toPx() }
        desiredVelocity = when {
            toTop < (thresholdPx + with(density) { 64.dp.toPx() }) -> +1200f
            toBottom < thresholdPx -> -1200f
            else -> 0f
        }
    }

    /**
     * 자동 스크롤 루프
     *
     * [문제점 1] 프레임 기반 업데이트가 Composable 내부에 있음
     * -> 별도 컴포넌트나 유틸리티로 분리하는 것이 좋음
     *
     * [문제점 2] delay(16)으로 약 60fps 업데이트
     * -> 화면 주사율에 맞지 않을 수 있음
     * -> withFrameNanos 사용이 더 적합
     *
     * [문제점 3] 스크롤 속도가 고정됨
     * -> 리스트 끝에 가까울수록 속도를 줄이는 등의 가속도 조절이 없음
     */
    LaunchedEffect(Unit) {
        snapshotFlow { desiredVelocity }.collect { velocity ->
            while (desiredVelocity != 0f) {
                val pixels = (velocity / 60f).toInt()
                listState.dispatchRawDelta(-pixels.toFloat())
                delay(16)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DnD Demo (Legacy)") },
                actions = {
                    Text(
                        text = if (isEditMode) "Edit" else "View",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = isEditMode,
                        onCheckedChange = { isEditMode = it }
                    )
                }
            )
        }
    ) { paddingValues ->
        /**
         * DnD 타겟 영역 (전체 Box)
         *
         * [문제점] Box 전체를 dragAndDropTarget으로 설정
         * -> 각 아이템별로 설정하는 것보다 덜 정밀함
         * -> 하지만 구현이 더 간단하고 헤더/푸터 영역도 처리 가능
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event
                            .mimeTypes()
                            .contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    },
                    target = remember {
                        object : DragAndDropTarget {
                            /**
                             * 드래그가 타겟 영역에 진입했을 때
                             */
                            override fun onEntered(event: DragAndDropEvent) {
                                val y = event.toAndroidDragEvent().y
                                lastDragY = y
                                dndState.onDragStart(Offset(0f, y))
                                println("onEntered: $y")
                                targetedDropIndex = computeTargetIndex(
                                    y = y,
                                    listState = listState,
                                    totalCount = items.size,
                                    headerOffset = 0f
                                )
                            }

                            /**
                             * 드래그 중 이동할 때
                             *
                             * [성능 문제점] 매 프레임마다 computeTargetIndex 호출
                             * -> 리스트가 길어지면 성능 이슈 가능
                             * -> 스로틀링이나 디바운싱 적용 고려
                             */
                            override fun onMoved(event: DragAndDropEvent) {
                                val y = event.toAndroidDragEvent().y
                                val last = lastDragY
                                if (last != null) {
                                    val dy = y - last
                                    dndState.onDrag(Offset(0f, dy))
                                }
                                lastDragY = y
                                println("onMoved: $y") // 528부터 시작됨
                                targetedDropIndex = computeTargetIndex(
                                    y = y,
                                    listState = listState,
                                    totalCount = items.size,
                                    headerOffset = 0f
                                )
                            }

                            /**
                             * 드래그가 타겟 영역을 벗어났을 때
                             */
                            override fun onExited(event: DragAndDropEvent) {
                                desiredVelocity = 0f
                                lastDragY = null
                                targetedDropIndex = null
                            }

                            /**
                             * 드래그가 종료됐을 때 (드롭 여부와 관계없이)
                             */
                            override fun onEnded(event: DragAndDropEvent) {
                                desiredVelocity = 0f
                                lastDragY = null
                                dndState.onDragInterrupted()
                                targetedDropIndex = null
                            }

                            /**
                             * 드롭 실행
                             *
                             * [문제점 1] ClipData 파싱이 동기적으로 수행됨
                             * -> 큰 데이터의 경우 UI 블로킹 가능
                             *
                             * [문제점 2] 에러 처리가 단순히 false 반환
                             * -> 사용자에게 실패 이유를 알려주지 않음
                             */
                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                val text = event
                                    .toAndroidDragEvent()
                                    .clipData
                                    ?.getItemAt(0)
                                    ?.text
                                    ?.toString()

                                val draggedId = text?.toIntOrNull() ?: return false
                                val droppedItem = items.firstOrNull { it.id == draggedId }
                                    ?: return false

                                val y = event.toAndroidDragEvent().y
                                val at = computeTargetIndex(
                                    y = y,
                                    listState = listState,
                                    totalCount = items.size,
                                    headerOffset = 0f
                                )

                                /**
                                 * 최종 삽입 인덱스 계산
                                 *
                                 * [문제점] 이 로직이 View에 직접 구현되어 있음
                                 * -> 테스트하기 어렵고, 재사용성이 떨어짐
                                 * -> UseCase나 ViewModel로 분리 권장
                                 */
                                val currentIndex = items.indexOfFirst { it.id == draggedId }
                                val predictedInsertIndex = if (currentIndex != -1) {
                                    if (currentIndex < at) {
                                        (at - 1).coerceIn(0, (items.size - 1).coerceAtLeast(0))
                                    } else {
                                        at.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                                    }
                                } else {
                                    at.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                                }

                                // 아이템 이동 수행
                                items = moveItem(items, droppedItem, at)
                                targetedDropIndex = null

                                // 드롭 완료 후 해당 위치로 스크롤
//                                scrollScope.launch {
//                                    if (items.isNotEmpty()) {
//                                        listState.animateScrollToItem(
//                                            index = predictedInsertIndex,
//                                            scrollOffset = 0
//                                        )
//                                    }
//                                }
                                return true
                            }
                        }
                    }
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id }
                ) { index, item ->
                    Column(
                        /**
                         * animateItem() 사용
                         *
                         * [주의] Compose Foundation 1.7.0+ 필요
                         * 이전 버전에서는 animateItemPlacement() 사용
                         */
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        /**
                         * 드롭 인디케이터 (아이템 위)
                         *
                         * [UX 문제점] 인디케이터 높이가 12.dp로 작음
                         * -> 드래그 중 시각적 피드백이 약함
                         * -> 더 눈에 띄는 디자인 고려 필요
                         */
                        if (targetedDropIndex == index) {
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
                         * 아이템 행 (드래그 소스)
                         *
                         * [문제점] dragAndDropSource가 조건부로 적용됨
                         * -> Modifier 체인이 복잡해짐
                         * -> 별도 함수로 추출하는 것이 좋음
                         */
                        DemoItemRow(
                            modifier = Modifier
                                .then(
                                    if (isEditMode) {
                                        Modifier.dragAndDropSource {
                                            detectTapGestures(
                                                /**
                                                 * [UX 문제점] 탭 동작이 현재 아무것도 하지 않음
                                                 * -> 상세 화면 이동 등의 동작 추가 가능
                                                 */
                                                onTap = {
                                                    println("onTap")
                                                    // 데모에서는 탭 시 아무 동작 없음
                                                },
                                                /**
                                                 * 롱프레스로 드래그 시작
                                                 *
                                                 * [UX 문제점] 롱프레스 시간이 기본값(400ms)
                                                 * -> 일부 사용자에게는 너무 길거나 짧을 수 있음
                                                 * -> 접근성을 위해 조정 가능하게 만들 수 있음
                                                 */
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
                                        }
                                    } else {
                                        Modifier
                                    }
                                ),
                            item = item,
                            isEditMode = isEditMode
                        )

                        /**
                         * 드롭 인디케이터 (마지막 아이템 아래)
                         */
                        if (index == items.size - 1 && targetedDropIndex == items.size) {
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
                    }
                }
            }
        }
    }
}

/**
 * 드롭 타겟 인덱스 계산 함수
 *
 * Humania-android의 computeTargetIndex 함수와 동일한 로직
 *
 * [문제점 1] headerOffset이 매직 넘버로 전달됨
 * -> 원본 코드에서는 640이라는 하드코딩된 값 사용
 * -> 헤더 높이가 변경되면 수동으로 수정 필요
 *
 * [문제점 2] 각 아이템의 중간점 기준으로 판단
 * -> 아이템 크기가 다양한 경우 부정확할 수 있음
 *
 * [개선안] GeometryReader나 onGloballyPositioned를 사용해
 * 각 아이템의 정확한 위치를 실시간으로 추적
 */
private fun computeTargetIndex(
    y: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    totalCount: Int,
    headerOffset: Float = 0f
): Int {
    val items = listState.layoutInfo.visibleItemsInfo
    if (items.isEmpty()) return totalCount

    // 헤더 오프셋 보정
    val computedY = y - headerOffset

    // 첫 번째 아이템 중간점 계산
    val first = items.first()
    val firstMid = (first.offset + (first.offset + first.size)) / 2f
    if (computedY < firstMid) return 0

    // 각 아이템의 중간점과 비교
    for (info in items) {
        val top = info.offset.toFloat()
        val bottom = (info.offset + info.size).toFloat()
        val mid = (top + bottom) / 2f
        if (computedY < mid) return info.index
    }

    // 마지막 아이템 이후
    val last = items.last()
    return (last.index + 1).coerceAtMost(totalCount)
}

/**
 * 아이템 이동 함수
 *
 * Humania-android의 RoutineDetailViewModel.moveExerciseSet 함수와 동일한 로직
 *
 * [문제점] 매번 새 리스트를 생성하여 반환
 * -> 큰 리스트에서는 성능 이슈 가능
 * -> 불변성을 유지하려는 의도지만, 최적화 여지 있음
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
