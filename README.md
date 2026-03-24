# PZ Lua API Viewer

Static web app for browsing the Project Zomboid Lua API extracted from decompiled Java sources.

**Live:** https://geromet.github.io/PZJavaDocs/

## Repository layout

```text
index.html, app.css, js/   Main application
server/                    Local development server files
scripts/                   Maintenance and data-generation scripts
docs/                      Canonical project documentation
tests/                     Automated tests and archived debug output
.gsd/                      GSD milestone and execution state
```

## Documentation

Project-facing documentation lives in `docs/`.

Start with:
- `docs/README.md`
- `docs/STATUS.md`
- `docs/Philosophy.md`
- `docs/Decisions.md`

## Quick start

```bash
# Serve locally
python server/serve.py

# Regenerate lua_api.json from the local source tree
python scripts/extract_lua_api.py

# Run the automated browser suite
python .gsd/test-suite.py
```

## Features

- Browse exposed Java classes callable from Lua
- Search classes, methods, and fields
- Filter by `@UsedFromLua`, `setExposed`, callable status, and enums
- Browse global Lua functions
- URL-based navigation and version selection

## Project Zomboid Terms & Conditions
https://projectzomboid.com/blog/support/terms-conditions/
