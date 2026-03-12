# Next Session — Task Index

Work through these in order. Each file is self-contained.

| # | File | Task | Status | Touches |
|---|------|------|--------|---------|
| 1 | `task-01-source-coverage.md` | Fix missing classes + deep call linking | ✅ Done (phase 1) | `extract_lua_api.py`, `prepare_sources.py`, `js/app.js`, `js/state.js`, `js/source-viewer.js` |
| 2 | `task-02-nav-resilience.md`  | Refactor navigation state to be resilient | — | `js/state.js`, `js/app.js`, `js/globals.js` |
| 3 | `task-03-inheritance.md`     | Inheritance display (extends/implements/subclasses/inherited methods) | — | `extract_lua_api.py`, `js/class-detail.js`, `app.css` |

## Task 01 — What was done
- Added `build_source_index()` to `extract_lua_api.py` — scans all .java files under SRC_ROOT
  (skipping pz-lua-api-viewer/) and emits `_source_index: {SimpleName → relative/path.java}` in JSON
- `_source_index` covers 2105 source-linkable classes NOT already in the API
- `prepare_sources.py` now also copies `_source_index` files (total: ~3889 source files)
- `js/state.js`: added `sourceOnlyPaths = {}` map
- `js/app.js init()`: merges `API._source_index` into `sourceOnlyPaths`
- `js/source-viewer.js linkClassRefs()`: links source-only refs with `data-source-path` attribute
- `js/source-viewer.js showSourceByPath()`: renders source-only files in the source panel
- `js/app.js click handler`: routes `data-source-path` clicks to `showSourceByPath()`

## Task 01 — Phase 2 (not yet done)
See task-01-source-coverage.md Problem B (wildcard import fallback) and Problem C (method-level linking).
These are lower priority — phase 1 covers the main class-linking gap.

Start with task 02 or 03 next.
