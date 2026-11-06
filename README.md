# Workout Timer

An Android interval timer for round-based workouts, built for personal use on a 10" tablet running Android 8.0.

A workout is made of rounds. Each round is an ordered list of timed exercises, with configurable rests between exercises and between rounds. Spoken cues from the device's built-in text-to-speech announce each exercise and count down its last three seconds.

The app works offline, with no accounts and no analytics. Workouts are stored only on the device, so uninstalling the app deletes them.

## Status

Version 1.0.0 is finished. It was built for and tested on one device, a HUAWEI MediaPad T5 10 (Android 8.0, landscape, 1920 × 1200), so other screens may not suit it.

- [Releases](https://github.com/davamix/workout-timer/releases): the signed APK.
- [Specification](docs/SPECIFICATION.md): what the app does.
- [Implementation plan](docs/IMPLEMENTATION_PLAN.md): how it is built, in phases.
- [Acceptance record](docs/phases/P8-integration/ACCEPTANCE.md): how each requirement was checked on the tablet.

## Installing

The app isn't in any store. You need [adb](https://developer.android.com/tools/adb), and USB debugging turned on on the device. Download the APK from [Releases](https://github.com/davamix/workout-timer/releases), then run:

```bash
adb install workout-timer-1.0.0.apk
```

To update the app, run `adb install -r` with the new APK. This keeps your workouts, as long as the new APK is signed with the same key.

The cues always use the English (United States) voice, so it must be installed in the device's text-to-speech settings.

## Building

You need Android Studio 2026.1.3 or later. Gradle uses its bundled JDK, so point `JAVA_HOME` at it.

| Task | Command |
|---|---|
| Build, run unit tests and all checks | `./gradlew build` |
| Install a debug build on a connected device | `./gradlew installDebug` |
| Run instrumented tests on the device | `./gradlew connectedDebugAndroidTest` (uninstalls the debug build afterwards, which deletes its workouts) |
| Build a release APK | `./gradlew assembleRelease` |

The debug build has its own application ID, so it installs next to a release build, and each keeps its own workouts.

### Signing a release

Without the signing properties below, `assembleRelease` builds an unsigned APK, which Android won't install. To sign it:

1. Create a keystore outside the repo. `keytool` comes with the JDK, and it asks for the passwords:

   ```bash
   keytool -genkeypair -v -keystore <path>/workout-timer.keystore -alias <alias> -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Add these four properties to your user-level `~/.gradle/gradle.properties`, never to the repo's own file. A `\` is an escape character there, so write the path with forward slashes, and don't use one in a password.

   ```properties
   workoutTimerKeystoreFile=<path>/workout-timer.keystore
   workoutTimerKeystorePassword=<keystore password>
   workoutTimerKeyAlias=<alias>
   workoutTimerKeyPassword=<key password>
   ```

3. Run `./gradlew assembleRelease`. The APK is at `app/build/outputs/apk/release/app-release.apk`.
4. Check its signature with `apksigner verify --print-certs <apk>`. `apksigner` is in the Android SDK's `build-tools` folder.

**Keep a copy of the keystore and its passwords outside this PC.** Android only installs an update signed with the same key. Without the key, updating means uninstalling first, which deletes every workout.
