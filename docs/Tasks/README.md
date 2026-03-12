# Tasks

One file per implementation-ready unit of work. A Task is concrete: it has a plan, acceptance criteria, and is assigned to be worked now (or in the very next session). Do NOT pile up Tasks — if work isn't imminent, it belongs in `Bugs/` or `Planned_Features/`.

**When to read this folder:** At the start of a session to see what's queued.
**When to write here:** When elevating a Bug or Feature to active work. Create the Task, update `docs/STATUS.md`, and do NOT leave the Bug/Feature file behind (move it to Archive when the Task is complete, or keep it if the Task only partially resolves it).
**When to move a file:** Move to `docs/Archive/` when the task is complete. Add a completion note at the top. Update `docs/STATUS.md`.

## File Naming

`TASK-NNN-short-slug.md` (next available number, lowercase slug, hyphens)

## Template

```markdown
# TASK-NNN: Short Title

**Status:** Ready | In Progress | Done
**Estimated scope:** Tiny | Small | Medium | Large
**Touches:** list of files to change
**Resolves:** docs/Bugs/BUG-NNN or docs/Planned_Features/FEAT-NNN

## Context
Why this task exists and what problem it solves.

## Acceptance Criteria
- [ ] Specific, testable outcome
- [ ] ...

## Implementation Plan
Step-by-step. Enough detail to execute without guessing.

## Notes
Known edge cases, gotchas, things to double-check.
```
