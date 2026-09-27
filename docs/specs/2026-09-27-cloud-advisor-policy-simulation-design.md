# Cloud Advisor + Local Policy Simulation Design

**Date:** 2026-09-27  
**Status:** Implemented and validated in milestone 0.1.15

## 1. Purpose

The next milestone extends the 0.1.14 evidence/measurement foundation with a Cloud AI Advisor and a local Policy Simulation layer.

The goal is to let cloud reasoning propose optimization candidates without granting cloud output authority over device mutation. Local validation, simulation, the Safety Gate, and the Action Engine remain authoritative.

This milestone does not introduce adaptive learning, autonomous policy creation, privileged-operation expansion, or a direct Cloud-to-Action path.

## 2. Architecture

The intended flow is:

`Local Evidence → Local Diagnosis → Cloud Advisor → Contract Validation → Local Policy Simulation → Local Safety Gate → Action Engine → Measurement`

Cloud output is treated as untrusted candidate data.

The Cloud Advisor has no direct path to:
- the Action Engine;
- Safety Gate bypass;
- permission escalation;
- modification of local safety thresholds;
- modification of the action allowlist.

The existing offline-first behavior remains intact. If cloud connectivity is unavailable, local monitoring and any already-supported safe local policies continue without requiring the Cloud Advisor.

## 3. Cloud Advisor Input

The cloud request should contain only validated, purpose-specific information needed for advisory reasoning.

The initial contract is conceptually:

```json
{
  "schemaVersion": 1,
  "evidenceId": "example-evidence-id",
  "device": {
    "manufacturer": "example-manufacturer",
    "model": "example-model",
    "apiLevel": 33
  },
  "diagnosis": {
    "classification": "REGRESSION",
    "signals": []
  },
  "request": {
    "goal": "reduce_resource_pressure"
  }
}
```

The implementation must follow the repository's established evidence and diagnosis contracts rather than inventing incompatible representations.

## 4. Cloud Advisor Output

The Advisor may return candidate policies only:

```json
{
  "schemaVersion": 1,
  "advisorVersion": 1,
  "recommendations": [
    {
      "policyId": "example-policy-id",
      "actionType": "example-allowlisted-action",
      "parameters": {},
      "reason": "example-reason",
      "expectedEffect": "example-effect"
    }
  ]
}
```

The output is advisory. Informational fields such as `reason` and `expectedEffect` do not authorize execution.

The local implementation validates the response before any candidate reaches simulation.

## 5. Local Validation and Safety Boundary

The local side fails closed for invalid advisor output.

Required behavior:
- unsupported schema version → reject;
- missing required contract data → reject;
- malformed response → reject;
- unknown `actionType` → reject locally through the ActionCatalog;
- parameters outside the locally supported contract → reject;
- any cloud-supplied authorization field cannot alter local controls.

The action allowlist and security constraints remain local.

No Cloud Advisor response may directly trigger the Action Engine.

## 6. Policy Simulation

Every cloud-generated candidate intended for execution passes local Policy Simulation before Safety Gate evaluation.

Simulation is measurement/prediction infrastructure, not authorization.

The implemented adapter produces a deterministic dry-run result using the existing `PolicySimulator`. A simulation that lacks required local evidence reports `INSUFFICIENT_EVIDENCE` rather than assuming benefit.

A candidate indicating regression or unsafe/unsupported behavior is not forced through the pipeline.

## 7. Failure Handling

The implemented fail-closed outcomes are:

| Condition | Result |
|---|---|
| Cloud unavailable/timeout | No cloud candidate; local path remains usable |
| Invalid Cloud response | Contract validation failure |
| Unknown policy/action | Local policy validation rejection |
| Invalid parameters | Local policy validation rejection |
| Insufficient simulation evidence | `INSUFFICIENT_EVIDENCE` |
| Simulation indicates disallowed/regressive outcome | Candidate rejected |
| Safety Gate rejects | `REJECTED_BY_SAFETY_GATE` |
| Simulation succeeds | DRY_RUN result only; execution remains disabled |

No failure mode falls through to execution by default.

## 8. Provenance and Auditability

Advisor responses and simulation results retain the linkage fields needed to relate the candidate to its originating evidence and policy:

`evidenceId → advisorVersion → policyId → simulation result → local safety decision`

Existing 0.1.14 canonical provenance and append-only history principles are reused rather than replaced.

The milestone does not mutate source evidence.

## 9. Testing and CI

The implementation includes contract and integration coverage for:

- valid Cloud response;
- malformed JSON;
- unsupported schema version;
- missing required fields;
- unknown action type;
- invalid/out-of-range confidence;
- cloud execution/mutation flags;
- cloud timeout/offline contract behavior;
- successful simulation;
- insufficient simulation evidence;
- Safety Gate rejection;
- proof that cloud output cannot alter local execution authority;
- deterministic simulation;
- provenance/audit linkage;
- input immutability;
- regression coverage for existing P661N/API33 evidence.

All required CI checks for the implementation PR passed, including Python validation, Android unit tests, Android debug build, and Android native/release validation.

## 10. Scope Exclusions

This milestone does not include:
- adaptive learning;
- autonomous policy generation beyond cloud candidate recommendations;
- direct cloud execution;
- new privileged Android capabilities;
- Safety Gate bypass;
- interpreting PSS/RSS alone as proof of a memory leak;
- unrelated refactoring.

Future ideas may extend or revise this design in a later approved milestone.

## 11. Acceptance Criteria

The milestone is complete because:

1. Cloud Advisor candidate output is contract-validated locally.
2. Every executable cloud candidate passes local Policy Simulation.
3. Safety Gate remains authoritative.
4. Cloud failure does not disable the useful local path.
5. Invalid or unsafe candidates fail closed.
6. Provenance links advisor context to the originating evidence and policy candidate.
7. Automated tests cover the listed failure and success cases.
8. Existing 0.1.14 validation remains green.
9. Existing P661N/API33 evidence remains a regression fixture; no fabricated optimization benefit is claimed.
10. Android version 0.1.15/15 is reserved for the validated milestone release.
