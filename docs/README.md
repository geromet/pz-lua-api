# PZ Lua API Viewer — Docs

This folder organises all working documents for the project. Use the right subfolder — everything has a place.

## Folder Structure

| Folder | Purpose |
|--------|---------|
| `Bugs/` | One file per confirmed defect |
| `Planned_Features/` | One file per feature that is designed but not yet scheduled |
| `Tasks/` | Implementation-ready tasks assigned for active work |
| `Archive/` | Completed or superseded documents — kept for reference |
| `Temp/` | Scratch space for a single session (gitignored) |
| `Knowledge_Base/` | Stable reference: decisions, patterns, domain language, style |
| `Skills/` | Claude task-handling instructions for recurring scenarios |

Each folder has its own `README.md` explaining its purpose, conventions, and templates.

## Where to Find the Current State

**→ `STATUS.md` (this folder)** is the live tracker. It lists all open bugs, active tasks, and planned features. Check it at the start of a session and update it when work is completed.

## Decision Tree — Which Folder?

```
Something is broken or wrong?
  → Bugs/

It's a new capability we want but won't build right now?
  → Planned_Features/

It's ready to implement right now (has a plan + acceptance criteria)?
  → Tasks/

Work is done on a Task or Bug?
  → Archive/ (move the file, add a completion note)

Stable facts about how this project works?
  → Knowledge_Base/

One-off notes, scripts, dumps just for this session?
  → Temp/
```
