package com.neon.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray15

/**
 * 섹션 간 시각적 구분을 위한 수평 구분선 컴포넌트.
 *
 * 전체 너비를 채우는 8.dp 높이의 배경 블록으로, 리스트나 콘텐츠 영역 사이에
 * 섹션 구분을 표현할 때 사용합니다. 일반적인 1.dp 라인 구분선이 아닌,
 * 영역 구분용 두꺼운 배경 블록 형태이다.
 *
 * @param modifier 구분선에 적용할 Modifier.
 * @param color 구분선의 배경색. 기본값은 [Gray15].
 */
@Composable
fun NeoDivider(modifier: Modifier = Modifier, color: Color = Gray15) {
    Row(modifier.fillMaxWidth().height(8.dp).background(color)){}
}
