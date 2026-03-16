package com.neon.core.ui.component.button

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray25
import kotlinx.coroutines.launch

/**
 * 터치 애니메이션이 적용되는 범용 클릭 래퍼 컴포넌트.
 *
 * 자식 컴포저블을 감싸서 Press/Release 시 축소(scale) 및 배경 애니메이션을 적용합니다.
 * iOS의 버튼 터치 피드백과 유사한 사용자 경험을 제공하며, [TransitionType]에 따라
 * 세 가지 전환 효과를 지원합니다:
 *
 * - [TransitionType.Shrink]: 크기가 줄어드는 효과. 영역이 명확한 대부분의 버튼에 사용합니다.
 * - [TransitionType.ShrinkWithTilt]: 축소 + 3D 기울기 효과. 탭 위치에 따라 최대 5도까지
 *   기울어지며, 큰 카드형 버튼이나 feature 버튼에 적합합니다.
 * - [TransitionType.ShrinkWithGrayBackground]: 축소 + 회색 배경 하이라이트. 영역이 명확하지
 *   않은 텍스트 버튼이나 리스트 아이템에 사용합니다. 4.dp 패딩이 자동 적용됩니다.
 *
 * Press 시 100ms 동안 0.97 스케일로 축소되고, Release 시 400ms 동안 원래 크기로 복원됩니다.
 *
 * 내부적으로 Modifier.Node API를 사용하여 애니메이션 중 리컴포지션을 완전히 제거합니다.
 * pointerInput, draw, layout 감지를 단일 노드(HMClickableModifierNode)로 통합하여
 * remember, mutableStateOf, rememberCoroutineScope 없이 애니메이션을 처리합니다.
 *
 * @param modifier 바깥 여백을 padding으로 조절해주세요.
 *        버튼영역에 기본 background값이 있을 경우, BaseButton이 아닌 내부 요소에 직접 적용 후, paddingDp를 0으로 설정해주세요.
 *        modifier에 배경을 적용할 경우, 해당 색상이 차지하는 영역은 graphic layer로 적용되는 scale 애니메이션이 적용되지 않습니다.
 * @param action 탭 시 호출되는 콜백.
 * @param transitionType 터치 전환 효과 유형. 기본값은 [TransitionType.Shrink].
 * @param isDisabled true이면 터치 이벤트와 애니메이션을 비활성화.
 * @param content 클릭 영역 내부에 표시할 컴포저블.
 *
 * @see TransitionType
 */

object HMClickable {
    enum class TransitionType {
        Shrink,
        ShrinkWithTilt,
        /** 주의: 4 padding이 기본 적용됩니다. */
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
        Row(
            modifier = modifier
                .then(HMClickableElement(transitionType, isDisabled, action))
                .then(
                    if (transitionType == TransitionType.ShrinkWithGrayBackground)
                        Modifier.padding(4.dp)
                    else
                        Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

/**
 * [ModifierNodeElement]: HMClickableModifierNode의 생성 및 업데이트를 관리합니다.
 */
private class HMClickableElement(
    private val transitionType: HMClickable.TransitionType,
    private val isDisabled: Boolean,
    private val action: () -> Unit,
) : ModifierNodeElement<HMClickableNode>() {
    override fun create() = HMClickableNode(transitionType, isDisabled, action)

    override fun update(node: HMClickableNode) {
        node.update(transitionType, isDisabled, action)
    }

    override fun hashCode(): Int {
        var result = transitionType.hashCode()
        result = 31 * result + isDisabled.hashCode()
        result = 31 * result + action.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HMClickableElement) return false
        return transitionType == other.transitionType &&
                isDisabled == other.isDisabled &&
                action == other.action
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "hmButton"
        properties["transitionType"] = transitionType
        properties["isDisabled"] = isDisabled
    }
}

/**
 * [Modifier.Node]: pointer input, draw(background + transform), size tracking을 단일 노드에서 처리합니다.
 *
 * 최적화 포인트:
 * - Animatable 상태를 노드에 직접 저장 → remember 4개, rememberCoroutineScope 1개, mutableStateOf 1개 제거 (Slot Table 오버헤드 제거)
 * - LayoutModifierNode.placeWithLayer로 RenderNode 기반 GPU 변환
 * - DrawModifierNode는 ShrinkWithGrayBackground 배경 전용 (transform 전 원본 크기)
 * - SuspendingPointerInputModifierNode 위임으로 pointerInput 기능 통합
 * - LayoutAwareModifierNode.onRemeasured()로 크기 추적 기능 통합
 * - 4~5개의 개별 modifier 노드를 단일 DelegatingNode로 통합 → modifier chain 단축
 */
private class HMClickableNode(
    var transitionType: HMClickable.TransitionType,
    var isDisabled: Boolean,
    var action: () -> Unit,
) : DelegatingNode(),
    LayoutModifierNode,         // RenderNode 기반 scale/alpha/rotation (GPU 가속)
    DrawModifierNode,           // ShrinkWithGrayBackground 배경 전용
    LayoutAwareModifierNode {   // 크기 추적

    // 애니메이션 상태 (노드에 직접 저장 — remember 불필요)
    private val scale = Animatable(1f)
    private val backgroundAlpha = Animatable(0f)
    private val tiltX = Animatable(0f)
    private val tiltY = Animatable(0f)
    // ShrinkWithTilt용 컴포넌트 크기 (mutableStateOf 불필요)
    private var componentSize = IntSize.Zero

    // Pointer input 위임: 제스처 감지를 노드 수준에서 처리
    private val pointerInputNode = delegate(
        SuspendingPointerInputModifierNode {
            detectTapGestures(
                onPress = { offset ->
                    if (!this@HMClickableNode.isDisabled) {
                        this@HMClickableNode.animatePress(offset)
                        tryAwaitRelease()
                        this@HMClickableNode.animateRelease()
                    }
                },
                onTap = {
                    if (!this@HMClickableNode.isDisabled) {
                        this@HMClickableNode.action()
                    }
                }
            )
        }
    )

    fun update(
        newTransitionType: HMClickable.TransitionType,
        newIsDisabled: Boolean,
        newAction: () -> Unit,
    ) {
        val needsPointerReset = isDisabled != newIsDisabled
        transitionType = newTransitionType
        isDisabled = newIsDisabled
        action = newAction
        if (needsPointerReset) {
            pointerInputNode.resetPointerInputHandler()
        }
    }

    // LayoutAwareModifierNode: ShrinkWithTilt용 크기 추적
    override fun onRemeasured(size: IntSize) {
        if (transitionType == HMClickable.TransitionType.ShrinkWithTilt) {
            componentSize = size
        }
    }

    // LayoutModifierNode: RenderNode 기반 GPU 가속 변환
    // placeWithLayer 내부에서 State를 읽으므로 layer invalidation만 발생 (Composition/Layout 건너뜀)
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = this@HMClickableNode.scale.value
                scaleY = this@HMClickableNode.scale.value
                alpha = 1f - this@HMClickableNode.backgroundAlpha.value
                transformOrigin = TransformOrigin.Center

                if (this@HMClickableNode.transitionType == HMClickable.TransitionType.ShrinkWithTilt) {
                    rotationX = this@HMClickableNode.tiltX.value
                    rotationY = this@HMClickableNode.tiltY.value
                }
            }
        }
    }

