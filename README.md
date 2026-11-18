# Pocam

**Pocket Camera** — an offline-first Android camera app.

## Current state
Early prototype (v0.2). CameraX preview and basic photo capture are implemented in source. The shutter long-press mode wheel is experimental. Pro, Video, Portrait and Night camera features are not implemented yet. Builds and device behavior require verification.

## Stack
- Kotlin, Jetpack Compose, Material 3 dynamic colors
- CameraX and Camera2 integration planned
- Future offline OpenCV and on-device ML

## Design
- Full-screen portrait and landscape camera
- Minimal circular controls and white shutter
- Long-press shutter to show radial modes; drag to highlight; release to select and close
- Optional performance overlay planned
- Approved Pocam pocket-camera logo is a design reference; production drawable assets not yet integrated

## Build
The repository currently does not include a Gradle wrapper. Android CI uses a pinned Gradle 8.11.1 installation. For local builds, install Android Studio and JDK 17, then generate the wrapper with:

```sh
gradle wrapper --gradle-version 8.11.1
./gradlew :app:assembleDebug
```

Alternatively use `gradle :app:assembleDebug` with Gradle 8.11.1 installed. Check the [Actions tab](../../actions) for build results.

## Planned packages
- `camera/`: capture and device capabilities
- `domain/model/`: shooting modes and state
- `ui/components/`: shutter and radial mode wheel
- `ui/screen/`: immersive camera screen
- `ui/theme/`: Material You theme
- `performance/`: supported performance metrics
- `processing/`: future offline image processing
