# Bugs

One file per confirmed defect. File a bug here when something is reproducibly broken and won't be fixed in the same session it was found.

**When to read this folder:** When investigating a known issue, or to pick the next thing to fix.
**When to write here:** When a new bug is confirmed. Also update `docs/STATUS.md`.
**When to move a file:** Move to `docs/Archive/` when the bug is fixed. Add a one-line fix summary at the top of the file and update `docs/STATUS.md`.

## File Naming

`BUG-NNN-short-slug.md` (next available number, lowercase slug, hyphens)

## Template

```markdown
# BUG-NNN: Short Title

**Status:** Open | Fixed (date)
**Severity:** Critical | High | Medium | Low
**Touches:** file(s) involved

## Description
What goes wrong and where.

## Steps to Reproduce
1. ...

## Root Cause
(fill in when known)

## Fix Sketch
Enough detail to write a Task from.
```
