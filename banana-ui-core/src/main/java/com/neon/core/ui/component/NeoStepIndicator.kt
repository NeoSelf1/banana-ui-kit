package com.neon.core.ui.component

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.BananaDesign
import com.neon.core.ui.theme.Gray50
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.Primary50

/**
 * 단계 진행률을 시각적으로 표시하는 스텝 인디케이터 컴포넌트.
 *
 * 원형 인디케이터와 연결선으로 구성되며, 현재 활성 단계에는
 * 글로우 애니메이션이 적용됩니다. 각 단계 아래에 라벨 텍스트가 표시됩니다.
 *
 * @param steps 각 단계의 라벨 텍스트 목록.
 * @param currentIndex 현재 활성 단계의 인덱스 (0부터 시작).
 * @param modifier 인디케이터에 적용할 Modifier.
 * @param activeColor 활성 단계의 원형 및 연결선 색상.
 * @param inactiveColor 비활성 단계의 원형 배경색.
 * @param activeLabelColor 활성 단계의 라벨 텍스트 색상.
 * @param inactiveLabelColor 비활성 단계의 라벨 텍스트 색상.
 * @param dotColor 활성 원형 내부 점 색상.
 * @param circleSize 원형 인디케이터 크기.
 * @param connectorWidth 단계 간 연결선 너비.
 */
@Composable
fun NeoStepIndicator(
    steps: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Primary50,
    inactiveColor: Color = Gray80.copy(alpha = 0.05f),
    activeLabelColor: Color = Gray80,
    inactiveLabelColor: Color = Gray50,
    dotColor: Color = Color.White,
    circleSize: Dp = 32.dp,
    connectorWidth: Dp = 16.dp,
) {
    val typography = BananaDesign.typography

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.height(circleSize + 20.dp)
    ) {
        steps.forEachIndexed { index, label ->
            val isActive = index == currentIndex

            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isActive) {
                        ActiveCircle(
                            activeColor = activeColor,
                            dotColor = dotColor,
                            size = circleSize
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(circleSize)
                                .background(inactiveColor, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = label,
                        style = typography.caption1,
                        color = if (isActive) activeLabelColor else inactiveLabelColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                if (index < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(top = circleSize / 2 - 1.dp)
                            .width(connectorWidth)
                            .height(2.dp)
                            .background(
                                if (isActive) activeColor else inactiveColor
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveCircle(
    activeColor: Color,
    dotColor: Color,
    size: Dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = activeColor.copy(alpha = glowAlpha).toArgb()
                            setShadowLayer(
                                24f,
                                0f,
                                8f,
                                activeColor.copy(alpha = glowAlpha).toArgb()
                            )
                        }
                    }
                    canvas.drawCircle(
                        center = center,
                        radius = this.size.minDimension / 2f,
                        paint = paint
                    )
                }
            }
            .background(activeColor, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
    }
}
