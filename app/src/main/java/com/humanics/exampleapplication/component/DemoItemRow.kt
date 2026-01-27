package com.humanics.exampleapplication.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humanics.exampleapplication.model.DemoItem

/**
 * DemoItemRow
 *
 * Humania-android의 ExerciseSetRow를 단순화한 버전
 * 드래그 앤 드롭 데모를 위한 아이템 행 컴포넌트
 *
 * [문제점 1] isEditMode에 따라 드래그 핸들 아이콘 표시 여부만 바뀌고,
 * 실제 드래그 가능 여부는 부모 컴포넌트에서 별도로 제어함
 * -> 상태와 UI가 분리되어 있어 일관성이 떨어짐
 *
 * [UX 문제점] 드래그 핸들이 오른쪽 끝에 위치해 있어
 * 왼손잡이 사용자나 한 손 조작 시 불편함
 * 또한 드래그 핸들 영역이 너무 작아 터치하기 어려울 수 있음
 */
@Composable
fun DemoItemRow(
    modifier: Modifier = Modifier,
    item: DemoItem,
    isEditMode: Boolean = true
) {
    SideEffect {
        Log.d("디버깅", "Row recomposed: id=${item.id}")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘 (운동 종류 표시용)
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
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        /**
         * 드래그 핸들 아이콘
         *
         * [문제점] 편집 모드에서만 표시되지만, 실제 드래그 동작은
         * 아이콘 영역이 아닌 전체 행에서 롱프레스로 시작됨
         * -> 사용자에게 혼란을 줄 수 있음 (핸들을 잡아야 할 것 같지만 실제로는 아님)
         *
         * [개선안] 드래그 핸들 영역에만 롱프레스를 적용하거나,
         * 아이콘을 터치하면 바로 드래그가 시작되도록 변경
         */
        if (isEditMode) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                contentDescription = "Drag handle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DemoItemRowPreview() {
    MaterialTheme {
        Column {
            DemoItemRow(
                item = DemoItem(1, "Bench Press", "3 sets × 12 reps • 60s rest"),
                isEditMode = true
            )
            DemoItemRow(
                item = DemoItem(2, "Squat", "4 sets × 10 reps • 90s rest"),
                isEditMode = false
            )
        }
    }
}
