# Git Hooks — dev-oocol-fil-rouge

Automatic quality checks during development.

## Install

```sh
git config core.hooksPath .githooks
```

(The devcontainer auto-configures this.)

## What runs when

| Hook | Trigger | Scope | Time |
|------|---------|-------|------|
| `pre-commit` | `git commit` | Compile changed modules only | ~5s |
| `pre-push` | `git push` | Test changed modules + dependents | ~30s |
| `commit-msg` | `git commit` | Validate Conventional Commits format | <1s |

## Philosophy

- **Local hooks are fast** — they catch compile errors and test regressions early.
- **CI is authoritative** — full static analysis (PMD, SpotBugs, Checkstyle), JDK 21+25 matrix, SonarQube, and CodeQL run in Actions.
- **Incremental** — hooks detect changed modules via `git diff` and scope the build.

## Emergency bypass

```sh
git push --no-verify   # skip pre-push (use sparingly)
```

## Full local verification

When you want to replicate CI locally:
```sh
./mvnw -B clean verify    # all modules, all tests, static analysis
```
