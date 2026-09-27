# Adaptive Learning Foundation

Milestone 0.1.17 introduces a local Adaptive Learning Foundation. It is an offline-first, deterministic, incremental learning boundary built on top of the existing evidence measurement and offline evaluation layers.

## Always-on behavior

"Always-on" means the learner is available to process new validated learning records automatically in small, resource-aware batches. It does not mean a continuously running high-CPU training process.

The processing boundary is:

`Validated Evidence → Evaluation → Learning Record → Knowledge State → Candidate → Policy Simulation → Safety Gate`

The learner can update knowledge automatically. It cannot authorize execution.

## Learning inputs

Only validated evaluation records enter the learner.

- `INVALID_EVIDENCE` is rejected.
- `INSUFFICIENT_EVIDENCE` becomes an explicit abstention.
- Existing classifications remain descriptive and are not treated as causal rewards.
- Features are limited to validated measurement fields already supported by the evidence layer.
- Device and workload identity are retained for lineage isolation.

## Knowledge state

The persistent Knowledge State is a deterministic materialized representation of history. The evidence/evaluation history remains the source of truth.

State contains version information, processed evidence IDs, abstention counts, device/workload patterns, supporting evidence IDs, feature aggregates, eligibility state, and a canonical state fingerprint.

Updates are idempotent. Processing the same evidence ID twice does not double-count it.

State files are validated and written atomically. Unsupported or corrupted state fails closed.

## Minimum evidence and abstention

The current foundation requires three consistent `NO_REGRESSION` observations for a pattern to become an eligible advisory candidate.

This is not an effectiveness claim. It is a conservative minimum-evidence gate for the foundation.

Mixed, regression, insufficient, malformed, or inconsistent observations do not become eligible candidates.

## Lineage protection

Deterministic train/validation/test splitting is performed by provenance lineage. Records sharing the same `provenance.analysisId` remain in the same split, preventing paired observations from leaking across evaluation boundaries.

The split is deterministic from the lineage identifier.

## Candidate boundary

Candidates contain policy identity, supporting evidence, learner/feature versions, and provenance. They intentionally do not contain execution authorization.

The learner cannot:

- execute an action;
- bypass the Safety Gate;
- change Safety Gate rules;
- modify the ActionCatalog;
- escalate permissions;
- modify measurement rules;
- select a policy automatically.

Candidate output must continue through the existing Policy Simulation and Safety Gate path.

## Resource-aware processing

The core processing API accepts a local resource predicate. When resources are not suitable, the learner leaves the existing state unchanged.

The foundation is network-independent and does not require Cloud AI.

## Recovery

Knowledge State persistence uses temporary-file validation followed by atomic replacement. A failed update does not intentionally replace the last valid state.

The state can be rebuilt from the persistent evidence/evaluation history.

## Verification

The Measurement Validation workflow covers:

- adaptive learning record validation;
- deterministic feature extraction;
- minimum-evidence abstention;
- idempotent knowledge updates;
- deterministic lineage splitting;
- candidate safety boundary;
- persistence and reload;
- corrupted/unsupported state rejection;
- resource-aware processing.

The existing P661N/API33 evidence remains a regression fixture and is not interpreted as proof of optimization benefit.
