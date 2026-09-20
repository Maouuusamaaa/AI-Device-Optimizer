# Stage 5 — Kaggle Training & Model Readiness

Stage 5 establishes the reproducible handoff from the GitHub-controlled dataset to Kaggle without pretending that the current synthetic fixture is trainable.

## Safety boundary

Kaggle is a compute/evaluation environment. It is not an execution authority.

The cloud side must not:
- mutate an Android device;
- request execution;
- set `deviceMutationAllowed=true`;
- receive private device credentials;
- replace the local Safety Gate.

The Android path remains:

Device telemetry → local diagnosis → policy simulation → local Safety Gate → local Action Engine → measurement.

Kaggle produces model artifacts and evidence for the advisory layer only.

## Readiness gate

Run:

```bash
python3 cloud/dataset/training-readiness.py cloud/dataset/example-training-dataset.json
```

Exit code 0 means the dataset has at least two distinct outcome classes and passes structural checks. Exit code 2 means the dataset is structurally valid but supervised training is blocked because label diversity is insufficient.

The repository fixture is expected to return:

`TRAINING_BLOCKED_INSUFFICIENT_LABEL_DIVERSITY`

That is intentional. A one-class classifier could report misleadingly high accuracy while learning no useful action distinction.

## Kaggle notebook

Notebook:

`cloud/kaggle/stage5-training-readiness.ipynb`

It clones the public repository, runs the same readiness gate, checks safety invariants, and records the result. It does not contain private device data or credentials.

## Training readiness requirements

Before a learned action/diagnosis model is admitted:
1. Every row has valid provenance and deterministic split membership.
2. Required telemetry is present.
3. Labels represent validated evidence, not guesses.
4. There are at least two meaningful outcome classes.
5. Evaluation uses held-out data.
6. Recommendation validity and action IDs are checked locally.
7. Model output remains advisory-only.
8. Physical-device benefit is not inferred from offline metrics.

## Next data requirement

The next empirical milestone is to collect validated action → measurement outcomes from the existing local pipeline. Only outcomes that pass the local evidence rules should become candidate training labels. Private raw device artifacts stay outside the public repository; a sanitized, provenance-preserving dataset may be exported for Kaggle later.
