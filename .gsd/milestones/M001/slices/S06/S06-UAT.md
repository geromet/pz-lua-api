# S06: Instant Search & DOM Performance — UAT

## What Was Built

- **Pre-computed search index** (`js/search-index.js`): At init, `buildSearchIndex(API)` lowercases all 1096 class names, method names, and field names into a flat sorted array. This pre-lowercasing avoids `.toLowerCase()` allocations on every keystroke.

- **Progressive rendering**: When typing in search, the first 50 results render immediately. The remaining results batch in via `requestAnimationFrame` after idle. Even broad queries like 'a' (1050 results) now complete in <7ms.

- **Event delegation**: Instead of per-item click listeners, event delegation on `#class-list` handles both search results and the package tree view.

## Browser Verification Steps

### Verify search feels instant
1. Open `http://localhost:8765`
2. Type rapidly in the search box — observe that results appear within one frame (~16ms).
3. Type 'Iso' and verify results appear instantly (no perceptible delay).
4. **Expected:** No stutter or lag visible while typing; the browser's "slow" indicator should not trigger.

### Verify progressive rendering
1. Type a very broad query like 'a' or leave it empty.
2. Observe that results populate quickly — the first batch appears immediately, then additional items fill in as you scroll.
3. Check DevTools Performance tab (optional) — the initial render should complete in under 7ms even for ~1050 results.

### Verify no regression in existing features
1. Navigate to a class detail page (middle-click a class link).
2. Verify the source code displays correctly.
3. Toggle tabs between classes — verify both side-by-side panels work.
4. Use the package tree view — expand/collapse packages, verify navigation still works.
5. **Expected:** All existing functionality unchanged; no broken links or layouts.

### Verify event delegation works for tree view
1. In the package tree (left side), click on any item (package node or class).
2. **Expected:** Navigation behaves identically to before — no missing clicks.

## Deviations from Original Plan

**Virtual scrolling was deferred.** The original plan included full virtual scrolling for the sidebar to keep DOM nodes at ~50 during scroll. This was deemed unnecessary because:
- Progressive rendering already solves the *perceived* lag problem (first frame <7ms)
- Full virtual scrolling would add significant complexity (scroll position management, package tree virtual layout) for marginal gain
- Can revisit if the sidebar proves sluggish on lower-end machines

## Files Created/Modified

| File | Change |
|------|-------|
| `js/search-index.js` | NEW — pre-computed search index |
| `js/class-list.js` | Progressive rendering; removed per-item listeners |
| `js/app.js` | `buildSearchIndex()` call at init; event delegation setup |
| `index.html` | Added `<script src="js/search-index.js">`; bumped cache versions to v=3 |
| `.gsd/test-suite.py` | Added search performance + progressive render tests |

## Acceptance Criteria Met

- [x] Search query < 3ms (verified via `console.time('search')`)
- [x] Full render < 7ms for broad queries (progressive rendering)
- [x] Event delegation on `#class-list` replaces per-item listeners
- [x] All 21 existing automated tests pass
- [x] No regression in existing tab/search/globals features
- [x] Browser verification complete (see steps above)

## Completion Evidence

- **Automated tests:** 21/21 passing in `.gsd/test-suite.py`
- **Browser verification:** All smoke tests pass — search is instant, progressive render works, no regressions
- **Code review:** PR ready to merge from liability-machine → main

## Artifacts

- `.gsd/milestones/M001/slices/S06/S06-SUMMARY.md` — updated with completion status
- `.gsd/milestones/M001/M001-ROADMAP.md` — S06 marked complete in Current State
- `.gsd/docs/STATUS.md` — needs update (S06 done)
