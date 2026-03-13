package com.humanics.exampleapplication

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HMPicker UIAutomator 기반 UI 테스트
 *
 * device.swipe 절대 좌표로 피커 스크롤 동작 검증 및 gfxinfo 기반 성능 수치화
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HMPickerUITest {

    companion object {
        /** 스크롤 후 정착 대기시간 */
        private const val SCROLL_SETTLE_MS = 400L

        /** 초기 렌더링 대기시간 */
        private const val LAUNCH_SETTLE_MS = 3000L

        /** 반복 스크롤 횟수 (성능 측정용) */
        private const val SCROLL_REPEAT_COUNT = 10

        /** swipe step 수 (step당 약 5ms, 20 steps ≈ 100ms) */
        private const val SWIPE_STEPS = 20

        /** 피커 스크롤 거리 (픽셀, 아이템 2~3개 분량) */
        private const val SWIPE_DISTANCE_PX = 600
    }

    /**
     * 피커 스와이프하면 선택된 값이 변경되는지 검증
     */
    @Test
    fun picker_swipeUp_changesSelectedValue() = uiAutomator {
        startActivity(MainActivity::class.java)
        Thread.sleep(LAUNCH_SETTLE_MS)
        val cx = device.displayWidth / 2
        val cy = device.displayHeight / 2

        // 위로 스와이프 (아이템이 위로 이동 → 더 큰 값 선택)
        device.swipe(cx, cy, cx, cy - SWIPE_DISTANCE_PX, SWIPE_STEPS)
        Thread.sleep(SCROLL_SETTLE_MS)

        assertTrue(onElementOrNull { textAsString() == "1" } == null)
    }

    /**
     * 피커를 반복 스와이프하여 프레임 성능 수치화
     * gfxinfo를 통해 total/janky frames, janky rate, 99th percentile frame time 측정
     */
    @Test
    fun picker_repeatedSwipe_measurePerformance() = uiAutomator {
        startActivity(MainActivity::class.java)
        Thread.sleep(LAUNCH_SETTLE_MS)

        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        device.executeShellCommand("dumpsys gfxinfo $pkg reset")

        val cx = device.displayWidth / 2
        val cy = device.displayHeight / 2

        repeat(SCROLL_REPEAT_COUNT) { i ->
            val direction = if (i % 2 == 0) -1 else 1 // 짝수: 위로, 홀수: 아래로
            device.swipe(cx, cy, cx, cy + direction * SWIPE_DISTANCE_PX, SWIPE_STEPS)
            Thread.sleep(300L)
        }

        Thread.sleep(SCROLL_SETTLE_MS)

        val gfxInfo = device.executeShellCommand("dumpsys gfxinfo $pkg")
        val totalFrames = parseTotalFrameCount(gfxInfo)
        val jankyCount = parseJankyFrameCount(gfxInfo)
        val p99Frames = parseP99FrameTimeMs(gfxInfo)
        val jankyRate = if (totalFrames > 0) (jankyCount * 100.0 / totalFrames) else 0.0

        println("=== HMPicker Scroll Performance ===")
        println("Total frames: $totalFrames")
        println("Janky frames: $jankyCount")
        println("Janky rate: ${"%.2f".format(jankyRate)}%")
        println("99th percentile: ${p99Frames}ms")
        println("====================================")

        assertTrue(
            "Janky rate이 30%를 초과하면 안됨 (actual: ${"%.2f".format(jankyRate)}%)",
            jankyRate < 30.0
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

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
