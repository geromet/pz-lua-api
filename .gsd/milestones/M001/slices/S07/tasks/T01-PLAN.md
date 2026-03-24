---
estimated_steps: 5
estimated_files: 4
skills_used:
  - frontend-design
  - test
---

# T01: Wire hover prefetch and inspectable loading state

**Slice:** S07 — UX Polish
**Milestone:** M001

## Description

Implement the interaction/runtime half of S07 by extending the existing delegated hover-preview flow. Hovering an eligible class link should warm the shared source cache after a short delay, short hovers should cancel cleanly, and the UI should expose inspectable state so future agents and browser tests can tell whether prefetch and source loading are idle, pending, complete, or failed.

## Steps

1. Read the current hover-preview logic in `js/app.js` and the source loading/cache path in `js/source-viewer.js` before changing behavior.
2. Add delayed hover prefetch for `[data-fqn]` targets with a real `source_file`, reusing `sourceCache` and avoiding duplicate fetches for already-cached or already-pending sources.
3. Expose durable DOM state for prefetch/source loading on existing rendered elements so automated tests can assert pending/success/error without scraping visual text alone.
4. Add slice-specific browser coverage in `.gsd/test/s07_ux_polish.py` for hover prefetch success and for an inspectable loading-state transition or failure surface.
5. Register or update the S07 test entry in `.gsd/test/run.py` only as needed so this task’s tests are executable in isolation during implementation.

## Must-Haves

- [ ] Prefetch uses the same source-fetch/cache path as normal source opening rather than a second bespoke fetch implementation.
- [ ] Quick hover exit cancels the scheduled prefetch before network work starts, and already-warmed sources do not refetch on repeated hover.
- [ ] At least one DOM-visible state marker makes pending/success/error inspectable in tests and manual debugging.

## Verification

- `python -m pytest .gsd/test/s07_ux_polish.py -k "prefetch or loading_state"`
- Manual check in the browser: hover a class link, inspect the relevant element state/class, then click through and confirm source opens without waiting for a fresh fetch.

## Observability Impact

- Signals added/changed: prefetch/source state attributes or classes for idle, pending, ready, and error.
- How a future agent inspects this: browser devtools on `#hover-preview`, `#source-panel`, or the chosen state-bearing element plus Playwright assertions in `.gsd/test/s07_ux_polish.py`.
- Failure state exposed: the UI still shows whether prefetch never started, is stuck pending, or failed and fell back to the existing source error surface.

## Inputs

- `js/app.js` — existing delegated hover preview behavior and event timing
- `js/source-viewer.js` — shared source fetch and cache path
- `.gsd/milestones/M001/slices/S07/S07-PLAN.md` — slice goal and required verification

## Expected Output

- `js/app.js` — hover prefetch wiring and state updates
- `js/source-viewer.js` — shared prefetch/cache/state support
- `.gsd/test/s07_ux_polish.py` — automated hover prefetch coverage
- `.gsd/test/run.py` — executable test runner entry if required
