# BlockVerse build and verification report

Date: 2026-10-18

## Environment and configured toolchain

- Java runtime detected locally: OpenJDK 21.0.12.
- Project CI pins JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.2, libGDX 1.14.2, and LWJGL 3.3.3.
- Android module is configured for compile/target SDK 36, Build Tools 35.0.0, min SDK 21, and native ABIs `armeabi-v7a` + `arm64-v8a`.
- `gradle` is not installed in the creation environment; Android SDK is not configured.
- The attempt to obtain Gradle from `services.gradle.org` was blocked by unavailable DNS/network access (`curl: (6) Could not resolve host: services.gradle.org`).

## Checks actually run

| Check | Result | Notes |
|---|---|---|
| `./scripts/smoke-test.sh` | **PASS** | `javac` compiled the dependency-free world/player model and executed smoke tests for terrain, grass layer, save/load round trip, gravity settling, jump impulse, horizontal movement, and solid-wall collision. |
| `sh -n gradlew scripts/smoke-test.sh` | **PASS** | Shell syntax validation only; not a Gradle build. |
| Android manifest/resources XML parse | **PASS** | XML is well-formed; this does not prove Android compilation. |
| CI workflow YAML parse | **PASS WITH CAVEAT** | Parsed as YAML; GitHub-hosted job execution has not run. YAML 1.1 parsers may treat the workflow key `on` as a boolean, while GitHub Actions uses its own workflow schema. |
| `./gradlew :core:test :lwjgl3:dist --no-daemon` | **BLOCKED** | No Gradle executable/distribution was available, and downloading the pinned Gradle distribution failed due DNS/network access. No Gradle task ran. |
| JUnit (`:core:test`) | **NOT RUN** | Gradle/Maven dependencies could not be downloaded in this environment. JUnit test source is included. |
| Desktop compilation/distribution JAR | **NOT RUN** | No JAR was produced or visually launched. |
| Android debug APK | **NOT RUN** | No Android SDK and no Gradle distribution in this environment. No APK is included or claimed as built. |
| Windows x86/x64 runtime launch | **NOT RUN** | Both Windows native classifiers are configured, but launch on real 32-bit and 64-bit Windows/JVM combinations is not verified. |
| Visual/interaction test | **NOT RUN** | No display-backed OpenGL session or Android device was available. |

## Compatibility notes

- libGDX 1.14.2 is the stable release listed by the official project at archive creation time.
- Android Gradle Plugin 8.13 documents support through API 36.1, requires Gradle 8.13, and uses JDK 17; this project pins AGP 8.13.2 + Gradle 8.13 + SDK 36.
- Android native ABIs are configured as `armeabi-v7a` (ARM32) and `arm64-v8a` (ARM64). This is configuration, not proof an APK was produced.
- LWJGL Windows x64 and x86 native classifiers are configured. The desktop deliverable is a fat Java JAR, not a native `.exe`; matching Java architecture is needed. A 32-bit launch still requires a separate real-platform test.
- The Gradle scripts in the ZIP are transparent bootstrap scripts rather than the binary Gradle Wrapper JAR. They use an installed matching Gradle 8.13 when available; otherwise they try to download the pinned official distribution.

The dependency versions and build configuration were **not resolved or built end-to-end locally**. The first GitHub Actions run is required to verify dependency resolution, JUnit, desktop packaging, and Android packaging. This report makes no claim that a desktop JAR or APK has already been built.
