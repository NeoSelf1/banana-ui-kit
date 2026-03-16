package com.neon.core.ui.component.bottomSheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
/**
 * 선언형(Declarative) 글로벌 바텀시트 구조를 완성하기 위해서는 3가지 요소가 유기적으로 필요합니다. 각 역할은 다음과 같습니다:
 *
 * [InternalNeoBottomSheet]
 * (View)
 * 역할: 실제 UI 컴포넌트입니다. 드래그 애니메이션, 레이아웃, 모양, Scrim(배경 어두워짐) 등 "어떻게 보여질지"를 담당합니다.
 *
 * [NeoBottomSheetController]
 * (State Holder / Controller)
 * 역할: 앱 전역에서 바텀시트의 상태(열림/닫힘, 내용물, 타입, 애니메이션 등)를 관리하는 컨트롤러입니다.
 *
 * [NeoCustomBottomSheetProvider]
 * (Root Injector)
 * 역할: 앱의 최상위(Root)에 위치하여 Controller를 생성하고, CompositionLocal을 통해 하위 자식들에게 컨트롤러를 내려줍니다. 또한, "화면의 맨 위(z-index)"에 실제 InternalNeoBottomSheet뷰를 배치하여 어디서든 바텀시트가 네비게이션 바 위로 올라오도록 합니다.
 *
 * [NeoBottomSheet]
 * (Public API)
 * 역할: 개발자가 각 화면에서 iOS의 .sheet 처럼 간편하게 바텀시트를 호출할 수 있게 해주는 선언형 API입니다. NeoBottomSheetController의 show/hide 함수를 직접 부르는 복잡함을 숨기고, isPresent 변수 하나로 제어할 수 있게 해줍니다.
 */
val LocalNeoBottomSheetController = compositionLocalOf<NeoBottomSheetController> {
    error("No NeoBottomSheetController provided")
}


@Composable
fun NeoCustomBottomSheetProvider(
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val controller = remember(scope) { NeoBottomSheetController(scope) }

    /** [CompositionLocalProvider]: 하위 컴포저블 트리에 암시적으로 값을 전달하는 메커니즘입니다.
     * CompositionLocal(LocalNeoBottomSheetController)을 먼저 정의하고, Provider로 값을 제공하면 Provider로 감싸진 내부 컴포저블들에서 접근이 가능해집니다.
     * LocalDensity.current 등이 동일 매커니즘으로 접근가능한 속성입니다.
     *
     * 이로써, 하위에 선언된 NeoBottomSheet API들에서 LocalNeoBottomSheetController.current을 통해 controller를 간편하게 접근하여 제어할 수 있게 됩니다.
     * 이는 iOS의 @EnvironmentObject와 유사한 역할을 수행합니다..
     */
    CompositionLocalProvider(LocalNeoBottomSheetController provides controller) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            InternalNeoBottomSheet(
                controller = controller
            ) {
                controller.content()
            }
        }
    }
}

/**
 * 선언형 바텀시트 제어 컴포저블
 * iOS의 .sheet(isPresented: $isPresent) { ... } 와 유사하게 동작합니다.
 * PLATFORM: onDismiss 콜백을 삽입할 수 있는 체이닝 방식이 없기에, NeoBottomSheet가 맡고 있는 책임이 iOS에 비해 더 넓습니다.
 *
 * @param isPresent 바텀시트 표시 여부 상태: viewModel에서 선언형으로 바텀시트를 제어하기 위한 상태변수입니다.
 * @param onDismiss 바텀시트가 닫혀야 할 때 호출되는 콜백: 아래로 Drag 및 Scrim 영역 탭 상호작용과 연결됩니다. (ex: isPresent = false 처리)
 * @param onTapBackNavBtn 네비게이션 바의 뒤로가기 동작 수행 시 호출되는 콜백: (ex: selectedOption = null 처리 or isPresent = false 처리)
 *
 * - 바텀시트 내부 뎁스가 없을 시, 네비게이션 뒤로가기 상호작용이 바텀시트 닫기로 바로 연결될 수 있도록 onTapBackNavBtn을 주입하지 마세요.
 */
@Composable
fun NeoBottomSheet(
    isPresent: Boolean,
    height: Dp = 280.dp,
    onDismiss: () -> Unit,
    onTapBackNavBtn: (() -> Unit)? = null,
    onTapScrimDismissEnabled: Boolean = true,
    isDragDismissEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val controller = LocalNeoBottomSheetController.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnTapBackNavBtn by rememberUpdatedState(onTapBackNavBtn)
    val currentContent by rememberUpdatedState(content)

    // Composition 시점에 동기적으로 상태 동기화 (LaunchedEffect 제거로 경쟁 조건 방지)
    LaunchedEffect(isPresent, height) {
        if (isPresent) {
            controller.show(
                height = height,
                onDismiss = { currentOnDismiss() },
                onTapBackNavBtn = currentOnTapBackNavBtn ?: onDismiss,
                isOutTapDismissEnabled = onTapScrimDismissEnabled,
                isDragDismissEnabled = isDragDismissEnabled,
                content = { currentContent() }
            )
        } else {
            controller.hide()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isPresent) {
                controller.hide()
                currentOnDismiss()
            }
        }
    }
}