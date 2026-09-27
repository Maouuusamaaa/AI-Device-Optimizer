# Persistent Learning History

The repository now has two complementary persistence boundaries.

## Device decision history

The Android application persists measurement-only decision cycles in its private application storage.

The existing binary history contains:
- schemaVersion: 1;
- ordered decision entries;
- conditions and diagnoses;
- DRY_RUN policy decisions;
- simulated or blocked actions;
- descriptive-only measurement reports.

Unsupported schemas and malformed history are rejected. Writes use an atomic temporary-file replacement, and the store is bounded to 1000 entries by default.

This history cannot execute actions, authorize device mutation, create rewards, rank policies, or claim causal effectiveness.

## Offline evaluation outcome history

Milestone 0.1.16 adds a separate JSONL history for deterministic offline evaluation reports.

Each record contains:
- deterministic evaluationId;
- evaluator and rules versions;
- dataset fingerprint;
- per-case classifications;
- evidence-analysis provenance;
- explicit non-authorizing flags.

The history is logically append-only. Existing records are parsed before appending; malformed history fails closed; duplicate evaluation IDs are ignored rather than duplicated.

The evaluation history therefore records what the evaluator concluded from stored evidence, not what an optimization supposedly caused on a physical device.

Before historical data can influence optimization, a later milestone must define versioned features, explicit outcome labels, minimum sample requirements, confounder handling, validation/holdout rules, and a safety review.


## 0.1.17 Local Adaptive Learning

The 0.1.17 Local Learner consumes validated evaluation-derived learning records and maintains a deterministic materialized Knowledge State.

The Knowledge State is not a replacement for history. It contains processed evidence IDs, bounded feature statistics, device/workload patterns, abstention metadata, candidate eligibility, and a state fingerprint. It can be rebuilt from valid persistent history.

The learner is always-on in the resource-aware sense: it can process new records automatically in bounded local batches, but it does not run continuous high-CPU retraining.

Knowledge updates are idempotent. Invalid evidence is rejected, insufficient evidence abstains, and candidate output cannot authorize execution.

Persistence uses atomic replacement so a failed update does not intentionally replace the last valid state.
