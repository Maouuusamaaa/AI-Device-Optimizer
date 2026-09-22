# Workload Recovery Benchmark

## Purpose

This read-only physical-device experiment follows up the PSS increase observed in the 2026-09-22 five-minute workload benchmark.
It does not diagnose or mutate the device. It observes whether the optimizer process PSS returns toward the pre-workload state after the workload ends.

## Protocol

1. Baseline: 60 seconds.
2. Workload: 5 minutes.
3. Recovery: 2 minutes.

Sampling target is approximately every 2 seconds. Collection time is part of the interval; the scheduler does not append a fixed sleep after a slow collection.

During the workload phase, switch to the normal foreground app/game and use it normally. When the workload phase ends, stop the workload and return to the optimizer app if practical. The recovery phase starts automatically and remains read-only.

## Interpretation

A higher PSS during workload is an observation, not a memory-leak diagnosis.
Evidence for retention becomes stronger if repeated workload/recovery runs show that PSS remains elevated after recovery or accumulates across runs. A single run is insufficient.

## Output

The app writes a raw JSON artifact to its benchmark directory and attempts a copy to:

/storage/emulated/0/Download/AI-Device-Optimizer/benchmarks/

File prefix: workload-recovery-<timestamp>.json

The JSON labels every sample as baseline, workload, or recovery and preserves process PSS, available RAM, CPU time, battery, temperature, storage and collection duration.