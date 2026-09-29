# Finding GB-0001 — Rootless Game Boost capability baseline

Status: VERIFIED-RESEARCH
Date: 2026-09-29
Scope: Android 13 / itel P661N target; non-destructive, no root, no system partition modification.

## Question

Which Game Boost capabilities are technically plausible for AI Device Optimizer without assuming OEM-private privileges or claiming unverified performance gains?

## Evidence

### E1 — Android platform

Android documents Game Mode, ADPF, thermal APIs, CPU performance hints, and game-state mechanisms as the platform's supported performance/thermal optimization surface. Game Mode is available on Android 13+; Android 12 support is device-dependent. The platform also documents frame timing and jank metrics as performance evidence.

Source:
- https://developer.android.com/games/optimize/overview
- https://developer.android.com/games/codelab/adaptability-codelab
- https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics-views

Classification: Primary authoritative source.

### E2 — PerfOverlay

PerfOverlay documents rootless/overlay monitoring for FPS, frame time, CPU, GPU, temperatures, RAM and network, plus recording/comparison and Shizuku integration. Its README identifies concrete platform data sources such as /proc/stat, thermal sysfs, ActivityManager.MemoryInfo and TrafficStats.

Source:
- https://github.com/artos-n/PerfOverlay

Classification: Open-source implementation evidence; feature availability on P661N remains unverified.

### E3 — GameCore

GameCore documents per-game profiles, overlay/HUD features, thermal auto-downshift, network checks, refresh-rate/brightness/orientation controls, and an explicit Unavailable state when Android does not expose a requested value.

Source:
- https://github.com/Dreamucxe/GameCore

Classification: Open-source implementation evidence; device/OEM applicability remains unverified.

### E4 — FrameX-Android

FrameX documents rootless monitoring, thermal diagnostics, FPS measurement, Shizuku-assisted operations, rollback/snapshots, and vendor-specific branches. Its documentation explicitly states that sensor availability varies by device and separates generic Android behavior from Vivo/iQOO-specific behavior.

Source:
- https://github.com/MaheshSharan/FrameX-Android
- https://github.com/MaheshSharan/FrameX-Android/blob/main/KNOWN_LIMITATIONS.md

Classification: Open-source implementation evidence; vendor-specific features must not be assumed on itel P661N.

### E5 — GameOptimizer

GameOptimizer documents no-root performance profiles, PowerManager CPU/GPU hints, overlay telemetry, thermal monitoring, and explicit no-root limitations such as no direct GPU-frequency control and monitoring-only thermal control.

Source:
- https://github.com/AbyGya/GameOptimizer

Classification: Open-source implementation evidence; implementation quality and P661N applicability require independent validation.

## Findings

1. Monitoring is a safer first implementation target than aggressive system tuning.
2. A truthful telemetry model must support AVAILABLE/UNAVAILABLE/FAILED rather than substituting guessed values.
3. FPS/frame-time evidence should distinguish display refresh rate from actual game frame production.
4. Thermal state should be treated as a guard/measurement input, not as permission to override thermal protection.
5. Vendor-specific controls must be isolated behind capability detection and must default to unavailable.
6. Shizuku can provide privileged shell capabilities, but the optimizer must still enforce its own allowlist, Safety Gate, rollback and evidence rules.
7. No external project's code or resources should be copied unless its license and attribution requirements are satisfied. Independent reimplementation of documented concepts is preferred where practical.
8. No performance-gain percentage is established by this finding. P661N-specific measurements are required.

## Candidate capability matrix

| Capability | Initial disposition | Evidence required on P661N |
|---|---|---|
| Game library/profile | Candidate | Foreground/game detection validation |
| CPU/RAM telemetry | Candidate | Sensor/source availability and overhead |
| Thermal telemetry | Candidate | thermalservice/sysfs mapping |
| Network latency/throughput telemetry | Candidate | measurement validity and overhead |
| FPS/frame-time telemetry | Candidate | SurfaceFlinger/other viable source and game coverage |
| Session recording | Candidate | storage format, sampling overhead |
| Benchmark/A-B comparison | Candidate | preregistered workload and metrics |
| Refresh-rate profile | Investigate | supported modes + safe reversible control |
| Brightness/orientation profile | Investigate | public API/permission behavior |
| CPU performance hints | Investigate | Android API behavior on P661N |
| GPU frequency control | Not assumed | direct control evidence required |
| Thermal override | Exclude from initial scope | conflicts with safety posture |
| OEM-private Game Mode commands | Not assumed | documented/authorized interface required |

## Conclusion

The first Game Boost milestone should focus on observability, profiles, capability detection, reversible user-visible controls, and benchmark evidence. It should not begin by reproducing vendor-private booster internals or by promising performance improvements.

This finding is a research baseline, not an implementation approval.
