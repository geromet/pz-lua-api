# Working Agreement — Pi (GSD)

How Pi and the developer collaborate on this project. Mirrors and extends `pz-lua-api-viewer/docs/Knowledge_Base/Working-Agreement.md` — that file is the shared source of truth; this file covers Pi-specific additions.

## Branch discipline
- **Never push to `main`.** It deploys to GitHub Pages immediately.
- Feature work goes on a named branch (currently `liability-machine`).
- Ask before creating a new branch or merging.

## Commits
- Commit after each logical unit of work, not after every file edit.
- Commit messages: imperative mood, short subject, body explains *why* not *what*.
- Co-author tag: `Co-Authored-By: Pi (GSD) <noreply@gsd.dev>`

## Extractor runs
- After any change to `extract_lua_api.py`, run it to regenerate `lua_api.json`.
- Always check the summary output (class counts, extends counts) against the previous run to catch regressions.

## Testing
- Test locally with `server.py` before committing frontend changes.
- For extractor changes, run a quick Python sanity check verifying the changed data (see `pz-lua-api-viewer/docs/Knowledge_Base/Testing.md`).
- Use browser tools to visually verify frontend changes at `http://localhost:8765`.

## Scope discipline
- Do not refactor, clean up, or add features that weren't requested.
- If a requested change reveals a nearby problem, flag it — don't silently fix it.
- Ask before making changes to `index.html` structure or CSS variables (shared tokens).

## Documents
- Task files in `docs/Tasks/` should be complete enough to execute in one session without questions.
- Archive documents when done; don't leave completed task files in active folders.
- Use `docs/Temp/` for throwaway scripts and data dumps; it's gitignored.

## Pi-specific workflow notes
- Use `bg_shell` to start `server.py` as a background process for local testing.
- Use browser tools to verify frontend changes visually — navigate to `http://localhost:8765`.
- Use `subagent` for isolated research or parallel implementation when tasks are independent.
- Read the full task file and all referenced source files before making any code changes.

## Known constraints
- `javalang` can't parse Java 14+ switch expressions — `strip_method_bodies()` fallback handles most cases. 5 files still fail parsing entirely.
- `is_relative_to()` requires Python 3.9+.
- GitHub Pages serves from `main` branch root of `pz-lua-api-viewer/`.
