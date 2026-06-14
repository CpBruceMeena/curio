# Installing Curio on an Android Emulator

## Prerequisites

- **Android Emulator** — running via Android Studio's AVD Manager or command line
- **ADB** (Android Debug Bridge) — included with the Android SDK
- **Debug APK** — built with `./gradlew assembleDebug` (see [BUILD.md](../BUILD.md))

## Step 1: Start the Emulator

```bash
# List available AVDs
emulator -list-avds

# Start one (e.g. "Pixel_6_API_36")
emulator -avd Pixel_6_API_36 &

# Or use Android Studio → Device Manager → Run
```

Wait for the emulator to fully boot. Check it's ready:

```bash
adb devices -l
```

Expected output:
```
emulator-5554  device product:sdk_gphone64_arm64 model:Pixel_6_API_36 ...
```

If the list is empty, restart ADB:
```bash
adb kill-server && adb start-server
```

## Step 2: Install the APK

```bash
# Fresh install
adb -s emulator-5554 install app/build/outputs/apk/debug/app-debug.apk

# Reinstall (replaces existing, preserves app data)
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

The `-r` flag reinstalls the app while preserving the existing user data.  
Omit `-s emulator-5554` if only one device/emulator is connected.

## Step 3: Launch the App

```bash
# Launch by package name
adb shell am start -n com.curio.app/.MainActivity
```

Or tap the Curio icon in the emulator's app drawer.

## Step 4: View Logs (Debugging)

```bash
# Live logcat filtered to the app
adb logcat --pid=$(adb shell pidof -s com.curio.app)

# Filter by log tag
adb logcat -s CurioApp

# Save logs to file
adb logcat -d > /tmp/curio_logs.txt
```

## Common Issues

| Symptom | Cause | Fix |
|---------|-------|-----|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Existing install has conflicting signature | `adb uninstall com.curio.app` then reinstall |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | Wrong APK architecture | Build with `./gradlew assembleDebug` (should target all ABIs) |
| `Error: Could not access the Package Manager` | Emulator not fully booted | Wait 30s, run `adb wait-for-device` |
| App opens then immediately closes | Missing permission or crash | Check `adb logcat -s AndroidRuntime` for stack trace |
| Network calls fail with `Connection refused` | Backend not running or ngrok not forwarding | Verify backend is running and ngrok URL is correct |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | Emulator disk full | Wipe emulator data: `emulator -avd Pixel_6_API_36 -wipe-data` |

## Quick One-liner (Build + Install)

```bash
cd android && ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Installing a Release APK

```bash
# Build release (signed with keystore)
cd android && ./gradlew assembleRelease

# Install on emulator or device
adb install -r app/build/outputs/apk/release/app-release.apk
```

See [BUILD.md](../BUILD.md) for detailed build instructions and signing config.
