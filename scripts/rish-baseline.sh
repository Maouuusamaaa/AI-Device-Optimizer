#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.maouuusama.ai.device.optimizer"
ACTIVITY="$PACKAGE/.MainActivity"
RISH="${RISH:-$HOME/rish}"
OUT_DIR="${1:-benchmarks/results}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUT_FILE="$OUT_DIR/rish-baseline-$TIMESTAMP.txt"
JSON_FILE="$OUT_DIR/rish-baseline-$TIMESTAMP.json"
CSV_FILE="$OUT_DIR/rish-baseline-$TIMESTAMP.csv"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANALYZER="$SCRIPT_DIR/benchmark-analyzer.py"
CSV_EXPORTER="$SCRIPT_DIR/benchmark-to-csv.py"

mkdir -p "$OUT_DIR"

if [[ ! -f "$ANALYZER" || ! -f "$CSV_EXPORTER" ]]; then
  echo "Benchmark analyzer/exporter not found beside the script."
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to generate JSON/CSV benchmark artifacts."
  exit 1
fi

if [[ ! -x "$RISH" ]]; then
  echo "Rish executable not found: $RISH"
  echo "Set RISH=/path/to/rish or place rish at ~/rish."
  exit 1
fi

rsh() {
  "$RISH" -c "$1"
}

{
  echo "AI Device Optimizer - external Termux/Shizuku baseline"
  echo "timestamp=$(date -Is)"
  echo "rish=$RISH"
  echo

  echo "[device]"
  rsh 'getprop ro.product.manufacturer'
  rsh 'getprop ro.product.model'
  rsh 'getprop ro.build.version.sdk'
  echo

  echo "[battery]"
  rsh 'dumpsys battery' | grep -E 'level:|temperature:|status:|AC powered:|USB powered:|Wireless powered:'
  echo

  echo "[startup]"
  for i in 1 2 3 4 5; do
    rsh "am force-stop $PACKAGE"
    echo "run=$i"
    rsh "am start -W -n $ACTIVITY" | grep -E 'ThisTime:|TotalTime:|WaitTime:'
  done
  echo

  echo "[app-memory]"
  rsh "dumpsys meminfo $PACKAGE" | grep -E 'TOTAL|Java Heap:|Native Heap:|Graphics:|Private Other:|System:'
} | tee "$OUT_FILE"

echo
python3 "$ANALYZER" --rish "$OUT_FILE" --out "$JSON_FILE" >/dev/null
python3 "$CSV_EXPORTER" --input "$JSON_FILE" --output "$CSV_FILE"

echo "Saved: $OUT_FILE"
echo "Saved: $JSON_FILE"
echo "Saved: $CSV_FILE"
