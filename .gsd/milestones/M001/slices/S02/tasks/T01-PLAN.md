# T01: Middle-click new tab (FEAT-007)

**Status:** In Progress  
**Complexity:** Tiny  
**Estimate:** 45m  
**Depends on:** Tab system (shipped in S00/foundation)

## Why

Implements FEAT-007: middle-clicking or Ctrl+clicking a class link opens that class in a new tab rather than navigating the current tab.

## Files

- `pz-lua-api-viewer/js/app.js` — event delegation for click handling
- `pz-lua-api-viewer/app.css` — cursor hint for middle-click affordance (optional)

## Do

### Step 1: Read existing click handlers in app.js

The global click handler (`#content`) already has delegated handlers for:
- Source class refs (`a.src-class-ref`) — calls `selectClass(fqn)`
- Inheritance header class links (`a.inherit-link[data-fqn]`) — calls `selectClass(fqn)`
- Inherited method links (`a.inherit-method-link[data-fqn]`) — calls `selectClass(targetFqn, null, method)`

### Step 2: Add middle-click handler

Add to the click handler's loop (after the source class ref check):

```js
// Middle-click or Ctrl+click → open in new tab
if (e.button === 1 || e.ctrlKey) {
  const fqn = inheritMethod ? inheritMethod.dataset.fqn : a.dataset.fqn;
  if (fqn && API.classes[fqn]) {
    openNewTab(fqn);
    return;
  }
}
```

Place this after the existing `if (a)` block and before the `a.inherit-link` check.

### Step 3: Verify `openNewTab` exists

Read `state.js` to confirm `openNewTab()` is defined and available globally.

### Step 4: Test with browser screenshots

- Start server: `cd pz-lua-api-viewer && python server.py`
- Open browser, go to http://localhost:8765
- Find IsoPlayer class in sidebar, middle-click it
- Verify a new tab opens with that class loaded
- Take screenshot showing two tabs
- Test Ctrl+click as well

### Step 5: Optional cursor hint

Consider adding a cursor change on hover for elements with `[data-fqn]`:

```css
[data-fqn]:hover { cursor: alias; }
```

This signals the middle-click affordance.

## Acceptance Criteria

1. Middle-clicking any class link opens a new tab with that class
2. Ctrl+clicking any class link opens a new tab with that class
3. Left-click still navigates current tab (no regression)
4. No console errors in browser dev tools
5. Browser screenshot shows two tabs after middle-click

## Done When

- Middle-click opens new tab
- Ctrl+click opens new tab  
- Left-click still navigates current tab
