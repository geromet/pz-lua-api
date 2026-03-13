---
id: T02
parent: S02
milestone: M001
provides:
  - Hover preview card (FEAT-014) — 400ms delay, viewport-clamped, shows name/FQN/badges/methods/hint
key_files:
  - pz-lua-api-viewer/js/app.js
  - pz-lua-api-viewer/app.css
  - pz-lua-api-viewer/index.html
key_decisions:
  - Single shared #hover-preview div, positioned with fixed CSS (not absolute) so viewport-clamping math uses window dimensions directly
  - mouseover/mouseout on document (not individual elements) via event delegation — avoids attaching per-element listeners on a large list
  - 400ms show delay; 80ms hide grace period to prevent flicker when cursor moves over child text nodes
  - Only callable methods (lua_tagged or set_exposed) shown in preview — matches what users actually care about
patterns_established:
  - IIFE in setupEvents() to encapsulate preview state (timer, lastFqn) without polluting module scope
observability_surfaces:
  - none
duration: ~40m
verification_result: passed
completed_at: 2026-03-13
blocker_discovered: false
---

# T02: Hover preview card (FEAT-014)

**Floating preview card appears after 400ms hover on any `[data-fqn]` element, showing class name, FQN, exposure badges, method/field counts, top 3 callable methods, and a keyboard hint.**

## What Happened

Added `#hover-preview` div to `index.html`, CSS styles to `app.css`, and an IIFE hover logic block in `setupEvents()` in `app.js`. The card uses delegated `mouseover`/`mouseout` on `document`, a 400ms setTimeout before showing, and a small 80ms grace period on hide to avoid flickering. Card positioning uses `getBoundingClientRect()` and clamps to viewport edges.

## Verification

- Browser: hovered over `SleepingEvent` sidebar item — card appeared after 400ms showing name, FQN, setExposed + @UsedFromLua badges, 5 methods (3 shown + "…and 2 more"), hint
- Browser: hovered over `zombie.entity.GameEntity` inherit-link in IsoPlayer detail — card showed GameEntity with 55 methods
- Left-click still navigates normally (card dismissed on click via document click listener)
- No console JS errors (only pre-existing 404 from earlier test navigation to wrong port)

## Diagnostics

None — feature is purely visual/interactive with no async state.

## Deviations

None — implemented as specified in FEAT-014 and S02-PLAN.

## Known Issues

None discovered.

## Files Created/Modified

- `pz-lua-api-viewer/index.html` — added `<div id="hover-preview">` before script tags
- `pz-lua-api-viewer/app.css` — added `#hover-preview` and `.hp-*` styles
- `pz-lua-api-viewer/js/app.js` — added IIFE hover preview logic in `setupEvents()`
