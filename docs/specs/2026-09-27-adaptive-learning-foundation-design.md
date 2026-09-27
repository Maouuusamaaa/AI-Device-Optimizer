# Adaptive Learning Foundation Design

**Date:** 2026-09-27  
**Target milestone:** 0.1.17  
**Status:** Design approved by user; implementation not yet started

## 1. Goal

Build a local, offline-first Adaptive Learning Foundation that continuously and automatically learns from validated measurement/evaluation history while remaining advisory-only. The learner may update persistent knowledge and generate candidate recommendations, but it may not automatically select policies, mutate the device, alter the ActionCatalog, bypass the Safety Gate, escalate permissions, or depend on Cloud AI.

“Always-on” means an event-driven and resource-aware background learning loop that processes new evidence incrementally. It does not mean a permanently CPU-intensive model-training process.

## 2. Existing System Context

The current project flow is:

Monitor → Local Policy → Action Engine → Benchmark → Cloud API → Cloud AI → Offline Fallback → Adaptive Learning

Milestone 0.1.16 established deterministic offline replay and persistent evaluation outcome history. The existing evidence measurement layer is the measurement authority and must be reused rather than duplicated.

The relevant safety boundary is:

Validated Evidence → Measurement/Evaluation → Persistent History → Local Learner → Knowledge State → Candidate Recommendation → Policy Simulation → Safety Gate → Action Engine

For 0.1.17, the final two execution-capable stages remain gated. Automatic policy selection is disabled.

## 3. Design Principles

The milestone preserves these project principles:

- measure before optimize;
- reversible actions;
- explicit allowlist;
- minimum permissions;
- no assumed benefit without measurement;
- lightweight idle behavior;
- preserve Android behavior;
- log decisions and outcomes;
- Cloud AI cannot bypass the local Safety Gate;
- offline mode remains useful;
- no fabricated results;
- mutation/action only after evidence and safety validation.

Additional learning-specific principles:

- validated evidence is the only learning input;
- history is immutable/append-only at the evidence layer;
- learning updates are deterministic and idempotent;
- insufficient evidence causes abstention;
- causal effectiveness is not inferred from descriptive classifications;
- train/validation/holdout lineage must be isolated;
- knowledge state is versioned and reproducible;
- a learner failure must fail closed without authorizing an action.

## 4. Approach

The milestone uses a deterministic incremental learner foundation rather than a full ML training system.

The learner processes only new, validated records. It extracts versioned features, updates bounded knowledge statistics/state, and produces candidate patterns. A future statistical or ML model may consume this foundation after sufficient data quality and safety validation exist.

A full continuously retrained ML model is intentionally out of scope for 0.1.17 because the current evidence volume and causal labeling are insufficient to justify it.

## 5. Always-On Learning Loop

The conceptual loop is:

New Evidence → Validate → Check Lineage/Duplicate → Extract Features → Incremental Update → Validate State → Atomic Persist → Wait

The Android background behavior is resource-aware:

1. Detect newly available learning material.
2. Check whether the record has already been consumed.
3. Check device/resource conditions before doing non-trivial processing.
4. Process only the incremental delta.
5. Persist the resulting state atomically.
6. Return to an idle/waiting state.

The learner must not run continuously at high CPU usage. Heavy retraining is not part of this milestone.

A restart must not lose a valid previous state. If an update is interrupted, the previous valid state remains usable.

## 6. Learning Input Contract

Learning records are derived only from validated evidence and established evaluation output.

The logical record contains:

- schemaVersion;
- learnerSchemaVersion;
- deviceIdentity containing manufacturer, model, and API level;
- workloadIdentity;
- validated measurement features;
- measurement classification;
- optional policyId when the source evidence is explicitly associated with one;
- evidence/evaluation provenance;
- timestamp or ordered event metadata;
- source fingerprint.

The learner must reject malformed records.

INVALID_EVIDENCE is never a positive or negative training signal.

INSUFFICIENT_EVIDENCE is retained as an abstention signal and must not be converted into a success/failure reward.

Existing descriptive classifications remain descriptive. In particular, NO_REGRESSION does not mean that a policy caused an improvement.

## 7. Feature Extraction

Feature extraction is deterministic and versioned.

Initial feature families are limited to measurements already supported by the evidence layer, such as:

- available RAM measurements;
- PSS when available;
- process CPU time when available;
- device identity;
- workload identity;
- measurement/classification metadata;
- policy association metadata when present.

No unnecessary user-private data is introduced.

A feature extractor version must be recorded in the knowledge state so the same source history can be interpreted reproducibly.

## 8. Knowledge State

The persisted materialized knowledge state contains:

- schemaVersion;
- learnerVersion;
- source history fingerprint;
- processed evidence count;
- abstention count;
- deterministic feature statistics;
- per-device/workload knowledge;
- candidate-pattern records;
- evidence/confidence metadata;
- update sequence/timestamp;
- state fingerprint.

The knowledge state is rebuildable from persistent history. History remains the source of truth; the state is a materialized representation.

Updates must be idempotent. Processing the same evidence twice must not double-count it.

State persistence uses:

temporary state → validate → atomic replacement

Corrupt or unsupported state must fail closed and leave the last valid state available.

## 9. Minimum Evidence and Abstention

The learner must not form a meaningful learned pattern from insufficient evidence.

The implementation will define explicit minimum-sample and consistency requirements as versioned learner rules rather than hard-coding implicit assumptions throughout the code.

If requirements are not met, the learner emits an abstention state instead of a recommendation.

