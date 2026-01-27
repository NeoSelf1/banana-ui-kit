package com.humanics.exampleapplication.component

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * DragAndDropListState
 *
 * Humania-android의 DragAndDropListState.kt를 그대로 가져온 헬퍼 컴포넌트
 * LazyList에서 드래그 앤 드롭 시 자동 스크롤 속도를 계산하기 위한 상태 관리 클래스
 *
 * [문제점 1] onMove 콜백이 실제로는 아이템 이동에 사용되지 않고
 * 자동 스크롤 속도 계산을 위한 트리거로만 사용됨 - 네이밍이 혼란스러움
 *
 * [문제점 2] draggingDistance를 누적하지만 실제로 이 값을 활용하는 곳이 없음
 * 원래는 드래그된 아이템의 시각적 위치 조정에 사용하려던 것으로 보이나 미완성
 */
class DragAndDropListState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    /**
     * 현재 드래그 중인 아이템 이동 거리 저장
     * [문제점] 이 값이 누적되지만 실제로 사용되는 곳이 없음
     */
    private var draggingDistance by mutableFloatStateOf(0f)

    /**
     * 드래그 시작한 아이템 초기 정보 저장
     * [문제점] 이 정보도 저장되지만 실제 활용되지 않음
     */
    private var initialDraggingElement by mutableStateOf<LazyListItemInfo?>(null)

    /**
     * 단일 아이템의 끝 오프셋 계산
     */
    private val LazyListItemInfo.offsetEnd: Int
        get() = this.offset + this.size

    /**
     * 드래그 시작 시 호출
     * @param offset 드래그 시작 위치
     */
    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..item.offsetEnd }
            ?.also {
                initialDraggingElement = it
            }
    }

    /**
     * 드래그 종료/취소 시 호출
     */
    fun onDragInterrupted() {
        initialDraggingElement = null
        draggingDistance = 0f
    }

    /**
     * 드래그 중 호출
     * @param offset 드래그 변위량
     *
     * [문제점] onMove가 항상 (0, 0)으로 호출됨 - 의미없는 파라미터
     * 실제로는 이 콜백 호출 자체가 자동 스크롤 속도를 재계산하는 트리거 역할만 함
     */
    fun onDrag(offset: Offset) {
        draggingDistance += offset.y
        // 외부 자동 스크롤 컨트롤러에 콜백
        onMove.invoke(0, 0)
    }
}

/**
 * DragAndDropListState를 remember로 래핑한 Composable 함수
 *
 * [UX 문제점] 이 상태는 자동 스크롤을 위한 것인데,
 * 실제 자동 스크롤 로직은 View 레벨에서 별도로 구현해야 함
 * 상태와 동작이 분리되어 있어 사용하기 불편함
 */
@Composable
fun rememberDragAndDropListState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragAndDropListState {
    return remember { DragAndDropListState(lazyListState, onMove) }
}
