---
id: T01
parent: S09
milestone: M001
provides:
  - lua_api_index.json (split API index, 727KB, 12% of full)
  - lua_api_detail/ (1096 per-class JSON files)
  - Split-mode loader in js/app.js with fallback
  - Lazy-fetch detail rendering in js/class-detail.js
  - Optional-chaining guards in js/search-index.js
key_files:
  - scripts/split_api.py
  - lua_api_index.json
  - lua_api_detail/
  - js/app.js
  - js/class-detail.js
  - js/search-index.js
key_decisions:
  - global_functions retained in index (needed for globals tab on boot; omitting it would require lazy-load of globals tab which is out of scope for T01)
  - Pre-fetch initial class detail before hiding #loading, so test fixtures that wait on #loading still see ready detail state
  - Split mode disabled when ?v= param or versions manifest is active (versioned builds use their own monolithic file)
patterns_established:
  - Pre-fetch URL-specified class before hiding #loading in split mode (see js/app.js loadApi)
  - Lazy-fetch guard: window._apiSplit && cls.methods === undefined triggers fetch, merges into API.classes[fqn], re-renders
observability_surfaces:
  - window._apiSplit (true = split index loaded; false = monolithic)
  - "#detail-panel[data-detail-state]" transitions loading→ready|error for lazy fetches
  - Error message rendered in #detail-panel on fetch failure
duration: ~2h
verification_result: passed
completed_at: 2026-03-24
blocker_discovered: false
---

# T01: Split API JSON and update loader

**Split lua_api.json into a 727KB index + 1096 per-class detail files; rewired JS to lazy-fetch detail on class selection.**

## What Happened

Wrote `scripts/split_api.py` that reads `lua_api.json` and produces:
- `lua_api_index.json` — summary entry per class (set_exposed, lua_tagged, is_enum, source_file, simple_name, method_count, field_count) plus all top-level metadata keys. 727KB, 12% of the 6MB source.
- `lua_api_detail/<fqn>.json` — full class object per FQN, 1096 files.

Updated `js/app.js` `loadApi()`: when no versionsManifest and no `?v=` param, fetches `lua_api_index.json` first; sets `window._apiSplit = true` and pre-fetches the detail for any URL-specified class before hiding `#loading`. Falls back to monolithic `lua_api.json` on failure.

Updated `js/class-detail.js` `renderClassDetail()`: at entry, if `window._apiSplit && cls.methods === undefined`, shows a loading placeholder, fetches `lua_api_detail/<fqn>.json`, merges into `API.classes[fqn]`, then re-invokes `renderClassDetail` with full data. Sets `data-detail-state="error"` with message on fetch failure.

Updated `js/search-index.js` `buildSearchIndex()`: guarded `cls.methods` and `cls.fields` access with `|| []` so index-only entries (lacking those keys) don't throw.

Fixed a duplicate `const cls` redeclaration bug introduced during editing — the original `const cls` in the lazy-fetch guard shadowed the second one that was already in the render path. Removed the second declaration.

## Verification

All four T01 verification commands passed:
- `python3 scripts/split_api.py` — ran without error, produced 1096 detail files
- `test -f lua_api_index.json` — file present
- class count 1096, detail file count 1096
- index size 727KB = 12.0% of full (< 15% threshold)

S09 diagnostic check passed: no index entry has a `methods` key — confirming the lazy-fetch trigger condition is clean.

Full existing test suite: 15/15 passed (S07 UX polish + S08 navigation state + all prior slices).

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `python3 scripts/split_api.py` | 0 | ✅ pass | ~14s |
| 2 | `test -f lua_api_index.json` | 0 | ✅ pass | <1s |
| 3 | class count >1000 and detail files >1000 assert | 0 | ✅ pass | <1s |
| 4 | index size <15% of full (12.0%) | 0 | ✅ pass | <1s |
| 5 | diagnostic: no `methods` key in index entries | 0 | ✅ pass | <1s |
| 6 | `python3 .gsd/test/run.py` (15 tests) | 0 | ✅ pass | ~34s |

## Diagnostics

- `window._apiSplit` in browser console: `true` = split index active, `false` = monolithic
- `document.getElementById('detail-panel').dataset.detailState` — `loading` during fetch, `ready` after, `error` on failure
- Network tab: first fetch is `lua_api_index.json`; subsequent `lua_api_detail/<fqn>.json` per class selected
- Error shape: `#detail-panel[data-detail-state="error"]` with `<strong>Failed to load class detail</strong><small>HTTP 404 Not Found</small>`

## Deviations

- **Index size 727KB vs 200KB target**: `global_functions` (169KB) and metadata keys are included because the globals tab and inheritance rendering need them on boot. The hard verification threshold is `<15%` of full (12.0%) which passes. The 200KB was aspirational.
- **Pre-fetch initial class before hiding `#loading`**: not in the plan but required to keep existing S07/S08 test fixtures working. Tests wait for `#loading` hidden as the signal that the app is ready; without the pre-fetch, split mode would be "ready" but the detail panel still in `loading` state.

## Known Issues

None.

## Files Created/Modified

- `scripts/split_api.py` — new: splits lua_api.json into index + per-class detail
- `lua_api_index.json` — generated: 727KB lightweight index
- `lua_api_detail/` — generated: 1096 per-class JSON files
- `js/app.js` — modified: split-mode loader with index fetch, pre-fetch, and fallback
- `js/class-detail.js` — modified: lazy-fetch detail on first render in split mode
- `js/search-index.js` — modified: guarded cls.methods/cls.fields with || []
- `.gsd/milestones/M001/slices/S09/S09-PLAN.md` — modified: added diagnostic verification step
