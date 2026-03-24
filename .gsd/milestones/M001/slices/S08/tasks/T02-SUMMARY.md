---
id: T02
parent: S08
milestone: M001
provides:
  - Recently viewed classes UI with bounded localStorage persistence, inspectable DOM diagnostics, and browser coverage for ordering, deduplication, and stale-entry pruning
key_files:
  - index.html
  - app.css
  - js/app.js
  - .gsd/test/s08_navigation_state.py
key_decisions:
  - none
patterns_established:
  - Render recent-class diagnostics on `#recent-classes[data-recent-*]` so tests and future agents can inspect count, source, pruning, and last action without scraping visual text
observability_surfaces:
  - "#recent-classes[data-recent-state|data-recent-count|data-recent-source|data-recent-pruned|data-recent-last-action]"
duration: 48m
verification_result: passed
completed_at: 2026-03-24T19:35:30+01:00
blocker_discovered: false
---

# T02: Add recently viewed classes UI and persistence

**Added a header recent-classes control backed by deduplicated localStorage state and covered it with S08 browser assertions.**

## What Happened

I added a compact recent-classes disclosure control to the header, keeping it close to search and navigation without crowding the shell. The control renders a bounded most-recent-first list of real classes, shows an empty state when nothing has been visited yet, and closes cleanly on outside click or Escape.

In `js/app.js` I added a small persistence layer for recent classes. It loads from `localStorage`, removes duplicates and stale class entries, caps the list to eight items, and rewrites sanitized state back to storage. Class selections now call `rememberRecentClass(fqn)` only after the FQN has already passed the existing `API.classes` resolution check, so unresolved classes are ignored and recent selections reuse the same `selectClass` path as normal sidebar navigation.

The recent control publishes stable diagnostics on `#recent-classes`: count, empty/ready state, storage source, prune count, and the last control action. I extended the S08 Playwright module to verify stored-entry hydration, stale-entry pruning, deduplication/reordering after repeat navigation, and recent-driven navigation back into the normal detail view.

## Verification

Ran the task-level S08 browser checks for recent/diagnostics coverage and then reran the full S08 standalone module. Both passed.

I also ran the slice-level consolidated runner and the report assertion to record current closure state. Those still fail on pre-existing non-T02 runner cases (`back-forward-buttons`, `class-detail-panel`, `copy-fqn`, `inheritance-chain`, `tab-navigation`, `class-link-clicks`), so the slice gate is not yet closed and remains for T03.

Manual browser review was not run in this task context.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `python -m pytest .gsd/test/s08_navigation_state.py -k "recent or diagnostics"` | 1 | ❌ fail | 7.00s |
| 2 | `python -m pytest .gsd/test/s08_navigation_state.py -k "recent or diagnostics"` | 0 | ✅ pass | 7.30s |
| 3 | `python -m pytest .gsd/test/s08_navigation_state.py` | 0 | ✅ pass | 8.40s |
| 4 | `python .gsd/test/run.py` | 1 | ❌ fail | 41.80s |
| 5 | `python - <<'PY'
import json
from pathlib import Path
report = json.loads(Path('.gsd/test-reports/report.json').read_text())
assert report['failed'] == 0, report
print('report-ok')
PY` | 1 | ❌ fail | 3.90s |

## Diagnostics

Inspect `#recent-classes` in devtools or Playwright and read:
- `data-recent-state`
- `data-recent-count`
- `data-recent-source`
- `data-recent-pruned`
- `data-recent-last-action`

Rendered recent entries expose `data-recent-fqn` on each button, and the empty state exposes `data-recent-empty="true"` for direct assertions.

## Deviations

None.

## Known Issues

- `pytest` still warns about unregistered `restore` and `recent` markers in `.gsd/test/s08_navigation_state.py`.
- The consolidated runner/report gate still fails on pre-existing non-T02 checks and needs slice-closure work in T03.
- Manual browser review for the recent control was not run in this task context.

## Files Created/Modified

- `index.html` — added the recent-classes disclosure control markup and inspectable container
- `app.css` — styled the recent trigger, panel, items, and responsive header placement
- `js/app.js` — added recent-class load/sanitize/persist/render logic and wired recent selection back through `selectClass`
- `.gsd/test/s08_navigation_state.py` — added browser coverage for recent ordering, deduplication, stale-entry pruning, and recent navigation
