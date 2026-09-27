# AI Device Optimizer

An Android-first adaptive device optimization system designed to keep a device operating efficiently according to its current workload and state.

## Project Goal

The optimizer continuously observes device conditions, evaluates them against controlled policies, and applies only validated actions when they are expected to improve measurable outcomes.

The project uses a hybrid architecture:

- Local Android agent for monitoring, fast decisions, offline operation, and controlled execution.
- Cloud AI for heavier reasoning, policy generation, analysis, and longer-term learning.
- Offline-first fallback so core monitoring and safe local policies continue without cloud connectivity.

## Core Development Flow

Monitor → Local Policy → Action Engine → Benchmark → Cloud API → Cloud AI → Offline Fallback → Adaptive Learning

## Architecture

- android/app/ — Android application
- android/app/src/main/ — current Android monitor, dry-run policy pipeline, and local benchmark implementation
- android/app/src/test/ — JVM unit tests for the current read-only foundation and monitor-to-policy flow
- cloud/api/ — cloud service interface
- cloud/optimizer/ — server-side policy reasoning
- cloud/models/ — model adapters and inference logic
- cloud/database/ — historical telemetry and policy data
- policies/ — versioned optimization policies
- tests/ — unit, integration, and safety tests
- benchmarks/ — reproducible before/after measurements
- docs/ — architecture, permissions, safety, and development documentation
- scripts/ — development and benchmarking utilities

## Design Principles

1. Measure before optimizing.
2. Prefer reversible actions.
3. Use an explicit action allowlist.
4. Apply the minimum permissions required.
5. Never assume an optimization is beneficial without device measurements.
6. Keep the local agent lightweight while idle.
7. Preserve normal Android system behavior unless a policy has evidence that an intervention is useful.
8. Record decisions and outcomes so policies can be evaluated.
9. Cloud reasoning must never bypass local safety and permission constraints.
10. Offline operation must remain useful without requiring cloud connectivity.

## Versioning and Release Milestones

The project separates ongoing engineering work from validated release milestones.

A commit, experiment, diagnostic, benchmark fix, documentation change, or CI repair does not automatically require a new `versionName`. The exact implementation state is tracked by Git commit and PR, while `versionName` identifies a milestone that has been explicitly accepted.

Android `versionCode` remains monotonic for installable updates because Android uses it to determine update ordering. It is not used as an experiment counter.

See [docs/VERSIONING.md](docs/VERSIONING.md) for the complete policy.

## Benchmark Targets

Every optimization should be evaluated against measurable device behavior, including RAM usage, CPU utilization, battery drain, device temperature, application launch time, FPS where measurable, frame-time stability, network latency where relevant, storage usage, and optimizer CPU/RAM overhead.

Initial engineering target for the lightweight local agent: remain below roughly 100 MB average RAM usage and remain near-idle when no optimization work is required. These are engineering targets, not guarantees; real-device benchmarks determine whether they are achieved.

## Safety Model

The action engine must be capability-based and policy-controlled. Monitoring should be available independently from mutation. Actions should be categorized by risk and require explicit policy authorization. High-impact system changes must not be triggered merely because an AI model suggested them. The optimizer should fail safely when device state, permissions, or expected outcomes are uncertain.

## Development Status

Current milestone: 0.1.16 — Offline Evaluation + Persistent Outcome History.

Completed through 0.1.14:

- Stage 1–8 local AI/runtime foundation, including Qwen3 0.6B GGUF Q4_0 and Android JNI integration
- Stage 9 real-device local inference benchmark
- Stage 9.1 methodology hardening, including profileable release benchmarking and controlled thread scheduling
- persistent benchmark/evidence synchronization to GitHub with integrity checks and retry handling
- controlled runtime lifecycle memory diagnostic separating post-cleanup observation from the explicit native reset boundary
- no-reset control arm and fresh-process paired lifecycle protocol
- two independent fresh-process reset/no-reset pairs on the itel P661N / Android API 33
- final lifecycle classification: MIXED; lifecycle-dependent PSS transitions are reproducible, but reset-specific causality is not established
- explicit non-diagnosis of memory leak from PSS alone
- lifecycle investigation closed without production runtime changes or a version bump
- deterministic evidence contract validation for schema versions 1 and 2
- comparable device/workload pairing with fail-closed insufficient-evidence handling
- deterministic NO_REGRESSION / REGRESSION / MIXED classification
- canonical SHA-256 provenance and append-only analysis history
- Measurement Validation CI coverage for the hardened evidence layer
- validation of existing itel P661N / Android API 33 benchmark evidence

Completed in 0.1.15:

- Cloud Advisor v1 advisory-only response contract
- structural validation that rejects malformed, unsupported, and authorization-bearing cloud output
- local immutable Cloud Policy Candidate validation
- integration with the existing ActionCatalog rather than introducing a second action authority
- deterministic adaptation of validated cloud candidates into the existing PolicySimulator
- explicit DRY_RUN-only simulation with execution permanently disabled
- fail-closed insufficient-evidence behavior when referenced evidence is unavailable locally
- preservation of the existing DryRunSafetyGate as the local authority
- provenance linkage through evidenceId, advisorVersion, and policyId
- Cloud Advisor safety/contract CI coverage
- Android unit-test coverage and release/native APK validation
- no new privileged operation, new mutation capability, adaptive learning, or Cloud-to-Action bypass

Cloud Advisor output remains untrusted candidate data. This milestone establishes the advisory and local-simulation boundary; it does not grant cloud output execution authority.

The primary mobile development path remains Termux + Shizuku/Rish on the physical Android device, with PC + ADB as an optional fallback.

Completed in 0.1.16:

- deterministic offline replay of paired measurement evidence using the existing evidence measurement classifier
- versioned offline evaluation dataset contract with unique case IDs
- deterministic dataset fingerprints and evaluation IDs
- preservation of INVALID_EVIDENCE and INSUFFICIENT_EVIDENCE classifications
- per-case evidence provenance in evaluation reports
- append-only JSONL evaluation outcome history with duplicate suppression and fail-closed malformed-history handling
- end-to-end offline evaluation fixture validation in Measurement Validation CI
- no policy selection, action ranking, causal effectiveness claim, or device mutation introduced

Offline evaluation remains descriptive. Its outcome history is prepared as a future input boundary for Adaptive Learning, not as an authorization mechanism.

## Research Direction

The project will use Android platform documentation, real-device measurements, and relevant open-source implementations as references. Existing projects are treated as research references rather than code to copy blindly.

Relevant areas include Android performance and health telemetry, on-device AI, controlled Android automation, and local/cloud hybrid inference.

## License

License will be selected before the first distributable release.
