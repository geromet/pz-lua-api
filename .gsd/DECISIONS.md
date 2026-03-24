# Decisions Register

<!-- Append-only. Never edit or remove existing rows.
     To reverse a decision, add a new row that supersedes it.
     Read this file at the start of any planning or research phase. -->

| # | When | Scope | Decision | Choice | Rationale | Revisable? | Made By |
|---|------|-------|----------|--------|-----------|------------|---------|
| D001 | M001/S07/T02 | ui-navigation | How detail breadcrumbs navigate package segments | Package breadcrumb clicks reuse the existing global class search by filling the search box with the package prefix instead of introducing separate breadcrumb navigation state. | This keeps breadcrumb behavior inspectable through the existing search and class-list flow, preserves current filter semantics, and avoids a second package-navigation model that would drift from the main sidebar/search wiring. | Yes | agent |
| D002 | M001/S07 | source-loading | How hover warming should fetch source code | Route hover prefetch through the existing shared source fetch/cache path with shared pending/status tracking instead of building a separate prefetch implementation. | This keeps hover warming, direct source opens, cache reuse, and error diagnostics on one code path so tests and runtime inspection see the same state transitions. | Yes | agent |
| D003 | M001/S08 | ui-navigation | How S08 should encode shareable viewer state | Use query parameters for shareable viewer state (`filter`, `search`, `tab`, `ctab`) while keeping the selected class or globals sentinel in the hash and preserving the existing `?v=` version parameter. | The current app already uses the hash for selected-class/globals navigation and version selection already occupies query params. Extending the query string for filter/search/tab state preserves backward compatibility with existing links, keeps globals/class targeting simple, and avoids inventing a second class-identity channel that would fight current hash/history behavior. | Yes | agent |
