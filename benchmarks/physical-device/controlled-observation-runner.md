# Controlled Observation Runner

Use `scripts/controlled-observation-run.sh` to execute the first candidate, `observe.remeasure_baseline`.

The runner performs three identical observation phases:

- `baseline`
- `experiment` (repeat observation only; no optimizer action)
- `post`

Each phase uses the existing five-sample Rish baseline routine. The runner stores raw TXT/JSON/CSV artifacts under one timestamped directory and writes a manifest declaring that execution is disabled and device mutation is not allowed.

## Run on the physical device

From the repository root in Termux:

```bash
bash scripts/controlled-observation-run.sh
```

Optional output directory:

```bash
bash scripts/controlled-observation-run.sh benchmarks/results/controlled-observation-YYYYMMDD-HHMMSS
```

The script requires the existing `~/rish` executable and Python 3. It only force-stops/starts the optimizer app for measurement and reads telemetry through the existing Rish baseline routine. It does not invoke an optimizer action.

## Evidence to preserve

Keep the complete output directory, especially `manifest.json`, raw TXT files, JSON analysis, and CSV exports. Do not replace raw observations with averages.

The three phases are deliberately identical at this milestone. This establishes repeatability and variance evidence for the candidate observation contract without claiming an optimization effect.
