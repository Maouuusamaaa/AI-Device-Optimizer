# Safety Gate Contract

The Safety Gate is the hard boundary between policy simulation and future action execution.

Current contract:
1. Execution is globally disabled.
2. Only DRY_RUN policy decisions are accepted.
3. Candidate actions must exist in the explicit ActionCatalog allowlist.
4. Candidate actions must be LOW risk and require no additional permission.
5. Candidate actions must be reversible.
6. Candidate actions must declare expected effect, rollback semantics, and measurements.
7. Unknown actions are denied.
8. Denials contain human-readable reasons and structured SafetyBlockReason values.

The current catalog contains observation-only actions and cannot mutate device state.

A future executor must add explicit execution mode, authorization, preconditions, reversible behavior, post-action measurement, timeout handling, and rollback verification.
