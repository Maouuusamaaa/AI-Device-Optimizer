#!/usr/bin/env python3
"""Aggregate multiple external Termux/Rish baseline observations.

This is measurement-only: it parses each raw Rish TXT independently and
reports cross-run descriptive statistics without claiming causation.
"""

from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path

import importlib.util

SCRIPT = Path(__file__).resolve().parent / "benchmark-analyzer.py"


def load_analyzer():
    spec = importlib.util.spec_from_file_location("benchmark_analyzer", SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError("Unable to load benchmark-analyzer.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


analyzer = load_analyzer()


def stats(values):
    if not values:
        return {"average": None, "median": None, "stdev": None, "min": None, "max": None, "sampleCount": 0}
    return {
        "average": statistics.mean(values),
        "median": statistics.median(values),
        "stdev": statistics.stdev(values) if len(values) >= 2 else 0.0,
        "min": min(values),
        "max": max(values),
        "sampleCount": len(values),
    }


def aggregate(paths):
    observations = [analyzer.parse_rish(Path(path)) for path in paths]
    devices = {
        (
            item["device"].get("manufacturer"),
            item["device"].get("model"),
            item["device"].get("androidApi"),
        )
        for item in observations
    }

    startup_average = [item["startupMs"]["average"] for item in observations if item["startupMs"]["average"] is not None]
    startup_median = [item["startupMs"]["median"] for item in observations if item["startupMs"]["median"] is not None]
    wait_average = [item["waitMs"]["average"] for item in observations if item["waitMs"]["average"] is not None]
    pss = [item["memoryKb"]["pss"] for item in observations if item["memoryKb"]["pss"] is not None]
    rss = [item["memoryKb"]["rss"] for item in observations if item["memoryKb"]["rss"] is not None]
    swap = [item["memoryKb"]["swapPss"] for item in observations if item["memoryKb"]["swapPss"] is not None]
    battery = [item["batteryPercent"] for item in observations if item["batteryPercent"] is not None]
    temperature = [item["temperatureC"] for item in observations if item["temperatureC"] is not None]

    return {
        "aggregationVersion": 1,
        "measurementOnly": True,
        "observationCount": len(observations),
        "deviceConsistency": {
            "allMatch": len(devices) <= 1,
            "devices": [
                {"manufacturer": m, "model": model, "androidApi": api}
                for m, model, api in sorted(devices, key=lambda value: str(value))
            ],
        },
        "observations": observations,
        "crossRun": {
            "startupAverageMs": stats(startup_average),
            "startupMedianMs": stats(startup_median),
            "waitAverageMs": stats(wait_average),
            "pssKb": stats(pss),
            "rssKb": stats(rss),
            "swapPssKb": stats(swap),
            "batteryPercent": stats(battery),
            "temperatureC": stats(temperature),
        },
        "notes": [
            "These are descriptive statistics across independent Rish runs.",
            "Different battery, temperature, foreground workload, thermal state, and system state can affect startup and memory measurements.",
            "Cross-run variation does not establish that an optimizer caused any observed change.",
            "External Rish startup/memory measurements are kept separate from the internal monitor benchmark because their measurement methods differ.",
        ],
    }


def main():
    parser = argparse.ArgumentParser(description="Aggregate external Rish baseline TXT files.")
    parser.add_argument("paths", nargs="+", help="Rish baseline TXT files")
    parser.add_argument("--out", required=True, help="Output JSON path")
    args = parser.parse_args()

    result = aggregate(args.paths)
    rendered = json.dumps(result, indent=2, ensure_ascii=False)
    print(rendered)
    output = Path(args.out)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(rendered + "\n", encoding="utf-8")
    print(f"Saved: {output}")


if __name__ == "__main__":
    main()
