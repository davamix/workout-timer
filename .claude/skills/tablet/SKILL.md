---
name: tablet
description: Build, install, launch, screenshot and read logs of the Workout Timer app on the HUAWEI tablet over adb; read the text on screen without a screenshot, and wait for a text with a deadline; load a test workout without the UI; run one instrumented test without wiping the app's data, and summarise the results; measure device behaviour with a throwaway probe; read the app's private files; reboot the tablet. Use whenever the app needs to run on the device, be checked visually, be measured, or be debugged on the device, including instrumented tests.
---

# Work with the tablet

## Device

| | |
|---|---|
| Model | HUAWEI MediaPad T5 10 (`AGS2-W09`) |
| OS | Android 8.0, API 26, EMUI 8 |
| Screen | 1920 × 1200 px in landscape (960 × 600 dp) |
| System language | Spanish |
| TTS engine | Google Speech Services, package `com.google.android.tts` |
| TTS voices installed | Spanish (Spain) and English (United States) |
| Keyboard | Microsoft SwiftKey (`com.touchtype.swiftkey`). Not full screen in landscape. The letter keyboard is 372 dp tall, which leaves 204 dp above it; the number pad is about 210 dp. `MainActivity` uses `adjustResize`, so the window shrinks above it (P6 SPEC §4.3.6). |
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
- Audio debug screen, debug build only (P5): `io.github.davamix.workouttimer.audiodebug.AudioDebugActivity`, with its own launcher icon, "WT audio debug"
- Instrumented tests (debug): `io.github.davamix.workouttimer.debug.test`, runner `androidx.test.runner.AndroidJUnitRunner`. `adb shell pm list instrumentation` confirms it.

## Common tasks

Gradle finds adb by itself, so the Gradle tasks need no path.

| Task | Command |
|---|---|
| Build and install | `./gradlew installDebug` |
| Instrumented tests | `./gradlew connectedDebugAndroidTest`. This uninstalls the app afterwards, which also deletes its data, so run `installDebug` again to keep using it. |
| Run one instrumented test and keep the app's data | `./gradlew installDebug installDebugAndroidTest`, then `adb shell am instrument -w [-e <arg> <value>] -e class io.github.davamix.workouttimer.<package>.<Class>#<method> io.github.davamix.workouttimer.debug.test/androidx.test.runner.AndroidJUnitRunner \| tail -8`. It passes when it ends with `OK (1 test)`. Read the tail rather than grepping for `OK`: a filter hides errors such as `no devices/emulators found`. Chain the build and the run with `&&`: if the build fails, `am instrument` runs the test APK installed before, and a new test class then fails with a misleading `ClassNotFoundException`. |
| Load a workout for a visual check, without the UI | P3's `PersistenceCheck`: `adb shell am instrument -w -e persistenceCheck seed -e class io.github.davamix.workouttimer.data.PersistenceCheck#seed io.github.davamix.workouttimer.debug.test/androidx.test.runner.AndroidJUnitRunner`. It saves "P3 persistence check" (two rounds, total 4:08), and **also turns voice cues off**. Afterwards, run the same command with `clean` in both places: it deletes the workout and turns voice cues back on. |
| Results of the instrumented tests | See "Instrumented test results" below. |
| Read the app's private files (debug build only) | `adb shell run-as io.github.davamix.workouttimer.debug ls -la files/workouts`, or `… cat files/workouts/<id>.json`. Settings are in `files/datastore/settings.preferences_pb`, a binary file. |
| Launch and time it | `adb shell am start -W -n io.github.davamix.workouttimer.debug/io.github.davamix.workouttimer.MainActivity` |
| Launch the audio debug screen | `adb shell am start -n io.github.davamix.workouttimer.debug/io.github.davamix.workouttimer.audiodebug.AudioDebugActivity` |
| Read the text on screen | `adb exec-out uiautomator dump /dev/tty \| grep -o 'text="[^"]\+"'`. It lists every text shown, Compose's included, using far less context than a screenshot. Prefer it when only the words matter. To wait for a text, use `wait_for` in "Waiting for a text on screen" below. |
| Stop | `adb shell am force-stop io.github.davamix.workouttimer.debug` |
| Cold start of the speech engine | `adb shell am force-stop com.google.android.tts`, and stop the app. The next connection then takes ~3 s, and the speaker's whole `prepare()` 9–10 s. |
| Battery level | `adb shell dumpsys battery \| grep -e level -e powered`. Check it before a long manual run, such as a full workout. |
| Screenshot | `adb shell screencap -p /sdcard/wt.png && adb pull /sdcard/wt.png <scratchpad>/wt.png && adb shell rm /sdcard/wt.png`, then Read the PNG to look at it |
| App logs | `adb logcat -d --pid=$(adb shell pidof -s io.github.davamix.workouttimer.debug) \| tail -200`. Clear the log first with `adb logcat -c`. |
| Crash logs | `adb logcat -d -b crash \| tail -100` |
| Send the app to the background (`EX-10`) | `adb shell input keyevent KEYCODE_HOME` |
| Simulate Android closing the app (`EX-11`) | Background the app, then run `adb shell am kill io.github.davamix.workouttimer.debug` |
| Tap a point | `adb shell input tap <x> <y>`, using pixel coordinates from a screenshot |

