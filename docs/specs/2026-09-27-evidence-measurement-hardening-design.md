# Evidence/Measurement Hardening Design

**Milestone:** 0.1.14  
**Status:** Design approved for implementation planning; release/version bump is not part of this design commit.  
**Baseline:** 0.1.13, whose controlled lifecycle investigation is closed as MIXED and did not establish reset-specific causality.

## 1. Goal

Strengthen the local measurement and evidence pipeline so that real-device observations are validated, compared only when comparable, classified deterministically, and retained with provenance before any result can become input to the local Safety Gate.

This milestone is measurement/evidence hardening only. It does not enable new optimization mutation and does not introduce the Cloud AI Advisor.

## 2. Existing repository boundary

The repository already has:

- `benchmarks/schema.json` as the benchmark evidence schema.
- `benchmarks/workloads.json` as the canonical workload matrix.
- `scripts/benchmark-analyzer.py` for normalization.
- `scripts/compare-internal-benchmarks.py` for descriptive comparison.
- `docs/BENCHMARK_ANALYZER.md` documenting measurement-only behavior.
- `docs/VERSIONING.md` defining milestone/version boundaries.
- Existing lifecycle evidence under `benchmarks/` and documented lifecycle closure in the 0.1.13 milestone.

The new implementation must extend these conventions rather than replace the existing benchmark format or reinterpret historical evidence. The Android benchmark writer currently emits schema version 2 while the historical contract used version 1, so the hardened validator and canonical schema remain compatible with both versions until a deliberate schema migration is completed.

## 3. Architecture

The evidence flow is:

`Experiment Runner → Raw Evidence → Evidence Validator → Pair Resolver → Comparison Engine → Regression Classifier → Evidence History → Safety Gate Input`

Each component has one responsibility:

### Evidence Validator

Validates an evidence record against the supported schema/contract before analysis. Invalid evidence is rejected from analysis and remains preserved as raw input.

### Pair Resolver

Selects comparison candidates only when required identity and workload dimensions are compatible. A missing or incompatible pair is not interpreted as a healthy result.

### Comparison Engine

Computes descriptive deltas from compatible observations. It does not modify raw evidence and does not make optimization decisions.

### Regression Classifier

Converts comparison results into deterministic classifications:

- `NO_REGRESSION`
- `REGRESSION`
- `MIXED`
- `INSUFFICIENT_EVIDENCE`
- `INVALID_EVIDENCE`

The classifier must expose the evidence/rules responsible for its result. It must not fabricate a probability or confidence score.

### Evidence History

Persists analysis results with references to their source evidence, comparison identity, classifier result, and analyzer/schema version. Raw evidence remains immutable.

### Safety Gate Input

The analyzer output may become diagnostic input to the local Safety Gate. It is never an authorization and must not directly invoke the Action Engine.

## 4. Comparison contract

Evidence may be paired only when the comparison identity required by the workload is compatible. At minimum, the implementation must preserve and compare the dimensions already represented by the repository's benchmark contracts, including device identity, Android/API level, application/runtime version, workload identity, and relevant experiment metadata. Supported benchmark schema versions are 1 and 2 for backward-compatible evidence ingestion.

A pair must not be created merely because two JSON records contain overlapping metric names.

When a required comparison dimension is missing, incompatible, or ambiguous, the result is `INSUFFICIENT_EVIDENCE` rather than `NO_REGRESSION`.

## 5. Classification rules

The classifier is fail-closed:

| Condition | Classification |
| --- | --- |
| Evidence contract invalid | `INVALID_EVIDENCE` |
| Required fields missing or malformed | `INVALID_EVIDENCE` |
| No valid comparable pair | `INSUFFICIENT_EVIDENCE` |
| Pair metadata incompatible | `INSUFFICIENT_EVIDENCE` |
| Comparable metrics indicate no configured regression | `NO_REGRESSION` |
| Comparable metrics cross the configured regression rule | `REGRESSION` |
| Valid metrics provide materially conflicting positive and negative measurement signals | `MIXED` |

