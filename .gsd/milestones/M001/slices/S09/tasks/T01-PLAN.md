---
estimated_steps: 6
estimated_files: 4
skills_used: []
---

# T01: Split API JSON and update loader

**Slice:** S09 — Load Performance
**Milestone:** M001

## Description

Produce `lua_api_index.json` (lightweight class list for fast boot) and `lua_api_detail/<fqn>.json` (one file per class, full data). Rewire `js/app.js`, `js/search-index.js`, and `js/class-detail.js` to use the split format. The existing `lua_api.json` remains as a fallback for environments that haven't run the splitter.

## Steps

1. Write `scripts/split_api.py`: load `lua_api.json`, write `lua_api_index.json` (same top-level structure but each class entry contains only: `set_exposed`, `lua_tagged`, `is_enum`, `source_file`, `simple_name`, `method_count` = `len(methods or [])`, `field_count` = `len(fields or [])`). Write `lua_api_detail/<fqn>.json` for each class (full class object). Preserve top-level `global_functions`, `_source_index`, `_class_by_simple_name`, `_source_only_paths` in the index file.
2. In `js/app.js` `loadApi()`: try fetching `lua_api_index.json` first; on 404 or failure fall back to `lua_api.json`. Set a module-level flag `window._apiSplit = true/false` so detail code knows which mode is active.
3. In `js/search-index.js`: confirm it only needs `set_exposed`, `lua_tagged`, `is_enum`, `simple_name` per class — these are present in both index and full JSON. No change needed if so; if full fields are used, guard them with optional chaining.
4. In `js/class-detail.js` `renderClassDetail(fqn)`: before rendering, check if `API.classes[fqn]` has `methods` defined (distinguishes summary from full). If not and `window._apiSplit`, fetch `./lua_api_detail/${encodeURIComponent(fqn)}.json`, merge the result into `API.classes[fqn]`, then render. Set `panel.dataset.detailState = 'loading'` before fetch; set `'error'` on fetch failure with a human-readable error message in panel innerHTML.
5. Run `python3 scripts/split_api.py` and verify output counts.
6. Serve the viewer locally (`python3 -m http.server 8000`) and open in browser to confirm class list loads and clicking a class fetches detail without errors.

## Must-Haves

- [ ] `scripts/split_api.py` runs without error on the current `lua_api.json`
- [ ] `lua_api_index.json` is produced and weighs significantly less than `lua_api.json` (target <500KB)
- [ ] `lua_api_detail/` contains one file per class (1096+ files)
- [ ] `js/app.js` falls back to `lua_api.json` if index is absent
- [ ] `js/class-detail.js` shows `data-detail-state="loading"` then `"ready"` when a class is clicked in split mode
- [ ] `js/class-detail.js` shows `data-detail-state="error"` on fetch failure

## Verification

- `python3 scripts/split_api.py`
- `test -f lua_api_index.json`
- `python3 -c "import json,pathlib; idx=json.load(open('lua_api_index.json')); assert len(idx['classes'])>1000,'index class count'; assert len(list(pathlib.Path('lua_api_detail').glob('*.json')))>1000,'detail file count'"`
- `python3 -c "import os; assert os.path.getsize('lua_api_index.json') < os.path.getsize('lua_api.json')*0.15, 'index should be <15% of full'"`

## Observability Impact

- Signals added/changed: `window._apiSplit` flag; `#detail-panel[data-detail-state]` now transitions `loading → ready | error` for lazy fetches
- How a future agent inspects this: check `document.getElementById('detail-panel').dataset.detailState` in browser console; network tab shows individual `lua_api_detail/*.json` requests
- Failure state exposed: `data-detail-state="error"` with error message text in detail panel

## Inputs

- `lua_api.json` — source of truth for all class data
- `js/app.js` — current loader to modify
- `js/search-index.js` — current search index builder
- `js/class-detail.js` — current detail renderer to add lazy-fetch to

## Expected Output

- `scripts/split_api.py` — new script that produces split format
- `lua_api_index.json` — generated lightweight index
- `lua_api_detail/` — generated directory with per-class JSON files
- `js/app.js` — modified to load index JSON
- `js/class-detail.js` — modified to lazy-fetch per-class detail
