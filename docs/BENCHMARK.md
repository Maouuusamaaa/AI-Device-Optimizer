# Benchmark Milestone

This milestone establishes a read-only physical-device baseline.

The Android benchmark runner collects repeated telemetry samples without changing device state. The current baseline uses 30 samples at 2-second intervals, for roughly 60 seconds.

Each sample records:

- available and total RAM
- battery percentage and charging state
- battery-reported temperature when available
- app-private storage availability
- optimizer process elapsed CPU time
- monitor collection duration

The benchmark writes a JSON result using benchmarks/schema.json.

## Canonical workload matrix

The repository now defines four canonical workload scenarios in benchmarks/workloads.json:

| Scenario | Duration | Purpose |
| --- | ---: | --- |
| idle | 60 s | Establish background/launcher behavior |
| normal_use | 120 s | Represent repeatable everyday application use |
| sustained_workload | 300 s | Measure behavior under sustained device activity |
| gaming | 300 s | Measure application/game performance under a fixed manual test |

The workload file is a measurement contract, not an optimization policy. It describes how evidence should be collected and compared; it does not authorize mutation.

For gaming, the test should be performed manually with the same game, mode, graphics settings, and test sequence. Gameplay is not automated by the benchmark.

## Baseline procedure

1. Install the CI-built debug APK on the physical device.
2. Open AI Device Optimizer.
3. Run the app's 60-second read-only baseline and retain its JSON result.
4. From Termux, use the repository script scripts/rish-baseline.sh to collect an independent external baseline through Shizuku/Rish.
5. Do not enable optimization mutation; the external runner only reads telemetry and measures app startup/memory.
6. Record both outputs and compare them later in the benchmark analyzer.
7. Repeat under the canonical workloads: idle, normal application use, sustained workload, and gaming.
8. Keep device model, Android version, app version, benchmark version, battery state, and workload conditions recorded for each run.
9. Keep repeated observations separate by workload. Do not average idle, normal-use, sustained-load, and gaming runs into one baseline.

The Termux/Shizuku runner is now the primary mobile-side external benchmark path. The scripts/adb-baseline.sh runner is retained as an optional PC-side equivalent for sessions where a desktop is available.

The benchmark is evidence collection only. It does not kill applications, change settings, modify CPU/GPU configuration, alter network behavior, or execute optimization actions.

For application startup and UI performance, Android Macrobenchmark is the appropriate next measurement layer. It runs outside the target app process and is intended for repeated end-user interaction measurements on physical devices.

## Interpretation

Never treat a single run as proof of an optimization. Compare repeated runs against a documented baseline, and keep device state, workload, battery state, and software version recorded.

Benchmark results are evidence for policy decisions, not proof of causation. A before/after difference should be reproduced under comparable conditions before it is used to justify an optimization policy.

## Safety boundary

Monitoring and benchmarking remain independent from mutation. No privileged action is enabled by this milestone.
