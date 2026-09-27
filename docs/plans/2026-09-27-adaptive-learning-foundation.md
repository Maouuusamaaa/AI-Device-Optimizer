# Adaptive Learning Foundation Implementation Plan

> **For agentic workers:** Use the host's available task-by-task implementation workflow. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the 0.1.17 local Adaptive Learning Foundation with an always-on, incremental, persistent, deterministic learner that can generate advisory candidates but cannot select or execute policies.

**Architecture:** Reuse the existing evidence measurement and offline evaluation layers as the only measurement authority. Add a deterministic learning-record/feature layer, a persistent materialized knowledge state, and a resource-aware processing boundary; keep candidate output behind existing Policy Simulation and Safety Gate boundaries.

**Tech Stack:** Python 3, existing Android/Kotlin project conventions, JSON/JSONL persistence, unittest, GitHub Actions.

## Global Constraints

- Only validated evidence/evaluation outputs may enter learning.
- INVALID_EVIDENCE is never a learning signal.
- INSUFFICIENT_EVIDENCE causes abstention.
- Learning is incremental, deterministic, idempotent, and provenance-preserving.
- History remains source-of-truth; Knowledge State is rebuildable materialized state.
- State writes are atomic and fail closed.
- Train/validation/holdout splits preserve experiment lineage.
- Always-on means event-driven/resource-aware background processing, not continuous high-CPU retraining.
- Candidate generation cannot authorize execution.
- Automatic policy selection remains disabled in 0.1.17.
- Learner cannot modify ActionCatalog, Safety Gate rules, permissions, measurement rules, or evidence classifications.
- Cloud AI is not required for learning.
- No causal effectiveness claim is produced.
- No device mutation is introduced by this milestone.

---

### Task 1: Learning record and deterministic feature extraction

**Files:**
- Create: `scripts/adaptive_learning.py`
- Test: `tests/adaptive_learning_test.py`

**Interfaces:**
- Consumes: validated evidence/evaluation records from existing `scripts/evidence_measurement.py` and `scripts/offline_evaluation.py`.
- Produces: `create_learning_record(...)`, `extract_features(...)`, and versioned learning-record data suitable for Task 2.

- [ ] **Step 1: Add the focused failing test**
  - Test valid paired evaluation output creates a deterministic learning record with device/workload identity, classification, policy association, provenance, and feature-extractor version.
  - Test INVALID_EVIDENCE is rejected as a learning input.
  - Test INSUFFICIENT_EVIDENCE creates an abstention record.
  - Test repeated identical input produces identical output and does not mutate input.
- [ ] **Step 2: Verify the relevant failure**
  - Run: `python -m unittest tests/adaptive_learning_test.py -v`
  - Expected: import/module failure because `scripts/adaptive_learning.py` does not yet exist.
- [ ] **Step 3: Implement the minimum behavior**
  - Reuse existing classifier output rather than reimplementing measurement.
  - Define schema/version constants and deterministic canonical hashing.
  - Extract only existing validated metrics: available RAM, PSS when available, process CPU time when available, device/workload identity, classification, and optional policy association.
  - Reject malformed records and INVALID_EVIDENCE.
  - Preserve INSUFFICIENT_EVIDENCE as abstention.
- [ ] **Step 4: Verify the focused pass**
  - Run the same unittest command.
  - Expected: all Task 1 tests pass.
- [ ] **Step 5: Run the affected integration check**
  - Run: `python -m unittest tests/evidence_measurement_test.py tests/offline_evaluation_test.py -v`
  - Expected: existing measurement/evaluation tests remain green.
- [ ] **Step 6: Commit the passing deliverable**
  - Commit: `feat: add adaptive learning records`

### Task 2: Incremental knowledge state and candidate generation

**Files:**
- Modify: `scripts/adaptive_learning.py`
- Test: `tests/adaptive_learning_test.py`

**Interfaces:**
- Consumes: Task 1 learning records.
- Produces: deterministic `KnowledgeState` JSON object, `update_knowledge_state(...)`, `load/save_knowledge_state(...)`, and non-authorizing `generate_candidates(...)`.

- [ ] **Step 1: Add the focused failing tests**
  - Test below-minimum evidence abstains.
  - Test sufficient consistent records create a learned pattern.
  - Test duplicate evidence IDs do not double-count.
  - Test replaying the same records produces identical state/fingerprint.
  - Test candidate output contains provenance and no execution-authorizing fields.
  - Test a learner candidate cannot alter ActionCatalog/Safety Gate configuration.
- [ ] **Step 2: Verify the relevant failure**
  - Run: `python -m unittest tests/adaptive_learning_test.py -v`
  - Expected: AttributeError for missing Task 2 interfaces.
- [ ] **Step 3: Implement the minimum behavior**
  - Maintain bounded per-device/workload statistics and processed evidence IDs.
  - Require an explicit versioned minimum sample rule.
  - Abstain for insufficient, inconsistent, incompatible, or out-of-domain data.
  - Make state updates idempotent and deterministic.
  - Derive state fingerprint from canonical state.
  - Generate only advisory candidate records with evidence references and knowledge version.
  - Do not implement automatic policy selection or ranking.
