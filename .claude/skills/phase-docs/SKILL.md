---
name: phase-docs
description: Create the SPEC.md and HANDOVER.md of a workout-timer phase (P1–P8) from the implementation plan and the templates. Use when asked to write, create or prepare a phase's documents or specification.
---

# Create a phase's documents

Input: a phase ID, e.g. `P3`.

Phase folders:

| Phase | Folder |
|---|---|
| P1 | `docs/phases/P1-foundation/` |
| P2 | `docs/phases/P2-domain/` |
| P3 | `docs/phases/P3-storage/` |
| P4 | `docs/phases/P4-engine/` |
| P5 | `docs/phases/P5-audio/` |
| P6 | `docs/phases/P6-management-ui/` |
| P7 | `docs/phases/P7-runner-ui/` |
| P8 | `docs/phases/P8-integration/` |

## Steps

1. **Read the plan.** In `docs/IMPLEMENTATION_PLAN.md`, read the parts that concern this phase:
   - its summary in §5.2, its row in §5.1 (dependencies) and §5.3 (coverage);
   - the decisions `D-n`, §2 architecture, §3 security and §4 testing.
2. **Quote the requirements.** For every requirement ID the phase covers, copy its exact text from `docs/SPECIFICATION.md`. Note the specification version.
3. **Read the code it builds on.** Read only the merged code this phase uses: the interfaces from its dependencies, plus the shared files it will touch.
4. **Write SPEC.md.** Copy `docs/phases/_templates/SPEC_TEMPLATE.md` into the phase folder as `SPEC.md` and fill in every section:
   - The document must stand alone. A session that implements the phase should not need the specification or the plan.
   - List the packages and files the phase owns. Name every shared file it changes.
   - Tasks: in order, each one session or less, with IDs `T1`, `T2`, …
   - Every covered requirement needs a test or a manual check in §6.
   - Anything unclear goes into §9 as a question, with the assumption the draft uses.
5. **Write HANDOVER.md.** Copy `docs/phases/_templates/HANDOVER_TEMPLATE.md` as `HANDOVER.md`:
   - Status: Not started.
   - Every task listed as To do.
   - Next steps: start with `T1`.
   - **Prompt to start or continue:** fill in the phase ID, name, folder and branch. Write the phase's own notes in point 4, for example whether the tablet must be connected, or which fakes to use while a dependency isn't merged.
6. **Update the plan.** Set the phase's status in plan §5.1 to `Spec draft`. Status changes don't need a new plan version.
7. **Commit and push to `main`.** Use the message `PN spec v0.1: <phase name>`. Draft documents are pushed at every version.
8. **Report to the user.** Summarise the spec and list its open questions for review.

When a review changes the spec, apply the changes, bump its version and change log, then commit and push again. Once the user agrees to it, set its status to `Ready`, and set plan §5.1 to `Ready`.
