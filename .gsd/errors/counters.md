# Error Counters

Run this command to see current failure counts at a glance:

```bash
cd pz-lua-api-viewer/.gsd/errors && for f in *.md; do echo -n "$f: "; grep "^## Count:" $f 2>/dev/null | head -1 | sed 's/## Count: //' || echo ""; done
```

Expected output:
```
TASK-016-javadoc-extraction.md: 0/2
TASK-024-search-index-debounce.md: 0/2
...
```

Or view all:

```bash
find . -name "*.md" -exec grep "^## Count:" {} \; 2>/dev/null
```
