package com.neon.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray10

/**
 * 모든 화면의 기본 컨테이너 역할을 하는 스크린 래퍼 컴포넌트.
 *
 * 상태바 패딩, 네비게이션바 패딩, 수평 패딩, IME 처리, 로딩 오버레이 등
 * 화면 레이아웃의 공통 관심사를 일괄적으로 처리합니다. 모든 화면은 이 컴포넌트를
 * 최상위 컨테이너로 사용하여 일관된 레이아웃 구조를 유지해야 합니다.
 *
 * [isLoading]이 true이면 컨텐츠 위에 [NeoLoadingIndicator]가 오버레이로 표시됩니다.
 * [onTapToHideIME]가 제공되면 imePadding이 자동 적용되고, 빈 영역 탭 시 키보드가 숨겨진다.
 *
 * @param modifier 바텀탭과 함께 Column에서 사용할 경우 Modifier.weight(1f)를 전달하여
 *        바텀탭 영역을 차지하지 않도록 합니다. modifier가 주입되지 않으면
 *        서브뷰로 간주되어 기본 navigationBarsPadding()이 적용됩니다.
 *        modifier에는 weight(1f) 외에 별다른 수정자를 주입하지 않는다.
 * @param isAlignCenter true이면 내부 Column의 자식들을 수평 중앙 정렬.
 * @param horizontalPadding 좌우 수평 패딩. 기본값은 16.dp.
 * @param isLoading true이면 컨텐츠 위에 로딩 인디케이터를 오버레이로 표시.
 * @param onTapToHideIME 키보드가 표시되는 화면에서만 사용합니다. 빈 영역 탭 시 호출되는 콜백으로,
 *        일반적으로 focusManager.clearFocus()를 전달합니다.
 * @param content 화면 본문 컨텐츠. ColumnScope 내에서 구성됩니다.
 *
 * @see NeoLoadingIndicator
 * @see NeoBackHeader
 */

@Composable
fun NeoScreen(
    modifier: Modifier = Modifier.navigationBarsPadding(),
    isAlignCenter: Boolean = false,
    horizontalPadding: Dp = 16.dp,
    isLoading: Boolean = false,
    onTapToHideIME:(() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Gray10)
            .statusBarsPadding()
            .then(if (onTapToHideIME != null) Modifier.imePadding() else Modifier)
            .then(
                if (onTapToHideIME != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { onTapToHideIME() })
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = horizontalPadding)
    ) {
        Column(horizontalAlignment = if (isAlignCenter) Alignment.CenterHorizontally else Alignment.Start) {
            content()
        }

        if (isLoading) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NeoLoadingIndicator()
            }
        }
    }
}
