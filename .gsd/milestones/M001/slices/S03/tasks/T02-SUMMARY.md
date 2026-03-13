---
id: T02
parent: S03
milestone: M001
provides:
  - Version-aware API loader in app.js (fetches versions/versions.json, selects versioned JSON)
  - #version-select dropdown in toolbar (hidden when ≤1 version)
  - ?v=<id> URL query param handling (load-time selection + hash preservation on switch)
key_files:
  - pz-lua-api-viewer/js/app.js
  - pz-lua-api-viewer/index.html
  - pz-lua-api-viewer/app.css
key_decisions:
  - Version switching uses full page reload (location.href) — avoids complex global state reset
  - Dropdown hidden when manifest absent or has ≤1 entry — no visible change for single-version deploys
  - Hash preserved across version switch (appended to new URL) so currently-open class reopens
  - versions.json fetch failure is silently caught; falls back to ./lua_api.json (single-file mode)
  - Unknown ?v= param falls back to first manifest entry (graceful degradation)
patterns_established:
  - Async IIFE at top of app.js for API loading (replaces synchronous .then chain)
  - setupVersionDropdown(manifest, currentId) called from loader after API ready
observability_surfaces:
  - none
duration: ~45m
verification_result: passed
completed_at: 2026-03-13
blocker_discovered: false
---

# T02: Frontend version dropdown and switching

**Version-aware loader replaces the single-fetch chain; `#version-select` dropdown appears in toolbar when manifest has ≥2 versions; `?v=<id>` URL param selects version on load and is preserved on navigation.**

## What Happened

Replaced the top-level `fetch('./lua_api.json').then(...)` in `app.js` with an async IIFE that:
1. Reads `?v=` from query params
2. Fetches `versions/versions.json` (silently swallows 404)
3. Selects the right versioned file, falls back to `lua_api.json`
4. After load, calls `setupVersionDropdown()` then `init()`

Added `<select id="version-select">` to `index.html` header and CSS for it. `setupVersionDropdown()` hides the select when manifest is absent or has ≤1 version; otherwise populates and shows it. Switching triggers a full page reload with `?v=<newId>` + existing hash.

## Verification

- Browser: loaded with single entry in versions.json → dropdown hidden, API loads normally, no console errors
- Browser: added fake second entry (r900) → dropdown showed "Build r964 / Build r900 (old)"
- Browser: switched to r900 → URL became `?v=r900#zombie.characters.IsoPlayer`, page reloaded, IsoPlayer reloaded, "Build r900 (old)" selected
- Browser: `?v=nonexistent#IsoPlayer` → gracefully fell back to first manifest entry, IsoPlayer loaded
- No console JS errors in any scenario

## Diagnostics

None — version selection is purely URL-driven; current version is always visible in dropdown or URL.

## Deviations

None.

## Known Issues

None — S03/T03 will archive docs and commit.

## Files Created/Modified

- `pz-lua-api-viewer/js/app.js` — replaced top-level fetch with async IIFE; added `setupVersionDropdown()`
- `pz-lua-api-viewer/index.html` — added `<select id="version-select">`
- `pz-lua-api-viewer/app.css` — added `#version-select` styles
