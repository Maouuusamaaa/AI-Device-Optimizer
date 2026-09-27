# Cloud Advisor + Local Policy Simulation Implementation Plan

> **For agentic workers:** Use the host's available task-by-task implementation workflow. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a validated Cloud Advisor candidate contract and connect it to the existing local dry-run Policy Simulator/Safety Gate without granting cloud output execution authority.

**Architecture:** Extend the existing Python Cloud contract layer with an explicit advisory-candidate contract and deterministic validator. On Android, adapt only validated cloud candidates into the existing `PolicySimulator`/dry-run Safety Gate path; no new execution capability is introduced. Preserve the current allowlist, dry-run mode, provenance, and offline behavior.

**Tech Stack:** Python 3 standard library, JSON contracts, Kotlin/JVM Android unit tests, existing Gradle test infrastructure, GitHub Actions.

## Global Constraints

- Cloud output is untrusted candidate data.
- Cloud has no direct path to Action Engine, Safety Gate bypass, permission escalation, allowlist changes, or safety-threshold changes.
- Invalid/malformed/unsupported Cloud output fails closed.
- Unknown action IDs are rejected by local allowlist validation.
- Policy Simulation is deterministic and never authorizes execution.
- Existing `PolicySimulator` and `DryRunSafetyGate` remain the local authority.
- Cloud failure must leave the useful local path available.
- Existing 0.1.14 evidence contracts, provenance principles, and P661N/API33 regression evidence remain intact.
- No adaptive learning, autonomous policy generation, new privileged capability, or direct cloud execution is introduced.
- Source evidence is not mutated.
- Existing tests and CI must remain green.
- Version bump is deferred until implementation and release verification are complete.

---

### Task 1: Harden the Cloud Advisor candidate contract

**Files:**
- Create: `cloud/contracts/cloud-advisor-schema.json`
- Create: `cloud/contracts/example-cloud-advisor-response.json`
- Modify: `scripts/validate-ai-cloud-contract.py`
- Modify: `tests/ai_cloud_contract_test.py`

**Interfaces:**
- Consumes: existing Stage 3 AI Cloud foundation response plus the new advisory candidate shape.
- Produces: `validate_advisor_response(value: dict) -> dict` and deterministic advisory-only validation report.

- [ ] **Step 1: Add focused failing tests**

Add tests for:
- valid advisory candidate;
- unsupported schema version;
- malformed/missing required candidate fields;
- unknown action ID;
- out-of-range confidence;
- `execution.requested=true`;
- `execution.deviceMutationAllowed=true`;
- cloud response containing an authorization/bypass field;
- input object remains unchanged after validation.

Expected assertions: invalid inputs raise `ContractError`; valid input returns advisory-only status and never execution authorization.

- [ ] **Step 2: Verify the relevant failure**

Run:
`python3 -m unittest tests/ai_cloud_contract_test.py -v`

Expected: the new candidate tests fail because the new advisor validator/schema does not yet exist.

- [ ] **Step 3: Implement minimum behavior**

Add an explicit schema with:
- `schemaVersion: 1`;
- `advisorVersion`;
- `evidenceId`;
- `recommendations`;
- each recommendation has `policyId`, `actionType`, `parameters`, `reason`, and `expectedEffect`;
- execution authority is not represented as a permitted true value.

The validator must:
- parse JSON without mutating the source object;
- require supported schema;
- require non-empty evidence/policy/action identifiers;
- require finite numeric confidence where used;
- reject unknown action IDs using the local allowlist;
- reject authorization/bypass fields rather than treating them as authority;
- return a normalized advisory-only report with no executable authorization.

Use the repository's existing `ActionCatalog` IDs as the Android-side allowlist source; do not create a second action catalog in Python.

- [ ] **Step 4: Verify the focused pass**

Run:
`python3 -m unittest tests/ai_cloud_contract_test.py -v`

Expected: all Cloud Advisor contract tests pass.

- [ ] **Step 5: Run affected integration check**

Run:
`python3 scripts/validate-ai-cloud-contract.py cloud/contracts/example-ai-cloud-response.json`

Expected: existing Stage 3 contract remains valid and reports execution disabled.

- [ ] **Step 6: Commit the passing deliverable**

Commit message:
`feat: add cloud advisor candidate contract`

---

### Task 2: Add deterministic local Policy Simulation adapter for Cloud candidates

