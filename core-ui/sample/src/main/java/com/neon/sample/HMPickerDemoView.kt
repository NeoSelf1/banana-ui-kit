package com.neon.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.core.ui.component.HMPicker

private val PICKER_ITEMS = (1..30).map { it.toString() }

/**
 * HMPicker 테스트 및 성능 수치화 데모 화면
 * 1~30 단일 피커를 사용하여 HMPicker의 동작 검증 및 성능 측정 가능
 */
@Composable
fun HMPickerDemoView() {
    var selectedValue by remember { mutableStateOf("1") }
    var selectionCount by remember { mutableIntStateOf(0) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HMPicker Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("picker_title")
        )

        Spacer(Modifier.height(16.dp))

        // 선택된 값 표시
        Text(
            text = "선택: $selectedValue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("selected_value")
        )

        Spacer(Modifier.height(8.dp))

        // 선택 변경 횟수 (성능 측정용)
        Text(
            text = "선택 변경 횟수: $selectionCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("selection_count")
        )

        Spacer(Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(Modifier.weight(1f))

        HMPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .testTag("picker"),
            items = PICKER_ITEMS,
            initialItem = selectedValue,
            onItemSelected = { _, item ->
                selectedValue = item
                selectionCount++
            }
        ) { item, isSelected ->
            PickerItemContent(text = item, isSelected = isSelected)
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider()
    }
}

@Composable
private fun PickerItemContent(text: String, isSelected: Boolean) {
    Text(
        text = text,
        fontSize = if (isSelected) 18.sp else 14.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("picker_item_$text")
    )
}

@Preview(showBackground = true)
@Composable
private fun HMPickerDemoViewPreview() {
    MaterialTheme {
        HMPickerDemoView()
    }
}
