# Evidence/Measurement Hardening Implementation Plan

> **For agentic workers:** Use the host's available task-by-task implementation workflow. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a dependency-light, deterministic local evidence pipeline that validates benchmark contracts, pairs only comparable observations, classifies measurement outcomes fail-closed, and preserves provenance without enabling mutation.

**Architecture:** Add one focused Python measurement module behind the existing benchmark scripts/tests. The module validates the existing benchmark contract, resolves comparable observation pairs, computes descriptive deltas, classifies outcomes, and creates immutable provenance records; existing raw evidence and Android runtime behavior remain unchanged.

**Tech Stack:** Python 3.12, JSON, existing repository benchmark schema/workload contracts, GitHub Actions Measurement Validation workflow, Android/P661N real-device evidence.

## Global Constraints

- Preserve the 0.1.13 baseline and historical evidence.
- Do not change versionName/versionCode during implementation.
- Do not add privileged actions or automatic optimization mutation.
- Raw evidence is immutable to the analyzer.
- Invalid/unknown evidence fails closed.
- Missing/incompatible pairs produce INSUFFICIENT_EVIDENCE, never NO_REGRESSION.
- Classification is deterministic and rule-based; no fabricated confidence/probability.
- PSS/RSS changes alone cannot be classified as a memory leak.
- Analyzer output cannot directly invoke Action Engine.
- Cloud AI remains outside this milestone.
- Private device data must not be committed.

---

### Task 1: Evidence contract and validator

**Files:**
- Create: `scripts/evidence_measurement.py`
- Test: `tests/evidence_measurement_test.py`

**Interfaces:**
- Consumes: benchmark JSON objects matching `benchmarks/schema.json`.
- Produces: validation result containing `valid`, normalized identity fields, and explicit validation errors.

- [ ] **Step 1: Add the focused failing test**
  - Test valid fixture acceptance.
  - Test missing `schemaVersion`, unsupported schema version, missing workload, malformed device identity, and malformed sample fields.
  - Assert invalid input returns explicit errors rather than raising an uncaught exception.

- [ ] **Step 2: Verify the relevant failure**
  - Run: `python tests/evidence_measurement_test.py`
  - Expected: import/behavior failure because the evidence measurement module does not yet provide the validator.

- [ ] **Step 3: Implement the minimum behavior**
  - Implement `validate_evidence(record) -> dict`.
  - Accept schema version 1 only.
  - Validate required top-level fields and required sample fields using the existing schema semantics.
  - Reject boolean values where numeric fields are required.
  - Validate device identity when present and require androidApi/manufacturer/model for comparable device evidence.
  - Return structured errors without mutating the input object.

- [ ] **Step 4: Verify the focused pass**
  - Run: `python tests/evidence_measurement_test.py`
  - Expected: validator tests pass.

- [ ] **Step 5: Run the affected integration check**
  - Run: `python tests/benchmark_analyzer_test.py` when present.
  - Expected: existing benchmark analyzer behavior remains green.

- [ ] **Step 6: Commit the passing deliverable**
  - Commit: `feat: add benchmark evidence contract validation`

### Task 2: Comparable-pair resolver and descriptive comparison

**Files:**
- Modify: `scripts/evidence_measurement.py`
- Test: `tests/evidence_measurement_test.py`

**Interfaces:**
- Consumes: validated evidence records.
- Produces: pair decision plus deterministic metric deltas.

- [ ] **Step 1: Add the focused failing test**
  - Pair same device/API/workload records.
  - Reject different device model/API.
  - Reject different workload.
  - Reject missing comparison identity.
  - Verify no pair returns an explicit insufficient-evidence reason.
  - Verify raw records remain byte-equivalent after comparison.

- [ ] **Step 2: Verify the relevant failure**
  - Run: `python tests/evidence_measurement_test.py`
  - Expected: pairing API is absent or assertions fail.

- [ ] **Step 3: Implement the minimum behavior**
  - Implement `resolve_pair(baseline, variant)`.
  - Require schema compatibility, device identity compatibility, workload equality, and valid timestamps.
  - Implement `compare_pair(pair) -> dict` with descriptive deltas only.
  - Do not mutate either source object.

- [ ] **Step 4: Verify the focused pass**
  - Run: `python tests/evidence_measurement_test.py`
  - Expected: pairing/comparison tests pass.

- [ ] **Step 5: Run the affected integration check**
  - Run: `python tests/controlled_observation_analyzer_test.py`
  - Expected: existing analyzer remains green.

- [ ] **Step 6: Commit the passing deliverable**
  - Commit: `feat: add comparable evidence pairing`

### Task 3: Deterministic regression classifier

**Files:**
- Modify: `scripts/evidence_measurement.py`
- Test: `tests/evidence_measurement_test.py`
- Create: `benchmarks/evidence_rules.json`

**Interfaces:**
- Consumes: validated pair comparison.
- Produces: classification, rule identifiers, and metric-level reasons.

- [ ] **Step 1: Add the focused failing test**
  - Assert `NO_REGRESSION` for changes within configured thresholds.
  - Assert `REGRESSION` when a configured regression threshold is crossed.
  - Assert `MIXED` when independent metrics provide conflicting regression signals.
  - Assert `INSUFFICIENT_EVIDENCE` when no valid pair exists.
  - Assert `INVALID_EVIDENCE` for invalid source evidence.

