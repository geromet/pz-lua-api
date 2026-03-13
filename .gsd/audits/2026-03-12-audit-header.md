# Audit: For you to understand the current state of the codebase, and all the existing md files claude uses. I want you to follow all conventionts explained in the (non archived) docs.

**Date:** March 12, 2026
**Goal:** For you to understand the current state of the codebase, and all the existing md files claude uses. I want you to follow all conventionts explained in the (non archived) docs.
**Codebase:** PZ Lua API Viewer (E:\SteamLibrary\steamapps\common\ProjectZomboid\projectzomboid\pz-lua-api-viewer)

---

## Overview

This project is a **static web application** for browsing the Project Zomboid Lua API, extracted from decompiled Java sources. It is shared between two AI agents: **Claude Code** (via `.claude/` and `pz-lua-api-viewer/CLAUDE.md`) and **Pi / GSD** (via `.gsd/`). Both agents work on the same codebase and must follow shared conventions defined in `pz-lua-api-viewer/docs/Knowledge_Base/`.

The viewer is deployed to GitHub Pages from the `main` branch. All development happens on feature branches (currently `liability-machine`). **Never push directly to `main`** — it auto-deploys on push.
