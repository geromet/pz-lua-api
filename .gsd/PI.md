#WK:# PZ Lua API Viewer — Pi (GSD) Working Instructions
#KM:
#QR:## Cohabitation Notice
#RW:
#RS:This project is shared between **Claude Code** (via `.claude/` and `pz-lua-api-viewer/CLAUDE.md`) and **Pi / GSD** (via `.gsd/`). Both agents work on the same codebase. Never modify `.claude/`, `pz-lua-api-viewer/CLAUDE.md`, or Claude Code's settings.
#SY:
#VV:## Directory Structure — READ THIS FIRST
#XW:
#BX:```
#JP:ProjectZomboid/
#YH:├── projectzomboid/              ← PZ game install dir (NOT a git repo)
#RK:│   ├── sources/                 ← Decompiled Java sources (read by extract_lua_api.py)
#TX:│   └── pz-lua-api-viewer/      ← THE GIT REPO (this project)
#QX:│       ├── .git/
#NN:│       ├── sources/             ← Pre-shipped .java copies for GitHub Pages
#WX:│       ├── extract_lua_api.py
#VY:│       ├── lua_api.json
#VV:│       ├── index.html, app.css, js/
#BH:│       └── .gsd/                ← ALL docs now live here!
#TH:```
#RJ:
#RJ:**Critical rules:**
#WS:- The **only** git repo is `pz-lua-api-viewer/`. The parent `projectzomboid/` must NEVER have a `.git` directory or any project files.
#TT:- Never run `git init` or `git clone` in `projectzomboid/`. Never create project files (`.gitignore`, `.gitattributes`, `README.md`, etc.) at the parent level.
#MN:- CWD for all git operations and file editing must be `pz-lua-api-viewer/`, not the parent.
#BH:- There are **two** `sources/` directories: `projectzomboid/sources/` (full decompiled sources, read by the extractor) and `pz-lua-api-viewer/sources/` (subset shipped to GitHub Pages). Do not confuse them.
#JJ:
#BK:## GSD Structure
#ZR:
#HY:All GSD planning artifacts live in `.gsd/` inside `pz-lua-api-viewer/`:
#SZ:
#VP:```
#WN:.gsd/
#QB:  state.md              ← MAIN working doc (replaces PROJECT.md, STATUS.md, PI.md)
#QK:  DECISIONS.md          ← append-only architectural decisions register
#BM:  milestones/
#XW:    M001/
#KZ:      M001-CONTEXT.md
#NZ:      M001-ROADMAP.md
#WR:      slices/
#QN:        S01/  S01-PLAN.md   ← Build-time precomputation (TASK-017)
#BJ:        S02/  S02-PLAN.md   ← Middle-click tabs + hover preview (FEAT-007, FEAT-014)
#JS:        S03/  S03-PLAN.md   ← Version selector (TASK-018 / FEAT-005)
#MM:        S04/  S04-PLAN.md   ← Javadoc (TASK-016 / FEAT-010) — BLOCKED
#NX:```
#BY:
#QZ:**At the start of every session or task:** Read `state.md` first, then the active slice plan.
#QW:
#HN:## Project Overview
#NM:
#SY:A static web app for browsing the Project Zomboid Lua API. Deployed to GitHub Pages from the `main` branch. All development on feature branch `liability-machine`.
#YJ:
#YN:**NEVER push directly to `main`** — it auto-deploys to GitHub Pages.
#XN:
#VB:## Key Files
#KR:
#TZ:| File | Purpose |
#ZH:|------|---------|
#MS:| `pz-lua-api-viewer/extract_lua_api.py` | Run from `projectzomboid/` to regenerate `lua_api.json` |
#YR:| `pz-lua-api-viewer/lua_api.json` | ~6MB extracted API — written directly by extractor |
#KT:| `pz-lua-api-viewer/index.html` | Single-file HTML shell |
#ZH:| `pz-lua-api-viewer/app.css` | All styles |
#BT:| `pz-lua-api-viewer/js/state.js` | Global state variables |
#VQ:| `pz-lua-api-viewer/js/app.js` | Init, navigation, event setup |
#KY:| `pz-lua-api-viewer/js/class-detail.js` | Detail panel renderer |
#BV:| `pz-lua-api-viewer/js/source-viewer.js` | Source panel renderer + class-ref linking |
#YK:| `pz-lua-api-viewer/js/globals.js` | Globals tab |
#BT:| `pz-lua-api-viewer/sources/` | 971+ pre-shipped .java files |
#PR:
#RQ:## Running Locally
#HV:
#BV:```bash
#YK:cd pz-lua-api-viewer
#NM:python server.py
#RB:# → http://localhost:8765
#RX:```
#BR:
#NB:## Regenerating the API
#JQ:
#BV:```bash
#RM:# From projectzomboid/ (CWD):
#BX:python pz-lua-api-viewer/extract_lua_api.py
#KZ:```
#KR:
#NR:Expected baselines: ~1096 classes, ~19099 methods, ~745 globals.
#QV:After any extractor change, regenerate immediately before committing.
#WY:
#HB:## Working Through the GSD Roadmap (Auto Mode)
#NJ:
#MP:1. **Read `state.md`** — find the active slice and task.
#YQ:2. **Read the active slice plan** (e.g. `.gsd/milestones/M001/slices/S01/S01-PLAN.md`).
#HW:3. **For each task in the slice:**
#WT:   - Read the corresponding legacy task file if one exists (e.g. `archive/Tasks/TASK-XXX-slug.md`) — it has detailed implementation plans.
#KK:   - Implement per the plan.
#VQ:   - After any extractor change: regenerate `lua_api.json`.
#SZ:   - Take browser screenshots to verify (start server first: `cd pz-lua-api-viewer && python server.py`).
#WT:   - Mark the task checkbox in the slice plan (`[ ]` → `[x]`).
#YB:   - If the task resolves a legacy task file: prepend `> **COMPLETED YYYY-MM-DD** — ...` blockquote, then `cd pz-lua-api-viewer && python docs/archive.py docs/Tasks/TASK-NNN-slug.md`.
#XW:   - Update `state.md`.
#BR:4. **When all slice tasks are complete:**
#ZM:   - Write a slice summary to `.gsd/milestones/M001/slices/SXX/SXX-SUMMARY.md`.
#XM:   - Mark the slice checkbox in `M001-ROADMAP.md`.
#JQ:   - Update `state.md` to point at the next slice.
#ZP:   - Commit and push: `cd pz-lua-api-viewer && git add -A && git commit -m "..." --trailer "Co-Authored-By: Pi (GSD) <noreply@gsd.dev>" && git push`
#ZV:5. **If a task is blocked:** note the blocker in `state.md` and the slice plan, skip to the next unblocked task.
#JX:6. **Creating new bugs:** file in `archive/Bugs/BUG-NNN-slug.md`. Check `archive/Knowledge_Base/Bug-Feature-Triage.md` first.
#WK:7. **Creating new planned features:** file in `archive/Planned_Features/FEAT-NNN-slug.md` and add to the appropriate future slice plan.
#NR:8. **After discovering new work:** add to the appropriate GSD slice plan or create a new slice/milestone as needed.
#YB:
#QT:## Testing
#XB:
#PV:- **Before committing any frontend change:** start the server and take a browser screenshot.
#YS:- **After extractor changes:** run the extractor; check output baselines (±10% triggers investigation).
#RR:- See `knowledge/Testing.md` for full verification procedures.
#WP:
#PR:## Commit / PR Rules
#BM:
#KB:- Branch from `liability-machine` for new work.
#HM:- One logical change per commit.
#HM:- Never `--no-verify` or `--force` push.
#MW:- Co-author tag: `Co-Authored-By: Pi (GSD) <noreply@gsd.dev>`
#VM:- PR `liability-machine → main` when a batch of work is shippable (ask user first).
#QR:

---

## Reorganization Complete (2026-03-13)

- `docs/` folder deleted — all docs consolidated into `.gsd/`
- `state.md` is now the single source of truth (merged from PROJECT+STATUS+PI)
- Legacy task files moved to `.gsd/milestones/M001/slices/archive/Tasks/`
- Reference knowledge base copied to `.gsd/knowledge/`
- Error tracker created at `.gsd/errors/` for autonomous failure tracking

All working instructions have been updated to use `state.md` instead of the old scattered docs.
