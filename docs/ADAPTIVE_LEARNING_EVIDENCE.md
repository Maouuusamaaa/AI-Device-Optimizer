# Adaptive Learning Evidence Evaluation

This milestone adds a measurement-only evidence-quality layer above persistent decision history.

## What it evaluates

The evaluator reports, per observed action:

- number of descriptive observations
- number of observations with finite RAM deltas
- number of observations with finite battery deltas
- whether the configured minimum sample count has been reached

The default minimum sample count is 10. This is a data-quality threshold, not a claim that ten observations establish effectiveness.

## What it does not do

The evaluator does not:

- rank actions
- estimate causal effects
- choose a policy
- change DRY_RUN
- grant permissions
- authorize execution
- treat simulated before/after values as action effects

All input measurement reports must remain \`descriptive_only\`.

## Future learning boundary

Reaching the sample threshold does not make an action eligible for execution. A future learning experiment still requires explicit outcome definitions, controlled comparisons, confounder handling, holdout evaluation, reproducibility, and a separate safety review.

The current report deliberately emits \`futureOfflineEvaluationReady=false\`.
