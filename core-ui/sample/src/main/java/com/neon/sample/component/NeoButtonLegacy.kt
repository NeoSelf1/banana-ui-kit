package com.neon.sample.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * @param modifier 바깥 여백을 padding으로 조절해주세요.
 *        버튼영역에 기본 background값이 있을 경우, BaseButton이 아닌 내부 요소에 직접 적용 후, paddingDp를 0으로 설정해주세요.
 *        modifier에 배경을 적용할 경우, 해당 색상이 차지하는 영역은 graphic layer로 적용되는 scale 애니메이션이 적용되지 않습니다.
 * Press 시 scale과 배경 애니메이션이 적용됩니다.
 * */

object NeoButtonLegacy {
    enum class TransitionType {
        // 크기가 줄어드는 효과만 제공합니다. 영역이 명확하게 정의된 대부분의 버튼에 적용합니다.
        Shrink,
        // 크기가 줄어드는 효과에 tilt효과를 추가합니다. 큰 feature를 수행하는 버튼에 적용합니다.
        ShrinkWithTilt,
        // 영역이 명확하지 않은 버튼에 제공합니다.
        /** @warning 4 padding이 기본 적용됩니다. */
        ShrinkWithGrayBackground,
    }

    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
        action: () -> Unit,
        transitionType: TransitionType = TransitionType.Shrink,
        isDisabled: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val backgroundAlphaTarget = 0.3f
        val scaleTarget = 0.97f
        val maxTiltAngle = 5f

        // Press 애니메이션
        val scale = remember { Animatable(1f) }
        val backgroundAlpha = remember { Animatable(0f) }
        val tiltX = remember { Animatable(0f) }
        val tiltY = remember { Animatable(0f) }
        val coroutineScope = rememberCoroutineScope()

        // 컴포넌트 크기 추적 (ShrinkWithTilt용)
        var componentSize by remember { mutableStateOf(IntSize.Zero) }

        Row(
            modifier = modifier
                .then(
                    if (transitionType == TransitionType.ShrinkWithGrayBackground) {
                        Modifier
                            .background(
                                Gray.copy(alpha = backgroundAlpha.value),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp)
                    } else {
                        Modifier
                    }
                )
                .onSizeChanged { size ->
                    if (transitionType == TransitionType.ShrinkWithTilt) {
                        componentSize = size
                    }
                }
                .graphicsLayer {
                    alpha = 1f - backgroundAlpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                    if (transitionType == TransitionType.ShrinkWithTilt) {
                        rotationX = tiltX.value
                        rotationY = tiltY.value
                    }
                }
                .pointerInput(isDisabled) {
                    detectTapGestures(
                        onPress = { offset ->
                            if (!isDisabled) {
                                // Press 시작 시 축소 및 배경 표시
                                coroutineScope.launch {
                                    backgroundAlpha.animateTo(
                                        targetValue = backgroundAlphaTarget,
                                        animationSpec = tween(100)
                                    )
                                }
                                coroutineScope.launch {
                                    scale.animateTo(
                                        targetValue = scaleTarget,
                                        animationSpec = tween(100)
                                    )
                                }

                                if (transitionType == TransitionType.ShrinkWithTilt && componentSize != IntSize.Zero) {
                                    // 탭 위치를 중심점 기준 상대 좌표로 변환 (-1.0 ~ 1.0)
                                    val centerX = componentSize.width / 2f
                                    val centerY = componentSize.height / 2f
                                    val relativeX = (offset.x - centerX) / (componentSize.width / 2f)
                                    val relativeY = (offset.y - centerY) / (componentSize.height / 2f)

                                    // rotationX: Y축 위치에 따라 상하 기울기 (위를 누르면 앞으로, 아래를 누르면 뒤로)
                                    val targetRotationX = -relativeY * maxTiltAngle
                                    // rotationY: X축 위치에 따라 좌우 기울기 (왼쪽을 누르면 왼쪽으로, 오른쪽을 누르면 오른쪽으로)
                                    val targetRotationY = relativeX * maxTiltAngle

                                    coroutineScope.launch {
                                        tiltX.animateTo(
                                            targetValue = targetRotationX,
                                            animationSpec = tween(100)
                                        )
                                    }
                                    coroutineScope.launch {
                                        tiltY.animateTo(
                                            targetValue = targetRotationY,
                                            animationSpec = tween(100)
                                        )
                                    }
                                }

                                // Press가 끝날 때까지 대기
                                tryAwaitRelease()

                                // Release 시 원래 크기로 복원 및 배경 숨김
                                coroutineScope.launch {
                                    scale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(400)
                                    )
                                }
                                coroutineScope.launch {
                                    backgroundAlpha.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(400)
                                    )
                                }
                                if (transitionType == TransitionType.ShrinkWithTilt) {
                                    coroutineScope.launch {
                                        tiltX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(400)
                                        )
                                    }
                                    coroutineScope.launch {
                                        tiltY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(400)
                                        )
                                    }
                                }
                            }
                        },
                        onTap = {
                            if (!isDisabled) {
                                action()
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}
