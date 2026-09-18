#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.maouuusama.ai.device.optimizer"
ACTIVITY="$PACKAGE/.MainActivity"
OUT_DIR="${1:-benchmarks/results}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUT_FILE="$OUT_DIR/adb-baseline-$TIMESTAMP.txt"

mkdir -p "$OUT_DIR"

if ! adb get-state >/dev/null 2>&1; then
  echo "No authorized Android device is available through adb."
  exit 1
fi

{
  echo "AI Device Optimizer - external ADB baseline"
  echo "timestamp=$(date -Is)"
  echo

  echo "[device]"
  adb shell getprop ro.product.manufacturer
  adb shell getprop ro.product.model
  adb shell getprop ro.build.version.sdk
  echo

  echo "[battery]"
  adb shell dumpsys battery | grep -E 'level:|temperature:|status:|AC powered:|USB powered:|Wireless powered:'
  echo

  echo "[startup]"
  for i in 1 2 3 4 5; do
    adb shell am force-stop "$PACKAGE"
    echo "run=$i"
    adb shell am start -W -n "$ACTIVITY" | grep -E 'ThisTime:|TotalTime:|WaitTime:'
  done
  echo

  echo "[app-memory]"
  adb shell dumpsys meminfo "$PACKAGE" | grep -E 'TOTAL|Java Heap:|Native Heap:|Graphics:|Private Other:|System:'
} | tee "$OUT_FILE"

echo
echo "Saved: $OUT_FILE"
