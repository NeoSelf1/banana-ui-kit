package com.neon.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neon.core.ui.component.list.NeoDraggableList
import com.neon.core.ui.theme.Gray10
import com.neon.core.ui.theme.Gray20
import com.neon.core.ui.theme.Gray50
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.NeoFont
import com.neon.core.ui.theme.Primary10
import com.neon.core.ui.theme.Primary50
import com.neon.sample.component.DemoItem
import com.neon.sample.component.NeoDraggableListLegacy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoDraggableListDemoView() {
    var legacyItems by remember { mutableStateOf(generateDemoItems()) }
    var newItems by remember { mutableStateOf(generateDemoItems()) }
    var isEditMode by remember { mutableStateOf(true) }
    var legacyReorderCount by remember { mutableIntStateOf(0) }
    var newReorderCount by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("DraggableList Comparison", style = NeoFont.subhead3)
                },
                actions = {
                    Text(
                        text = if (isEditMode) "Edit" else "View",
                        style = NeoFont.body2,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = isEditMode,
                        onCheckedChange = { isEditMode = it }
                    )
                }
            )
        },
    ) { paddingValues ->

        Row(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left: Legacy (useItemKey = true)
            Column(Modifier.weight(1f)) {
                ListLabel(
                    title = "Legacy",
                    subtitle = "useItemKey=true",
                    reorderCount = legacyReorderCount
                )
                NeoDraggableListLegacy(
                    items = legacyItems,
                    rowHeight = 64.dp,
                    isDragEnabled = isEditMode,
                    useItemKey = true,
                    onReorder = { item, targetIndex ->
                        val currentIndex = legacyItems.indexOfFirst { it.id == item.id }
                        if (currentIndex != -1 && currentIndex != targetIndex) {
                            val list = legacyItems.toMutableList()
                            list.add(targetIndex, list.removeAt(currentIndex))
                            legacyItems = list
                            legacyReorderCount++
                        }
                    },
                    onTapRow = {},
                    itemContent = { item, isDragging ->
                        CompactItemRow(item = item as DemoItem, isDragging = isDragging, isEditMode = isEditMode)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            VerticalDivider(Modifier.fillMaxHeight().width(1.dp))

            // Right: New (core-ui)
            Column(Modifier.weight(1f)) {
                ListLabel(
                    title = "New",
                    subtitle = "rememberUpdatedState",
                    reorderCount = newReorderCount
                )
                NeoDraggableList(
                    items = newItems,
                    rowHeight = 64.dp,
                    isDragEnabled = isEditMode,
                    onReorder = { item, targetIndex ->
                        val currentIndex = newItems.indexOfFirst { it.id == item.id }
                        if (currentIndex != -1 && currentIndex != targetIndex) {
                            val list = newItems.toMutableList()
                            list.add(targetIndex, list.removeAt(currentIndex))
                            newItems = list
                            newReorderCount++
                        }
                    },
                    onTapRow = {},
                    itemContent = { item, isDragging ->
                        CompactItemRow(item = item, isDragging = isDragging, isEditMode = isEditMode)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ListLabel(title: String, subtitle: String, reorderCount: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Gray20)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = NeoFont.subhead5, color = Gray80)
            Text("$reorderCount", style = NeoFont.body2, color = Primary50)
        }
        Text(subtitle, style = NeoFont.body6, color = Gray50)
    }
}

@Composable
private fun CompactItemRow(
    item: DemoItem,
    isDragging: Boolean,
    isEditMode: Boolean,
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Primary10),
            contentAlignment = Alignment.Center
        ) {
            Text(item.iconLetter, style = NeoFont.body1, color = Gray10)
        }

        Column(Modifier.weight(1f)) {
            Text(item.title, style = NeoFont.body1, color = Gray80)
            Text(
                text = if (isDragging) "Dragging..." else item.subtitle,
                style = NeoFont.body6,
                color = if (isDragging) Primary50 else Gray50
            )
        }

        if (isEditMode) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(
                    id = android.R.drawable.ic_menu_sort_by_size
                ),
                contentDescription = "Drag handle",
                tint = if (isDragging) Primary50 else Gray50,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun generateDemoItems(): List<DemoItem> {
    return (1..15).map { index ->
        DemoItem(
            id = index,
            title = "Item $index",
            subtitle = "Drag to reorder"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NeoDraggableListDemoViewPreview() {
    NeoDraggableListDemoView()
}
