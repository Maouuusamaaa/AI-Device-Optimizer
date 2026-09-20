# Stage 4 — AI Dataset & Baseline Model

Stage 4 prepares the project for Kaggle without connecting Kaggle to the device.

Benchmark artifacts -> reproducible dataset builder -> deterministic baseline -> offline evaluation.

Real device artifacts are inputs, not public training data. Private telemetry must remain outside Git.

The initial labels are deliberately conservative. The baseline uses observation-only behavior until action-specific evidence exists. This prevents the first model from learning unsupported mutations.

Each dataset row carries a stable row ID, observation reference, normalized features, label, and provenance. Train/validation/test membership is explicit. Synthetic fixtures are allowed in the repository; private device artifacts are not.

The deterministic baseline is an offline reference implementation, not a learned model. Its output is advisory and always uses OBSERVE_ONLY.

Evaluation is descriptive: coverage, abstention, and label agreement. It is not evidence that optimization improves a physical device.

Kaggle should consume versioned dataset artifacts and the same contracts. Model artifacts must be versioned by immutable identifiers and evaluated offline before local integration.

Safety invariant: no cloud or baseline component can set deviceMutationAllowed=true or request execution. Local Policy Simulation and Safety Gate remain authoritative.
