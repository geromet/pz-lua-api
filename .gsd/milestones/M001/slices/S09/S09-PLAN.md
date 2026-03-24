# S09: Load Performance

**Goal:** Split `lua_api.json` (6MB) into an index + per-class files; inline critical CSS; add a service worker for cached repeat visits.
**Demo:** First visit fetches ~200KB index and renders the class list. Clicking a class fetches its detail JSON on demand. Repeat visit loads everything from the service worker cache. A Playwright test verifies index-only boot and per-class lazy fetch.

## Must-Haves

- `lua_api.json` split: `lua_api_index.json` (~200KB, class list + metadata) and `lua_api_detail/<fqn>.json` per class (1096 files)
- Build script (`scripts/extract_lua_api.py` or a new `scripts/split_api.py`) generates both formats from existing `lua_api.json`
- `js/app.js` loads index JSON on boot; full `API.classes[fqn]` object fetched per-class on demand
- `js/search-index.js` builds index from the lighter index JSON (no per-method data needed)
- `js/class-detail.js` fetches per-class JSON when detail is needed; shows a loading state
- Critical CSS (header, sidebar skeleton, loading spinner) inlined in `<head>`; `app.css` loaded async to avoid render-blocking
- `sw.js` service worker: cache-first for index JSON + static assets; network-first for per-class detail JSON
- All existing Playwright tests pass unchanged

## Proof Level

- This slice proves: integration
- Real runtime required: yes
- Human/UAT required: no

## Verification

