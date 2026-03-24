---
id: T02
parent: S07
milestone: M001
provides:
  - The viewer shell now uses a compact header summary, a dropdown-backed class filter, clickable package breadcrumbs, and reserved detail/source panel space with inspectable layout state.
key_files:
  - index.html
  - app.css
  - js/app.js
  - js/class-detail.js
  - .gsd/test/s07_ux_polish.py
  - pytest.ini
key_decisions:
  - Package breadcrumb clicks reuse the existing global search path by writing the package prefix into `#global-search` instead of introducing parallel breadcrumb navigation state.
patterns_established:
  - UI polish state remains inspectable through existing DOM seams: `#filter-select[data-active-filter]`, `#active-filter-chip[data-filter]`, `#detail-panel[data-detail-state][data-detail-fqn]`, and breadcrumb data attributes in the detail header.
observability_surfaces:
  - DOM state on `#filter-select`, `#active-filter-chip`, `#detail-panel`, `#source-panel`, and `.class-breadcrumbs`; Playwright coverage in `.gsd/test/s07_ux_polish.py`.
duration: 2h
verification_result: passed
completed_at: 2026-03-24
blocker_discovered: false
---

# T02: Recompose header, filters, breadcrumbs, and stable layout styling

**Shipped a slimmer viewer shell with dropdown filters, package breadcrumbs, and stable detail/source panel chrome.**

## What Happened

I replaced the sidebar filter button wall with a compact select plus an inspectable active-filter chip, but kept the underlying `currentFilter` values and class-list filtering logic unchanged. `js/app.js` now syncs that control through a single helper so the active filter stays visible in the DOM and the class list rebuild path remains the same.

I recomposed the detail header in `js/class-detail.js` around a stable card shell. It now renders package breadcrumbs from the class FQN, exposes `data-detail-state` and `data-detail-fqn` on `#detail-panel`, and wires package crumb clicks back into the existing global search/class-list flow by filling `#global-search` with the package prefix. The class leaf remains visible as the terminal breadcrumb while source-path and copy-FQN behavior continue to work.

In `app.css` and `index.html` I tightened the shell chrome: the header stats became compact summary pills, the sidebar gained a smaller toolbar, and both detail/source regions now reserve panel height before content arrives. The source panel uses the existing `data-source-state` seam to show a stable loading shell instead of relying on late content growth. I also extended `.gsd/test/s07_ux_polish.py` to assert the new header/filter, breadcrumb, and layout surfaces and registered the new pytest markers in `pytest.ini`.

## Verification

Verified the task-specific browser checks in Playwright: the compact filter control preserves and exposes the active filter value, breadcrumbs render from the selected class FQN and drive the existing search/list workflow, and the detail/source panels expose stable shell state while source loading is pending. I also reran the full S07 module and the diagnostics subset to confirm the T01 prefetch and failure-state coverage still passes after the UI changes.

Slice-level status for this intermediate task: the standalone S07 module command and the diagnostics subset now pass. The slice-wide runner/report checks and manual browser review listed in the slice plan still belong to T03.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `node --check js/app.js && node --check js/class-detail.js && python -m py_compile .gsd/test/s07_ux_polish.py` | 0 | ✅ pass | n/a |
| 2 | `python -m pytest .gsd/test/s07_ux_polish.py -k "header or breadcrumbs or layout"` | 0 | ✅ pass | 5.59s |
| 3 | `python -m pytest .gsd/test/s07_ux_polish.py` | 0 | ✅ pass | 10.42s |
| 4 | `python -m pytest .gsd/test/s07_ux_polish.py -k "loading_state or failure or diagnostics"` | 0 | ✅ pass | 1.30s |

## Diagnostics

Inspect the active filter on `#filter-select[data-active-filter]` and `#active-filter-chip[data-filter]`. Inspect detail header breadcrumbs on `#detail-panel .class-breadcrumbs [data-breadcrumb-package]` and the selected class on `[data-breadcrumb-leaf]`. Inspect reserved panel/loading state on `#detail-panel[data-detail-state][data-detail-fqn]`, `#source-panel[data-source-state][data-source-path]`, and `#source-loading[data-source-state]`. `.gsd/test/s07_ux_polish.py` now asserts the compact filter, breadcrumb, and stable panel shell directly.

## Deviations

I registered `header`, `breadcrumbs`, and `layout` pytest markers in `pytest.ini` so the new slice assertions run without warning noise. That was not called out in the task plan, but it keeps the verification surface clean and durable.

## Known Issues

The slice-wide runner in `.gsd/test/run.py`, the report-file assertion against `.gsd/test-reports/report.json`, and manual browser review are still pending for T03.

## Files Created/Modified

- `index.html` — replaced the old header/button-wall chrome with a compact summary layout and dropdown-backed sidebar filter control.
- `app.css` — tightened palette/spacing, styled the compact header and sidebar toolbar, added breadcrumb/detail card styling, and reserved stable detail/source panel space.
- `js/app.js` — added shared filter-control syncing and breadcrumb package navigation through the existing search/list flow.
- `js/class-detail.js` — rendered FQN breadcrumbs, exposed detail-panel state attributes, and bound breadcrumb clicks to the current navigation model.
- `.gsd/test/s07_ux_polish.py` — added Playwright assertions for the new header/filter, breadcrumb, and stable layout behavior.
- `pytest.ini` — registered the new S07 UI markers used by the task-level browser checks.
