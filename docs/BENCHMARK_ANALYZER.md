# Benchmark Analyzer

The benchmark analyzer normalizes the two current baseline sources:

1. Internal app baseline JSON from the physical-device read-only benchmark.
2. External Termux/Shizuku/Rish baseline TXT from `scripts/rish-baseline.sh`.

It does not change device state and does not perform optimization actions.

## Termux usage

From the repository root:

```bash
python3 scripts/benchmark-analyzer.py --rish benchmarks/results/rish-baseline-YYYYMMDD-HHMMSS.txt
```

For the internal app JSON:

```bash
python3 scripts/benchmark-analyzer.py \
  --internal /path/to/baseline-YYYYMMDDHHMMSS.json \
  --rish benchmarks/results/rish-baseline-YYYYMMDD-HHMMSS.txt
```

To save normalized output:

```bash
python3 scripts/benchmark-analyzer.py \
  --rish benchmarks/results/rish-baseline-YYYYMMDD-HHMMSS.txt \
  --out benchmarks/results/baseline-analysis.json
```

## Metrics

The external analyzer extracts:

- startup `TotalTime` samples, average, minimum, maximum
- `WaitTime` samples and average
- battery percentage
- battery temperature in °C
- application PSS, RSS, and swap PSS

The internal analyzer extracts:

- sample count and elapsed duration
- available RAM average/min/max
- telemetry collection duration
- battery start/end
- temperature start/end
- cumulative process CPU time start/end/delta

## Interpretation

This analyzer produces measurements, not optimization decisions.

A later before/after analyzer should compare matched workloads and device conditions. A change should only be considered useful when the measured effect is reproducible and the action's own overhead is accounted for.

The next planned layer is:

`baseline → dry-run action → after measurement → normalized comparison → policy candidate`

No system mutation is performed by this analyzer.
