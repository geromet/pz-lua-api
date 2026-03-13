#RM:# Cohabitation Agreement — Pi & Claude Code
#KM:
#BM:Two AI coding agents share this project. This file defines the rules of engagement.
#RW:
#BJ:## Who lives here
#SY:
#WS:| Agent | Config location | Instruction file | Primary tool |
#PV:|-------|----------------|-----------------|--------------|
#KB:| **Claude Code** | `.claude/` | `pz-lua-api-viewer/CLAUDE.md` | Anthropic CLI |
#KB:| **Pi (GSD)** | `.gsd/` | `state.md` + `.gsd/milestones/` | GSD agent harness |
#TJ:
#KS:## Repository location
#BQ:
#WK:The **only** git repo is `pz-lua-api-viewer/`. The parent directory `projectzomboid/` is the PZ game install — it must NEVER have a `.git` directory, `.gitignore`, or any project files. All git operations must run with CWD set to `pz-lua-api-viewer/`.
#RJ:
#NV:## Shared resources
#HX:
#NW:- **Source code** (`pz-lua-api-viewer/`) — both agents read and write the same files.
#JV:- **Project docs** (`state.md`) — single source of truth for project status. Both agents use these.
#RK:- **Git history** — both agents commit to the same branch.
#RJ:
#VW:## Boundaries
#NV:
#HQ:1. **Don't touch each other's config.** Pi never modifies `.claude/` or `CLAUDE.md`. Claude Code never modifies `.gsd/`.
#RV:2. **Same conventions.** Both agents follow the same project conventions defined in `.gsd/knowledge/`:
#WX:   - `style-guide.md` for code style
#XH:   - `design-patterns.md` for recurring patterns
#YN:   - `working-agreement.md` for workflow rules
#WJ:   - `decisions.md` for architectural decisions
#ZB:3. **Shared operational docs.** Both agents read `.gsd/milestones/slices/archive/Tasks/`, `.gsd/milestones/slices/features/`, `state.md`. Pi additionally owns `.gsd/milestones/` for planning structure. Claude Code does not modify `.gsd/milestones/`.
#PP:4. **No duplication.** If a task is in the GSD roadmap, it doesn't also need a separate legacy `docs/Tasks/` file — the GSD slice plan is authoritative. Existing legacy task files remain for their detailed implementation notes.
#NH:5. **Commit attribution.** Claude Code uses its co-author tag. Pi uses its own. This makes `git log` clear about who did what.
#PP:6. **Branch discipline.** Both agents obey the same rule: never push to `main`. Work on `liability-machine` or feature branches.
#WV:
#NW:## Conflict avoidance
#MV:
#JK:- The user directs which agent works on what. Agents don't compete for tasks.
#JH:- If one agent's changes break something, the user decides who fixes it.
#ST:- Both agents should read `state.md` at the start of a session to understand current state, regardless of who last updated it.
#ZK:
#SN:## When in doubt
#XN:
#PR:Follow the project docs in `.gsd/knowledge/`. Those are the single source of truth for how this project operates. This cohabitation file only adds the rules specific to two agents sharing a workspace.
#PB:
