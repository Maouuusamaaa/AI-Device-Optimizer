# Controlled Observation Analyzer

The `scripts/controlled-observation-analyzer.py` script validates and compares the three phases created by `scripts/controlled-observation-run.sh`.

It is measurement-only. It does not execute Rish commands, modify Android state, enable candidate actions, or produce an optimization decision.

## Usage

From the repository root:

```bash
python3 scripts/controlled-observation-analyzer.py \
  benchmarks/results/controlled-observation-YYYYMMDD-HHMMSS
```

To write both reports into the observation directory:

```bash
ROOT=benchmarks/results/controlled-observation-YYYYMMDD-HHMMSS

python3 scripts/controlled-observation-analyzer.py "$ROOT" \
  --out-json "$ROOT/controlled-observation-analysis.json" \
  --out-md "$ROOT/controlled-observation-report.md"
```

The analyzer validates the manifest safety flags, all three phases, one JSON artifact per phase, Rish metric structure, sample counts, and consistent device identity.

It compares startup timing, wait timing, PSS, RSS, and swap PSS. Battery percentage and temperature are reported as device conditions.

## Interpretation

The report is descriptive only. Differences are not attributed to an optimizer action because the current controlled runner intentionally performs the same read-only measurement routine in all three phases.
