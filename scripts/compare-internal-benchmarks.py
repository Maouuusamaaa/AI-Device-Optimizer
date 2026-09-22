#!/usr/bin/env python3
"""Compare two AI Device Optimizer internal benchmark JSON observations.

Usage:
  python3 scripts/compare-internal-benchmarks.py baseline.json workload.json
  python3 scripts/compare-internal-benchmarks.py baseline.json workload.json --out result.json

The comparison is descriptive only. It does not make optimization decisions.
"""

from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path


def nums(samples, key):
    return [
        s[key] for s in samples
        if isinstance(s.get(key), (int, float))
        and not isinstance(s.get(key), bool)
    ]


def mean(values):
    return statistics.mean(values) if values else None


def delta(a, b):
    return None if a is None or b is None else b - a


def pct_change(a, b):
    if a in (None, 0) or b is None:
        return None
    return ((b - a) / a) * 100.0


def summarize(path: Path):
    data = json.loads(path.read_text(encoding="utf-8"))
    samples = data.get("samples", [])
    ts = nums(samples, "timestampMs")
    ram = nums(samples, "availableRamMb")
    collection = nums(samples, "collectionDurationMs")
    battery = nums(samples, "batteryPercent")
    temp = nums(samples, "temperatureC")
    cpu = nums(samples, "processCpuTimeMs")

    return {
        "source": str(path),
        "workload": data.get("workload"),
        "schemaVersion": data.get("schemaVersion"),
        "sampleCount": len(samples),
        "durationMs": (ts[-1] - ts[0]) if len(ts) >= 2 else None,
        "availableRamMb": {
            "average": mean(ram),
            "min": min(ram) if ram else None,
            "max": max(ram) if ram else None,
        },
        "collectionDurationMs": {
            "average": mean(collection),
            "min": min(collection) if collection else None,
            "max": max(collection) if collection else None,
        },
        "batteryPercent": {
            "start": battery[0] if battery else None,
            "end": battery[-1] if battery else None,
            "delta": delta(battery[0], battery[-1]) if len(battery) >= 2 else None,
        },
        "temperatureC": {
            "start": temp[0] if temp else None,
            "end": temp[-1] if temp else None,
            "delta": delta(temp[0], temp[-1]) if len(temp) >= 2 else None,
        },
        "processCpuTimeMs": {
            "start": cpu[0] if cpu else None,
            "end": cpu[-1] if cpu else None,
            "delta": delta(cpu[0], cpu[-1]) if len(cpu) >= 2 else None,
        },
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("baseline")
    p.add_argument("workload")
    p.add_argument("--out")
    args = p.parse_args()

    base = summarize(Path(args.baseline))
    work = summarize(Path(args.workload))

    result = {
        "comparisonVersion": 1,
        "baseline": base,
        "workload": work,
        "deltas": {
            "durationMs": delta(base["durationMs"], work["durationMs"]),
            "sampleCount": delta(base["sampleCount"], work["sampleCount"]),
            "averageAvailableRamMb": delta(
                base["availableRamMb"]["average"],
                work["availableRamMb"]["average"],
            ),
            "averageAvailableRamPercent": pct_change(
                base["availableRamMb"]["average"],
                work["availableRamMb"]["average"],
            ),
            "averageCollectionDurationMs": delta(
                base["collectionDurationMs"]["average"],
                work["collectionDurationMs"]["average"],
            ),
            "temperatureEndDeltaC": delta(
                base["temperatureC"]["delta"],
                work["temperatureC"]["delta"],
            ),
            "batteryDeltaPoints": delta(
                base["batteryPercent"]["delta"],
                work["batteryPercent"]["delta"],
            ),
            "processCpuDeltaMs": delta(
                base["processCpuTimeMs"]["delta"],
                work["processCpuTimeMs"]["delta"],
            ),
        },
        "interpretation": [
            "This is a descriptive comparison of two observations, not an optimization decision.",
            "The baseline and workload use different durations and workloads, so their absolute values are not interchangeable performance scores.",
            "A workload effect should be reproduced across matched runs before being treated as causal.",
            "Process CPU time is CPU time for the optimizer process, not whole-device CPU utilization.",
        ],
    }

    rendered = json.dumps(result, indent=2, ensure_ascii=False)
    print(rendered)
    if args.out:
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(rendered + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
