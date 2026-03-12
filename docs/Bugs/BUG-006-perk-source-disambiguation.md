# BUG-006: Perk class opens wrong source file (disambiguation failure)

**Status:** Open
**Severity:** Medium
**Touches:** `js/source-viewer.js`, `js/class-detail.js`

## Description

When viewing the `Perk` class (FQN: `zombie.characters.skills.Perk`), clicking the Source tab or a method link opens the wrong file. The class list may also show two entries for `Perk` from different packages if disambiguation is not applied.

The issue is related to `classBySimpleName` having multiple FQNs for the simple name `Perk` (e.g. `zombie.characters.skills.Perk` vs `PerkFactory.Perk` as a nested class).

## Steps to Reproduce

1. Select `Perk` (zombie.characters.skills) in the class list.
2. Click the Source tab.
3. Observe whether the correct file opens, or whether an unrelated `Perk` appears.

## Root Cause

`classBySimpleName['Perk']` may contain multiple FQNs. When `linkClassRefs()` encounters `Perk` as a token, it picks the first match without considering which one is relevant to the current file's package context. Additionally `PerkFactory.Perk` is a nested class and its simple name `Perk` collides with the top-level `Perk` class.

## Fix Sketch

In `linkClassRefs()`, when `classBySimpleName[token]` has multiple entries:
1. Prefer the entry whose package matches the current file's package.
2. Prefer non-nested (no `$` in FQN) over nested class names.
3. Fall back to first entry if no better match.

Also, in `showSource()`, ensure the `source_file` field on the class entry is used directly (absolute source path) rather than re-deriving it from simple name lookup.
