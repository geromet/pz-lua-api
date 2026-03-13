---
id: T01
parent: S03
milestone: M001
provides:
  - detect_pz_version() helper in extract_lua_api.py using SVNRevision.txt / bat file
  - versions/lua_api_r964.json — versioned copy of current API
  - versions/versions.json — manifest with single entry for r964
key_files:
  - pz-lua-api-viewer/extract_lua_api.py
  - pz-lua-api-viewer/versions/versions.json
  - pz-lua-api-viewer/versions/lua_api_r964.json
key_decisions:
  - SVNRevision.txt is the most reliable version source (single integer, no parsing needed); yields "r964"
  - versions.json is a list so future entries can be appended without restructuring
  - New entry is always inserted at front (newest first); stale same-id entry removed before re-insert
  - lua_api.json at root kept unchanged for backwards compatibility (single-file deployments still work)
patterns_established:
  - detect_pz_version() in extractor: SVNRevision.txt → bat file → "unknown" fallback
observability_surfaces:
  - none
duration: ~25m
verification_result: passed
completed_at: 2026-03-13
blocker_discovered: false
---

# T01: Extractor versioned output

**Added `detect_pz_version()` to extractor; extractor now writes `versions/lua_api_r964.json` and creates/updates `versions/versions.json` after each run.**

## What Happened

Added `detect_pz_version(pz_root)` function that reads `SVNRevision.txt` (returns `r964` for current install) with fallback to bat file version string, then `"unknown"`. At end of extractor, creates `pz-lua-api-viewer/versions/` dir, writes versioned JSON copy, and merges entry into `versions/versions.json`.

## Verification

- `python pz-lua-api-viewer/extract_lua_api.py` ran cleanly (code 0, 1096 classes)
- `versions/lua_api_r964.json` created (5.9 MB)
- `versions/versions.json` contains 1 entry: `{"id":"r964","label":"Build r964","file":"versions/lua_api_r964.json"}`
- Root `lua_api.json` still present and unchanged in role

## Diagnostics

None — extractor prints version detected during each run.

## Deviations

None.

## Known Issues

None.

## Files Created/Modified

- `pz-lua-api-viewer/extract_lua_api.py` — added `detect_pz_version()` + versioned output block at end
- `pz-lua-api-viewer/versions/versions.json` — created (new)
- `pz-lua-api-viewer/versions/lua_api_r964.json` — created (new, ~5.9MB)
