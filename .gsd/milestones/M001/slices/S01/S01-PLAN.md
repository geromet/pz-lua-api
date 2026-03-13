# S01: Unblocked Improvements

**Goal:** Ship TASK-017 (build-time precomputation) and TASK-018-prerequisite work, plus FEAT-004 (resizable panels polish if needed). These are self-contained improvements with no inter-dependency on new tab features.
**Demo:** Local server at http://localhost:8765 loads with precomputed class maps; panels are resizable.

## Must-Haves

- `lua_api.json` contains `_class_by_simple_name` and `_source_only_paths` fields (TASK-017)
- `init()` in `app.js` uses precomputed fields, falls back if absent
- Existing navigation, tabs, search, globals all work unchanged after extractor change
- Browser screenshot confirms no regressions

## Verification

- `python pz-lua-api-viewer/extract_lua_api.py` completes without error
- `lua_api.json` contains `_class_by_simple_name` key: `python -c "import json; d=json.load(open('pz-lua-api-viewer/lua_api.json')); print('ok' if '_class_by_simple_name' in d else 'MISSING')"`
- Browser screenshot of http://localhost:8765 showing class list loaded and a class detail panel open

## Tasks

- [ ] **T01: Build-time precomputation** `est:1h`
  - Why: Eliminates `Object.keys()` loop over 1096 FQNs on every page load; implements FEAT-008 / TASK-017
  - Files: `pz-lua-api-viewer/extract_lua_api.py`, `pz-lua-api-viewer/js/app.js`, `pz-lua-api-viewer/js/state.js`
  - Do: Follow the plan in `pz-lua-api-viewer/docs/Tasks/TASK-017-build-time-precomputation.md`. Add `_class_by_simple_name` and `_source_only_paths` to extractor output. Update `init()` to use them with backwards-compatible fallback. Run extractor. Verify JSON.
  - Verify: `python -c "import json; d=json.load(open('pz-lua-api-viewer/lua_api.json')); assert '_class_by_simple_name' in d; print('PASS')"` then browser screenshot
  - Done when: extractor runs clean, JSON has precomputed fields, browser loads without console errors, class linking works identically

- [ ] **T02: Commit S01** `est:10m`
  - Why: Lock in S01 changes before starting S02
  - Files: all modified files in pz-lua-api-viewer/
  - Do: `cd pz-lua-api-viewer && git add -A && git commit -m "feat: build-time precomputation for classBySimpleName and sourceOnlyPaths (FEAT-008)" --trailer "Co-Authored-By: Pi (GSD) <noreply@gsd.dev>"` then `git push`
  - Verify: `git log --oneline -3` shows the commit; `git status` is clean
  - Done when: pushed to `liability-machine`

## Files Likely Touched

- `pz-lua-api-viewer/extract_lua_api.py`
- `pz-lua-api-viewer/js/app.js`
- `pz-lua-api-viewer/js/state.js`
- `pz-lua-api-viewer/lua_api.json` (regenerated)