## Waiting for a text on screen

A dump takes about 2 s, so a loop of dumps can wait for a state without sleeping: a screen opened, a total updated, `Speaker: READY`. Always give the loop a deadline. A bare `until …; do :; done` hangs until the tool call times out if the text never appears, for example after a crash.

```bash
export MSYS_NO_PATHCONV=1; adb=/d/Android/Sdk/platform-tools/adb.exe
# Waits until TEXT is on screen, for at most SECONDS (default 30). Fails with a message on timeout.
wait_for() { local end=$((SECONDS + ${2:-30})); until $adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep -q "$1"; do [ $SECONDS -ge $end ] && { echo "timeout: '$1' not on screen after ${2:-30} s"; return 1; }; done; }

# For example: tap New workout on the list, wait for the new workout's total, then take a screenshot.
$adb shell input tap 1600 111 && wait_for 'Total 0:43' && $adb shell screencap -p /sdcard/wt.png
```

Define it in the same Bash call that uses it: shell functions don't carry over between calls. The first launch after installing can take over a minute (see "Notes"), so give that one a longer deadline, such as `wait_for 'Workouts' 120`.

## Instrumented test results

After `./gradlew connectedDebugAndroidTest`, read the results from the XML instead of the Gradle output. The totals, then one line per failing test with its class, name and the first 300 characters of its message:

```bash
f=$(ls app/build/outputs/androidTest-results/connected/debug/*.xml)
grep -o '<testsuites tests="[0-9]*" failures="[0-9]*" errors="[0-9]*" skipped="[0-9]*"' "$f" | sed 's/<testsuites //'
awk '/<testcase /{match($0,/ name="[^"]*"/); n=substr($0,RSTART+7,RLENGTH-8); match($0,/classname="[^"]*"/); c=substr($0,RSTART+11,RLENGTH-12); sub(/.*\./,"",c)} /<(failure|error)/{l=$0; sub(/.*<(failure|error)[^>]*>/,"",l); if (l ~ /^[[:space:]]*$/) getline l; print c" > "n"\n    "substr(l,1,300)}' "$f"
```

P3's `PersistenceCheck` has 3 tests that are always skipped: they only run by hand.

## Measuring device behaviour

When a design depends on how the tablet behaves (timing, latency, what Android does in the background), measure it with a throwaway probe before relying on it:

1. Write a JUnit 4 test in `app/src/androidTest/kotlin/io/github/davamix/workouttimer/probe/<Name>Probe.kt`. It logs what it measures with `Log.i("<Name>Probe", …)`, using `SystemClock.elapsedRealtime()` for times.
2. Run `./gradlew installDebug installDebugAndroidTest`. Clear the log with `adb logcat -c`, then run the probe with `am instrument`, as in "Run one instrumented test".
3. Read the results with `adb logcat -d -s <Name>Probe:I`.
4. Repeat each measurement 2–3 times. For a cold start, force-stop whatever must be cold first, for example the app and the speech engine.
5. Record the numbers in the handover, or in the SPEC if the design depends on them. Then delete the probe: it never goes in a commit.

## Rebooting

A reboot needs the user at the tablet. Until the screen is unlocked once after boot, the app's private storage is locked, so anything that reads the app's data fails.

1. Ask the user first, and tell them they will need to unlock the tablet when it restarts.
2. Run `adb reboot`.
3. Wait in the background (`run_in_background`), not with a foreground sleep. This waits for the boot, then for the unlock, then for the USB reset that follows it:

   ```bash
   export MSYS_NO_PATHCONV=1; adb=/d/Android/Sdk/platform-tools/adb.exe
   sleep 15; $adb wait-for-device
   until [ "$($adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
   until $adb shell dumpsys window policy 2>/dev/null | grep -q 'isStatusBarKeyguard=false'; do sleep 3; done
   sleep 15; $adb wait-for-device; $adb shell uptime
   ```

4. **USB reset after unlocking:** after the first unlock, the tablet drops off `adb devices` for about 10 s while USB reconnects. The last line of the script waits for that. If a later command says `no devices/emulators found`, wait for the tablet to come back, and check with `adb shell uptime` that it didn't reboot again.

## Notes

- **Slow first launch:** the first launch after installing takes 30–75 s, because Android 8 compiles the debug APK on the device. Later cold starts take about 2 s. Measure timing on the second launch.
- **Orientation:** don't change the tablet's rotation settings. A person has to turn the tablet to check how the app follows it.
- **Audio:** cues can't be checked through adb. Ask the user to listen, and record the result in the handover. The audio debug screen runs a workout with its cues before the runner screen exists.
- **`am instrument` output** has `\r\n` line endings and ends with blank lines, so `tail -2` shows nothing useful. Use `tail -8`, or pipe it through `tr -d '\r' | grep -v '^$'`.
