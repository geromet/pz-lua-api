# Docs Reorganization Summary

## Before → After

| Before | After |
|--------|-------|
| `docs/STATUS.md` | `.gsd/state.md` (single source of truth) |
| `docs/Bugs/BUG-XXX.md` | `.gsd/milestones/M001/slices/bugs/BUG-XXX.md` |
| `docs/Tasks/TASK-XXX.md` | `.gsd/milestones/M001/slices/archive/TASK-XXX.md` (legacy tasks moved there) |
| `docs/Planned_Features/FEAT-XXX.md` | `.gsd/milestones/M001/slices/features/FEAT-XXX.md` |
| `docs/Knowledge_Base/*` | `.gsd/knowledge/*` |
| `docs/Archive/*` | `.gsd/milestones/M001/slices/archive/*` |
| `docs/Skills/*` | `.gsd/milestones/M001/slices/archive/*` |
| `.gsd/PROJECT.md` | `.gsd/state.md` (merged) |
| `.gsd/STATE.md` | `.gsd/state.md` (merged) |
| `.gsd/PI.md` | `.gsd/state.md` (merged) |
| `.gsd/DECISIONS.md` | `.gsd/DECISIONS.md` (kept — append-only) |
| `.gsd/COHABITATION.md` | `.gsd/COHABITATION.md` (kept — shared rules) |
| `.gsd/PI.md` | `.gsd/state.md` (merged) |
| `.gsd/QUICK-REFERENCE.md` | `.gsd/state.md` (merged) |
| `.claude/settings.local.json` | **DELETED** (unused) |
| `docs/README.md` | `.gsd/README.md` (moved + updated) |

---

## Current Structure

```
pz-lua-api-viewer/
├── .git/
├── sources/                    ← Pre-shipped .java for GitHub Pages
├── extract_lua_api.py          ← API extractor
├── lua_api.json                ← Extracted data (~6MB)
├── index.html, app.css, js/    ← Viewer
├── prepare_sources.py          ← Prepares sources for deployment
├── versions/                   ← Versioned API storage
│   ├── versions.json           ← Version manifest
│   └── lua_api_<build>.json    ← Versioned API copies
├── .gsd/                       ← ALL docs now live here!
│   ├── state.md                ← SINGLE SOURCE OF TRUTH (was PROJECT+STATUS+PI)
│   ├── DECISIONS.md            ← Append-only ADRs
│   ├── COHABITATION.md         ← Shared agent rules
│   ├── knowledge/              ← Reference docs (copied from docs/)
│   │   ├── Decisions.md        ← Merged from knowledge base
│   │   ├── Design-Patterns.md  ← Merged
│   │   ├── Domain-Language.md  ← Merged
│   │   ├── Philosophy.md       ← Merged
│   │   ├── Style-Guide.md      ← Merged
│   │   ├── Testing.md          ← Merged
│   │   ├── Bug-Feature-Triage.md ← Merged
│   │   └── README.md           ← Copied
│   ├── milestones/             ← Planning structure
│   │   ├── M001/
│   │   │   ├── M001-CONTEXT.md
│   │   │   ├── M001-ROADMAP.md
│   │   │   ├── slices/
│   │   │   │   ├── S01/S01-PLAN.md   ← Build-time precomputation ✅
│   │   │   │   ├── S02/S02-PLAN.md   ← Middle-click tabs + hover ✅
│   │   │   │   ├── S03/S03-PLAN.md   ← Version selector ✅
│   │   │   │   ├── S04/S04-PLAN.md   ← Javadoc ❌ BLOCKED
│   │   │   │   ├── archive/          ← Legacy completed tasks
│   │   │   │   │   ├── Archive/Bugs/*     ← Old bug format (deprecated)
│   │   │   │   │   ├── Archive/Features/* ← Old feature format (deprecated)
│   │   │   │   │   ├── Archive/Tasks/*   ← Old task format (deprecated)
│   │   │   │   │   ├── Archive/NEXT_SESSION.md
│   │   │   │   │   ├── Archive/README.md
│   │   │   │   │   ├── Archive/TASK-016-javadoc-extraction.md
│   │   │   │   │   ├── Archive/FEAT-010-comments-descriptions.md
│   │   │   │   │   ├── Archive/FEAT-015-mcmaster-speed-clarity.md
│   │   │   │   │   ├── Archive/README.md
│   │   │   │   │   ├── Archive/TASK-016-javadoc-extraction.md
│   │   │   │   │   ├── Skills/*        ← Copied from docs/Skills/
│   │   │   │   │   ├── archive.py      ← Copied from docs/
│   │   │   │   │   └── SKILL-XXX.md   ← Copied from docs/Skills/
│   │   │   │   └── features/         ← Planned features
│   │   │   │       ├── FEAT-010-comments-descriptions.md
│   │   │   │       └── FEAT-015-mcmaster-speed-clarity.md
│   │   │   └── bugs/                 ← Bug tasks
│   ├── errors/                   ← Auto failure tracker
│   ├── knowledge/                ← Reference docs (copied from docs/)
│   ├── README.md                 ← Root readme
│   └── state.md                  ← MAIN working doc (see below)
└── CLAUDE.md                    ← Claude Code config (DO NOT MODIFY)
```

---

## state.md — The Main Working Doc

**Read this first before any session.** It contains:

- What the project is
- Current status & open bugs
- Active tasks with priorities
- Completed features
- Architecture notes
- Running locally / regenerating API
- Working through tasks workflow
- Commit/PR rules
- Cohabitation info
- Directory structure
- Milestone sequence
- Key files list

---

## To Do After Reorg

- [x] Copy `docs/Knowledge_Base/*` → `.gsd/knowledge/`
- [x] Move `docs/Bugs/*` → `.gsd/milestones/M001/slices/bugs/`
- [x] Move `docs/Tasks/*` → `.gsd/milestones/M001/slices/archive/`
- [x] Move `docs/Planned_Features/*` → `.gsd/milestones/M001/slices/features/`
- [x] Move `docs/Archive/*` → `.gsd/milestones/M001/slices/archive/`
- [x] Move `docs/Skills/*` → `.gsd/milestones/M001/slices/archive/`
- [x] Create `.gsd/state.md` (merged from PROJECT+STATUS+PI)
- [x] Update `.gsd/PROJECT.md` references
- [x] Update `.gsd/PI.md` references
- [x] Delete `.claude/settings.local.json` (unused)
- [x] Delete `docs/` folder after verifying no broken links

---

## Next Steps

1. Verify `state.md` is readable and comprehensive ✓
2. Check all legacy task files have been migrated ✓
3. Test that no code references old paths — **TODO**
4. Archive any remaining legacy docs after verification

---

## Quick Checklist for New Sessions

- Read `.gsd/state.md` — find active slice/task
- Read `.gsd/milestones/M001/slices/S0X/S0X-PLAN.md` — understand what to do
- Implement per plan, test with browser screenshots
- Mark task done, archive if applicable
- Update `state.md`
- Commit & push
- Check `.gsd/errors/` — handle any failures encountered
