# S05: Resizable Sidebar (FEAT-004)

**Goal:** Ship FEAT-004 — draggable splitter between sidebar and content area, with localStorage persistence.
**Demo:** Drag the handle between sidebar and content to resize. Width persists across reload.

## Must-Haves

- Sidebar is resizable via a draggable splitter
- Minimum sidebar width: 150px
- Width persists in localStorage (key: `splitW-sidebar`)
- No regression in split layout mode (detail/source splitter still works)

## Verification

- Browser: drag sidebar splitter → sidebar width changes
- Browser: reload → saved width restored
- Browser: select a class, switch to split layout → both splitters work independently
- No console errors

## Tasks

- [x] **T01: Sidebar splitter (FEAT-004)** `est:30m`
  - Why: Implements FEAT-004; reuses existing `initSplitter()` infrastructure
  - Files: `index.html`, `app.css`, `js/app.js`
  - Done: Added `#sidebar-splitter` div between sidebar and content; CSS makes it always visible; `initSplitter('sidebar-splitter', 'sidebar', 'splitW-sidebar')` wired in init()

- [ ] **T02: Update docs and commit S05** `est:10m`
  - Why: Archive FEAT-004; update STATUS.md
  - Files: `pz-lua-api-viewer/docs/STATUS.md`, `pz-lua-api-viewer/docs/Planned_Features/FEAT-004-resizable-panels.md`
  - Do: Archive FEAT-004. Update STATUS.md. Commit and push.
  - Done when: pushed to `liability-machine`

## Files Touched

- `pz-lua-api-viewer/index.html`
- `pz-lua-api-viewer/app.css`
- `pz-lua-api-viewer/js/app.js`
