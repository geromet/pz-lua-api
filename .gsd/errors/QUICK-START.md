# Quick Start — Error Tracker

## What This Is

An auto-failure tracker for autonomous mode. When you fail an action 2+ times, document it here.

## How to Use

**After any failure:** Check `errors/*.md` for existing entry. If none exists, create one:

```markdown
## Count: 0/2
## Last Failure: TIMESTAMP
## Context: SHORT DESCRIPTION OF WHAT I WAS TRYING
## Notes: ROOT CAUSE + FIX
```

**When count hits 2:** Stop that approach, read all relevant error files, attempt recovery, reset counts to 0/2 on success.

## Common Failures

| Error | Fix |
|-------|-----|
| `python: command not found` | Run from PowerShell, or `cd pz-lua-api-viewer && python server.py` |
| `extract_lua_api.py: No module named 'lxml'` | `pip install lxml` in projectzomboid/ |
| `prepare_sources.py: [Errno 145] ...` | Run from projectzomboid/ root, NOT pz-lua-api-viewer/ |
| Browser hangs during nav | Refresh page, check console for JS errors |
| Extractor takes forever | Check if sources folder has changed since last run |
| Class not found in viewer | Regenerate lua_api.json, verify class exists in JSON |

## Visual Status

Run this to see current error state at a glance:

```bash
cd pz-lua-api-viewer/.gsd/errors && for f in *.md; do echo -n "$f: "; grep "^## Count:" $f | head -1; done
```

## Emergency Reset

If something is broken and you need to explore fresh, delete `errors/` and start clean.
