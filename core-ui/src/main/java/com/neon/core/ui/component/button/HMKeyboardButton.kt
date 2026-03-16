package com.neon.core.ui.component.button

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray10
import com.neon.core.ui.theme.Gray25
import com.neon.core.ui.theme.Gray70
import com.neon.core.ui.theme.HMFont
import com.neon.core.ui.theme.Primary50

/**
 * 키보드 상단에 고정되어 표시되는 액션 버튼 컴포넌트.
 *
 * imePadding 및 navigationBarsPadding이 적용된 화면에서 키보드 바로 위에 위치하며,
 * 전체 너비를 채우는 56.dp 높이의 버튼으로 렌더링됩니다.
 *
 * 로딩 상태에서는 텍스트가 페이드 아웃되고 [CircularProgressIndicator]가 페이드 인되며,
 * 비활성화 상태에서는 배경색이 [Gray25], 텍스트 색상이 [Gray70]으로 변경됩니다.
 * 활성화 상태에서는 [Primary50] 배경에 [Gray10] 텍스트가 표시됩니다.
 *
 * @param modifier 버튼에 적용할 Modifier.
 * @param text 버튼에 표시할 텍스트.
 * @param isLoading true이면 로딩 인디케이터를 표시하고 클릭을 비활성화.
 * @param isDisabled true이면 비활성화 스타일을 적용하고 클릭을 비활성화.
 * @param onTap 버튼 탭 시 호출되는 콜백.
 *
 * @see HMScreen
 */
@Composable
fun HMKeyboardButton(
    modifier: Modifier = Modifier,
    text: String,
    isLoading: Boolean = false,
    isDisabled: Boolean = false,
    onTap: () -> Unit,
) {
    val backgroundColor = if (isDisabled) Gray25 else Primary50
    val textColor = if (isDisabled) Gray70 else Gray10
    
    val loadingAlpha by animateFloatAsState(
        targetValue = if (isLoading) 1f else 0f,
        animationSpec = spring(),
        label = "loadingAlpha"
    )
    
    val textAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = spring(),
        label = "textAlpha"
    )

    /// KeyboardButton이 적용된 뷰들은 imePadding과 NavigationBarPadding이 추가로 주입됩니다.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(0.dp))
            .background(backgroundColor)
            .clickable(enabled = !isDisabled && !isLoading) { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, Modifier.alpha(textAlpha), style = HMFont.subhead4, color = textColor)
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .alpha(loadingAlpha),
                color = Gray10,
                strokeWidth = 2.dp
            )
        }
    }
}
