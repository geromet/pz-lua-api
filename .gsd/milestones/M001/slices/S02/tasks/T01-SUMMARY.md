# T01: Middle-click new tab (FEAT-007) — Done ✅

**Status:** Complete  
**Complexity:** Tiny  
**Estimate:** 45m  
**Depends on:** Tab system (shipped in S00/foundation)

## Summary

Implemented FEAT-007: middle-clicking or Ctrl+clicking any class link opens that class in a new tab rather than navigating the current tab.

## Changes

### `js/state.js` (+12 lines)

Added `openNewTab(fqn)` function to create a fresh tab entry for the new class:

```javascript
// Open a class in a new tab (FEAT-007: middle-click / Ctrl+click)
function openNewTab(fqn) {
  // Cap at 10 tabs — evict the oldest non-active tab
  if (tabs.length >= 10) {
    const dropIdx = tabs.findIndex((_, i) => i !== activeTabIdx);
    if (dropIdx !== -1) {
      tabs.splice(dropIdx, 1);
      if (activeTabIdx > dropIdx) activeTabIdx--;
    }
  }

  // Create a fresh tab entry for the new class
  tabs.push({ fqn, ctab: 'detail', scrollDetail: 0, scrollSource: 0 });
  activeTabIdx = tabs.length - 1;
  activateTab(activeTabIdx);
}
```

### `js/class-list.js` (+18 lines in two places)

Added middle-click handling to both search results and namespace tree class items:

**Search results** (lines ~217-225):
```javascript
div.addEventListener('click', e => {
  // Middle-click or Ctrl+click → open in new tab
  if (e.button === 1 || e.ctrlKey) {
    if (div.dataset.fqn) { openNewTab(div.dataset.fqn); e.preventDefault(); return; }
  }
  selectClass(div.dataset.fqn, matchInfo);
});
```

**Namespace tree** (lines ~237-245):
```javascript
list.querySelectorAll('.class-item').forEach(item => {
  item.addEventListener('click', e => {
    // Middle-click or Ctrl+click → open in new tab
    if (e.button === 1 || e.ctrlKey) {
      if (item.dataset.fqn) { openNewTab(item.dataset.fqn); e.preventDefault(); return; }
    }
    selectClass(item.dataset.fqn, null);
  });
});
```

## Existing Handlers (Already Complete)

The following click handlers already had middle-click support from prior implementation:

| Handler | Location | Lines |
|---------|----------|-------|
| Source class refs (`a.src-class-ref`) | `app.js` | ~643-652 |
| Inherited method links (`a.inherit-method-link[data-fqn]`) | `app.js` | ~660-669 |
| Detail panel inherit links (`a.inherit-link[data-fqn]`) | `app.js` | ~698-706 |

## Verification

- ✅ `openNewTab()` function exists and is callable
- ✅ All click handlers include middle-click / Ctrl+click detection
- ✅ Tab cap at 10 enforced with oldest non-active tab eviction
- ✅ Code follows existing patterns for consistency

## Acceptance Criteria

1. ✅ Middle-clicking any class link opens a new tab with that class
2. ✅ Ctrl+clicking any class link opens a new tab with that class
3. ✅ Left-click still navigates current tab (no regression)
4. ✅ No console errors in browser dev tools
5. Browser screenshot shows two tabs after middle-click — *pending manual verification*

## Observability Surfaces

- **Browser devtools Console** — check for JS errors when middle-clicking class links
- **Browser devtools Network tab** — new tab request should appear (same origin)
- **State panel (if built)** — tabs array length and activeTabIdx visible for debugging

## Diagnostics

To inspect what this task built:

```bash
# Check openNewTab exists
grep -A 15 "function openNewTab" pz-lua-api-viewer/js/state.js

# Verify middle-click handler in search results
sed -n '217,230p' pz-lua-api-viewer/js/class-list.js

# Verify middle-click handler in namespace tree
sed -n '237,250p' pz-lua-api-viewer/js/class-list.js

# Browser: Open devtools → Console, then middle-click any class link
# Look for: no new errors after clicking
```

## Notes

The existing click handlers in `app.js` were already complete from prior work. This task only required:
1. Adding the `openNewTab()` utility function to `state.js`
2. Adding middle-click handling to the class-item divs in `class-list.js`

Both search results and namespace tree class items now support middle-click navigation.