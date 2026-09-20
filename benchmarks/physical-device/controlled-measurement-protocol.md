# Controlled Measurement Protocol

## Purpose

This protocol defines a repeatable, measurement-only method for evaluating a candidate optimizer action before automatic execution is enabled.

## Phases

1. BASELINE: collect metrics without applying the candidate action.
2. EXPERIMENT: observe or simulate the candidate action. Real execution remains disabled at the current milestone.
3. POST: collect the same metrics and compare them with baseline.

The protocol does not grant execution permission.

## Required controls

Record device model, Android API, app build, timestamp, battery, charging state, temperature, workload, candidate action ID, and sample count. Keep workload and measurement procedure as constant as practical.

## Sampling

Use at least 5 samples per phase for a short controlled experiment. Preserve every raw observation; do not keep only averages.

## Metrics

At minimum: startup latency, available RAM, CPU utilization, and temperature. Add battery percentage, swap usage, and workload-specific metrics when relevant.

## Analysis

Report sample count, mean, median, range, and variation. Compute percentage deltas only when the baseline denominator is non-zero. A lower value is not automatically better for every metric; interpret each metric according to the candidate action's documented expected effect.

Do not claim causation from a single before/after pair. Re-run when variance is high or the workload changed.

## Safety boundary

The Action Catalog and Safety Gate remain authoritative. actionExecutionAllowed remains false until a separate execution milestone is explicitly implemented and validated.

## Evidence

Preserve raw measurements, build identifiers, decision-log entries, and analysis output for every experiment.
