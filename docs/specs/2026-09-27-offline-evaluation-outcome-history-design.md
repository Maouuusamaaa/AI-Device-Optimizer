# 2026-09-27 — Offline Evaluation + Persistent Outcome History

Status: Implemented and validated in milestone 0.1.16

## Purpose

Milestone 0.1.16 turns the existing descriptive offline-evaluation foundation into a deterministic replay boundary for stored evidence and a persistent history of evaluation outcomes.

The milestone does not train a model, estimate causal effects, rank policies, select actions, authorize execution, or mutate a device.

## Architecture

The flow is:

```
Persistent Measurement Evidence
        |
        v
Offline Dataset Contract
        |
        v
Deterministic Evidence Replay
        |
        v
Existing Evidence Measurement Classifier
        |
        v
Evaluation Outcome Report
        |
        v
Append-only Evaluation Outcome History
        |
        v
Future Adaptive Learning input
```

The evaluator reuses the established `evidence_measurement.py` validation, pairing, comparison, classification, and provenance logic instead of creating a second measurement authority.

## Dataset Contract

The offline evaluation dataset has:

- `schemaVersion = 1`;
- unique non-empty `caseId`;
- optional `policyId` used only as provenance;
- one baseline evidence record;
- one variant evidence record.

Evidence remains subject to the existing schema-version and device/workload comparability rules.

Malformed evidence remains `INVALID_EVIDENCE`. Missing comparison identity remains `INSUFFICIENT_EVIDENCE`. Comparable evidence is classified using the existing versioned measurement rules.

## Evaluation Contract

Each evaluation produces:

- deterministic `datasetFingerprint`;
- deterministic `evaluationId`;
- evaluator and rule versions;
- per-case classification;
- aggregate classification counts;
- source evidence IDs and analysis provenance;
- `executionAllowed = false`;
- `policySelectionAllowed = false`;
- a descriptive-only interpretation.

The evaluator never changes source evidence.

## Persistent Outcome History

Evaluation reports can be appended to a JSONL history file.

History rules:

1. Existing history is parsed before a new record is written.
2. Malformed history is rejected rather than silently discarded.
3. Every record requires a non-empty `evaluationId`.
4. An existing `evaluationId` is treated as a duplicate and is not appended again.
5. New records are appended without replacing prior records.
6. History contains evaluation outcomes, not claims of physical-device causality.

The history is therefore suitable as a future learning input while remaining non-authorizing in this milestone.

## Safety Boundary

The evaluator cannot:

- execute an Android action;
- enable device mutation;
- bypass the Safety Gate;
- select or rank a policy;
- infer causal effectiveness from dry-run data;
- replace the established evidence measurement classifier.

The existing P661N/API33 evidence remains a regression fixture. No new optimization benefit is claimed from it.

## Testing

The milestone covers:

- deterministic replay;
- regression classification preservation;
- insufficient-evidence preservation;
- duplicate case rejection;
- input immutability;
- append-only history behavior;
- duplicate evaluation suppression;
- malformed dataset rejection;
- Python compilation;
- existing evidence measurement tests;
- existing Cloud Advisor contract tests;
- Android unit tests;
- Android debug build;
- Android native/release APK validation.

## Acceptance Criteria

The milestone is accepted only when:

- offline evaluator tests pass;
- measurement and Cloud contract CI pass;
- Android unit/build CI passes;
- native release validation passes;
- source evidence remains immutable;
- evaluation and history records remain non-authorizing;
- the implementation is documented;
- `versionName = 0.1.16` and `versionCode = 16` are introduced only in the release change after the implementation PR is green.