    // DrawModifierNode: ShrinkWithGrayBackground 배경만 처리 (transform 전 원본 크기에 그리기)
    override fun ContentDrawScope.draw() {
        if (transitionType == HMClickable.TransitionType.ShrinkWithGrayBackground) {
            val bgAlpha = backgroundAlpha.value
            if (bgAlpha > 0f) {
                drawRoundRect(
                    color = Gray25.copy(alpha = bgAlpha),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
            }
        }
        drawContent()
    }

    // Press 시작: 축소 + 배경 + tilt 애니메이션
    private fun animatePress(offset: Offset) {
        coroutineScope.launch {
            backgroundAlpha.animateTo(BACKGROUND_ALPHA_TARGET, tween(PRESS_DURATION))
        }
        coroutineScope.launch {
            scale.animateTo(SCALE_TARGET, tween(PRESS_DURATION))
        }

        if (transitionType == HMClickable.TransitionType.ShrinkWithTilt && componentSize != IntSize.Zero) {
            // 탭 위치를 중심점 기준 상대 좌표로 변환 (-1.0 ~ 1.0)
            val centerX = componentSize.width / 2f
            val centerY = componentSize.height / 2f
            val relativeX = (offset.x - centerX) / (componentSize.width / 2f)
            val relativeY = (offset.y - centerY) / (componentSize.height / 2f)

            // rotationX: Y축 위치에 따라 상하 기울기
            val targetRotationX = -relativeY * MAX_TILT_ANGLE
            // rotationY: X축 위치에 따라 좌우 기울기
            val targetRotationY = relativeX * MAX_TILT_ANGLE

            coroutineScope.launch {
                tiltX.animateTo(targetRotationX, tween(PRESS_DURATION))
            }
            coroutineScope.launch {
                tiltY.animateTo(targetRotationY, tween(PRESS_DURATION))
            }
        }
    }

    // Release: 원래 상태로 복원 애니메이션
    private fun animateRelease() {
        coroutineScope.launch { scale.animateTo(1f, tween(RELEASE_DURATION)) }
        coroutineScope.launch { backgroundAlpha.animateTo(0f, tween(RELEASE_DURATION)) }

        if (transitionType == HMClickable.TransitionType.ShrinkWithTilt) {
            coroutineScope.launch { tiltX.animateTo(0f, tween(RELEASE_DURATION)) }
            coroutineScope.launch { tiltY.animateTo(0f, tween(RELEASE_DURATION)) }
        }
    }

    companion object {
        private const val BACKGROUND_ALPHA_TARGET = 0.3f
        private const val SCALE_TARGET = 0.97f
        private const val MAX_TILT_ANGLE = 5f
        private const val PRESS_DURATION = 100
        private const val RELEASE_DURATION = 400
    }
}