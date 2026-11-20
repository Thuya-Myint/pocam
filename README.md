# Pocam (Pocket Camera)

Offline-first Android camera prototype, written in Kotlin with Jetpack Compose, CameraX and Camera2 interop.

## Current stage: v0.5 — not production-ready

- Live CameraX preview, photo capture, video recording, lens switching and tap-to-focus.
- Pro controls for supported Camera2 ISO, exposure time, focus, white balance presets and exposure compensation.
- Curved zoom/mode controls and four-tab settings in a graphite/silver/orange design.
- CameraX Night and Portrait names currently share the ordinary photo pipeline because OEM extensions are disabled. These are **not verified night/portrait processing modes**.
- Video recording currently has no microphone permission or audio-enabled recorder configuration: do **not** advertise audio recording.
- Capture mode prioritizes low latency. Highest photo quality and exact photo/video resolutions have not been benchmarked on all devices.
- Native release performance, frame pacing, UI screenshot fidelity, and device coverage have **not** passed acceptance tests.

See [v0.5 acceptance criteria](docs/v0.5-ui-acceptance.md). **Android CI only builds an installable debug APK; it does not establish user-experience quality.**

## Technology

- Kotlin, Jetpack Compose, Material 3
- CameraX 1.4.2, Camera2 interop
- Device-local capture with no required cloud services or per-photo charges

## Local development

The repository does not currently include a Gradle wrapper. Use Android Studio and JDK 17, or run:

```sh
gradle :app:assembleDebug
```

GitHub Actions installs Gradle 8.11.1 and creates a downloadable debug APK artifact.

## Production prerequisites

Before publishing to Google Play: implement or explicitly disable unverified features, finish real-device manual camera testing, test Android back/navigation and permissions, run release-build profiling on the target phone, add automated behavior/screenshot coverage, verify gallery/export/video files, and provide appropriate store/privacy declarations.
