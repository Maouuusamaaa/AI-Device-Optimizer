# Reproducible Offline Evaluation

This milestone establishes a deterministic dataset boundary for future offline experiments.

The evaluator:
- requires chronological records;
- detects duplicate observation IDs;
- detects duplicate timestamps as a leakage risk;
- verifies train/holdout observation-ID disjointness;
- uses a deterministic chronological split (default 80/20);
- counts incomplete metadata without filling or inferring values;
- counts declared outcome labels without interpreting them;
- emits a deterministic dataset fingerprint.

The evaluator does not train a model, estimate causal effects, rank actions, choose a policy, authorize execution, or mutate a device.

The fields \`evaluationReady\` and \`executionAllowed\` are intentionally fixed to \`false\`. A future milestone must add an explicit experimental protocol and validated outcome semantics before effectiveness analysis is permitted.

The current physical-device history remains descriptive-only. No real optimization outcome is fabricated from existing DRY_RUN observations.
