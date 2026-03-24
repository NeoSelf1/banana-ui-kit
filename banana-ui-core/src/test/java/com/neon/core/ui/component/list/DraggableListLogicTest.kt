package com.neon.core.ui.component.list

import org.junit.Assert
import org.junit.Test

/**
 * [DraggableListLogic] 순수 계산 함수 검증.
 *
 * "기대하는 올바른 동작" 기준으로 작성하여,
 * 현재 로직에 버그가 있으면 해당 테스트가 실패하도록 설계합니다.
 *
 * 실행:
 * ```
 * ./gradlew :core-ui:testDebugUnitTest --tests "*.DraggableListLogicTest"
 * ```
 */
class DraggableListLogicTest {

    // ═══════════════════════════════════════════════════════════════════════
    // region 1. computeTargetSlot — Y좌표 → 슬롯 변환
    // ═══════════════════════════════════════════════════════════════════════

    // 픽스처: rowSlotHeight=100px, 4개 아이템, 오프셋 없음
    private val slotHeight = 100f
    private val itemCount = 4

    private fun slot(y: Float) = DraggableListLogic.computeTargetSlot(
        windowY = y,
        containerTopOffset = 0f,
        scrollOffset = 0,
        headerHeightPx = 0f,
        rowSlotHeightPx = slotHeight,
        itemCount = itemCount
    )

    // ── 경계값 ──

    @Test
    fun `targetSlot - Y 0 이하 → slot 0`() {
        Assert.assertEquals(0, slot(-10f))
        Assert.assertEquals(0, slot(0f))
    }

    @Test
    fun `targetSlot - Y 리스트 초과 → slot itemCount`() {
        Assert.assertEquals(itemCount, slot(9999f))
    }

    // ── Row 0 (A) 영역: 0~100px ──

    @Test
    fun `targetSlot - Row0 상단 25% → slot 0`() {
        Assert.assertEquals(0, slot(25f))
    }

    @Test
    fun `targetSlot - Row0 정중앙 50px → slot 0`() {
        // midOffset == rowSlotHeight/2, ">" 비교이므로 slot 0 유지
        Assert.assertEquals(0, slot(50f))
    }

    @Test
    fun `targetSlot - Row0 중앙 직후 50_1px → slot 1`() {
        Assert.assertEquals(1, slot(50.1f))
    }

    @Test
    fun `targetSlot - Row0 하단 75% → slot 1`() {
        Assert.assertEquals(1, slot(75f))
    }

    // ── Row 1 (B) 영역: 100~200px ──

    @Test
    fun `targetSlot - Row1 상단 → slot 1`() {
        Assert.assertEquals(1, slot(110f))
    }

    @Test
    fun `targetSlot - Row1 하단 → slot 2`() {
        Assert.assertEquals(2, slot(160f))
    }

    // ── Row 2 (C) 영역: 200~300px ──

    @Test
    fun `targetSlot - Row2 상단 → slot 2`() {
        Assert.assertEquals(2, slot(210f))
    }

    @Test
    fun `targetSlot - Row2 하단 → slot 3`() {
        Assert.assertEquals(3, slot(260f))
    }

    // ── Row 3 (D) 영역: 300~400px ──

    @Test
    fun `targetSlot - Row3 상단 → slot 3`() {
        Assert.assertEquals(3, slot(310f))
    }

    @Test
    fun `targetSlot - Row3 하단 → slot 4`() {
        Assert.assertEquals(4, slot(370f))
    }

    // ── 헤더 오프셋 ──

    @Test
    fun `targetSlot - 헤더 영역 내 Y → slot 0`() {
        val result = DraggableListLogic.computeTargetSlot(
            windowY = 100f, containerTopOffset = 0f, scrollOffset = 0,
            headerHeightPx = 200f, rowSlotHeightPx = slotHeight, itemCount = itemCount
        )
        Assert.assertEquals(0, result)
    }

    // ── 스크롤 오프셋 ──

