---
estimated_steps: 4
estimated_files: 3
skills_used:
  - test
---

# T03: Close slice verification and docs for UX polish

**Slice:** S07 — UX Polish
**Milestone:** M001

## Description

Finish the slice by folding the new S07 coverage into the normal test entrypoint, running the full suite, and making sure the documented verification commands and output files match the real repo state. This task exists so the slice ends with executable proof, not just changed UI code.

## Steps

1. Read the final `S07-PLAN.md`, `.gsd/test/run.py`, and `.gsd/test/s07_ux_polish.py` to confirm the named verification targets are real.
2. Register any missing S07 test imports/calls in `.gsd/test/run.py` so the full suite executes the new UX-polish checks.
3. Run the full suite and the standalone S07 test module, then fix any verification drift in the test runner or slice plan if commands/files changed during implementation.
4. Leave the plan and test entrypoint aligned so a later slice can trust `python .gsd/test/run.py` and the standalone S07 command without rereading implementation history.

## Must-Haves

- [ ] The main automated runner includes the S07 checks.
- [ ] The standalone S07 test module remains runnable by path/module name.
- [ ] The slice plan’s Verification section matches the actual executable commands and files on disk.

## Verification

- `python .gsd/test/run.py && python -m pytest .gsd/test/s07_ux_polish.py`
- `python - <<'PY'
from pathlib import Path
assert Path('.gsd/milestones/M001/slices/S07/S07-PLAN.md').exists()
assert Path('.gsd/test/s07_ux_polish.py').exists()
print('paths-ok')
PY`

## Inputs

- `.gsd/milestones/M001/slices/S07/S07-PLAN.md` — verification contract to keep accurate
- `.gsd/test/run.py` — main automated runner
- `.gsd/test/s07_ux_polish.py` — slice-specific browser coverage

## Expected Output

- `.gsd/test/run.py` — full-suite registration for S07 tests
- `.gsd/test/s07_ux_polish.py` — finalized runnable S07 coverage
- `.gsd/milestones/M001/slices/S07/S07-PLAN.md` — verification text still aligned with reality if any command/path drift was fixed

## Observability Impact

- Signals changed: the main runner now needs to surface S07 UX-polish results alongside the existing JSON report so prefetch/loading/breadcrumb regressions show up in the same report file future agents already inspect.
- How to inspect: run `python .gsd/test/run.py` for the consolidated report, `python -m pytest .gsd/test/s07_ux_polish.py` for the slice module in isolation, and inspect `.gsd/test-reports/report.json` for aggregated pass/fail status.
- Failure visibility: if S07 coverage fails from the main runner, the failing UX-polish test names and report counters must make that visible without rereading task history.