- [ ] **Step 4: Verify the focused pass**
  - Run the same unittest command.
  - Expected: all Task 1 and Task 2 tests pass.
- [ ] **Step 5: Run affected integration checks**
  - Run: `python -m unittest tests/adaptive_learning_test.py tests/offline_evaluation_test.py tests/evidence_measurement_test.py -v`
  - Expected: all pass.
- [ ] **Step 6: Commit the passing deliverable**
  - Commit: `feat: add persistent adaptive knowledge state`

### Task 3: Atomic persistence and resource-aware always-on processing

**Files:**
- Modify: `scripts/adaptive_learning.py`
- Create: `tests/adaptive_learning_persistence_test.py`
- Create: `docs/ADAPTIVE_LEARNING.md`

**Interfaces:**
- Consumes: learning records and Knowledge State from Tasks 1–2.
- Produces: atomic state persistence/recovery helpers and a resource-aware incremental processing entry point.

- [ ] **Step 1: Add focused failing tests**
  - Test save/reload preserves state.
  - Test corrupt JSON fails closed without replacing a valid state.
  - Test unsupported schema fails closed.
  - Test interrupted/failed replacement leaves the previous state usable.
  - Test processing the same input twice is idempotent.
  - Test no-network operation by exercising only local inputs.
  - Test resource guard prevents non-trivial learning when the supplied resource predicate is false.
- [ ] **Step 2: Verify the relevant failure**
  - Run: `python -m unittest tests/adaptive_learning_persistence_test.py -v`
  - Expected: missing persistence/processing interfaces.
- [ ] **Step 3: Implement the minimum behavior**
  - Write validated state to a temporary sibling file and atomically replace the target.
  - Validate loaded schema/version before use.
  - Preserve the last valid state on load/write failures.
  - Process only unconsumed records.
  - Require an injected/local resource predicate before processing; do not introduce a permanent high-CPU service.
  - Keep the implementation network-independent.
- [ ] **Step 4: Verify the focused pass**
  - Run the same persistence test command.
  - Expected: all persistence/resource tests pass.
- [ ] **Step 5: Run affected integration checks**
  - Run: `python -m unittest tests/adaptive_learning_test.py tests/adaptive_learning_persistence_test.py tests/offline_evaluation_test.py tests/evidence_measurement_test.py -v`
  - Expected: all pass.
- [ ] **Step 6: Commit the passing deliverable**
  - Commit: `feat: add safe always-on learning persistence`

### Task 4: CI, lineage validation, documentation, and milestone integration

**Files:**
- Modify: `.github/workflows/measurement-validation.yml`
- Modify: `docs/OFFLINE_EVALUATION_PROTOCOL.md`
- Modify: `docs/PERSISTENT_LEARNING_HISTORY.md`
- Modify: `README.md`
- Modify: `docs/VERSIONING.md`
- Test: `tests/adaptive_learning_test.py`, `tests/adaptive_learning_persistence_test.py`

**Interfaces:**
- Consumes: adaptive learning module and tests from Tasks 1–3.
- Produces: CI-enforced 0.1.17 contract and documentation of always-on learning/safety boundaries.

- [ ] **Step 1: Add failing integration/lineage tests**
  - Test paired experiment lineage cannot be split across train/validation/holdout.
  - Test deterministic split output for identical dataset fingerprints.
  - Test candidate provenance includes source evidence and learner versions.
- [ ] **Step 2: Verify the relevant failure**
  - Run the focused adaptive-learning tests.
  - Expected: missing lineage/split integration behavior.
- [ ] **Step 3: Implement the minimum behavior**
  - Reuse the existing offline evaluation split-policy contract.
  - Add deterministic lineage validation without creating a second measurement authority.
  - Wire Python compilation and focused tests into Measurement Validation CI.
  - Document always-on learning, resource-aware operation, persistent state, abstention, lineage isolation, and the no-execution boundary.
- [ ] **Step 4: Verify the focused pass**
  - Run: `python -m unittest tests/adaptive_learning_test.py tests/adaptive_learning_persistence_test.py -v`
  - Expected: all pass.
- [ ] **Step 5: Run full affected validation**
  - Run: `python -m unittest tests/evidence_measurement_test.py tests/offline_evaluation_test.py tests/adaptive_learning_test.py tests/adaptive_learning_persistence_test.py -v`
  - Run: `python -m py_compile scripts/evidence_measurement.py scripts/offline_evaluation.py scripts/adaptive_learning.py`
  - Expected: all tests pass and compilation succeeds.
  - CI expected: Measurement Validation, AI Cloud Validation, Android CI, and Local Llama Android Validation remain green.
- [ ] **Step 6: Commit the passing deliverable**
  - Commit: `feat: validate adaptive learning foundation`

## Unresolved Product Decisions

None. The approved specification defines the externally observable 0.1.17 behavior and safety boundary. Exact internal statistics are implementation details as long as the stated deterministic, bounded, provenance-preserving, abstaining behavior and tests are maintained.
