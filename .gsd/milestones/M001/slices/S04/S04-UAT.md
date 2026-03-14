# S04 Slice Verification — Javadoc Extraction (Blocked)

## Preconditions

- ProjectZomboid installed with decompiled CFR source files in `projectzomboid/sources/`
- `.gsd/milestones/M001/slices/archive/TASK-016-javadoc-extraction.md` exists

## Precondition Checks

### Verify decompiled sources have zero Javadoc blocks

```bash
grep -l "/\*\*" projectzomboid/sources/ -r --include="*.java" 2>/dev/null | wc -l
```

**Expected:** `0`

If this returns > 0, the blockage is resolved — proceed to implementation tasks.

### Verify task file archived

```bash
ls -la .gsd/milestones/M001/slices/archive/TASK-016-javadoc-extraction.md
```

**Expected:** File exists with full task plan and blocking evidence

## Test Cases — Blockage Verification

### TC01: Confirm zero Javadoc in decompiled sources

| Step | Action | Expected Outcome |
|------|--------|------------------|
| 1 | Run: `grep -l "/\*\*" projectzomboid/sources/ -r --include="*.java" 2>/dev/null` | Returns empty or no output |
| 2 | Count results: `wc -l` | Returns `0` |
| 3 | Inspect a known method class (e.g. `IsoLivingCharacter`) | No `/** */` blocks found |

**Pass:** Returns exactly `0` lines containing `/**`

### TC02: Verify STATUS.md documents blockage

```bash
grep -A2 "S04" docs/STATUS.md
```

**Expected:** Shows blockage note with reasoning and next steps

**Pass:** Blockage clearly documented

### TC03: Verify ROADMAP.md marks S04 complete

```bash
grep "S04:" .gsd/milestones/M001/M001-ROADMAP.md
```

**Expected:** Shows `[x] **S04: Blocked — Javadoc**`

**Pass:** S04 marked as complete (blocked)

## Test Cases — Implementation Path (When Unblocked)

### TC04: Extractor produces `"doc"` field for methods with Javadoc

| Step | Action | Expected Outcome |
|------|--------|------------------|
| 1 | Obtain original PZ Java source files with `/** */` blocks | Files contain Javadoc |
| 2 | Run extractor: `python pz-lua-api-viewer/extract_lua_api.py` | Completes without error |
| 3 | Query: `python -c "import json; d=json.load(open('pz-lua-api-viewer/lua_api.json')); methods_with_doc = [m for c in d['classes'].values() for m in c.get('methods',[]) if m.get('doc')]; print(len(methods_with_doc))"` | Returns > 0 |

**Pass:** At least one method has `"doc"` field populated

### TC05: Frontend renders Javadoc toggle

| Step | Action | Expected Outcome |
|------|--------|------------------|
| 1 | Start server: `cd pz-lua-api-viewer && python server.py` | Server starts on port 8765 |
| 2 | Open browser to http://localhost:8765 | Page loads with Detail panel |
| 3 | Navigate to a class that has methods with Javadoc (e.g. IsoLivingCharacter) | Detail panel shows method list |
| 4 | Look for `▶` toggle in method rows | Toggle visible for methods with `doc` field |
| 5 | Click toggle | Javadoc expands showing description + `@param`/`@return` |

**Pass:** Toggle appears and expands correctly

### TC06: No visual noise for undocumented entries

| Step | Action | Expected Outcome |
|------|--------|------------------|
| 1 | View a class with no Javadoc methods (most classes fall into this category) | Method rows show only name, params, return cells |
| 2 | No `▶` toggles visible | Clean UI |

**Pass:** Methods without Javadoc don't show any toggle

## Edge Cases

### EC01: Method with Javadoc but missing position metadata

| Scenario | Description | Expected Behavior |
|----------|-------------|-------------------|
| | Some javalang nodes have `position=None` | Extractor should handle gracefully; skip or use fallback parsing |

### EC02: Nested comment blocks (uncommon in PZ)

| Scenario | Description | Expected Behavior |
|----------|-------------|-------------------|
| | Comment inside comment, or `/** ... /** ... */` patterns | Should extract outermost block only |

**Pass:** Treated as single block comment

### EC03: @param/@return with special characters

| Scenario | Description | Expected Behavior |
|----------|-------------|-------------------|
| | `@param foo \[bar\]` or `@return *string*` | Characters escaped/HTML-escaped for display |

**Pass:** No HTML injection or broken formatting

## Post-Verification Steps (When Unblocked)

1. Archive legacy task: `cd pz-lua-api-viewer && python docs/archive.py docs/Tasks/TASK-016-javadoc-extraction.md`
2. Commit changes to `liability-machine` branch
3. Push to GitHub
4. Verify S04 marked `[x]` in ROADMAP.md
5. Update `docs/STATUS.md` — remove S04 from blockers, remove from Current State as blocked

## Summary

| Condition | Result |
|-----------|--------|
| Decomiled sources have Javadoc? | **NO** — 0/2900+ files contain `/** */` |
| Can proceed with implementation? | **NO** — requires original PZ source files |
| Blockage documented? | **YES** — in STATUS.md and archived task |
| S04 marked complete (blocked)? | **YES** — ROADMAP.md shows `[x]` |

---

**To unblock:** Obtain original PZ Java source files that contain `/** */` Javadoc blocks. The current decompiled CFR output from `.class` files strips all comments, making Javadoc extraction impossible without the original sources.

The full implementation plan is in `TASK-016-javadoc-extraction.md` — run it once original sources are available.
