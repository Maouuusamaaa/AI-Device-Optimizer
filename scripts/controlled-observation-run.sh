#!/usr/bin/env bash
set -euo pipefail

OUT_ROOT="${1:-benchmarks/results/controlled-observation-$(date +%Y%m%d-%H%M%S)}"
BASELINE_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/rish-baseline.sh"

if [[ ! -x "$BASELINE_SCRIPT" ]]; then
  echo "Missing executable baseline script: $BASELINE_SCRIPT" >&2
  exit 1
fi

mkdir -p "$OUT_ROOT"

run_phase() {
  local phase="$1"
  local dir="$OUT_ROOT/$phase"
  mkdir -p "$dir"
  echo "=== phase=$phase ==="
  "$BASELINE_SCRIPT" "$dir"
  local latest
  latest="$(find "$dir" -maxdepth 1 -type f -name 'rish-baseline-*.txt' -print | sort | tail -n 1)"
  [[ -n "$latest" ]] || { echo "No raw baseline artifact produced for $phase" >&2; exit 1; }
  printf '%s\n' "$latest"
}

baseline_file="$(run_phase baseline | tail -n 1)"
experiment_file="$(run_phase experiment | tail -n 1)"
post_file="$(run_phase post | tail -n 1)"

python3 - "$OUT_ROOT" "$baseline_file" "$experiment_file" "$post_file" <<'PY'
import json
import sys
from pathlib import Path
from datetime import datetime

root = Path(sys.argv[1])
files = {"baseline": sys.argv[2], "experiment": sys.argv[3], "post": sys.argv[4]}
manifest = {
    "schemaVersion": 1,
    "candidateActionId": "observe.remeasure_baseline",
    "executionEnabled": False,
    "deviceMutationAllowed": False,
    "createdAt": datetime.now().astimezone().isoformat(),
    "phases": {phase: {"rawText": str(Path(path).resolve())} for phase, path in files.items()},
}
(root / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
print(f"Controlled observation complete: {root}")
print("Execution was disabled; all three phases used the same read-only measurement routine.")
PY
