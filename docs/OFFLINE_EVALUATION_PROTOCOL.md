# Offline Evaluation Protocol

Milestone 0.1.16 defines a deterministic replay boundary for stored measurement evidence.

The offline evaluator:
- accepts a versioned dataset of baseline/variant evidence pairs;
- validates unique case identifiers and evidence contracts;
- reuses the established evidence measurement validator, pair resolver, comparison logic, and regression classifier;
- preserves `INVALID_EVIDENCE` and `INSUFFICIENT_EVIDENCE` rather than guessing;
- emits deterministic dataset and evaluation fingerprints;
- records source evidence provenance for every case;
- never mutates source evidence.

Evaluation outputs are descriptive only. They do not establish physical-device causality or optimization effectiveness.

The protocol explicitly keeps:
- policy selection disabled;
- execution disabled;
- causal inference disabled;
- action ranking disabled;
- device mutation disabled.

## Persistent evaluation outcome history

Evaluation reports may be appended to a JSONL history. The history is logically append-only and uses the deterministic `evaluationId` as its identity.

History handling is fail-closed:
- malformed existing history is rejected;
- records without an `evaluationId` are rejected;
- duplicate `evaluationId` records are not appended again;
- existing records are never replaced by a later evaluation.

This history stores evaluation outcomes and provenance, not fabricated action-effect claims.

Existing physical-device history and the P661N/API33 evidence remain descriptive/regression fixtures. DRY_RUN observations do not become causal action-effect evidence merely because offline replay is available.

Milestone 0.1.17 now consumes this history through a deterministic local learning-record boundary. The learner requires explicit minimum evidence, preserves descriptive classifications, rejects invalid evidence, abstains on insufficient evidence, and preserves experiment lineage during deterministic train/validation/test splitting. Learning remains advisory-only; it does not establish causal effectiveness or authorize policy selection/execution.