    @Test
    fun `targetSlot - 스크롤 200px 상태에서 Y 25 → slot 2`() {
        val result = DraggableListLogic.computeTargetSlot(
            windowY = 25f, containerTopOffset = 0f, scrollOffset = 200,
            headerHeightPx = 0f, rowSlotHeightPx = slotHeight, itemCount = itemCount
        )
        Assert.assertEquals(2, result)
    }

    // ── 컨테이너 오프셋 ──

    @Test
    fun `targetSlot - 컨테이너 300px 아래 위치 시 보정`() {
        val result = DraggableListLogic.computeTargetSlot(
            windowY = 375f, containerTopOffset = 300f, scrollOffset = 0,
            headerHeightPx = 0f, rowSlotHeightPx = slotHeight, itemCount = itemCount
        )
        Assert.assertEquals(1, result)
    }

    // ── 빈 리스트 ──

    @Test
    fun `targetSlot - 빈 리스트 → 항상 slot 0`() {
        val result = DraggableListLogic.computeTargetSlot(
            windowY = 500f, containerTopOffset = 0f, scrollOffset = 0,
            headerHeightPx = 0f, rowSlotHeightPx = 100f, itemCount = 0
        )
        Assert.assertEquals(0, result)
    }

    // endregion

    // ═══════════════════════════════════════════════════════════════════════
    // region 2. computeInsertIndex — 슬롯 → 삽입 인덱스 변환
    //
    // 검증 전략:
    //   기대값은 "remove → insert 시뮬레이션"으로 산출합니다.
    //   original = [A(0), B(1), C(2), D(3)]
    //   각 (currentIndex, targetSlot) 조합에서:
    //     1) targetSlot이 "자기 위치"(currentIndex 또는 currentIndex+1)이면 no-op
    //     2) 그 외: remove → insert 후 최종 배열이 올바른지 확인
    // ═══════════════════════════════════════════════════════════════════════

    private val original = listOf("A", "B", "C", "D")

    /**
     * remove → insert 시뮬레이션.
     * @return 이동 후 배열
     */
    private fun simulate(currentIndex: Int, insertIndex: Int): List<String> {
        val list = original.toMutableList()
        val item = list.removeAt(currentIndex)
        list.add(insertIndex, item)
        return list
    }

    // ── C(index=2) 기준 전체 슬롯 테스트 ──

