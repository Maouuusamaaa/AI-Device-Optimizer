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
- android/app/src/main/ — current Android monitor and local benchmark implementation
- android/app/src/test/ — JVM unit tests for the current read-only foundation
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

## Benchmark Targets

Every optimization should be evaluated against measurable device behavior, including:

- RAM usage
- CPU utilization
- battery drain
- device temperature
- application launch time
- game/application FPS where measurable
- frame-time stability
- network latency where relevant
- storage usage
- optimizer CPU/RAM overhead

Initial engineering target for the lightweight local agent: remain below roughly 100 MB average RAM usage and remain near-idle when no optimization work is required. These are engineering targets, not guarantees; real-device benchmarks determine whether they are achieved.

## Safety Model

The action engine must be capability-based and policy-controlled.

Monitoring should be available independently from mutation. Actions should be categorized by risk and require explicit policy authorization. High-impact system changes must not be triggered merely because an AI model suggested them.

The optimizer should fail safely when device state, permissions, or expected outcomes are uncertain.

## Development Status

Current milestone: physical-device baseline benchmark.

Completed:

- read-only Android Monitor layer
- repeatable monitor benchmark foundation
- read-only physical baseline runner
- Local Policy Engine
- Android unit tests for the current monitor and policy foundation
- GitHub Actions build/test workflow configuration
- successful post-fix GitHub Actions build/test verification
- debug APK installed and running on a physical Android API 33 device

Current baseline runner:

- 30 telemetry samples
- 2-second sampling interval
- roughly 60 seconds per run
- JSON result saved under the app external-files benchmark directory
- no Shizuku or privileged mutation

The dry-run Action Engine and connected optimization pipeline described by the target architecture are not present in this checkout and remain future work.

Next milestone: run the physical baseline, retain the result, then add independent startup/performance measurements before enabling privileged mutation.

## Research Direction

The project will use Android platform documentation, real-device measurements, and relevant open-source implementations as references. Existing projects are treated as research references rather than code to copy blindly.

Relevant areas include Android performance and health telemetry, on-device AI, controlled Android automation, and local/cloud hybrid inference.

## License

License will be selected before the first distributable release.
