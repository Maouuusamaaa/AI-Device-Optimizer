# Benchmark Protocol

The benchmark layer measures the optimizer before automatic actions are enabled.

## Goals

Record repeatable baseline data for RAM availability, battery state, monitor sampling duration, app startup behavior, and optimizer overhead.

The first milestone is read-only. Benchmark code must not change device state.

## Workloads

Run repeated measurements under idle, normal application use, sustained workload, and gaming workload. Record device conditions, workload, build, battery state, and iteration count.

## Android Macrobenchmark

Android Macrobenchmark is intended for larger end-user interactions such as app startup and UI interactions. It can produce JSON results and system traces. Representative measurements should use a physical device.

## Interpretation

Never treat a single run as proof of an optimization. Compare repeated runs against a documented baseline. The current physical baseline consists of three independent Rish observations on the same itel P661N/API 33 device; see `benchmarks/physical-device/2026-09-20-itel-p661n-rish-protocol.md` for the candidate protocol.


## Physical Rish baseline status

The first three-observation physical baseline is complete. The aggregated reference is stored at `benchmarks/results/rish-baseline-aggregate.json`.

Cross-run descriptive reference:

- Startup average across run averages: 671.93 ms
- Median run-average startup: 662 ms
- Run-average startup range: 651.4–702.4 ms
- WaitTime average across run averages: 678.6 ms
- PSS average: 55,528.7 KB
- RSS average: 180,134.7 KB
- Swap PSS average: 82 KB

These values are descriptive only. Battery and thermal conditions varied between observations, so they must not be interpreted as causal optimization effects or guaranteed performance targets.
