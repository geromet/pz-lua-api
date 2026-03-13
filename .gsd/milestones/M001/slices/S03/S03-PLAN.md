# S03: Version Selector

**Goal:** Ship TASK-018 / FEAT-005 — a version dropdown in the toolbar that lets users switch between pre-extracted PZ API versions.
**Demo:** A version dropdown appears in the toolbar. Selecting a different version reloads the API data and re-initialises the viewer. `?v=B42#ClassName` URL form loads the correct version and class.

## Must-Haves

- Version dropdown in toolbar; single-version deploy shows only one entry (no visible change from current)
- `versions/versions.json` manifest drives the dropdown
- Selecting a version fetches the versioned JSON and reinitialises the viewer
- `?v=<build>#<class>` URL works: correct version selected, correct class opened
- Extractor writes `lua_api_<build>.json` when version detected; updates `versions.json`
- Existing single-JSON behaviour unchanged when `versions/versions.json` absent

## Proof Level

- This slice proves: integration — version switch fetches new data and re-renders correctly in the live browser
- Real runtime required: yes (fetch must actually load a versioned JSON)
- Human/UAT required: yes (screenshot to confirm dropdown and version switch)

## Verification

- `python -c "import json; v=json.load(open('pz-lua-api-viewer/versions/versions.json')); print(v)"` shows at least one entry
- Browser screenshot: toolbar shows version dropdown
- Browser screenshot: after selecting a version, class list still loads
- URL `http://localhost:8765?v=<build>#IsoPlayer` opens the correct class

## Tasks

- [x] **T01: Extractor versioned output** `est:1h`
  - Why: Implements the server-side half of FEAT-005 / TASK-018 Step 1
  - Files: `pz-lua-api-viewer/extract_lua_api.py`
  - Do: Read `pz-lua-api-viewer/docs/Tasks/TASK-018-version-selector.md` Step 1. Detect PZ build number (try `version.txt`, `.properties` files, EXE metadata, fall back to `"unknown"`). Write `lua_api_<build>.json` to `pz-lua-api-viewer/versions/`. Write/update `pz-lua-api-viewer/versions/versions.json` with `[{"id": "<build>", "label": "Build <build>", "file": "versions/lua_api_<build>.json"}]`. Keep writing `lua_api.json` at root for backwards compatibility.
  - Verify: Run extractor; confirm `versions/versions.json` and `versions/lua_api_*.json` exist
  - Done when: Both files exist; extractor exits cleanly; existing `lua_api.json` still present

- [x] **T02: Frontend version dropdown and switching** `est:2h`
  - Why: Implements the UI half of FEAT-005 / TASK-018 Steps 2–4
  - Files: `pz-lua-api-viewer/index.html`, `pz-lua-api-viewer/js/app.js`, `pz-lua-api-viewer/js/state.js`, `pz-lua-api-viewer/app.css`
  - Do: Read TASK-018 Steps 2–4. Add `<select id="version-select">` to toolbar. On init, try fetching `versions/versions.json`; if present, populate dropdown; if absent, hide dropdown. Selecting a version fetches the versioned JSON, replaces `window.API`, and calls `reinit()` (or full `init()` equivalent). Handle `?v=<build>` query param on load: select the matching version. Handle `#ClassName` after version switch. Store selected version in `localStorage`.
  - Verify: Browser screenshot showing dropdown; select different entry → class list reloads; URL `?v=<build>#IsoPlayer` works
  - Done when: All acceptance criteria in TASK-018 pass; no console errors

- [x] **T03: Update docs and commit S03** `est:10m`
  - Why: Keep STATUS.md accurate; archive TASK-018; lock in S03
  - Files: `pz-lua-api-viewer/docs/STATUS.md`, `pz-lua-api-viewer/docs/Tasks/TASK-018-version-selector.md`
  - Do: Prepend completion blockquote to TASK-018; run `python docs/archive.py docs/Tasks/TASK-018-version-selector.md`. Update STATUS.md. Commit and push.
  - Verify: `git status` clean; STATUS.md accurate
  - Done when: pushed to `liability-machine`

## Files Likely Touched

- `pz-lua-api-viewer/extract_lua_api.py`
- `pz-lua-api-viewer/js/app.js`
- `pz-lua-api-viewer/js/state.js`
- `pz-lua-api-viewer/index.html`
- `pz-lua-api-viewer/app.css`
- `pz-lua-api-viewer/versions/versions.json` (new)
- `pz-lua-api-viewer/versions/lua_api_*.json` (new)
- `pz-lua-api-viewer/lua_api.json` (regenerated)
