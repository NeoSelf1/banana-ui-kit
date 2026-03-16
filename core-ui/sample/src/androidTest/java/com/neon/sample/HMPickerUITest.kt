package com.neon.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HMPickerUITest {
    companion object {
        private const val SCROLL_SETTLE_MS = 400L
        private const val LAUNCH_SETTLE_MS = 3000L
        private const val SCROLL_REPEAT_COUNT = 10
        private const val SWIPE_STEPS = 20
        private const val SWIPE_DISTANCE_PX = 600
        private const val NAV_SETTLE_MS = 500L
    }

    @Test
    fun picker_swipeUp_changesSelectedValue() = uiAutomator {
        startActivity(MainActivity::class.java)
        Thread.sleep(LAUNCH_SETTLE_MS)
        // Navigate: Demo list → Picker
        onElement { textAsString() == DemoRoute.Picker.title }.click()
        Thread.sleep(NAV_SETTLE_MS)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val cx = device.displayWidth / 2
        val cy = device.displayHeight / 2

        device.swipe(cx, cy, cx, cy - SWIPE_DISTANCE_PX, SWIPE_STEPS)
        Thread.sleep(SCROLL_SETTLE_MS)

        assertTrue(onElementOrNull { textAsString() == "1" } == null)
    }

    @Test
    fun picker_repeatedSwipe_measurePerformance() = uiAutomator {
        startActivity(MainActivity::class.java)
        Thread.sleep(LAUNCH_SETTLE_MS)
        // Navigate: Demo list → Picker
        onElement { textAsString() == DemoRoute.Picker.title }.click()
        Thread.sleep(NAV_SETTLE_MS)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        device.executeShellCommand("dumpsys gfxinfo $pkg reset")

        val cx = device.displayWidth / 2
        val cy = device.displayHeight / 2

        repeat(SCROLL_REPEAT_COUNT) { i ->
            val direction = if (i % 2 == 0) -1 else 1
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

    private fun parseTotalFrameCount(gfxInfo: String): Int =
        Regex("""Total frames rendered:\s+(\d+)""")
            .find(gfxInfo)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun parseJankyFrameCount(gfxInfo: String): Int =
        Regex("""Janky frames:\s+(\d+)""")
            .find(gfxInfo)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun parseP99FrameTimeMs(gfxInfo: String): Long =
        Regex("""99th percentile:\s+(\d+)ms""")
            .find(gfxInfo)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
}
