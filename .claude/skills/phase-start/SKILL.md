---
name: phase-start
description: Start or resume implementing a workout-timer phase. It loads only that phase's SPEC.md and HANDOVER.md, checks dependencies, and sets up the phase branch or worktree. Use at the start of a session that works on a phase, e.g. "start P3" or "continue P2".
---

# Start or resume a phase

Input: a phase ID, e.g. `P3`. The phase folders are listed in the `phase-docs` skill.

**No phase ID given?** Don't guess. Read plan §5.1 and list the phases that can start: not yet `Done`, with every dependency `Done`. For each one, say whether its SPEC.md and HANDOVER.md exist, and what depends on it. Then ask the user which one to start, recommending the one that unblocks the most work.

## Steps

1. **Load the phase.** Read `docs/phases/PN-*/HANDOVER.md` first, then `SPEC.md`.
   - If the phase has no SPEC.md yet, write it with the `phase-docs` skill, report it, and stop until the user has reviewed it. Once they agree and the SPEC is `Ready`, carry on from step 2 in the same session. Don't start implementing a draft.
   - Don't read `docs/SPECIFICATION.md` or the whole plan. The phase docs are meant to be enough.
   - Only open those files if the phase docs point to something they don't contain. If that happens, note it as a gap in the handover.
2. **Check that the phase can start.**
   - In plan §5.1 (`docs/IMPLEMENTATION_PLAN.md`), every dependency must be `Done`.
   - If one isn't, stop and tell the user. Work can still go ahead with the fakes described in plan §5.1, but only if the user agrees.
   - SPEC.md must be `Ready`. If it's still a draft, ask the user before implementing.
3. **Set up the branch.** The branch is named `phase/pN-<name>`; `<name>` is the folder name without the `PN-` prefix. Run `git fetch origin` first.
   - **If the branch already exists,** switch to it, or to its worktree, and go to step 5.
   - **Otherwise, this is the first session.** Mark the phase in progress on `main` first, so the branch starts from that commit. Status changes always go to `main`.
     1. Get an up-to-date `main`: `git switch main && git pull --ff-only`. If this working copy is busy with another phase, don't switch it. Use a new worktree instead, and work there from now on: `git worktree add --detach ../workout-timer-pN origin/main`.
     2. In plan §5.1, set the phase's status to `In progress`. Commit, then push with `git push origin HEAD:main`.
     3. Create the branch from there: `git switch --no-track -c phase/pN-<name>`. Don't branch from `origin/main` without `--no-track`: the branch would then track `main`, and a plain `git push` would push to `main`.
     4. On the first push, set the upstream: `git push -u origin phase/pN-<name>`.
4. **Mark it in progress in the handover** (first session only). Set HANDOVER.md's status to `In progress`, and commit it on the phase branch.
5. **Tell the user** the current state, the next steps from the handover, and what you are going to do now.

## While working

- Follow SPEC.md. When something has to differ, record it straight away under "Decisions and deviations" in HANDOVER.md.
- If a change affects a requirement, update SPEC.md first, then the specification or the plan if needed, and tell the user.
- Commit small, coherent steps on the phase branch.
