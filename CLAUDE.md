# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.humanics.exampleapplication.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Project Overview

Android Jetpack Compose practice project focused on **drag-and-drop list reordering** and **optimized button components**. Single-module app (`app/`), package: `com.humanics.exampleapplication`.

**SDK:** minSdk 24, targetSdk 35, compileSdk 35, JVM 11. Compose BOM 2024.12.01, Kotlin 2.0.21.

## Architecture

### Core Components (`component/`)

- **HMDraggableList** — Generic `<T : Draggable>` composable for drag-and-drop reordering. Uses `Column + verticalScroll` (not LazyColumn) to avoid scroll position issues during reorder. Frame-synchronized auto-scrolling via `withFrameNanos`. Callback-based: `onReorder`, `onTapRow`, `onMoveItem`. Supports header/footer slots.

- **HMButton** — Uses Modifier.Node API (`DrawModifierNode`) to eliminate recompositions during press animation. Supports three transition types: Shrink, ShrinkWithTilt, ShrinkWithGrayBackground. The legacy version (`HMButtonLegacy`) is kept as a reference.

- **DragAndDropListState** — Helper for simple DnD state management (used by `DragAndDropDemoView`).

### Data Model (`model/`)

`Draggable` interface (requires `id: Int`) implemented by `DemoItem`. Any item used with `HMDraggableList` must implement `Draggable`.

### Demo Views

- `HMDraggableListDemoView` — Full-featured demo using `HMDraggableList` with edit mode, counters, header/footer.
- `DragAndDropDemoView` — Simpler DnD demo using `DragAndDropListState`.

## Conventions

- Commit messages are in Korean, prefixed with type: `fix:`, `refactor:`, `delete:`, `feat:`
- Dependencies managed via `gradle/libs.versions.toml` version catalog
- No third-party UI libraries; only AndroidX/Compose dependencies
- Constants for magic numbers are defined at file top level (e.g., `SCROLL_THRESHOLD_DP`, `SCROLL_VELOCITY`)
