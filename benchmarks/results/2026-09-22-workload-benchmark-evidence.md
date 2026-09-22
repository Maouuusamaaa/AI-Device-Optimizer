# 2026-09-22 — 5-minute workload benchmark evidence

## Observation

Physical device:
- Manufacturer: ITEL
- Model: itel P661N
- Android API: 33
- Workload file: `workload-foreground_user_workload-1790071333652.json`
- Samples: 145
- UI-reported duration: 298 s
- Battery: 25% → 23%
- Temperature: 37.6°C → 38.5°C
- Average available RAM: 1880 MB
- Available RAM range: 1771–1940 MB
- Average telemetry collection: 2069.80 ms

The supplied terminal capture is 106,984 bytes and contains 3,996 lines. Its beginning is truncated inside the first JSON object, so it is not treated as a standalone parseable JSON artifact. The benchmark filename and UI provide the start timestamp, while the capture contains the later timestamps and final timestamp.

## Timing verification

The workload start timestamp is the timestamp encoded in the benchmark filename:
`1790071333652`.

The final timestamp visible in the supplied capture is:
`1790071632487`.

Elapsed timestamp span:
`298,835 ms` = `298.835 s`.

With 145 samples there are 144 inter-sample intervals, giving an overall mean interval of approximately:
`298,835 / 144 = 2,075.24 ms`.

This is consistent with the intended approximately-2-second sampling cadence while allowing collection time to consume part of the interval.

The capture contains examples where collection itself exceeds 2 seconds. The scheduler introduced by PR #63 is designed not to add another full 2-second sleep after an overlong collection.

## Baseline comparison

Reference read-only baseline:
- Samples: 30
- Timestamp span: 61.257 s
- Average available RAM: 1908 MB
- RAM range: 1849–1931 MB
- Average collection: 2008.17 ms
- Battery: 27% → 27%
- Temperature: 37.7°C → 37.5°C

Workload observation:
- Samples: 145
- Timestamp span: 298.835 s
- Average available RAM: 1880 MB
- RAM range: 1771–1940 MB
- Average collection: 2069.80 ms
- Battery: 25% → 23%
- Temperature: 37.6°C → 38.5°C

Descriptive differences:
- Average available RAM: -28 MB, approximately -1.47% relative to the baseline average.
- Average collection duration: +61.63 ms.
- Workload battery change: -2 percentage points.
- Workload temperature change: +0.9°C.

These are observations, not causal effects. The two measurements have different durations and workloads, so they should not be interpreted as a performance score or an optimization verdict.

## Process memory observation

The supplied workload capture shows the optimizer process PSS around 60,086 KiB during later workload samples, while the earlier read-only baseline showed approximately 9,967 KiB.

This is an observation of different workload states. It is not sufficient to classify the increase as a memory leak. A repeated workload followed by a post-workload recovery measurement is required to test whether memory remains elevated.

## Status

Classification: **Confirmed — workload benchmark completed and timing is consistent with the corrected sampling scheduler.**

The raw JSON should be preserved from the device separately when available. This evidence document intentionally records only measurements that can be supported by the supplied capture and UI.
