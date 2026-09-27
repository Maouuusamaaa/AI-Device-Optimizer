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

For 0.1.15, the required real-device evidence is the existing P661N/API33 regression fixture; no new optimization benefit is claimed.

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

- `versionName = 0.1.15`
- `versionCode = 15`

Milestone 0.1.15 is Cloud Advisor + Local Policy Simulation.

The milestone adds an advisory-only Cloud Advisor contract, local Cloud Policy Candidate validation, deterministic local Policy Simulation through the existing dry-run pipeline, provenance linkage, and fail-closed safety/contract tests.

Cloud output cannot directly execute actions, bypass the Safety Gate, change the local allowlist, or enable device mutation. No new privileged capability or adaptive learning loop is introduced.

## Previous State

Milestone 0.1.14 was Evidence/Measurement Hardening:

- deterministic evidence contract validation for schema versions 1 and 2;
- comparable device/workload pairing with fail-closed insufficient-evidence handling;
- deterministic NO_REGRESSION / REGRESSION / MIXED classification;
- canonical SHA-256 provenance and append-only analysis history;
- P661N/API33 evidence validation;
- no optimization mutation or Cloud AI bypass.

## Example Workflow

```text
Milestone 0.1.14
  |
  +-- evidence/measurement hardening
  +-- CI validation
  +-- milestone accepted
          |
          v
      define 0.1.15
          |
          +-- Cloud Advisor contract
          +-- local candidate validation
          +-- Policy Simulation
          +-- Safety Gate boundary
          +-- CI validation
          +-- milestone accepted
```

The repository's source of truth remains Git history, while the version label communicates the validated milestone state.
