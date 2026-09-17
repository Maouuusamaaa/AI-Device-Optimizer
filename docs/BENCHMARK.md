# Benchmark Milestone

This milestone establishes a read-only benchmark foundation.

The Monitor layer provides device telemetry. MonitorBenchmark measures the duration of one telemetry collection. This is a measurement of the monitor call itself, not a complete measurement of system-wide optimizer overhead.

For app startup and UI performance, use Android Macrobenchmark. It runs outside the target app process and can produce JSON results and system traces.

## Baseline procedure

For each workload, collect multiple iterations and record device model, Android API, build/version, battery and charging state, workload, available RAM, monitor duration, and Macrobenchmark results when applicable.

Use a physical device for representative end-user measurements.

## Safety boundary

This milestone performs no optimization and no privileged mutation. It must not kill applications, change settings, modify CPU/GPU configuration, or alter network behavior.

## Interpretation

Benchmark results are evidence for later policy evaluation, not automatic policy decisions.
