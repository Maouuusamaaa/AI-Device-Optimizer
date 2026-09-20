# Physical Baseline and Candidate Protocol

## Status

The first physical baseline set is complete for the itel P661N running Android API 33.

Baseline source artifacts:

- benchmarks/results/rish-baseline-20260920-063723.txt
- benchmarks/results/rish-baseline-20260920-064229.txt
- benchmarks/results/rish-baseline-20260920-065355.txt
- benchmarks/results/rish-baseline-aggregate.json

All three observations used the same device and the same external Rish startup/memory workload. No privileged mutation was enabled.

## Aggregated baseline

| Metric | Cross-run result |
| --- | ---: |
| Observations | 3 |
| Startup average across run averages | 671.93 ms |
| Median run-average startup | 662 ms |
| Run-average startup range | 651.4–702.4 ms |
| WaitTime average across run averages | 678.6 ms |
| PSS average | 55,528.7 KB |
| RSS average | 180,134.7 KB |
| Swap PSS average | 82 KB |
| Temperature average | 43.2°C |
| Battery average at capture | 44.7% |

The three run-average startup values were 651.4 ms, 702.4 ms, and 662.0 ms. The PSS values were 55,469 KB, 55,521 KB, and 55,596 KB.

## Interpretation

This is a descriptive baseline, not an optimization result.

The device identity is consistent across all three observations. Startup has materially more run-to-run variation than PSS. The capture conditions were not fully controlled: battery ranged from 39% to 52% and temperature ranged from 42.8°C to 43.7°C. Foreground/system state can also affect Android launch measurements.

The baseline therefore defines a reference distribution and expected measurement noise. It does not define a guaranteed performance target, and it does not establish causation.

## Candidate protocol

Candidate measurements must use the same:

1. physical device and Android API;
2. Rish measurement path;
3. application package and launch activity;
4. startup workload and five-sample structure per observation;
5. charging/power condition where practical;
6. benchmark timing procedure;
7. memory capture procedure.

For a candidate experiment:

- Keep mutation disabled until the policy proposal and safety checks have been reviewed.
- Capture at least three candidate observations using the same protocol.
- Prefer baseline and candidate captures in close temporal proximity under comparable thermal and power conditions.
- Preserve every raw TXT result.
- Generate structured JSON and CSV artifacts for every observation.
- Aggregate baseline and candidate observations separately.
- Report both central tendency and variation.
- Do not declare an optimization successful from one metric alone.
- Do not treat a lower startup time or lower memory value as causal evidence unless the comparison controls for relevant workload and device-state differences.

## Decision rule for the next phase

The next optimization experiment should be measurement-only first. A dry-run policy may propose an action, but the Action Engine must not apply a privileged mutation merely because the candidate appears better.

A candidate can proceed to further investigation only when its measurements are reproducible enough to distinguish the observed change from ordinary benchmark variation. The project should then evaluate side effects using CPU, battery, temperature, memory, and stability measurements rather than optimizing startup time in isolation.

## Safety boundary

This protocol does not authorize any system mutation. The current Rish benchmark tooling remains read-only. Cloud or local AI suggestions must not bypass the local safety and permission model.
