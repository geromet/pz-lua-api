## Docs Folder Structure

`pz-lua-api-viewer/docs/` is the canonical location for all project documentation and tracked items.

### Subfolders

| Folder | Purpose |
|--------|---------|
| `Bugs/` | One file per confirmed defect (BUG-NNN-slug.md) |
| `Planned_Features/` | One file per feature that is designed but not yet scheduled (FEAT-NNN-slug.md) |
| `Tasks/` | Implementation-ready tasks assigned for active work (TASK-NNN-slug.md) |
| `Archive/` | Completed or superseded documents (keeps git history clean) |
| `Temp/` | Scratch space for a single session (gitignored) |
| `Knowledge_Base/` | Stable reference: decisions, patterns, domain language, style |
| `Skills/` | Claude task-handling instructions for recurring scenarios |

Each folder has its own `README.md` with templates and conventions.

### Status Tracker

`pz-lua-api-viewer/docs/STATUS.md` is the live tracker listing all open bugs, active tasks, and planned features. This is checked at the start of a session and updated when work completes.

### Knowledge Base (Stable Reference)

The `Knowledge_Base/` folder contains:

- `Bug-Feature-Triage.md` — workflow for deciding if a bug should be bundled into a planned feature
- `Decisions.md` — Architectural Decision Records (ADR-001 and onwards)
- `Domain-Language.md` — project-specific terminology
- `Design-Patterns.md` — recurring code patterns to follow
- `Style-Guide.md` — naming, formatting, code style conventions
- `Philosophy.md` — core principles guiding design
- `Working-Agreement.md` — how Claude and the user collaborate
- `Testing.md` — manual verification procedures (no automated tests)
- `Refactoring.md` — guidelines for when/how to refactor

### Archive Examples

`docs/Archive/Tasks/` and `docs/Archive/Bugs/` contain previously completed work. `docs/Archive/NEXT_SESSION.md` tracks previous sessions and what was completed.
