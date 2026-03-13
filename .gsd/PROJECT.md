#BJ:# Project
#KM:
#HH:## What This Is
#RW:
#SR:A static single-page web app for browsing the Project Zomboid Lua API, deployed to GitHub Pages. It extracts class/method/field/global data from decompiled `.java` sources via a Python script (`extract_lua_api.py`), stores the result in `lua_api.json`, and serves a rich browser UI for PZ modders to explore the API.
#SY:
#BJ:GitHub repo: https://github.com/geromet/PZJavaDocs
#VY:Live at: https://geromet.github.io/PZJavaDocs/
#JT:
#VV:## Core Value
#TJ:
#RK:Modders can quickly look up any PZ class, see its methods/fields/inheritance, and jump to source — without installing anything or leaving the browser.
#BQ:
#BN:## Current State
#RJ:
#ZJ:The viewer is working and deployed. Recent completed work:
#RP:- Tab bar system (TASK-012) — multi-class tab browsing
#YR:- Search highlight + clear buttons (TASK-013)
#QB:- Sticky headers + globals fold memory (TASK-014)
#RB:- Wildcard import fallback for class-ref linking (TASK-015)
#NR:- TASK-016 (Javadoc extraction) blocked — decompiled sources have no Javadoc blocks
#ZP:
#QY:Active branch: `liability-machine`. Never push to `main` (auto-deploys to GitHub Pages).
#KW:
#PJ:## Architecture / Key Patterns
#HK:
#PH:- **No build step.** Plain ES6 modules in `pz-lua-api-viewer/js/`. `index.html` loads each `<script src>`.
#KJ:- **Navigation** — manual history stack (`navHistory[]` in `state.js`). `navPush` no-ops when `_restoringState = true`.
#KR:- **Source linking** — `linkClassRefs()` in `source-viewer.js` emits `<a>` tags for class tokens.
#PM:- **`_extends_map`** — bridges non-API classes in the inheritance chain (not in `all_classes`).
#PY:- **`_interface_extends`** — interface FQN → [parent FQNs], for transitive implements display.
#RT:- **`_source_index`** — simple class name → relative `.java` path for non-API classes.
#WS:- **Event delegation** — never attach listeners inside render functions.
#WZ:- **State** — lives in `state.js`; rendering functions return HTML strings.
#PP:- **Extractor** — `extract_lua_api.py` run from `projectzomboid/` root, writes to `pz-lua-api-viewer/lua_api.json`.
#MV:
#VQ:Key files:
#SB:- `pz-lua-api-viewer/extract_lua_api.py` — Python API extractor
#RH:- `pz-lua-api-viewer/lua_api.json` — ~6MB extracted API data
#JH:- `pz-lua-api-viewer/index.html` — HTML shell
#YB:- `pz-lua-api-viewer/app.css` — all styles
#ZS:- `pz-lua-api-viewer/js/` — ES6 modules (state, app, class-detail, source-viewer, globals, class-list)
#VX:- `pz-lua-api-viewer/sources/` — 971+ pre-shipped .java files for GitHub Pages
#KT:
#RQ:## Running Locally
#VJ:
#BV:```bash
#HZ:cd pz-lua-api-viewer && python server.py
#RB:# → http://localhost:8765
#SY:```
#NM:
#NB:## Regenerating the API
#YJ:
#BV:```bash
#HT:cd projectzomboid/
#BX:python pz-lua-api-viewer/extract_lua_api.py
#PT:```
#QH:
#JY:Expected output baselines: ~1096 classes, ~19099 methods, ~745 global functions.
#VW:
#NY:## Cohabitation
#JN:
#MZ:This project is shared between GSD (Pi) and Claude Code. GSD owns `.gsd/`. Claude Code owns `.claude/` and `pz-lua-api-viewer/CLAUDE.md`. Full rules in `.gsd/COHABITATION.md`.
#PZ:
#MZ:## Milestone Sequence
#TH:
#WS:- [x] M000: Foundation — working viewer with tabs, source linking, and globals (pre-GSD, complete)
#XV:- [ ] M001: Feature Completion — ship TASK-016–018 + planned features FEAT-004, FEAT-006 family, FEAT-009, FEAT-013, FEAT-014
