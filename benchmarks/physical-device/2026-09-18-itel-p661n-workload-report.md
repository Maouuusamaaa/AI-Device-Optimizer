# Physical Device Workload Benchmark Report — ITEL P661N

Date: 2026-09-18
Device: ITEL itel P661N
Android API: 33
Workload label: foreground_user_workload
Mode: read-only / DRY_RUN

## Dataset

The latest physical-device workload run produced 150 samples over 298.002 seconds at approximately 2-second intervals.

Measured workload results:

| Metric | Result |
|---|---:|
| Samples | 150 |
| Duration | 298.002 s |
| Available RAM average | 1771.09 MB |
| Available RAM minimum | 1145 MB |
| Available RAM maximum | 1934 MB |
| Battery | 45% → 43% |
| Temperature | 41.7°C → 40.4°C |
| Monitor collection average | 5.21 ms |
| Monitor collection minimum | 1 ms |
| Monitor collection maximum | 38 ms |
| Optimizer process CPU time | 4060 → 6146 ms |
| Optimizer CPU time delta | 2086 ms |
| Optimizer CPU time / wall time | 0.700% |

## Previously recorded idle baseline

The original 60-second physical baseline was recorded before the baseline JSON was removed from the device. Its recorded aggregate values were:

| Metric | Result |
|---|---:|
| Samples | 30 |
| Duration | ~58 s |
| Available RAM average | ~2082 MB |
| Available RAM minimum | 2012 MB |
| Available RAM maximum | 2155 MB |
| Battery | 63% → 62% |
| Temperature | 36.0°C → 35.8°C |
| Monitor collection average | 5.17 ms |
| Optimizer process CPU time | 2918 → 3906 ms |
| Optimizer CPU time delta | 988 ms |
| Optimizer CPU time / wall time | ~1.70% |

The raw idle JSON is no longer present on the device, so the idle figures above are retained as recorded aggregate values rather than re-derived from the original file.

## Interpretation

The workload run had substantially less available RAM than the recorded idle baseline: approximately 1771 MB versus 2082 MB average, with a workload minimum of 1145 MB. This demonstrates a real change in device state under workload, but it does not establish causality or prove that the optimizer caused or improved any performance change.

Monitor collection cost remained close to the recorded idle value: approximately 5.21 ms during workload versus 5.17 ms during the earlier baseline. The maximum workload collection time was 38 ms, so percentile-based latency analysis should be added before treating average collection time as sufficient.

The optimizer CPU-time ratio was approximately 0.700% of benchmark wall time during the workload. This is process CPU time for the optimizer relative to benchmark wall time, not total device CPU utilization.

Battery percentage is coarse-grained and the two runs were performed at different starting charge levels and different device conditions. Battery percentage deltas therefore must not be used as a controlled energy-efficiency comparison.

Temperature also cannot be compared causally because the runs were performed under different workloads and device conditions.

## Measurement limitations

This is an observational workload dataset, not a controlled A/B experiment. The idle and workload runs differ in time, device state, battery level, foreground activity, and thermal state.

The current data is sufficient to inform the next dry-run policy experiment, but not sufficient to claim an optimization benefit.

## Next experiment

The next stage is to validate memory-state thresholds with additional physical-device observations while keeping the optimizer in DRY_RUN/read-only mode.

Candidate policy states should initially be treated as hypotheses:

- device.normal: available RAM comfortably above observed pressure region.
- memory_pressure: available RAM approaches the lower observed workload range.
- memory_critical: reserved for substantially stronger evidence after repeated measurements.

No system-mutating action should be attached to these states yet. Thresholds must be validated across repeated workloads and must account for device-specific total RAM and measurement variance.

## Provenance

Raw workload dataset retained locally at:
~/storage/downloads/ml-workload.json

The Android benchmark source writes workload observations to the application's external-files benchmark directory.
