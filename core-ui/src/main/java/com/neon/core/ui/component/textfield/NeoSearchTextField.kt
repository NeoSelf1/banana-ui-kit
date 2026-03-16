package com.neon.core.ui.component.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray15
import com.neon.core.ui.theme.Gray45
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.NeoFont

/**
 * 검색 전용 텍스트 입력 필드 컴포넌트.
 *
 * 40.dp 높이의 둥근 모서리(8.dp) 입력 필드로, [Gray15] 배경색을 가지며
 * 테두리가 없는 플랫한 스타일이다. 검색바 UI에 적합하도록 설계되었다.
 *
 * [NeoTextField]와 달리 포커스 애니메이션이나 테두리 강조가 없으며,
 * 배경색만으로 영역을 구분합니다. 텍스트가 비어있을 때 [placeholder]가 [Gray45] 색상으로 표시됩니다.
 *
 * @param modifier 검색 필드 컨테이너에 적용할 Modifier.
 * @param value 현재 검색 텍스트 값.
 * @param onValueChange 텍스트 변경 시 호출되는 콜백.
 * @param placeholder 텍스트가 비어있을 때 표시되는 힌트 텍스트.
 * @param textFieldModifier 내부 BasicTextField에 직접 적용할 Modifier.
 *
 * @see NeoTextField
 */
@Composable
fun NeoSearchTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textFieldModifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Gray15, shape = shape)
            .height(40.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = NeoFont.body2, color = Gray45)
        }

        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onValueChange(newValue.text)
            },
            textStyle = NeoFont.subhead6.copy(color = Gray80),
            singleLine = true,
            modifier = textFieldModifier.fillMaxWidth()
        )
    }
}
