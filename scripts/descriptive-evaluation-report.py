#!/usr/bin/env python3
"""Deterministic, measurement-only descriptive evaluation report."""

from __future__ import annotations
import argparse, hashlib, json, math
from collections import Counter
from pathlib import Path


def finite_number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def load(path):
    with Path(path).open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    if not isinstance(data, list):
        raise ValueError("dataset must be a JSON array")
    return data


def validate(records):
    ids = [r.get("observationId") for r in records]
    if any(not isinstance(x, str) or not x for x in ids):
        raise ValueError("every observationId must be a non-empty string")
    if len(ids) != len(set(ids)):
        raise ValueError("duplicate observationId detected")
    times = [r.get("timestampMs") for r in records]
    if any(not isinstance(x, int) or isinstance(x, bool) or x < 0 for x in times):
        raise ValueError("timestampMs must be non-negative integers")
    if times != sorted(times):
        raise ValueError("records must be chronological")
    for record in records:
        for key in ("conditionIds", "actionIds"):
            if not isinstance(record.get(key), list):
                raise ValueError(f"{key} must be a list")
        metadata = record.get("metadata")
        if not isinstance(metadata, dict):
            raise ValueError("metadata must be an object")
        for key in ("charging", "batteryTemperatureC", "thermalStatus",
                    "networkTransport", "networkValidated", "interactive", "workload"):
            if key in metadata and metadata[key] is not None:
                if key in ("batteryTemperatureC",) and not finite_number(metadata[key]):
                    raise ValueError(f"{key} must be finite")
        if "outcomeLabel" in record and record["outcomeLabel"] is not None and not isinstance(record["outcomeLabel"], str):
            raise ValueError("outcomeLabel must be a string or null")


def summarize(records):
    conditions = Counter(x for r in records for x in r["conditionIds"])
    actions = Counter(x for r in records for x in r["actionIds"])
    outcomes = Counter(r["outcomeLabel"] for r in records if r.get("outcomeLabel"))
    charging = Counter(r["metadata"]["charging"] for r in records if r["metadata"].get("charging") is not None)
    workloads = Counter(r["metadata"]["workload"] for r in records if r["metadata"].get("workload"))
    temps = [r["metadata"]["batteryTemperatureC"] for r in records
             if finite_number(r["metadata"].get("batteryTemperatureC"))]
    return {
        "observationCount": len(records),
        "conditionCoverage": dict(sorted(conditions.items())),
        "actionCoverage": dict(sorted(actions.items())),
        "outcomeLabelCoverage": dict(sorted(outcomes.items())),
        "chargingCoverage": {str(k): v for k, v in sorted(charging.items(), key=lambda x: str(x[0]))},
        "workloadCoverage": dict(sorted(workloads.items())),
        "batteryTemperatureC": {
            "count": len(temps),
            "average": sum(temps) / len(temps) if temps else None,
            "minimum": min(temps) if temps else None,
            "maximum": max(temps) if temps else None,
        },
    }


def fingerprint(records):
    canonical = json.dumps(records, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("dataset")
    parser.add_argument("--out-json", required=True)
    args = parser.parse_args()
    records = load(args.dataset)
    validate(records)
    report = {
        "reportVersion": 1,
        "measurementOnly": True,
        "datasetFingerprint": fingerprint(records),
        **summarize(records),
        "interpretation": (
            "descriptive_only: statistics summarize declared observations; "
            "no effectiveness, causal, ranking, policy-selection, or execution claim is produced."
        ),
        "policySelectionAllowed": False,
        "executionAllowed": False,
    }
    Path(args.out_json).write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
