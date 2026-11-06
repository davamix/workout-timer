---
name: tablet
description: Build, install, launch, screenshot and read logs of the Workout Timer app on the HUAWEI tablet over adb, with ready-made Bash helpers (helpers.sh); read the text on screen without a screenshot, wait for a text or a log line with a deadline, tap by text, start a workout; run instrumented tests (one, several or all) without wiping the app's data, and summarise the results; check every state of a screen as screenshots; check that the screen stays on; end the app's process as Android does; load a test workout without the UI; measure device behaviour with a throwaway probe; read the app's private files; reboot the tablet. Use whenever the app needs to run on the device, be checked visually, be measured, or be debugged on the device, including instrumented tests and manual checks.
---

# Work with the tablet

## Device

| | |
|---|---|
| Model | HUAWEI MediaPad T5 10 (`AGS2-W09`) |
| OS | Android 8.0, API 26, EMUI 8 |
| Screen | 1920 × 1200 px in landscape (960 × 600 dp) |
| App content area | 960 × 542 dp: below a 24 dp status bar and above a 33 dp navigation bar (P7 T1). Every screen must fit in it. |
| System language | Spanish |
| TTS engine | Google Speech Services, package `com.google.android.tts` |
| TTS voices installed | Spanish (Spain) and English (United States) |
| Keyboard | Microsoft SwiftKey (`com.touchtype.swiftkey`). Not full screen in landscape. The letter keyboard is 372 dp tall, which leaves 204 dp above it; the number pad is about 210 dp. `MainActivity` uses `adjustResize`, so the window shrinks above it (P6 SPEC §4.3.6). |
| Connection | USB |

## adb

`adb` is not on PATH.

- Git Bash: `/d/Android/Sdk/platform-tools/adb.exe`
- PowerShell: `D:\Android\Sdk\platform-tools\adb.exe`

In Git Bash, always `export MSYS_NO_PATHCONV=1` before any adb command that takes a device path such as `/sdcard/...`. Otherwise Git Bash rewrites the path into a Windows path and the command fails. The helpers below do it.

Check the connection with `adb devices -l`. The tablet must be listed as `device`. If it shows as `unauthorized`, ask the user to accept the USB-debugging prompt on the tablet.

## Package names

- Debug build: `io.github.davamix.workouttimer.debug`
- Release build: `io.github.davamix.workouttimer`
- Activity, both builds: `io.github.davamix.workouttimer.MainActivity`
- Audio debug screen, debug build only (P5): `io.github.davamix.workouttimer.audiodebug.AudioDebugActivity`, with its own launcher icon, "WT audio debug"
- Instrumented tests (debug): `io.github.davamix.workouttimer.debug.test`, runner `androidx.test.runner.AndroidJUnitRunner`. `adb shell pm list instrumentation` confirms it.

## Helpers

[helpers.sh](helpers.sh) holds the Bash functions used most, for the debug build. Load it at the start of every Git Bash call that uses them, from the repo root; functions don't carry over between calls:

```bash
source .claude/skills/tablet/helpers.sh
```

It sets `$adb`, `$pkg` (the debug package), `$release_pkg`, `$activity` and `$runner`. The helpers read `$pkg` when they run, so `pkg=$release_pkg` after loading the file points them at the release app. `end_process`, `instrument` and `run-as` only work on the debug app. It defines:

