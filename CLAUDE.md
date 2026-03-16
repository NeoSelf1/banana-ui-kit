# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build core-ui library
./gradlew :core-ui:assembleDebug

# Build sample app
./gradlew :core-ui:sample:assembleDebug

# Run core-ui unit tests (DraggableListLogicTest)
./gradlew :core-ui:testDebugUnitTest

# Run sample unit tests
./gradlew :core-ui:sample:testDebugUnitTest

# Run sample instrumented tests (requires connected device/emulator)
./gradlew :core-ui:sample:connectedAndroidTest

# Build all
./gradlew assembleDebug

# Lint
./gradlew lint
```

## Project Overview

Multi-module Android Jetpack Compose project. `core-ui` is a reusable UI component library, `core-ui/sample` is a demo app that showcases the components.

**SDK:** minSdk 24, targetSdk 35, compileSdk 35, JVM 17. Compose BOM 2024.12.01, Kotlin 2.0.21.

## Module Structure

### `:core-ui` — Library module (`com.neon.core.ui`)

Reusable UI components, theme, and utilities:

- **component/list/** — `HMDraggableList` (generic drag-and-drop), `HMInfinityScrollList` (pull-to-refresh + infinite scroll), `Draggable` interface
- **component/button/** — `HMClickable` (Modifier.Node optimized tap animation), `HMKeyboardButton` (keyboard-attached action button)
- **component/textfield/** — `HMTextField`, `HMSearchTextField`
- **component/bottomSheet/** — `HMBottomSheet` (declarative global bottom sheet with Provider/Controller/View pattern)
- **component/loadingState/** — `LoadingState<T>` sealed class, `ItemLoadingState<ID>`
- **component/** — `HMPicker`, `HMBackHeader`, `HMDivider`, `HMIcon`, `HMLoadingIndicator`, `HMPlaceholder`, `HMScreen`, `HMToggle`
- **theme/** — `HumaniaTheme`, `HMFont` (Pretendard typography), `HMAnimations`, Color palette
- **image/** — `SvgImageLoader` (Coil SVG singleton)
- **modifier/** — `dropShadow` (custom shadow with blur/spread)
- **infrastructure/** — `RecompositionTracker`

### `:core-ui:sample` — Application module (`com.neon.sample`)

Demo views and performance tests:

- `HMDraggableListDemoView` — Drag-and-drop demo with edit mode, counters, header/footer
- `DragAndDropDemoView` — Simple DnD demo using `DragAndDropListState`
- `HMButtonComparisonDemoView` — `HMClickable` vs `HMButtonLegacy` performance comparison
- `HMPickerDemoView` — Picker demo with selection counter
- `HMButtonLegacy` / `HMPickerLegacy` — Legacy implementations for benchmarking
- Instrumented tests: button performance, list scroll performance, picker UI test

## Conventions

- Commit messages are in Korean, prefixed with type: `fix:`, `refactor:`, `delete:`, `feat:`
- Dependencies managed via `gradle/libs.versions.toml` version catalog
- core-ui uses Coil for SVG rendering; sample has no additional third-party UI libraries
- Constants for magic numbers are defined at file top level (e.g., `SCROLL_THRESHOLD_DP`, `SCROLL_VELOCITY`)
