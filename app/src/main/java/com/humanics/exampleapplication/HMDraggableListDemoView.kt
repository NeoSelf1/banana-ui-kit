package com.humanics.exampleapplication

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humanics.exampleapplication.component.HMDraggableList
import com.humanics.exampleapplication.model.DemoItem

/**
 * HMDraggableListDemoView - 개선된 DnD 컴포넌트 데모
 *
 * HMDraggableList를 사용하여 레거시 코드 대비 개선된 DnD 경험을 보여주는 데모 뷰
 *
 * === 레거시 DragAndDropDemoView 대비 개선점 ===
 *
 * [아키텍처 개선]
 * 1. DnD 로직이 HMDraggableList 컴포넌트로 완전 분리
 *    - 레거시: View에 DnD 상태와 로직이 직접 구현
 *    - 개선: 재사용 가능한 컴포넌트로 캡슐화
 *
 * 2. 콜백 기반 아키텍처
 *    - 레거시: moveItem 함수가 View 내부에 정의
 *    - 개선: onMoveItem, onTapRow, onReorder 콜백으로 ViewModel과 분리 가능
 *
 * 3. 상태 관리 개선
 *    - 레거시: targetedDropIndex, lastDragY 등이 View에 분산
 *    - 개선: HMDraggableList 내부에서 관리
 *
 * [UX 개선]
 * 1. 햅틱 피드백
 *    - 레거시: 없음
 *    - 개선: 드롭 타겟 변경 시 HapticFeedbackType.TextHandleMove 피드백
 *
 * 2. 드래그 중 아이템 시각적 피드백
 *    - 레거시: 모든 아이템 동일하게 표시
 *    - 개선: draggingItemId로 드래그 중인 아이템 opacity 0.8 적용
 *
 * 3. 프레임 동기화 자동 스크롤
 *    - 레거시: delay(16)으로 약 60fps (부정확)
 *    - 개선: withFrameNanos로 정확한 프레임 동기화
 *
 * [코드 품질 개선]
 * 1. 매직 넘버 제거
 *    - 레거시: 96.dp, 64.dp, 1200f, 640(headerOffset) 등 하드코딩
 *    - 개선: 상수로 분리, headerOffset 파라미터화
 *
 * 2. Header/Footer 지원
 *    - 레거시: 지원 안함
 *    - 개선: 컴포저블 슬롯으로 유연하게 지원
 *
 * 3. 제네릭 타입
 *    - 레거시: 특정 데이터 타입에 종속
 *    - 개선: 제네릭 T로 어떤 타입에도 적용 가능
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HMDraggableListDemoView() {
    // ========================================
    // 상태 관리
    // [개선] DnD 관련 상태가 View에서 제거됨 (HMDraggableList 내부에서 관리)
    // ========================================
    var items by remember { mutableStateOf(generateDemoItems()) }
    var isEditMode by remember { mutableStateOf(true) }
    var reorderCount by remember { mutableIntStateOf(0) }
    var lastTappedItem by remember { mutableStateOf<DemoItem?>(null) }

    // LazyListState 외부 주입 (필요 시 스크롤 위치 제어 가능)
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HMDraggableList Demo")
                        Text(
                            text = "Reorders: $reorderCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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

        /**
         * [개선] HMDraggableList 사용
         *
         * 레거시 대비 장점:
         * - DnD 로직이 캡슐화되어 View가 깔끔해짐
         * - 콜백으로 비즈니스 로직 분리 가능
         * - 재사용 가능한 컴포넌트
         */
        HMDraggableList(
            items = items,
            rowHeight = 80.dp,
            isDragEnabled = isEditMode,

            // [개선] 콜백 기반 아키텍처
            // ViewModel로 쉽게 이동 가능
            onReorder = { item, targetIndex ->
                val currentIndex = items.indexOfFirst { it.id == item.id }
                if (currentIndex != -1 && currentIndex != targetIndex) {
                    val mutableList = items.toMutableList()
                    val movedItem = mutableList.removeAt(currentIndex)
                    mutableList.add(targetIndex, movedItem)
                    items = mutableList
                    reorderCount++
                    println("Moved ${item.title} from $currentIndex to $targetIndex")
                }
            },

            onTapRow = { item ->
                lastTappedItem = item
                println("Tapped: ${item.title}")
            },

            // [개선] 아이템 컨텐츠 - isDragging 파라미터로 시각적 피드백
            itemContent = { item, isDragging ->
                DemoItemRowContent(
                    item = item,
                    isDragging = isDragging,
                    isEditMode = isEditMode,
                    isLastTapped = lastTappedItem?.id == item.id
                )
            },

            // [개선] Header 슬롯 지원
            header = {
                DemoHeader(itemCount = items.size)
            },

            // [개선] Footer 슬롯 지원
            footer = {
                DemoFooter(
                    onClearAll = { items = emptyList() },
                    onReset = { items = generateDemoItems() }
                )
            },

            listState = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

/**
 * 데모 아이템 행 컨텐츠
 */
@Composable
private fun DemoItemRowContent(
    item: DemoItem,
    isDragging: Boolean,
    isEditMode: Boolean,
    isLastTapped: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = when {
                    isLastTapped -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 아이콘
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.iconLetter,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }

        // 텍스트
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isDragging) "Dragging..." else item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 13.sp
            )
        }

        // 드래그 핸들 (편집 모드에서만)
        if (isEditMode) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(
                    id = android.R.drawable.ic_menu_sort_by_size
                ),
                contentDescription = "Drag handle",
                tint = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 데모 헤더
 */
@Composable
private fun DemoHeader(itemCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = "Total Items: $itemCount",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 데모 푸터
 */
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

/**
 * 샘플 데이터 생성
 */
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
    MaterialTheme {
        HMDraggableListDemoView()
    }
}
