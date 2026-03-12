# BUG-007: Navigation / URI hash edge cases

**Status:** Open
**Severity:** Low–Medium
**Touches:** `js/app.js`

## Description

Several edge cases in URL hash handling and navigation have been reported:

1. **Hash not updated on back/forward**: When using Alt+Left / Alt+Right (or back/forward buttons), `location.hash` is not always updated to reflect the restored state. This means a page reload after navigation lands on the wrong class.

2. **Hash encoding issues**: Some FQNs with special characters (e.g. `$` for nested classes) may not round-trip cleanly through `encodeURIComponent` / `decodeURIComponent`.

3. **Double-push on initial load**: On page load with a hash, `selectClass(val)` is called, which calls `navPush`. But the initial state is never pushed, so the first back-press goes nowhere instead of to the placeholder.

## Steps to Reproduce

For (1): Navigate to class A, then class B using Alt+Left go back to A. Reload page — should show A but may show B.

For (3): Load `#zombie.characters.IsoPlayer`. Press Alt+Left. Back button should go to the "no class selected" placeholder, but may be disabled or go to the wrong entry.

## Fix Sketch

- In `applyState`, call `location.hash = ...` appropriately for each case (class → FQN, globals → 'globals', placeholder → '').
- For (3), push a `{type: 'placeholder'}` entry into `navHistory` as the very first entry in `init()` before processing the initial hash.
- Audit `encodeURIComponent` usage — nested class FQNs use `$` which is safe in URIs but double-encoding may occur.