Abstention is expected behavior, not an error.

The learner must also abstain when source data is malformed, lineage is incompatible, or the observed data is outside the supported knowledge domain.

## 10. Lineage and Leakage Prevention

Training, validation, and holdout separation must use experiment/evidence lineage.

For a paired experiment, baseline + variant + associated measurements must remain in the same split.

Individual samples from one experiment must not be distributed across train and validation/holdout merely to increase sample counts.

This prevents the learner from seeing effectively identical evidence during training and evaluation.

The deterministic split policy established in the offline evaluation protocol remains the contract boundary.

## 11. Candidate Generation

The learner may produce a candidate recommendation containing:

- candidate policyId;
- supporting evidence references;
- knowledge/learner version;
- confidence/evidence metadata;
- descriptive reason.

Candidate generation is not policy authorization.

The learner must not produce or honor execution-authorizing fields such as:

- execute=true;
- bypassSafetyGate=true;
- permission escalation;
- ActionCatalog mutation;
- safety-threshold mutation.

Candidate output must enter the existing Policy Simulation and Safety Gate path.

Automatic policy selection remains disabled in 0.1.17.

## 12. Safety Boundary

There is no direct path from Learner to Action Engine.

There is no direct path from Learner to privileged Android API.

There is no path for learner state to modify:

- ActionCatalog;
- Safety Gate rules;
- permissions;
- measurement rules;
- evidence classifications.

Cloud AI is not required for the learning loop and cannot override local learning or safety decisions.

A learner crash, corrupt state, unsupported schema, insufficient evidence, or validation failure must not result in action execution.

## 13. Persistence and Recovery

Learning history remains append-only at the evidence/evaluation layer.

Knowledge state is replaceable materialized state and must be written atomically.

The implementation must support:

- clean first initialization;
- incremental update;
- duplicate input;
- restart/reload;
- corrupted state;
- unsupported schema;
- interrupted update;
- empty history;
- history with only insufficient/invalid evidence.

For corrupted materialized state, the system must fail closed and preserve the ability to rebuild from valid history rather than silently inventing state.

## 14. Testing Strategy

Tests must cover:

### Determinism
- identical input history produces identical feature/state output;
- state fingerprints are reproducible;
- candidate generation is deterministic.

### Idempotency
- duplicate evidence does not double-count;
- replaying the same history produces the same state.

### Evidence integrity
- malformed evidence is rejected;
- INVALID_EVIDENCE cannot train the learner;
- INSUFFICIENT_EVIDENCE causes abstention;
- source evidence is not mutated.

### Minimum evidence
- below-threshold data abstains;
- sufficient consistent data creates a learned pattern;
- inconsistent data does not produce an unjustified recommendation.

### Lineage isolation
- paired baseline/variant evidence remains in one split;
- no experiment lineage crosses train/validation/holdout.

### Persistence
- save/reload preserves state;
- interrupted/failed writes preserve the previous state;
- corrupt state fails closed;
- unsupported schema fails closed.

### Safety
- learner output cannot execute an action;
- learner cannot bypass Safety Gate;
- learner cannot modify ActionCatalog;
- learner cannot escalate permissions;
- candidate output must pass the existing simulation boundary.

### Android behavior
- background learning remains bounded;
- learning does not require network access;
- learning remains usable after application restart;
- resource-aware scheduling prevents unnecessary work during resource pressure.

### Existing regression coverage
The established P661N/API33 evidence remains usable as a regression fixture. It must not be interpreted as proof of optimization benefit.

## 15. Observability

Every learned state update should be attributable through:

evidence/evaluation ID → feature extractor version → learner version → knowledge state fingerprint

Logs should remain lightweight and descriptive.

The implementation must not log sensitive content unnecessarily.

## 16. Scope Exclusions

The following are explicitly not part of 0.1.17:

- automatic device optimization;
- automatic policy selection;
- direct Action Engine calls from the learner;
- Safety Gate bypass;
- ActionCatalog mutation;
- permission escalation;
- cloud-dependent learning;
- autonomous online ML retraining;
- causal-effect estimation;
- policy ranking presented as an authoritative decision;
- adaptive learning that changes safety constraints.

These may be considered only in later milestones after additional design and safety review.

## 17. Acceptance Criteria

Milestone 0.1.17 is complete only when:

1. Local learning can process new validated evidence incrementally.
2. Learning state persists across application restarts.
3. Duplicate input is idempotent.
4. State updates are deterministic and fingerprinted.
5. Invalid and insufficient evidence are handled conservatively.
6. Minimum evidence and abstention behavior are tested.
7. Experiment lineage is preserved across train/validation/holdout splits.
8. Background processing is resource-aware and does not require Cloud AI.
9. Candidate recommendations retain provenance.
10. No automatic policy selection is enabled.
11. No action can reach the Action Engine directly from the learner.
12. Safety Gate and ActionCatalog remain authoritative.
13. CI validates the learner, persistence, safety boundary, and existing regression fixtures.
14. Documentation describes the always-on learning behavior and its safety limits.

## 18. Future Evolution

After this foundation is validated, later milestones may introduce:

- stronger statistical uncertainty models;
- larger offline datasets;
- explicit outcome labels with defined semantics;
- model training;
- model calibration;
- distribution-shift detection;
- cloud-assisted analysis;
- controlled adaptive policy selection.

Each such capability requires its own design and safety review. The existence of the 0.1.17 learner must not be interpreted as approval for those future behaviors.