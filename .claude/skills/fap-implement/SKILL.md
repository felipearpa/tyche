---
name: fap-implement
description: Implement an OpenSpec change by handing its task groups to the Implementer agent, while this session tracks progress in tasks.md and brings questions to the user. Use when the user wants to implement, continue implementing, or work through the tasks of an OpenSpec change.
argument-hint: "[change-name]"
allowed-tools: Bash(openspec:*)
---

# Implement an OpenSpec Change

This session coordinates and the `Implementer` agent writes the code. OpenSpec supplies the plan through its CLI. The Implementer doesn't edit the change's planning files, and this session doesn't edit product code.

If no `Implementer` agent is available in this session, say so and stop.

## 1. Pick the change

- Use the change name passed as the argument, or the change the user has been discussing.
- Otherwise run `openspec list --json`: use the only active change, ask the user to choose when there are several, and stop when there are none.
- Announce `Using change: <name>` and that `/fap-implement <other-change>` picks a different one.

## 2. Load the plan

```bash
openspec instructions apply --change "<name>" --json
```

- `state: "blocked"`: report which artifacts are missing and stop.
- `state: "all_done"`: say every task is complete, suggest `/opsx:archive`, and stop.
- Otherwise keep `changeDir`, `contextFiles`, `progress`, and `context` (the project context from `openspec/config.yaml`), and read every file listed in `contextFiles`.

## 3. Group the pending tasks

Group the unchecked tasks by the tasks.md section they sit under, such as `## 2. iOS`, and skip sections with nothing pending. Refer to tasks by their tasks.md number (`2.1`). The JSON `id` is a running count, not that number.

Show the schema name, `N/M tasks complete`, and the pending groups in order.

## 4. Send each group to the Implementer

Work through the groups in tasks.md order, one at a time. Run two groups at once only when the user asks and the groups don't touch the same files.

Start an `Implementer` agent. Its brief gives:

- The change name and the `contextFiles` paths to read first. Give paths; don't paste contents.
- The tasks in scope, with their numbers and full text.
- The `context` string from step 2.
- Anything else it needs that isn't in those files, such as decisions made in this conversation, build and test commands, or device and simulator notes. It starts with a fresh context and can't see this conversation.

The brief tells it to:

- Leave every other task alone.
- Not edit the planning files in `changeDir` (the files in `contextFiles`, including tasks.md), because this session tracks progress. The only files it may write in `changeDir` are records a task explicitly asks for, such as `verification.md` and captures under `verification/`.
- Run the verification each task describes before calling the task done.
- Stop and return a question, instead of guessing, when a task is unclear, conflicts with the design or specs, or needs a decision.
- Report each task as done and verified, done but not verified (and why), or not done (and why), plus the files it changed, the checks it ran and their results, and open questions.

## 5. Handle the report

- For each task reported as done and verified, change `- [ ]` to `- [x]` in tasks.md.
- Compare `git status` with the files it reported, and point out any it didn't mention.
- For a task done but not verified, leave the box unchecked and tell the user exactly what wasn't verified. The user decides whether to accept it.
- For questions or blockers, pause and ask the user. If the answer changes the design or the tasks, suggest `/opsx:update` first so the files stay the source of truth. Then send the answer to the same Implementer so it keeps what it learned, or start a new one if you can't.
- Run the command from step 2 again and confirm the progress matches.

Then move on to the next group.

## 6. Finish or pause

Show:

- The tasks completed this session, by number
- `N/M tasks complete`
- Anything not verified and any open questions
- When everything is done, a suggestion to run `/opsx:archive`

## Rules

- Don't write or fix product code in this session. Send fixes back to the Implementer.
- The only edit you make to the change's files is checking boxes in tasks.md. Change the proposal, design, specs, or task text through `/opsx:update`.
