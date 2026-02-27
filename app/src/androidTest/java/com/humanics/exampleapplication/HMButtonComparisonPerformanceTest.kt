package com.humanics.exampleapplication

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HMButton (Modifier.Node 최적화) vs HMButtonLegacy (standard Compose)
 * 스크롤 렌더링 성능을 gfxinfo 메트릭으로 비교하는 테스트
 *
 * 각 탭에서 동일한 스크롤 사이클을 반복하여
 * Janky Frame 수와 P99 프레임 시간을 수집한 뒤 개선율을 출력합니다.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HMButtonComparisonPerformanceTest {

    companion object {
        /** 탭 전환 애니메이션 완료 여유시간 */
        private const val TAB_TRANSITION_WAIT_MS = 500L

        /** 스크롤 제스처 후 콘텐츠 렌더링 정착 여유시간 */
        private const val SCROLL_SETTLE_MS = 300L

        /**
         * 스와이프 스텝 수
         *
         * 값이 클수록 제스처 속도가 느려져 fling 없이 부드러운 스크롤이 적용됩니다.
         */
        private const val SWIPE_STEPS = 5

        /** 탭당 스크롤 반복 측정 횟수 */
        private const val REPEAT_COUNT = 10

        /** gfxinfo 리셋 후 안정화 대기시간 */
        private const val RESET_SETTLE_MS = 100L

        // HMButtonComparisonDemoView 탭 라벨
        private const val TAB_OPTIMIZED = "HMButton"
        private const val TAB_LEGACY = "HMButtonLegacy"
    }

    data class FrameData(
        val jankyCount: Int,
        val p99Frames: Long,
    )

    /**
     * HMButton vs HMButtonLegacy 스크롤 성능 비교
     *
     * 1. HMButton 탭에서 [REPEAT_COUNT]회 스크롤 사이클 → gfxinfo 수집
     * 2. HMButtonLegacy 탭에서 동일 [REPEAT_COUNT]회 스크롤 사이클 → gfxinfo 수집
     * 3. 평균 비교 및 개선율 출력
     */
    @Test
    fun scrollPerformance_hmButton_vs_hmButtonLegacy() = uiAutomator {
        startActivity(MainActivity::class.java)

        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // ── Phase 1: HMButton (Modifier.Node 최적화) ──
        val optimizedData = measureTabScrollPerformance(
            device = device,
            pkg = pkg,
            tabLabel = TAB_OPTIMIZED,
        )

        // ── Phase 2: HMButtonLegacy (Standard Compose) ──
        val legacyData = measureTabScrollPerformance(
            device = device,
            pkg = pkg,
            tabLabel = TAB_LEGACY,
        )

        // ── Results ──
        printResults(optimizedData, legacyData)
    }

    // ── Measurement ─────────────────────────────────────────────────────────────

    /**
     * 지정 탭으로 이동 후 스크롤 사이클을 [REPEAT_COUNT]회 반복하여
     * 사이클별 [FrameData] 리스트를 반환합니다.
     *
     * 각 사이클: gfxinfo reset → 아래 스크롤 → 위 스크롤 → gfxinfo 수집
     */
    private fun UiAutomatorTestScope.measureTabScrollPerformance(
        device: UiDevice,
        pkg: String,
        tabLabel: String,
    ): List<FrameData> {
        val dataList = mutableListOf<FrameData>()

        onElement { textAsString() == tabLabel }.click()
        Thread.sleep(TAB_TRANSITION_WAIT_MS)

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
            println("$tag  #${i + 1}  Janky: ${d.jankyCount}, P99: ${d.p99Frames}ms")
        }
        val optAvgJanky = optimizedData.map { it.jankyCount }.average()
        val optAvgP99 = optimizedData.map { it.p99Frames }.average()
        println("$tag  AVG Janky: ${"%.2f".format(optAvgJanky)}, AVG P99: ${"%.2f".format(optAvgP99)}ms")

        println("$tag ════════════════════════════════════════")
        println("$tag  HMButtonLegacy (Standard Compose) Results")
        println("$tag ════════════════════════════════════════")
        legacyData.forEachIndexed { i, d ->
            println("$tag  #${i + 1}  Janky: ${d.jankyCount}, P99: ${d.p99Frames}ms")
        }
        val legAvgJanky = legacyData.map { it.jankyCount }.average()
        val legAvgP99 = legacyData.map { it.p99Frames }.average()
        println("$tag  AVG Janky: ${"%.2f".format(legAvgJanky)}, AVG P99: ${"%.2f".format(legAvgP99)}ms")

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

    // ── Helpers ─────────────────────────────────────────────────────────────────

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
