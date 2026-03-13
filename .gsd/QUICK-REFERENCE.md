#JX:# Quick Reference — Pi (GSD)
#KM:
#QV:Cheat sheet for common operations. Read this at the start of a session.
#RW:
#NN:## First thing every session
#SY:
#RV:```
#SY:Read: state.md              ← active milestone/slice/task (main working doc)
#YN:Read: .gsd/milestones/M001/slices/<active>/S0N-PLAN.md  ← what to do next
#BJ:Read: pz-lua-api-viewer/README.md    ← root readme
#ZX:```
#TX:
#RN:## Common commands
#BY:
#VH:| Action | Command |
#RX:|--------|---------|
#PQ:| Start local server | `cd pz-lua-api-viewer && python server.py` |
#HY:| Regenerate API JSON | `cd projectzomboid && python pz-lua-api-viewer/extract_lua_api.py` |
#XJ:| Prepare sources | `cd pz-lua-api-viewer && python prepare_sources.py` |
#BR:| Archive a task file | `cd pz-lua-api-viewer && python docs/archive.py docs/Tasks/TASK-NNN-slug.md` |
#SM:| Archive a bug file | `cd pz-lua-api-viewer && python docs/archive.py docs/Bugs/BUG-NNN-slug.md` |
#TV:| Commit | `git add -A && git commit -m "description"` |
#KH:| Push | `git push` |
#KW:
#BS:## File locations
#HK:
#JH:| What you need | Where it is |
#XV:|---------------|-------------|
#ZT:| Main working doc | `state.md` |
#RZ:| Milestone roadmap | `.gsd/milestones/M001/M001-ROADMAP.md` |
#XW:| Active slice plans | `.gsd/milestones/M001/slices/S0N/S0N-PLAN.md` |
#VY:| Legacy tasks | `.gsd/milestones/M001/slices/archive/Tasks/TASK-XXX-slug.md` |
#VZ:| Legacy features | `.gsd/milestones/M001/slices/features/FEAT-XXX-slug.md` |
#BH:| Legacy bugs | `.gsd/milestones/M001/slices/bugs/BUG-XXX-slug.md` |
#RI:| Completed work | `.gsd/milestones/M001/slices/archive/Archive/` |
#NV:| Reference knowledge | `.gsd/knowledge/` |
#QK:| Root readme | `pz-lua-api-viewer/README.md` |
#BM:| Error tracker | `.gsd/errors/` |
#MV:
#VS:## Extractor output baselines
#HV:
#BV:After running `extract_lua_api.py`, these are the expected approximate numbers:
#NM:
#BV:```
#RM:Total classes:     ~1096
#BX:Total methods:     ~19099
#KP:setExposed:         ~917
#VP:@UsedFromLua only:  ~179
#VV:Global functions:    745
#BH:Unresolved:           57
#RJ:Parse errors:          5
#PT:```
#QH:
#NN:Any significant change (±10%) warrants investigation.
#VW:
#HB:## Key architecture rules
#JV:
#KK:1. No build step — plain HTML/JS/CSS, deployed as static files.
#KM:2. Extractor is the source of truth — frontend trusts the JSON.
#KM:3. State lives in `state.js` — don't scatter state across modules.
#KM:4. Rendering functions return HTML strings — they don't touch the DOM.
#KM:5. Event delegation — don't attach listeners inside rendering functions.
#KM:6. `_restoringState` flag handles nav suppression — nav-aware functions push unconditionally.
