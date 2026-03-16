package com.neon.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neon.core.ui.component.HMPicker
import com.neon.core.ui.theme.Gray50
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.HMFont

private val PICKER_ITEMS = (1..30).map { it.toString() }

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
            style = HMFont.headline3,
            modifier = Modifier.testTag("picker_title")
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "선택: $selectedValue",
            style = HMFont.subhead1,
            modifier = Modifier.testTag("selected_value")
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "선택 변경 횟수: $selectionCount",
            style = HMFont.body2,
            color = Gray50,
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
        style = if (isSelected) HMFont.subhead3 else HMFont.body2,
        color = if (isSelected) Gray80 else Gray50,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("picker_item_$text")
    )
}

@Preview(showBackground = true)
@Composable
private fun HMPickerDemoViewPreview() {
    HMPickerDemoView()
}
