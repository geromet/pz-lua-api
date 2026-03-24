---
estimated_steps: 4
estimated_files: 3
skills_used:
  - test
---

# T03: Close slice verification and runner integration for navigation state

**Slice:** S08 — Navigation State
**Milestone:** M001

## Description

Finish S08 by folding the new navigation-state coverage into the normal verification entrypoint, running the full suite, and leaving the slice plan aligned with the actual commands and files on disk. This task exists so the slice ends with executable proof across both URL restoration and recent-history behavior.

## Steps

1. Read the final `S08-PLAN.md`, `.gsd/test/run.py`, and `.gsd/test/s08_navigation_state.py` to confirm the named verification targets are real and sufficient.
2. Register any missing S08 test import/command wiring in `.gsd/test/run.py` so the consolidated suite exercises the new navigation-state module alongside existing coverage.
3. Run the full suite and the standalone S08 module, then fix any report, harness, or selector drift exposed by the new stateful navigation tests.
4. Leave the plan and runner aligned so later slices can trust `python .gsd/test/run.py` and `python -m pytest .gsd/test/s08_navigation_state.py` without rereading slice history.

## Must-Haves

- [ ] The consolidated runner includes the S08 browser checks.
- [ ] The standalone S08 module remains runnable by path/module name.
- [ ] The slice plan’s Verification section still matches the real commands and files after implementation settles.

## Verification

- `python .gsd/test/run.py && python -m pytest .gsd/test/s08_navigation_state.py`
- `python - <<'PY'
from pathlib import Path
assert Path('.gsd/milestones/M001/slices/S08/S08-PLAN.md').exists()
assert Path('.gsd/test/s08_navigation_state.py').exists()
print('paths-ok')
PY`

## Inputs

- `.gsd/milestones/M001/slices/S08/S08-PLAN.md` — verification contract to keep accurate
- `.gsd/test/run.py` — consolidated browser runner
- `.gsd/test/s08_navigation_state.py` — slice-specific navigation-state coverage

## Expected Output

- `.gsd/test/run.py` — full-suite registration for S08 tests
- `.gsd/test/s08_navigation_state.py` — finalized runnable S08 coverage
- `.gsd/milestones/M001/slices/S08/S08-PLAN.md` — verification text aligned with shipped commands and files

## Observability Impact

- Signals changed: the consolidated runner now reports S08 navigation-state results in `.gsd/test-reports/report.json` alongside the existing slice/module outcomes.
- How a future agent inspects this: run `python .gsd/test/run.py`, inspect `.gsd/test-reports/report.json`, or run `python -m pytest .gsd/test/s08_navigation_state.py` in isolation.
- Failure state exposed: restoration/recent-state regressions surface as named S08 failures in the main report rather than disappearing into manual browser-only checks.
