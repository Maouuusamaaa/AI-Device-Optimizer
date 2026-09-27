# Cloud Advisor + Local Policy Simulation

The Cloud Advisor is an advisory boundary, not an execution authority.

The runtime flow is:

`Evidence → Local Diagnosis → Cloud Advisor → Contract Validation → Local Policy Simulation → Local Safety Gate → Action Engine → Measurement`

The Cloud Advisor receives purpose-specific validated information and may return candidate policies. Its output is untrusted data. The Android-side `ActionCatalog`, `PolicySimulator`, and `DryRunSafetyGate` remain authoritative.

## Cloud contract

The v1 candidate response contains:

- `schemaVersion`
- `advisorVersion`
- `evidenceId`
- `recommendations[]`

Each recommendation contains:

- `policyId`
- `actionType`
- `parameters`
- `reason`
- `expectedEffect`
- optional `confidence`

The contract deliberately contains no executable authorization. Unknown or malformed fields are rejected by the contract validator.

The Python validator is a structural contract boundary. It does not replace Android authorization. The Android `ActionCatalog` is the authoritative local allowlist.

## Local validation

A candidate must satisfy:

1. supported schema version;
2. non-empty advisor/evidence/policy/action identifiers;
3. valid parameters object;
4. non-empty reason and expected effect;
5. finite confidence in the range 0..1 when supplied;
6. no cloud authorization or Safety Gate bypass fields;
7. local action allowlist validation.

The validator does not mutate the source candidate.

## Policy Simulation

A validated candidate is adapted into the existing local dry-run policy model.

The adapter:

- copies candidate collections;
- resolves the action through `ActionCatalog`;
- creates an advisory local diagnosis;
- passes it through `PolicySimulator`;
- forces `PolicyMode.DRY_RUN`;
- never produces `executionAllowed=true`.

If the candidate references evidence that is unavailable locally, the result is `INSUFFICIENT_EVIDENCE`. The system does not assume that the proposed action is beneficial.

## Safety boundary

The existing `DryRunSafetyGate` remains authoritative after simulation.

Cloud output cannot:

- execute an action;
- bypass the Safety Gate;
- enable device mutation;
- add an action to the allowlist;
- change local risk or permission rules;
- escalate permissions.

An allowlisted candidate can therefore be simulated while still being denied by the current observation-only execution boundary.

## Offline behavior

Cloud availability is not required for the existing local monitoring and dry-run policy path. A timeout or unavailable Cloud Advisor produces no cloud candidate and does not disable the local path.

## Provenance

The candidate carries `evidenceId`, `advisorVersion`, and `policyId`. These fields provide the linkage:

`evidenceId → advisorVersion → policyId → simulation result → local safety decision`

Source evidence remains immutable. Existing 0.1.14 append-only provenance rules remain unchanged.

## Testing

CI validates:

- valid and malformed Cloud Advisor responses;
- unsupported schema versions;
- missing fields;
- invalid confidence;
- authorization/bypass field rejection;
- input immutability;
- existing P661N/API33 measurement fixture compatibility;
- Android allowlist validation;
- deterministic local simulation;
- insufficient-evidence behavior;
- Safety Gate authority;
- execution remaining disabled.

This milestone does not introduce adaptive learning, new privileged Android capabilities, autonomous policy execution, or direct Cloud-to-Action behavior.
