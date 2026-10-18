# BlockVerse — Stage 1 prototype

BlockVerse is an early, offline 3D voxel sandbox prototype written in Java with libGDX and Gradle. The goal of this stage is a small playable foundation, not a Roblox clone with online services. Accounts, a creator studio, a game catalogue, and multiplayer are intentionally **not implemented**.

## What is implemented

- 3D block world with grass, dirt, stone, wood, leaves, and brick.
- Simple blocky humanoid, third-person follow camera, keyboard/mouse controls, gravity, jump, and voxel collision.
- Four-slot block selection; break/place blocks from the crosshair.
- Android touch controls: virtual joystick, look drag, jump, break, and place buttons.
- Local world save/load (`saves/world-1.bvw`) and graphics quality preference.
- Main menu, pause menu, and graphics setting.
- Gradle modules for shared core, desktop (LWJGL3), and Android.
- JUnit world-model tests plus a dependency-free smoke test.

This is deliberately lightweight physics, not a full rigid-body physics engine. There are no textures, audio, authentication, remote database, publishing service, catalogue, or multiplayer transport in this stage.

## Requirements

- JDK 17 recommended.
- Internet access on first Gradle run, because the Gradle distribution and Maven dependencies must be downloaded.
- For Android builds: Android SDK Platform 36 and Build Tools 35.0.0 (the AGP 8.13 default). Android Studio can install these from SDK Manager.
- Git for source control and GitHub CLI (`gh`) for convenient repository creation and Actions access.

## Run on desktop

Linux/macOS:

```sh
./gradlew :lwjgl3:run
```

Windows:

```bat
gradlew.bat :lwjgl3:run
```

The launcher opens a 1280×720 resizable window. Press **Play** on the main menu.

### Controls

| Action | Desktop | Android |
|---|---|---|
| Move | W/A/S/D or arrow keys | Virtual joystick |
| Jump | Space | Jump button |
| Look around | Captured mouse | Drag on the right side |
| Remove block | Left-click | Break button |
| Place selected block | Right-click | Place button |
| Select slots 1–4 | Number keys 1–4 | Tap a slot in the bottom inventory bar |
| Save / load | F5 / F9, or Pause menu | Pause menu |
| Pause / resume | Esc | Back button or pause/menu action |
| Zoom camera | Mouse wheel | Not implemented |

Desktop first click on **Play** captures the cursor. Press Esc to open the pause menu and release the cursor.

## Save files

On desktop, the save is written relative to the desktop launch working directory (the `assets/` folder when launched by the Gradle task), so the default path is `assets/saves/world-1.bvw`. On Android, libGDX local storage maps to the app's private local directory. The save format is text-based and begins with `BLOCKVERSE_WORLD 1`.

## Build and test

```sh
# Shared-core unit tests and desktop distribution
./gradlew :core:test :lwjgl3:dist

# Android debug APK containing both ARM ABIs
./gradlew :android:assembleDebug

# Dependency-free world/physics smoke test (works with javac only)
./scripts/smoke-test.sh
```

Desktop JAR output: `lwjgl3/build/libs/BlockVerse-desktop-0.1.0.jar`.

Android APK output: `android/build/outputs/apk/debug/android-debug.apk` (the exact filename can differ if Android Gradle Plugin naming changes). The APK is configured to include **armeabi-v7a (ARM 32-bit)** and **arm64-v8a (ARM 64-bit)** native libraries in one APK.

The Gradle launcher scripts in this source ZIP are small bootstrap scripts rather than the binary Gradle Wrapper JAR. They use an installed Gradle 8.13 if available, or download the pinned official Gradle 8.13 distribution on first use. If your team prefers the standard wrapper JAR checked into Git, install/run Gradle once and generate it with:

```sh
gradle wrapper --gradle-version 8.13 --distribution-type bin
git add gradlew gradlew.bat gradle/wrapper
```

Gradle 8.13 is used with Android Gradle Plugin 8.13.2, Android compile/target SDK 36, Build Tools 35.0.0, and JDK 17 in CI. The CI workflow is the authoritative full build check; see `BUILD_REPORT.md` for what was actually tested while creating this archive.

## Git + GitHub CLI: create a repository and push

Install Git and GitHub CLI first, then from the project folder:

```sh
git init -b main
git add .
git commit -m "Initial BlockVerse stage 1"
gh auth login
gh repo create blockverse --public --source=. --remote=origin --push
```

For a private repository, replace `--public` with `--private`. If Git reports missing author identity, set your own identity before committing:

```sh
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

After pushing, open the repository on GitHub and choose **Actions**. The `BlockVerse CI` workflow runs the unit tests, builds the desktop distribution, and attempts an Android ARM debug APK build. Download the generated files from the **Artifacts** section of the completed workflow run. Failed jobs do not produce a verified artifact. You can also use `gh` after logging in:

```sh
gh run list --workflow .github/workflows/ci.yml
gh run download RUN_ID --name BlockVerse-desktop-jar --dir ./artifacts
gh run download RUN_ID --name BlockVerse-android-arm32-arm64-debug --dir ./artifacts
```

Replace `RUN_ID` with an ID returned by `gh run list`. Download each artifact name only after its workflow job succeeds.

## Platform notes (be precise)

- **Android ARM32 + ARM64:** Gradle's ABI filter and native classifiers are configured for `armeabi-v7a` and `arm64-v8a`. An APK is not claimed to have built until the Android job passes.
- **Windows x64 + x86:** the desktop runtime configuration includes the LWJGL Windows x64 and x86 native classifiers. The output is a runnable Java JAR, not a native `.exe`; users need a matching Java runtime architecture. The 32-bit launch path still needs to be tested on a real 32-bit Java/Windows environment before calling it fully verified.
- **Desktop testing:** actual graphical run requires a display/OpenGL-capable host. CI compile/package is not the same as a visual gameplay test.

## Roadmap (not in this stage)

1. Stabilize and test the local game foundation.
2. Add an online account service and database.
3. Build BlockVerse Studio for Windows and online publishing.
4. Add an online game catalogue.
5. Implement client/server multiplayer after the previous systems are stable.

Do not treat these planned steps as existing features.
