#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.maouuusama.ai.device.optimizer"
ACTIVITY="$PACKAGE/.MainActivity"
RISH="${RISH:-$HOME/rish}"
OUT_DIR="${1:-benchmarks/results}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUT_FILE="$OUT_DIR/rish-baseline-$TIMESTAMP.txt"

mkdir -p "$OUT_DIR"

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
echo "Saved: $OUT_FILE"
