---
estimated_steps: 5
estimated_files: 3
skills_used: []
---

# T02: Inline critical CSS and add service worker

**Slice:** S09 — Load Performance
**Milestone:** M001

## Description

Eliminate the render-blocking `app.css` stylesheet by inlining above-the-fold styles directly in `<head>` and loading the full stylesheet asynchronously. Add `sw.js` with a cache-first strategy for static assets and network-first for per-class detail JSON, making repeat visits load from cache.

## Steps

1. Identify the critical-path CSS in `app.css`: rules for `body`, `#header`, `#tabs`, `#main`, `#sidebar`, `#loading`, `.loading-spinner`, and the layout grid/flex primitives (roughly the top ~80 lines). Extract these into an inline `<style>` block at the top of `<head>` in `index.html`.
2. Replace `<link rel="stylesheet" href="app.css">` with the async-load pattern:
   ```html
   <link rel="preload" href="app.css" as="style" onload="this.onload=null;this.rel='stylesheet'">
   <noscript><link rel="stylesheet" href="app.css"></noscript>
   ```
3. Write `sw.js`:
   - Cache name: `pz-api-v1` (increment for future updates)
   - On `install`: pre-cache `index.html`, `app.css`, all `js/*.js`, `lua_api_index.json`
   - On `fetch`: for requests matching `lua_api_detail/`, use network-first (try network, fall back to cache, cache successful responses); for all other requests, use cache-first (serve from cache if present, else fetch and cache)
   - On `activate`: delete caches with names that don't match current version
4. Register the service worker in `index.html` (inline script at bottom of `<body>`):
   ```js
   if ('serviceWorker' in navigator) {
     navigator.serviceWorker.register('./sw.js')
       .then(r => console.info('[SW] registered', r.scope))
       .catch(e => console.error('[SW] registration failed', e));
   }
   ```
5. Verify the inline style block renders the page skeleton without FOUC: open the viewer in a browser with DevTools → Network throttling to "Slow 3G" and confirm the header and sidebar appear before `app.css` finishes loading.

## Must-Haves

- [ ] `index.html` contains an inline `<style>` block in `<head>` with critical layout rules
- [ ] `app.css` is loaded asynchronously (no `<link rel="stylesheet">` blocking)
- [ ] `sw.js` exists with cache-first for static assets and network-first for detail JSON
- [ ] SW registration in `index.html`
- [ ] Cache version string in `sw.js` so future updates bust the cache cleanly

## Verification

- `grep -q '<style>' index.html`
- `grep -q 'preload' index.html`
- `! grep -q '<link rel="stylesheet" href="app.css">' index.html`
- `grep -q 'sw.js' index.html`
- `test -f sw.js && grep -q 'pz-api-v' sw.js`

## Observability Impact

- Signals added/changed: SW registration logs `[SW] registered <scope>` or `[SW] registration failed <error>` to console
- How a future agent inspects this: browser DevTools → Application → Service Workers shows registration status; console shows registration outcome on every page load
- Failure state exposed: SW registration failure logged to `console.error` with full error object

## Inputs

- `index.html` — to add inline style, async CSS load, and SW registration
- `app.css` — source of critical rules to extract

## Expected Output

- `index.html` — modified with inline `<style>`, async `app.css` load, SW registration
- `sw.js` — new service worker with cache-first + network-first strategies
