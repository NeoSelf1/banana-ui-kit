package com.neon.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.neon.core.ui.component.list.HMDraggableList
import com.neon.core.ui.theme.Gray10
import com.neon.core.ui.theme.Gray20
import com.neon.core.ui.theme.Gray50
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.HMFont
import com.neon.core.ui.theme.Primary10
import com.neon.core.ui.theme.Primary50
import com.neon.sample.component.DemoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HMDraggableListDemoView() {
    var items by remember { mutableStateOf(generateDemoItems()) }
    var isEditMode by remember { mutableStateOf(true) }
    var reorderCount by remember { mutableIntStateOf(0) }
    var lastTappedItem by remember { mutableStateOf<DemoItem?>(null) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HMDraggableList Demo", style = HMFont.subhead3)
                        Text(
                            text = "Reorders: $reorderCount",
                            style = HMFont.body4,
                            color = Gray50
                        )
                    }
                },
                actions = {
                    Text(
                        text = if (isEditMode) "Edit" else "View",
                        style = HMFont.body2,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = isEditMode,
                        onCheckedChange = { isEditMode = it }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val newId = (items.maxOfOrNull { it.id } ?: 0) + 1
                    items = items + DemoItem(
                        id = newId,
                        title = "New Item $newId",
                        subtitle = "Added dynamically"
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { paddingValues ->

        HMDraggableList(
            items = items,
            rowHeight = 80.dp,
            isDragEnabled = isEditMode,
            onReorder = { item, targetIndex ->
                val currentIndex = items.indexOfFirst { it.id == item.id }
                if (currentIndex != -1 && currentIndex != targetIndex) {
                    val mutableList = items.toMutableList()
                    val movedItem = mutableList.removeAt(currentIndex)
                    mutableList.add(targetIndex, movedItem)
                    items = mutableList
                    reorderCount++
                }
            },
            onTapRow = { item ->
                lastTappedItem = item
            },
            itemContent = { item, isDragging ->
                DemoItemRowContent(
                    item = item,
                    isDragging = isDragging,
                    isEditMode = isEditMode
                )
            },
            header = {
                DemoHeader(itemCount = items.size)
            },
            footer = {
                DemoFooter(
                    onClearAll = { items = emptyList() },
                    onReset = { items = generateDemoItems() }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun DemoItemRowContent(
    item: DemoItem,
    isDragging: Boolean,
    isEditMode: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                style = HMFont.subhead3,
                color = Gray10
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = HMFont.subhead5,
                color = Gray80
            )
            Text(
                text = if (isDragging) "Dragging..." else item.subtitle,
                style = HMFont.body4,
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
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DemoHeader(itemCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Gray20)
            .padding(16.dp)
    ) {
        Text(
            text = "Total Items: $itemCount",
            style = HMFont.subhead5,
            color = Gray50
        )
    }
}

@Composable
private fun DemoFooter(
    onClearAll: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onClearAll) {
                Text("Clear All")
            }
            Button(onClick = onReset) {
                Text("Reset")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
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
private fun HMDraggableListDemoViewPreview() {
    HMDraggableListDemoView()
}
