#!/usr/bin/env python3
"""Flatten a benchmark analyzer JSON report into CSV."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

FIELDS = [
    "sourceType", "source", "timestamp", "manufacturer", "model", "androidApi",
    "workload", "sampleCount", "startupAverageMs", "startupMedianMs",
    "startupStdevMs", "startupMinMs", "startupMaxMs", "waitAverageMs",
    "pssKb", "rssKb", "swapPssKb", "batteryPercent", "temperatureC",
]


def row_external(data):
    startup = data.get("startupMs", {})
    wait = data.get("waitMs", {})
    memory = data.get("memoryKb", {})
    device = data.get("device", {})
    return {
        "sourceType": "external_rish",
        "source": data.get("source"),
        "timestamp": data.get("timestamp"),
        "manufacturer": device.get("manufacturer"),
        "model": device.get("model"),
        "androidApi": device.get("androidApi"),
        "workload": "external_startup_memory",
        "sampleCount": startup.get("sampleCount"),
        "startupAverageMs": startup.get("average"),
        "startupMedianMs": startup.get("median"),
        "startupStdevMs": startup.get("stdev"),
        "startupMinMs": startup.get("min"),
        "startupMaxMs": startup.get("max"),
        "waitAverageMs": wait.get("average"),
        "pssKb": memory.get("pss"),
        "rssKb": memory.get("rss"),
        "swapPssKb": memory.get("swapPss"),
        "batteryPercent": data.get("batteryPercent"),
        "temperatureC": data.get("temperatureC"),
    }


def row_internal(data):
    device = data.get("device", {})
    battery = data.get("batteryPercent", {})
    temperature = data.get("temperatureC", {})
    return {
        "sourceType": "internal_app",
        "source": data.get("source"),
        "timestamp": data.get("timestampMs"),
        "manufacturer": device.get("manufacturer"),
        "model": device.get("model"),
        "androidApi": device.get("androidApi"),
        "workload": data.get("workload"),
        "sampleCount": data.get("sampleCount"),
        "startupAverageMs": None,
        "startupMedianMs": None,
        "startupStdevMs": None,
        "startupMinMs": None,
        "startupMaxMs": None,
        "waitAverageMs": None,
        "pssKb": None,
        "rssKb": None,
        "swapPssKb": None,
        "batteryPercent": battery.get("delta"),
        "temperatureC": temperature.get("delta"),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    data = json.loads(Path(args.input).read_text(encoding="utf-8"))
    rows = []
    if data.get("externalRish"):
        rows.append(row_external(data["externalRish"]))
    rows.extend(row_internal(item) for item in data.get("internalObservations", []))
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    print(f"Saved: {output}")


if __name__ == "__main__":
    main()