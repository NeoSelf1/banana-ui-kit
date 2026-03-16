package com.neon.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import com.neon.sample.HMButtonListScrollPerformanceTest.Companion.REPEAT_COUNT
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HMButtonListScrollPerformanceTest {
    companion object {
        /** 탭 전환 애니메이션 완료 여유시간 */
        private const val TAB_TRANSITION_WAIT_MS = 500L

        /** 스크롤 제스처 후 콘텐츠 렌더링 정착 여유시간 */
        private const val SCROLL_SETTLE_MS = 300L

        /**
         * 스와이프 스텝 수
         *
         * 값이 클수록 제스처 속도가 느려져 fling 없이 부드러운 스크롤이 적용됩니다.
         * 20 스텝 ≈ 약 200ms 소요 (UIAutomator 내부 5ms/step 기준)
         */
        private const val SWIPE_STEPS = 5

        /** 탭 전환 + 스크롤 반복 측정 횟수 */
        private const val REPEAT_COUNT = 20
    }

    data class FrameData(
        val jankyCount: Int,
        val p99Frames: Long,
    )

    /**
     * 루틴 탭 이동 → 스크롤 → 홈 탭 이동 사이클을 [REPEAT_COUNT]회 반복 측정하며
     * 각 사이클의 Janky Frame 수와 P99 프레임 시간을 출력합니다.
     *
     * 각 iteration은 gfxinfo reset → 루틴 탭 클릭 → 스크롤 → 홈 탭 클릭 순으로 진행하며
     * 단일 측정의 GC·OS 스케줄링 노이즈를 반복 평균으로 보정합니다.
     */
    @Test
    fun tabNavigationScroll_repeatedCycles_jankyFramesAndP99() = uiAutomator {
        startActivity(MainActivity::class.java)
        login(this)

        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val dataList = mutableListOf<FrameData>()

        repeat(REPEAT_COUNT) {
            val chip = onElement { textAsString() == "루틴" }

            // 측정 직전 프레임 통계 리셋
            device.executeShellCommand("dumpsys gfxinfo $pkg reset")
            chip.click()
            Thread.sleep(TAB_TRANSITION_WAIT_MS)

            // 화면 하단 → 상단 방향 스와이프로 리스트를 아래로 스크롤
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 5 / 6,
                device.displayWidth / 2,
                device.displayHeight / 6,
                SWIPE_STEPS
            )
            Thread.sleep(SCROLL_SETTLE_MS)

            // 홈 탭으로 이동 (HMBottomTab index 0, displayText = "홈")

            // gfxinfo 수집 및 파싱
            val gfxInfo = device.executeShellCommand("dumpsys gfxinfo $pkg")
            val jankyCount = parseJankyFrameCount(gfxInfo)
            val p99Frames = parseP99FrameTimeMs(gfxInfo)
            dataList.add(FrameData(jankyCount = jankyCount, p99Frames = p99Frames))
            onElement { textAsString() == "홈" }.click()
            Thread.sleep(TAB_TRANSITION_WAIT_MS)
        }

        dataList.forEachIndexed { index, data ->
            println("[TabScrollPerformance] #${index + 1} Janky frames: ${data.jankyCount}, P99: ${data.p99Frames}ms")
        }
        println("[TabScrollPerformance] average Janky frames ($REPEAT_COUNT 회): ${dataList.map { it.jankyCount }.average()}")
        println("[TabScrollPerformance] average P99 ($REPEAT_COUNT 회): ${dataList.map { it.p99Frames }.average()}ms")
    }

    // ── Navigation ────────────────────────────────────────────────────────────────

    /**
     * 로그인 화면이 표시된 경우 자격증명을 입력하고 로그인합니다.
     * 이미 로그인된 상태라면 로그인 화면이 없으므로 즉시 반환합니다.
     *
     * 로그인 완료 후 하단 탭바의 "루틴" 텍스트 출현을 확인하여 홈 화면 로드를 보장합니다.
     */
    private fun login(scope: UiAutomatorTestScope) {
        val loginButton = scope.onElementOrNull { textAsString() == "로그인" }
        if (loginButton == null) return

        scope.onElements { isEditable }[0].setText("test")
        scope.onElements { isEditable }[1].setText("qwer123!")
        loginButton.click()
        // 홈 화면 로드 완료까지 대기 — 하단 탭바 "루틴" 텍스트 출현으로 확인
        scope.onElement { textAsString() == "루틴" }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

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
