package com.neon.core.ui.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray10
import com.neon.core.ui.theme.Gray45
import com.neon.core.ui.theme.Gray50
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.NeoFont
import com.neon.core.ui.theme.Primary50

/**
 * 범용 다이얼로그 컴포넌트.
 *
 * @param onDismissRequest 다이얼로그 외부 탭 또는 뒤로가기 시 호출되는 콜백.
 * @param title 다이얼로그 타이틀 텍스트.
 * @param message 본문 메시지 텍스트. null이면 본문을 표시하지 않습니다.
 * @param confirmText 확인 버튼 텍스트.
 * @param onConfirm 확인 버튼 탭 시 호출되는 콜백.
 * @param dismissText 취소 버튼 텍스트. null이면 취소 버튼을 표시하지 않습니다.
 * @param onDismiss 취소 버튼 탭 시 호출되는 콜백. null이면 [onDismissRequest]가 사용됩니다.
 * @param containerColor 다이얼로그 배경색.
 * @param titleColor 타이틀 텍스트 색상.
 * @param messageColor 본문 텍스트 색상.
 * @param confirmButtonColor 확인 버튼 배경색.
 * @param confirmTextColor 확인 버튼 텍스트 색상.
 * @param dismissTextColor 취소 버튼 텍스트 색상.
 * @param icon 타이틀 위에 표시할 아이콘 컴포저블.
 * @param extraContent 메시지 아래에 추가할 커스텀 컨텐츠.
 */
@Composable
fun NeoDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    message: String? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    containerColor: Color = Gray10,
    titleColor: Color = Gray80,
    messageColor: Color = Gray50,
    confirmButtonColor: Color = Primary50,
    confirmTextColor: Color = Gray10,
    dismissTextColor: Color = Gray45,
    icon: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = containerColor,
        shape = RoundedCornerShape(12.dp),
        icon = icon,
        title = {
            Text(
                text = title,
                style = NeoFont.headline4.copy(color = titleColor),
                textAlign = TextAlign.Center
            )
        },
        text = if (message != null || extraContent != null) {
            {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (message != null) {
                        Text(
                            text = message,
                            style = NeoFont.subhead6.copy(color = messageColor),
                            textAlign = TextAlign.Center
                        )
                    }
                    if (extraContent != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        extraContent()
                    }
                }
            }
        } else null,
        dismissButton = if (dismissText != null) {
            {
                TextButton(onClick = onDismiss ?: onDismissRequest) {
                    Text(
                        text = dismissText,
                        style = NeoFont.subhead5.copy(color = dismissTextColor)
                    )
                }
            }
        } else null,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmButtonColor),
                shape = RoundedCornerShape(8.dp),
                modifier = if (dismissText == null) Modifier.fillMaxWidth() else Modifier
            ) {
                Text(
                    text = confirmText,
                    style = NeoFont.subhead5.copy(
                        color = confirmTextColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    )
}
