# S09: Load Performance — UAT

**Milestone:** M001
**Written:** 2026-03-24

## UAT Type

- UAT mode: mixed (artifact-driven + live-runtime)
- Why this mode is sufficient: The split JSON files are verifiable artifacts; the lazy-fetch behavior, service worker caching, and critical CSS require a running server.

## Preconditions

- `lua_api_index.json` and `lua_api_detail/` directory exist (run `python3 scripts/split_api.py` if not)
- A local HTTP server serving the project root: `python3 -m http.server 8000`
- Browser with DevTools available (Chrome/Chromium recommended for SW inspection)

## Smoke Test

Open `http://localhost:8000/` in a browser. The class list should render within 1–2 seconds. Open DevTools → Network and confirm the first data request is `lua_api_index.json` (~727KB), not `lua_api.json` (~6MB).

## Test Cases

### 1. Index-only boot

1. Clear browser cache and unregister any existing service worker for localhost.
2. Open `http://localhost:8000/` with DevTools → Network open.
3. Wait for the class list to appear in the sidebar.
4. Filter Network requests to XHR/Fetch.
5. **Expected:** `lua_api_index.json` was fetched. `lua_api.json` was NOT fetched. `lua_api_detail/` requests appear only if a class was specified in the URL.

### 2. Lazy detail fetch on class selection

1. From the loaded viewer (no class selected), click any class in the sidebar (e.g., `IsoPlayer`).
2. Observe the detail panel.
3. **Expected:** The detail panel briefly shows a loading state, then renders the full class detail (methods, fields, constructors). DevTools → Network shows a request to `lua_api_detail/zombie.characters.IsoPlayer.json`. The `#detail-panel` has `data-detail-state="ready"`.

### 3. Detail fetch error handling

1. Open DevTools → Network, enable "Block request URL" for `lua_api_detail/*` (or use offline mode after initial load).
2. Click a class that hasn't been loaded yet.
3. **Expected:** The detail panel shows `data-detail-state="error"` with an error message like "Failed to load class detail". The app does not crash.

### 4. Critical CSS — no FOUC

1. In DevTools → Network, set throttling to "Slow 3G".
2. Hard-refresh the page (Ctrl+Shift+R).
3. **Expected:** The page skeleton (header, sidebar outline, loading spinner) appears immediately before `app.css` finishes loading. No flash of unstyled content.

### 5. Async stylesheet loading

1. View page source (`Ctrl+U`).
2. Search for `<style>` in `<head>`.
3. Search for `app.css`.
4. **Expected:** An inline `<style>` block with critical CSS is present in `<head>`. `app.css` is loaded via `<link rel="preload" as="style" onload=...>` with a `<noscript>` fallback. There is no blocking `<link rel="stylesheet" href="app.css">` outside the noscript tag.

### 6. Service worker registration

1. Open `http://localhost:8000/` and wait for the page to fully load.
2. Open DevTools → Console.
3. **Expected:** A `[SW] registered` message appears in the console. DevTools → Application → Service Workers shows an active service worker for the scope.

### 7. Service worker caching — repeat visit

1. After the service worker is registered (test 6), navigate to a few classes to populate the detail cache.
2. In DevTools → Application → Cache Storage, open `pz-api-v1`.
3. **Expected:** The cache contains `index.html`, `app.css`, JS files, `lua_api_index.json`, and any `lua_api_detail/<fqn>.json` files that were fetched.
4. Set DevTools → Network to "Offline".
5. Hard-refresh the page.
6. **Expected:** The page loads from the SW cache. Previously visited classes render their detail. Unvisited classes show an error state (network unavailable).

### 8. Split mode disabled with version parameter

1. Navigate to `http://localhost:8000/?v=someversion`.
2. Open DevTools → Console.
3. **Expected:** `window._apiSplit` is `false` (or undefined). The viewer attempts to load the versioned `lua_api.json`, not the split index. (It may fail if the version doesn't exist — that's expected; the point is it doesn't use split mode.)

## Edge Cases

### Class with special characters in FQN

1. Find a class with dots or inner-class separators in its FQN.
2. Click it in the sidebar.
3. **Expected:** The detail JSON file is fetched correctly (dots are part of the filename). Detail renders without error.

### Rapid class switching

1. Click through 5–6 different classes quickly in succession.
2. **Expected:** Each class eventually renders correctly. No stale detail from a previous class appears. The final class shown matches the last one clicked. No console errors.

### Browser with service workers disabled

1. In browser settings, disable service workers (or use a browser that doesn't support them).
2. Load the page.
3. **Expected:** The page loads normally using split-index mode. The SW registration failure is logged to console.error but does not affect functionality.

## Failure Signals

- `lua_api.json` (6MB) fetched on initial load instead of `lua_api_index.json` — split mode not active
- `window._apiSplit` is `false` when no `?v=` param is present — fallback triggered unexpectedly
- Detail panel stuck on `data-detail-state="loading"` — lazy fetch hung or failed silently
- Flash of unstyled content on page load — critical CSS not inlined or not covering necessary rules
- No `[SW]` console message — SW registration not wired
- `pz-api-v1` cache empty after browsing — SW fetch handler not caching responses

## Not Proven By This UAT

- Performance under real-world network conditions (CDN, geographic latency)
- Split mode with versioned builds (`?v=` parameter) — this is intentionally disabled
- Service worker update flow when `pz-api-v2` replaces `pz-api-v1`
- Actual time-to-interactive metrics (this is functional correctness, not benchmarking)

## Notes for Tester

- The 2 flaky S07 tests (hover prefetch timing, source panel timing) are pre-existing and unrelated to S09. Ignore them.
- If `lua_api_index.json` doesn't exist, run `python3 scripts/split_api.py` first — it generates both the index and the 1096 detail files.
- The service worker requires HTTPS in production but works on `localhost` for testing.
