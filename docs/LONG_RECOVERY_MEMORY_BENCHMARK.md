# Long-recovery memory benchmark

This protocol is the next controlled experiment after the repeated workload → recovery observation.

## Protocol

- baseline: 60 seconds
- workload: 5 minutes of normal foreground use
- recovery: 10 minutes after the workload stops
- telemetry interval: 2 seconds
- cycles: 1
- monitoring is read-only; no system mutation or optimization action is performed

The benchmark writes:

`long-recovery-memory-<timestamp>.json`

with protocol:

`long_recovery_memory_observation`

and queues the evidence through the normal GitHub Evidence Sync pipeline.

## Purpose

The earlier repeated protocol showed a large PSS increase during workload and only a small decline during the 2-minute recovery phase. A longer recovery window is intended to measure whether PSS:

1. returns toward baseline over time, which is compatible with transient retention;
2. remains elevated for the full recovery window, which strengthens the observation of persistent retention; or
3. continues changing after the workload stops, which provides a more informative decay curve for later diagnosis.

This experiment does not by itself diagnose a memory leak. PSS can remain elevated because of allocator retention, native caches, model/runtime mappings, or other lifecycle behavior.

## Interpretation

Compare the baseline PSS with the end of the 10-minute recovery and inspect the complete recovery time series. Do not infer a leak from a single endpoint. A repeated experiment under the same protocol is required before attributing the behavior to a specific mechanism.

Temperature, available RAM, battery, collection duration, and process RSS/swap PSS are retained in the evidence so that thermal or system-state changes can be considered alongside PSS.

## Physical-device procedure

1. Keep the phone in a normal idle state before starting.
2. Press **Run 10min recovery memory benchmark**.
3. During the 5-minute workload phase, use a normal foreground app/game consistently.
4. When the workload phase ends, stop using the workload and return to AI Device Optimizer.
5. Leave the device alone for the complete 10-minute recovery phase.
6. Do not press other benchmark controls during the run.
7. After completion, verify the JSON filename and GitHub Evidence Sync status.

The total nominal protocol duration is approximately 16 minutes plus sampling overhead.
