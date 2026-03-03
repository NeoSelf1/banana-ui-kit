package com.humanics.exampleapplication.component

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
import androidx.compose.ui.graphics.Color.Companion.Gray
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
import kotlinx.coroutines.launch

/**
 * @param modifier 바깥 여백을 padding으로 조절해주세요.
 *        버튼영역에 기본 background값이 있을 경우, BaseButton이 아닌 내부 요소에 직접 적용 후, paddingDp를 0으로 설정해주세요.
 *        modifier에 배경을 적용할 경우, 해당 색상이 차지하는 영역은 graphic layer로 적용되는 scale 애니메이션이 적용되지 않습니다.
 * Press 시 scale과 배경 애니메이션이 적용됩니다.
 *
 * Modifier.Node API로 최적화:
 * - 애니메이션 중 리컴포지션 제거 (remember/mutableStateOf 불필요)
 * - pointerInput, onSizeChanged, graphicsLayer, background에 해당하는 기능을 단일 DelegatingNode로 통합
 *   → modifier chain 노드 수 감소 및 노드 간 탐색 오버헤드 감소
 * - coroutineScope를 노드에서 직접 접근 (rememberCoroutineScope 불필요)
 * */

object HMButton {
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
        Row(
            modifier = modifier
                .then(HMButtonElement(transitionType, isDisabled, action))
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
 * ModifierNodeElement: HMButtonModifierNode의 생성 및 업데이트를 관리합니다.
 */
private class HMButtonElement(
    private val transitionType: HMButton.TransitionType,
    private val isDisabled: Boolean,
    private val action: () -> Unit,
) : ModifierNodeElement<HMButtonNode>() {
    override fun create() = HMButtonNode(transitionType, isDisabled, action)

    override fun update(node: HMButtonNode) {
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
        if (other !is HMButtonElement) return false
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
 * Modifier.Node: pointer input, RenderNode 기반 transform, background, size tracking을 단일 노드에서 처리합니다.
 *
 * 최적화 포인트:
 * - Animatable 상태를 노드에 직접 저장 → remember 4개, rememberCoroutineScope 1개, mutableStateOf 1개 제거 (Slot Table 오버헤드 제거)
 * - LayoutModifierNode.placeWithLayer로 RenderNode 기반 GPU 변환
 * - DrawModifierNode는 ShrinkWithGrayBackground 배경 전용 (transform 전 원본 크기)
 * - SuspendingPointerInputModifierNode 위임으로 pointerInput 기능 통합
 * - LayoutAwareModifierNode.onRemeasured()로 크기 추적 기능 통합
 * - 4~5개의 개별 modifier 노드를 단일 DelegatingNode로 통합 → modifier chain 단축
 */
private class HMButtonNode(
    var transitionType: HMButton.TransitionType,
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
                    if (!this@HMButtonNode.isDisabled) {
                        this@HMButtonNode.animatePress(offset)
                        tryAwaitRelease()
                        this@HMButtonNode.animateRelease()
                    }
                },
                onTap = {
                    if (!this@HMButtonNode.isDisabled) {
                        this@HMButtonNode.action()
                    }
                }
            )
        }
    )

    fun update(
        newTransitionType: HMButton.TransitionType,
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
        if (transitionType == HMButton.TransitionType.ShrinkWithTilt) {
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
                scaleX = this@HMButtonNode.scale.value
                scaleY = this@HMButtonNode.scale.value
                alpha = 1f - this@HMButtonNode.backgroundAlpha.value
                transformOrigin = TransformOrigin.Center

                if (this@HMButtonNode.transitionType == HMButton.TransitionType.ShrinkWithTilt) {
                    rotationX = this@HMButtonNode.tiltX.value
                    rotationY = this@HMButtonNode.tiltY.value
                }
            }
        }
    }

    // DrawModifierNode: ShrinkWithGrayBackground 배경만 처리 (transform 전 원본 크기에 그리기)
    override fun ContentDrawScope.draw() {
        if (transitionType == HMButton.TransitionType.ShrinkWithGrayBackground) {
            val bgAlpha = backgroundAlpha.value
            if (bgAlpha > 0f) {
                drawRoundRect(
                    color = Gray.copy(alpha = bgAlpha),
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

        if (transitionType == HMButton.TransitionType.ShrinkWithTilt && componentSize != IntSize.Zero) {
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

        if (transitionType == HMButton.TransitionType.ShrinkWithTilt) {
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