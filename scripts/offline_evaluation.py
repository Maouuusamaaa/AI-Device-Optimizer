#!/usr/bin/env python3
"""Deterministic offline replay of paired device evidence.

This module consumes immutable benchmark/evidence pairs, reuses the established
measurement classifier, and records evaluation outcomes without authorizing
policy selection or device execution.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from evidence_measurement import (
    CLASSIFICATIONS,
    classify_comparison,
    compare_pair,
    create_provenance_record,
    resolve_pair,
)


EVALUATION_SCHEMA_VERSION = 1
EVALUATOR_VERSION = 1


class EvaluationError(ValueError):
    pass


def _canonical_sha256(value: Any) -> str:
    payload = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _load_rules(path: Path) -> dict[str, Any]:
    try:
        rules = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise EvaluationError(f"cannot read rules: {exc}") from exc
    if not isinstance(rules, dict) or not isinstance(rules.get("version"), int):
        raise EvaluationError("rules must contain an integer version")
    return rules


def _validate_dataset(dataset: Any) -> list[dict[str, Any]]:
    if not isinstance(dataset, dict):
        raise EvaluationError("dataset must be an object")
    if dataset.get("schemaVersion") != EVALUATION_SCHEMA_VERSION:
        raise EvaluationError("dataset schemaVersion must be 1")

    cases = dataset.get("cases")
    if not isinstance(cases, list) or not cases:
        raise EvaluationError("dataset cases must be a non-empty list")

    ids: set[str] = set()
    for index, case in enumerate(cases):
        if not isinstance(case, dict):
            raise EvaluationError(f"cases[{index}] must be an object")
        case_id = case.get("caseId")
        if not isinstance(case_id, str) or not case_id.strip():
            raise EvaluationError(f"cases[{index}].caseId must be non-empty")
        if case_id in ids:
            raise EvaluationError(f"duplicate caseId: {case_id}")
        ids.add(case_id)
        if not isinstance(case.get("baseline"), dict):
            raise EvaluationError(f"cases[{index}].baseline must be an object")
        if not isinstance(case.get("variant"), dict):
            raise EvaluationError(f"cases[{index}].variant must be an object")
        if "policyId" in case and (
            not isinstance(case["policyId"], str) or not case["policyId"].strip()
        ):
            raise EvaluationError(f"cases[{index}].policyId must be non-empty when present")
    return cases


def evaluate_dataset(dataset: dict[str, Any], rules: dict[str, Any] | None = None) -> dict[str, Any]:
    """Replay all evidence pairs deterministically without mutating *dataset*."""
    source = copy.deepcopy(dataset)
    cases = _validate_dataset(source)

    if rules is None:
        rules = {"version": 1, "rules": {
            "availableRamDecreasePct": -5.0,
            "pssIncreasePct": 20.0,
        }}

    results: list[dict[str, Any]] = []
    counts = {classification: 0 for classification in CLASSIFICATIONS}

    for case in cases:
        baseline = copy.deepcopy(case["baseline"])
        variant = copy.deepcopy(case["variant"])
        pair = resolve_pair(baseline, variant)
        comparison = compare_pair(pair)
        classification = classify_comparison(comparison, rules)
        label = classification["classification"]
        if label not in counts:
            raise EvaluationError(f"unsupported classifier output: {label}")

        counts[label] += 1
        provenance = create_provenance_record(
            baseline,
            variant,
            comparison,
            classification,
            rules_version=rules["version"],
        )
        results.append({
            "caseId": case["caseId"],
            "policyId": case.get("policyId"),
            "baselineEvidenceId": baseline.get("evidenceId"),
            "variantEvidenceId": variant.get("evidenceId"),
            "classification": label,
            "reasons": list(classification.get("reasons", [])),
            "analysisId": provenance["analysisId"],
            "provenance": provenance,
        })

    dataset_fingerprint = _canonical_sha256(source)
    report_core = {
        "schemaVersion": EVALUATION_SCHEMA_VERSION,
        "evaluatorVersion": EVALUATOR_VERSION,
        "rulesVersion": rules["version"],
        "datasetFingerprint": dataset_fingerprint,
        "caseCount": len(results),
        "classificationCounts": counts,
        "cases": results,
        "executionAllowed": False,
        "policySelectionAllowed": False,
        "interpretation": (
            "descriptive_only: offline replay re-runs the established measurement "
            "classifier over stored evidence; it does not estimate causal effects, "
            "rank policies, select actions, or authorize device mutation."
        ),
    }
    evaluation_id = _canonical_sha256(report_core)
    return {
        "evaluationId": evaluation_id,
        **report_core,
    }


def append_evaluation_history(path: Path, report: dict[str, Any]) -> dict[str, Any]:
    """Append one evaluation result, rejecting malformed history and deduplicating IDs."""
    target = Path(path)
    existing_ids: set[str] = set()

    if target.exists():
        try:
            lines = target.read_text(encoding="utf-8").splitlines()
        except OSError as exc:
            raise EvaluationError(f"cannot read evaluation history: {exc}") from exc
        for line_number, line in enumerate(lines, 1):
            if not line.strip():
                continue
            try:
                record = json.loads(line)
            except json.JSONDecodeError as exc:
                raise EvaluationError(
                    f"evaluation history line {line_number} is invalid JSON"
                ) from exc
            if not isinstance(record, dict):
                raise EvaluationError(
                    f"evaluation history line {line_number} must be an object"
                )
            evaluation_id = record.get("evaluationId")
            if not isinstance(evaluation_id, str) or not evaluation_id:
                raise EvaluationError(
                    f"evaluation history line {line_number} lacks evaluationId"
                )
            existing_ids.add(evaluation_id)

    evaluation_id = report.get("evaluationId")
    if not isinstance(evaluation_id, str) or not evaluation_id:
        raise EvaluationError("report requires evaluationId")

    if evaluation_id in existing_ids:
        return {"appended": False, "duplicate": True, "evaluationId": evaluation_id}

    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("a", encoding="utf-8") as handle:
        handle.write(
            json.dumps(report, ensure_ascii=False, sort_keys=True) + "\n"
        )
    return {"appended": True, "duplicate": False, "evaluationId": evaluation_id}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("dataset", type=Path)
    parser.add_argument("--rules", type=Path)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--history", type=Path)
    args = parser.parse_args()

    try:
        dataset = json.loads(args.dataset.read_text(encoding="utf-8"))
        rules = _load_rules(args.rules) if args.rules else None
        report = evaluate_dataset(dataset, rules)
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(
            json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        if args.history:
            append_evaluation_history(args.history, report)
    except (OSError, json.JSONDecodeError, EvaluationError) as exc:
        parser.error(str(exc))

    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
