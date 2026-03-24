---
estimated_steps: 5
estimated_files: 4
skills_used:
  - frontend-design
  - make-interfaces-feel-better
  - test
---

# T02: Add recently viewed classes UI and persistence

**Slice:** S08 — Navigation State
**Milestone:** M001

## Description

Implement the second user-facing outcome of the slice: a compact recent-classes control backed by localStorage. The control should reflect real class navigation, deduplicate entries, cap the list, reuse existing class-selection/tab wiring, and publish enough DOM state that tests can distinguish empty, populated, and fallback cases.

## Steps

1. Read `index.html`, `app.css`, `js/app.js`, and `.gsd/test/s08_navigation_state.py` together so the new control fits the current shell and test seam.
2. Add a recent-classes trigger and list container to `index.html`, placing it near the existing search/navigation controls without crowding the header.
3. Implement localStorage-backed recent-class persistence in `js/app.js`, updating the list from actual class-selection events, deduplicating by FQN, capping length, and ignoring unresolved classes.
4. Render the recent list into the new control, wire selection back through existing `selectClass` or equivalent active-tab logic, and expose stable DOM data for count/state so tests can inspect the control without relying on styling details alone.
5. Extend `.gsd/test/s08_navigation_state.py` to cover recent-list ordering, deduplication, and navigation back into the correct class/content view.

## Must-Haves

- [ ] Recent classes are persisted in localStorage as a bounded, deduplicated, most-recent-first list.
- [ ] Choosing a recent class reuses existing navigation wiring instead of introducing a parallel rendering path.
- [ ] The rendered control exposes stable DOM state for empty/populated/count behavior so browser tests and future debugging can inspect it directly.

## Verification

- `python -m pytest .gsd/test/s08_navigation_state.py -k "recent or diagnostics"`
- Manual check in the browser: open several classes, confirm the recent control updates in order, then choose an older entry and verify it navigates back correctly.

## Observability Impact

- Signals added/changed: DOM data attributes for recent-list count/state and any invalid-entry pruning/fallback marker.
- How a future agent inspects this: browser devtools on the recent control plus Playwright assertions in `.gsd/test/s08_navigation_state.py`.
- Failure state exposed: empty storage, stale class entries, or pruning of missing classes becomes visible through stable control state instead of hidden localStorage behavior.

## Inputs

- `index.html` — current header/search/navigation structure
- `app.css` — shell layout and control styling rules
- `js/app.js` — class-selection and navigation wiring to reuse
- `.gsd/test/s08_navigation_state.py` — S08 runtime assertions to extend from T01 output

## Expected Output

- `index.html` — recent-classes control markup
- `app.css` — recent-control layout and interaction styling
- `js/app.js` — recent-class persistence/rendering/navigation wiring
- `.gsd/test/s08_navigation_state.py` — browser coverage for recent classes behavior
