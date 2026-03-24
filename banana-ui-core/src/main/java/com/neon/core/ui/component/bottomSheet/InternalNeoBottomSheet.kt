package com.neon.core.ui.component.bottomSheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray10
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Custom BottomSheet State Definition
 */
enum class NeoSheetStatus {
    HIDDEN,
    ANIMATING,
    EXPANDED
}

/** [Stable] 어노테이션
 *
 * 해당 클래스 인스턴스가 recompose를 트리거할 일이 없다고 전달하는 어노테이션입니다.
 * 이로 인해 Compose는 recomposition 판단 기준에서 해당 클래스를 배제합니다.
 * 이를 충족시키기 위해선 모든 public 프로퍼티가 stable 타입 즉, 기본 타입이거나 val 타입이여야 하며,
 * stable하지 않은 public 프로퍼티가 존재할 경우, 변경 시 Composition에 알림이 가야합니다..
 *
 * NeoBottomSheetController는 MutableState로 var 프로퍼티의 변경을 알리고 있기에 Stable 조건을 충족합니다.
 * 이로써, Controller를 사용하는 Composable 내부에서 불필요한 Recomposition을 방지합니다.
 *
 *
 * [NeoBottomSheetController]
 *
 * NeoBottomSheetController는 기존 NeoBottomSheetState와 NeoCustomBottomSheetNavigator를 통합한 클래스입니다.
 * 바텀시트의 속성 저장, 관리 및 애니메이션 관리를 단일 클래스에서 담당하며, 이를 통해 다수의 LaunchedEffect로 인한 데이터 경쟁 조건을 방지합니다.
 * */
@Stable
class NeoBottomSheetController(val scope: CoroutineScope) {

    // ==================== 애니메이션 상태 (기존 NeoBottomSheetState) ====================

    /**
     * private set - NeoBottomSheetController 내장 메서드를 통해서만 변경이 가능토록 하여 예상치 못한 외부 변경을 방지
     */
    var status by mutableStateOf(NeoSheetStatus.HIDDEN)
        private set

    // 바텀시트가 완전히 펼쳐진 상태(EXPANDED)를 기준으로 얼마나 아래로 내려가 있는지(이동했는지)를 나타내는 값
    val offsetY = Animatable(0f)

    var sheetHeightPx by mutableFloatStateOf(0f)
        private set

    // 바텀시트 높이값 수정을 담당하는 이벤트입니다.
    var onSlide: ((Float) -> Unit)? = null

    // 실제 바텀시트 state를 0.0(hidden)에서 1.0(expanded) 사이의 수치로 치환한 값입니다.
    val progress: Float
        get() {
            if (sheetHeightPx == 0f) return 0f
            return 1f - (offsetY.value / sheetHeightPx).coerceIn(0f, 1f)
        }

    // ==================== 바텀시트 속성 (기존 NeoCustomBottomSheetNavigator) ====================

    var content by mutableStateOf<@Composable () -> Unit>({})
        private set

    var height by mutableStateOf(280.dp)
        private set

    private var onDismissCallback: (() -> Unit)? = null
    private var onTapBackNavBtnCallback: (() -> Unit)? = null

    var onTapScrimDismissEnabled by mutableStateOf(true)
        private set

    var isDragDismissEnabled by mutableStateOf(true)
        private set

    // ==================== 애니메이션 메서드 ====================

    // Initialize state - 바텀시트 높이 업데이트
    // 주의: 애니메이션 진행 중에는 snapTo()를 호출하지 않습니다. snapTo()가 animateTo()를 취소시키기 때문입니다.
    internal fun updateSheetHeight(heightPx: Float) {
        if (this.sheetHeightPx != heightPx) {
            this.sheetHeightPx = heightPx
            // 애니메이션 중이면 높이만 업데이트하고 offset 조정은 스킵
            if (status == NeoSheetStatus.ANIMATING) return

            // 만일 HIDDEN 상태이면, offset을 강제로 조정합니다.
            if (status == NeoSheetStatus.HIDDEN) {
                scope.launch {
                    offsetY.snapTo(heightPx)
                }
            } else if (status == NeoSheetStatus.EXPANDED) {
                scope.launch {
                    offsetY.snapTo(0f)
                }
            }
        } else {
            // 애니메이션 중이면 스킵
            if (status == NeoSheetStatus.ANIMATING) return

            // 만일 status가 HIDDEN인데 offset가 height와 불일치할 경우는 offset을 강제로 heightPx에 맞춥니다.
            if (status == NeoSheetStatus.HIDDEN && offsetY.value != heightPx) {
                scope.launch { offsetY.snapTo(heightPx) }
            }
        }
    }

