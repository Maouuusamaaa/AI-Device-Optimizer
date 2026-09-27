# Android Adaptive Learning Runtime Implementation Plan

> **For agentic workers:** Use the host's available task-by-task implementation workflow. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect the 0.1.17 adaptive-learning foundation to the Android monitoring service as a bounded, idempotent, resource-aware, advisory-only runtime and release it as 0.1.18.

**Architecture:** Add a Kotlin-native adaptive-learning runtime beside the existing measurement/policy classes. It will adapt persisted descriptive decision-history entries into validated learning records, maintain a separate atomic knowledge-state file, and expose advisory candidates without any execution path. The foreground monitoring service invokes one bounded learning pass only after decision-history persistence succeeds; learner failures remain isolated from monitoring and Safety Gate behavior.

**Tech Stack:** Kotlin, Android SDK API 29+, AndroidX Core, JUnit 4 JVM tests, Gradle, existing GitHub Actions workflows.

## Global Constraints

- Process only validated descriptive evidence; no causal effectiveness claims.
- Reject malformed/unsupported learning input and never ingest INVALID_EVIDENCE.
- Treat INSUFFICIENT_EVIDENCE as an abstention.
- Deduplicate by deterministic evidenceId.
- Process at most 8 new evidence records per runtime pass.
- Persist knowledge separately from bounded decision history.
- Persist atomically and validate before replacement.
- Missing knowledge state initializes empty.
- Corrupt or unsupported state fails closed without deleting the existing corrupt file.
- Candidate eligibility requires at least 3 observations for the same device/workload/policy lineage, all classified NO_REGRESSION.
- Candidates are advisory only and retain provenance/evidence references.
- Learner has no path to ActionEngine, privileged Android APIs, ActionCatalog mutation, Safety Gate bypass, permission escalation, policy auto-selection, or measurement-rule changes.
- No network or cloud dependency is required.
- Resource pressure/thermal constraints defer learning rather than fail the monitoring loop.
- Android runtime uses Kotlin-native logic; Python adaptive-learning code remains the reference/foundation and is not executed on-device.
- Release version is 0.1.18 / versionCode 18 only after focused tests and CI validation pass.

---

### Task 1: Add the Kotlin learning-record adapter and deterministic state model

**Files:**
- Create: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningRecord.kt`
- Create: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningKnowledgeState.kt`
- Create: `android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningRecordTest.kt`
- Create: `android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningKnowledgeStateTest.kt`

**Interfaces:**
- Consumes: `DecisionHistoryEntry`, `PostActionMeasurementReport`, `MeasurementDelta`, `DeviceState`, `PolicyDecision`.
- Produces: immutable learning records containing schema/learner/feature versions, deterministic `evidenceId`, device identity, workload, policy lineage, classification, extracted features, provenance, and learning disposition.
- Produces: knowledge state with processed evidence IDs/counts, abstentions, per-pattern observations/classifications/features, eligibility, and deterministic state fingerprint.

- [ ] **Step 1: Add focused failing tests**
  - Convert a valid DRY_RUN history entry into descriptive learning records.
  - Verify deterministic evidence IDs for identical source history.
  - Verify malformed source records are rejected.
  - Verify INSUFFICIENT_EVIDENCE becomes ABSTAIN.
  - Verify duplicate evidence IDs do not increase counts.
  - Verify a third consistent NO_REGRESSION observation makes a pattern eligible.
  - Verify REGRESSION, MIXED, abstention, and inconsistent classifications keep a pattern ineligible.
  - Verify fingerprints are stable for equivalent sorted state.

- [ ] **Step 2: Verify relevant failure**
  - Run from `android/`: `./gradlew :app:testDebugUnitTest --tests '*AdaptiveLearning*Test'`
  - Expected: compilation/test failures because the runtime model and adapter do not yet exist.

- [ ] **Step 3: Implement minimum behavior**
  - Build deterministic IDs from existing timestamp/policy/action/report lineage rather than inventing mutable identifiers.
  - Map available RAM, PSS/process and CPU-related metrics only when the existing history contains validated values; do not manufacture missing metrics.
  - Keep provenance tied to the originating history timestamp and descriptive analysis lineage.
  - Use sorted sets/maps for deterministic state serialization/fingerprints.
  - Keep all learning outputs descriptive/advisory.

