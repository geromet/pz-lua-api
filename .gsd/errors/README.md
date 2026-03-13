# Error Tracker — Auto Mode

This folder tracks failures during autonomous operation. Each file corresponds to a specific failure type.

## How It Works

When an action fails repeatedly (2+ times), create/update a `.md` file in this folder:
```markdown
## Count: 0/2
## Last Failure: TIMESTAMP
## Context: WHAT I WAS TRYING TO DO
## Notes: WHY IT FAILED + WHAT WORKED INSTEAD
```

## When to Use

| Count | Action |
|-------|--------|
| 0/2 | Keep trying with same approach |
| 1/2 | Note the failure, try alternative |
| 2/2 | Stop trying, use documented workaround or ask user |

## Example

```markdown
## Count: 1/2
## Last Failure: 2026-03-13T23:15:00Z
## Context: Tried `python extract_lua_api.py` from projectzomboid/ but got "No module named 'lxml'"
## Notes: pip install lxml fixed it. Add to requirements.txt if needed.
```

## Recovery Pattern

When hitting count 2, read all relevant error files, combine solutions, attempt recovery, update counts to 0/2 on success.
