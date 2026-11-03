---
name: handover
description: End-of-session routine for a workout-timer phase. It runs the checks, updates the phase's HANDOVER.md, and commits and pushes; when the done criteria pass, it opens the pull request. Use when the user says to wrap up, hand over, end the session, or save progress.
---

# Hand over a phase

## Steps

1. **Run the checks.**
   - Always run `./gradlew build`; it covers compilation, unit tests, lint, ktlint and the no-INTERNET check.
   - If UI or Android code changed and the tablet is connected, also run `./gradlew connectedDebugAndroidTest`. The `tablet` skill covers the device.
   - Note the results exactly. Don't call something passing unless it ran and passed.
2. **Update HANDOVER.md** in the phase folder:
   - **Last session:** today's date.
   - **Status:** `In progress`, `Blocked` or `Done`.
   - **Prompt to start or continue:** make sure it works as-is for the next session. Use point 4 for anything that session must know first, such as a question waiting for the user, or that the tablet must be connected. When the phase is `Done`, replace the prompt with "Phase done: no prompt needed."
   - **Current state:** rewrite it so it describes the state now. Don't append to it.
   - **Next steps:** concrete enough that a new session can start without reading anything else.
   - **Tasks:** update each status.
   - **Decisions and deviations:** add a row for each new decision.
   - **Known issues** and **Pending wiring:** keep them current.
   - **How to verify:** the commands and manual steps that check the current state.
   - **Session log:** add a new entry at the top, saying what was done and anything worth knowing.
3. **Push decisions up.** If a decision changes SPEC.md, update it, with a new version and change-log entry. If it changes the specification or the plan, update those first and tell the user.
4. **Commit and push** on the phase branch, with a message that summarises the session.
5. **If every done criterion in SPEC.md §8 passes:**
   - Open a pull request into `main` with `gh pr create`. Its body should state the goal, link SPEC.md and HANDOVER.md, list the check results, and list any pending wiring.
   - Wait for CI to pass: `gh pr checks --watch`.
   - Leave the merge to the user, unless they have said otherwise.
   - After the merge, set the plan §5.1 status to `Done` on `main` and push.
6. **Report to the user:** what was done, the check results, and the next steps.
