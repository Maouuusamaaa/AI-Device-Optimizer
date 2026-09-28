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

For 0.1.16, the required validation is deterministic offline replay plus the existing Android/P661N regression fixture. No new physical-device optimization benefit or causal action effect is claimed.

For 0.1.17, the required validation is deterministic adaptive-learning contract coverage, persistent Knowledge State recovery, lineage-safe splitting, and resource-aware processing. The learner remains advisory-only.

For 0.1.18, the required validation is Android-native runtime integration, bounded background processing, and diagnostic validation for missing, valid, and corrupt Knowledge State. The runtime remains advisory-only.

For 0.1.19, the required validation is read-only SmartPanel Game Mode provider access, evidence-contract coverage, release/native CI validation, and controlled ON → OFF → ON UI correlation on the itel P661N/API33 test device. The result is observational and does not authorize Game Mode mutation.

For 0.1.20, the required validation is deterministic descriptive outcome mapping, fail-closed context/confounder handling, train-derived candidate evaluation against a disjoint matching holdout, minimum holdout enforcement, and green AI Cloud, Measurement, Android, and Local Llama CI. No new physical-device optimization benefit or causal-effect claim is introduced.

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

- `versionName = 0.1.21`
- `versionCode = 21`

Milestone 0.1.20 is Adaptive Learning Evaluation Hardening.

The milestone adds read-only SmartPanel Game Mode evidence on supported Transsion/itel builds, separates provider availability from configured and checked package sets, validates conservative checked-state parsing, and records the evidence in the existing snapshot contract.

A controlled ON → OFF → ON UI validation was completed for Minecraft on the itel P661N / Android 13 API 33 test device. The observed provider state tracked the SmartPanel Game Management toggle across all three states. This is an observational device/build result, not a universal SmartPanel semantic guarantee.

The milestone does not add Game Mode mutation, privileged write permissions, Action Engine integration, Safety Gate bypass, or SmartPanel APK modification.

Milestone 0.1.18 preceded 0.1.19 and integrated the Android-native adaptive-learning runtime with bounded processing and Knowledge State diagnostics. It remained advisory-only.

Milestone 0.1.17 was Adaptive Learning Foundation:

- deterministic local learning records;
- persistent idempotent Knowledge State;
- minimum-evidence abstention;
- lineage-preserving deterministic splits;
- atomic state persistence;
- resource-aware always-on processing;
- no automatic policy selection, action ranking, device mutation, or causal effectiveness claims.

Milestone 0.1.15 remains Cloud Advisor + Local Policy Simulation and established the advisory-only cloud boundary.

The milestone adds an advisory-only Cloud Advisor contract, local Cloud Policy Candidate validation, deterministic local Policy Simulation through the existing dry-run pipeline, provenance linkage, and fail-closed safety/contract tests.

Cloud output cannot directly execute actions, bypass the Safety Gate, change the local allowlist, or enable device mutation. No new privileged capability or adaptive learning loop is introduced.

## Previous State

Milestone 0.1.16 was Offline Evaluation + Persistent Outcome History:

- deterministic offline evidence replay;
- evaluation fingerprints and provenance;
- persistent evaluation outcome history;
- CI fixture validation;
- no policy selection, action ranking, causal action-effect claim, or device mutation.

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
      0.1.15
          |
          +-- Cloud Advisor contract
          +-- local candidate validation
          +-- Policy Simulation
          +-- Safety Gate boundary
          +-- CI validation
          +-- milestone accepted
          |
          v
      0.1.16
          |
          +-- deterministic offline evidence replay
          +-- evaluation fingerprints/provenance
          +-- persistent evaluation outcome history
          +-- CI fixture validation
          +-- milestone accepted
          |
          v
      0.1.17
          |
          +-- local adaptive learning records
          +-- persistent Knowledge State
          +-- abstention and minimum evidence
          +-- lineage-preserving deterministic splits
          +-- resource-aware processing
          +-- CI validation
          +-- milestone accepted
          |
          v
      0.1.18
          |
          +-- Android adaptive-learning runtime
          +-- bounded background processing
          +-- Knowledge State diagnostics
          +-- Android/CI validation
          +-- milestone accepted
          |
          v
      0.1.19
          |
          +-- SmartPanel Game Mode read-only evidence
          +-- evidence contract tests
          +-- real-device ON/OFF/ON validation
          +-- CI/native validation
          +-- milestone accepted
```

The repository's source of truth remains Git history, while the version label communicates the validated milestone state.


## Milestone 0.1.20

Adaptive Learning Evaluation Hardening adds a strict descriptive evaluation boundary above the existing learner:

- existing measurement classifications map to explicit non-causal outcome labels;
- runtime context/confounder fields are declared and compared explicitly;
- incomplete or mismatched context cannot be treated as matched evidence;
- train-derived advisory candidates are checked only against matching holdout patterns;
- holdout evidence has a minimum count of two;
- evaluation reports permanently keep causal inference, policy selection, and execution disabled.

The milestone is evaluation-only. It does not add device mutation, privileged permissions, Action Engine authority, Safety Gate changes, or SmartPanel mutation. The existing P661N/API33 evidence remains descriptive/regression evidence.

The implementation is documented in docs/ADAPTIVE_LEARNING_EVALUATION.md.


## Milestone 0.1.21

Adaptive Optimizer Execution-Readiness Hardening establishes a non-executing structural readiness boundary for future candidate actions:

- allowlist membership is checked;
- only LOW-risk, permission-free, reversible actions can reach READY_FOR_REVIEW;
- measurement, rollback, verification, and kill-switch metadata are required;
- execution remains permanently disabled;
- candidate executionEnabled=true fails closed;
- Android unit tests cover the boundary.

0.1.21 does not introduce a real device action, policy-selection authority, permission escalation, SmartPanel/Game Mode mutation, or Action Engine execution capability.
