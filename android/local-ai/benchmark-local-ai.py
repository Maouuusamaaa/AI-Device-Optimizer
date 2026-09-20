#!/usr/bin/env python3
"""Measure local advisory inference overhead without mutating the device."""
from __future__ import annotations
import json
import resource
import subprocess
import sys
import time

def main() -> int:
    payload = sys.stdin.read()
    before = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    start = time.perf_counter()
    proc = subprocess.run(
        [sys.executable, "android/local-ai/local-ai-advisor.py"],
        input=payload,
        text=True,
        capture_output=True,
        check=True,
    )
    elapsed_ms = (time.perf_counter() - start) * 1000.0
    after = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    result = json.loads(proc.stdout)
    result["benchmark"] = {
        "inferenceWallTimeMs": round(elapsed_ms, 3),
        "childProcessMaxRssKb": after,
        "childProcessMaxRssDeltaKb": max(0, after - before),
        "measurementOnly": True,
    }
    json.dump(result, sys.stdout, indent=2)
    sys.stdout.write("\n")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
