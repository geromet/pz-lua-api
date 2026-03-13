# Project

## What This Is

A static single-page web app for browsing the Project Zomboid Lua API, deployed to GitHub Pages. It extracts class/method/field/global data from decompiled `.java` sources via a Python script (`extract_lua_api.py`), stores the result in `lua_api.json`, and serves a rich browser UI for PZ modders to explore the API.

GitHub repo: https://github.com/geromet/PZJavaDocs
Live at: https://geromet.github.io/PZJavaDocs/

## Core Value

Modders can quickly look up any PZ class, see its methods/fields/inheritance, and jump to source — without installing anything or leaving the browser.

## Current State

The viewer is working and deployed. Recent completed work:
- Tab bar system (TASK-012) — multi-class tab browsing
- Search highlight + clear buttons (TASK-013)
- Sticky headers + globals fold memory (TASK-014)
- Wildcard import fallback for class-ref linking (TASK-015)
- TASK-016 (Javadoc extraction) blocked — decompiled sources have no Javadoc blocks

Active branch: `liability-machine`. Never push to `main` (auto-deploys to GitHub Pages).

## Architecture / Key Patterns

- **No build step.** Plain ES6 modules in `pz-lua-api-viewer/js/`. `index.html` loads each `<script src>`.
- **Navigation** — manual history stack (`navHistory[]` in `state.js`). `navPush` no-ops when `_restoringState = true`.
- **Source linking** — `linkClassRefs()` in `source-viewer.js` emits `<a>` tags for class tokens.
- **`_extends_map`** — bridges non-API classes in the inheritance chain (not in `all_classes`).
- **`_interface_extends`** — interface FQN → [parent FQNs], for transitive implements display.
- **`_source_index`** — simple class name → relative `.java` path for non-API classes.
- **Event delegation** — never attach listeners inside render functions.
- **State** — lives in `state.js`; rendering functions return HTML strings.
- **Extractor** — `extract_lua_api.py` run from `projectzomboid/` root, writes to `pz-lua-api-viewer/lua_api.json`.

Key files:
- `pz-lua-api-viewer/extract_lua_api.py` — Python API extractor
- `pz-lua-api-viewer/lua_api.json` — ~6MB extracted API data
- `pz-lua-api-viewer/index.html` — HTML shell
- `pz-lua-api-viewer/app.css` — all styles
- `pz-lua-api-viewer/js/` — ES6 modules (state, app, class-detail, source-viewer, globals, class-list)
- `pz-lua-api-viewer/sources/` — 971+ pre-shipped .java files for GitHub Pages
- `pz-lua-api-viewer/docs/` — all project docs (shared with Claude Code)

## Running Locally

```bash
cd pz-lua-api-viewer && python server.py
# → http://localhost:8765
```

## Regenerating the API

```bash
cd projectzomboid/
python pz-lua-api-viewer/extract_lua_api.py
```

Expected output baselines: ~1096 classes, ~19099 methods, ~745 global functions.

## Cohabitation

This project is shared between GSD (Pi) and Claude Code. GSD owns `.gsd/`. Claude Code owns `.claude/` and `pz-lua-api-viewer/CLAUDE.md`. Both share `pz-lua-api-viewer/docs/`. Full rules in `.gsd/COHABITATION.md`.

## Milestone Sequence

- [x] M000: Foundation — working viewer with tabs, source linking, and globals (pre-GSD, complete)
- [ ] M001: Feature Completion — ship TASK-016–018 + planned features FEAT-004, FEAT-006 family, FEAT-009, FEAT-013, FEAT-014
