## Key Files

### Core Application Files

| File | Purpose |
|------|---------|
| `pz-lua-api-viewer/extract_lua_api.py` | Python script to regenerate `lua_api.json` from Java sources |
| `pz-lua-api-viewer/lua_api.json` | ~6MB extracted API data (written by extractor) |
| `pz-lua-api-viewer/index.html` | Single-file HTML shell (~720 lines) |
| `pz-lua-api-viewer/app.css` | All styles (uses CSS variables) |
| `pz-lua-api-viewer/js/state.js` | Global state variables (no modules) |
| `pz-lua-api-viewer/js/app.js` | Init, navigation, event setup |
| `pz-lua-api-viewer/js/class-detail.js` | Detail panel renderer |
| `pz-lua-api-viewer/js/source-viewer.js` | Source panel renderer + class-ref linking |
| `pz-lua-api-viewer/js/globals.js` | Globals tab |
| `pz-lua-api-viewer/js/class-list.js` | Sidebar class list |
| `pz-lua-api-viewer/prepare_sources.py` | Copies .java sources to `sources/` for GitHub Pages |
| `pz-lua-api-viewer/sources/` | 971+ pre-shipped .java files (15 MB) |
| `pz-lua-api-viewer/serve.py` | Simple HTTP server for local testing |

### Architecture Notes

- **No build step** — plain ES6 modules loaded directly via `<script src>` tags
- **Navigation** uses a manual history stack (`navHistory[]` in `state.js`) with `_restoringState` flag to suppress history writes during state restoration
- **State lives in `state.js`** — all global state declared there with `let` or `const`
- **Rendering functions return HTML strings** — they don't touch the DOM directly
- **Event delegation** — all click handling uses `closest()` on stable ancestors (e.g., `#detail-panel`, `#class-list`)
- **The extractor is the source of truth** — frontend trusts the JSON, doesn't re-parse Java

### Key JSON Fields

| Field | Description |
|-------|-------------|
| `classes` | Map of FQN → class data |
| `_extends_map` | Non-API intermediate → parent FQN (fills inheritance gaps) |
| `_interface_extends` | Interface FQN → [parent interface FQNs] (BFS computed) |
| `_source_index` | Simple name → relative `.java` path for source-only classes |