| Function | What it does |
|---|---|
| `gradle <tasks>` | Runs `./gradlew` quietly, with the output in `build/gradle.log`. On failure it prints the errors. It returns Gradle's own exit code, so `gradle … && …` stops when the build fails. |
| `instrument [classes] [-e key value]` | Runs instrumented tests and keeps the app's data: the whole suite with no class; or classes and methods separated by commas (`ui.runner.RunnerScreenTest#name,…`), with the package prefix optional. |
| `texts` | Every text on screen, on one line |
| `wait_for <pattern> [seconds]` | Waits until a text matching the grep pattern is on screen (default 30 s). Fails with a message on timeout. |
| `tap_text <text> [last]` | Taps the node with exactly that text. `last` picks the last match, such as a dialog's Stop over the screen's Stop. |
| `launch` | Opens the app as its launcher icon does: an existing task comes to the front as it was |
| `stop_app [cold]` | Force-stops the app; `cold` also stops the speech engine |
| `shot <file>` | Saves a screenshot to a local file, to look at with Read |
| `start_workout <name>` | Presses Start on that workout in the list |
| `end_process` | Ends the app's process as Android does to free memory (`EX-11`); see below |
| `keeps_screen_on` | `yes` if the app's window has `FLAG_KEEP_SCREEN_ON` (`NF-2`), `no` otherwise |
| `time_run [slow_seconds] [timeout_seconds]` | Times a workout from outside the app, on the PC's clock (`NF-1`): prints `on`, `off` and `span` in seconds, from the runner's `FLAG_KEEP_SCREEN_ON`. See "Common tasks". |
| `wakefulness` | `Awake`, or `Asleep` or `Dozing` when the screen is off |
| `screen_timeout <millis> \| never` | Sets the screen timeout and prints it. Ask the user first, and put it back with `never`. |
| `wait_for_log <tag> <regex> [seconds]` | Waits until a log line from that tag matches (default 60 s). Clear the log first with `$adb logcat -c`. |

For example, start a workout, check that it keeps the screen on, pause it with Back and read the screen:

```bash
source .claude/skills/tablet/helpers.sh
stop_app; launch; wait_for 'text="Example"' 60 && start_workout Example && wait_for 'GET READY' 20
keeps_screen_on; $adb shell input keyevent KEYCODE_BACK; wait_for PAUSED 10 && texts
```

The first launch after installing can take over a minute (see "Notes"), so give it a longer deadline, such as `wait_for 'Workouts' 120`.

**Never pipe Gradle into `grep` or `tail` before `&&`.** The pipe returns the last command's code, so `./gradlew … | tail && adb shell am instrument …` runs the old test APK after a failed build. A new test class then fails with a misleading `ClassNotFoundException`. `set -o pipefail` doesn't help either: `grep -v` returns 1 when it has nothing left to print, which looks like a failed build. Use `gradle`, or write the output to a file and check `$?`.

## Common tasks

Gradle finds adb by itself, so the Gradle tasks need no path.

