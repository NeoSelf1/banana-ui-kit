package com.neon.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neon.core.ui.component.list.NeoDraggableList
import com.neon.core.ui.theme.Gray10
import com.neon.core.ui.theme.Gray50
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.NeoFont
import com.neon.core.ui.theme.Primary10
import com.neon.sample.component.DemoItem
import com.neon.sample.component.generateSampleItems

@Composable
fun DragAndDropDemoView() {
    var items by remember { mutableStateOf(generateSampleItems()) }

    NeoDraggableList(
        items = items,
        rowHeight = 80.dp,
        isDragEnabled = true,
        onReorder = { item, targetIndex ->
            val currentIndex = items.indexOfFirst { it.id == item.id }
            if (currentIndex != -1 && currentIndex != targetIndex) {
                val mutableList = items.toMutableList()
                val movedItem = mutableList.removeAt(currentIndex)
                mutableList.add(targetIndex, movedItem)
                items = mutableList
            }
        },
        onTapRow = { item -> println("Tapped: ${item.title}") },
        itemContent = { item, isDragging ->
            DemoItemRowContent(
                item = item,
                isEditMode = true
            )
        },
        header = null,
        footer = null,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun DemoItemRowContent(
    item: DemoItem,
    isEditMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray10)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Primary10),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.iconLetter,
                style = NeoFont.subhead3,
                color = Gray10
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = NeoFont.subhead5,
                color = Gray80
            )
            Text(
                text = item.subtitle,
                style = NeoFont.body4,
                color = Gray50
            )
        }

        if (isEditMode) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                contentDescription = "Drag handle",
                tint = Gray50,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DragAndDropDemoViewPreview() {

    DragAndDropDemoView()

}
