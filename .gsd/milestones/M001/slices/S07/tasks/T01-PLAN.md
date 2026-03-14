# T01: Hover prefetch (TASK-026)

**Goal:** On `mouseenter` of a class link (`a[data-fqn]`), start fetching its source file into `sourceCache`. Use a 200ms delay to avoid prefetching on quick mouse movement. Cancel on `mouseleave`.

## Implementation

### Step 1: Add hover timer and cancel handler to app.js

Add a new function that installs hover handlers on all `[data-fqn]` links:

```javascript
// ── Hover prefetch (TASK-026) ──────────────────────────────────────────────
(function () {
  let prefetchTimer = null;

  // Install hover handlers on all class links
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
          // Silently fail — will try pre-shipped or local sources
        });
      }
    }, 200);
  });

  // Cancel prefetch on mouse leave
  document.addEventListener('mouseout', e => {
    clearTimeout(prefetchTimer);
    prefetchTimer = null;
  });
})();
```

### Step 2: Update `showSource` in source-viewer.js to use cache

Modify the fetch line to check cache first:

```javascript
// Before:
text = await fetchSource(cls.source_file);

// After:
if (sourceCache[cls.source_file]) {
  text = sourceCache[cls.source_file];
} else {
  text = await fetchSource(cls.source_file);
}
```

## Files Modified

- `js/app.js`: Add hover prefetch handler
- `js/source-viewer.js`: Use cached sources

## Verification

1. Open browser, open a class with a source file
2. Hover over a class link — no visible effect (background fetch)
3. Click the link — should load instantly if previously hovered
4. Clear cache and test again — first click after hover should still be fast
