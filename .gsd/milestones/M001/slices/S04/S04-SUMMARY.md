---
id: S04
parent: M001
milestone: M001
provides:
  - marked as complete (blocked)
requires:
  - none
affects:
  - downstream slices that might attempt Javadoc extraction
key_files:
  - .gsd/milestones/M001/M001-ROADMAP.md
  - docs/STATUS.md
  - .gsd/milestones/M001/slices/archive/TASK-016-javadoc-extraction.md
key_decisions:
  - S04 blocked: decompiled CFR sources contain zero `/**` blocks; Javadoc extraction requires original PZ source files with Javadoc intact
patterns_established:
  - blocked slice documented in STATUS.md and ROADMAP.md
observability_surfaces:
  - none
drill_down_paths:
  - .gsd/milestones/M001/slices/archive/TASK-016-javadoc-extraction.md — full task plan and blocking evidence
duration: ~2h research (confirmed block)
verification_result: passed
completed_at: 2026-03-14
---

# S04: Javadoc Extraction (Blocked)

**S04 marked complete — no implementation shipped. The original PZ Java source files with Javadoc intact are required to unblock this slice.**

## What Happened

The slice plan was researched and the blocking condition confirmed: decompiled CFR sources contain zero `/**` Javadoc blocks because comments are stripped during decompilation. This is a fundamental constraint, not an implementation gap.

**Blockage evidence:**
- Scanned all 2900+ `.java` files in `projectzomboid/sources/`
- Zero files contain `/**` markers
- Task file archived at `.gsd/milestones/M001/slices/archive/TASK-016-javadoc-extraction.md`

**What's ready to implement once unblocked:**
- Extractor code in `extract_lua_api.py` (see TASK-016 plan)
- Frontend toggle UI in `js/class-detail.js` and `app.css`

## Verification

- Confirmed zero Javadoc blocks in decompiled sources via bash grep
- S04 marked `[x]` in `.gsd/milestones/M001/M001-ROADMAP.md`
- Blockage documented in `docs/STATUS.md`
- Task file archived with full context

## Requirements Advanced

None — slice was blocked.

## Requirements Validated

None — slice was blocked.

## New Requirements Surfaced

None.

## Deviations From Plan

The slice plan anticipated this scenario (see "Blocked" section). No actual deviation occurred.

## Known Limitations

- Requires original PZ Java source files (not decompiled CFR output) for Javadoc extraction
- Cannot proceed until those sources are available

## Follow-ups

- Obtain original PZ Java sources with Javadoc intact
- Implement TASK-016 per the archived task plan
- Regenerate `lua_api.json` via `python pz-lua-api-viewer/extract_lua_api.py`
- Add `"doc"` field to method/field entries in JSON
- Implement frontend toggle UI to display Javadoc in Detail panel

## Files Created/Modified

- `.gsd/milestones/M001/M001-ROADMAP.md` — S04 marked `[x]` complete
- `docs/STATUS.md` — added blockage note for S04
- `.gsd/milestones/M001/slices/archive/TASK-016-javadoc-extraction.md` — archived task plan

## Forward Intelligence

### What the next slice should know

**To unblock:** Obtain original PZ Java source files that contain `/**` Javadoc blocks. The decompiled CFR output from `.class` files strips all comments, making Javadoc extraction impossible without the original sources.

### What's fragile

None — no implementation shipped.

### Authoritative diagnostics

Run: `grep -l "/\*\*" projectzomboid/sources/ -r --include="*.java" 2>/dev/null | wc -l`
This will return 0 for decompiled sources. With original sources, it will show files with Javadoc.

### What assumptions changed

The slice plan already accounted for this possibility in its "Blocked" section. No assumption was violated.
