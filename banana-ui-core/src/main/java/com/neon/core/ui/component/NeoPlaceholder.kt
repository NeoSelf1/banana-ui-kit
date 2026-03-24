package com.neon.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.neon.core.ui.theme.Gray50
import com.neon.core.ui.theme.BananaDesign

/**
 * 데이터가 없을 때 표시되는 빈 상태 플레이스홀더 컴포넌트.
 *
 * 화면 중앙에 안내 텍스트를 표시하여 사용자에게 현재 콘텐츠가 없음을 알립니다.
 * 화면 높이의 18%를 기준으로 동적 상단 패딩을 계산하여 (최소 80.dp, 최대 200.dp),
 * 다양한 화면 크기에서 자연스러운 위치에 표시되도록 합니다.
 *
 * 주로 NeoScrollList에서 빈 리스트 상태이거나, 검색 결과가 없을 때 사용됩니다.
 *
 * @param text 표시할 안내 텍스트. 중앙 정렬됩니다.
 * @param modifier 플레이스홀더 컨테이너에 적용할 Modifier.
 *
 * @see com.neon.core.ui.component.list.NeoInfinityScrollList
 */
@Composable
fun NeoPlaceholder(
    text: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val calculated = screenHeight * 0.18f
    val dynamicTopPadding = min(max(calculated, 80.dp), 200.dp)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text, modifier.padding(top = dynamicTopPadding), style = BananaDesign.typography.subhead6, color = Gray50, textAlign = TextAlign.Center)
    }
}
