# PN — <Phase name>: Specification

| | |
|---|---|
| **Status** | Draft v0.1 / Ready |
| **Phase** | PN, see [Implementation plan §5](../../IMPLEMENTATION_PLAN.md#5-phases) |
| **Depends on** | PX, PY |
| **Branch** | `phase/pN-<name>` |
| **Handover** | [HANDOVER.md](HANDOVER.md) |

This document holds everything needed to implement the phase. Working on it shouldn't require reading the full specification or plan.

## 1. Goal

What exists at the end of this phase that didn't exist before, in two or three sentences.

## 2. Requirements covered

The text of each requirement is quoted from [SPECIFICATION.md](../../SPECIFICATION.md) (version vX.Y), so this document stands on its own.

| ID | Requirement (quoted) | What this phase does for it |
|---|---|---|
| | | |

## 3. Scope

### In scope

### Out of scope

Things that belong to other phases, and which phase owns them.

## 4. Design

### 4.1 Packages and files owned

The packages and resource files this phase creates or changes. Changes to shared files are listed explicitly.

### 4.2 Interfaces and data

Public types, functions and data formats, with their signatures.

### 4.3 Behaviour

Rules, state transitions, diagrams and edge cases.

### 4.4 Dependencies

Libraries added (with versions), and interfaces used from other phases. Say which fakes are used if the providing phase isn't merged yet.

## 5. Security and privacy

What this phase must respect or check, with reference to plan §3.

## 6. Testing

| Test | Type (unit / UI / manual) | Covers |
|---|---|---|
| | | |

### Manual checks on the tablet

Steps and expected results.

## 7. Tasks

In order. Each task should take one working session or less.

| ID | Task |
|---|---|
| T1 | |

## 8. Done criteria

The checks that must all pass before the phase is merged.

- [ ] All tasks done
- [ ] Unit tests and lint pass locally and in CI
- [ ] Manual checks on the tablet pass
- [ ] HANDOVER.md is up to date, and the plan's status table is updated

## 9. Open questions

| # | Question | Draft assumption |
|---|---|---|

## 10. Change log

| Version | Date | Changes |
|---|---|---|
| 0.1 | YYYY-MM-DD | First draft |
