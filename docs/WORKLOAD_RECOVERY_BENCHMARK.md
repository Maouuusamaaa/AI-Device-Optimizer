# Repeated Workload → Recovery Memory Benchmark

## Purpose

This read-only physical-device experiment extends the original workload → recovery protocol after the first 0.1.9 run showed optimizer PSS rising from 10,170 KiB to 58,094 KiB and remaining at 58,094 KiB throughout the two-minute recovery observation.

The repeated protocol does not diagnose or mutate the device. It tests whether an elevated PSS level persists across multiple workload/recovery cycles and whether the post-recovery level accumulates.

## Protocol

1. Baseline: 60 seconds.
2. Cycle 1 workload: 5 minutes.
3. Cycle 1 recovery: 2 minutes.
4. Cycle 2 workload: 5 minutes.
5. Cycle 2 recovery: 2 minutes.

A five-second switch delay is inserted before each workload phase. Sampling targets approximately every 2 seconds and remains collection-aware: collection time is part of the interval rather than being followed by a fixed sleep.

During each workload phase, switch to the normal foreground app/game and use it normally. When the workload phase ends, stop the workload and return to the optimizer app if practical. The recovery phase remains read-only.

## Interpretation

This protocol distinguishes several observations:

- If recovery 1 and recovery 2 both remain near the same elevated PSS, that supports persistent retention across repeated cycles.
- If recovery 2 is higher than recovery 1 after comparable workloads, that is evidence of accumulation across cycles.
- If PSS decreases substantially during recovery or returns toward baseline, the earlier elevation may be transient retention rather than persistent growth.

These are descriptive observations, not a memory-leak diagnosis. A leak conclusion requires additional isolation and repeated evidence.

## Output

The app writes a raw JSON artifact to its benchmark directory and attempts a copy to:

/storage/emulated/0/Download/AI-Device-Optimizer/benchmarks/

File prefix:

repeated-workload-recovery-<timestamp>.json

Schema version 2 records cycle for every sample and preserves process PSS, RSS, SwapPSS, available RAM, CPU time, battery, temperature, storage and collection duration.
