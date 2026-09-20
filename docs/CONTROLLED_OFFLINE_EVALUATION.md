# Controlled Offline Evaluation

This milestone establishes a deterministic temporal holdout boundary for future learning experiments.

History is kept in chronological order and split into an earlier training partition and a later holdout partition. The evaluator reports counts and condition/action coverage only.

It does not train a model, estimate effectiveness, infer causality, rank actions, select a policy, change DRY_RUN, grant permissions, or authorize device mutation.

The current report intentionally exposes `evaluationReady=false`. A future learning experiment must define explicit outcomes, control confounders, preserve a holdout set, establish reproducible evaluation criteria, and pass a separate safety review.

The default split is 80% training / 20% holdout with a minimum holdout size of two observations. These are data-partition parameters, not effectiveness thresholds.