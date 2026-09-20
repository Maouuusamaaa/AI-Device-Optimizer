# Adaptive Learning and Decision History

This milestone adds a measurement-only history layer after post-action measurement evaluation.

Flow:

Device State -> Diagnosis -> Policy Simulation -> Safety Gate -> Dry-Run Action Simulation -> Descriptive Measurement -> Decision History -> Adaptive Learning Summary

Decision history records:
- timestamp;
- condition IDs and evidence-backed diagnoses;
- DRY_RUN policy decisions;
- simulated/blocked action results;
- descriptive before/after measurement reports.

The history store is currently in-memory and preserves insertion order. It returns immutable list snapshots to callers.

The adaptive-learning summarizer aggregates observed condition counts and descriptive measurement deltas by action. It deliberately does not:
- enable or execute an action;
- rank actions;
- choose a policy;
- infer causal effectiveness;
- convert observed deltas into a reward;
- bypass the safety gate;
- grant permissions.

This distinction is important: the project can accumulate empirical evidence before it is allowed to learn an optimization policy.

A future learning milestone should define a versioned feature schema, explicit outcome labels, confounder handling, minimum sample requirements, validation/holdout rules, and a safety review before any learned recommendation can affect execution.