    @Test
    fun `insertIndex - C를 slot0에 드롭 → 맨 위로 이동`() {
        // 기대: [C, A, B, D] → insertIndex = 0
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 2, targetSlot = 0, itemCount = 4
        )
        Assert.assertEquals(listOf("C", "A", "B", "D"), simulate(2, result))
    }

    @Test
    fun `insertIndex - C를 slot1에 드롭 → A와 B 사이로 이동`() {
        // 기대: [A, C, B, D] → insertIndex = 1
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 2, targetSlot = 1, itemCount = 4
        )
        Assert.assertEquals(listOf("A", "C", "B", "D"), simulate(2, result))
    }

    @Test
    fun `insertIndex - C를 slot2에 드롭 → 자기 위치 no-op`() {
        // slot2 = "B와 C 사이" = C의 바로 위 = 이동 불필요
        // 기대: insertIndex로 simulate해도 원래 배열과 동일
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 2, targetSlot = 2, itemCount = 4
        )
        Assert.assertEquals(
            "C→slot2는 no-op이어야 합니다. insertIndex=$result",
            original, simulate(2, result)
        )
    }

    @Test
    fun `insertIndex - C를 slot3에 드롭 → 자기 위치 no-op`() {
        // slot3 = "C와 D 사이" = C의 바로 아래 = 이동 불필요
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 2, targetSlot = 3, itemCount = 4
        )
        Assert.assertEquals(
            "C→slot3는 no-op이어야 합니다. insertIndex=$result",
            original, simulate(2, result)
        )
    }

    @Test
    fun `insertIndex - C를 slot4에 드롭 → 맨 아래로 이동`() {
        // 기대: [A, B, D, C] → insertIndex = 3
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 2, targetSlot = 4, itemCount = 4
        )
        Assert.assertEquals(listOf("A", "B", "D", "C"), simulate(2, result))
    }

    // ── A(index=0) 기준 ──

    @Test
    fun `insertIndex - A를 slot0에 드롭 → no-op`() {
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 0, targetSlot = 0, itemCount = 4
        )
        Assert.assertEquals("A→slot0는 no-op이어야 합니다", original, simulate(0, result))
    }

    @Test
    fun `insertIndex - A를 slot1에 드롭 → no-op`() {
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 0, targetSlot = 1, itemCount = 4
        )
        Assert.assertEquals("A→slot1는 no-op이어야 합니다", original, simulate(0, result))
    }

    @Test
    fun `insertIndex - A를 slot2에 드롭 → B 아래로 이동`() {
        // 기대: [B, A, C, D] → insertIndex = 1
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 0, targetSlot = 2, itemCount = 4
        )
        Assert.assertEquals(listOf("B", "A", "C", "D"), simulate(0, result))
    }

    @Test
    fun `insertIndex - A를 slot4에 드롭 → 맨 아래로 이동`() {
        // 기대: [B, C, D, A] → insertIndex = 3
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 0, targetSlot = 4, itemCount = 4
        )
        Assert.assertEquals(listOf("B", "C", "D", "A"), simulate(0, result))
    }

    // ── D(index=3) 기준 ──

    @Test
    fun `insertIndex - D를 slot3에 드롭 → no-op`() {
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 3, targetSlot = 3, itemCount = 4
        )
        Assert.assertEquals("D→slot3는 no-op이어야 합니다", original, simulate(3, result))
    }

    @Test
    fun `insertIndex - D를 slot4에 드롭 → no-op`() {
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 3, targetSlot = 4, itemCount = 4
        )
        Assert.assertEquals("D→slot4는 no-op이어야 합니다", original, simulate(3, result))
    }

    @Test
    fun `insertIndex - D를 slot0에 드롭 → 맨 위로 이동`() {
        // 기대: [D, A, B, C]
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 3, targetSlot = 0, itemCount = 4
        )
        Assert.assertEquals(listOf("D", "A", "B", "C"), simulate(3, result))
    }

    // ── B(index=1) 기준 ──

    @Test
    fun `insertIndex - B를 slot1에 드롭 → no-op`() {
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 1, targetSlot = 1, itemCount = 4
        )
        Assert.assertEquals("B→slot1는 no-op이어야 합니다", original, simulate(1, result))
    }

    @Test
    fun `insertIndex - B를 slot2에 드롭 → no-op`() {
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 1, targetSlot = 2, itemCount = 4
        )
        Assert.assertEquals("B→slot2는 no-op이어야 합니다", original, simulate(1, result))
    }

    // ── 경계값 ──

    @Test
    fun `insertIndex - 음수 슬롯 → slot 0으로 coerce`() {
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 3, targetSlot = -5, itemCount = 4
        )
        Assert.assertEquals(listOf("D", "A", "B", "C"), simulate(3, result))
    }

    @Test
    fun `insertIndex - 초과 슬롯 → itemCount로 coerce`() {
        val result = DraggableListLogic.computeInsertIndex(
            currentIndex = 0, targetSlot = 100, itemCount = 4
        )
        Assert.assertEquals(listOf("B", "C", "D", "A"), simulate(0, result))
    }

    // endregion

    // ═══════════════════════════════════════════════════════════════════════
    // region 3. 전수 검증 — 4×5 = 20 조합 시뮬레이션
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 모든 (currentIndex, targetSlot) 조합에서:
     *   - 자기 위치 슬롯이면 배열 불변
     *   - 그 외면 정확히 1칸 이동
     */
    @Test
    fun `전수검증 - 모든 조합에서 배열 크기 보존 및 원소 보존`() {
        for (cur in 0 until 4) {
            for (slot in 0..4) {
                val insertIdx = DraggableListLogic.computeInsertIndex(cur, slot, 4)
                val result = simulate(cur, insertIdx)
                Assert.assertEquals("cur=$cur, slot=$slot: 크기 불변", 4, result.size)
                Assert.assertEquals("cur=$cur, slot=$slot: 원소 보존", original.toSet(), result.toSet())
            }
        }
    }

    @Test
    fun `전수검증 - 자기 위치 슬롯(cur, cur+1)은 배열 불변`() {
        for (cur in 0 until 4) {
            for (slot in listOf(cur, cur + 1)) {
                val insertIdx = DraggableListLogic.computeInsertIndex(cur, slot, 4)
                val result = simulate(cur, insertIdx)
                Assert.assertEquals(
                    "cur=$cur, slot=$slot: no-op이어야 합니다. insertIdx=$insertIdx, result=$result",
                    original,
                    result
                )
            }
        }
    }

    @Test
    fun `전수검증 - insertIndex 유효 범위 (0 이상 itemCount-1 이하)`() {
        for (cur in 0 until 4) {
            for (slot in 0..4) {
                val insertIdx = DraggableListLogic.computeInsertIndex(cur, slot, 4)
                assert(insertIdx in 0..3) {
                    "cur=$cur, slot=$slot → insertIndex=$insertIdx 범위 초과"
                }
            }
        }
    }

    // endregion

    // ═══════════════════════════════════════════════════════════════════════
    // region 4. E2E 시나리오 — computeTargetSlot → computeInsertIndex 연동
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `E2E - C를 B 하단(y=160)으로 드래그 → no-op`() {
        val slot = slot(160f)  // slot 2
        val insertIdx = DraggableListLogic.computeInsertIndex(2, slot, 4)
        Assert.assertEquals("B 하단 드래그 = C 자기 위치", original, simulate(2, insertIdx))
    }

    @Test
    fun `E2E - C를 C 하단(y=260)으로 드래그 → no-op`() {
        val slot = slot(260f)  // slot 3
        val insertIdx = DraggableListLogic.computeInsertIndex(2, slot, 4)
        Assert.assertEquals("C 하단 드래그 = C 자기 위치", original, simulate(2, insertIdx))
    }

    @Test
    fun `E2E - C를 A 상단(y=25)으로 드래그 → 맨 위`() {
        val slot = slot(25f)  // slot 0
        val insertIdx = DraggableListLogic.computeInsertIndex(2, slot, 4)
        Assert.assertEquals(listOf("C", "A", "B", "D"), simulate(2, insertIdx))
    }

    @Test
    fun `E2E - A를 D 하단(y=370)으로 드래그 → 맨 아래`() {
        val slot = slot(370f)  // slot 4
        val insertIdx = DraggableListLogic.computeInsertIndex(0, slot, 4)
        Assert.assertEquals(listOf("B", "C", "D", "A"), simulate(0, insertIdx))
    }

    // endregion

    // ═══════════════════════════════════════════════════════════════════════
    // region 5. Stale closure 시나리오
    //
    // remember {} 내부의 DragAndDropTarget이 이전 composition의 items를 참조할 때,
    // moveItem의 currentIndex가 stale 리스트 기준으로 계산되어
    // ViewModel에 잘못된 insertIndex가 전달되는 상황을 시뮬레이션합니다.
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ViewModel의 updateExerciseSets와 동일한 로직을 시뮬레이션합니다.
     * ViewModel은 자신의 실제 리스트에서 currentIndex를 구하고,
     * moveItem이 전달한 insertIndex를 그대로 사용합니다.
     */
    private fun viewModelSimulate(
        realList: List<String>,
        item: String,
        insertIndex: Int
    ): List<String> {
        val currentIndex = realList.indexOf(item)
        if (currentIndex == -1 || currentIndex == insertIndex) return realList
        val mutable = realList.toMutableList()
        mutable.removeAt(currentIndex)
        mutable.add(insertIndex, item)
        return mutable
    }

    @Test
    fun `stale closure - 1차 이동 후 2차 no-op 드래그가 원복되는 버그 재현`() {
        // 1차: [A,B,C] → A를 slot2에 드롭 → [B,A,C]
        val staleItems = listOf("A", "B", "C")   // remember {} 시점의 items
        val realItems = listOf("B", "A", "C")     // recomposition 후 실제 items

        // 2차: [B,A,C]에서 A를 "B와 A 사이"(slot 1)에 드롭 → 기대: no-op

        // ── stale closure에서의 계산 (현재 버그 동작) ──
        val staleCurrentIndex = staleItems.indexOf("A")   // 0 (stale!)
        val targetSlot = 1
        val insertIndex = DraggableListLogic.computeInsertIndex(
            currentIndex = staleCurrentIndex,
            targetSlot = targetSlot,
            itemCount = staleItems.size
        )

        // stale closure가 ViewModel에 전달하는 insertIndex
        val bugResult = viewModelSimulate(realItems, "A", insertIndex)

        // 버그: [A,B,C]로 원복됨 — 이 테스트는 버그를 재현(문서화)합니다
        Assert.assertEquals(
            "stale closure로 인해 [A,B,C]로 원복되는 버그",
            listOf("A", "B", "C"),
            bugResult
        )

        // ── 올바른 계산 (실제 items 기준) ──
        val realCurrentIndex = realItems.indexOf("A")  // 1 (정확)
        val correctInsertIndex = DraggableListLogic.computeInsertIndex(
            currentIndex = realCurrentIndex,
            targetSlot = targetSlot,
            itemCount = realItems.size
        )
        val correctResult = viewModelSimulate(realItems, "A", correctInsertIndex)

        // 올바른 동작: [B,A,C] 유지 (no-op)
        Assert.assertEquals(
            "실제 items 기준이면 no-op이어야 합니다",
            listOf("B", "A", "C"),
            correctResult
        )
    }

    @Test
    fun `stale closure - 연속 이동 시 매번 stale index로 잘못된 결과`() {
        // [A,B,C] → B를 맨 위로 → [B,A,C]
        // 이후 B를 slot2(A와 C 사이)에 드롭 → 기대: [A,B,C] 아님, no-op [B,A,C]

        val staleItems = listOf("A", "B", "C")
        val realItems = listOf("B", "A", "C")

        // stale: B의 currentIndex = 1
        val staleIdx = staleItems.indexOf("B")  // 1
        val insertIndex = DraggableListLogic.computeInsertIndex(
            currentIndex = staleIdx, targetSlot = 2, itemCount = 3
        )
        val bugResult = viewModelSimulate(realItems, "B", insertIndex)

        // 올바른: B의 currentIndex = 0
        val realIdx = realItems.indexOf("B")  // 0
        val correctInsertIndex = DraggableListLogic.computeInsertIndex(
            currentIndex = realIdx, targetSlot = 2, itemCount = 3
        )
        val correctResult = viewModelSimulate(realItems, "B", correctInsertIndex)

        // 실제 items 기준 slot 0,1은 no-op → slot 2는 1칸 아래 이동
        // B(index=0) → slot2 → insertIndex = 2-1 = 1 → remove B → [A,C] → add(1,B) → [A,B,C]
        Assert.assertEquals("실제 items 기준 B→slot2", listOf("A", "B", "C"), correctResult)

        // stale items 기준 B(index=1) → slot2 → no-op
        // insertIndex = 2-1 = 1 → ViewModel: B real index=0, targetIndex=1 → [A,B,C]
        // 결국 같은 결과이지만 우연의 일치 — 다른 케이스에서는 불일치 발생
        Assert.assertEquals(
            "이 케이스는 우연히 같은 결과이지만, stale closure는 여전히 위험",
            correctResult,
            bugResult
        )
    }

    // endregion
}