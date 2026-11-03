# PN — <Phase name>: Handover

| | |
|---|---|
| **Status** | Not started / In progress / Blocked / Done |
| **Spec** | [SPEC.md](SPEC.md), version vX.Y |
| **Branch** | `phase/pN-<name>` |
| **Last session** | YYYY-MM-DD |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Paste this prompt into a new Claude Code session to start or continue the phase. When the phase is done, replace it with "Phase done: no prompt needed."

```text
Start or continue phase PN (<Phase name>) of the workout timer.

1. Use the phase-start skill for PN. Read only docs/phases/PN-<name>/HANDOVER.md and SPEC.md; they contain everything needed.
2. Carry on from "Next steps" in the handover. Work through the tasks in SPEC.md §7 in order, on branch phase/pN-<name>. Commit after each task, once ./gradlew build passes.
3. Follow SPEC.md. If something in it is wrong or unclear, stop and ask me before changing course. Record every decision or deviation in the handover.
4. <Notes for this phase or for the next session: whether the tablet is needed, which fakes to use, a question waiting for an answer, …>
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it.
```

## Current state

One short paragraph: what works now, and what doesn't yet.

## Next steps

1. The very next thing to do, specific enough to start on without re-reading anything else.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | To do / Doing / Done | |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|

## Known issues

## Pending wiring

Connections to other phases left for later, e.g. "uses the in-memory repository; switch to the file repository once P3 is merged".

## How to verify

The commands and manual steps that check the current state.

## Session log

Newest first.

### YYYY-MM-DD

- Done:
- Notes:
