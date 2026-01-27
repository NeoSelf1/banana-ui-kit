package com.humanics.exampleapplication.model

/**
 * DnD 데모를 위한 샘플 아이템 데이터 모델
 * Humania-android의 ExerciseSet을 단순화한 버전
 *
 * [문제점] 실제 프로덕션에서는 order 필드가 있어 정렬 순서를 서버와 동기화할 수 있지만,
 * 이 데모에서는 리스트 인덱스에 의존하고 있어 서버 동기화 시 추가 작업이 필요함
 */
data class DemoItem(
    override val id: Int,
    val title: String,
    val subtitle: String,
    val iconLetter: String = title.firstOrNull()?.uppercase() ?: "?"
) : Draggable

/**
 * 샘플 데이터 생성
 */
fun generateSampleItems(count: Int = 20): List<DemoItem> {
    return (1..count).map { index ->
        DemoItem(
            id = index,
            title = "Item $index",
            subtitle = "subtitle"
        )
    }
}
