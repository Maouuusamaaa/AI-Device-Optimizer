# Offline Evaluation Protocol

This milestone defines the boundary between a structurally valid dataset and any future effectiveness analysis.

The protocol checks:
- chronological deterministic partitioning;
- train/holdout integrity;
- duplicate/leakage signals from the reproducible evaluator;
- metadata completeness;
- condition coverage;
- action coverage;
- outcome-label counts separately for training and holdout.

A dataset can become READY_FOR_DESCRIPTIVE_REVIEW only when it has no detected leakage, complete evaluation metadata, and both training and holdout observations.

That readiness does not mean that an optimization is effective. It only means the dataset is structurally suitable for descriptive review.

The protocol explicitly keeps:
- policy selection disabled;
- execution disabled;
- causal inference disabled;
- effectiveness estimation disabled;
- action ranking disabled.

The current physical-device history is still descriptive-only. Existing DRY_RUN observations do not become action-effect evidence merely because this protocol exists.
