# Building & Installing Curio

## Prerequisites

- **Android Studio** (Hedgehog or later) or **Android SDK** installed
- **JDK 17+**
- Android SDK with **API 36** (`compileSdk = 36`) and **build-tools**
- An **AVD emulator** or physical device with **Android 8.0 (API 26) or higher**
- Gradle wrapper is checked into the repo — no manual Gradle install needed

## Creating a Debug Build

A debug APK is unsigned but sufficient for emulator testing.

```bash
cd android

# Build the debug APK
./gradlew assembleDebug

# File output:
# app/build/outputs/apk/debug/app-debug.apk
```

To speed up subsequent builds (skips the Gradle daemon startup):

```bash
# First build with daemon
./gradlew assembleDebug

# Subsequent builds reuse the daemon
```

## Creating a Release Build

A release APK is signed with the keystore (`android/curio-keystore.jks`) and uses
ProGuard/R8 for minification and resource shrinking.

```bash
cd android

# Build the release APK
./gradlew assembleRelease

# File output:
# app/build/outputs/apk/release/app-release.apk
```

### Release signing config

The signing config is in `android/app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("curio-keystore.jks")
        storePassword = "curio123"
        keyAlias = "curio"
        keyPassword = "curio123"
    }
}
```

> ⚠️ **Security note:** The keystore and its credentials are checked into the repo for
> development convenience. For a production release, store the keystore securely and
> inject credentials via environment variables or a CI secrets store.

### Clean build (start fresh)

```bash
./gradlew clean
./gradlew assembleDebug   # or assembleRelease
```

## Installing on an Emulator

### 1. Check running emulators

```bash
adb devices -l
```

You should see a device like `emulator-5554` in the list. If none appear, open Android
Studio → Device Manager → start an AVD.

### 2. Install the APK

```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

Flags:
- `-s emulator-5554` — target a specific device (omit if only one device is connected)
- `-r` — reinstall (replaces existing installation, preserving app data)

For a fresh install (wipes app data):

```bash
adb -s emulator-5554 install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Launch the app

```bash
# Launch by package name
adb -s emulator-5554 shell am start -n com.curio.app/.MainActivity
```

Or simply tap the app icon in the emulator.

### 4. View logs (debugging)

```bash
# Filter by app package
adb -s emulator-5554 logcat --pid=$(adb -s emulator-5554 shell pidof -s com.curio.app)

# Or filter by tag
adb -s emulator-5554 logcat -s CurioApp
```

## Build & Install One-liner

```bash
cd android && ./gradlew assembleDebug && adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Uninstall first: `adb uninstall com.curio.app` then reinstall |
| `SDK location not found` | Set `ANDROID_HOME` env var to your SDK path |
| `java.lang.OutOfMemoryError` | Increase JVM heap in `gradle.properties`: `org.gradle.jvmargs=-Xmx4096m` |
| ADB offline / no devices | Restart ADB server: `adb kill-server && adb start-server` |
| Gradle daemon memory | Run with `--no-daemon` to isolate builds |
