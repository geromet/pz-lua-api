# Quick Reference — Pi (GSD)

Cheat sheet for common operations. Read this at the start of a session.

## First thing every session

```
Read: .gsd/STATE.md          ← active milestone/slice/task
Read: .gsd/milestones/M001/slices/<active>/S0N-PLAN.md  ← what to do next
Read: pz-lua-api-viewer/docs/STATUS.md    ← shared project status
```

## Common commands

| Action | Command |
|--------|---------|
| Start local server | `cd pz-lua-api-viewer && python server.py` |
| Regenerate API JSON | `cd projectzomboid && python pz-lua-api-viewer/extract_lua_api.py` |
| Prepare sources | `cd pz-lua-api-viewer && python prepare_sources.py` |
| Archive a task | `cd pz-lua-api-viewer && python docs/archive.py docs/Tasks/TASK-NNN-slug.md` |
| Archive a bug | `cd pz-lua-api-viewer && python docs/archive.py docs/Bugs/BUG-NNN-slug.md` |
| Commit | `git add -A && git commit -m "description"` |
| Push | `git push` |

## File locations

| What you need | Where it is |
|---------------|-------------|
| GSD quick status | `.gsd/STATE.md` |
| GSD milestone roadmap | `.gsd/milestones/M001/M001-ROADMAP.md` |
| GSD slice plans | `.gsd/milestones/M001/slices/S0N/S0N-PLAN.md` |
| GSD decisions | `.gsd/DECISIONS.md` |
| Project overview | `.gsd/PROJECT.md` |
| Shared project status | `pz-lua-api-viewer/docs/STATUS.md` |
| Legacy task files | `pz-lua-api-viewer/docs/Tasks/` |
| Bug files | `pz-lua-api-viewer/docs/Bugs/` |
| Feature plans | `pz-lua-api-viewer/docs/Planned_Features/` |
| Knowledge base | `pz-lua-api-viewer/docs/Knowledge_Base/` |
| Style guide | `pz-lua-api-viewer/docs/Knowledge_Base/Style-Guide.md` |
| Design patterns | `pz-lua-api-viewer/docs/Knowledge_Base/Design-Patterns.md` |
| Decisions (ADRs) | `pz-lua-api-viewer/docs/Knowledge_Base/Decisions.md` |
| Domain glossary | `pz-lua-api-viewer/docs/Knowledge_Base/Domain-Language.md` |
| Claude Code config | `.claude/` (DO NOT MODIFY) |
| Pi config | `.gsd/` |

## Extractor output baselines

After running `extract_lua_api.py`, these are the expected approximate numbers:

```
Total classes:     ~1096
Total methods:     ~19099
setExposed:         ~917
@UsedFromLua only:  ~179
Global functions:    745
Unresolved:           57
Parse errors:          5
```

Any significant change (±10%) warrants investigation.

## Key architectural rules

1. No build step — plain HTML/JS/CSS, deployed as static files.
2. Extractor is the source of truth — frontend trusts the JSON.
3. State lives in `state.js` — don't scatter state across modules.
4. Rendering functions return HTML strings — they don't touch the DOM.
5. Event delegation — don't attach listeners inside rendering functions.
6. `_restoringState` flag handles nav suppression — nav-aware functions push unconditionally.
