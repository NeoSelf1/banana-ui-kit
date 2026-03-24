package com.neon.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.neon.core.ui.image.SvgImageLoader
import com.neon.core.ui.theme.Primary50

/**
 * SVG 아이콘을 로드하여 표시하는 공용 아이콘 컴포넌트.
 *
 * `assets/icons/` 디렉토리에서 [name]에 해당하는 SVG 파일을 Coil의 [SvgImageLoader]로 비동기 로드합니다.
 * 선택적으로 색상 틴트, 알림 배지, 탭 콜백을 지원합니다.
 *
 * 아이콘 파일 경로는 `file:///android_asset/icons/{name}.svg` 형식으로 자동 구성되므로,
 * [name] 파라미터에는 확장자 없이 파일명만 전달하면 됩니다. (예: "chevron_left", "home", "close_sm")
 *
 * [onTap]이 제공되면 클릭 가능한 아이콘이 되며, 리플 효과 없이(indication = null) 동작합니다.
 * [isBadgeVisible]이 true이면 아이콘 우측 상단에 [Primary50] 색상의 작은 원형 배지가 표시됩니다.
 *
 * **주의:**
 * 이 라이브러리는 아이콘 에셋을 포함하지 않습니다.
 * 소비자 앱에서 `src/main/assets/icons/` 디렉토리에 SVG 파일을 직접 배치해야 합니다.
 * 컴포넌트에서 참조하는 아이콘이 해당 경로에 존재하지 않으면 빈 영역으로 렌더링됩니다.
 *
 * @param modifier 아이콘에 적용할 Modifier.
 * @param name SVG 아이콘 파일명 (확장자 제외). `assets/icons/` 디렉토리 기준.
 * @param sizeDp 아이콘의 너비와 높이. 기본값은 36.dp.
 * @param contentDescription 접근성을 위한 콘텐츠 설명.
 * @param color 아이콘에 적용할 틴트 색상. null이면 원본 색상 유지.
 * @param isBadgeVisible true이면 우측 상단에 알림 배지를 표시.
 * @param onTap 아이콘 탭 시 호출되는 선택적 콜백. null이면 클릭 불가.
 */
@Composable
fun NeoIcon(
    modifier: Modifier = Modifier,
    name: String,
    sizeDp: Dp = 36.dp,
    contentDescription: String? = null,
    color: Color? = null,
    isBadgeVisible: Boolean = false,
    onTap: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val model = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/$name.svg")
        .build()

    val finalColorFilter = when {
        color != null -> ColorFilter.tint(color)
        else -> null
    }

    AsyncImage(
        model = model,
        modifier = modifier.size(sizeDp)
            .then(
                if (isBadgeVisible)
                    Modifier.drawBehind {
                        drawCircle(
                            color = Primary50,
                            radius = 4.dp.toPx(),
                            center = Offset(
                                sizeDp.toPx() - 10.dp.toPx(),
                                sizeDp.toPx() / 2f
                            )
                        )
                    }
                else
                    Modifier
            )
            .then(
                if (onTap != null) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTap() }
                } else {
                    Modifier
                }),
        contentDescription = contentDescription,
        imageLoader = SvgImageLoader.get(context),
        contentScale = ContentScale.Fit,
        colorFilter = finalColorFilter
    )
}


@Preview
@Composable
private fun NeoIconPreview() {
    NeoIcon(
        name = "hello",
        isBadgeVisible = true
    )
}