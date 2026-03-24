---
id: T03
parent: S07
milestone: M001
provides:
  - The main browser test runner now executes the S07 UX-polish module, writes a clean aggregate report, and uses current routes/selectors for the existing browser checks.
key_files:
  - .gsd/test/run.py
  - .gsd/test/s02_features.py
  - .gsd/test/navigation.py
  - .gsd/test/search.py
  - .gsd/test/globals.py
  - .gsd/test/detail.py
  - .gsd/test/inheritance.py
  - .gsd/milestones/M001/slices/S07/tasks/T03-PLAN.md
  - .gsd/test-reports/report.json
key_decisions:
  - Kept the existing hand-rolled browser runner but repaired it to manage its own server/browser lifecycle and register S07 by invoking the standalone pytest module from the consolidated report path.
patterns_established:
  - Legacy browser checks in `.gsd/test/run.py` run on a fresh page per test so hash-routed UI state and opened tabs do not leak between assertions.
observability_surfaces:
  - `.gsd/test-reports/report.json` now includes the S07 aggregate result alongside the legacy browser checks, and the standalone `.gsd/test/s07_ux_polish.py` module remains runnable for focused DOM-state inspection.
duration: 45m
verification_result: passed
completed_at: 2026-03-24
blocker_discovered: false
---

# T03: Close slice verification and docs for UX polish

**Repaired the browser test entrypoint, folded S07 into the consolidated runner, and closed the slice with a zero-failure report.**

## What Happened

The existing `.gsd/test/run.py` was not just missing S07 coverage. It was broken against the current Playwright API, did not manage its own local server, reused one browser page across all legacy checks, and depended on stale selectors/routes in the older browser tests. I kept the current runner shape so later slices can keep using the same entrypoint, but replaced its internals with a managed server lifecycle, a current Playwright browser/context harness, fresh-page isolation for each legacy test, and an aggregated S07 pytest invocation that is recorded in the same JSON report.

I then updated the legacy browser modules to match the current viewer contract: hash-based routes, current search/filter/globals/detail selectors, the copyable FQN surface, and current class/source navigation seams. That let `python .gsd/test/run.py` become trustworthy again instead of silently passing weak checks or failing before coverage started. I also added the missing `## Observability Impact` section to `T03-PLAN.md`, because this task changes how future agents inspect slice-level verification state.

## Verification

Verified the repaired consolidated runner end-to-end, then reran the standalone S07 module and the diagnostics-focused subset. The generated `.gsd/test-reports/report.json` now reports `failed: 0`, and the slice plan / test module paths named in the task contract exist on disk.

For the slice’s human-review intent, I exercised the real browser flows through Playwright-backed checks covering the same surfaces called out in the slice plan: hover-prefetch state before click, breadcrumb rendering/click behavior, and stable detail/source loading shells with inspectable diagnostics.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `python .gsd/test/run.py` | 0 | ✅ pass | 38.2s |
| 2 | `python -m pytest .gsd/test/s07_ux_polish.py` | 0 | ✅ pass | 10.04s |
| 3 | `python -m pytest .gsd/test/s07_ux_polish.py -k "loading_state or failure or diagnostics"` | 0 | ✅ pass | 1.45s |
| 4 | `python - <<'PY' ... report['failed'] == 0 ... PY` | 0 | ✅ pass | <1s |
| 5 | `python - <<'PY' ... Path('.gsd/milestones/M001/slices/S07/S07-PLAN.md').exists() ... PY` | 0 | ✅ pass | <1s |

## Diagnostics

Run `python .gsd/test/run.py` to execute the consolidated browser suite and regenerate `.gsd/test-reports/report.json`. Inspect the report’s `results`, `passed`, `failed`, and screenshot entries for the aggregate state, including the `s07-ux-polish` result. For focused UX-polish debugging, run `python -m pytest .gsd/test/s07_ux_polish.py` and inspect the existing DOM seams from earlier tasks: `#hover-preview[data-prefetch-state][data-prefetch-path]`, `#detail-panel[data-detail-state][data-detail-fqn]`, `#source-panel[data-source-state][data-source-path]`, and `#source-loading[data-source-state]`.

## Deviations

I updated several older browser test modules in addition to `.gsd/test/run.py`. That was not extra scope so much as the minimum needed to make the documented `python .gsd/test/run.py` command truthful again, because the runner was still pointing at stale routes and selectors from earlier UI revisions.

## Known Issues

Regression coverage in `.gsd/test/regression.py` is still fairly shallow compared with the newer S07 pytest module. It passes, but if future slices rely on those checks for stronger guarantees they should tighten those assertions rather than treating them as exhaustive.

## Files Created/Modified

- `.gsd/test/run.py` — repaired the consolidated browser runner, added managed server/browser lifecycle, isolated legacy tests per page, and registered the S07 pytest module in the aggregate report path.
- `.gsd/test/s02_features.py` — updated feature checks to current hash routing, current source-link seams, and a reliable new-tab gesture for automation.
- `.gsd/test/navigation.py` — updated navigation checks to current class-list/hash behavior and current back-button flow.
- `.gsd/test/search.py` — updated search/filter checks to the current global search input and compact filter control.
- `.gsd/test/globals.py` — updated globals coverage to the current globals panel route, search input, row selectors, and source-view state.
- `.gsd/test/detail.py` — updated detail coverage to the current breadcrumb/detail shell and copyable FQN surface.
- `.gsd/test/inheritance.py` — updated inheritance coverage to current rendered tree selectors.
- `.gsd/milestones/M001/slices/S07/tasks/T03-PLAN.md` — added the missing observability-impact section required by the pre-flight check.
- `.gsd/test-reports/report.json` — regenerated the aggregate browser test report with zero failures after the runner and test refresh.