- [ ] **Step 4: Verify focused pass**
  - Run the same Gradle test command.
  - Expected: all adaptive-learning model/adapter tests pass.

- [ ] **Step 5: Run affected integration check**
  - Run: `./gradlew :app:testDebugUnitTest`
  - Expected: all Android JVM tests pass.

- [ ] **Step 6: Commit the passing deliverable**
  - `git add android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningRecord.kt android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningKnowledgeState.kt android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningRecordTest.kt android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningKnowledgeStateTest.kt`
  - `git commit -m "feat: add android adaptive learning state model"`

### Task 2: Add atomic knowledge persistence and resource guard

**Files:**
- Create: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningKnowledgeStore.kt`
- Create: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningResourceGuard.kt`
- Create: `android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningKnowledgeStoreTest.kt`
- Create: `android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningResourceGuardTest.kt`

**Interfaces:**
- Consumes: `Context.filesDir`, `AdaptiveLearningKnowledgeState`, Android `ActivityManager.MemoryInfo`, `PowerManager` thermal status.
- Produces: validated/reloaded knowledge state and a boolean/resource decision describing whether a learning pass may run.

- [ ] **Step 1: Add failing persistence/guard tests**
  - Missing state returns empty state.
  - Valid state survives save/reload.
  - Corrupt JSON/unsupported schema throws while preserving the existing file.
  - Failed replacement does not erase the last valid state.
  - Low-memory guard defers.
  - severe/critical thermal status defers.
  - Normal resources permit processing.
  - The guard requires no network and no new Android permission.

- [ ] **Step 2: Verify failure**
  - Run: `./gradlew :app:testDebugUnitTest --tests '*AdaptiveLearningKnowledgeStoreTest' --tests '*AdaptiveLearningResourceGuardTest'`
  - Expected: failures because persistence/guard classes do not exist.

- [ ] **Step 3: Implement minimum behavior**
  - Store state at `filesDir/adaptive-learning-knowledge.json`.
  - Write to a temporary sibling file, flush/close, validate, then atomically replace the target.
  - Never delete a corrupt existing state as recovery.
  - Use API 29-compatible thermal APIs.
  - Defer when `ActivityManager.MemoryInfo.isLowMemory` is true or available memory falls below a conservative internal threshold.
  - Defer for severe/critical thermal status.
  - Keep thresholds constants covered by tests.

- [ ] **Step 4: Verify focused pass**
  - Run the same focused command.
  - Expected: all persistence/guard tests pass.

- [ ] **Step 5: Run affected integration check**
  - Run: `./gradlew :app:testDebugUnitTest`
  - Expected: all Android JVM tests pass.

- [ ] **Step 6: Commit the passing deliverable**
  - `git add android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningKnowledgeStore.kt android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningResourceGuard.kt android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningKnowledgeStoreTest.kt android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningResourceGuardTest.kt`
  - `git commit -m "feat: persist adaptive learning knowledge safely"`

### Task 3: Add bounded runtime and integrate it after successful history persistence

**Files:**
- Create: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningRuntime.kt`
- Create: `android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningRuntimeTest.kt`
- Modify: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/agent/OptimizerBackgroundService.kt`
- Modify: `android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/DecisionHistoryRecorder.kt` only if required to expose the persisted entry without changing its contract.

**Interfaces:**
- `AdaptiveLearningRuntime.processAvailable(): AdaptiveLearningRunResult`
- `AdaptiveLearningRuntime.lastResult`
- `AdaptiveLearningRunResult` reports processed count, deferred/abstained counts, candidate count, and state fingerprint descriptively.
- The service invokes learning only when `DecisionHistoryRecorder.record()` has successfully persisted the entry.

- [ ] **Step 1: Add failing runtime/integration tests**
  - Empty history produces zero processed records.
  - A pass processes no more than 8 new records.
  - Repeated passes process each evidence ID once.
  - Resource deferral leaves knowledge unchanged.
  - Persistence failure leaves the previous state intact and does not crash monitoring.
  - Three consistent NO_REGRESSION records expose an advisory candidate with provenance.
  - Candidate output cannot be passed directly to ActionEngine/Safety Gate.
  - Service integration path calls the learner only after successful history persistence.
  - Learner exception is isolated and does not alter Safety Gate/action authorization.

- [ ] **Step 2: Verify failure**
  - Run: `./gradlew :app:testDebugUnitTest --tests '*AdaptiveLearningRuntimeTest'`
  - Expected: failures because the runtime and integration changes are not present.

