# Adaptive Learning Summary

The adaptive-learning summary is a measurement-only aggregation of persistent decision history.

## Pipeline

Device snapshot → diagnosis → dry-run policy simulation → simulated action → persistent decision history → descriptive summary.

The summary reports:

- number of historical observations
- condition frequency
- action observation counts
- average descriptive RAM deltas
- average descriptive battery deltas

The report does not rank actions, select a policy, infer causality, grant permissions, or authorize execution.

## Persistence

The background agent writes `files/adaptive-learning-summary.json` using an atomic temporary-file replacement. The source history remains `files/decision-history.bin`.

## Safety boundary

All historical measurement reports must be `descriptive_only`. The adaptive-learning summary cannot change `DRY_RUN`, bypass the safety gate, or execute a device action.

A future learning milestone requires explicit outcome definitions, sufficient sample sizes, confounder handling, holdout evaluation, and a separate safety review before any learned policy could influence an execution path.
