# Stage 2 Controlled Experiment Framework

Stage 2 formalizes the protocol for moving from passive monitoring toward
real-device optimization experiments without prematurely enabling mutation.

Each experiment declares a unique experiment ID, one candidate action with
safety metadata, baseline/intervention/post phases, an explicit measurement
plan, and an execution state.

## Safety contract

The current implementation requires:

- execution.enabled=false
- execution.deviceMutationAllowed=false
- policySelectionAllowed=false
- executionAllowed=false

The protocol therefore cannot authorize an Android mutation. The intervention
phase is a reserved experimental slot, not permission to execute an action.

The existing controlled-observation runner can populate the three measurement
phases while this contract remains in force.

## What this milestone establishes

This is a protocol boundary, not an optimizer. It provides a stable input
contract for the future real Action Engine and makes measurement requirements
explicit before implementation.

No effectiveness, causal, ranking, or policy-selection claim is generated.

## Next gate before real mutation

Before enabling an actual action, the repository should add:

1. a concrete action implementation;
2. an allowlist entry with explicit permission and risk metadata;
3. device-state preconditions;
4. rollback verification;
5. an intervention-specific measurement adapter;
6. a kill switch and failure recovery path;
7. real-device validation with the action disabled by default.

Only after those checks should an action-specific experiment be considered.
