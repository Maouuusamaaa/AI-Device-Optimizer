# Cloud Advisor + Local Policy Simulation Design

**Date:** 2026-09-27  
**Status:** Accepted design; implementation pending

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

The local implementation must validate the response before any candidate reaches simulation.

## 5. Local Validation and Safety Boundary

The local side must fail closed for invalid advisor output.

Required behavior:
- unsupported schema version → reject;
- missing required contract data → reject;
- malformed response → reject;
- unknown `actionType` → reject;
- parameters outside the locally supported contract → reject;
- any cloud-supplied authorization/bypass field is ignored as authority and cannot bypass local controls.

The action allowlist and security constraints remain local.

No Cloud Advisor response may directly trigger the Action Engine.

## 6. Policy Simulation

Every cloud-generated candidate intended for execution must pass local Policy Simulation before Safety Gate evaluation.

Simulation is measurement/prediction infrastructure, not authorization.

The simulator should produce a deterministic result using the available local evidence and established policy contracts. A simulation that lacks sufficient evidence must report an insufficient-evidence outcome rather than assuming benefit.

A candidate indicating regression or unsafe/unsupported behavior must not be forced through the pipeline.

## 7. Failure Handling

The intended fail-closed outcomes are:

| Condition | Result |
|---|---|
| Cloud unavailable/timeout | No cloud candidate; local path remains usable |
| Invalid Cloud response | `INVALID_ADVISOR_RESPONSE` |
| Unknown policy/action | `REJECTED_BY_POLICY_VALIDATION` |
| Invalid parameters | `REJECTED_BY_POLICY_VALIDATION` |
| Insufficient simulation evidence | `INSUFFICIENT_EVIDENCE` |
| Simulation indicates disallowed/regressive outcome | Candidate rejected |
| Safety Gate rejects | `REJECTED_BY_SAFETY_GATE` |
| All required checks pass | Candidate may proceed to Action Engine |

No failure mode may fall through to execution by default.

## 8. Provenance and Auditability

Advisor responses, validation results, simulation results, and final decisions should retain sufficient provenance to relate them to the originating evidence and policy candidate.

Existing 0.1.14 canonical provenance and append-only history principles should be reused rather than replaced.

The milestone must not mutate source evidence.

## 9. Testing and CI

The milestone must include contract and integration coverage for:

- valid Cloud response;
- malformed JSON;
- unsupported schema version;
- missing required fields;
- unknown action type;
- invalid/out-of-range parameters;
- cloud timeout/offline behavior;
- successful simulation;
- insufficient simulation evidence;
- simulation regression/rejection;
- Safety Gate rejection;
- proof that cloud output cannot bypass Safety Gate;
- deterministic simulation;
- deterministic classification;
- provenance/audit linkage;
- input immutability;
- regression coverage for existing P661N/API33 evidence.

The existing 0.1.14 measurement tests and CI workflows must remain green.

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

The milestone is complete only when:

1. Cloud Advisor candidate output is contract-validated locally.
2. Every executable cloud candidate passes local Policy Simulation.
3. Safety Gate remains authoritative.
4. Cloud failure does not disable the useful local path.
5. Invalid or unsafe candidates fail closed.
6. Provenance connects advisor → validation → simulation → decision → measurement where applicable.
7. Automated tests cover the listed failure and success cases.
8. Existing 0.1.14 validation remains green.
9. Real-device validation is performed only where the implementation requires it; existing P661N/API33 evidence remains a regression fixture rather than fabricated new evidence.
10. A new version milestone is released only after implementation and CI/release verification succeed.