**Files:**
- Create: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/CloudPolicyCandidate.kt`
- Create: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/CloudPolicyCandidateValidator.kt`
- Create: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/CloudPolicySimulationAdapter.kt`
- Create/modify: `android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/CloudPolicySimulationAdapterTest.kt`

**Interfaces:**
- Consumes: validated Cloud Advisor candidate data and existing `ActionCatalog`.
- Produces: dry-run `PolicyDecision`/simulation results that can only be evaluated by existing `DryRunSafetyGate`.

- [ ] **Step 1: Add focused failing tests**

Cover:
- valid allowlisted candidate becomes DRY_RUN only;
- unknown action is rejected before simulation;
- malformed/empty candidate is rejected;
- cloud-provided execution flags cannot enable execution;
- simulation is deterministic;
- source candidate is not mutated;
- simulation cannot produce `executionAllowed=true`;
- insufficient local evidence returns an explicit insufficient-evidence result rather than assuming benefit.

- [ ] **Step 2: Verify the relevant failure**

Run:
`cd android && ./gradlew :app:testDebugUnitTest --tests '*CloudPolicySimulationAdapterTest'`

Expected: the new tests fail because the adapter classes do not yet exist.

- [ ] **Step 3: Implement minimum behavior**

Create immutable Kotlin data classes for candidate and simulation result.

Validation must:
- require non-empty policy/action IDs;
- resolve action IDs exclusively through existing `ActionCatalog`;
- reject unsupported/unknown actions;
- preserve the cloud reason as informational text;
- never consume a cloud authorization field as executable authority.

The adapter should translate a validated candidate into the existing dry-run policy representation. It must force `PolicyMode.DRY_RUN` and `executionAllowed=false`.

If required evidence is absent, return `INSUFFICIENT_EVIDENCE` and do not create an executable candidate.

Do not modify `ActionCatalog` or enable any currently unavailable mutation.

- [ ] **Step 4: Verify the focused pass**

Run:
`cd android && ./gradlew :app:testDebugUnitTest --tests '*CloudPolicySimulationAdapterTest'`

Expected: all adapter tests pass.

- [ ] **Step 5: Run affected integration check**

Run:
`cd android && ./gradlew :app:testDebugUnitTest`

Expected: existing Android unit tests plus the new adapter tests pass.

- [ ] **Step 6: Commit the passing deliverable**

Commit message:
`feat: simulate cloud policies through local dry-run gate`

---

### Task 3: Add provenance and CI coverage

**Files:**
- Modify: `tests/ai_cloud_contract_test.py`
- Create: `tests/cloud_policy_simulation_contract_test.py`
- Modify: `.github/workflows/ai-cloud-validation.yml`
- Modify: the workflow containing existing Measurement Validation, if required by the repository's current test organization
- Create: `docs/CLOUD_ADVISOR_POLICY_SIMULATION.md`

**Interfaces:**
- Consumes: Task 1 validator and Task 2 simulation contract.
- Produces: deterministic provenance/audit linkage and CI-enforced safety invariants.

- [ ] **Step 1: Add focused failing tests**

Test:
- advisor response references originating evidence ID;
- validation result identifies advisor schema/version;
- simulation result identifies policy candidate;
- malformed/unknown candidates cannot reach simulation;
- Safety Gate rejection remains authoritative;
- no cloud field can change allowlist or execution state;
- existing P661N/API33 evidence fixtures remain accepted by the 0.1.14 measurement layer.

- [ ] **Step 2: Verify the relevant failure**

Run:
`python3 -m unittest tests/cloud_policy_simulation_contract_test.py -v`

Expected: new provenance/safety tests fail until the integration contract is implemented.

- [ ] **Step 3: Implement minimum behavior**

Record deterministic IDs/fields linking:
`evidenceId → advisorVersion → policyId → simulation result → safety decision`.

Keep source evidence immutable and append-only where history is used.

Update CI to compile/run the new contract tests and preserve existing Cloud/Measurement validation.

Document:
- request/response boundary;
- local validation;
- simulation boundary;
- offline fallback;
- fail-closed outcomes;
- provenance;
- explicit non-authority of Cloud output.

- [ ] **Step 4: Verify the focused pass**

Run:
`python3 -m unittest discover -s tests -p '*test.py' -v`

Expected: all Python tests pass.

- [ ] **Step 5: Run affected integration check**

Run:
`python3 -m py_compile scripts/validate-ai-cloud-contract.py tests/ai_cloud_contract_test.py tests/cloud_policy_simulation_contract_test.py`

and:
`cd android && ./gradlew :app:testDebugUnitTest`

Expected: both complete successfully.

- [ ] **Step 6: Commit the passing deliverable**

Commit message:
`test: enforce cloud advisor safety and provenance`

---

### Task 4: Milestone documentation, versioning, and release verification

**Files:**
- Modify: `README.md`
- Modify: `docs/VERSIONING.md`
- Modify: `docs/EVIDENCE_MEASUREMENT.md` only where the pipeline boundary needs to reference Cloud Advisor
- Modify: `docs/specs/2026-09-27-cloud-advisor-policy-simulation-design.md` status after implementation
- Modify: `android/app/build.gradle.kts` only after all implementation CI is green

**Interfaces:**
- Consumes: completed Cloud Advisor contract, local simulation, safety tests, provenance, and CI results.
- Produces: released milestone metadata consistent with repository versioning policy.

- [ ] **Step 1: Add documentation/version tests if the repository has existing version contract tests**

Expected: current version remains 0.1.14 until the implementation is fully validated.

- [ ] **Step 2: Verify current release baseline**

Run the existing complete Python and Android test suites before version mutation.

Expected: baseline is green.

- [ ] **Step 3: Update documentation and release metadata**

Document the new milestone as Cloud Advisor + Local Policy Simulation.

Only after all functional tests pass, increment Android versionCode/versionName according to the existing monotonic policy. Do not fabricate real-device evidence; use the existing P661N/API33 evidence as a regression fixture unless implementation-specific device validation is genuinely required.

- [ ] **Step 4: Verify release candidate**

Run the repository's full CI-equivalent local checks and Android release/build checks available in the project.

Expected: no contract, unit, safety, or versioning failures.

- [ ] **Step 5: Commit the passing release deliverable**

Commit message:
`release: cloud advisor policy simulation milestone`

## Unresolved externally observable decisions

None. The approved design fixes the cloud advisory-only boundary, local simulation requirement, fail-closed behavior, provenance relationship, offline fallback, and scope exclusions.
