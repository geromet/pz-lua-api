# Project Knowledge

Append-only register of project-specific rules, patterns, and lessons learned.
Agents read this before every unit. Add entries when you discover something worth remembering.

## Rules

| # | Scope | Rule | Why | Added |
|---|-------|------|-----|-------|

## Patterns

| # | Pattern | Where | Notes |
|---|---------|-------|-------|
| P001 | Publish runtime UI status through DOM data attributes and assert those seams in browser tests. | `#hover-preview`, `#detail-panel`, `#source-panel`, `#source-loading`, `#filter-select`, `#active-filter-chip`, `.gsd/test/s07_ux_polish.py` | This is now the authoritative way to verify pending/ready/error transitions for hover prefetch, detail loading, source loading, and compact filter state. |
| P002 | Treat URL/query state and localStorage-backed recent history as inspectable runtime contracts, not hidden implementation details. | `#content[data-nav-*]`, `#recent-classes[data-recent-*]`, `.gsd/test/s08_navigation_state.py` | S08 made navigation restore state and recent-history state part of the observable surface, which keeps browser verification stable even when UI wording or layout changes. |

## Lessons Learned

| # | What Happened | Root Cause | Fix | Scope |
|---|--------------|------------|-----|-------|
| L001 | The documented `.gsd/test/run.py` slice verification path could not be trusted until late in S07. | The runner and several older browser checks had drifted from current hash routes, selectors, and Playwright lifecycle expectations. | Repair the consolidated runner, give each legacy check a fresh page, and keep the stronger slice-specific pytest module alongside the aggregate report. | Browser verification and future slice close-out work |
| L002 | Legacy navigation browser checks can fail even when the shipped viewer works after URL-contract changes. | Older tests encoded class state as hash-only routes, while S08 moved the authoritative shareable state to query parameters. | Keep aggregate navigation assertions tolerant of both query-backed and legacy hash URLs until the old checks are fully rewritten or retired. | `.gsd/test/run.py` closure work and future navigation slices |
| L003 | Split-index mode silently fell back to monolithic load — `init()` crashed accessing `.methods` on index-only entries, and the error was swallowed by the try/catch around the split path. | `hasTaggedMethods`, `hasCallableMethods`, and `scoreClass` in `class-list.js` assumed all class entries have `.methods` and `.fields` arrays. In split-index mode, index entries only have summary fields. | Guard all `.methods` and `.fields` accesses with `\|\| []` fallback. Any new code touching class entries must handle the split-index shape where methods/fields/constructors are undefined until detail is lazy-fetched. | `js/class-list.js`, `js/class-detail.js`, any future code accessing `API.classes[fqn]` |
