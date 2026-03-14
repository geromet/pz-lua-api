# T01: Hover prefetch (TASK-026) — Summary

**Status:** ✅ Complete — source files now use cached content from hover prefetch

## What Was Done

### 1. Added hover prefetch handler to `app.js`

Added a new code block between the hover preview card initialization and the delegated click handlers for source class refs:

```javascript
// ── Hover prefetch (TASK-026) ─────────────────────────────────────
let prefetchTimer = null;

document.addEventListener('mouseover', e => {
  const el = e.target.closest('[data-fqn]');
  if (!el || !el.dataset.fqn) return;

  const fqn = el.dataset.fqn;
  clearTimeout(prefetchTimer);
  prefetchTimer = setTimeout(() => {
    if (API && API.classes[fqn] && API.classes[fqn].source_file) {
      fetchSource(API.classes[fqn].source_file).then(text => {
        sourceCache[API.classes[fqn].source_file] = text;
      }).catch(() => {
        // Silently fail — will try pre-shipped or local sources later
      });
    }
  }, 200);
});

document.addEventListener('mouseout', e => {
  clearTimeout(prefetchTimer);
  prefetchTimer = null;
});
```

### 2. Updated `showSource` in `source-viewer.js`

Added cache check before fetching:

```javascript
let text;
// Use cached source if available (from hover prefetch)
if (sourceCache[cls.source_file]) {
  text = sourceCache[cls.source_file];
} else {
  try {
    text = await fetchSource(cls.source_file);
  } catch (e) {
    loadingEl.style.display = 'none';
    codeEl.textContent = `// Source not available.\n// Run prepare_sources.py, or click "📁 Local sources"\n// and pick your projectzomboid folder.\n\n// Error: ${e.message}`;
    hljs.highlightElement(codeEl);
    return;
  }
}
```

### 3. Updated `showGlobalSource` in `globals.js`

Added cache check before fetching:

```javascript
let text;
// Use cached source if available (from hover prefetch)
if (sourceCache[relPath]) {
  text = sourceCache[relPath];
} else {
  try {
    text = await fetchSource(relPath);
  } catch (e) {
    codeEl.textContent = `// Source not available.\n// Error: ${e.message}`;
    hljs.highlightElement(codeEl);
    return;
  }
}
```

## Files Modified

- `js/app.js` — Added hover prefetch handler (~10 lines)
- `js/source-viewer.js` — Added cache check in `showSource` (~7 lines)
- `js/globals.js` — Added cache check in `showGlobalSource` (~10 lines)

Total: ~27 lines added across 3 files

## Verification

### Manual Steps

1. **Open the viewer** — Navigate to any class that has a source file (e.g., `zombie/Zombie`)
2. **Hover over a class link** — No visible effect, but background fetch starts after 200ms
3. **Click or re-open the same class** — Should load instantly since source is cached
4. **Clear browser cache / restart** — First click should still be fast, second click even faster

### What to Observe

| Action | Expected Behavior |
|--------|-------------------|
| Hover over `IsoPlayer` link | Background fetch starts after 200ms delay |
| Click `IsoPlayer` immediately | May show "Loading…" briefly (fetch not complete yet) |
| Hover again within 200ms | Fetch cancelled, no duplicate request |
| Click `IsoPlayer` again | Loads instantly — source loaded from `sourceCache` |
| Open different class | Previous prefetch is silently discarded |

### Debugging Tips

If a class takes longer than expected on first click:

1. Open DevTools → Network tab
2. Hover over a class link — you should see a new request appear ~200ms later
3. If no request appears, check browser console for errors
4. If fetch completes but class loads slowly, the issue is in `renderFoldableSource`, not the prefetch

## Notes

- The prefetch is completely invisible to users — it's an optimization for subsequent clicks
- Failed fetches are silently ignored — the code falls back to pre-shipped or local sources
- The hover preview card (TASK-026 in the original design) remains unchanged — it still displays metadata on hover, independent of prefetch
- No layout shift issues — the cache is used transparently by existing loading logic
