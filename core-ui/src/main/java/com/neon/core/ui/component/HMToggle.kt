package com.neon.core.ui.component

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Material3 Switch를 래핑한 토글 스위치 컴포넌트.
 *
 * 체크된 상태의 Thumb 색상을 흰색으로 고정하여 디자인 시스템에 맞춘 토글을 제공합니다.
 * 그 외의 색상(트랙, 비활성 등)은 Material3 기본 테마를 따른다.
 *
 * @param checked 토글의 현재 활성화 상태.
 * @param onCheckedChange 토글 상태 변경 시 호출되는 콜백.
 * @param modifier 토글에 적용할 Modifier.
 */
@Composable
fun HMToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(checkedThumbColor = Color.White)
    )
}