The classifier must never infer causality from PSS/RSS changes alone. In particular, the existing 0.1.13 lifecycle result remains `MIXED`; this milestone must not rewrite it as a memory leak.

Regression thresholds must be explicit, versioned, deterministic, and covered by tests. They are measurement policy, not optimization policy.

## 6. Failure and data-integrity behavior

Raw evidence is never modified or deleted by validation or analysis.

Validation failures are represented separately from source evidence so an invalid artifact can still be audited.

Analyzer failures must not silently produce a successful classification.

Unknown schema versions are rejected rather than guessed.

Duplicate evidence identifiers must not overwrite an existing history record.

A history record must retain provenance sufficient to locate the source evidence and identify the analyzer/contract version that produced the result.

## 7. Safety boundary

This milestone adds no new privileged operation.

The following paths remain prohibited:

`Evidence → Action Engine`

`Classifier → mutation`

`Cloud/AI → bypass Safety Gate`

Only:

`Evidence → Analysis → Safety Gate input`

is introduced or strengthened.

The existing measure-before-optimize and reversible-action principles remain unchanged.

## 8. Testing strategy

### Contract tests

Use deterministic fixtures for:

- valid evidence;
- missing required field;
- malformed field type;
- unsupported schema version;
- inconsistent device/runtime metadata;
- duplicate evidence identifier.

### Pairing tests

Verify:

- compatible records pair;
- different device identities do not pair;
- incompatible workload identities do not pair;
- missing comparison identity produces `INSUFFICIENT_EVIDENCE`;
- no pair never becomes `NO_REGRESSION`.

### Classification tests

Verify deterministic results for:

- no regression;
- regression;
- mixed/conflicting signals;
- insufficient evidence;
- invalid evidence.

### Provenance/history tests

Verify:

- raw evidence remains unchanged;
- analysis references source evidence;
- analyzer/schema versions are retained;
- duplicate identifiers do not overwrite history.

### Integration tests

Run the complete offline evidence pipeline against repository fixtures and confirm that no Action Engine or mutation path is invoked.

### Physical-device validation

Run the evidence pipeline against a real-device observation from the P661N/API33 environment. The validation must demonstrate successful contract ingestion and classification without enabling production mutation.

## 9. Acceptance criteria

0.1.14 evidence/measurement hardening is complete when:

1. Contract validation is implemented and covered by tests.
2. Comparable-pair resolution is deterministic and covered by tests.
3. Regression classification is deterministic and covered by tests.
4. Raw evidence remains immutable.
5. Analysis provenance/history is preserved.
6. Analyzer output cannot directly invoke mutation.
7. Offline integration tests pass.
8. Required CI validation passes.
9. A real-device P661N/API33 validation is completed and documented.
10. Results are documented without overstating causality.
11. No new optimization mutation is enabled.
12. The release version is changed only after the milestone acceptance process in `docs/VERSIONING.md` is satisfied.

## 10. Explicit non-goals

This milestone does not:

- implement Cloud AI Advisor;
- train or deploy a cloud model;
- add new optimization actions;
- automatically change device settings;
- reinterpret the 0.1.13 lifecycle evidence;
- replace the existing benchmark schema without compatibility justification;
- treat one observation as proof of an optimization benefit.

## 11. Transition toward Cloud AI

After this milestone is accepted, the next engineering decision should be based on the remaining locally solvable gaps.

If local measurement, diagnosis, history, safety validation, and offline evaluation have no material unresolved blocker, the project can proceed to the Cloud AI Advisor while preserving the same local-authority boundary:

`Local Evidence → Local Diagnosis → Cloud Advisor → Local Policy Simulation → Local Safety Gate → Action Engine → Measurement`

Cloud AI remains advisory and cannot bypass local validation or authorization.
