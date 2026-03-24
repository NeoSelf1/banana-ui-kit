package com.neon.core.ui.component.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.neon.core.ui.component.NeoIcon
import com.neon.core.ui.theme.Gray10
import com.neon.core.ui.theme.Gray30
import com.neon.core.ui.theme.Gray40
import com.neon.core.ui.theme.Gray60
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.NeoAnimations
import com.neon.core.ui.theme.NeoFont
import com.neon.core.ui.theme.Primary50

/**
 * 기본 텍스트 입력 필드 컴포넌트.
 *
 * 48.dp 높이의 둥근 모서리(8.dp) 입력 필드로, 포커스 상태에 따라 테두리 색상이
 * 애니메이션으로 전환됩니다. 포커스 시 [Primary50], 비포커스 시 [Gray30] 테두리가 적용됩니다.
 *
 * [isPassword]가 true이면 입력 텍스트가 마스킹 처리되고,
 * [isDeleteButtonPresent]가 true이면 우측에 삭제 아이콘이 표시되어 텍스트를 한번에 지울 수 있습니다.
 * [maxLength]를 초과하는 입력은 자동으로 무시됩니다.
 *
 * 내부적으로 [TextFieldValue]를 사용하여 커서 위치를 관리하며,
 * 외부 [value] 변경과 내부 상태를 동기화합니다.
 *
 * @param modifier 텍스트 필드에 적용할 Modifier.
 * @param value 현재 텍스트 값.
 * @param onValueChange 텍스트 변경 시 호출되는 콜백.
 * @param placeholder 텍스트가 비어있을 때 표시되는 힌트 텍스트.
 * @param maxLength 최대 입력 글자 수. 기본값은 99.
 * @param isPassword true이면 비밀번호 마스킹을 적용.
 * @param isDeleteButtonPresent true이면 우측에 텍스트 삭제 버튼을 표시.
 * @param keyboardOptions 키보드 유형 및 IME 액션 설정.
 * @param keyboardActions IME 액션 수행 시 호출되는 콜백.
 *
 * @see NeoAuthTextField
 * @see NeoSearchTextField
 */
@Composable
fun NeoTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int = 99,
    isPassword: Boolean = false,
    isDeleteButtonPresent: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
    keyboardActions: KeyboardActions = KeyboardActions()
) {
    val shape = RoundedCornerShape(size = 8.dp)
    val (isFocused, setFocused) = remember { mutableStateOf(false) }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            )
        )
    }

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Primary50 else Gray30,
        animationSpec = NeoAnimations.colorMediumTween(),
        label = "neoTextFieldBorderColor"
    )

    val clearButtonScale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = spring(),
        label = "clearButtonScale"
    )

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
            .border(width = 1.dp, color = borderColor, shape = shape)
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp)
            .background(color = Gray10, shape = shape),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (value.isEmpty()) {
            Text(placeholder, Modifier.align(Alignment.CenterStart), style = NeoFont.subhead6, color = Gray40)
        }

        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                if (newValue.text.length <= maxLength) {
                    onValueChange(newValue.text)
                    textFieldValue = newValue
                }
            },
            singleLine = true,
            textStyle = NeoFont.subhead6,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            cursorBrush = SolidColor(Gray80),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { setFocused(it.isFocused) }
        )

        if (isDeleteButtonPresent) {
            NeoIcon(
                Modifier.padding(8.dp).scale(clearButtonScale),
                name = "close_sm",
                color = Gray60
            ) {
                textFieldValue = TextFieldValue(
                    text = "",
                    selection = TextRange(0),
                )
                onValueChange("")
            }
        }
    }
}
