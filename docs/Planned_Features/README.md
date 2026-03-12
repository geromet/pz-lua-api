# Planned Features

One file per confirmed-desirable feature that hasn't been scheduled yet. Features live here until they are ready to implement, at which point a Task is created in `docs/Tasks/`.

**When to read this folder:** When picking what to work on next, or to understand the intended design of something.
**When to write here:** When a new feature request is confirmed. Also add it to `docs/STATUS.md` with a priority.
**When to move a file:** Move to `docs/Archive/` when the feature is implemented (via a completed Task). Update `docs/STATUS.md`.

Do NOT create a Task for a Feature until the feature has a clear plan, acceptance criteria, and you are about to start it.

## File Naming

`FEAT-NNN-short-slug.md` (next available number, lowercase slug, hyphens)

## Template

```markdown
# FEAT-NNN: Short Title

**Status:** Planned
**Priority:** High | Medium | Low
**Complexity:** Tiny | Small | Medium | Large
**Depends on:** (other FEATs or external requirements, if any)

## Description
What this adds for the user and why it's valuable.

## Proposed UX
What it looks like / how the user interacts with it.

## Implementation Notes
Extractor changes, frontend changes, data shape changes.

## Notes
Edge cases, open questions, things to decide before starting.
```
