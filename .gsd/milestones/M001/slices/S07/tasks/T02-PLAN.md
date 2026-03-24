---
estimated_steps: 5
estimated_files: 4
skills_used:
  - frontend-design
  - make-interfaces-feel-better
---

# T02: Recompose header, filters, breadcrumbs, and stable layout styling

**Slice:** S07 — UX Polish
**Milestone:** M001

## Description

Implement the visible polish pass across the viewer shell and detail view. The work should reduce chrome density, keep the existing filtering/navigation model intact, render breadcrumbs from class FQNs, and reserve stable space for detail/source loading so the UI does not visibly jump while content arrives.

## Steps

1. Read `index.html`, `app.css`, `js/app.js`, and `js/class-detail.js` together so the new header/sidebar structure matches current event wiring.
2. Replace the always-visible sidebar filter button wall with a compact dropdown or similarly compact control that still maps cleanly onto the existing `currentFilter` values in `js/app.js`.
3. Compress header stats into a quieter summary layout, keeping version selection and local-source controls usable without reintroducing clutter.
4. Add breadcrumb rendering at the top of the detail panel from the selected class FQN, and wire breadcrumb clicks into existing navigation/filter behavior rather than creating a parallel state model.
5. Tighten palette and typography tokens in `app.css`, and add stable loading/skeleton or placeholder sizing so opening a class/source does not cause obvious height or width jumps.

## Must-Haves

- [ ] The new filter control preserves all current filter options and keeps the active filter inspectable and operable.
- [ ] Breadcrumbs render as clickable segments plus the current class leaf, using existing navigation/filter logic where possible.
- [ ] CSS changes reduce accent noise and reserve stable panel space instead of relying on late content growth.

## Verification

- `python -m pytest .gsd/test/s07_ux_polish.py -k "header or breadcrumbs or layout"`
- Manual browser review confirms the header/sidebar are visibly slimmer, breadcrumbs are clickable, and opening a class does not produce an obvious panel jump.

## Observability Impact

- Signals added/changed: breadcrumb DOM, active filter control value, and any layout/skeleton state classes needed for tests.
- How a future agent inspects this: browser DOM inspection plus targeted assertions in `.gsd/test/s07_ux_polish.py`.
- Failure state exposed: missing breadcrumb segments, wrong active filter value, or absent loading placeholders become directly assertable in the DOM.

## Inputs

- `index.html` — current header, sidebar, and content structure
- `app.css` — palette, spacing, and panel layout rules
- `js/app.js` — active filter/header behavior wiring
- `js/class-detail.js` — detail header rendering seam
- `.gsd/milestones/M001/slices/S07/tasks/T01-PLAN.md` — prefetch/loading state assumptions to preserve

## Expected Output

- `index.html` — decluttered header/sidebar markup
- `app.css` — tightened palette/typography and stable loading/layout styling
- `js/app.js` — filter control/header wiring updates
- `js/class-detail.js` — breadcrumb rendering and click behavior
