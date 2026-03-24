---
estimated_steps: 5
estimated_files: 3
skills_used: []
---

# T03: Write S09 Playwright tests and run full suite

**Slice:** S09 — Load Performance
**Milestone:** M001

## Description

Write a Playwright test module that verifies the split-JSON load path and per-class lazy fetch. Register it in the consolidated test runner. Run the full test suite to confirm no regressions.

## Steps

1. Write `.gsd/test/s09_load_perf.py` with at least three pytest-playwright tests:
   - **test_index_loads_not_full_json**: intercept all fetch requests; assert `lua_api_index.json` is fetched and `lua_api.json` is NOT fetched on boot (when `lua_api_index.json` is present)
   - **test_detail_lazy_fetch**: after page loads, click a class in the sidebar; assert a request to `lua_api_detail/*.json` is made and `#detail-panel[data-detail-state]` transitions to `"ready"`
   - **test_detail_error_state**: simulate a failing detail fetch (e.g., rename a detail file temporarily or intercept and abort); assert `#detail-panel[data-detail-state]` becomes `"error"` and the panel shows an error message
   Use the existing pattern from `s08_navigation_state.py` for server startup and page fixture.
2. Add `import .gsd/test/s09_load_perf` (or equivalent pytest discovery path) in `.gsd/test/run.py` so the consolidated runner picks it up.
3. Run `python3 -m pytest .gsd/test/s09_load_perf.py -v` and fix any failures.
4. Run `python3 -m pytest .gsd/test/run.py -v` to confirm no regressions in S02, S07, S08 tests.
5. If any existing tests need minor tolerance updates (e.g., new network requests in the intercept list), update them — but do not weaken their assertions.

## Must-Haves

- [ ] `s09_load_perf.py` exists with at least 3 tests
- [ ] `test_index_loads_not_full_json` passes (proves split format is used)
- [ ] `test_detail_lazy_fetch` passes (proves on-demand fetch works)
- [ ] `test_detail_error_state` passes (proves failure visibility)
- [ ] Full suite (`run.py`) passes with no regressions

## Verification

- `test -f .gsd/test/s09_load_perf.py`
- `python3 -m pytest .gsd/test/s09_load_perf.py -v`
- `python3 -m pytest .gsd/test/run.py -v`

## Inputs

- `.gsd/test/s08_navigation_state.py` — reference for server fixture pattern
- `.gsd/test/run.py` — to register the new module
- `lua_api_index.json` — must exist (produced by T01)
- `lua_api_detail/` — must exist (produced by T01)
- `js/class-detail.js` — `data-detail-state` contract (modified in T01)

## Expected Output

- `.gsd/test/s09_load_perf.py` — new Playwright test module
- `.gsd/test/run.py` — updated to include S09 tests
