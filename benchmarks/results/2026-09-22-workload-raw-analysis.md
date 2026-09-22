# 2026-09-22 — Raw workload JSON analysis

Source:
- Raw artifact: `benchmarks/results/workload-foreground_user_workload-1790071333652.json`
- Git commit containing raw artifact: `fc5fd30`
- Device: ITEL itel P661N, Android API 33
- Schema version: 2

## Parsed measurements

The raw JSON contains 145 samples over a timestamp span of 298,835 ms (298.835 s), producing 144 inter-sample intervals.

| Metric | Observation |
|---|---:|
| Samples | 145 |
| Timestamp span | 298.835 s |
| Mean inter-sample interval | 2075.24 ms |
| Minimum interval | 1731 ms |
| Maximum interval | 3370 ms |
| Average available RAM | 1879.51 MB |
| Available RAM range | 1771–1940 MB |
| Average collection duration | 2069.80 ms |
| Collection duration range | 1732–3368 ms |
| Collections > 2000 ms | 60 / 145 |
| Collections > 2500 ms | 21 / 145 |
| Battery | 25% → 23% |
| Temperature | 37.6°C → 38.5°C |
| Process CPU time | 8241 → 18111 ms |
| Storage available | 33304 → 33302 MB |

The interval distribution is consistent with the corrected scheduler: collection time is part of the sampling interval, rather than being followed by an additional fixed two-second sleep.

## Process PSS observation

The optimizer process PSS was 9,967 KiB at the beginning of the workload and remained at that value through the first 16 samples.

At sample index 16, timestamp 1790071366105, PSS changed from 9,967 KiB to 60,086 KiB. At that same sample:
- available RAM was 1888 MB;
- collection duration was 2765 ms.

PSS then remained at 60,086 KiB through the remainder of the recorded workload. The raw artifact therefore shows a persistent elevated PSS state within this single run.

This does not establish a memory leak. The measurement only shows that the process entered a higher-PSS state during the workload and did not return to the earlier value before the workload ended. A leak requires evidence of retention across repeated workload/recovery cycles.

## Comparison with the existing read-only baseline

The repository's existing evidence records the read-only baseline as:
- 30 samples;
- 61.257 s;
- average available RAM 1908 MB;
- RAM range 1849–1931 MB;
- average collection duration 2008.17 ms;
- battery 27% → 27%;
- temperature 37.7°C → 37.5°C.

The workload raw JSON gives:
- 145 samples;
- 298.835 s;
- average available RAM 1879.51 MB;
- RAM range 1771–1940 MB;
- average collection duration 2069.80 ms;
- battery 25% → 23%;
- temperature 37.6°C → 38.5°C.

Descriptive differences are approximately:
- available RAM average: -28.49 MB;
- collection duration average: +61.63 ms;
- battery: -2 percentage points;
- temperature: +0.9°C.

These measurements are not a controlled causal comparison because duration, workload state, battery state, and thermal state differ.

## Decision from this evidence

The corrected sampling scheduler should not be changed based on this workload result. The timing behavior is internally consistent with its intended design.

The PSS transition is the next observation that requires controlled follow-up. The next experiment should specifically test recovery rather than changing optimizer code:

1. Start from an idle, foreground state and record a short baseline.
2. Run the same workload under the same benchmark conditions.
3. Immediately after workload completion, continue read-only monitoring for a recovery window.
4. Record process PSS, available RAM, CPU time, temperature, battery, and collection duration.
5. Repeat the workload/recovery sequence enough times to determine whether the elevated PSS returns toward the pre-workload level or is retained across runs.
6. Do not classify the PSS increase as a leak until recovery/repetition data supports retention.

Safety posture remains unchanged: the evidence benchmark is observational and does not authorize device mutation.
