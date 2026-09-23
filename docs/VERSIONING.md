# Versioning and Release Milestones

## Purpose

AI Device Optimizer separates ongoing engineering work from release milestones.

A code change, experiment, benchmark, diagnostic, bug fix, or CI repair does not automatically constitute a new application release.

## Rules

- `versionName` identifies a validated project milestone intended to be treated as a release.
- `versionCode` follows Android's monotonic update requirement. It must increase when a new installable APK is distributed as an Android update.
- Git commits identify the exact implementation state.
- Pull requests identify a bounded engineering change.
- Experiment IDs identify controlled investigations and their evidence.
- GitHub Actions identifies automated validation for a specific commit.
- Benchmark/evidence artifacts identify real-device observations.

## Development Within a Milestone

During an active milestone, implementation may change through multiple commits and pull requests while the milestone's `versionName` remains unchanged.

Examples include:

- diagnostic instrumentation
- benchmark fixes
- test fixes
- documentation updates
- internal refactoring
- controlled experiment support
- CI fixes
- evidence-export fixes

These changes must be traceable through Git history and experiment/PR records.

## When to Change versionName

Change `versionName` only when the current milestone has been explicitly closed and the next milestone is defined.

A milestone should normally have:

1. implementation complete for its stated scope;
2. automated CI validation passing;
3. required unit/contract tests passing;
4. required real-device experiments completed;
5. evidence analyzed and documented;
6. known blockers recorded;
7. no unresolved issue that would make the milestone's stated result misleading.

The exact acceptance criteria may differ by milestone and must be documented in the relevant experiment or release record.

## Android versionCode

Android `versionCode` is different from the project's milestone label. Android uses it to determine whether one APK is a newer application update than another.

Therefore:

- do not use `versionCode` as an experiment counter;
- do not reuse a lower `versionCode` for a separately distributed APK;
- keep it monotonic for installable update builds;
- record the relationship between `versionCode`, `versionName`, commit SHA, and experiment/PR in CI or release evidence when a build is distributed.

An internal CI build may therefore have the same `versionName` as the active milestone while still using the next valid `versionCode` when Android requires a newer installable update.

## Current State

The repository currently declares:

- `versionName = 0.1.13`
- `versionCode = 13`

These values are not changed merely because additional investigation occurs. The next version milestone is created only after the current milestone's acceptance criteria are satisfied.

## Example Workflow

```text
Milestone 0.1.13
  |
  +-- commit: diagnostic implementation
  +-- commit: benchmark fix
  +-- PR: lifecycle diagnostic
  +-- CI validation
  +-- physical-device experiment
  +-- evidence analysis
  |
  +-- milestone accepted
          |
          v
      define 0.1.14
```

The repository's source of truth remains Git history, while the version label communicates the validated milestone state.