- `test -f lua_api_index.json && python3 -c "import json; d=json.load(open('lua_api_index.json')); assert len(d['classes']) > 1000"`
- `test -d lua_api_detail && python3 -c "import pathlib; assert len(list(pathlib.Path('lua_api_detail').glob('*.json'))) > 1000"`
- `python3 -m pytest .gsd/test/s09_load_perf.py -v` — Playwright tests: boot with index only, detail lazy-fetch, SW registration check
- `python3 -m pytest .gsd/test/run.py -v` — all existing tests still pass
- `grep -q 'sw.js' index.html` — SW registration wired
- `grep -q '<style>' index.html` — critical CSS inlined
- `python3 -c "import json; d=json.load(open('lua_api_index.json')); [d['classes'][k] for k in list(d['classes'])[:5]]; print('index entries have no methods key:', all('methods' not in v for v in d['classes'].values()))"` — confirms index entries are summaries (failure-path diagnostic: if methods present, lazy-fetch won't trigger)

## Observability / Diagnostics

- Runtime signals: detail panel emits `data-detail-state="loading|ready|error"` (already exists); SW registration logged to `console.info` with cache name
- Inspection surfaces: browser DevTools → Network tab shows `lua_api_index.json` as first fetch, per-class detail fetches on class selection; Application → Service Workers shows SW active
- Failure visibility: if a per-class fetch fails, `data-detail-state="error"` is set on `#detail-panel` with an error message; SW registration failure logged to `console.error` with reason
- Redaction constraints: none — no secrets or PII in API data

## Integration Closure

- Upstream surfaces consumed: `lua_api.json` (existing), `js/app.js`, `js/class-detail.js`, `js/search-index.js`, `index.html`, `app.css`
- New wiring introduced: `lua_api_index.json`, `lua_api_detail/` directory, `sw.js`, inline `<style>` block in `index.html`
- What remains before the milestone is truly usable end-to-end: nothing — S09 is the last unblocked slice

## Tasks

- [x] **T01: Split API JSON and update loader** `est:2h`
  - Why: The 6MB monolithic `lua_api.json` is the primary load bottleneck. This task produces the split files and rewires JS to use them.
  - Files: `scripts/split_api.py`, `js/app.js`, `js/search-index.js`, `js/class-detail.js`
  - Do: Write `scripts/split_api.py` that reads `lua_api.json` and writes `lua_api_index.json` (top-level keys + per-class summary: `set_exposed`, `lua_tagged`, `is_enum`, `method_count`, `field_count`, `source_file`, `simple_name`) and one `lua_api_detail/<fqn>.json` per class (full class object). Update `js/app.js` `loadApi()` to fetch `lua_api_index.json` instead of `lua_api.json`; keep existing fallback to `lua_api.json` if index is absent. Update `js/class-detail.js` `renderClassDetail()` to check if full class data is already in `API.classes[fqn]`; if not, fetch `./lua_api_detail/<fqn>.json`, merge into `API.classes`, then render. Set `#detail-panel` `data-detail-state="loading"` before fetch and `"error"` on failure. Update `js/search-index.js` to build from the lighter index fields.
  - Verify: `python3 scripts/split_api.py && test -f lua_api_index.json && python3 -c "import json,pathlib; d=json.load(open('lua_api_index.json')); assert len(d['classes'])>1000; assert len(list(pathlib.Path('lua_api_detail').glob('*.json')))>1000"`
  - Done when: `lua_api_index.json` and `lua_api_detail/` exist with 1000+ entries each; viewer JS loads without errors when served

- [x] **T02: Inline critical CSS and add service worker** `est:1.5h`
  - Why: Render-blocking `app.css` adds latency on first visit; a service worker makes repeat visits instant.
  - Files: `index.html`, `app.css`, `sw.js`
  - Do: Extract above-the-fold rules (header, sidebar skeleton, loading spinner, basic layout) from `app.css` into an inline `<style>` block in `<head>`; load remaining `app.css` via `<link rel="preload" as="style" onload="this.onload=null;this.rel='stylesheet'">` + `<noscript>` fallback. Write `sw.js`: on install, cache `lua_api_index.json`, `app.css`, all `js/*.js`, `index.html`; on fetch, use cache-first for those assets and network-first (with cache fallback) for `lua_api_detail/*.json`; use a versioned cache name (`pz-api-v1`) so updates bust the cache. Register SW in `js/app.js` or inline script in `index.html`.
  - Verify: `grep -q '<style>' index.html && grep -q 'sw.js' index.html && test -f sw.js && grep -q 'pz-api-v' sw.js`
  - Done when: `index.html` contains an inline `<style>` block and SW registration; `sw.js` exists with cache-first strategy for static assets

- [ ] **T03: Write S09 Playwright test and run full suite** `est:45m`
  - Why: Proves index-only boot works and per-class detail fetches correctly; catches any regressions in existing tests.
  - Files: `.gsd/test/s09_load_perf.py`, `.gsd/test/run.py`
  - Do: Write `s09_load_perf.py` with pytest-playwright tests: (1) serve the viewer with `python -m http.server` in background, (2) assert `lua_api_index.json` is fetched on load (intercept network or check `data-detail-state` absence until class clicked), (3) click a class and assert `data-detail-state` transitions `loading` → `ready` and a `lua_api_detail/*.json` request was made, (4) assert `#detail-panel` renders class name. Register `s09_load_perf.py` in `.gsd/test/run.py`. Run full suite.
  - Verify: `python3 -m pytest .gsd/test/s09_load_perf.py -v && python3 -m pytest .gsd/test/run.py -v`
  - Done when: All S09 tests pass; no regressions in existing suite

## Files Likely Touched

- `scripts/split_api.py` — new: produces index + per-class JSON
- `lua_api_index.json` — generated output
- `lua_api_detail/` — generated directory (1096 files)
- `js/app.js` — load index JSON; register SW
- `js/search-index.js` — build from index fields
- `js/class-detail.js` — lazy-fetch per-class JSON; `data-detail-state` transitions
- `index.html` — inline critical CSS; SW registration
- `app.css` — mark/retain non-critical rules
- `sw.js` — new: cache-first + network-first strategies
- `.gsd/test/s09_load_perf.py` — new: Playwright tests
- `.gsd/test/run.py` — register new test module
