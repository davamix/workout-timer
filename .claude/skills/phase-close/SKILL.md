---
name: phase-close
description: Close a workout-timer phase after the user has merged its pull request. On main, it marks the phase Done in the plan, ticks its done criteria in SPEC.md, and finishes its HANDOVER.md. Use when the user says a phase's pull request is merged, e.g. "PR is merged" or "close P3".
---

# Close a merged phase

Input: a phase ID, e.g. `P3`. If none is given, take it from the merged pull request or the current branch. The phase folders are listed in the `phase-docs` skill.

## Steps

1. **Check the merge.** `gh pr view <N> --json state,mergedAt,url` must show `MERGED`. If it doesn't, stop and tell the user. Use the `mergedAt` date in the documents below.
2. **Update `main`.** Run `git switch main && git pull --ff-only`. If this working copy is busy with another phase, don't switch it; use `git worktree add --detach ../workout-timer-close origin/main` instead, and remove that worktree when done.
3. **Edit the three documents.** These closing edits go straight to `main`, because the phase branch is merged. It's the one exception to "HANDOVER.md updates go on the phase branch".
   - **Plan §5.1** (`docs/IMPLEMENTATION_PLAN.md`): set the phase's status to `Done`. Status changes don't need a new plan version.
   - **SPEC.md §8:** tick every done criterion (`- [x]`). Ticking isn't a spec change, so don't bump the version.
   - **HANDOVER.md:**
     - **Status:** `Done. Merged in pull request [#N](<url>) on <date>.`
     - **Last session:** today's date.
     - **Prompt to start or continue:** replace everything under the heading, both the paragraph and the prompt block, with "Phase done: no prompt needed."
     - **Current state:** say it is merged into `main`. Keep the summary of what was built.
     - **Next steps:** "None for PN.", then the phases that can now start: those in plan §5.1 whose dependencies are all `Done`.
     - **Tasks:** set the last task (the pull request one) to `Done`.
     - **Session log:** add an entry at the top, `### <date> (close)`, saying the user merged the pull request and the phase is closed.
4. **Commit and push to `main`,** with the message `Close PN: mark done in plan, spec and handover`.
5. **Report to the user:** what was closed, and which phases can start now. For each of those, say whether its SPEC.md and HANDOVER.md exist yet. If they don't, the `phase-docs` skill writes them.

Leave the phase branch and its worktree alone unless the user asks to delete them.
