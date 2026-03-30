# BananaUIKit
Android Jetpack Compose 기반의 재사용 가능한 UI 컴포넌트 라이브러리입니다.

## Tech Stack

- **Kotlin** 2.0.21 / **Compose BOM** 2024.12.01
- **minSdk** 24 / **targetSdk** 35 / **compileSdk** 35 / **JVM** 17
- **Coil** 2.6.0 (SVG 렌더링)

## 모듈 구조

```
banana-ui-kit/
├── banana-ui-core/       # 라이브러리 모듈 (com.neon.core.ui)
│   └── sample/           # 데모 앱 모듈 (com.neon.sample)
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
| `NeoTextField` | 포커스 테두리 애니메이션, 비밀번호 마스킹, 삭제 버튼, 최대 길이 제한 지원 |
| `NeoSearchTextField` | 검색 전용 플랫 스타일 텍스트 필드 |

### Picker

| 컴포넌트 | 설명 |
|---|---|
| `NeoPicker` | iOS 스타일 휠 피커. 스냅 스크롤, 엣지 페이드, 센터 하이라이트. `itemHeight`, `highlightColor`, `highlightBlendMode` 파라미터로 라이트/다크 테마 대응 |

### Dialog

| 컴포넌트 | 설명 |
|---|---|
| `NeoDialog` | 범용 다이얼로그. 타이틀/메시지/확인·취소 버튼 + 아이콘·추가 컨텐츠 슬롯. 색상 전부 파라미터화 |

### Bottom Sheet

| 컴포넌트 | 설명 |
|---|---|
| `NeoBottomSheet` | Provider / Controller / View 패턴의 선언형 글로벌 바텀시트. iOS `.sheet(isPresented:)` 스타일 API |

### Background

| 컴포넌트 | 설명 |
|---|---|
| `NeoGradientBackground` | 방사형 웨이브 애니메이션 그라디언트 배경. `NeoGradientWaveState`로 웨이브 제어, `background` 슬롯으로 베이스 이미지 교체 가능. 기본 `gradient_base` 이미지 내장 |

### Indicator

| 컴포넌트 | 설명 |
|---|---|
| `NeoStepIndicator` | 단계 진행률 인디케이터. 원형 + 연결선 + 글로우 애니메이션. `activeColor`, `inactiveColor` 등 커스터마이징 가능 |
| `NeoLoadingIndicator` | 동적 상단 패딩이 적용된 로딩 스피너 |

### 기타 컴포넌트

| 컴포넌트 | 설명 |
|---|---|
| `NeoScreen` | 상태바/네비게이션바 패딩, IME 처리, 로딩 오버레이를 포함한 베이스 스크린 |
| `NeoIcon` | Coil 기반 SVG 아이콘 로더. 알림 뱃지, 탭 콜백 지원 |
| `NeoPlaceholder` | 빈 상태 안내 텍스트. 화면 높이 비례 동적 패딩 |
| `NeoBackHeader` | 뒤로가기 네비게이션 헤더. Base / WithTitle / WithProgress 3가지 유형 |
| `NeoDivider` | 수평 구분선 |
| `NeoToggle` | Material3 Switch 래퍼 |

### 유틸리티

| 항목 | 설명 |
|---|---|
| `LoadingState<T>` | Idle → Loading → Loaded / Refreshing 시드 클래스 |
| `ItemLoadingState<ID>` | 개별 아이템 로딩 상태 추적 |
| `dropShadow` | blur, spread, offset 커스텀 그림자 Modifier |
| `SvgImageLoader` | Coil SVG 싱글톤 이미지 로더 |

### Typography — `NeoFont`

Pretendard (Regular, Medium, SemiBold, Bold) + Esamanru (Bold) 폰트 기반 싱글톤 타이포그래피:

| 계층 | 스타일 | 크기 |
|---|---|---|
| **Display** | display1 ~ display4 | 128sp ~ 48sp |
| **Headline** | headline1 ~ headline6 | 36sp ~ 22sp |
| **Subhead** | subhead1 ~ subhead6 | 20sp ~ 16sp |
| **Body** | body1 ~ body6 | 14sp ~ 10sp |
| **Caption** | caption1 | 12sp |
| **Logo** | `LogoTextStyle` | 28sp (Esamanru Bold) |

```kotlin
Text("제목", style = NeoFont.headline1)
Text("본문", style = NeoFont.body2)
Text("로고", style = LogoTextStyle)
```

### Color
| 그룹 | 범위 |
|---|---|
| **Primary** | Primary05 ~ Primary90 (퍼플 계열) |
| **Secondary** | Secondary05 ~ Secondary90 (틸 계열) |
| **Gray** | Gray10 ~ Gray90 |
| **Surface** | SurfaceDisabled, SurfaceMedium, SurfaceHigh |
| **System** | Warning, Caution |
| **Dark** | DarkBackground70/80/90, DarkPrimary40/50, DarkSecondary50, DarkWarning, DarkCaution |

```kotlin
// 라이트 테마 (Humania-mobile)
Box(Modifier.background(Gray10))
Text("텍스트", color = Primary50)

// 다크 테마 (SEGYM_CR-mobile)
Box(Modifier.background(DarkBackground90))
Text("텍스트", color = DarkPrimary50)
```

### Animation — `NeoAnimations`
- **Tween**: Fast (100ms), MediumFast (200ms), Medium (300ms)
- **Spring**: FastSpringFloat, MediumSpringFloat

## 빌드

```bash
# 라이브러리 빌드
./gradlew :banana-ui-core:assembleDebug

# 샘플 앱 빌드
./gradlew :banana-ui-core:sample:assembleDebug

# 전체 빌드
./gradlew assembleDebug
```

## 테스트

```bash
# 유닛 테스트
./gradlew :banana-ui-core:testDebugUnitTest

# 샘플 유닛 테스트
./gradlew :banana-ui-core:sample:testDebugUnitTest

# 계측 테스트 (디바이스/에뮬레이터 필요)
./gradlew :banana-ui-core:sample:connectedAndroidTest

# 린트
./gradlew lint
```

## 샘플 앱

`:banana-ui-core:sample` 모듈에서 각 컴포넌트의 데모를 확인할 수 있습니다:

- **NeoDraggableListDemoView** — 드래그 앤 드롭 데모 (편집 모드, 리오더 카운터, 헤더/푸터)
- **DragAndDropDemoView** — 간단한 DnD 데모
- **NeoButtonComparisonDemoView** — NeoClickable vs 레거시 버튼 성능 비교
- **NeoPickerDemoView** — 피커 컴포넌트 쇼케이스
