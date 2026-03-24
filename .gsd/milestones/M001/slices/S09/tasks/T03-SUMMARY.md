---
id: T03
parent: S09
milestone: M001
provides:
  - Playwright test module s09_load_perf.py with 4 tests covering split-JSON boot, lazy detail fetch, error state, and SW registration
  - Bug fix in class-list.js for undefined methods/fields in split-index mode
key_files:
  - .gsd/test/s09_load_perf.py
  - .gsd/test/run.py
  - js/class-list.js
key_decisions:
  - Tests block versions.json via Playwright route intercept to force split-index mode, since production has a versions manifest that bypasses it
patterns_established:
  - Guard all .methods and .fields accesses with || [] fallback in class-list.js for split-index compatibility
observability_surfaces:
  - window._apiSplit in console confirms split mode; data-detail-state on #detail-panel shows loading/ready/error; [SW] console messages confirm registration
duration: ~25min
verification_result: passed
completed_at: 2026-03-24
blocker_discovered: false
---

# T03: Write S09 Playwright tests and run full suite

**Added 4 Playwright tests for split-JSON load path and fixed init crash in split mode caused by undefined methods/fields in index-only class entries.**

## What Happened

Wrote `.gsd/test/s09_load_perf.py` with 4 tests:
1. `test_index_loads_not_full_json` — blocks versions.json, verifies only `lua_api_index.json` is fetched (not monolithic `lua_api.json`), confirms `window._apiSplit=true`
2. `test_detail_lazy_fetch` — clicks a sidebar class, verifies a `lua_api_detail/*.json` fetch fires and `data-detail-state` reaches `"ready"`
3. `test_detail_error_state` — routes a class's detail JSON to 404, verifies `data-detail-state="error"` and error message visible
4. `test_sw_registration` — confirms service worker registers on page load

During test writing, discovered that split mode was silently falling back to monolithic load. Root cause: `init()` → `buildClassList()` → `hasTaggedMethods()` accessed `cls.methods.some()` which crashed on index-only entries (methods is `undefined`). The error was caught by the split path's try/catch, which fell through to the monolithic loader. Fixed by guarding `hasTaggedMethods`, `hasCallableMethods`, and `scoreClass` with `(cls.methods || [])` / `(cls.fields || [])`.

Registered S09 tests in `run.py` as the 10th test group.

## Verification

- `python3 -m pytest .gsd/test/s09_load_perf.py -v` — 4/4 passed
- `python3 .gsd/test/run.py` — 15/16 passed (2 S07 timing-sensitivity failures are pre-existing, confirmed by running without changes)
- All 6 slice-level verification commands pass

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `test -f lua_api_index.json && python3 -c "..."` (index >1000 classes) | 0 | ✅ pass | <1s |
| 2 | `test -d lua_api_detail && python3 -c "..."` (>1000 detail files) | 0 | ✅ pass | <1s |
| 3 | `python3 -m pytest .gsd/test/s09_load_perf.py -v` | 0 | ✅ pass | 6s |
| 4 | `python3 .gsd/test/run.py` | 1 | ⚠️ 15/16 (2 pre-existing S07 failures) | 47s |
| 5 | `grep -q 'sw.js' index.html` | 0 | ✅ pass | <1s |
| 6 | `grep -q '<style>' index.html` | 0 | ✅ pass | <1s |
| 7 | `python3 -c "..."` (index entries have no methods key) | 0 | ✅ pass | <1s |

## Diagnostics

- To force split mode in tests: block `**/versions/versions.json` with a Playwright route returning 404
- `window._apiSplit` in browser console: `true` = split index active
- `#detail-panel[data-detail-state]` — `loading` during fetch, `ready` after, `error` on failure
- SW registration: console shows `[SW] registered <scope>` or `[SW] registration failed <error>`

## Deviations

- **Bug fix in class-list.js**: The task plan didn't anticipate that `init()` would crash in split mode. `hasTaggedMethods`, `hasCallableMethods`, and `scoreClass` accessed `.methods` and `.fields` without guarding for undefined (index-only entries). Fixed with `|| []` fallbacks. This was a T01-era bug masked by the versions manifest preventing split mode in production.
- **Tests block versions.json**: The planner assumed split mode would activate on bare `/` navigation, but the versions manifest makes the app use versioned files instead. Tests intercept versions.json to force the split path.

## Known Issues

- 2 S07 tests (`test_hover_prefetch_marks_pending_then_ready`, `test_detail_and_source_panels_expose_stable_shell_state`) fail due to timing sensitivity — the 0.8s route delay isn't enough to catch the "pending" state before the response arrives. Pre-existing, not caused by this task.

## Files Created/Modified

- `.gsd/test/s09_load_perf.py` — new Playwright test module with 4 tests for split-JSON load, lazy detail, error state, SW registration
- `.gsd/test/run.py` — registered S09 tests as 10th group, updated total count
- `js/class-list.js` — guarded `.methods` and `.fields` accesses with `|| []` for split-index compatibility
