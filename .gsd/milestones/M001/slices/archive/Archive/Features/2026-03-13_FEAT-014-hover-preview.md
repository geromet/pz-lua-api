> **COMPLETED 2026-03-13** — 400ms hover preview card on any `[data-fqn]` element; shows name, FQN, badges, method count, top-3 callable methods; viewport-clamped; dismisses on click/scroll. Shipped in bf40c8f.

# FEAT-014: Hover preview window for class/method links

**Status:** Planned
**Priority:** Low
**Complexity:** Medium

## Description

When hovering over a class reference link (in source code, inheritance header, or inherited methods table), show a small floating preview panel with the target class's key info: description (if available from FEAT-010), method count, inheritance line.

## Proposed UX

- After a 400ms hover delay, a small card appears near the cursor with:
  - Class simple name + FQN
  - `setExposed` / `@UsedFromLua` tags
  - First 3–5 methods (sorted by relevance/alphabetical)
  - "Click to open" hint
- Moving off the link or clicking dismisses the preview.
- Preview is positioned to avoid overflowing the viewport edge.

## Implementation Notes

- A single shared `<div id="hover-preview">` is positioned absolutely and populated on hover.
- Use `mouseenter` + `setTimeout` for delay; `mouseleave` clears the timer.
- Data comes from `API.classes[fqn]` — no additional fetch needed.

## Notes

- Low priority — nice UX touch but not blocking any core workflow.
- Must not appear on mobile/touch (no hover events on touch devices, so it naturally won't).
- Implement only after FEAT-006 (tabs) since tabs change the interaction model significantly.
