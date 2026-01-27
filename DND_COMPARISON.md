# DnD 구현 비교 분석: Legacy vs HMDraggableList

## 개요

이 문서는 Humania-android 프로젝트의 DnD(Drag and Drop) 구현 과정에서
**레거시 코드(DragAndDropDemoView)**와 **개선된 코드(HMDraggableList)**의 차이점을 분석합니다.

---

## 1. 아키텍처 비교

### 레거시 구현 (DragAndDropDemoView)

```
┌─────────────────────────────────────────────────────┐
│                 DragAndDropDemoView                 │
│  ┌───────────────────────────────────────────────┐  │
│  │ 상태 관리                                      │  │
│  │ - targetedDropIndex                           │  │
│  │ - lastDragY                                   │  │
│  │ - desiredVelocity                             │  │
│  │ - items                                       │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │ DnD 로직                                       │  │
│  │ - dragAndDropSource                           │  │
│  │ - dragAndDropTarget                           │  │
│  │ - computeTargetIndex()                        │  │
│  │ - moveItem()                                  │  │
│  │ - 자동 스크롤 루프                             │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │ UI 렌더링                                      │  │
│  │ - LazyColumn                                  │  │
│  │ - DemoItemRow                                 │  │
│  │ - Drop Indicator                              │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

**문제점:**
- 모든 로직이 하나의 View에 집중
- 재사용 불가능
- 테스트하기 어려움
- 관심사 분리 없음

### 개선된 구현 (HMDraggableList)

```
┌─────────────────────────────────────────────────────┐
│              HMDraggableListDemoView                │
│  ┌───────────────────────────────────────────────┐  │
│  │ 비즈니스 상태                                   │  │
│  │ - items                                       │  │
│  │ - isEditMode                                  │  │
│  │ - reorderCount                                │  │
│  └───────────────────────────────────────────────┘  │
│                        │                            │
│                        ▼                            │
│  ┌───────────────────────────────────────────────┐  │
│  │           HMDraggableList<T>                  │  │
│  │  ┌─────────────────────────────────────────┐  │  │
│  │  │ DnD 상태 (내부 관리)                     │  │  │
│  │  │ - targetedDropIndex                     │  │  │
│  │  │ - prevTargetedDropIndex                 │  │  │
│  │  │ - draggingItemId                        │  │  │
│  │  │ - desiredVelocity                       │  │  │
│  │  │ - lastDragY                             │  │  │
│  │  └─────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────┐  │  │
│  │  │ 콜백 인터페이스                          │  │  │
│  │  │ - onMoveItem: (T, Int) -> Unit          │  │  │
│  │  │ - onTapRow: (T) -> Unit                 │  │  │
│  │  │ - onReorder: () -> Unit                 │  │  │
│  │  └─────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

**개선점:**
- DnD 로직이 재사용 가능한 컴포넌트로 분리
- 콜백으로 비즈니스 로직 위임
- 제네릭으로 어떤 타입에도 적용 가능
- 테스트 용이

---

## 2. 상세 비교표

| 항목 | 레거시 (DragAndDropDemoView) | 개선 (HMDraggableList) | 해결 여부 |
|------|------------------------------|------------------------|-----------|
| **재사용성** | View에 직접 구현 | 제네릭 컴포넌트 분리 | ✅ 해결 |
| **상태 관리** | View에 분산 | 컴포넌트 내부 캡슐화 | ✅ 해결 |
| **콜백 아키텍처** | 없음 (직접 조작) | onMoveItem, onTapRow, onReorder | ✅ 해결 |
| **햅틱 피드백** | 없음 | 드롭 타겟 변경 시 피드백 | ✅ 해결 |
| **드래그 중 시각적 피드백** | 없음 | draggingItemId + opacity 0.8 | ✅ 해결 |
| **자동 스크롤** | delay(16) (부정확) | withFrameNanos (정확) | ✅ 해결 |
| **매직 넘버** | 96.dp, 64.dp, 1200f, 640 | 상수 분리 + 파라미터화 | ✅ 해결 |
| **Header/Footer** | 미지원 | 컴포저블 슬롯 지원 | ✅ 해결 |
| **rowHeight** | 하드코딩 | 파라미터로 주입 | ✅ 해결 |
| **headerOffset** | 하드코딩 (640) | 파라미터로 주입 | ✅ 해결 |
| **ListState** | 내부 생성 | 외부 주입 가능 | ✅ 해결 |

---

## 3. 코드 비교

### 3.1 자동 스크롤 구현

#### 레거시 (문제점: 부정확한 타이밍)

```kotlin
// delay(16)으로 약 60fps 시도 - 정확하지 않음
LaunchedEffect(Unit) {
    snapshotFlow { desiredVelocity }.collect { velocity ->
        while (desiredVelocity != 0f) {
            val pixels = (velocity / 60f).toInt()
            listState.dispatchRawDelta(-pixels.toFloat())
            delay(16)  // ❌ 화면 주사율과 동기화되지 않음
        }
    }
}
```

#### 개선 (해결: 프레임 동기화)

```kotlin
// withFrameNanos로 정확한 프레임 동기화
LaunchedEffect(listState) {
    var lastTime = 0L
    while (isActive) {
        val dtNanos = withFrameNanos { now ->  // ✅ 프레임과 동기화
            val dt = if (lastTime == 0L) 0L else now - lastTime
            lastTime = now
            dt
        }
        val v = desiredVelocity
        if (v != 0f && dtNanos > 0L) {
            val dtSec = dtNanos / 1_000_000_000f
            val delta = v * dtSec
            val consumed = listState.scrollBy(delta)
            if (abs(consumed) < 0.5f) {
                desiredVelocity = 0f  // ✅ 스크롤 끝 감지
            }
        }
    }
}
```

