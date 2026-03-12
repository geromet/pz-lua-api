# FEAT-007: Middle-click to open class in new tab

**Status:** Planned
**Priority:** Low
**Complexity:** Tiny
**Depends on:** FEAT-006 (tab bar system)

## Description

Middle-clicking (or Ctrl+clicking) a class link — whether in the sidebar, inheritance header, inherited methods table, or source class refs — should open that class in a new tab rather than navigating the current tab.

## Implementation

Once FEAT-006 tabs exist, add a `mousedown` handler (or augment existing click handlers) that checks `e.button === 1` (middle click) or `e.ctrlKey`. On match, call `openNewTab(fqn)` instead of `selectClass(fqn)`.
