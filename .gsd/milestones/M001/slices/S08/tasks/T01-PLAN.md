---
estimated_steps: 5
estimated_files: 3
skills_used:
  - test
---

# T01: Serialize and restore shareable URL navigation state

**Slice:** S08 — Navigation State
**Milestone:** M001

## Description

Turn the current hash-first navigation into an explicit shareable state contract. The viewer should serialize the active browsing context into the URL, restore it on fresh load after API data is ready, preserve the existing version selector query parameter, and expose inspectable diagnostics so restoration failures are visible instead of silent.

## Steps

1. Read the current navigation/search/filter/tab orchestration in `js/app.js` and the consolidated runner in `.gsd/test/run.py` before changing behavior.
2. Introduce explicit serialize/parse/apply helpers in `js/app.js` that preserve `?v=` while encoding filter, search text, top-level tab, content tab, and class/globals selection in a stable URL format.
3. Update URL state from real user actions without full reload, keeping the existing back/forward model coherent and avoiding duplicate or conflicting history writes.
4. Add DOM-visible navigation-state diagnostics for the last parsed/applied state and invalid-input fallback so failures are assertable in tests and devtools.
5. Create `.gsd/test/s08_navigation_state.py` with browser checks for fresh-load restoration plus at least one invalid/partial-state diagnostic case, and register it in `.gsd/test/run.py` only as needed for isolated execution.

## Must-Haves

- [ ] URL state preserves the active viewer state without clobbering the existing `?v=` version selection contract.
- [ ] Initial page load restores the encoded state in the correct order instead of applying state before API data, tabs, or panels exist.
- [ ] Invalid or unresolvable URL state leaves an inspectable fallback/error marker in the DOM rather than silently failing.

## Verification

- `python -m pytest .gsd/test/s08_navigation_state.py -k "restore or diagnostics or failure"`
- Manual check in the browser: load a crafted URL in a fresh tab and confirm the search box, filter control, selected class/globals view, and detail/source state all match the encoded values.

## Observability Impact

- Signals added/changed: DOM data attributes for serialized/applied navigation state, restore status, and invalid-state fallback/error details.
- How a future agent inspects this: browser devtools on the chosen shell/control element plus Playwright assertions in `.gsd/test/s08_navigation_state.py`.
- Failure state exposed: missing class, unsupported tab/filter values, or malformed URL state remains visible through a stable DOM marker even when the UI falls back.

## Inputs

- `js/app.js` — existing hash, tab, search, filter, and history orchestration
- `.gsd/test/run.py` — consolidated browser runner to extend carefully
- `.gsd/milestones/M001/slices/S08/S08-PLAN.md` — slice verification contract and diagnostic expectations

## Expected Output

- `js/app.js` — explicit URL state serialization/restoration and inspectable diagnostics
- `.gsd/test/s08_navigation_state.py` — browser coverage for URL restoration and failure-state diagnostics
- `.gsd/test/run.py` — runnable S08 test entry if required
