package com.neon.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import com.neon.sample.HMButtonComparisonPerformanceTest.Companion.LONG_CLICK_REPEAT_COUNT
import com.neon.sample.HMButtonComparisonPerformanceTest.Companion.REPEAT_COUNT
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HMButton (Modifier.Node 최적화) vs HMButtonLegacy (standard Compose)
 * 스크롤 렌더링 성능을 gfxinfo 메트릭으로 비교하는 테스트
 *
 * 각 하위 화면에서 동일한 스크롤 사이클을 반복하고, systemBack으로
 * Compose 트리를 완전히 해제한 뒤 다음 화면에 진입하여
 * Janky Frame 수와 P99 프레임 시간을 수집한 뒤 개선율을 출력합니다.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HMButtonComparisonPerformanceTest {

    companion object {
        /** 화면 전환 후 Compose 렌더링 완료 여유시간 */
        private const val SCREEN_TRANSITION_WAIT_MS = 800L

        /** 스크롤 제스처 후 콘텐츠 렌더링 정착 여유시간 */
        private const val SCROLL_SETTLE_MS = 300L

        /**
         * 스와이프 스텝 수
         *
         * 값이 클수록 제스처 속도가 느려져 fling 없이 부드러운 스크롤이 적용됩니다.
         */
        private const val SWIPE_STEPS = 5

        /** 화면당 스크롤 반복 측정 횟수 */
        private const val REPEAT_COUNT = 10

        /** gfxinfo 리셋 후 안정화 대기시간 */
        private const val RESET_SETTLE_MS = 100L

        /** longClick 유지 시간 (press 애니메이션 100ms + 유지) */
        private const val LONG_CLICK_DURATION_MS = 600L

        /** release 애니메이션 완료 대기 (HMButton RELEASE_DURATION = 400ms) */
        private const val RELEASE_ANIMATION_WAIT_MS = 500L

        /** 단일 버튼 longClick 반복 측정 횟수 */
        private const val LONG_CLICK_REPEAT_COUNT = 20

        // HMButtonComparisonDemoView 메뉴 버튼 라벨
        private const val BUTTON_OPTIMIZED = "HMButton"
        private const val BUTTON_LEGACY = "HMButtonLegacy"
        private const val BUTTON_SINGLE_LEGACY = "Single Legacy"
        private const val BUTTON_SINGLE_NODE = "Single Node"
    }

    data class FrameData(
        val totalFrames: Int,
        val jankyCount: Int,
        val p99Frames: Long,
    )

    /**
     * HMButton vs HMButtonLegacy 스크롤 성능 비교
     *
     * 각 버튼의 하위 화면에 진입하여 [REPEAT_COUNT]회 스크롤 사이클 후 gfxinfo 수집,
     * systemBack으로 메뉴 복귀하여 Compose 트리를 완전히 해제한 뒤
     * 다음 화면에 진입합니다. 이를 통해 캐시 영향을 제거합니다.
     */
    @Test
    fun scrollPerformance_hmButton_vs_hmButtonLegacy() = uiAutomator {
        startActivity(MainActivity::class.java)

        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // ── Phase 1: HMButtonLegacy (Standard Compose) ──
        val legacyData = measureScreenScrollPerformance(
            device = device,
            pkg = pkg,
            buttonLabel = BUTTON_LEGACY,
        )

        // systemBack으로 메뉴 복귀 → 이전 Compose 트리 완전 해제
        device.pressBack()
        Thread.sleep(SCREEN_TRANSITION_WAIT_MS)

        // ── Phase 2: HMButton (Modifier.Node 최적화) ──
        val optimizedData = measureScreenScrollPerformance(
            device = device,
            pkg = pkg,
            buttonLabel = BUTTON_OPTIMIZED,
        )

        // ── Results ──
        printResults(optimizedData, legacyData)
    }

    /**
     * 단일 버튼 longClick 시 Transition 애니메이션 프레임 성능 비교
     *
     * 각 버전의 단일 버튼 하위 화면에 진입하여 longClick(press → hold → release)
     * 사이클을 [LONG_CLICK_REPEAT_COUNT]회 반복하고, 사이클별 총 프레임 수·Janky 프레임 수·P99를 수집합니다.
     * systemBack으로 Compose 트리를 완전히 해제한 뒤 다음 화면에 진입합니다.
     */
    @Test
    fun longClickTransition_hmButton_vs_hmButtonLegacy() = uiAutomator {
        startActivity(MainActivity::class.java)

        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // ── Phase 1: HMButtonLegacy (Standard Compose) ──
        val legacyData = measureSingleButtonLongClickPerformance(
            device = device,
            pkg = pkg,
            menuButtonLabel = BUTTON_SINGLE_LEGACY,
        )

        // systemBack으로 메뉴 복귀 → Compose 트리 완전 해제
        device.pressBack()
        Thread.sleep(SCREEN_TRANSITION_WAIT_MS)

        // ── Phase 2: HMButton (Modifier.Node 최적화) ──
        val optimizedData = measureSingleButtonLongClickPerformance(
            device = device,
            pkg = pkg,
            menuButtonLabel = BUTTON_SINGLE_NODE,
        )

        // ── Results ──
        printLongClickResults(optimizedData, legacyData)
    }

    // ── Measurement ─────────────────────────────────────────────────────────────

    /**
     * 메뉴에서 지정 버튼을 클릭하여 하위 화면에 진입한 후
     * 스크롤 사이클을 [REPEAT_COUNT]회 반복하여 사이클별 [FrameData] 리스트를 반환합니다.
     *
     * 각 사이클: gfxinfo reset → 아래 스크롤 → 위 스크롤 → gfxinfo 수집
     * 측정 완료 후 하위 화면에 머문 상태로 반환합니다.
     */
    private fun UiAutomatorTestScope.measureScreenScrollPerformance(
        device: UiDevice,
        pkg: String,
        buttonLabel: String,
    ): List<FrameData> {
        val dataList = mutableListOf<FrameData>()

        // 메뉴에서 하위 화면 진입
        onElement { textAsString() == buttonLabel }.click()
        Thread.sleep(SCREEN_TRANSITION_WAIT_MS)

        repeat(REPEAT_COUNT) {
            // 측정 직전 프레임 통계 리셋
            device.executeShellCommand("dumpsys gfxinfo $pkg reset")
            Thread.sleep(RESET_SETTLE_MS)

            // 아래로 스크롤 (하단 → 상단 방향 스와이프)
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 5 / 6,
                device.displayWidth / 2,
                device.displayHeight / 6,
                SWIPE_STEPS
            )
            Thread.sleep(SCROLL_SETTLE_MS)

            // 위로 스크롤 (상단 → 하단 방향 스와이프)
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight / 6,
                device.displayWidth / 2,
                device.displayHeight * 5 / 6,
                SWIPE_STEPS
            )
            Thread.sleep(SCROLL_SETTLE_MS)

            // gfxinfo 수집 및 파싱
            val gfxInfo = device.executeShellCommand("dumpsys gfxinfo $pkg")
            dataList.add(
                FrameData(
                    totalFrames = parseTotalFrameCount(gfxInfo),
                    jankyCount = parseJankyFrameCount(gfxInfo),
                    p99Frames = parseP99FrameTimeMs(gfxInfo),
                )
            )
        }

        return dataList
    }

    /**
     * 메뉴에서 단일 버튼 하위 화면에 진입한 후
     * longClick(press → hold → release) 사이클을 [LONG_CLICK_REPEAT_COUNT]회 반복하여
     * 사이클별 [FrameData] 리스트를 반환합니다.
     *
     * 각 사이클: gfxinfo reset → 화면 중앙 longClick → release 애니메이션 대기 → gfxinfo 수집
     */
    private fun UiAutomatorTestScope.measureSingleButtonLongClickPerformance(
        device: UiDevice,
        pkg: String,
        menuButtonLabel: String,
    ): List<FrameData> {
        val dataList = mutableListOf<FrameData>()

        // 메뉴에서 하위 화면 진입
        onElement { textAsString() == menuButtonLabel }.click()
        Thread.sleep(SCREEN_TRANSITION_WAIT_MS)

        val cx = device.displayWidth / 2
        val cy = device.displayHeight / 2

        // longClick 스텝: LONG_CLICK_DURATION_MS / 5ms(UIAutomator step interval)
        val longClickSteps = (LONG_CLICK_DURATION_MS / 5).toInt()

        repeat(LONG_CLICK_REPEAT_COUNT) {
            // 측정 직전 프레임 통계 리셋
            device.executeShellCommand("dumpsys gfxinfo $pkg reset")
            Thread.sleep(RESET_SETTLE_MS)

            // longClick: 같은 좌표로 swipe하여 press 유지 후 release
            device.swipe(cx, cy, cx, cy, longClickSteps)

            // release 애니메이션 완료 대기
            Thread.sleep(RELEASE_ANIMATION_WAIT_MS)

            // gfxinfo 수집 및 파싱
            val gfxInfo = device.executeShellCommand("dumpsys gfxinfo $pkg")
            dataList.add(
                FrameData(
                    totalFrames = parseTotalFrameCount(gfxInfo),
                    jankyCount = parseJankyFrameCount(gfxInfo),
                    p99Frames = parseP99FrameTimeMs(gfxInfo),
                )
            )
        }

        return dataList
    }

    // ── Output ──────────────────────────────────────────────────────────────────

    private fun printResults(
        optimizedData: List<FrameData>,
        legacyData: List<FrameData>,
    ) {
        val tag = "[ButtonPerfComparison]"

        println("$tag ════════════════════════════════════════")
        println("$tag  HMButton (Modifier.Node) Results")
        println("$tag ════════════════════════════════════════")
        optimizedData.forEachIndexed { i, d ->
            println("$tag  #${i + 1}  Total: ${d.totalFrames}, Janky: ${d.jankyCount}, P99: ${d.p99Frames}ms")
        }
        val optAvgTotal = optimizedData.map { it.totalFrames }.average()
        val optAvgJanky = optimizedData.map { it.jankyCount }.average()
        val optAvgP99 = optimizedData.map { it.p99Frames }.average()
        println("$tag  AVG Total: ${"%.2f".format(optAvgTotal)}, AVG Janky: ${"%.2f".format(optAvgJanky)}, AVG P99: ${"%.2f".format(optAvgP99)}ms")

        println("$tag ════════════════════════════════════════")
        println("$tag  HMButtonLegacy (Standard Compose) Results")
        println("$tag ════════════════════════════════════════")
        legacyData.forEachIndexed { i, d ->
            println("$tag  #${i + 1}  Total: ${d.totalFrames}, Janky: ${d.jankyCount}, P99: ${d.p99Frames}ms")
        }
        val legAvgTotal = legacyData.map { it.totalFrames }.average()
        val legAvgJanky = legacyData.map { it.jankyCount }.average()
        val legAvgP99 = legacyData.map { it.p99Frames }.average()
        println("$tag  AVG Total: ${"%.2f".format(legAvgTotal)}, AVG Janky: ${"%.2f".format(legAvgJanky)}, AVG P99: ${"%.2f".format(legAvgP99)}ms")

        println("$tag ════════════════════════════════════════")
        println("$tag  COMPARISON")
        println("$tag ════════════════════════════════════════")

        if (legAvgJanky > 0) {
            val jankyImprovement = ((legAvgJanky - optAvgJanky) / legAvgJanky) * 100
            println("$tag  Janky frames improvement: ${"%.1f".format(jankyImprovement)}%")
        } else {
            println("$tag  Janky frames: both 0 (no jank detected)")
        }

        if (legAvgP99 > 0) {
            val p99Improvement = ((legAvgP99 - optAvgP99) / legAvgP99) * 100
            println("$tag  P99 frame time improvement: ${"%.1f".format(p99Improvement)}%")
        } else {
            println("$tag  P99 frame time: both 0ms")
        }

        println("$tag ════════════════════════════════════════")
    }

    private fun printLongClickResults(
        optimizedData: List<FrameData>,
        legacyData: List<FrameData>,
    ) {
        val tag = "[LongClickPerfComparison]"

        println("$tag ════════════════════════════════════════")
        println("$tag  HMButton (Modifier.Node) — LongClick Transition")
        println("$tag ════════════════════════════════════════")
        optimizedData.forEachIndexed { i, d ->
            println("$tag  #${i + 1}  Total: ${d.totalFrames}, Janky: ${d.jankyCount}, P99: ${d.p99Frames}ms")
        }
        val optAvgTotal = optimizedData.map { it.totalFrames }.average()
        val optAvgJanky = optimizedData.map { it.jankyCount }.average()
        val optAvgP99 = optimizedData.map { it.p99Frames }.average()
        println("$tag  AVG Total: ${"%.2f".format(optAvgTotal)}, AVG Janky: ${"%.2f".format(optAvgJanky)}, AVG P99: ${"%.2f".format(optAvgP99)}ms")

        println("$tag ════════════════════════════════════════")
        println("$tag  HMButtonLegacy (Standard Compose) — LongClick Transition")
        println("$tag ════════════════════════════════════════")
        legacyData.forEachIndexed { i, d ->
            println("$tag  #${i + 1}  Total: ${d.totalFrames}, Janky: ${d.jankyCount}, P99: ${d.p99Frames}ms")
        }
        val legAvgTotal = legacyData.map { it.totalFrames }.average()
        val legAvgJanky = legacyData.map { it.jankyCount }.average()
        val legAvgP99 = legacyData.map { it.p99Frames }.average()
        println("$tag  AVG Total: ${"%.2f".format(legAvgTotal)}, AVG Janky: ${"%.2f".format(legAvgJanky)}, AVG P99: ${"%.2f".format(legAvgP99)}ms")

        println("$tag ════════════════════════════════════════")
        println("$tag  COMPARISON")
        println("$tag ════════════════════════════════════════")

        // 총 프레임 수 비교 (Node 최적화는 리컴포지션이 적어 프레임 수가 낮을 수 있음)
        println("$tag  Total frames — Node: ${"%.2f".format(optAvgTotal)}, Legacy: ${"%.2f".format(legAvgTotal)}")

        if (legAvgJanky > 0) {
            val jankyImprovement = ((legAvgJanky - optAvgJanky) / legAvgJanky) * 100
            println("$tag  Janky frames improvement: ${"%.1f".format(jankyImprovement)}%")
        } else if (optAvgJanky > 0) {
            println("$tag  Janky frames — Node: ${"%.2f".format(optAvgJanky)}, Legacy: 0")
        } else {
            println("$tag  Janky frames: both 0 (no jank detected)")
        }

        if (legAvgP99 > 0) {
            val p99Improvement = ((legAvgP99 - optAvgP99) / legAvgP99) * 100
            println("$tag  P99 frame time improvement: ${"%.1f".format(p99Improvement)}%")
        } else {
            println("$tag  P99 frame time: both 0ms")
        }

        println("$tag ════════════════════════════════════════")
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun parseTotalFrameCount(gfxInfo: String): Int =
        Regex("""Total frames rendered:\s+(\d+)""")
            .find(gfxInfo)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull() ?: 0

    private fun parseJankyFrameCount(gfxInfo: String): Int =
        Regex("""Janky frames:\s+(\d+)""")
            .find(gfxInfo)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull() ?: 0

    private fun parseP99FrameTimeMs(gfxInfo: String): Long =
        Regex("""99th percentile:\s+(\d+)ms""")
            .find(gfxInfo)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull() ?: 0L
}
