# CI/CD Improvements: Preventing broken publishes

Re: #291 — and the pattern of bugs that should be caught before they reach users.

## What happened

The v2.10.6 npm package shipped without `dist/` — the directory containing the actual CLI entrypoint. Every user who installed it got a broken `gsd` command.

The root cause is in `build-native.yml`. The publish step explicitly skips the build:

```yaml
npm publish --ignore-scripts
```

The comment says *"build already done upstream"* — but there is no upstream build step in the workflow. The TypeScript is never compiled. `dist/loader.js` never gets created. npm publishes the tarball without it.

## The deeper issue

The repo currently has **one workflow** (`build-native.yml`), and it only handles Rust native binaries + npm publish. There is no CI pipeline for the actual application — no build verification, no tests, no smoke test on any branch or PR.

The repo *has* tests (`npm test`, `test:browser-tools`, `test:native`) and a working build script. They just never run in CI.

This is the fourth bug that would have been caught by running `node dist/loader.js --version` in automation. That pattern is worth fixing structurally.

## Proposed changes

### 1. Add a CI workflow that runs on every push and PR

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        node-version: [20, 22]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
      - run: npm ci
      - run: npm run build
      - run: npm test

      # Smoke test: does the CLI actually launch?
      - name: Smoke test
        run: node dist/loader.js --version
```

This alone would have caught this bug and likely the previous three. If `dist/loader.js` doesn't exist, the build fails. If it exists but crashes on import, the smoke test fails.

### 2. Build before publish in the release workflow

The "Publish main package" step in `build-native.yml` needs to actually build first instead of assuming it was done elsewhere:

```yaml
      - name: Install dependencies
        run: npm ci

      - name: Build
        run: npm run build

      - name: Verify dist exists
        run: test -f dist/loader.js || { echo "::error::dist/loader.js missing after build"; exit 1; }

      - name: Publish main package
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
        run: npm publish
```

### 3. Verify tarball contents before publish

Belt-and-suspenders check — make sure the files that need to be in the package are actually there:

```yaml
      - name: Verify tarball contents
        run: |
          npm pack --dry-run 2>&1 | tee /tmp/pack-output.txt
          grep -q "dist/loader.js" /tmp/pack-output.txt || {
            echo "::error::dist/loader.js not in tarball"
            exit 1
          }
```

### 4. Post-publish smoke test

After publishing, install the package from npm in an isolated directory and verify it actually works:

```yaml
      - name: Post-publish smoke test
        run: |
          VERSION=$(node -p "require('./package.json').version")
          sleep 15
          TMPDIR=$(mktemp -d)
          cd "$TMPDIR"
          npm init -y
          npm install "gsd-pi@${VERSION}"
          npx gsd --version
          echo "Published package is functional"
```

This catches everything: missing files, broken imports, bad shebang lines, missing dependencies. If a user can't run `gsd`, this step fails before anyone finds out the hard way.

### 5. Branch protection

Require the CI workflow to pass before merging to `main`. This keeps broken code from reaching the tag-push trigger that starts a release.

## Summary

| Gap | What breaks | Fix |
|-----|-------------|-----|
| No CI workflow | Broken builds merge freely | Add `ci.yml` with build + test + smoke test |
| Publish skips build via `--ignore-scripts` | `dist/` never created | Explicit build step before publish |
| No tarball verification | Missing files ship silently | `npm pack --dry-run` assertion |
| No post-publish check | Broken packages reach users | Smoke test the installed package |
| No branch protection | Untested code reaches release tags | Require CI pass on `main` |

All four recent bugs would have been caught by a single line in CI: `node dist/loader.js --version`. The rest is defense in depth.
