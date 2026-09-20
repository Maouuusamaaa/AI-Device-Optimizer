# Dry-Run Policy Proposal

## Purpose

The local policy layer now exposes an explicit, immutable dry-run proposal object. It connects the measured device state to the policy decisions that would be considered by a future action engine without executing any device mutation.

The proposal is deliberately observation-only:

- It preserves the complete DeviceState used for evaluation.
- It contains only DRY_RUN policy decisions.
- Proposed action IDs are descriptive observation targets, not commands.
- actionExecutionAllowed is permanently false in this milestone.
- No Shizuku/Rish privileged action is invoked by this layer.

## Decision flow

DeviceSnapshot -> DeviceState -> LocalPolicyEngine -> DryRunPolicyProposal -> benchmark/candidate observation

The proposal can therefore be captured alongside a candidate Rish observation. This establishes the missing experimental link between the measured device state and the local policy proposal without claiming that the proposal caused any performance change.

## Candidate gate

A candidate observation should only proceed when the proposal is generated from a real device snapshot and remains observation-only. The candidate protocol must continue to use the same Rish workload, five startup samples, memory capture, device identity, and comparable power/thermal conditions described in the physical baseline protocol.

A lower metric in a candidate run is not, by itself, evidence of an optimization effect. Baseline and candidate observations remain descriptive until a controlled intervention exists.
