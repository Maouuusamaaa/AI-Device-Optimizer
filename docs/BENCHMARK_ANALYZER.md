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


## 0.1.14 Evidence/Measurement Hardening

The repository now provides `scripts/evidence_measurement.py` as a deterministic
measurement-only contract layer.

It validates benchmark evidence for schema versions 1 and 2, resolves pairs only
when device identity, Android API level, workload, and schema compatibility are
present, computes descriptive metric deltas, and classifies results as:

- `NO_REGRESSION`
- `REGRESSION`
- `MIXED`
- `INSUFFICIENT_EVIDENCE`
- `INVALID_EVIDENCE`

The current Android `BenchmarkJsonWriter` emits schema version 2. The canonical
`benchmarks/schema.json` therefore accepts both version 1 and version 2 so
historical evidence remains readable while current device evidence is valid.

Analysis records contain canonical SHA-256 provenance for their source evidence
and an analysis identifier. History is append-only; an existing analysis
identifier is never overwritten.

A classification is diagnostic evidence only. It does not authorize an action,
invoke the Action Engine, or diagnose a memory leak from PSS/RSS alone.
