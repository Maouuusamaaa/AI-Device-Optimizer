#!/usr/bin/env python3
"""Evaluate like-for-like benchmark observations without mixing workloads.

This evaluator is intentionally measurement-only. It does not execute or
authorize optimization actions.
"""

from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path


METRICS = {
    "availableRamMb": {"higherIsBetter": True, "source": "availableRamMb.average"},
    "processCpuPercentOfWallTime": {
        "higherIsBetter": False,
        "source": "processCpuTimeMs.percentOfWallTime",
    },
    "monitorCollectionDurationMs": {
        "higherIsBetter": False,
        "source": "collectionDurationMs.average",
    },
    "batteryDrainPercent": {"higherIsBetter": False, "source": "batteryPercent.delta"},
    "temperatureDeltaC": {"higherIsBetter": False, "source": "temperatureC.delta"},
}


def _load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def _observations(report: dict) -> list[dict]:
    observations = report.get("internalObservations")
    if not isinstance(observations, list):
        raise ValueError("Report must contain internalObservations.")
    return observations


def _device_key(device: dict) -> tuple:
    return (
        device.get("manufacturer"),
        device.get("model"),
        device.get("androidApi"),
    )


def _workload(observation: dict) -> str | None:
    return observation.get("workload")


def _value(observation: dict, metric: str):
    if metric == "availableRamMb":
        return observation.get("availableRamMb", {}).get("average")
    if metric == "processCpuPercentOfWallTime":
        return observation.get("processCpuTimeMs", {}).get("percentOfWallTime")
    if metric == "monitorCollectionDurationMs":
        return observation.get("collectionDurationMs", {}).get("average")
    if metric == "batteryDrainPercent":
        delta = observation.get("batteryPercent", {}).get("delta")
        return abs(delta) if isinstance(delta, (int, float)) else None
    if metric == "temperatureDeltaC":
        return observation.get("temperatureC", {}).get("delta")
    raise KeyError(metric)


def _direction(baseline, candidate, higher_is_better: bool):
    if baseline is None or candidate is None or candidate == baseline:
        return "unchanged"
    improved = candidate > baseline if higher_is_better else candidate < baseline
    return "improved" if improved else "regressed"


def evaluate(baseline: dict, candidate: dict, min_runs: int = 3) -> dict:
    base = _observations(baseline)
    cand = _observations(candidate)

    if len(base) < min_runs or len(cand) < min_runs:
        raise ValueError(
            f"At least {min_runs} baseline and candidate observations are required."
        )
    if len(base) != len(cand):
        raise ValueError("Baseline and candidate must contain the same number of observations.")

    pairs = []
    for index, (before, after) in enumerate(zip(base, cand), start=1):
        if _device_key(before.get("device", {})) != _device_key(after.get("device", {})):
            raise ValueError(f"Device mismatch at run {index}.")
        if _workload(before) != _workload(after):
            raise ValueError(f"Workload mismatch at run {index}.")
        if not _workload(before):
            raise ValueError(f"Missing workload at run {index}.")

        metrics = {}
        for name, spec in METRICS.items():
            b = _value(before, name)
            c = _value(after, name)
            metrics[name] = {
                "baseline": b,
                "candidate": c,
                "delta": (c - b) if b is not None and c is not None else None,
                "direction": _direction(b, c, spec["higherIsBetter"]),
            }

        pairs.append({"run": index, "workload": _workload(before), "metrics": metrics})

    summary = {}
    for name in METRICS:
        deltas = [
            pair["metrics"][name]["delta"]
            for pair in pairs
            if pair["metrics"][name]["delta"] is not None
        ]
        directions = [
            pair["metrics"][name]["direction"]
            for pair in pairs
            if pair["metrics"][name]["direction"] != "unchanged"
        ]
        summary[name] = {
            "averageDelta": statistics.mean(deltas) if deltas else None,
            "minDelta": min(deltas) if deltas else None,
            "maxDelta": max(deltas) if deltas else None,
            "directionCounts": {
                "improved": directions.count("improved"),
                "unchanged": directions.count("unchanged"),
                "regressed": directions.count("regressed"),
            },
            "directionConsistency": (
                max(directions.count("improved"), directions.count("regressed")) / len(directions)
                if directions
                else 1.0
            ),
        }

    return {
        "evaluatorVersion": 1,
        "measurementOnly": True,
        "runCount": len(pairs),
        "workload": _workload(base[0]),
        "device": base[0].get("device", {}),
        "pairs": pairs,
        "summary": summary,
        "notes": [
            "Only like-for-like runs are compared.",
            "Baseline and candidate observations are never merged across workloads.",
            "A delta describes an observed change; it does not establish causation.",
            "batteryDrainPercent uses the absolute battery percentage change recorded by the benchmark.",
            "temperatureDeltaC is reported as observed and should be interpreted with environmental conditions.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Evaluate like-for-like benchmark observations.")
    parser.add_argument("--baseline", required=True)
    parser.add_argument("--candidate", required=True)
    parser.add_argument("--min-runs", type=int, default=3)
    parser.add_argument("--out")
    args = parser.parse_args()

    if args.min_runs < 1:
        parser.error("--min-runs must be >= 1")

    result = evaluate(_load(Path(args.baseline)), _load(Path(args.candidate)), args.min_runs)
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