### 3.2 햅틱 피드백

#### 레거시 (문제점: 피드백 없음)

```kotlin
// 햅틱 피드백 코드 없음
// 사용자가 드롭 위치 변경을 인지하기 어려움
```

#### 개선 (해결: 드롭 타겟 변경 시 피드백)

```kotlin
// 이전 인덱스와 비교하여 변경 시에만 피드백
LaunchedEffect(targetedDropIndex) {
    if (prevTargetedDropIndex != targetedDropIndex && targetedDropIndex != null) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    prevTargetedDropIndex = targetedDropIndex
}
```

### 3.3 드래그 중 시각적 피드백

#### 레거시 (문제점: 구분 불가)

```kotlin
// 모든 아이템이 동일하게 표시됨
DemoItemRow(
    modifier = Modifier.dragAndDropSource { ... },
    item = item,
    isEditMode = isEditMode
)
```

#### 개선 (해결: 드래그 중인 아이템 구분)

```kotlin
// draggingItemId로 추적하여 시각적 피드백 제공
var draggingItemId by remember { mutableStateOf<Int?>(null) }

Box(
    modifier = Modifier
        .alpha(if (isDragging) 0.8f else 1f)  // ✅ 드래그 중 opacity 변경
        .dragAndDropSource {
            detectTapGestures(
                onLongPress = {
                    draggingItemId = itemId  // ✅ 드래그 시작 시 ID 저장
                    startTransfer(...)
                }
            )
        }
) {
    itemContent(item, isDragging)  // ✅ isDragging 상태 전달
}
```

### 3.4 콜백 기반 아키텍처

#### 레거시 (문제점: View에서 직접 조작)

```kotlin
// View 내부에서 리스트 직접 조작
private fun moveItem(items: List<DemoItem>, item: DemoItem, to: Int): List<DemoItem> {
    val mutableList = items.toMutableList()
    val movedItem = mutableList.removeAt(currentIndex)
    mutableList.add(insertIndex, movedItem)
    return mutableList  // View에서 상태 직접 변경
}
```

#### 개선 (해결: 콜백으로 위임)

```kotlin
// 컴포넌트는 콜백만 호출, 실제 로직은 외부에서 처리
HMDraggableList(
    items = items,
    onMoveItem = { item, targetIndex ->
        // ViewModel이나 외부에서 처리 가능
        viewModel.moveItem(item, targetIndex)
    },
    onReorder = {
        viewModel.saveOrder()
    },
    ...
)
```

---

## 4. 매직 넘버 제거

### 레거시 (문제점)

```kotlin
// 하드코딩된 값들이 코드 곳곳에 흩어져 있음
val thresholdPx = with(density) { 96.dp.toPx() }  // 왜 96?
toTop < (thresholdPx + with(density) { 64.dp.toPx() })  // 왜 64?
desiredVelocity = -1200f  // 왜 1200?
val computedY = y - 640  // 왜 640? (헤더 높이라는 설명 없음)
```

### 개선 (해결)

```kotlin
// 상수로 분리하여 의미 명확화
private val SCROLL_THRESHOLD_DP = 96.dp      // 스크롤 영역 임계값
private val HEADER_THRESHOLD_DP = 64.dp      // 헤더 근처 추가 여유
private const val SCROLL_VELOCITY = 1200f    // 자동 스크롤 속도 (px/s)

// 파라미터로 주입하여 유연성 확보
fun <T> HMDraggableList(
    headerOffset: Float = 0f,  // 외부에서 주입
    rowHeight: Dp,             // 외부에서 주입
    ...
)
```

---

## 5. 사용 예시 비교

### 레거시 사용법

```kotlin
// 재사용 불가 - 직접 복사/붙여넣기 필요
@Composable
fun MyView() {
    var items by remember { mutableStateOf(myItems) }
    var targetedDropIndex by remember { mutableStateOf<Int?>(null) }
    var desiredVelocity by remember { mutableFloatStateOf(0f) }
    // ... 모든 상태와 로직을 직접 구현해야 함
}
```

### 개선된 사용법

```kotlin
// 재사용 가능한 컴포넌트
@Composable
fun MyView() {
    var items by remember { mutableStateOf(myItems) }

    HMDraggableList(
        items = items,
        rowHeight = 80.dp,
        isDragEnabled = true,
        onMoveItem = { item, targetIndex -> /* 이동 처리 */ },
        onReorder = { /* 정렬 완료 처리 */ },
        onTapRow = { item -> /* 탭 처리 */ },
        getItemId = { it.id },
        itemContent = { item, isDragging -> MyItemRow(item, isDragging) }
    )
}
```

---

## 6. 결론

HMDraggableList는 레거시 구현의 모든 주요 문제점을 해결했습니다:

| 카테고리 | 해결된 문제 |
|----------|-------------|
| **아키텍처** | 재사용 가능한 제네릭 컴포넌트로 분리, 콜백 기반 아키텍처 |
| **UX** | 햅틱 피드백, 드래그 중 시각적 피드백, 정확한 자동 스크롤 |
| **코드 품질** | 매직 넘버 제거, 상수 분리, 파라미터화 |
| **확장성** | Header/Footer 슬롯, 외부 ListState 주입, rowHeight 파라미터 |

이 개선된 구현은 iOS의 HMDraggableScrollView와 동일한 인터페이스를 제공하여
크로스 플랫폼 일관성도 확보했습니다.
