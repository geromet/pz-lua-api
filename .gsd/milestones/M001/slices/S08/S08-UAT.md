# S08: Navigation State — UAT

**Milestone:** M001
**Written:** 2026-03-24

## UAT Type

- UAT mode: mixed
- Why this mode is sufficient: S08 is a runtime navigation slice with both URL-state and localStorage behavior, so it needs direct viewer interaction plus observable DOM/test-report evidence.

## Preconditions

- Run the local viewer server from the project root so the app is available at `http://127.0.0.1:8000/`.
- Use a browser profile or private window where you can freely inspect the URL and localStorage.
- Start with browser devtools available so `#content[data-nav-*]` and `#recent-classes[data-recent-*]` can be inspected if behavior is ambiguous.
- If you want a clean recent-history test, clear `localStorage.pzRecentClasses` before starting.

## Smoke Test

Open `http://127.0.0.1:8000/?v=r964`, select any class, switch to the Source sub-tab, type `iso` into the main search box, change Scope to `setExposed only`, then refresh the page. The same class, search text, filter, and source sub-tab should be restored after reload.

## Test Cases

### 1. Shared URL restores full class browsing context

1. Open `http://127.0.0.1:8000/?v=r964`.
2. In the main header search box, type `iso`.
3. In the Scope dropdown, choose `setExposed only`.
4. Select class `zombie.characters.IsoDummyCameraCharacter` from the class list.
5. Click the `Source` content sub-tab and wait for source to load.
6. Copy the full browser URL.
7. Open a fresh browser tab or window and paste the copied URL.
8. **Expected:** The new page restores search text `iso`, Scope `setExposed only`, class `zombie.characters.IsoDummyCameraCharacter`, and the `Source` sub-tab with source content visible.
9. **Expected:** Inspecting `#content` shows `data-nav-restore-status="restored"`, `data-nav-url-state-source="query"`, and matching parsed/applied state in `data-nav-parsed-state` and `data-nav-applied-state`.

### 2. Default load keeps version selector and writes shareable state back into the URL

1. Open `http://127.0.0.1:8000/?v=r964`.
2. Confirm the page loads normally without a class selected.
3. Choose Scope `setExposed only`.
4. Select the first available class in the filtered list.
5. Switch to the `Source` sub-tab.
6. Type `iso` in the main search box.
7. **Expected:** The current URL now includes `v=r964` and query parameters for `class`, `search=iso`, `filter=exposed`, and `ctab=source`.
8. **Expected:** Inspecting `#content` shows `data-nav-restore-status="defaulted"` and `data-nav-serialized-state` matching the visible UI state.

### 3. Invalid class state falls back without silent mismatch

1. Open `http://127.0.0.1:8000/?v=r964&tab=classes&class=zombie.characters.DoesNotExist&search=iso&filter=exposed&ctab=source`.
2. Wait for the viewer to finish loading.
3. **Expected:** The page does not land on a broken class view; the placeholder remains visible instead.
4. **Expected:** The main search box still shows `iso` and Scope still shows `setExposed only`.
5. **Expected:** Inspecting `#content` shows `data-nav-restore-status="fallback"`, `data-nav-url-state-source="query"`, and `data-nav-restore-error="class not found:zombie.characters.DoesNotExist"`.
6. **Expected:** The live URL is sanitized so the missing class is no longer present, while valid state like `search=iso` and `filter=exposed` remains.

### 4. Recent classes are deduplicated, newest-first, and navigable

1. Clear `localStorage.pzRecentClasses`.
2. Open `http://127.0.0.1:8000/?v=r964`.
3. Select `zombie.characters.IsoDummyCameraCharacter`.
4. Select `zombie.characters.IsoZombie`.
5. Select `zombie.characters.IsoPlayer`.
6. Open the `Recent` control in the header.
7. **Expected:** The list shows three entries in newest-first order: `IsoPlayer`, `IsoZombie`, `IsoDummyCameraCharacter`.
8. Click `IsoDummyCameraCharacter` from the recent list.
9. **Expected:** The viewer navigates back to that class through the normal detail view.
10. Open `Recent` again.
11. **Expected:** `IsoDummyCameraCharacter` is now first in the list instead of duplicated.
12. **Expected:** Inspecting `#recent-classes` shows `data-recent-state="ready"`, `data-recent-count="3"`, and a meaningful `data-recent-last-action` value after open/close and selection.

### 5. Empty or pruned recent history remains inspectable

1. In devtools, set `localStorage.pzRecentClasses` to either `[]` or a list containing one real class and one fake class.
2. Reload `http://127.0.0.1:8000/?v=r964`.
3. Open the `Recent` control.
4. **Expected for empty storage:** The panel shows the empty-state message and `#recent-classes` exposes `data-recent-state="empty"`, `data-recent-count="0"`, and `data-recent-pruned="0"`.
5. **Expected for mixed valid/fake storage:** Only real classes are shown, stale entries are removed, and `data-recent-pruned` reports how many invalid entries were discarded.

## Edge Cases

- Opening an old hash-style link such as `/#globals` or `/#zombie.characters.IsoPlayer` should still restore that target, but subsequent live state changes should serialize into the query-backed contract.
- Switching versions with the version selector should preserve the viewer-state contract shape and keep `?v=` intact in the resulting URL.
- Recent history should never contain unresolved classes or duplicates, even if localStorage is manually corrupted before load.
- Invalid query values for `tab`, `filter`, or `ctab` should be rejected with inspectable diagnostics rather than silently producing mismatched UI state.

## Evidence to Capture

- Final URL from a restored class/source scenario.
- Screenshot of the restored page with the expected class and Source tab active.
- Screenshot of the Recent panel showing newest-first ordering.
- Devtools capture or note of `#content[data-nav-*]` and `#recent-classes[data-recent-*]` for one happy-path and one fallback-path case.
- `.gsd/test-reports/report.json` showing zero failures after running the automated suite.