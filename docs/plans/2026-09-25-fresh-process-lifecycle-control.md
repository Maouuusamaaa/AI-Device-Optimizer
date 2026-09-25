# Fresh-Process Lifecycle Reset Control Experiment Implementation Plan

> For agentic workers: Use the host's available task-by-task implementation workflow. Steps use checkbox syntax for tracking.

Goal: Add a fresh-process-aware paired reset/no-reset lifecycle experiment to determine whether observed PSS transitions are specifically associated with the explicit native reset boundary.

Architecture: Extend the existing runtime lifecycle diagnostic rather than creating a parallel benchmark. The benchmark will capture process identity/start metadata, use a common lifecycle with a reset branch or no-reset branch, and persist wall-clock plus monotonic lifecycle events. Existing sampling and evidence synchronization remain the source of truth.

Tech Stack: Kotlin/Android, JNI/C++, existing runtime lifecycle benchmark JSON writer, Python contract tests, GitHub Actions, Qwen3 0.6B Q4_0 diagnostic runtime.

## Global Constraints
- Keep VersionName at 0.1.13.
- Do not change production runtime generation/reset semantics.
- Do not kill processes, force-stop packages, change Android settings, or mutate device state beyond normal benchmark execution.
- Use identical inference parameters, durations, sampling interval, build, model, and device across reset/no-reset modes.
- Retain PSS, RSS, Swap PSS, available RAM, and temperature measurements.
- Record lifecycle events and detect/report material PSS transitions.
- Prefer monotonic elapsed timing in addition to wall-clock timestamps.
- Do not classify a PSS transition as a memory leak without corroborating evidence.
- Require at least two comparable reset/control pairs for an experimental conclusion unless an implementation/instrumentation failure blocks collection.
- Preserve the existing evidence-sync mechanism and benchmark result directory.

---

### Task 1: Extend lifecycle telemetry with fresh-process metadata and monotonic events

Files:
- Modify: android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/RuntimeLifecycleMemoryBenchmark.kt (observed benchmark path; confirm exact path before editing)
- Modify: android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/RuntimeLifecycleMemoryBenchmarkJsonWriter.kt (observed writer path; confirm exact path before editing)
- Modify: tests/runtime_lifecycle_memory_benchmark_contract_test.py

Interfaces:
- Consumes: existing RuntimeLifecycleDiagnosticResult, lifecycle sample collection, resetEnabled.
- Produces: process metadata and lifecycle events containing wall-clock timestamp plus monotonic elapsed timestamp where supported.

- [ ] Add failing assertions requiring PID, process-start metadata or an explicit unavailable marker, monotonic event timing, and fresh-process state.
- [ ] Run python3 tests/runtime_lifecycle_memory_benchmark_contract_test.py and confirm the new contract fails.
- [ ] Capture current process PID, supported process-start metadata, a monotonic benchmark origin, and monotonic elapsed milliseconds for events. Never fabricate unavailable metadata.
- [ ] Re-run the focused contract test and require a pass.
- [ ] Run the existing measurement-validation command used by CI and require a pass.
- [ ] Commit with message: test: record fresh-process lifecycle metadata.

### Task 2: Add fresh-process paired experiment execution modes

Files:
- Modify: RuntimeLifecycleMemoryBenchmark.kt
- Modify: RuntimeLifecycleMemoryBenchmarkJsonWriter.kt
- Modify: MainActivity.kt
- Modify: tests/runtime_lifecycle_memory_benchmark_contract_test.py

Interfaces:
- Consumes: run(resetEnabled: Boolean = true, ...).
- Produces: reset-enabled and no-reset results with identical lifecycle timing and process metadata; modes remain reset_enabled and no_reset_control.

- [ ] Add failing contract cases for identical lifecycle stages/inference parameters, fresh-process metadata in both modes, reset boundary events in reset mode, and reset_skipped/post_no_reset in control mode.
- [ ] Run the focused contract test and confirm failure.
- [ ] Preserve the existing 60s baseline, one inference, 60s post-cleanup, and 5min observation sequence. Keep inference at 4 threads and 64 max tokens. Capture process metadata once at benchmark start. Do not add in-app process termination.
- [ ] Re-run the focused contract test and require a pass.
- [ ] Run the existing Android/Local Llama contract validation and require a pass.
- [ ] Commit with message: experiment: add fresh-process reset control.

### Task 3: Add transition analysis and evidence documentation

Files:
- Modify: existing benchmark result writer or analysis component responsible for result metadata.
- Modify: docs/RUNTIME_LIFECYCLE_MEMORY_DIAGNOSTIC.md
- Modify: tests/runtime_lifecycle_memory_benchmark_contract_test.py

Interfaces:
- Consumes: samples, phase labels, wall-clock/monotonic events, reset mode, process metadata.
- Produces: machine-readable transition metadata with timestamp, phase, before/after PSS, delta, and relevant boundary-relative timing.

- [ ] Add failing assertions for transition list, timestamps, phase, before/after PSS, delta, and relative timing.
- [ ] Run the focused contract test and confirm failure.
- [ ] Detect every material PSS transition from actual samples, preserve raw samples, and calculate relative elapsed time from monotonic timestamps where available. Do not introduce a leak threshold.
- [ ] Re-run the focused contract test and require a pass.
- [ ] Document the paired protocol, metadata, transition fields, decision criteria, and the limitation around proving a fresh Android process from inside the app.
- [ ] Run measurement validation and Android CI checks.
- [ ] Commit with message: docs: define fresh-process lifecycle evidence.

### Task 4: Build, validate, and collect paired real-device evidence

Files:
- No source changes expected unless validation exposes a defect.
- Evidence outputs: benchmarks/results/runtime-lifecycle-memory-*.json

Interfaces:
- Consumes: validated APK from main and existing benchmark UI.
- Produces: at least two reset-enabled/no-reset paired evidence sets on the same device/build.

- [ ] Require Android CI, Local Llama Android Validation, AI Cloud Validation, and Measurement Validation to succeed.
- [ ] Update the existing installation without uninstalling and verify version 0.1.13.
- [ ] Collect one reset-enabled and one no-reset run per pair from freshly launched app processes; verify distinct process identities where exposed and preserve environmental telemetry.
- [ ] Compare within pairs first and classify as reset-only, both-mode, neither-mode, or mixed. Report RSS, Swap PSS, available RAM, and temperature with PSS. Do not call it a memory leak from PSS alone.
- [ ] Update the tracking issue and documentation with result filenames, process metadata, transition timing, and conclusion. Keep VersionName 0.1.13.
- [ ] Re-run applicable checks after documentation changes and require green results.

## Unresolved externally observable decisions
None. The experiment contract supplies the modes, durations, sampling interval, inference configuration, evidence location, and versioning policy.