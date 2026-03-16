package com.neon.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray10
import com.neon.core.ui.theme.Gray25
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.HMFont
import com.neon.core.ui.theme.Primary50

/**
 * 화면 상단에 표시되는 뒤로가기 헤더 컴포넌트.
 *
 * [HMBackHeaderType]에 따라 세 가지 형태로 렌더링된다:
 * - [HMBackHeaderType.Base]: 뒤로가기 버튼만 표시되는 기본 형태. 우측에 [rightContent]를 배치할 수 있습니다.
 * - [HMBackHeaderType.WithTitle]: 중앙에 타이틀 텍스트가 추가된 형태. 타이틀은 좌우 컨텐츠 사이에서 중앙 정렬됩니다.
 * - [HMBackHeaderType.WithProgress]: 하단에 애니메이션 프로그레스 바가 추가된 형태.
 *   percentage 값(0.0~1.0)에 따라 바의 너비가 spring 애니메이션으로 전환됩니다.
 *
 * 높이는 48.dp로 고정되며, 배경색은 [Gray10]이 적용됩니다.
 *
 * @param modifier 헤더에 적용할 Modifier.
 * @param headerType 헤더의 표시 유형. 기본값은 [HMBackHeaderType.Base].
 * @param rightContent 헤더 우측에 표시할 선택적 컴포저블. Base와 WithTitle 유형에서만 표시됩니다.
 * @param onBack 뒤로가기 버튼 탭 시 호출되는 콜백.
 *
 * @see HMBackHeaderType
 * @see HMScreen
 */
sealed class HMBackHeaderType {
    object Base : HMBackHeaderType()
    data class WithTitle(val title: String) : HMBackHeaderType()
    data class WithProgress(val percentage: Float) : HMBackHeaderType()
}

@Composable
fun HMBackHeader(
    modifier: Modifier = Modifier,
    headerType: HMBackHeaderType = HMBackHeaderType.Base,
    rightContent: @Composable (() -> Unit)? = null,
    onBack: () -> Unit
) {
    when (headerType) {
        is HMBackHeaderType.Base -> {
            Row(
                modifier.fillMaxWidth().height(48.dp).background(Gray10),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HMIcon(name = "chevron_left"){ onBack() }

                if (rightContent != null) {
                    rightContent()
                } else {
                    Spacer(Modifier.width(1.dp))
                }
            }
        }
        is HMBackHeaderType.WithTitle -> {
            Box(
                modifier.fillMaxWidth().height(48.dp).background(Gray10),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HMIcon(name = "chevron_left"){ onBack() }

                    if (rightContent != null) {
                        rightContent()
                    }
                }

                Text(headerType.title, Modifier.fillMaxWidth(), style = HMFont.subhead5, color = Gray80, textAlign = TextAlign.Center)
            }
        }

        is HMBackHeaderType.WithProgress -> {
            val animatedProgress by animateFloatAsState(
                targetValue = headerType.percentage,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f
                ),
                label = "progress"
            )

            Column(
                modifier.fillMaxWidth().background(Gray10)
            ) {
                HMIcon(
                    Modifier.padding(top = 8.dp).padding(bottom = 24.dp),
                    name = "chevron_left",
                    color = Gray80,
                ){ onBack() }

                Box(
                    Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Gray25, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .background(Primary50, RoundedCornerShape(2.dp))
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
