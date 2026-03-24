# Decisions Register

<!-- Append-only. Never edit or remove existing rows.
     To reverse a decision, add a new row that supersedes it.
     Read this file at the start of any planning or research phase. -->

| # | When | Scope | Decision | Choice | Rationale | Revisable? | Made By |
|---|------|-------|----------|--------|-----------|------------|---------|
| D001 | M001/S07/T02 | ui-navigation | How detail breadcrumbs navigate package segments | Package breadcrumb clicks reuse the existing global class search by filling the search box with the package prefix instead of introducing separate breadcrumb navigation state. | This keeps breadcrumb behavior inspectable through the existing search and class-list flow, preserves current filter semantics, and avoids a second package-navigation model that would drift from the main sidebar/search wiring. | Yes | agent |
