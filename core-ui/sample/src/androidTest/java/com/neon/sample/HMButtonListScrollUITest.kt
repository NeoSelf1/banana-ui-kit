package com.neon.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import com.neon.sample.HMButtonListScrollUITest.Companion.REPEAT_COUNT
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HMButton 리스트 스크롤 성능 반복 측정 테스트
 *
 * ButtonComparison 데모 내 HMButton 스크롤 리스트에서
 * 반복 스크롤 사이클을 수행하고 Janky Frame / P99 프레임 시간을 수집합니다.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HMButtonListScrollUITest {
    companion object {
        private const val SCREEN_TRANSITION_WAIT_MS = 800L
        private const val SCROLL_SETTLE_MS = 300L
        private const val SWIPE_STEPS = 5
        private const val REPEAT_COUNT = 20
        private const val RESET_SETTLE_MS = 100L
    }

    data class FrameData(
        val jankyCount: Int,
        val p99Frames: Long,
    )

    /**
     * HMButton 리스트에서 반복 스크롤 사이클을 [REPEAT_COUNT]회 수행하며
     * 각 사이클의 Janky Frame 수와 P99 프레임 시간을 출력합니다.
     */
    @Test
    fun hmButtonList_repeatedScroll_jankyFramesAndP99() = uiAutomator {
        startActivity(MainActivity::class.java)
        // Navigate: Demo list → Button Comparison → HMButton scroll list
        onElement { textAsString() == DemoRoute.ButtonComparison.title }.click()
        Thread.sleep(SCREEN_TRANSITION_WAIT_MS)
        onElement { textAsString() == "HMButton" }.click()
        Thread.sleep(SCREEN_TRANSITION_WAIT_MS)

        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val dataList = mutableListOf<FrameData>()

        repeat(REPEAT_COUNT) {
            device.executeShellCommand("dumpsys gfxinfo $pkg reset")
            Thread.sleep(RESET_SETTLE_MS)

            // Scroll down
            device.swipe(
                device.displayWidth / 2, device.displayHeight * 5 / 6,
                device.displayWidth / 2, device.displayHeight / 6, SWIPE_STEPS
            )
            Thread.sleep(SCROLL_SETTLE_MS)

            // Scroll up
            device.swipe(
                device.displayWidth / 2, device.displayHeight / 6,
                device.displayWidth / 2, device.displayHeight * 5 / 6, SWIPE_STEPS
            )
            Thread.sleep(SCROLL_SETTLE_MS)

            val gfxInfo = device.executeShellCommand("dumpsys gfxinfo $pkg")
            dataList.add(FrameData(
                jankyCount = parseJankyFrameCount(gfxInfo),
                p99Frames = parseP99FrameTimeMs(gfxInfo),
            ))
        }

        dataList.forEachIndexed { index, data ->
            println("[ButtonListScroll] #${index + 1} Janky: ${data.jankyCount}, P99: ${data.p99Frames}ms")
        }
        println("[ButtonListScroll] avg Janky ($REPEAT_COUNT cycles): ${dataList.map { it.jankyCount }.average()}")
        println("[ButtonListScroll] avg P99 ($REPEAT_COUNT cycles): ${dataList.map { it.p99Frames }.average()}ms")
    }

    private fun parseJankyFrameCount(gfxInfo: String): Int =
        Regex("""Janky frames:\s+(\d+)""")
            .find(gfxInfo)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun parseP99FrameTimeMs(gfxInfo: String): Long =
        Regex("""99th percentile:\s+(\d+)ms""")
            .find(gfxInfo)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
}
