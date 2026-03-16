package com.neon.sample.component

import com.neon.core.ui.component.list.Draggable
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
