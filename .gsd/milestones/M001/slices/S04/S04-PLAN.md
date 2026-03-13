# S04: Javadoc Extraction (Blocked)

**Goal:** Extract and display Javadoc comments from PZ Java sources in the Detail panel.
**Demo:** Methods/fields with Javadoc show a `▶` toggle that expands to show description, @param, @return.

## Blocked

**Reason:** Decompiled sources (CFR output) contain zero `/**` Javadoc blocks — comments are stripped during decompilation. This task is not actionable until original PZ source files are available.

**Evidence:** Investigation on 2026-03-12 confirmed 0/2938 `.java` files contain any `/**` blocks.

**How to unblock:** Obtain original PZ Java sources with Javadoc intact. The extractor plan (see TASK-016) is already complete and ready to implement once sources are available.

## Must-Haves (when unblocked)

- Methods/fields with Javadoc show `▶` toggle in Detail panel (FEAT-010 / TASK-016)
- Expanding shows description + @param + @return formatted text
- `lua_api.json` grows `"doc"` field on method/field entries
- Fields with Javadoc also show toggle
- No visual noise for entries without Javadoc

## Verification (when unblocked)

- `python -c "import json; d=json.load(open('pz-lua-api-viewer/lua_api.json')); methods_with_doc = [m for c in d['classes'].values() for m in c.get('methods',[]) if m.get('doc')]; print(len(methods_with_doc), 'methods with doc')"` — should be > 0
- Browser screenshot showing expanded Javadoc toggle

## Tasks (when unblocked)

- [ ] **T01: Javadoc extraction in extractor** `est:2h`
  - Why: Implements TASK-016 Step 1 — extract `/** */` blocks from source before declarations
  - Files: `pz-lua-api-viewer/extract_lua_api.py`
  - Do: Follow `pz-lua-api-viewer/docs/Tasks/TASK-016-javadoc-extraction.md` Step 1. Implement `extract_javadoc_before_line()`. Call it for each method/field AST node. Add `"doc"` field to JSON output. Run extractor.
  - Verify: `python -c "..."` shows > 0 methods with doc
  - Done when: Extractor runs clean; JSON contains doc fields

- [ ] **T02: Detail panel Javadoc display** `est:1.5h`
  - Why: Implements TASK-016 Step 2 — render toggleable doc sections in the Detail panel
  - Files: `pz-lua-api-viewer/js/class-detail.js`, `pz-lua-api-viewer/app.css`
  - Do: Follow TASK-016 Step 2. Add `▶` toggle button to method/field rows that have a `doc` field. On click, expand to show formatted Javadoc. Style consistently with existing UI.
  - Verify: Browser screenshot of expanded Javadoc toggle on a method
  - Done when: Toggle works; no visual noise for undocumented entries

- [ ] **T03: Update docs and commit S04** `est:10m`
  - Why: Archive TASK-016; update STATUS.md
  - Files: `pz-lua-api-viewer/docs/STATUS.md`, `pz-lua-api-viewer/docs/Tasks/TASK-016-javadoc-extraction.md`
  - Do: Prepend completion blockquote; archive; update STATUS.md; commit and push
  - Done when: pushed to `liability-machine`

## Files Likely Touched

- `pz-lua-api-viewer/extract_lua_api.py`
- `pz-lua-api-viewer/js/class-detail.js`
- `pz-lua-api-viewer/app.css`
- `pz-lua-api-viewer/lua_api.json` (regenerated)