| Task | Command |
|---|---|
| Build and install | `gradle installDebug` |
| Instrumented tests, uninstalling afterwards | `./gradlew connectedDebugAndroidTest`. It uninstalls the app when it ends, which also deletes its data, such as workouts the user made, so run `installDebug` again to keep using it. Results: "Instrumented test results" below. |
| Instrumented tests, keeping the app's data | `gradle installDebug installDebugAndroidTest && instrument` runs the whole suite on the installed app, and `instrument <classes>` runs some. It passes when it ends with `OK (N tests)`. Read the summary rather than grepping for `OK`: a filter hides errors such as `no devices/emulators found`. The raw command is `adb shell am instrument -w [-e class <full class>[#method][,…]] [-e <arg> <value>] $runner`. |
| Load a workout for a visual check, without the UI | P3's `PersistenceCheck`: `instrument data.PersistenceCheck#seed -e persistenceCheck seed`. It saves "P3 persistence check" (two rounds, total 4:08), and **also turns voice cues off**. Afterwards, run it again with `clean` in both places: it deletes the workout and turns voice cues back on. |
| Read the app's private files (debug build only) | `$adb shell run-as $pkg ls -la files/workouts`, or `… cat files/workouts/<id>.json`. Settings are in `files/datastore/settings.preferences_pb`, a binary file. To copy a binary file to the PC, use `exec-out`, which keeps the bytes: `$adb exec-out run-as $pkg cat <path> > <local file>`. |
| Launch and time it | `$adb shell am start -W -n $pkg/$activity` |
| Launch the audio debug screen | `$adb shell am start -n $pkg/io.github.davamix.workouttimer.audiodebug.AudioDebugActivity` |
| Read the text on screen | `texts`. It lists every text shown, Compose's included, using far less context than a screenshot. Prefer it when only the words matter. **It can't read a running workout:** the time changes every second, so uiautomator never sees the screen idle, and the dump fails. Read it while paused, or use `shot`. |
| Stop | `stop_app` |
| Cold start of the speech engine | `stop_app cold`. The next connection then takes ~3 s, and the speaker's whole `prepare()` 9–10 s. |
| Battery level | `$adb shell dumpsys battery \| grep -e level -e powered`. Check it before a long manual run, such as a full workout. |
| Screenshot | `shot <scratchpad>/wt.png`, then Read the PNG to look at it |
| App logs | `$adb logcat -d --pid=$($adb shell pidof -s $pkg) \| tail -200`. Clear the log first with `$adb logcat -c`. |
| Crash logs | `$adb logcat -d -b crash \| tail -100` |
| Send the app to the background (`EX-10`) | `$adb shell input keyevent KEYCODE_HOME`. Recents is `KEYCODE_APP_SWITCH`, and the power button `KEYCODE_POWER`, then `KEYCODE_WAKEUP` to turn the screen back on. All three stop the activity (P7 T1). |
| Simulate Android closing the app (`EX-11`) | `end_process`, then `launch`. `am kill` doesn't end the process on this tablet, even when it's cached. `end_process` presses Home, opens Settings and presses Home again, so the app is no longer the "previous" one. Then it sends SIGKILL through `run-as`. Logcat shows "Process … has died: cch CAC", as for a low-memory kill. The task stays in Recents, and reopening it rebuilds the activity from its saved state (P7 T1). |
| Check that the screen stays on (`NF-2`) | `keeps_screen_on` reads the window's flag. To see it really work, ask the user first, then set `screen_timeout 30000`, wait over a minute on the screen, and check that `wakefulness` is still `Awake`. Then check that a screen that doesn't keep it on goes `Asleep` after 30 s. Put it back with `screen_timeout never`. Touching the tablet, or `input` events, restart the timeout. |
| Time a workout from outside the app (`NF-1`) | `time_run > <file> &`, then `start_workout <name>`, then `wait`. It works on release builds, which have no logs. The span also holds fixed costs: preparing the audio, "Get ready in", and the move to the completed screen. Measure them as `span − planned` on a 60 s workout with the same voice setting, and subtract them. Numbers from P8 T1, debug build: 2.20 s with voice on, 0.98 s with voice off, each within ±0.07 s. Taps take 0.75 s ± 0.03 s, so the time between a Pause and a Resume tap is accurate to about ±0.15 s. Pause is at (960, 1012) and Resume at (818, 1013). |
| Tap | `tap_text <text>`, or `$adb shell input tap <x> <y>` with pixel coordinates from a screenshot |

## Instrumented test results

After `./gradlew connectedDebugAndroidTest`, read the results from the XML instead of the Gradle output. The totals, then one line per failing test with its class, name and the first 300 characters of its message:

```bash
f=$(ls app/build/outputs/androidTest-results/connected/debug/*.xml)
grep -o '<testsuites tests="[0-9]*" failures="[0-9]*" errors="[0-9]*" skipped="[0-9]*"' "$f" | sed 's/<testsuites //'
awk '/<testcase /{match($0,/ name="[^"]*"/); n=substr($0,RSTART+7,RLENGTH-8); match($0,/classname="[^"]*"/); c=substr($0,RSTART+11,RLENGTH-12); sub(/.*\./,"",c)} /<(failure|error)/{l=$0; sub(/.*<(failure|error)[^>]*>/,"",l); if (l ~ /^[[:space:]]*$/) getline l; print c" > "n"\n    "substr(l,1,300)}' "$f"
```

`instrument` prints its own summary instead: one line per class with a dot per test, then each failure with its message, and `OK (N tests)` or `FAILURES!!!`.

