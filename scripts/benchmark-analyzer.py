#!/usr/bin/env python3
"""Analyze AI Device Optimizer baseline results.

Supports:
- Internal app JSON produced by BenchmarkJsonWriter.
- External Termux/Rish text produced by rish-baseline.sh.

Usage:
  python3 scripts/benchmark-analyzer.py --rish benchmarks/results/rish-baseline-*.txt
  python3 scripts/benchmark-analyzer.py --internal path/to/baseline.json --rish path/to/rish.txt
"""
from __future__ import annotations

import argparse
import glob
import json
import re
import statistics
from pathlib import Path


def newest(pattern: str) -> str | None:
    matches = sorted(glob.glob(pattern))
    return matches[-1] if matches else None


def parse_rish(path: Path) -> dict:
    text = path.read_text(encoding="utf-8")

    startup = [int(x) for x in re.findall(r"^(?:TotalTime):\s*(\d+)\s*$", text, re.MULTILINE)]
    wait = [int(x) for x in re.findall(r"^WaitTime:\s*(\d+)\s*$", text, re.MULTILINE)]

    def one(pattern: str):
        m = re.search(pattern, text, re.MULTILINE)
        return m.group(1) if m else None

    temp_raw = one(r"^\s*temperature:\s*(\d+)\s*$")
    level = one(r"^\s*level:\s*(\d+)\s*$")
    pss = one(r"^\s*TOTAL\s+(\d+)\s+",)
    rss = one(r"TOTAL RSS:\s*(\d+)")
    swap = one(r"TOTAL SWAP PSS:\s*(\d+)")
    manufacturer = one(r"^([^\n]+)\n([^\n]+)\n(\d+)\s*$")

    return {
        "source": str(path),
        "manufacturer": manufacturer if isinstance(manufacturer, str) else one(r"^([^\n]+)$"),
        "batteryPercent": int(level) if level else None,
        "temperatureC": int(temp_raw) / 10 if temp_raw else None,
        "startupMs": {
            "samples": startup,
            "average": statistics.mean(startup) if startup else None,
            "min": min(startup) if startup else None,
            "max": max(startup) if startup else None,
        },
        "waitMs": {
            "samples": wait,
            "average": statistics.mean(wait) if wait else None,
        },
        "memoryKb": {
            "pss": int(pss) if pss else None,
            "rss": int(rss) if rss else None,
            "swapPss": int(swap) if swap else None,
        },
    }


def parse_internal(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    samples = data.get("samples", [])

    def values(key):
        return [s[key] for s in samples if isinstance(s.get(key), (int, float))]

    ram = values("availableRamMb")
    collection = values("collectionDurationMs")
    battery = [s["batteryPercent"] for s in samples if isinstance(s.get("batteryPercent"), int)]
    temp = values("temperatureC")
    cpu = values("processCpuTimeMs")

    return {
        "source": str(path),
        "schemaVersion": data.get("schemaVersion"),
        "workload": data.get("workload"),
        "device": data.get("device", {}),
        "sampleCount": len(samples),
        "durationMs": (
            samples[-1]["timestampMs"] - samples[0]["timestampMs"]
            if len(samples) >= 2 else None
        ),
        "availableRamMb": {
            "average": statistics.mean(ram) if ram else None,
            "min": min(ram) if ram else None,
            "max": max(ram) if ram else None,
        },
        "collectionDurationMs": {
            "average": statistics.mean(collection) if collection else None,
            "min": min(collection) if collection else None,
            "max": max(collection) if collection else None,
        },
        "batteryPercent": {
            "start": battery[0] if battery else None,
            "end": battery[-1] if battery else None,
        },
        "temperatureC": {
            "start": temp[0] if temp else None,
            "end": temp[-1] if temp else None,
        },
        "processCpuTimeMs": {
            "start": cpu[0] if cpu else None,
            "end": cpu[-1] if cpu else None,
            "delta": (cpu[-1] - cpu[0]) if len(cpu) >= 2 else None,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Analyze AI Device Optimizer benchmark results.")
    parser.add_argument("--internal", help="Path to internal app baseline JSON.")
    parser.add_argument("--rish", help="Path to external Rish baseline TXT.")
    parser.add_argument("--out", help="Optional output JSON path.")
    args = parser.parse_args()

    if not args.internal and not args.rish:
        parser.error("Provide --internal and/or --rish.")

    result = {"analyzerVersion": 1}
    if args.internal:
        result["internal"] = parse_internal(Path(args.internal))
    if args.rish:
        result["externalRish"] = parse_rish(Path(args.rish))

    print(json.dumps(result, indent=2, ensure_ascii=False))

    if args.out:
        Path(args.out).write_text(
            json.dumps(result, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        print(f"Saved: {args.out}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
