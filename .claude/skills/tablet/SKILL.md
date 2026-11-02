---
name: tablet
description: Build, install, launch, screenshot and read logs of the Workout Timer app on the HUAWEI tablet over adb. Use whenever the app needs to run on the device, be checked visually, or be debugged on the device, including instrumented tests.
---

# Work with the tablet

## Device

| | |
|---|---|
| Model | HUAWEI MediaPad T5 10 (`AGS2-W09`) |
| OS | Android 8.0, API 26, EMUI 8 |
| Screen | 1920 × 1200 px in landscape (960 × 600 dp) |
| System language | Spanish |
| TTS voices installed | Spanish (Spain) and English (United States) |
| Connection | USB |

## adb

`adb` is not on PATH.

- Git Bash: `/d/Android/Sdk/platform-tools/adb.exe`
- PowerShell: `D:\Android\Sdk\platform-tools\adb.exe`

In Git Bash, always `export MSYS_NO_PATHCONV=1` before any adb command that takes a device path such as `/sdcard/...`. Otherwise Git Bash rewrites the path into a Windows path and the command fails.

Check the connection with `adb devices -l`. The tablet must be listed as `device`. If it shows as `unauthorized`, ask the user to accept the USB-debugging prompt on the tablet.

## Package names

- Debug build: `io.github.davamix.workouttimer.debug`
- Release build: `io.github.davamix.workouttimer`
- Activity, both builds: `io.github.davamix.workouttimer.MainActivity`

## Common tasks

Gradle finds adb by itself, so the Gradle tasks need no path.

| Task | Command |
|---|---|
| Build and install | `./gradlew installDebug` |
| Instrumented tests | `./gradlew connectedDebugAndroidTest`. This uninstalls the app afterwards, so run `installDebug` again to keep using it. |
| Launch and time it | `adb shell am start -W -n io.github.davamix.workouttimer.debug/io.github.davamix.workouttimer.MainActivity` |
| Stop | `adb shell am force-stop io.github.davamix.workouttimer.debug` |
| Screenshot | `adb shell screencap -p /sdcard/wt.png && adb pull /sdcard/wt.png <scratchpad>/wt.png && adb shell rm /sdcard/wt.png`, then Read the PNG to look at it |
| App logs | `adb logcat -d --pid=$(adb shell pidof -s io.github.davamix.workouttimer.debug) \| tail -200`. Clear the log first with `adb logcat -c`. |
| Crash logs | `adb logcat -d -b crash \| tail -100` |
| Send the app to the background (`EX-10`) | `adb shell input keyevent KEYCODE_HOME` |
| Simulate Android closing the app (`EX-11`) | Background the app, then run `adb shell am kill io.github.davamix.workouttimer.debug` |
| Tap a point | `adb shell input tap <x> <y>`, using pixel coordinates from a screenshot |

## Notes

- **Slow first launch:** the first launch after installing can take about 30 s, because Android 8 compiles the debug APK on the device. Later cold starts take about 2 s. Measure timing on the second launch.
- **Orientation:** don't change the tablet's rotation settings. A person has to turn the tablet to check how the app follows it.
- **Audio:** cues can't be checked through adb. Ask the user to listen, and record the result in the handover.
