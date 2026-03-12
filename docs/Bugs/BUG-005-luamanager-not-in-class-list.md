# BUG-005: LuaManager not visible in the class list

**Status:** Open
**Severity:** Low
**Touches:** `extract_lua_api.py`

## Description

`zombie.Lua.LuaManager` does not appear in the class list, even though it is the source of all global Lua functions and is a heavily referenced class. Users looking for global function source code have no direct class entry to navigate to.

## Steps to Reproduce

1. Search for "LuaManager" in the class search box.
2. No result appears.

## Root Cause

`LuaManager` is not `setExposed` and is not tagged `@UsedFromLua` directly, so it never enters `all_classes` in the extractor. It only appears as a source path (`zombie/Lua/LuaManager.java`) referenced by the globals panel.

## Fix Options

**Option A (preferred):** Add `LuaManager` to `all_classes` in the extractor as a special synthetic entry (similar to how `_source_index` handles source-only classes). Mark it as a special system class so it can be filtered if desired.

**Option B:** Add a visible link to LuaManager's source from the Globals tab header, without adding it to the class list.

**Option C:** Include any class in `_source_index` in the class list under a new "Source only" filter category.

## Notes

Option A risks cluttering the class list with internal classes. Option B is surgical but doesn't help if the user doesn't notice it. Option C is the most general solution but requires UI filter work.
