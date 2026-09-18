#!/usr/bin/env python3
"""Analyze AI Device Optimizer baseline results.

Supports:
- Internal app JSON produced by BenchmarkJsonWriter.
- External Termux/Rish text produced by rish-baseline.sh.
- Multiple internal observations in one unified report.

Usage:
  python3 scripts/benchmark-analyzer.py --rish benchmarks/results/rish-baseline-*.txt
  python3 scripts/benchmark-analyzer.py --internal path/to/baseline.json --rish path/to/rish.txt
  python3 scripts/benchmark-analyzer.py --internal baseline-a.json --internal baseline-b.json
"""

from __future__ import annotations

import argparse
import json
import re
import statistics
from pathlib import Path


def mean_or_none(values):
    return statistics.mean(values) if values else None


def numeric_values(samples, key):
    return [
        sample[key]
        for sample in samples
        if isinstance(sample.get(key), (int, float))
        and not isinstance(sample.get(key), bool)
    ]


def parse_rish(path: Path) -> dict:
    text = path.read_text(encoding="utf-8")

    startup = [int(x) for x in re.findall(r"^TotalTime:\s*(\d+)\s*$", text, re.MULTILINE)]
    wait = [int(x) for x in re.findall(r"^WaitTime:\s*(\d+)\s*$", text, re.MULTILINE)]

    def one(pattern: str):
        match = re.search(pattern, text, re.MULTILINE)
        return match.group(1) if match else None

    temp_raw = one(r"^\s*temperature:\s*(\d+)\s*$")
    level = one(r"^\s*level:\s*(\d+)\s*$")
    pss = one(r"^\s*TOTAL\s+(\d+)\s+")
    rss = one(r"TOTAL RSS:\s*(\d+)")
    swap = one(r"TOTAL SWAP PSS:\s*(\d+)")

    device_section = (
        text.split("[device]", 1)[1].split("[battery]", 1)[0]
        if "[device]" in text
        else ""
    )
    device_values = [line.strip() for line in device_section.splitlines() if line.strip()]

    return {
        "source": str(path),
        "device": {
            "manufacturer": device_values[0] if len(device_values) > 0 else None,
            "model": device_values[1] if len(device_values) > 1 else None,
            "androidApi": (
                int(device_values[2])
                if len(device_values) > 2 and device_values[2].isdigit()
                else None
            ),
        },
        "batteryPercent": int(level) if level else None,
        "temperatureC": int(temp_raw) / 10 if temp_raw else None,
        "startupMs": {
            "samples": startup,
            "average": mean_or_none(startup),
            "min": min(startup) if startup else None,
            "max": max(startup) if startup else None,
        },
        "waitMs": {
            "samples": wait,
            "average": mean_or_none(wait),
            "min": min(wait) if wait else None,
            "max": max(wait) if wait else None,
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

    ram = numeric_values(samples, "availableRamMb")
    total_ram = numeric_values(samples, "totalRamMb")
    collection = numeric_values(samples, "collectionDurationMs")
    storage = numeric_values(samples, "storageAvailableMb")
    battery = [
        sample["batteryPercent"]
        for sample in samples
        if isinstance(sample.get("batteryPercent"), int)
        and not isinstance(sample.get("batteryPercent"), bool)
    ]
    temp = numeric_values(samples, "temperatureC")
    cpu = numeric_values(samples, "processCpuTimeMs")

    duration_ms = (
        samples[-1]["timestampMs"] - samples[0]["timestampMs"]
        if len(samples) >= 2
        and isinstance(samples[0].get("timestampMs"), (int, float))
        and isinstance(samples[-1].get("timestampMs"), (int, float))
        else None
    )

    cpu_delta = cpu[-1] - cpu[0] if len(cpu) >= 2 else None
    cpu_pct_of_wall = (
        (cpu_delta / duration_ms) * 100
        if cpu_delta is not None and duration_ms and duration_ms > 0
        else None
    )

    return {
        "source": str(path),
        "schemaVersion": data.get("schemaVersion"),
        "timestampMs": data.get("timestampMs"),
        "workload": data.get("workload"),
        "device": data.get("device", {}),
        "sampleCount": len(samples),
        "durationMs": duration_ms,
        "availableRamMb": {
            "average": mean_or_none(ram),
            "min": min(ram) if ram else None,
            "max": max(ram) if ram else None,
            "percentOfTotalAverage": (
                (mean_or_none(ram) / mean_or_none(total_ram)) * 100
                if ram and total_ram and mean_or_none(total_ram)
                else None
            ),
        },
        "collectionDurationMs": {
            "average": mean_or_none(collection),
            "min": min(collection) if collection else None,
            "max": max(collection) if collection else None,
        },
        "storageAvailableMb": {
            "start": storage[0] if storage else None,
            "end": storage[-1] if storage else None,
        },
        "batteryPercent": {
            "start": battery[0] if battery else None,
            "end": battery[-1] if battery else None,
            "delta": (battery[-1] - battery[0]) if len(battery) >= 2 else None,
        },
        "temperatureC": {
            "start": temp[0] if temp else None,
            "end": temp[-1] if temp else None,
            "delta": (temp[-1] - temp[0]) if len(temp) >= 2 else None,
        },
        "processCpuTimeMs": {
            "start": cpu[0] if cpu else None,
            "end": cpu[-1] if cpu else None,
            "delta": cpu_delta,
            "percentOfWallTime": cpu_pct_of_wall,
        },
    }


def build_unified(internal, external):
    observations = internal or []
    devices = [item.get("device", {}) for item in observations]
    return {
        "internalObservationCount": len(observations),
        "internalObservations": observations,
        "externalRish": external,
        "deviceConsistency": {
            "devices": devices,
            "allMatch": len({
                (
                    device.get("manufacturer"),
                    device.get("model"),
                    device.get("androidApi"),
                )
                for device in devices
            }) <= 1,
        },
        "comparisonNotes": [
            "Internal and external measurements are kept separate because they use different measurement methods.",
            "Multiple internal observations are retained instead of averaged across different device conditions.",
            "processCpuTimeMs is process CPU time; percentOfWallTime is an approximation of CPU time relative to benchmark wall time.",
            "storageAvailableMb is app-private external-files storage availability, not a whole-device storage metric.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Analyze AI Device Optimizer baseline results.")
    parser.add_argument(
        "--internal",
        action="append",
        help="Path to an internal app baseline JSON. Repeat for multiple observations.",
    )
    parser.add_argument("--rish", help="Path to external Rish baseline TXT.")
    parser.add_argument("--out", help="Optional output JSON path.")
    args = parser.parse_args()

    if not args.internal and not args.rish:
        parser.error("Provide --internal and/or --rish.")

    internal = [parse_internal(Path(path)) for path in args.internal or []]
    external = parse_rish(Path(args.rish)) if args.rish else None

    result = {
        "analyzerVersion": 2,
        **build_unified(internal, external),
    }

    rendered = json.dumps(result, indent=2, ensure_ascii=False)
    print(rendered)

    if args.out:
        output = Path(args.out)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(rendered + "\n", encoding="utf-8")
        print(f"Saved: {output}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
