# Evidence Measurement Contract

## Purpose

The evidence measurement layer turns raw benchmark observations into auditable
diagnostic evidence without authorizing device mutation.

## Pipeline

`Raw Evidence → Contract Validation → Comparable Pair → Descriptive Comparison → Regression Classification → Provenance/History → Safety Gate Input`

## Supported evidence

The canonical benchmark schema accepts versions 1 and 2. Version 2 is the format
currently emitted by the Android benchmark writer. Historical version 1 evidence
remains readable.

A comparable pair requires:

- valid evidence contracts;
- matching manufacturer/model;
- matching Android API level;
- matching workload;
- compatible schema version;
- required benchmark identity fields.

A missing or incompatible comparison does not imply health. It produces
`INSUFFICIENT_EVIDENCE`.

## Classifications

### NO_REGRESSION

No configured regression threshold was crossed.

### REGRESSION

At least one configured negative measurement signal crossed its threshold and
there was no contradictory improvement signal.

### MIXED

Valid evidence contains materially conflicting positive and negative measurement
signals. This classification is not authorization for optimization.

### INSUFFICIENT_EVIDENCE

There is no valid comparable pair or required comparison information is missing.

### INVALID_EVIDENCE

The source evidence does not satisfy the supported contract.

## Rules

Rules are versioned in `benchmarks/evidence_rules.json`.

Current rules use:

- available RAM decrease of 5% or more as a regression signal;
- PSS increase of 20% or more as a regression signal;
- corresponding strong improvements can create a MIXED result when another
  metric simultaneously signals regression.

These are descriptive classification thresholds, not optimization policies.

## Provenance

Every analysis record contains:

- analyzer version;
- source evidence SHA-256 values;
- benchmark schema version;
- rules version;
- deterministic analysis ID;
- classification and reasons;
- descriptive comparison data.

History is append-only. Duplicate analysis IDs are rejected without replacing
the original record.

## Safety boundary

The evidence layer has no privileged Android operation.

It cannot:

- invoke Action Engine;
- authorize a candidate action;
- bypass Safety Gate;
- infer a memory leak solely from PSS/RSS;
- replace raw evidence with an interpreted result.

## Real-device validation

The automated contract suite validates the existing P661N/API33 benchmark evidence
stored under `benchmarks/results/`. This verifies that real-device observations
can enter the hardened contract without introducing mutation.

The validation does not claim a new optimization benefit and does not reinterpret
the closed 0.1.13 lifecycle investigation.