    internal suspend fun animateTo(target: NeoSheetStatus) {
        // 애니메이션 진행 중이거나 ANIMATING이 target으로 들어오면 무시
        if (status == NeoSheetStatus.ANIMATING) return

        val targetOffset = when (target) {
            NeoSheetStatus.EXPANDED -> 0f
            NeoSheetStatus.HIDDEN -> sheetHeightPx
            NeoSheetStatus.ANIMATING -> return // ANIMATING은 target이 될 수 없음
        }

        status = NeoSheetStatus.ANIMATING

        try {
            offsetY.animateTo(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )

            // Ensure final value sync
            if (target == NeoSheetStatus.EXPANDED && offsetY.value != 0f) offsetY.snapTo(0f)
            if (target == NeoSheetStatus.HIDDEN && offsetY.value != sheetHeightPx) offsetY.snapTo(sheetHeightPx)
        } finally {
            status = target
        }
    }

    // 실제 바텀시트 높이값을 특정 수치로 이동시킵니다.
    internal fun dispatchSlide() {
        onSlide?.invoke(progress)
    }

    // ==================== 바텀시트 제어 메서드 ====================

    /**
     * 바텀시트를 표시합니다.
     * 데이터 경쟁 조건 방지를 위해 상태 변경과 애니메이션을 순차적으로 처리합니다.
     */
    fun show(
        height: Dp = 280.dp,
        onDismiss: () -> Unit,
        onTapBackNavBtn: () -> Unit,
        isOutTapDismissEnabled: Boolean = true,
        isDragDismissEnabled: Boolean = true,
        content: @Composable () -> Unit
    ) {
        // 애니메이션 진행 중이면 무시
        if (status == NeoSheetStatus.ANIMATING) return

        // 속성 설정
        this.height = height
        this.onDismissCallback = onDismiss
        this.onTapBackNavBtnCallback = onTapBackNavBtn
        this.onTapScrimDismissEnabled = isOutTapDismissEnabled
        this.isDragDismissEnabled = isDragDismissEnabled
        this.content = content

        // 애니메이션 시작
        scope.launch {
            animateTo(NeoSheetStatus.EXPANDED)
        }
    }

    /** 자식뷰의 ViewModel 비즈니스 로직을 통해 NeoBottomSheet에 주입되는 isPresent가 false로 변경되면,
     * NeoCustomBottomSheetProvider가 이를 감지하여 hide()를 호출합니다.
     *  */
    fun hide() {
        // 애니메이션 진행 중이면 무시
        if (status == NeoSheetStatus.ANIMATING) return

        scope.launch {
            animateTo(NeoSheetStatus.HIDDEN)
        }
    }

    // Called by the NeoCustomBottomSheet (UI) when it is dismissed by User Interaction
    internal fun onSheetDismiss() {
        // 애니메이션 진행 중이면 무시
        if (status == NeoSheetStatus.ANIMATING) return


        scope.launch {
            animateTo(NeoSheetStatus.HIDDEN)
            onDismissCallback?.invoke()
        }
    }

    internal fun onTapSheetBackNavBtn() {
        // 애니메이션 진행 중이면 무시
        if (status == NeoSheetStatus.ANIMATING) return
        onTapBackNavBtnCallback?.invoke()
    }