- [ ] **Step 2: Verify the relevant failure**
  - Run: `python tests/evidence_measurement_test.py`
  - Expected: classifier API/expected classifications are missing.

- [ ] **Step 3: Implement the minimum behavior**
  - Store explicit rule IDs and thresholds in `benchmarks/evidence_rules.json`.
  - Implement `classify_comparison(comparison, rules)`.
  - Keep rules deterministic and versioned.
  - Treat conflicting signals as MIXED.
  - Never emit a memory-leak diagnosis from PSS/RSS alone.

- [ ] **Step 4: Verify the focused pass**
  - Run: `python tests/evidence_measurement_test.py`
  - Expected: all classifier cases pass.

- [ ] **Step 5: Run the affected integration check**
  - Run: `python tests/runtime_lifecycle_memory_benchmark_contract_test.py`
  - Expected: existing lifecycle evidence contract remains green and its historical MIXED result is unchanged.

- [ ] **Step 6: Commit the passing deliverable**
  - Commit: `feat: add deterministic evidence regression classification`

### Task 4: Provenance/history record and offline integration

**Files:**
- Modify: `scripts/evidence_measurement.py`
- Test: `tests/evidence_measurement_test.py`
- Create: `tests/fixtures/evidence_measurement/` fixtures as needed.
- Modify: `.github/workflows/measurement-validation.yml`

**Interfaces:**
- Consumes: raw evidence + comparison + classification.
- Produces: immutable analysis record with source IDs/hashes, analyzer version, schema/rule versions, and classification.

- [ ] **Step 1: Add the focused failing test**
  - Verify history record contains source identifiers, source hashes, analyzer version, evidence schema version, rule version, and classification.
  - Verify source evidence is unchanged.
  - Verify duplicate evidence identifiers cannot overwrite an existing record in a deterministic in-memory history representation.
  - Verify analyzer output contains no action/mutation command or authorization field.

- [ ] **Step 2: Verify the relevant failure**
  - Run: `python tests/evidence_measurement_test.py`
  - Expected: provenance/history behavior is absent.

- [ ] **Step 3: Implement the minimum behavior**
  - Implement immutable record creation using canonical JSON hashing.
  - Implement deterministic duplicate detection.
  - Keep persistence format append-only/record-oriented; do not introduce a database dependency.
  - Add the focused test to the existing Measurement Validation workflow.

- [ ] **Step 4: Verify the focused pass**
  - Run: `python tests/evidence_measurement_test.py`
  - Expected: provenance/history tests pass.

- [ ] **Step 5: Run the affected integration check**
  - Run: `python tests/evidence_sync_contract_test.py` and the complete measurement validation test set.
  - Expected: all pass.

- [ ] **Step 6: Commit the passing deliverable**
  - Commit: `feat: preserve evidence analysis provenance`

### Task 5: Documentation and real-device validation

**Files:**
- Modify: `docs/BENCHMARK_ANALYZER.md`
- Create: `docs/EVIDENCE_MEASUREMENT.md`
- Create: `benchmarks/results/...` only for sanitized, non-sensitive real-device evidence permitted by the repository conventions.

**Interfaces:**
- Consumes: implemented analyzer output and P661N/API33 observations.
- Produces: documented evidence pipeline and reproducible validation record.

- [ ] **Step 1: Add the focused validation**
  - Use an existing P661N/API33 benchmark observation with no private identifiers.
  - Validate ingestion, pairing/classification where a compatible pair exists, and provenance generation.

- [ ] **Step 2: Verify expected failure/success boundary**
  - Run the validator against the selected real-device evidence.
  - Expected: contract validation succeeds; no mutation path is invoked.

- [ ] **Step 3: Document minimum behavior**
  - Document CLI/API usage, classifications, failure semantics, provenance fields, and safety boundary.
  - Explicitly preserve the 0.1.13 lifecycle MIXED interpretation.

- [ ] **Step 4: Verify**
  - Run all Python measurement tests.
  - Run repository CI through the Measurement Validation workflow.
  - Verify no versionName/versionCode changes.

- [ ] **Step 5: Commit**
  - Commit: `docs: document evidence measurement hardening`

### Task 6: Final verification and milestone decision

**Files:**
- Inspect: `docs/VERSIONING.md`
- Inspect: `docs/specs/2026-09-27-evidence-measurement-hardening-design.md`
- Inspect: all changed files and CI results.

- [ ] **Step 1: Run complete automated verification**
  - Python measurement tests.
  - Existing measurement validation workflow.
  - Existing evidence/lifecycle contract tests.
  - Android validation if touched files require it.

- [ ] **Step 2: Verify safety boundary**
  - Confirm no new privileged operation, Action Engine call, automatic mutation, or cloud bypass was introduced.

- [ ] **Step 3: Verify milestone criteria**
  - Contract validation, deterministic pairing/classification, provenance, integration tests, CI, and real-device evidence are all present.

- [ ] **Step 4: Release decision**
  - Only if every acceptance criterion is proven, define the release update from 0.1.13 to 0.1.14 and increment Android versionCode monotonically.
  - If any criterion remains unproven, keep the implementation on the active branch without claiming 0.1.14 complete.

- [ ] **Step 5: Final report**
  - Report exact commits/PR, test commands and outcomes, real-device evidence, unresolved blockers, and whether the local pipeline is ready for the next Cloud AI milestone.
