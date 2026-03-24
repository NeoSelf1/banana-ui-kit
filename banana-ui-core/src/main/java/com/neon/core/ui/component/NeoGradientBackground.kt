package com.neon.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.neon.core.ui.R
import kotlin.math.sqrt

/**
 * 웨이브 레이어 데이터.
 *
 * @param id 고유 식별자 (자동 생성).
 * @param color 웨이브의 색상.
 */
data class NeoWaveLayer(val id: Int, val color: Color)

/**
 * [NeoGradientBackground]에서 사용하는 웨이브 상태 관리자.
 *
 * [emitWave]를 호출하여 새로운 웨이브를 방출할 수 있으며,
 * 완료된 웨이브는 자동으로 정리됩니다.
 */
@Stable
class NeoGradientWaveState internal constructor() {
    var layers by mutableStateOf<List<NeoWaveLayer>>(emptyList())
    private var nextId = 0

    /** 지정된 색상으로 새로운 웨이브를 방출합니다. */
    fun emitWave(color: Color) {
        layers = layers + NeoWaveLayer(nextId++, color)
    }

    internal fun removeOlderThan(layerId: Int) {
        layers = layers.filter { it.id >= layerId }
    }
}

@Composable
fun rememberNeoGradientWaveState() = remember { NeoGradientWaveState() }

/**
 * 방사형 웨이브 애니메이션이 적용되는 그라디언트 배경 컴포넌트.
 *
 * [waveState]를 통해 외부에서 웨이브 방출을 제어할 수 있으며,
 * 각 웨이브는 [waveOriginRatio] 위치에서 시작하여 방사형으로 퍼져나갑니다.
 * 웨이브 애니메이션 완료 후 이전 레이어는 자동으로 제거됩니다.
 *
 * @param modifier 배경에 적용할 Modifier.
 * @param waveState 웨이브 상태 관리자. [rememberNeoGradientWaveState]로 생성합니다.
 * @param waveOriginRatio 웨이브 시작 위치 비율. (0.5f, 0.5f)이면 중앙에서 시작합니다.
 * @param waveDurationMs 웨이브 애니메이션 지속 시간 (밀리초).
 * @param waveAlpha 웨이브 최대 불투명도.
 * @param background 웨이브 뒤에 표시할 배경 컨텐츠. 기본값은 내장 gradient_base 이미지.
 *   null을 전달하면 배경 없이 웨이브만 표시됩니다.
 * @param content 전경 컨텐츠.
 */
@Composable
fun NeoGradientBackground(
    modifier: Modifier = Modifier,
    waveState: NeoGradientWaveState,
    waveOriginRatio: Offset = Offset(0.5f, 0.5f),
    waveDurationMs: Int = 1200,
    waveAlpha: Float = 0.30f,
    background: @Composable (() -> Unit)? = { DefaultGradientBaseImage() },
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        for (layer in waveState.layers) {
            key(layer.id) {
                val layerId = layer.id
                WaveOverlay(
                    color = layer.color,
                    originRatio = waveOriginRatio,
                    durationMs = waveDurationMs,
                    waveAlpha = waveAlpha,
                    onComplete = { waveState.removeOlderThan(layerId) }
                )
            }
        }

        if (background != null) {
            background()
        }
        content()
    }
}

/**
 * 단일 정적 색상으로 웨이브를 방출하는 간편 오버로드.
 *
 * 첫 컴포지션 시 [tintColor]로 한 번 웨이브를 방출합니다.
 * 운동 화면이 아닌 설정/대기 화면 등에서 사용합니다.
 *
 * @param modifier 배경에 적용할 Modifier.
 * @param tintColor 웨이브 색상. [Color.Transparent]이면 웨이브를 방출하지 않습니다.
 * @param background 웨이브 뒤에 표시할 배경 컨텐츠. 기본값은 내장 gradient_base 이미지.
 * @param content 전경 컨텐츠.
 */
@Composable
fun NeoGradientBackground(
    modifier: Modifier = Modifier,
    tintColor: Color = Color.Transparent,
    background: @Composable (() -> Unit)? = { DefaultGradientBaseImage() },
    content: @Composable () -> Unit
) {
    val waveState = rememberNeoGradientWaveState()

    LaunchedEffect(tintColor) {
        if (tintColor != Color.Transparent) {
            waveState.emitWave(tintColor)
        }
    }

    NeoGradientBackground(
        modifier = modifier,
        waveState = waveState,
        background = background,
        content = content
    )
}

@Composable
private fun WaveOverlay(
    color: Color,
    originRatio: Offset,
    durationMs: Int,
    waveAlpha: Float,
    onComplete: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMs, easing = EaseOut))
        onComplete()
    }

    val wp = progress.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val diagonal = sqrt(size.width * size.width + size.height * size.height)
                val radius = diagonal * 0.8f
                val center = Offset(size.width * originRatio.x, size.height * originRatio.y)

                val colorStops = buildWaveColorStops(color, wp, waveAlpha)

                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = colorStops,
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
    )
}

@Composable
private fun DefaultGradientBaseImage() {
    Image(
        painter = painterResource(id = R.drawable.gradient_base),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

private fun buildWaveColorStops(
    color: Color,
    wavePos: Float,
    waveAlpha: Float
): Array<Pair<Float, Color>> {
    if (wavePos >= 0.99f) {
        return arrayOf(0.0f to Color.Transparent, 1.0f to Color.Transparent)
    }

    val front = wavePos.coerceIn(0.01f, 0.98f)
    val innerEdge = (front - 0.3f).coerceAtLeast(0.005f)
    val outerEdge = (front + 0.05f).coerceAtMost(1.0f)
    return arrayOf(
        innerEdge to Color.Transparent,
        front to color.copy(alpha = waveAlpha),
        outerEdge to Color.Transparent
    )
}
