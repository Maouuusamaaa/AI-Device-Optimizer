# Offline Dataset Quality

This milestone adds a structural quality gate before future offline learning experiments.

Checks include:
- duplicate timestamps, treated as a potential leakage risk;
- adjacent action overlap, reported as a review risk rather than proof of leakage;
- actionless observations;
- descriptive-only measurement enforcement;
- explicit absence of controlled outcome and key confounder metadata.

The current history schema does not yet persist controlled outcome labels or per-observation charging, thermal, network, and foreground-workload metadata. The checker therefore reports those fields as missing instead of guessing them.

This layer does not train a model, estimate causal effects, rank actions, select policies, change DRY_RUN, grant permissions, or execute device mutations. `evaluationSafeForReview` is intentionally always false.

A future experimental dataset should add explicit outcome labels, richer condition metadata, a documented split strategy, leakage prevention rules, and reproducible evaluation criteria before any model-learning milestone.