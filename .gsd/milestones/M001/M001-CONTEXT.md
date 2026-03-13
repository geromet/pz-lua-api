# M001: Feature Completion — Context

**Gathered:** 2026-03-13
**Status:** In progress

## Project Description

PZ Lua API Viewer is a static SPA for browsing the Project Zomboid Lua API, deployed to GitHub Pages. A Python extractor (`extract_lua_api.py`) generates `lua_api.json` from decompiled Java sources; the frontend (plain ES6 modules, no build step) renders class detail, source code, and globals.

The viewer already ships: tab bar, source linking, search highlights, sticky headers, globals split view, side-by-side panels with draggable splitters.

## Why This Milestone

Several planned features and tasks are ready to implement. The goal is to work through them in dependency order, keep docs current, and leave the project in a state where future agents can continue without context from this session.

## User-Visible Outcome

### When this milestone is complete, the user can:

- Browse the API with resizable panels (drag to adjust column widths)
- Open classes in new tabs via middle-click
- Switch between PZ API versions via a dropdown (if multiple versions are extracted)
- See build-time precomputed data loading faster
- Use improved search with scope toggle and recent-search dropdown
- Hover over class links to see a preview card without navigating

### Entry point / environment

- Entry point: `http://localhost:8765` (local) or `https://geromet.github.io/PZJavaDocs/` (production)
- Environment: browser (static SPA, no server needed for the viewer itself)
- Live dependencies: none (all data in `lua_api.json`)

## Completion Class

- Contract complete means: all tasks in all unblocked slices pass their acceptance criteria; screenshots taken
- Integration complete means: local server runs cleanly, navigation works across all new features
- Operational complete means: committed to `liability-machine`, pushed, PR-ready for `main`

## Final Integrated Acceptance

To call this milestone complete, we must prove:

- All unblocked slices shipped and verified via browser screenshots
- `pz-lua-api-viewer/docs/STATUS.md` reflects actual state
- No regressions in existing tab/nav/search/globals behaviour

## Key Constraints

- Never push directly to `main` — all work on `liability-machine`
- No build step — plain HTML/JS/CSS only
- After any extractor change, regenerate `lua_api.json` immediately
- Run `python server.py` from `pz-lua-api-viewer/` to test locally (port 8765)
- Co-author tag for commits: `Co-Authored-By: Pi (GSD) <noreply@gsd.dev>`
