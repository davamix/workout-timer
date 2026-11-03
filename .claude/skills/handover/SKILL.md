---
name: handover
description: End-of-session routine for a workout-timer phase. It runs the checks, updates the phase's HANDOVER.md, and commits and pushes; when the done criteria pass, it opens the pull request and waits for CI. Use when the user says to wrap up, hand over, end the session, or save progress.
---

# Hand over a phase

## Steps

1. **Run the checks.**
   - Run `./gradlew ktlintFormat`, then `./gradlew build` as a separate command (see CLAUDE.md). The build covers compilation, unit tests, lint, ktlint and the no-INTERNET check. Total the test results with the one-liner in CLAUDE.md.
   - If UI or Android code changed and the tablet is connected, also run `./gradlew connectedDebugAndroidTest`. The `tablet` skill covers the device.
   - Note the results exactly. Don't call something passing unless it ran and passed.
2. **Push decisions up.** If a decision changes SPEC.md, update it, with a new version and change-log entry. If it changes the specification or the plan, update those first and tell the user.
3. **Open the pull request, if the phase is complete.** Do this when every done criterion in SPEC.md §8 passes, apart from the ones that need the pull request or the merge: CI, and the plan's status. Open it before writing the handover, so the handover can record the PR number and the CI result.
   - Commit any remaining work on the phase branch, and push it.
   - Write the body (template below) to a file in the session's scratchpad, not in the repository. Then run `gh pr create --base main --head phase/pN-<name> --title "PN: <phase name>" --body-file <file>`.
   - Wait for CI. Don't call `gh pr checks --watch` straight after creating the PR: until the run registers, it reports "no checks" and exits at once, which looks like success. Run this in the background and wait for it to finish:

     ```bash
     until [ "$(gh pr checks <N> --json name -q length 2>/dev/null)" -gt 0 ] 2>/dev/null; do sleep 10; done
     gh pr checks <N> --watch --interval 20
     ```

   - If CI fails, fix the cause on the phase branch, push, and wait again.
4. **Update HANDOVER.md** in the phase folder:
   - **Last session:** today's date.
   - **Status:** `In progress`, `Blocked` or `Done`. With a pull request open: `In progress: all tasks done; pull request [#N](<url>) is waiting for the user to merge it`.
   - **Prompt to start or continue:** make sure it works as-is for the next session. Use point 4 for anything that session must know first. Examples: a question waiting for the user, the tablet must be connected, or a pull request is open and may have been merged or have review comments.
   - **Current state:** rewrite it so it describes the state now. Don't append to it. Include the PR link and the CI result, if there is a PR.
   - **Next steps:** concrete enough that a new session can start without reading anything else. With a PR open: deal with any review comments, then use the `phase-close` skill after the merge.
   - **Tasks:** update each status. With a PR open, the last task is `In review`.
   - **Decisions and deviations:** add a row for each new decision.
   - **Known issues** and **Pending wiring:** keep them current.
   - **How to verify:** the commands and manual steps that check the current state.
   - **Session log:** add a new entry at the top, saying what was done and anything worth knowing.
5. **Commit and push** on the phase branch, with a message that summarises the session. With a PR open, this push runs CI again. Wait for it the same way as in step 3.
6. **Leave the merge to the user**, unless they have said otherwise. Once it's merged, the `phase-close` skill closes the phase on `main`.
7. **Report to the user:** what was done, the check results, the decisions they should review, and the next steps.

## Pull request body

Fill in each part from SPEC.md and HANDOVER.md. Leave out "Left for the reviewer" if there are no manual checks.

```markdown
## Goal

PN of the [implementation plan](docs/IMPLEMENTATION_PLAN.md): <goal, from SPEC.md §1>. Details: [SPEC.md](docs/phases/PN-<name>/SPEC.md) · progress and decisions: [HANDOVER.md](docs/phases/PN-<name>/HANDOVER.md).

## What's in it

- <one bullet per area, naming the requirement IDs it covers>

## Checks

- `./gradlew build`: <result, with the number of unit tests and failures>
- <instrumented tests and tablet checks, or "No tablet checks: <reason>">

## Decisions to review

<the decisions from HANDOVER.md that the user should look at, where SPEC.md was silent or was changed; or "None.">

## Left for the reviewer

- [ ] <manual check on the tablet>

## Pending wiring

<from HANDOVER.md, or "None.">
```
