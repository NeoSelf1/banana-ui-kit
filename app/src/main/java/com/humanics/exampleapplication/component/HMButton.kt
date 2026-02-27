package com.humanics.exampleapplication.component

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
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
 * - pointerInput, onSizeChanged, graphicsLayer, background를 단일 노드로 통합
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
 * Modifier.Node: pointer input, draw(background + transform), size tracking을 단일 노드에서 처리합니다.
 *
 * 최적화 포인트:
 * - Animatable 상태를 노드에 직접 저장 → remember 6개 제거
 * - DrawModifierNode로 background alpha를 draw phase에서 처리 → 리컴포지션 방지
 * - SuspendingPointerInputModifierNode 위임 → composed pointerInput 제거
 * - LayoutAwareModifierNode로 크기 추적 → composed onSizeChanged 제거
 */
private class HMButtonNode(
    var transitionType: HMButton.TransitionType,
    var isDisabled: Boolean,
    var action: () -> Unit,
) : DrawModifierNode, DelegatingNode(), LayoutAwareModifierNode {
    // 애니메이션 상태 (노드에 직접 저장 — remember 불필요)
    private val scale = Animatable(1f)
    private val backgroundAlpha = Animatable(0f)
    private val tiltX = Animatable(0f)
    private val tiltY = Animatable(0f)

    // ShrinkWithTilt용 컴포넌트 크기 (mutableStateOf 불필요)
    private var componentSize = IntSize.Zero

    // 3D 회전용 객체 (재사용하여 매 프레임 할당 방지)
    private val camera = Camera()
    private val cameraMatrix = Matrix()
    private val layerPaint = Paint()

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

    // DrawModifierNode: background와 transform을 draw phase에서 처리 (리컴포지션 방지)
    override fun ContentDrawScope.draw() {
        // Animatable.value 읽기는 draw phase에서 자동으로 추적되어 값 변경 시 invalidateDraw
        val bgAlpha = backgroundAlpha.value
        val currentScale = scale.value
        val currentAlpha = 1f - bgAlpha
        val currentTiltX = tiltX.value
        val currentTiltY = tiltY.value

        // 1. ShrinkWithGrayBackground: 배경 그리기 (transform 적용 전, 원본 크기 유지)
        if (transitionType == HMButton.TransitionType.ShrinkWithGrayBackground && bgAlpha > 0f) {
            drawRoundRect(
                color = Gray.copy(alpha = bgAlpha),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
        }

        // 2. Transform 적용 후 컨텐츠 그리기
        val hasTransform = currentScale != 1f || currentAlpha != 1f ||
                currentTiltX != 0f || currentTiltY != 0f

        if (hasTransform) {
            val nativeCanvas = drawContext.canvas.nativeCanvas
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Alpha 레이어 (graphicsLayer의 alpha와 동일한 효과)
            layerPaint.alpha = (currentAlpha.coerceIn(0f, 1f) * 255).toInt()
            nativeCanvas.saveLayer(0f, 0f, size.width, size.height, layerPaint)

            // Scale (중심점 기준)
            nativeCanvas.save()
            nativeCanvas.translate(cx, cy)
            nativeCanvas.scale(currentScale, currentScale)

            // 3D Rotation (ShrinkWithTilt 전용)
            if (transitionType == HMButton.TransitionType.ShrinkWithTilt &&
                (currentTiltX != 0f || currentTiltY != 0f)
            ) {
                camera.save()
                camera.rotateX(currentTiltX)
                camera.rotateY(currentTiltY)
                camera.getMatrix(cameraMatrix)
                camera.restore()
                nativeCanvas.concat(cameraMatrix)
            }

            nativeCanvas.translate(-cx, -cy)

            drawContent()

            nativeCanvas.restore() // scale + rotation
            nativeCanvas.restore() // alpha layer
        } else {
            drawContent()
        }
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