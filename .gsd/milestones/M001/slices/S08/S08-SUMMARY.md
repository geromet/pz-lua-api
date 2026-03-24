---
id: S08
parent: M001
milestone: M001
provides:
  - Shareable query-backed navigation state with durable recent-class history and inspectable DOM diagnostics
requires:
  - slice: S06
    provides: Instant search and progressive rendering that S08 reuses for restored class/search/filter state
affects:
  - S09
key_files:
  - js/app.js
  - index.html
  - app.css
  - .gsd/test/s08_navigation_state.py
  - .gsd/test/run.py
  - .gsd/test/navigation.py
  - pytest.ini
key_decisions:
  - D004: Serialize shareable viewer state into query parameters while preserving `?v=` and accepting legacy hashes as restore-only fallback
patterns_established:
  - Publish navigation restore and recent-history state through `#content[data-nav-*]` and `#recent-classes[data-recent-*]`, then assert those seams in browser tests instead of scraping presentation text
observability_surfaces:
  - `#content[data-nav-restore-status|data-nav-serialized-state|data-nav-parsed-state|data-nav-applied-state|data-nav-restore-error|data-nav-url-state-source|data-nav-url-state-href]`
  - `#recent-classes[data-recent-state|data-recent-count|data-recent-source|data-recent-pruned|data-recent-last-action]`
  - `python .gsd/test/run.py`
  - `python -m pytest .gsd/test/s08_navigation_state.py`
  - `.gsd/test-reports/report.json`
drill_down_paths:
  - .gsd/milestones/M001/slices/S08/tasks/T01-SUMMARY.md
  - .gsd/milestones/M001/slices/S08/tasks/T02-SUMMARY.md
  - .gsd/milestones/M001/slices/S08/tasks/T03-SUMMARY.md
duration: 3h05m
verification_result: passed
completed_at: 2026-03-24T19:43:48+01:00
---

# S08: Navigation State

**The viewer now round-trips shareable browsing context through the URL, restores that context on load, and keeps a durable recently viewed class list wired into the same navigation flow.**

## What Happened

S08 replaced the old hash-only shareability model with an explicit URL-state contract in `js/app.js`. The app now serializes and restores the active top-level tab, selected class, search text, filter, and class content tab through query parameters while preserving the existing version selector parameter `?v=`. Legacy hashes still restore as a fallback input path, but live state now writes back to the query contract so copied URLs capture the real browsing context.

Restoration is staged after API data and controls are ready, rather than racing initial render. Invalid or partial URL input no longer fails silently: `#content` now publishes parsed state, applied state, restore status, restore reason, restore error, and the effective source URL through stable `data-nav-*` attributes. That gives tests and future agents a direct way to tell whether state was restored, defaulted, rejected, or fell back.

The slice also added a real recent-classes control in the header. `index.html` and `app.css` now expose a compact disclosure UI, and `js/app.js` backs it with a bounded, deduplicated `localStorage` list. Only resolvable classes are stored, stale entries are pruned on load, reselecting a class moves it to the top, and choosing a recent class reuses the existing `selectClass` / content-tab wiring instead of creating a parallel navigation path.

Slice closure work integrated the S08 pytest module into `.gsd/test/run.py`, aligned the legacy navigation check in `.gsd/test/navigation.py` with the shipped query-backed URL format while keeping tolerance for older hash URLs, and registered the new pytest markers in `pytest.ini`. The consolidated runner now reports S08 explicitly in `.gsd/test-reports/report.json`.

## Verification

Verified against the slice plan’s automated gates:

- `python .gsd/test/run.py` → passed; consolidated report recorded 15/15 passing checks including S08.
- `python -m pytest .gsd/test/s08_navigation_state.py` → passed; 5/5 S08 tests green.
- `python -m pytest .gsd/test/s08_navigation_state.py -k "restore or recent or diagnostics or failure"` → passed.
- `python - <<'PY' ... report.json ... PY` → passed; confirmed `.gsd/test-reports/report.json` reports zero failures.

The slice’s observability contract was also confirmed through the passing S08 Playwright module, which asserts both `#content[data-nav-*]` and `#recent-classes[data-recent-*]` in happy-path and failure-path cases.

A literal human-operated browser pass was not executed in this harness because the available browser automation CLI was not installed (`agent-browser: command not found`). The automated Playwright coverage exercises the same restore/recent flows on the running app and is the authoritative runtime proof captured for this slice.

## New Requirements Surfaced

- none

## Deviations

None. The implementation stayed within the written slice scope; the only extra closure work was updating the legacy navigation test to accept the new query-backed contract so the existing runner remained trustworthy.

## Known Limitations

- The written slice demo mentions scroll state, but the implemented and tested S08 contract covers top-level tab, selected class or globals view, search text, filter, and content sub-tab. Scroll restoration is not part of the shipped query contract in this slice.
- The manual browser-review line in the plan was not completed as a literal human pass in this environment because the browser automation CLI needed for that extra check was unavailable.

## Follow-ups

- S09 should preserve S08’s query contract while changing payload loading and caching behavior; URL-state restore order must stay stable even if class data becomes lazy-loaded.
- When older navigation checks are next touched, finish retiring hash-era assumptions instead of keeping long-term dual tolerance in legacy assertions.

## Files Created/Modified

- `js/app.js` — added query-backed navigation serialization/restoration, recent-class persistence, and inspectable runtime diagnostics.
- `index.html` — added the recent-classes header control and its inspectable container.
- `app.css` — styled the recent-classes trigger, panel, and items for the tightened header layout.
- `.gsd/test/s08_navigation_state.py` — added runtime coverage for restore, diagnostics, recent ordering, deduplication, pruning, and fallback behavior.
- `.gsd/test/run.py` — integrated S08 into the consolidated runner and report output.
- `.gsd/test/navigation.py` — updated the legacy history check to accept query-backed URLs while remaining hash-tolerant.
- `pytest.ini` — registered the `restore` and `recent` markers used by the S08 module.

## Forward Intelligence

### What the next slice should know
- S08 made query parameters the authoritative shareable navigation contract. Treat `?v=` plus `tab`, `class`, `search`, `filter`, and `ctab` as part of the public runtime surface now, because tests and users can both observe them.

### What's fragile
- Initial restore order in `js/app.js` is sensitive — API load, filter/search hydration, class-list rebuild, class selection, and source-tab activation all happen in a specific sequence. If S09 changes data loading granularity, this path is easy to break with race conditions.

### Authoritative diagnostics
- Inspect `#content[data-nav-*]`, `#recent-classes[data-recent-*]`, and `.gsd/test-reports/report.json` first — those are the most trustworthy signals because the standalone S08 module and consolidated runner both assert against them directly.

### What assumptions changed
- The old assumption was that hash routes were the main shareable navigation mechanism. In practice, S08 moved shareable state into query params and kept hashes only as a restore-only compatibility path.