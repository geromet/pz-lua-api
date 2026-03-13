---
id: T01
parent: S05
milestone: M001
provides:
  - Sidebar ↔ content draggable splitter (FEAT-004) with localStorage persistence
key_files:
  - pz-lua-api-viewer/index.html
  - pz-lua-api-viewer/app.css
  - pz-lua-api-viewer/js/app.js
key_decisions:
  - Reused existing initSplitter() infrastructure — zero new logic needed
  - sidebar border-right removed (splitter provides visual separation)
  - sidebar CSS changed from fixed width to flex:0 0 var(--sidebar-w) so initSplitter's flex override works
  - Splitter always visible (not conditional on split-layout mode)
patterns_established:
  - none new — follows existing initSplitter pattern
observability_surfaces:
  - none
duration: ~20m
verification_result: passed
completed_at: 2026-03-13
blocker_discovered: false
---

# T01: Sidebar splitter (FEAT-004)

**Added a draggable splitter between sidebar and content area; width persists in localStorage; reuses existing `initSplitter()` entirely.**

## What Happened

Added `<div class="splitter" id="sidebar-splitter">` between `#sidebar` and `#content` in `index.html`. Added `#sidebar-splitter{display:block}` override in `app.css` (other splitters are hidden by default). Changed `#sidebar` from `width:var(--sidebar-w)` to `flex:0 0 var(--sidebar-w)` so `initSplitter`'s style manipulation works. Called `initSplitter('sidebar-splitter', 'sidebar', 'splitW-sidebar')` in `init()`.

## Verification

- Drag from x=320 to x=420 → sidebar flex changed to `0 0 420px`
- Reload → `splitW-sidebar` from localStorage applied, sidebar still 420px
- No console errors

## Diagnostics

None.

## Deviations

Not a scheduled S02/S03 task — picked up as an unblocked planned feature (FEAT-004 was listed as `*(unblocked)*` in docs/STATUS.md).

## Known Issues

None.

## Files Created/Modified

- `pz-lua-api-viewer/index.html` — added `#sidebar-splitter` div
- `pz-lua-api-viewer/app.css` — added `#sidebar-splitter{display:block}`; changed `#sidebar` to use flex
- `pz-lua-api-viewer/js/app.js` — added `initSplitter('sidebar-splitter', 'sidebar', 'splitW-sidebar')` call