- [ ] **Step 3: Implement minimum behavior**
  - Load knowledge state.
  - Read existing persistent history and adapt only new entries.
  - Select at most 8 unprocessed records in deterministic insertion order.
  - Apply state updates and generate advisory candidates.
  - Persist state atomically only when the pass changes it.
  - Return a descriptive result and retain the last result for diagnostics.
  - Change `recordHistory` to return a success indicator (or return the recorded entry) instead of swallowing success information; keep its exception isolation.
  - Invoke the learner after successful history persistence and before report generation.
  - Do not alter `PolicySimulationEvaluator`, `SafetyGateResult`, `ActionCatalog`, or `DryRunActionEngine`.

- [ ] **Step 4: Verify focused pass**
  - Run the focused runtime tests.
  - Expected: all runtime tests pass.

- [ ] **Step 5: Run affected integration checks**
  - Run: `./gradlew :app:testDebugUnitTest`
  - Run: `./gradlew :app:assembleDebug`
  - Expected: JVM tests and Android debug compilation/package succeed.

- [ ] **Step 6: Commit the passing deliverable**
  - `git add android/app/src/main/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningRuntime.kt android/app/src/test/java/com/maouuusama/ai/device/optimizer/policy/AdaptiveLearningRuntimeTest.kt android/app/src/main/java/com/maouuusama/ai/device/optimizer/agent/OptimizerBackgroundService.kt`
  - `git commit -m "feat: run adaptive learning incrementally on android"`

### Task 4: Release 0.1.18, documentation, CI and final verification

**Files:**
- Modify: `android/app/build.gradle.kts`
- Modify: relevant adaptive-learning documentation and version documentation identified in the repository.
- Modify: `docs/specs/2026-09-27-android-adaptive-learning-runtime-design.md` status from Proposed for implementation to Implemented/Validated after all checks pass.
- Create: `docs/plans/2026-09-27-android-adaptive-learning-runtime.md` as this implementation plan artifact if not already created.
- Test: existing Android and repository CI workflows.

**Interfaces:**
- Release artifact exposes versionCode 18 / versionName 0.1.18.
- Existing four validation workflows remain authoritative for cross-system regression coverage.

- [ ] **Step 1: Add release/documentation tests or checks**
  - Assert versionCode/versionName are 18/0.1.18.
  - Assert no learner-to-action or learner-to-privileged-API path exists in the changed sources.
  - Assert the design spec is marked implemented/validated only after tests pass.

- [ ] **Step 2: Verify release checks fail before the version bump**
  - Run focused repository checks that demonstrate the old version is still 0.1.17.
  - Expected: version assertion identifies the pending release bump.

- [ ] **Step 3: Implement release metadata/docs**
  - Bump Android version only after Tasks 1–3 pass.
  - Document continuous bounded local learning, state persistence, resource deferral, idempotence, and advisory-only candidates.
  - Document that this is not continuous LLM retraining and does not authorize device mutation.

- [ ] **Step 4: Verify complete local Android suite**
  - Run: `./gradlew :app:testDebugUnitTest`
  - Run: `./gradlew :app:assembleDebug`
  - Expected: all JVM tests pass and debug APK assembles successfully.

- [ ] **Step 5: Run repository CI and verify all four existing validation workflows**
  - Measurement Validation
  - AI Cloud Validation
  - Android CI
  - Local Llama Android Validation
  - Expected: all required workflow checks succeed for the release branch/PR.

- [ ] **Step 6: Commit and merge only after green validation**
  - Commit release/docs changes with `chore: release 0.1.18 adaptive learning runtime`.
  - Open a PR from `feat/android-adaptive-learning-runtime` to `main`.
  - Merge with squash only after required checks are green.

- [ ] **Step 7: Verify the merged main branch**
  - Confirm main contains versionCode 18/versionName 0.1.18, the runtime integration, knowledge persistence, tests, and updated design status.
  - Re-run the authoritative Android JVM test/build checks against main.
  - Expected: merged main is green and no safety-boundary regression is present.

## Unresolved externally observable product decisions

None. The approved design fixes the externally visible behavior: bounded maximum batch size 8, minimum eligibility evidence 3, advisory-only candidates, resource-aware deferral, separate atomic knowledge state, fail-closed state recovery, and post-persistence service integration.
