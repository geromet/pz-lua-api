# S02: Tab Enhancements

**Goal:** Ship FEAT-007 (middle-click to open in new tab) and FEAT-014 (hover preview card). Both depend on the tab system (TASK-012, already shipped in S00/foundation).
**Demo:** Middle-clicking a class link in the sidebar/source/inheritance opens a new tab. Hovering a class link for 400ms shows a preview card with class name, method count, and inheritance line.

## Must-Haves

- Middle-click (button 1) or Ctrl+click on any class link opens that class in a new tab without navigating the current tab (FEAT-007)
- Hover preview card appears after 400ms on class reference links; disappears on mouseout or click (FEAT-014)
- Preview card shows: simple name, FQN, method count, first 3 methods alphabetically
- Preview card stays within viewport (no overflow off right/bottom edge)
- Existing left-click navigation unaffected
- Browser screenshot confirms both features

## Verification

- Start server: `cd pz-lua-api-viewer && python server.py`
- Browser screenshot: open IsoPlayer, middle-click an inherited class — new tab appears with that class
- Browser screenshot: hover over a class link in source panel — preview card appears after delay
- No console errors in browser dev tools

## Tasks

- [x] **T01: Middle-click new tab (FEAT-007)** `est:45m`
  - Why: Implements FEAT-007; tab system already exists in `state.js` (tabs array, `openNewTab`)
  - Files: `pz-lua-api-viewer/js/app.js` (event delegation), `pz-lua-api-viewer/app.css` (cursor hint if needed)
  - Do: Read `pz-lua-api-viewer/docs/Planned_Features/FEAT-007-middle-mouse-new-tab.md`. In the global click handler (or mousedown), detect `event.button === 1` (middle) or `event.ctrlKey && event.button === 0` on elements with `data-fqn`. Call `openNewTab(fqn)`. Prevent default on middle-click (browser autoscroll). Applies to: sidebar class items, inheritance header links, inherited method class links, source `<a data-fqn>` tags.
  - Verify: Browser screenshot showing two tabs after middle-clicking a class link
  - Done when: Middle-click opens new tab; Ctrl+click opens new tab; left-click still navigates current tab

- [x] **T02: Hover preview card (FEAT-014)** `est:1.5h`
  - Why: Implements FEAT-014; class data already available in window-scope API object
  - Files: `pz-lua-api-viewer/js/app.js` (hover logic), `pz-lua-api-viewer/app.css` (preview card styles), `pz-lua-api-viewer/index.html` (preview card container div)
  - Do: Read `pz-lua-api-viewer/docs/Planned_Features/FEAT-014-hover-preview.md`. Add a `#hover-preview` div to `index.html` (hidden by default). In `app.js`, add `mouseover`/`mouseout` listeners on `[data-fqn]` elements. On mouseover, set a 400ms timeout; on fire, look up the class in the API, build preview HTML (name, FQN, method count, first 3 methods), position the card near the cursor clamped to viewport, show it. On mouseout, cancel timeout and hide card. Style in `app.css` (small card, shadow, dark theme consistent with existing UI).
  - Verify: Browser screenshot showing hover card over a class link
  - Done when: Card appears after 400ms; disappears on mouseout; no viewport overflow; no console errors

- [ ] **T03: Update docs and commit S02** `est:10m`
  - Why: Keep STATUS.md accurate; lock in S02
  - Files: `pz-lua-api-viewer/docs/STATUS.md`, `pz-lua-api-viewer/docs/Planned_Features/FEAT-007-middle-mouse-new-tab.md`, `pz-lua-api-viewer/docs/Planned_Features/FEAT-014-hover-preview.md`
  - Do: Archive FEAT-007 and FEAT-014 docs using `python docs/archive.py`. Update `docs/STATUS.md`. Commit and push.
  - Verify: `git status` clean; `git log --oneline -3` shows commit
  - Done when: pushed to `liability-machine`

## Files Likely Touched

- `pz-lua-api-viewer/js/app.js`
- `pz-lua-api-viewer/app.css`
- `pz-lua-api-viewer/index.html`
- `pz-lua-api-viewer/docs/STATUS.md`
