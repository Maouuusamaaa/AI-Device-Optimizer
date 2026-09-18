# Local Policy Threshold Hypothesis — ITEL P661N

Status: EXPERIMENTAL / DRY_RUN ONLY
Date: 2026-09-18
Device: ITEL itel P661N
Android API: 33

## Purpose

Define an initial memory-state classification hypothesis from the first physical idle and workload observations. These thresholds are not production optimization rules and must not trigger system-mutating actions.

## Observed reference points

- Recorded idle average available RAM: approximately 2082 MB.
- Recorded idle minimum available RAM: 2012 MB.
- Recorded workload average available RAM: 1771.09 MB.
- Recorded workload minimum available RAM: 1145 MB.
- Total RAM reported by the monitor: 5634 MB.

## Initial hypothesis

For this device profile only:

| State | Available RAM condition | Action |
|---|---:|---|
| device.normal | > 1500 MB | No mutation |
| memory_pressure | 1000–1500 MB | DRY_RUN observation only |
| memory_critical | < 1000 MB | DRY_RUN observation only |

## Rationale

1500 MB is deliberately treated as a provisional boundary because the measured workload average was 1771 MB while the same workload reached 1145 MB. The 1000 MB critical boundary is intentionally below the observed minimum so that a single workload sample does not cause an aggressive classification.

These values are hypotheses, not validated thresholds.

## Required validation

Before any action is attached:

1. Repeat the same workload on multiple runs.
2. Collect at least several independent idle observations.
3. Measure application/game performance where possible, including frame-time or FPS stability.
4. Record RAM distribution and percentiles, not only min/average/max.
5. Verify optimizer CPU and collection latency overhead.
6. Test state transitions with hysteresis to prevent rapid normal/pressure oscillation.
7. Keep all policy outcomes in DRY_RUN until repeated evidence supports an intervention.

## Safety constraint

No system-mutating action is authorized by this document. A future Action Engine must use an explicit allowlist, reversible operations where possible, permission checks, and measurable before/after validation.
