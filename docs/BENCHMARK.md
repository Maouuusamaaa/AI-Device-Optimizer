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

## Baseline procedure

1. Install the CI-built debug APK on the physical device.
2. Open AI Device Optimizer.
3. Do not grant Shizuku or other privileged permissions.
4. Start Run 60s read-only baseline.
5. Leave the device in the current state for the full measurement.
6. Record the displayed summary and retain the generated JSON file.
7. Repeat under documented workloads later: idle, normal application use, sustained workload, and gaming.

The benchmark is evidence collection only. It does not kill applications, change settings, modify CPU/GPU configuration, alter network behavior, or execute optimization actions.

For application startup and UI performance, Android Macrobenchmark is the appropriate next measurement layer. It runs outside the target app process and is intended for repeated end-user interaction measurements on physical devices.

## Interpretation

Never treat a single run as proof of an optimization. Compare repeated runs against a documented baseline, and keep device state, workload, battery state, and software version recorded.

## Safety boundary

Monitoring and benchmarking remain independent from mutation. No privileged action is enabled by this milestone.
