# BananaUIKit

Android Jetpack Compose 기반의 재사용 가능한 UI 컴포넌트 라이브러리입니다.

## Tech Stack

- **Kotlin** 2.0.21 / **Compose BOM** 2024.12.01
- **minSdk** 24 / **targetSdk** 35 / **compileSdk** 35 / **JVM** 17
- **Coil** 2.6.0 (SVG 렌더링)

## 모듈 구조

```
CoreUIModule/
├── core-ui/          # 라이브러리 모듈 (com.neon.core.ui)
│   └── sample/       # 데모 앱 모듈 (com.neon.sample)
├── gradle/
│   └── libs.versions.toml
└── build.gradle.kts
```

## 컴포넌트

### List

| 컴포넌트 | 설명 |
|---|---|
| `NeoDraggableList` | Column 기반 드래그 앤 드롭 리스트. Android Drag and Drop API 사용, 자동 스크롤, 스프링 애니메이션, 햅틱 피드백 지원 |
| `NeoInfinityScrollList` | Pull-to-refresh + 무한 스크롤 리스트. `LoadingState<T>` 기반 라이프사이클 관리 |

### Button

| 컴포넌트 | 설명 |
|---|---|
| `NeoClickable` | Modifier.Node 최적화 탭 애니메이션 래퍼. Shrink / ShrinkWithTilt / ShrinkWithGrayBackground 3가지 트랜지션 |
| `NeoKeyboardButton` | 키보드 상단에 부착되는 액션 버튼. 로딩 인디케이터, 비활성 상태 지원 |

### TextField

| 컴포넌트 | 설명 |
|---|---|
| `NeoTextField` | 기본 텍스트 입력 필드 |
| `NeoSearchTextField` | 검색 전용 텍스트 필드 |

### Bottom Sheet

| 컴포넌트 | 설명 |
|---|---|
| `NeoBottomSheet` | Provider / Controller / View 패턴의 선언형 글로벌 바텀시트. iOS `.sheet(isPresented:)` 스타일 API |

### 기타 컴포넌트

| 컴포넌트 | 설명 |
|---|---|
| `NeoScreen` | 상태바/네비게이션바 패딩, IME 처리, 로딩 오버레이를 포함한 베이스 스크린 |
| `NeoPicker` | iOS 스타일 휠 피커. 스냅 스크롤, 엣지 페이드, 센터 하이라이트 |
| `NeoIcon` | Coil 기반 SVG 아이콘 로더 |
| `NeoLoadingIndicator` | 로딩 스피너 |
| `NeoPlaceholder` | 빈 상태 플레이스홀더 |
| `NeoBackHeader` | 뒤로가기 네비게이션 헤더 |
| `NeoDivider` | 수평 구분선 |
| `NeoToggle` | 토글 스위치 |

### 유틸리티

| 컴포넌트 | 설명 |
|---|---|
| `LoadingState<T>` | Idle → Loading → Loaded / Refreshing 시드 클래스 |
| `ItemLoadingState<ID>` | 개별 아이템 로딩 상태 추적 |
| `dropShadow` | blur, spread, offset 커스텀 그림자 Modifier |
| `SvgImageLoader` | Coil SVG 싱글톤 이미지 로더 |

## 테마

### Typography

Pretendard 폰트 패밀리 (Regular, Medium, SemiBold, Bold) 기반 24가지 텍스트 스타일:

- **Headline** 1-6 (36sp ~ 22sp)
- **Subhead** 1-6 (20sp ~ 16sp)
- **Body** 1-6 (14sp ~ 10sp)

### Color

iOS 에셋과 동기화된 컬러 시스템:

- Primary (05-90), Secondary (05-90), Gray (10-90)
- Surface, System 컬러

### Animation

- Tween: Fast (100ms), MediumFast (200ms), Medium (300ms)
- Spring: FastSpringFloat, MediumSpringFloat

## 빌드

```bash
# 라이브러리 빌드
./gradlew :core-ui:assembleDebug

# 샘플 앱 빌드
./gradlew :core-ui:sample:assembleDebug

# 전체 빌드
./gradlew assembleDebug
```

## 테스트

```bash
# 유닛 테스트
./gradlew :core-ui:testDebugUnitTest

# 샘플 유닛 테스트
./gradlew :core-ui:sample:testDebugUnitTest

# 계측 테스트 (디바이스/에뮬레이터 필요)
./gradlew :core-ui:sample:connectedAndroidTest

# 린트
./gradlew lint
```

## 샘플 앱

`:core-ui:sample` 모듈에서 각 컴포넌트의 데모를 확인할 수 있습니다:

- **NeoDraggableListDemoView** — 드래그 앤 드롭 데모 (편집 모드, 리오더 카운터, 헤더/푸터)
- **DragAndDropDemoView** — 간단한 DnD 데모
- **NeoButtonComparisonDemoView** — NeoClickable vs 레거시 버튼 성능 비교
- **NeoPickerDemoView** — 피커 컴포넌트 쇼케이스
