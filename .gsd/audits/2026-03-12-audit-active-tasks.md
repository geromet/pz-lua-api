## Active Tasks (Priority Order)

All active tasks are located in `pz-lua-api-viewer/docs/Tasks/`. They are ordered by priority and should be worked through in sequence unless dependencies require a different order.

### TASK-012: Tab bar system (FEAT-006)
**Priority:** High  
**Touches:** `index.html`, `app.css`, `js/state.js`, `js/app.js`, `js/class-detail.js`  
**Resolves:** FEAT-006 (tab bar system)  
**Recommended after:** TASK-011 (split panels)  
**Unlocks:** FEAT-004 (resizable panels), FEAT-007 (middle-click new tab), FEAT-014 (hover preview)

A browser-style tab bar that allows multiple classes to be open simultaneously, each preserving scroll position and search state.

### TASK-013: Search improvements (FEAT-013)
**Priority:** Medium  
**Touches:** `js/class-list.js`, `js/class-detail.js`, `app.css`, `index.html`  
**Resolves:** FEAT-013 (search highlight + clear buttons)

Adding `<mark>` highlights to search results and clear (×) buttons for search inputs.

### TASK-014: Globals sticky headers + fold memory (FEAT-011, FEAT-012)
**Priority:** Medium  
**Touches:** `js/globals.js`, `app.css`  
**Resolves:** FEAT-011 (partial), FEAT-012

Implement sticky table headers and persist group fold state in `localStorage`.

### TASK-015: Wildcard import fallback (FEAT-009 Problem B)
**Priority:** Medium  
**Touches:** `js/source-viewer.js`  
**Resolves:** FEAT-009 Problem B

Handle `import zombie.characters.*;` wildcard imports for class-ref linking.

### TASK-016: Javadoc extraction and display (FEAT-010)
**Priority:** Medium  
**Touches:** `extract_lua_api.py`, `js/class-detail.js`, `app.css`  
**Resolves:** FEAT-010

Extract and display Javadoc comments in the Detail panel.

### TASK-017: Build-time precomputation (FEAT-008)
**Priority:** Low  
**Touches:** `extract_lua_api.py`, `js/app.js`, `js/state.js`  
**Resolves:** FEAT-008

Move class-by-simple-name computation to build time for faster startup.

### TASK-018: Version selector (FEAT-005)
**Priority:** Low-Medium  
**Touches:** `extract_lua_api.py`, `index.html`, `app.css`, `js/app.js`, `js/state.js`  
**Resolves:** FEAT-005

Add version dropdown to switch between pre-extracted API versions.
