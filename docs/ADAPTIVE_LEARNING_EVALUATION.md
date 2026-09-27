# Adaptive Learning Evaluation Hardening

Target milestone: 0.1.20

## Purpose

This milestone hardens the boundary between descriptive adaptive-learning evidence and any future effectiveness experiment.

The new layer does four things:

1. defines explicit descriptive outcome labels;
2. records declared runtime context/confounder fields without collecting private user content;
3. checks train-derived candidates against a disjoint holdout;
4. keeps causal inference, policy selection, and execution disabled.

## Explicit outcome contract

Existing measurement classifications are mapped without changing their meaning:

| Measurement classification | Descriptive outcome |
| --- | --- |
| \`NO_REGRESSION\` | \`NON_DEGRADING_DESCRIPTIVE\` |
| \`REGRESSION\` | \`DEGRADING_DESCRIPTIVE\` |
| \`MIXED\` | \`MIXED_DESCRIPTIVE\` |
| \`INSUFFICIENT_EVIDENCE\` | \`UNRESOLVED\` |

These labels describe the observed measurement classification. They are not rewards, causal effects, or proof that a policy caused a change.

## Context / confounder contract

The evaluator can compare a declared context set:

- \`interactive\`
- \`charging\`
- \`thermalStatus\`
- \`networkTransport\`
- \`gameModeChecked\`

A comparison is:

- \`MATCHED\` when every declared field exists on both sides and values agree;
- \`MISMATCHED\` when both sides provide a field but values differ;
- \`INCOMPLETE\` when required context is missing.

Unknown context fields are rejected so the evaluator does not silently absorb arbitrary/private data.

\`gameModeChecked\` is treated as observational workload context only. The SmartPanel provider remains read-only and device/build-specific.

## Holdout protocol

Candidates are generated from training state only. The evaluator then checks matching holdout observations.

A candidate receives:

- \`SUPPORTED_DESCRIPTIVELY\` when at least two holdout observations exist and all are non-degrading with complete matched context;
- \`CONTRADICTED_DESCRIPTIVELY\` when the holdout contains a degrading observation;
- \`MIXED_DESCRIPTIVELY\` when holdout observations contain mixed descriptive outcomes without a degrading observation;
- \`INSUFFICIENT_HOLDOUT\` when the holdout count is below two;
- \`CONTEXT_INCOMPLETE\` when holdout context is incomplete or unmatched.

The statuses are descriptive validation results. They do not select a policy or authorize execution.

## Safety boundary

Every report fixes:

- \`causalInferenceAllowed = false\`
- \`policySelectionAllowed = false\`
- \`executionAllowed = false\`

There is no new Action Engine path, permission escalation, Safety Gate modification, or SmartPanel mutation.

## Android/runtime relevance

The context fields are intentionally limited to state already observable by the local monitor. Android documentation describes thermal state and Game Mode as relevant runtime context for performance-sensitive workloads, including games; the optimizer uses such information only as evidence/context, not as authority to change another app's mode.

## Acceptance criteria

- deterministic explicit outcome mapping;
- confounder/context mismatch is surfaced rather than hidden;
- train-derived candidates are evaluated only against holdout records;
- holdout minimum is enforced;
- incomplete context fails closed;
- no causal/effectiveness claim is emitted;
- no policy selection or execution authority is introduced;
- Python compilation and Measurement Validation CI remain green.

No Android version bump is performed until these checks pass.
