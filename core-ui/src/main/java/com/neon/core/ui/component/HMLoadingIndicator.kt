package com.neon.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min

/**
 * 화면 중앙에 표시되는 로딩 인디케이터 컴포넌트.
 *
 * 24.dp 크기의 [CircularProgressIndicator]를 화면 중앙에 배치합니다.
 * 화면 높이의 18%를 기준으로 동적 상단 패딩을 계산하여 (최소 80.dp, 최대 200.dp),
 * 다양한 화면 크기에서 시각적으로 적절한 위치에 표시되도록 합니다.
 *
 * 단독으로 사용하거나 [HMScreen]의 isLoading 상태와 함께 사용할 수 있습니다.
 *
 * @param modifier 인디케이터 컨테이너에 적용할 Modifier.
 *
 * @see HMScreen
 */
@Composable
fun HMLoadingIndicator(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val calculated = screenHeight * 0.18f
    val dynamicTopPadding = min(max(calculated, 80.dp), 200.dp)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(Modifier.size(24.dp).padding(top = dynamicTopPadding))
    }
}
