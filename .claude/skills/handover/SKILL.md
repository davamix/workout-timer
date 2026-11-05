---
name: handover
description: End-of-session routine for a workout-timer phase. It runs the checks, updates the phase's HANDOVER.md, and commits and pushes; when the done criteria pass, it opens the pull request and waits for CI. Use when the user says to wrap up, hand over, end the session, or save progress.
---

# Hand over a phase

## Steps

1. **Run the checks.**
   - Run `./gradlew ktlintFormat`, then `./gradlew build` as a separate command (see CLAUDE.md). The build covers compilation, unit tests, lint, ktlint and the no-INTERNET check. Total the test results with the one-liner in CLAUDE.md.
   - **Instrumented tests:** if UI or Android code changed since the last full instrumented run, and the tablet is connected, run `./gradlew connectedDebugAndroidTest`. The `tablet` skill covers the device.
     - It uninstalls the app when it ends, which deletes the debug app's data, such as workouts the user made during manual checks. If the user may have data there, ask first, or run only the changed test classes with `am instrument` (`tablet` skill), which keeps it.
     - Afterwards, run `./gradlew installDebug` to leave the app installed.
     - If only documents changed since a full run passed, don't run it again. Say which commit it ran on.
   - **Merged manifests:** if dependencies or the manifest changed, check that the merged manifests gain no permission or component, with "Comparing the merged manifests with `main`" below.
   - When a unit test fails, print each failure's name and message with the one-liner in CLAUDE.md.
   - Note the results exactly. Don't call something passing unless it ran and passed.
2. **Push decisions up.**
   - **Does it need a new SPEC version?** A decision that only settles a point SPEC.md leaves open stays in HANDOVER.md. One that contradicts SPEC.md's text gets a new SPEC version, with a change-log entry. That includes a change to an internal API or behaviour the SPEC describes. New wording for a UI text the SPEC lists as a suggestion isn't a contradiction: the strings file holds the final wording (`phase-docs`, step 5).
   - **Specification or plan first.** If a decision changes the specification or the plan, update those first and tell the user.
   - **Where SPEC versions go:** to `main`, like every version of these documents (CLAUDE.md, "Git"). Commit them there, from a worktree if this working copy is busy, and push. Then run `git merge main` on the phase branch, so the pull request carries the same SPEC.md as `main`. Commit any pending work on the branch before switching. The full steps are in the `phase-docs` skill, "SPEC change during implementation".
   - **Note it in the handover.** Mention the new version in the decision's row.
3. **Open the pull request, if the phase is complete.** Do this when every done criterion in SPEC.md §8 passes, apart from the ones that need the pull request or the merge: CI, and the plan's status. Open it before writing the handover, so the handover can record the PR number and the CI result.
   - Commit any remaining work on the phase branch, and push it.
   - Write the body (template below) to a file in the session's scratchpad, not in the repository. Then run `gh pr create --base main --head phase/pN-<name> --title "PN: <phase name>" --body-file <file>`.
   - Wait for CI on the commit you just pushed. Base the wait on that commit's SHA, not on the pull request. Right after a push, `gh pr checks` can still show the previous commit's finished run, a false green. Before the new run registers, it can report "no checks", which `--watch` also treats as done. Run this in the background and wait for it to finish:

     ```bash
     sha=$(git rev-parse HEAD)
     until id=$(gh run list --commit "$sha" --workflow CI --json databaseId -q '.[0].databaseId' 2>/dev/null) && [ -n "$id" ]; do sleep 10; done
     gh run watch "$id" --exit-status --interval 20 > /dev/null; echo "CI exit=$? run=$id"
     ```

     `CI exit=0` means it passed. Otherwise, `gh run view <id> --log-failed` shows why.
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
5. **Commit and push** on the phase branch, with a message that summarises the session. With a PR open, this push runs CI again. Wait for it with the commands from step 3. They take the new `HEAD`, so they wait for this push's run, not the earlier one.
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

## Comparing the merged manifests with `main`

Lists the permissions and components of the debug and release manifests, on the branch and on `main`, and compares them. `main` is built in a scratch worktree, so this working copy stays on the branch. Run it in Git Bash after `./gradlew build`, with `sp` set to the session's scratchpad.

```bash
sp=<scratchpad>
# Permissions and components of a merged manifest, one "<tag> <name>" per line.
components() { tr '\n' ' ' < "$1" | grep -o -E '<(uses-permission|permission|activity|activity-alias|service|receiver|provider)[^>]*>' | sed -E 's/^<([a-z-]+)[^>]*android:name="([^"]*)".*/\1 \2/' | sort; }
manifest() { find "$1/app/build/intermediates/merged_manifest/$2" -name AndroidManifest.xml | head -1; }

./gradlew :app:processDebugMainManifest :app:processReleaseMainManifest --console=plain -q 2>&1 | grep -v -i 'unsafe\|^WARNING'
git worktree add -q --detach "$sp/main-wt" origin/main && cp local.properties "$sp/main-wt/" 2>/dev/null
(cd "$sp/main-wt" && ./gradlew :app:processDebugMainManifest :app:processReleaseMainManifest --console=plain -q 2>&1 | grep -v -i 'unsafe\|^WARNING')
for v in debug release; do
  a=$(manifest "$sp/main-wt" $v); b=$(manifest . $v)
  if [ -z "$a" ] || [ -z "$b" ]; then echo "$v: merged manifest not found"; continue; fi
  diff <(components "$a") <(components "$b") && echo "$v: same as main ($(components "$b" | wc -l) entries)"
done
git worktree remove --force "$sp/main-wt"
```

Every difference is a line starting with `<` (only on `main`) or `>` (new on the branch). A new permission or component must be explained in the handover, or removed. A changed attribute, such as `windowSoftInputMode`, doesn't show here: check that one in the manifest itself.
