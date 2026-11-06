# Workout Timer 1.0.0: acceptance record

| | |
|---|---|
| **Status** | In progress |
| **Specification** | [SPECIFICATION.md](../../SPECIFICATION.md) v0.11 |
| **Scenarios** | [P8 SPEC §6](SPEC.md#6-testing) |
| **Device** | HUAWEI MediaPad T5 10 (`AGS2-W09`), Android 8.0 (API 26), system language Spanish |
| **Commit** | `56b5747` on `phase/p8-integration`. Later commits must leave the code unchanged, or the checks they affect are repeated. |
| **Release APK** | SHA-256 `935c4f13c9b289ba65a7ea184d8ffb98d939a662e5962b7d7d90a4ca2d96b572`, signed by `CN=Daniel Valcarce, C=ES` (certificate SHA-256 `abd7e456e1edcd040377267afdcf0ebbd0ee7dd82c4f07a2466c366a074ad75f`) |

This records how each requirement of version 1.0.0 was checked on the tablet. A requirement passes when all its scenarios pass. Scenarios run on the signed release APK, except A11, which needs the debug APK from the same commit (P8 Q3). Nothing is marked as passing unless it was seen to pass.

## Scenarios

| # | What it checks | Checked by | Result | Notes |
|---|---|---|---|---|
| A1 | Installing, what's in the APK, launch | Claude | Pass | v2 signature, version 1.0.0, 9.7 MB. Not debuggable, no backup flag, `run-as` refused. The only permission is the one the app grants itself. One launcher entry. First launch 1.15 s, cold starts 1.02–1.09 s. Opens on the empty list. |
| A2 | English text, all in resources | Claude | Pass | Read in English on a Spanish tablet: the list, the editor, Settings, the runner in get ready, an exercise and a rest, "Getting ready…", every dialog and the completed screen. No user-facing text in the Kotlin sources. |
| A3 | Creating the §4.4 example | Claude | Pass | Typed with the real keyboard through adb, leaving linked values empty. "Total 4:23" before saving and on the list. |
| A4 | Editing, linked defaults, validation, discarding | Claude | Pass | Default duration 45: High knees and Crunches show 45, Jumping jacks keeps 30, total 4:33. "Not used" moved with the exercise, and Arm circles kept its 5. Messages: "Enter a name", "From 1 to 5999 s", "From 0 to 5999 s", "Add at least one exercise", "Add at least one round", and nothing saved. "Discard changes?", then 4:23. |
| A5 | Duplicating and deleting | Claude | Pass | "Example (copy)" with 4:23. The dialog names the copy. Cancel keeps it, Delete removes it. |
| A6 | A full run with voice, offline, from 3 m | User | Pass | From a cold speech engine: "Getting ready…", then the user confirmed every cue in the example table, the silent rests, the one chime, "Workout complete. Well done!", and that the text reads well from afar. Span 268.96 s. The user tried every screen at arm's length: the buttons are easy to hit. Offline is checked in A7, because Wi-Fi stayed on here (P8 Q9). |
| A7 | Cue edge cases | User | Pass | With Wi-Fi off (`Active default network: none`) and a cold speech engine. The user confirmed: Ten not named, Eleven named, the chime then "Twelve" not over it, the 3, 2 and 1 s exercises counting from their own duration, and the end message. "Total time 0:42 · 2 rounds · 6 exercises". |
| A8 | Pause and resume | User and Claude | Pass | Paused in get ready (00:03), an exercise (00:30, 00:25, 00:02) and a rest (00:04); each held still, then counted on. The orange paused colour, "PAUSED", and the four buttons. The user confirmed: speech stops at the pause, nothing is replayed, and "one" comes on time. |
| A9 | Next, Previous, Stop, Back | User and Claude | Pass | The whole Next and Previous table, on screen. Previous does nothing in get ready. The user confirmed no chime crossing into Core, "Plank" on resume, and "Workout complete. Well done!" on Next from the last exercise. Back pauses, then asks to stop, then closes the dialog. Stop, Cancel keeps it paused; Stop, Stop returns to the list. |
| A10 | Leaving the app | Claude | Pass | Paused at 00:23 after Home, 00:19 after Recents and 00:15 after the power button, and unchanged 4 s later each time. |
| A11 | Android closing the app (debug APK) | Claude | Pass | Debug APK from the same commit. The process ended as `cch CAC`. The list came back from the launcher and from Recents. |
| A12 | Turning the tablet, a forced configuration change | User and Claude | Pass | The display rotation was only ever 1 or 3, and the user confirmed it stayed landscape when upright. Turned while running, paused and with the dialog open: nothing reset, paused or skipped, and the dialog stayed. A real configuration change (`font_scale` 1.15, "relaunching resumed ActivityRecord") left the workout running at the right time. Put back to 1.0. |
| A13 | The screen stays on | Claude | Pass | With a 30 s timeout: still on after 40 s running and 70 s paused (`FLAG_KEEP_SCREEN_ON` set), and off after 45 s on the list. The timeout is back to "never". |
| A14 | Voice cues off | User | Pass | The switch stayed off after a force-stop. Span 42.97 s for 42 s planned, so get ready started at once. The user heard only the chime. |
| A15 | Volume keys | User | Pass | The media volume followed the keys during cues, between cues and on the list, and the ring volume never moved. The user confirmed the voice and the chime follow it. |
| A16 | A reboot | User and Claude | Pass | After `adb reboot` and unlocking: both workouts listed, and voice cues still off. Turned back on afterwards. |
| A17 | The 60-minute run | Claude; user at the end | To do | |
| A18 | No crash, no warning from the app | Claude | In progress | No crash, and no `WorkoutAudio` or `WorkoutStorage` line, so far. |

## Requirements

### Workout execution

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| EX-1 | Phases follow the sequencing rules | A6 | Pass |
| EX-2 | An exercise lasts its duration, then the next phase starts | A6 | Pass |
| EX-3 | A rest lasts its duration; 0 s means no rest | A6, A7 | Pass |
| EX-4 | Pause and resume an exercise | A8 | Pass |
| EX-5 | Pause and resume in rests and get ready | A8 | Pass |
| EX-6 | A completion screen at the end | A6 | Pass |
| EX-7 | A 3 s get ready before the first exercise | A6 | Pass |
| EX-8 | Stop asks for confirmation | A9 | Pass |
| EX-9 | Next and Previous | A9 | Pass |
| EX-10 | Pauses when the app leaves the foreground | A10 | Pass |
| EX-11 | Not restored after Android closes the app | A11 | Pass |

### Workout screen

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| UI-1 | Shows the round name | A6 | Pass |
| UI-2 | Shows the exercise name | A6 | Pass |
| UI-3 | Shows the current phase's time as `mm:ss` | A6 | Pass |
| UI-4 | A pause and resume control | A8 | Pass |
| UI-5 | A rest shows the next exercise, and the next round if it changes | A6 | Pass |
| UI-6 | Shows progress in the round and the workout | A6 | Pass |
| UI-7 | A background colour for each state | A6, A8 | Pass |
| UI-10 | Only Pause while running | A6 | Pass |
| UI-11 | Resume, Previous, Next and Stop while paused, and says it's paused | A8 | Pass |

### Audio cues

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| TTS-1 | The name is spoken when an exercise is longer than 10 s | A6, A7 | Pass |
| TTS-2 | "three", "two", "one" for the last 3 s | A6, A7 | Pass |
| TTS-3 | Pausing stops speech, and nothing is replayed | A8 | Pass |
| TTS-4 | Cues work without a network | A7 | Pass |
| TTS-7 | "Workout complete. Well done!" at the end | A6, A9 | Pass |
| TTS-9 | Voice cues can be turned off, and the choice is remembered | A14, A16 | Pass |
| TTS-10 | "Get ready in", then the countdown | A6, A14 | Pass |
| TTS-11 | Always in English (United States) | A6 | Pass |
| TTS-12 | Rests are silent | A6 | Pass |
| SND-1 | A sound when a round ends, not after the last | A6, A7 | Pass |
| SND-2 | No other sound effects | A6 | Pass |
| §5.3 | The volume keys always change the media volume | A15 | Pass |

### Workout management

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| WM-1 | Exercises have a name and a duration, with a default | A3, A4 | Pass |
| WM-2 | Rests with defaults, and own rests | A3 | Pass |
| WM-3 | Several workouts are listed | A5 | Pass |
| WM-4 | Create, edit, duplicate and delete | A3, A4, A5 | Pass |
| WM-5 | Add, rename, reorder, duplicate and remove rounds and exercises | A4 | Pass |
| WM-6 | The editor and the list show the total duration | A3, A4 | Pass |
| WM-7 | Validation | A4 | Pass |
| WM-8 | Survives restarts and reboots; no cloud backup | A1, A16 | Pass |
| WM-9 | Deleting asks for confirmation | A5 | Pass |
| WM-10 | Leaving with unsaved changes asks for confirmation | A4 | Pass |

### Non-functional

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| NF-1 | A 60-minute workout ends within ±1 s | A17 | To do |
| NF-2 | The screen stays on in the runner | A13 | Pass |
| NF-3 | Configuration changes don't affect the workout | A12 | Pass |
| NF-4 | No network permission, no accounts, no analytics | A1, A7 | Pass |
| NF-5 | Runs on Android 8.0 on the target device | A1 | Pass |
| NF-6 | Readable from 3 m, with large touch targets | A6 | Pass |
| NF-7 | Installed as an APK from the development machine | A1 | Pass |
| NF-8 | Landscape only, following the sensor | A12 | Pass |
| NF-9 | English UI, with all text in resources | A2 | Pass |

## The 60-minute run (`NF-1`)

To be filled in by A17: the overhead, the span, the paused time and the drift (P8 SPEC §4.3.5).

## Problems found

None so far.