P3's `PersistenceCheck` has 3 tests that are always skipped: they only run by hand.

## Checking a screen in every state

To see how a screen looks in each of its states, draw them in a throwaway test rather than driving the app to each one. In P7 this caught a layout that didn't fit, and a text colour bug, before any manual check.

1. Write a JUnit 4 test in `app/src/androidTest/kotlin/io/github/davamix/workouttimer/probe/<Name>Gallery.kt` with `createComposeRule()`. Hold the state in a `mutableStateOf`, and set the content once, inside `WorkoutTimerTheme`.
2. For each state: set it, call `compose.waitForIdle()`, then save `compose.onRoot().captureToImage().asAndroidBitmap()` as a PNG in `targetContext.cacheDir`.
3. Run it with `gradle installDebug installDebugAndroidTest && instrument probe.<Name>Gallery`.
4. Pull each file with `$adb exec-out run-as $pkg cat cache/<file>.png > <scratchpad>/<file>.png`, and look at it with Read.
5. Delete the test: it never goes in a commit. The PNGs can stay in the app's cache until the next uninstall.

Pictures come at the content area's size, 1920 × 1084 px. **Dialogs can't be captured below API 28** (`captureToImage` throws), so check a dialog on the device with `shot`.

## Measuring device behaviour

When a design depends on how the tablet behaves (timing, latency, what Android does in the background), measure it with a throwaway probe before relying on it:

1. Write a JUnit 4 test in `app/src/androidTest/kotlin/io/github/davamix/workouttimer/probe/<Name>Probe.kt`. It logs what it measures with `Log.i("<Name>Probe", …)`, using `SystemClock.elapsedRealtime()` for times.
   - To follow the activity's lifecycle, register `Application.ActivityLifecycleCallbacks` on the target application, and start `MainActivity` with the launcher intent (`ACTION_MAIN`, `CATEGORY_LAUNCHER`, `FLAG_ACTIVITY_NEW_TASK`). `launch` then brings that same task back after Home, as the launcher icon would.
   - While the probe waits, adb can act on the device, and the user can turn the tablet. Mark each step in the same log with `$adb shell log -p i -t <Name>Probe '"MARK <step>"'`.
   - A probe can't measure the app's process ending: the probe runs inside that process. Use `end_process` on the installed app instead.
2. Run `gradle installDebug installDebugAndroidTest`. Clear the log with `$adb logcat -c`, then run the probe with `instrument probe.<Name>Probe`, in the background if it waits for actions.
3. Wait for the probe's lines with `wait_for_log <Name>Probe '<regex>'` rather than sleeping, and read the results with `$adb logcat -d -s <Name>Probe:I`.
4. Repeat each measurement 2–3 times. For a cold start, force-stop whatever must be cold first, for example with `stop_app cold`.
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
- **Orientation:** don't change the tablet's rotation settings. A person has to turn the tablet to check how the app follows it. Turning it over (180°) only changes the display's rotation: the activity gets no callback and isn't recreated (P7 T1).
- **Screen timeout:** set to never (`settings get system screen_off_timeout` gives 2147483647). Checking that the app keeps the screen on needs a shorter timeout for a while; ask the user before changing it, and put it back afterwards (`screen_timeout`).
- **No lock screen:** after the power button, waking the tablet resumes the app directly.
- **Audio:** cues can't be checked through adb. Ask the user to listen, and record the result in the handover.
- **Waiting:** a foreground `sleep` is blocked. Wait for a state with `wait_for` or `wait_for_log`; a short pause between adb steps can run on the device, with `$adb shell sleep 2`.
- **Removing files on the device:** `run-as … sh -c 'rm …'` is refused by Claude Code's safety check, because it can't read the script. Leave throwaway files in the app's cache; the next uninstall removes them.
- **`am instrument` output** has `\r\n` line endings and ends with blank lines, so `tail -2` shows nothing useful. `instrument` cleans it up; by hand, pipe it through `tr -d '\r' | grep -v '^$'`.
