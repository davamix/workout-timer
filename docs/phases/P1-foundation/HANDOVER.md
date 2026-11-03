# P1 — Project foundation: Handover

| | |
|---|---|
| **Status** | In progress: implementation complete; waiting for the pull request review and two manual checks by the user |
| **Spec** | [SPEC.md](SPEC.md), version v0.1 |
| **Branch** | `phase/p1-foundation` |
| **Last session** | 2026-11-02 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Current state

All tasks are implemented, and the skeleton runs on the tablet:

- `./gradlew build` passes locally, with 0 lint issues and ktlint clean.
- The 6 instrumented tests pass on the tablet.
- The app opens in landscape, and the placeholder screens and navigation work.
- CI passes on pull request [#1](https://github.com/davamix/workout-timer/pull/1) (3m30s).

Two manual checks need a person at the tablet: the launcher icon, and turning the tablet upside down. See Next steps.

## Next steps

1. ~~Wait for CI to pass on the pull request.~~ Done: CI is green.
2. The user checks on the tablet:
   - the "Workout Timer (debug)" icon in the launcher;
   - turning the tablet upside down → the app follows; holding it upright → it stays landscape.
3. The user reviews and merges the pull request.
4. After the merge, set P1 to `Done` in plan §5.1, on `main`.
5. Then write the P2 documents (skill `phase-docs`).

## Tasks

| Task | Status | Notes |
|---|---|---|
| T1 | Done | Gradle 9.8.2 wrapper (with checksum), catalog, `.editorconfig`, `.gitattributes` |
| T2 | Done | `:core` has no sources yet; JUnit 6 is configured |
| T3 | Done | |
| T4 | Done | |
| T5 | Done | No-INTERNET check verified: it fails when the permission is added |
| T6 | Done | 6/6 pass on AGS2-W09 (Android 8.0) |
| T7 | Done | `.github/workflows/ci.yml` |
| T8 | Done | `CLAUDE.md`, `.claude/skills/tablet/SKILL.md` |
| T9 | Doing | Pull request #1 open, CI green. Manual checks 1 and 2 are pending the user (§Next steps). |

## Decisions and deviations

| Date | Decision | Reason |
|---|---|---|
| 2026-11-02 | Added `res/xml/backup_rules.xml` and `android:fullBackupContent`, which weren't in the SPEC file list. | Lint `DataExtractionRules`: Android 8–11 ignore `dataExtractionRules`. Backup is off anyway; this keeps the rules explicit. |
| 2026-11-02 | Espresso core (3.7.0) added to the instrumented test dependencies. | `pressBack()` in the smoke test |
| 2026-11-02 | The smoke test uses `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`. | The v1 rule is deprecated in Compose BOM 2026.09.00 |
| 2026-11-02 | `gradlew` is marked executable in git (`git update-index --chmod=+x`). | Windows doesn't record the bit, and CI on Linux needs it. |
| 2026-11-02 | `.kotlin/` added to `.gitignore`. | Kotlin 2.x stores compiler session data there |

## Known issues

- **Gradle 10 deprecation notice:** Gradle prints "Deprecated Gradle features were used… incompatible with Gradle 10". It comes from AGP 9.3.3 itself (`Configuration.setVisible`), not our scripts, and should disappear with a newer AGP.
- **`sun.misc.Unsafe` warnings:** ktlint prints them on JDK 25, from its embedded Kotlin compiler. They are harmless.
- **Slow first launch:** the first launch after installing takes about 29 s on the tablet, while Android 8 compiles the debug APK. Later cold starts take about 1.8 s.

## Pending wiring

None.

## How to verify

```
./gradlew build                        # compile, lint, ktlint, no-INTERNET check
./gradlew connectedDebugAndroidTest    # 6 smoke tests on the tablet
./gradlew installDebug                 # then open "Workout Timer (debug)" on the tablet
```

## Session log

Newest first.

### 2026-11-02

- **Done:** T1–T8 and most of T9. Spec v0.5 and plan v0.2 were updated before starting (PQ answers).
- **Versions:**
  - AGP 9.3.3, the newest the installed Android Studio 2026.1.3 supports (9.4 and 9.5 exist);
  - Gradle 9.8.2, Kotlin 2.4.21, Compose BOM 2026.09.00;
  - `compileSdk` and `targetSdk` 37, `minSdk` 26.
- **Notes:**
  - In Git Bash, adb commands that take device paths need `MSYS_NO_PATHCONV=1`. This is in the `tablet` skill.
  - `connectedDebugAndroidTest` uninstalls the app afterwards, so reinstall with `installDebug`.
