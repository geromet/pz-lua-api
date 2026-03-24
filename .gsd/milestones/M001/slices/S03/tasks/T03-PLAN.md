---
estimated_steps: 4
estimated_files: 2
skills_used: []
---

# T03: Add diagnostic data attribute and Playwright tests for version selector

**Slice:** S03 — Version Selector
**Milestone:** M001

## Description

S03's implementation (extractor versioned output in T01 + frontend version dropdown in T02) is already merged but has no automated tests and no diagnostic data attributes. This task adds a `data-version-active` attribute to `#version-select` for runtime inspection and writes a Playwright test suite covering happy paths and failure fallback.

## Steps

1. In `js/app.js`, find `setupVersionDropdown()` (around line 78). After populating the dropdown options and before the `change` event listener, add `sel.dataset.versionActive = currentId;`. In the early-return branch (manifest absent or ≤1 entry), add `delete sel.dataset.versionActive;` before the `return`.
2. Create `.gsd/test/s03_version_selector.py` following the same patterns as `.gsd/test/s09_load_perf.py` — module-scoped server fixture (`subprocess.Popen` + readiness poll), per-test Playwright page fixture, route intercepts for `versions.json`.
3. Write these test cases:
   - **test_single_version_dropdown_hidden**: Load the real app (which has only 1 entry in `versions/versions.json`). Assert `#version-select` is hidden (`display: none`) and has no `data-version-active` attribute.
   - **test_multi_version_dropdown_visible**: Intercept `**/versions/versions.json` to return a 2-entry manifest (use real `r964` + a fake `r900` entry, both pointing at `versions/lua_api_r964.json`). Assert `#version-select` is visible and has 2 `<option>` elements. Assert `data-version-active` equals the first entry's id (`r964`).
   - **test_v_param_selects_version**: Intercept `**/versions/versions.json` with same 2-entry manifest. Navigate to `?v=r900`. Assert the dropdown's selected value is `r900` and `data-version-active` is `r900`.
   - **test_versions_json_404_graceful_fallback**: Intercept `**/versions/versions.json` to return 404. Assert app still boots (`#class-list` has children or `#loading` is hidden), dropdown is hidden, no `data-version-active` attribute present.
4. Run `pytest .gsd/test/s03_version_selector.py -v` and fix any failures until all tests pass.

## Must-Haves

- [ ] `data-version-active` attribute set on `#version-select` when version manifest has ≥2 entries
- [ ] `data-version-active` removed/absent when no manifest or single-entry manifest
- [ ] At least 4 Playwright tests covering: single version hidden, multi-version visible, `?v=` param, 404 fallback
- [ ] All tests pass

## Verification

- `pytest .gsd/test/s03_version_selector.py -v` — all tests pass
- `grep -q 'versionActive' js/app.js` — attribute is wired in source

## Observability Impact

- Signals added: `#version-select[data-version-active]` — reflects active version ID, queryable via `document.querySelector('#version-select').dataset.versionActive`
- How a future agent inspects this: Check the data attribute in Playwright or browser console to determine which API version is loaded
- Failure state exposed: Absence of `data-version-active` means no versions manifest was loaded (single-file mode or fetch failed)

## Inputs

- `js/app.js` — contains `setupVersionDropdown()` function to modify (around line 78)
- `versions/versions.json` — real manifest with single r964 entry, used by tests
- `.gsd/test/s09_load_perf.py` — reference for server fixture, page fixture, and route intercept patterns
- `.gsd/test/config.py` — shared test configuration (SERVER_URL, paths)
- `server/serve.py` — local dev server started by test fixture

## Expected Output

- `js/app.js` — modified: `setupVersionDropdown()` sets/removes `data-version-active`
- `.gsd/test/s03_version_selector.py` — new: 4+ Playwright tests for version selector
