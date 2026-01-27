package com.humanics.exampleapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.humanics.exampleapplication.component.HMDraggableList
import com.humanics.exampleapplication.model.DemoItem
import com.humanics.exampleapplication.model.generateSampleItems

/**
 * DragAndDropDemoView
 *
 * HMDraggableList를 사용한 드래그 앤 드롭 데모
 * iOS HMDraggableScrollView와 동일한 인터페이스 및 동작 패턴
 *
 * === iOS HMDraggableScrollView와의 대응 ===
 *
 * [인자 매핑]
 * - items: Binding<[Item]> → items: List<T> + onMoveItem 콜백
 * - rowHeight: CGFloat → rowHeight: Dp
 * - isDragEnabled: Bool → isDragEnabled: Boolean
 * - onReorder: (() -> Void)? → onReorder: () -> Unit
 * - onTapRow: (Item) -> Void → onTapRow: (T) -> Unit
 * - itemContent: (Item, Bool) -> ItemRow → itemContent: @Composable (T, Boolean) -> Unit
 * - header/footer: ViewBuilder → header/footer: @Composable (() -> Unit)?
 *
 * [DropIndicator]
 * - iOS: UnevenRoundedRectangle, .primary10, 첫/마지막 모서리 처리
 * - Android: Box + RoundedCornerShape, primary.copy(alpha = 0.3f)
 * - 위치: targetedDropIndex == index (아이템 위), == items.size (마지막 뒤)
 *
 * [프레임워크 차이로 인한 차이점]
 * - iOS: Binding으로 양방향 바인딩
 * - Android: 단방향 데이터 흐름 (List + onMoveItem 콜백)
 * - iOS: UnevenRoundedRectangle 지원
 * - Android: RoundedCornerShape만 지원 (균일한 모서리)
 */
@Composable
fun DragAndDropDemoView() {
    // === 상태 관리 ===
    // iOS의 @Binding var items와 대응
    // Android는 단방향 데이터 흐름으로 상태 + 콜백 패턴 사용
    var items by remember { mutableStateOf(generateSampleItems()) }

    // === HMDraggableList 사용 ===
    // iOS HMDraggableScrollView와 동일한 인터페이스
    HMDraggableList(
        // iOS: @Binding var items
        items = items,

        // iOS: rowHeight: CGFloat = 60 → 72.dp (기존 DragAndDropDemoView 값 유지)
        rowHeight = 72.dp,

        // iOS: isDragEnabled: Bool
        isDragEnabled = true,

        // iOS: onReorder: (() -> Void)?
        onReorder = {
            // 재정렬 완료 시 호출
            // 실제 사용 시 서버 동기화 등 처리
        },

        // iOS: onTapRow: @escaping (Item) -> Void
        onTapRow = { item ->
            // 탭 시 처리
            println("Tapped: ${item.title}")
        },

        // Android 전용: 아이템 이동 콜백
        // iOS는 Binding으로 직접 수정, Android는 콜백으로 처리
        onMoveItem = { item, targetIndex ->
            val currentIndex = items.indexOfFirst { it.id == item.id }
            if (currentIndex != -1 && currentIndex != targetIndex) {
                val mutableList = items.toMutableList()
                val movedItem = mutableList.removeAt(currentIndex)
                val insertIndex = if (targetIndex > currentIndex) targetIndex - 1 else targetIndex
                mutableList.add(insertIndex, movedItem)
                items = mutableList
            }
        },

        // Android 전용: ID 추출 함수
        // iOS는 Identifiable 프로토콜로 자동 처리
        getItemId = { it.id },

        // iOS: @ViewBuilder itemContent: @escaping (Item, Bool) -> ItemRow
        // Bool 파라미터는 isDragging
        itemContent = { item, isDragging ->
            DemoItemRowContent(
                item = item,
                isDragging = isDragging,
                isEditMode = true
            )
        },

        // iOS: @ViewBuilder header: () -> Header
        // 현재 DragAndDropDemoView는 header 없음 (null)
        header = null,

        // iOS: @ViewBuilder footer: () -> Footer
        // 현재 DragAndDropDemoView는 footer 없음 (null)
        footer = null,

        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 아이템 행 컨텐츠
 *
 * iOS의 itemContent: (Item, Bool) -> ItemRow와 대응
 * 기존 DemoItemRow의 레이아웃을 그대로 유지
 */
@Composable
private fun DemoItemRowContent(
    item: DemoItem,
    isDragging: Boolean,
    isEditMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
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

        Spacer(modifier = Modifier.width(12.dp))

        // 텍스트 영역
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

        // 드래그 핸들 아이콘 (편집 모드에서만)
        if (isEditMode) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
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

@Preview(showBackground = true)
@Composable
private fun DragAndDropDemoViewPreview() {
    MaterialTheme {
        DragAndDropDemoView()
    }
}
