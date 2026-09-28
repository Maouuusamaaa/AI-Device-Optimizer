# Adaptive Optimizer Execution Readiness

Target milestone: 0.1.21

## Purpose

0.1.21 establishes a structural readiness boundary between learned/candidate actions and any future real device execution.

The milestone does not enable execution. It verifies that a candidate action has the information required for a separately reviewed execution milestone:

- allowlist membership;
- LOW action risk;
- no additional permission;
- reversible semantics;
- measurement definition;
- candidate preconditions;
- rollback plan;
- post-action verification;
- kill-switch identity.

## Readiness semantics

A candidate can receive:

- `READY_FOR_REVIEW`: the structural prerequisites are present, but execution remains disabled;
- `BLOCKED`: one or more prerequisites are missing or unsafe.

`READY_FOR_REVIEW` must not be interpreted as permission to execute.

Every readiness result permanently reports:

`executionAllowed = false`

## Boundary

The readiness evaluator:

- does not select a policy;
- does not rank policies;
- does not enable an ActionCatalog entry;
- does not grant permissions;
- does not call privileged Android APIs;
- does not mutate SmartPanel/Game Mode;
- does not execute an action;
- does not bypass the existing DryRunSafetyGate.

The existing `ActionExecutor` remains non-mutating.

## Candidate contract

`CandidateActionSpec.executionEnabled` remains hard-failed when set to true. This prevents a caller from silently converting a candidate into an executable action.

The candidate must also retain a kill-switch identifier and explicit rollback/verification semantics.

## Validation

The Android test suite covers:

1. a structurally valid observation candidate reaches `READY_FOR_REVIEW`;
2. execution remains disabled for that candidate;
3. an unknown action is blocked;
4. an action requiring a permission is blocked;
5. attempting to construct an execution-enabled candidate fails.

The milestone must also pass the existing Measurement Validation, AI Cloud Validation, Android CI, and Local Llama Android Validation workflows before release closure.

## Future execution milestone

A future real action requires a separate design and review containing, at minimum:

- explicit permission analysis;
- reversible implementation and rollback;
- pre-action Safety Gate conditions;
- post-action measurement and verification;
- decision-log provenance;
- dedicated failure/kill-switch behavior;
- controlled real-device evidence;
- CI coverage;
- explicit release acceptance.

Until that milestone is independently accepted, `READY_FOR_REVIEW` remains a review state only.
