# Physical Rish Baseline — itel P661N

- Date: 2026-09-20
- Timestamp: 2026-09-20T06:37:23+07:00
- Device: ITEL itel P661N
- Android API: 33
- Charging: AC powered
- Battery: 39%
- Battery temperature: 43.1°C
- Mutation: disabled
- Measurement path: Termux + Shizuku/Rish

## Startup

| Metric | Value |
| --- | ---: |
| Samples | 5 |
| Average TotalTime | 651.4 ms |
| Median TotalTime | 643 ms |
| Minimum | 601 ms |
| Maximum | 693 ms |

WaitTime samples were 698, 635, 605, 648, and 702 ms; average 657.6 ms.

## App memory

| Metric | Value |
| --- | ---: |
| Total PSS | 55,469 KB (~54.2 MiB) |
| Total RSS | 180,388 KB (~176.2 MiB) |
| Total Swap PSS | 77 KB |
| Java Heap | 9,092 KB (~8.9 MiB) |
| Native Heap | 13,588 KB (~13.3 MiB) |
| Graphics | 4,896 KB (~4.8 MiB) |
| Private Other | 8,784 KB (~8.6 MiB) |
| System | 10,881 KB (~10.6 MiB) |

## Interpretation

This is an initial startup and memory baseline, not an optimization result. Five startup samples are sufficient to establish a repeatable first reference point but are not enough to claim a stable performance distribution.

PSS is the preferred process-memory accounting metric here because it accounts for shared pages more meaningfully than RSS. The small swap PSS value indicates that the app had little process memory attributed to swap at capture time.

The result was captured before privileged mutation was enabled. No optimization benefit or causal effect is inferred from this observation.

## Next measurement

Collect at least three like-for-like baseline observations for the same workload using the structured Rish runner. Then, only after a dry-run policy proposal is observable and safety boundaries are verified, collect matching candidate observations. Keep workload, device, charging state, and measurement method consistent enough for a meaningful comparison.