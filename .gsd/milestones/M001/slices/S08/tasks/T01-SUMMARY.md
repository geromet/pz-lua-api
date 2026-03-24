---
id: T01
parent: S08
milestone: M001
provides:
  - Shareable query-parameter navigation state with DOM-visible restore diagnostics and S08 browser coverage
key_files:
  - js/app.js
  - .gsd/test/s08_navigation_state.py
  - .gsd/DECISIONS.md
key_decisions:
  - D004: Serialize viewer state into query params while preserving `?v=` and accepting legacy hashes as restore-only fallback
patterns_established:
  - Expose navigation restore state through `data-nav-*` attributes on `#content` so browser tests and future agents can assert parsed, applied, and rejected URL state directly
observability_surfaces:
  - "#content[data-nav-restore-status|data-nav-serialized-state|data-nav-parsed-state|data-nav-applied-state|data-nav-restore-error|data-nav-url-state-source]"
duration: 1h25m
verification_result: passed
completed_at: 2026-03-24T18:47:31+01:00
blocker_discovered: false
---

# T01: Serialize and restore shareable URL navigation state

**Added query-backed viewer state restoration, preserved `?v=` across live URL updates, and exposed restore diagnostics with new S08 Playwright coverage.**

## What Happened

`js/app.js` now treats the viewer state as an explicit query-parameter contract instead of a hash-only contract. I added helpers to serialize, parse, sanitize, and apply navigation state for the active top-level tab, selected class, search text, filter, and content sub-tab while preserving the existing version selector query parameter.

Initial load now restores state only after API data and UI controls are ready. Invalid input is no longer silent: `#content` exposes the parsed state, applied state, restore status, source, and any restore error through stable `data-nav-*` attributes. Legacy `#globals` / `#<fqn>` links still restore as a fallback input path, but live state updates are written back to the explicit query contract.

I added `.gsd/test/s08_navigation_state.py` with Playwright coverage for live URL serialization, crafted-URL restoration into source view, and an unresolvable-class fallback case that asserts the inspectable diagnostics and sanitized fallback URL.

## Verification

Ran the standalone S08 pytest module required by the task plan. It passed all three navigation-state checks covering serialization, restoration, and failure diagnostics.

Manual browser review was not run in this task context.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `python -m pytest .gsd/test/s08_navigation_state.py -k "restore or diagnostics or failure"` | 0 | ✅ pass | 3.58s |

## Diagnostics

Inspect `#content` in devtools or Playwright and read:
- `data-nav-serialized-state`
- `data-nav-parsed-state`
- `data-nav-applied-state`
- `data-nav-restore-status`
- `data-nav-restore-reason`
- `data-nav-restore-error`
- `data-nav-url-state-source`
- `data-nav-url-state-href`

Failure cases now surface `rejected` or `fallback` status instead of silently landing on the wrong view.

## Deviations

None.

## Known Issues

- `.gsd/test/s08_navigation_state.py` uses a new `restore` pytest marker that is not registered yet, so pytest emits warnings even though the module passes.
- Slice-level runner integration and the manual browser review remain for later tasks in S08.

## Files Created/Modified

- `js/app.js` — added query-backed navigation serialization/restoration, URL syncing, and DOM diagnostics
- `.gsd/test/s08_navigation_state.py` — added Playwright coverage for URL restore, serialization, and failure diagnostics
- `.gsd/DECISIONS.md` — recorded the URL-state contract decision as D004
