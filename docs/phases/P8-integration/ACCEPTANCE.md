# Workout Timer 1.0.0: acceptance record

| | |
|---|---|
| **Status** | In progress |
| **Specification** | [SPECIFICATION.md](../../SPECIFICATION.md) v0.11 |
| **Scenarios** | [P8 SPEC §6](SPEC.md#6-testing) |
| **Device** | HUAWEI MediaPad T5 10 (`AGS2-W09`), Android 8.0 (API 26), system language Spanish |
| **Commit** | Filled in when the checks start |
| **Release APK** | Filled in when the checks start |

This records how each requirement of version 1.0.0 was checked on the tablet. A requirement passes when all its scenarios pass. Scenarios run on the signed release APK, except A11, which needs the debug APK from the same commit (P8 Q3). Nothing is marked as passing unless it was seen to pass.

## Scenarios

| # | What it checks | Checked by | Result | Notes |
|---|---|---|---|---|
| A1 | Installing, what's in the APK, launch | Claude | To do | |
| A2 | English text, all in resources | Claude | To do | |
| A3 | Creating the §4.4 example | Claude | To do | |
| A4 | Editing, linked defaults, validation, discarding | Claude | To do | |
| A5 | Duplicating and deleting | Claude | To do | |
| A6 | A full run with voice, offline, from 3 m | User | To do | |
| A7 | Cue edge cases | User | To do | |
| A8 | Pause and resume | User and Claude | To do | |
| A9 | Next, Previous, Stop, Back | User and Claude | To do | |
| A10 | Leaving the app | Claude | To do | |
| A11 | Android closing the app (debug APK) | Claude | To do | |
| A12 | Turning the tablet, a forced configuration change | User and Claude | To do | |
| A13 | The screen stays on | Claude | To do | |
| A14 | Voice cues off | User | To do | |
| A15 | Volume keys | User | To do | |
| A16 | A reboot | User and Claude | To do | |
| A17 | The 60-minute run | Claude; user at the end | To do | |
| A18 | No crash, no warning from the app | Claude | To do | |

## Requirements

### Workout execution

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| EX-1 | Phases follow the sequencing rules | A6 | To do |
| EX-2 | An exercise lasts its duration, then the next phase starts | A6 | To do |
| EX-3 | A rest lasts its duration; 0 s means no rest | A6, A7 | To do |
| EX-4 | Pause and resume an exercise | A8 | To do |
| EX-5 | Pause and resume in rests and get ready | A8 | To do |
| EX-6 | A completion screen at the end | A6 | To do |
| EX-7 | A 3 s get ready before the first exercise | A6 | To do |
| EX-8 | Stop asks for confirmation | A9 | To do |
| EX-9 | Next and Previous | A9 | To do |
| EX-10 | Pauses when the app leaves the foreground | A10 | To do |
| EX-11 | Not restored after Android closes the app | A11 | To do |

### Workout screen

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| UI-1 | Shows the round name | A6 | To do |
| UI-2 | Shows the exercise name | A6 | To do |
| UI-3 | Shows the current phase's time as `mm:ss` | A6 | To do |
| UI-4 | A pause and resume control | A8 | To do |
| UI-5 | A rest shows the next exercise, and the next round if it changes | A6 | To do |
| UI-6 | Shows progress in the round and the workout | A6 | To do |
| UI-7 | A background colour for each state | A6, A8 | To do |
| UI-10 | Only Pause while running | A6 | To do |
| UI-11 | Resume, Previous, Next and Stop while paused, and says it's paused | A8 | To do |

### Audio cues

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| TTS-1 | The name is spoken when an exercise is longer than 10 s | A6, A7 | To do |
| TTS-2 | "three", "two", "one" for the last 3 s | A6, A7 | To do |
| TTS-3 | Pausing stops speech, and nothing is replayed | A8 | To do |
| TTS-4 | Cues work without a network | A6 | To do |
| TTS-7 | "Workout complete. Well done!" at the end | A6, A9 | To do |
| TTS-9 | Voice cues can be turned off, and the choice is remembered | A14, A16 | To do |
| TTS-10 | "Get ready in", then the countdown | A6, A14 | To do |
| TTS-11 | Always in English (United States) | A6 | To do |
| TTS-12 | Rests are silent | A6 | To do |
| SND-1 | A sound when a round ends, not after the last | A6, A7 | To do |
| SND-2 | No other sound effects | A6 | To do |
| §5.3 | The volume keys always change the media volume | A15 | To do |

### Workout management

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| WM-1 | Exercises have a name and a duration, with a default | A3, A4 | To do |
| WM-2 | Rests with defaults, and own rests | A3 | To do |
| WM-3 | Several workouts are listed | A5 | To do |
| WM-4 | Create, edit, duplicate and delete | A3, A4, A5 | To do |
| WM-5 | Add, rename, reorder, duplicate and remove rounds and exercises | A4 | To do |
| WM-6 | The editor and the list show the total duration | A3, A4 | To do |
| WM-7 | Validation | A4 | To do |
| WM-8 | Survives restarts and reboots; no cloud backup | A1, A16 | To do |
| WM-9 | Deleting asks for confirmation | A5 | To do |
| WM-10 | Leaving with unsaved changes asks for confirmation | A4 | To do |

### Non-functional

| ID | Requirement | Scenarios | Result |
|---|---|---|---|
| NF-1 | A 60-minute workout ends within ±1 s | A17 | To do |
| NF-2 | The screen stays on in the runner | A13 | To do |
| NF-3 | Configuration changes don't affect the workout | A12 | To do |
| NF-4 | No network permission, no accounts, no analytics | A1, A6 | To do |
| NF-5 | Runs on Android 8.0 on the target device | A1 | To do |
| NF-6 | Readable from 3 m, with large touch targets | A6 | To do |
| NF-7 | Installed as an APK from the development machine | A1 | To do |
| NF-8 | Landscape only, following the sensor | A12 | To do |
| NF-9 | English UI, with all text in resources | A2 | To do |

## The 60-minute run (`NF-1`)

To be filled in by A17: the overhead, the span, the paused time and the drift (P8 SPEC §4.3.5).

## Problems found

None so far.
