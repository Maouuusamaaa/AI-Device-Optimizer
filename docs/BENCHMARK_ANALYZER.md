# Benchmark Analyzer

The benchmark analyzer normalizes internal app baseline observations and external Termux/Shizuku/Rish measurements. It does not change device state and does not perform optimization actions.

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

## Baseline vs workload

For two valid internal JSON observations:

```bash
python3 scripts/compare-internal-benchmarks.py \
  /path/to/read-only-baseline.json \
  /path/to/workload.json \
  --out benchmarks/results/baseline-vs-workload.json
```

The comparison reports descriptive deltas for duration, sample count, available RAM, telemetry collection time, battery, temperature, and optimizer-process CPU time. It does not rank or recommend optimization actions.

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
- process PSS/RSS/swap PSS summaries when process telemetry is available

## Interpretation

These tools produce measurements, not optimization decisions.

Baseline and workload observations should be matched by workload and device conditions before causal conclusions are made. A change should only be considered useful when the measured effect is reproducible and the action's own overhead is accounted for.

The current 2026-09-22 workload evidence is recorded in `benchmarks/results/2026-09-22-workload-benchmark-evidence.md`.

No system mutation is performed by either analyzer.