    // 드래그 완료 후 상태 결정 및 애니메이션 처리
    internal fun handleDragEnd(velocityY: Float) {
        if (status == NeoSheetStatus.ANIMATING) return

        val currentY = offsetY.value
        val thresholdVelocity = 500f // arbitrary threshold for "Fast"
        val isFlingDown = velocityY > thresholdVelocity
        val isFlingUp = velocityY < -thresholdVelocity

        val target = if (isFlingDown) {
            NeoSheetStatus.HIDDEN
        } else if (isFlingUp) {
            NeoSheetStatus.EXPANDED
        } else {
            if (currentY > sheetHeightPx / 2) NeoSheetStatus.HIDDEN else NeoSheetStatus.EXPANDED // Distance based
        }

        scope.launch {
            animateTo(target)
            if (target == NeoSheetStatus.HIDDEN) {
                onDismissCallback?.invoke()
            }
        }
    }

    // 드래그 취소 시 현재 상태로 복귀
    internal fun handleDragCancel() {
        scope.launch { animateTo(status) }
    }

    // 드래그 중 offset 업데이트
    internal fun handleDrag(dragAmount: Float) {
        scope.launch {
            val resistanceFactor = if (offsetY.value < 0) 0.3f else 1f
            val newOffset = offsetY.value + dragAmount * resistanceFactor
            offsetY.snapTo(newOffset)
        }
    }
}

/**
 * InternalNeoBottomSheet
 * (View)
 * 역할: 실제 UI 컴포넌트입니다. 드래그 애니메이션, 레이아웃, 모양, Scrim(배경 어두워짐) 등 **"어떻게 보여질지"**를 담당합니다.
 * 필요성: 이 파일이 없으면 바텀시트 화면 자체가 존재하지 않다.
 *
 * Controller에서 관리하는 속성을 기반으로 UI만 렌더링합니다.
 */
@Composable
fun InternalNeoBottomSheet(
    controller: NeoBottomSheetController,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 하단 네비게이션 바 높이를 포함한 전체 높이값입니다.
    val fullSheetHeight = controller.height + navBottomPadding
    val sheetHeightPx = with(density) { fullSheetHeight.toPx() }

    // NeoBottomSheet가 포함된 자식뷰가 구성될 때, 자식뷰에서 정의한 height는 controller에 의해 이곳으로 주입되며 이에 맞춰 Px로 변환됩니다.
    // Composition 시점에 동기적으로 높이 업데이트 (LaunchedEffect 제거로 경쟁 조건 방지)
    controller.updateSheetHeight(sheetHeightPx)

    // offsetY 변경 시 슬라이드 콜백 호출
    controller.dispatchSlide()

    if (controller.status != NeoSheetStatus.HIDDEN) {
        // BackHandler inside the sheet composition ensures it's always registered
        // after NavHost's BackHandler, giving it higher priority
        BackHandler(enabled = controller.status != NeoSheetStatus.HIDDEN) {
            controller.onTapSheetBackNavBtn()
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (controller.status != NeoSheetStatus.HIDDEN || controller.offsetY.value < controller.sheetHeightPx) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Req 7: Scrim alpha mpped to slide progress
                            alpha = (controller.progress * 0.4f).coerceIn(0f, 0.4f)
                        }
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (controller.onTapScrimDismissEnabled) {
                                        controller.onSheetDismiss()
                                    }
                                }
                            )
                        }
                )
            }

            // Sheet Content
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .offset { IntOffset(0, controller.offsetY.value.roundToInt()) }
                    .height(fullSheetHeight)
                    .padding(bottom = navBottomPadding)
                    .background(
                        color = Gray10,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .drawBehind {
                        drawRect(
                            color = Gray10,
                            topLeft = Offset(0f, size.height - 1f), // Overlap 1px to avoid gap
                            size = Size(size.width, density.density * 1000f) // Extend 1000dp down
                        )
                    }
                    // Apply Drag Gesture to the whole sheet container
                    .pointerInput(controller.isDragDismissEnabled) {
                        if (!controller.isDragDismissEnabled) return@pointerInput

                        val velocityTracker = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = {
                                velocityTracker.resetTracking()
                            },
                            onDragEnd = {
                                // Req 2.2: Decision
                                val velocityY = velocityTracker.calculateVelocity().y
                                controller.handleDragEnd(velocityY)
                            },
                            onDragCancel = {
                                controller.handleDragCancel()
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            controller.handleDrag(dragAmount)
                        }
                    }
            ) {
                content()
            }
        }
    }
}