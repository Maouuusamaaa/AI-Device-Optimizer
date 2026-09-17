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

Never treat a single run as proof of an optimization. Compare repeated runs against a documented baseline.
