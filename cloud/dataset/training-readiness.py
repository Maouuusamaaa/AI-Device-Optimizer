#!/usr/bin/env python3
"""Validate whether the Stage 5 dataset is safe and meaningful for supervised training."""
from __future__ import annotations
import argparse, json
from collections import Counter
from pathlib import Path

REQUIRED_FEATURES = ("batteryPercent", "temperatureC", "availableRamMb")
ALLOWED_CLASSES = {"NO_ACTION", "OBSERVE", "CANDIDATE_ACTION", "INSUFFICIENT_EVIDENCE"}
REQUIRED_SPLITS = {"train", "validation", "test"}

def inspect_dataset(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    rows = data.get("rows")
    if not isinstance(rows, list) or not rows:
        raise ValueError("dataset must contain a non-empty rows array")
    if data.get("rowCount") != len(rows):
        raise ValueError("rowCount does not match rows length")
    labels = Counter()
    splits = Counter()
    row_ids = set()
    observations = set()
    for row in rows:
        if row.get("schemaVersion") != 1:
            raise ValueError("unsupported row schemaVersion")
        row_id = row.get("rowId")
        observation = row.get("observationRef")
        if not isinstance(row_id, str) or not row_id or row_id in row_ids:
            raise ValueError("rowId must be unique and non-empty")
        if not isinstance(observation, str) or not observation or observation in observations:
            raise ValueError("observationRef must be unique and non-empty")
        row_ids.add(row_id); observations.add(observation)
        features = row.get("features", {})
        for key in REQUIRED_FEATURES:
            if key not in features:
                raise ValueError(f"missing required feature: {key}")
        label = row.get("label", {}).get("outcomeClass")
        if label not in ALLOWED_CLASSES:
            raise ValueError(f"unsupported outcomeClass: {label}")
        split = row.get("provenance", {}).get("split")
        if split not in REQUIRED_SPLITS:
            raise ValueError("every row needs train/validation/test provenance split")
        labels[label] += 1; splits[split] += 1
    missing_splits = sorted(REQUIRED_SPLITS - set(splits))
    if missing_splits:
        raise ValueError("missing required splits: " + ", ".join(missing_splits))
    class_count = len(labels)
    ready = class_count >= 2
    reason = "READY_FOR_SUPERVISED_TRAINING" if ready else "TRAINING_BLOCKED_INSUFFICIENT_LABEL_DIVERSITY"
    return {
        "schemaVersion": 1,
        "datasetRowCount": len(rows),
        "labelDistribution": dict(sorted(labels.items())),
        "splitDistribution": dict(sorted(splits.items())),
        "uniqueLabelCount": class_count,
        "trainingReady": ready,
        "status": reason,
        "policy": {
            "privateDataMustRemainOutsideRepository": True,
            "cloudExecutionAllowed": False,
            "deviceMutationAllowed": False
        }
    }

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("dataset", type=Path)
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    report = inspect_dataset(args.dataset)
    text = json.dumps(report, indent=2) + "\n"
    if args.out:
        args.out.write_text(text, encoding="utf-8")
    print(text, end="")
    return 0 if report["trainingReady"] else 2

if __name__ == "__main__":
    raise SystemExit(main())
