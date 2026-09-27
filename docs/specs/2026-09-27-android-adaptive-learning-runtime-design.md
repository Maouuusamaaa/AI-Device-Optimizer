# Android Adaptive Learning Runtime Design

**Date:** 2026-09-27
**Target milestone:** 0.1.18
**Status:** Proposed for implementation

## 1. Goal

Connect the 0.1.17 Adaptive Learning Foundation to the Android runtime so validated local learning material is processed automatically in small, resource-aware, idempotent batches while remaining advisory-only.

## 2. Architecture

The Android runtime will use native Kotlin components rather than executing the Python learner on-device.

`Existing measurement/evaluation history → Android learning-record adapter → Knowledge State store → advisory candidates`

The existing `OptimizerBackgroundService` remains the scheduling owner. Learning runs after a successful monitoring/history cycle and never calls the Action Engine directly.

## 3. Components

### AdaptiveLearningRuntime

Owns one bounded learning pass. It reads new eligible history/evaluation material, converts it into the Android learning contract, invokes the deterministic Kotlin learner, and persists the resulting state.

It exposes a small synchronous API suitable for the existing single-thread background executor:
- `processAvailable(resourceGuard): AdaptiveLearningRunResult`
- `lastResult(): AdaptiveLearningRunResult?`

### AdaptiveLearningKnowledgeState

Immutable Kotlin model representing the materialized state:
- schema version;
- learner version;
- processed evidence IDs;
- abstention count;
- per-pattern observation count;
- classification consistency;
- feature aggregates;
- candidate eligibility;
- state fingerprint.

The state is rebuildable from valid source history.

### AdaptiveLearningKnowledgeStore

Persists the state under the app's internal files directory using temporary-file write followed by atomic replacement. Invalid or unsupported state is rejected without replacing the last valid state.

### AdaptiveLearningResourceGuard

Checks lightweight local conditions before non-trivial learning work. The first runtime policy is conservative:
- defer when the application reports resource pressure;
- defer when the device is thermally constrained;
- defer when the Android process is backgrounded and the existing monitoring loop is not in an active processing window;
- otherwise permit a small bounded batch.

No network check is required.

## 4. Learning Contract

The Android adapter accepts only locally validated descriptive records. It maps existing measurement/evaluation fields to the 0.1.17 contract.

- INVALID_EVIDENCE is rejected.
- INSUFFICIENT_EVIDENCE is retained as abstention.
- NO_REGRESSION, REGRESSION, and MIXED remain descriptive classifications.
- Evidence IDs are the idempotency key.
- Device manufacturer/model/API and workload remain part of the pattern key.
- Existing provenance is preserved.
- No private user content is added.

For the first runtime milestone, the learner processes at most 8 new records per pass. Remaining records stay pending for a later pass.

## 5. Knowledge Rules

A pattern becomes an eligible advisory candidate only after at least three processed observations for the same device/workload/policy lineage and all observations are NO_REGRESSION.

REGRESSION, MIXED, insufficient, malformed, or inconsistent observations do not become eligible.

Candidate output contains provenance and evidence references only. It contains no execution authorization.

## 6. Runtime Integration

After `recordHistory(...)` succeeds, `OptimizerBackgroundService` invokes the runtime learner.

The learning pass:
1. loads the current valid Knowledge State;
2. obtains only new source records not already processed;
3. applies the resource guard;
4. processes at most 8 records;
5. persists state atomically;
6. emits a lightweight descriptive result;
7. returns to the normal monitoring interval.

A learner failure is isolated from monitoring/policy evaluation. Monitoring continues and no action becomes newly executable.

The learner does not modify ActionCatalog, Safety Gate rules, permissions, measurement rules, or policy selection.

## 7. Persistence and Recovery

Knowledge State is stored separately from the existing bounded decision history.

State writes use:
`state.tmp → validate → atomic replacement of state file`.

Startup behavior:
- missing state → initialize empty state;
- valid state → reload;
- corrupt/unsupported state → fail closed and initialize only an in-memory empty state for the current pass, without deleting the corrupt file;
- subsequent valid history can rebuild the materialized state.

Duplicate evidence IDs never increase observation counts.

## 8. Testing

Android JVM tests will cover:
- learning-record conversion;
- minimum evidence and abstention;
- idempotent updates;
- deterministic fingerprints;
- candidate safety boundary;
- persistence/reload;
- corrupt and unsupported state;
- resource guard deferral;
- bounded batch size;
- runtime isolation when learning fails.

Integration tests will verify `OptimizerBackgroundService` invokes learning only after successful history persistence and that learning failure does not alter Safety Gate/action authorization.

CI will run the focused JVM tests plus existing Android CI, Measurement Validation, AI Cloud Validation, and Local Llama Android Validation workflows.

## 9. Release

The implementation will bump Android version to `0.1.18` only after all required CI checks pass. Documentation will state that Android now executes the local adaptive-learning loop, while full LLM self-training remains out of scope.

## 10. Explicit Safety Boundary

There is no Learner → Action Engine path.

There is no Learner → privileged Android API path.

The learner cannot:
- execute actions;
- select policies automatically;
- bypass Safety Gate;
- mutate ActionCatalog;
- escalate permissions;
- alter measurement/classification rules;
- require Cloud AI.

The existing DRY_RUN and Safety Gate authority remain unchanged.
