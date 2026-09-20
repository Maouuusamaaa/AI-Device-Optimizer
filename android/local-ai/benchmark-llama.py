#!/usr/bin/env python3
"""Measurement-only benchmark wrapper for the local GGUF advisor."""
from __future__ import annotations

import argparse
import json
import resource
import subprocess
import time

TIMEOUT_SECONDS = 150


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--adapter", required=True)
    p.add_argument("--llama-cli", required=True)
    p.add_argument("--model", required=True)
    p.add_argument("--input", required=True)
    a = p.parse_args()

    before = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    start = time.perf_counter()
    proc = subprocess.run(
        [
            "python3", a.adapter, "--llama-cli", a.llama_cli,
            "--model", a.model, "--input", a.input,
        ],
        text=True,
        capture_output=True,
        check=True,
        timeout=TIMEOUT_SECONDS,
    )
    elapsed = (time.perf_counter() - start) * 1000.0
    after = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    result = json.loads(proc.stdout)
    result["benchmark"] = {
        "inferenceWallTimeMs": round(elapsed, 3),
        "childProcessMaxRssKb": after,
        "childProcessMaxRssDeltaKb": max(0, after - before),
        "measurementOnly": True,
        "rssMetricCaveat": "RUSAGE_CHILDREN is a process-tree accounting metric, not a direct peak model RSS measurement.",
    }
    print(json.dumps(result, indent=2, ensure_ascii=False, allow_nan=False))


if __name__ == "__main__":
    raise SystemExit(main())
