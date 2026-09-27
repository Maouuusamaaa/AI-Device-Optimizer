#!/usr/bin/env python3
"""Descriptive evaluation hardening for adaptive-learning candidates.

This module adds an explicit, non-causal outcome contract, context/confounder
matching, and train-only candidate / holdout evaluation. It never authorizes
execution, policy selection, permission changes, or device mutation.
"""

from __future__ import annotations

import copy
import hashlib
import json
from typing import Any

EVALUATION_SCHEMA_VERSION = 1
EVALUATOR_VERSION = 1
MINIMUM_HOLDOUT_COUNT = 2

DESCRIPTIVE_OUTCOME_CLASSES = {
    "NON_DEGRADING_DESCRIPTIVE",
    "DEGRADING_DESCRIPTIVE",
    "MIXED_DESCRIPTIVE",
    "UNRESOLVED",
}

HOLDOUT_STATUSES = {
    "SUPPORTED_DESCRIPTIVELY",
    "CONTRADICTED_DESCRIPTIVELY",
    "MIXED_DESCRIPTIVELY",
    "INSUFFICIENT_HOLDOUT",
    "CONTEXT_INCOMPLETE",
}

DEFAULT_CONFOUNDER_KEYS = (
    "interactive",
    "charging",
    "thermalStatus",
    "networkTransport",
    "gameModeChecked",
)


class EvaluationHardeningError(ValueError):
    pass


def _canonical_sha256(value: Any) -> str:
    payload = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _require_string(value: Any, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise EvaluationHardeningError(f"{field} must be a non-empty string")
    return value


def classify_descriptive_outcome(classification: str) -> str:
    """Map an existing measurement classification to an explicit non-causal label."""
    mapping = {
        "NO_REGRESSION": "NON_DEGRADING_DESCRIPTIVE",
        "REGRESSION": "DEGRADING_DESCRIPTIVE",
        "MIXED": "MIXED_DESCRIPTIVE",
        "INSUFFICIENT_EVIDENCE": "UNRESOLVED",
    }
    try:
        return mapping[classification]
    except KeyError as exc:
        raise EvaluationHardeningError(
            f"unsupported measurement classification: {classification}"
        ) from exc


def normalize_context(
    context: dict[str, Any] | None,
    *,
    allowed_keys: tuple[str, ...] = DEFAULT_CONFOUNDER_KEYS,
) -> dict[str, Any]:
    """Keep only declared context fields and reject unknown fields."""
    if context is None:
        return {}
    if not isinstance(context, dict):
        raise EvaluationHardeningError("context must be an object")

    allowed = set(allowed_keys)
    unknown = sorted(set(context) - allowed)
    if unknown:
        raise EvaluationHardeningError(
            f"context contains unsupported keys: {', '.join(unknown)}"
        )

    normalized: dict[str, Any] = {}
    for key in allowed_keys:
        if key not in context:
            continue
        value = context[key]
        if not isinstance(value, (str, int, float, bool)) and value is not None:
            raise EvaluationHardeningError(f"context.{key} must be scalar or null")
        normalized[key] = value
    return normalized


def compare_context(
    baseline_context: dict[str, Any] | None,
    variant_context: dict[str, Any] | None,
    *,
    keys: tuple[str, ...] = DEFAULT_CONFOUNDER_KEYS,
) -> dict[str, Any]:
    """Report whether declared context/confounder fields are matched."""
    baseline = normalize_context(baseline_context, allowed_keys=keys)
    variant = normalize_context(variant_context, allowed_keys=keys)

    missing_baseline = [key for key in keys if key not in baseline]
    missing_variant = [key for key in keys if key not in variant]
    mismatched = [
        key for key in keys
        if key in baseline and key in variant and baseline[key] != variant[key]
    ]

    if missing_baseline or missing_variant:
        status = "INCOMPLETE"
    elif mismatched:
        status = "MISMATCHED"
    else:
        status = "MATCHED"

    return {
        "status": status,
        "matched": status == "MATCHED",
        "missingBaselineKeys": missing_baseline,
        "missingVariantKeys": missing_variant,
        "mismatchedKeys": mismatched,
        "keys": list(keys),
    }


def create_descriptive_outcome(
    record: dict[str, Any],
    *,
    baseline_context: dict[str, Any] | None = None,
    variant_context: dict[str, Any] | None = None,
    confounder_keys: tuple[str, ...] = DEFAULT_CONFOUNDER_KEYS,
) -> dict[str, Any]:
    """Create an explicit outcome record without converting it into a reward."""
    source = copy.deepcopy(record)
    if not isinstance(source, dict):
        raise EvaluationHardeningError("record must be an object")

    evidence_id = _require_string(source.get("evidenceId"), "evidenceId")
    classification = _require_string(source.get("classification"), "classification")
    context = compare_context(
        baseline_context,
        variant_context,
        keys=confounder_keys,
    )

    outcome = {
        "schemaVersion": EVALUATION_SCHEMA_VERSION,
        "evaluatorVersion": EVALUATOR_VERSION,
        "evidenceId": evidence_id,
        "classification": classification,
        "descriptiveOutcomeClass": classify_descriptive_outcome(classification),
        "confounderAssessment": context,
        "causalInferenceAllowed": False,
        "executionAllowed": False,
        "policySelectionAllowed": False,
    }
    outcome["outcomeFingerprint"] = _canonical_sha256(outcome)
    return outcome


def _pattern_key(record: dict[str, Any]) -> tuple[Any, ...]:
    device = record.get("device")
    if not isinstance(device, dict):
        raise EvaluationHardeningError("record.device must be an object")
    return (
        device.get("manufacturer"),
        device.get("model"),
        device.get("androidApi"),
        record.get("workload"),
        record.get("policyId"),
    )


def _holdout_status(
    outcomes: list[dict[str, Any]],
    *,
    minimum_holdout_count: int,
) -> str:
    if len(outcomes) < minimum_holdout_count:
        return "INSUFFICIENT_HOLDOUT"

    confounder_statuses = {
        item["confounderAssessment"]["status"] for item in outcomes
    }
    if confounder_statuses != {"MATCHED"}:
        return "CONTEXT_INCOMPLETE"

    labels = {item["descriptiveOutcomeClass"] for item in outcomes}
    if labels == {"NON_DEGRADING_DESCRIPTIVE"}:
        return "SUPPORTED_DESCRIPTIVELY"
    if "DEGRADING_DESCRIPTIVE" in labels:
        return "CONTRADICTED_DESCRIPTIVELY"
    return "MIXED_DESCRIPTIVELY"


def evaluate_holdout(
    train_state: dict[str, Any],
    holdout_records: list[dict[str, Any]],
    *,
    minimum_holdout_count: int = MINIMUM_HOLDOUT_COUNT,
) -> dict[str, Any]:
    """Evaluate train-derived advisory patterns on a disjoint descriptive holdout."""
    if not isinstance(train_state, dict):
        raise EvaluationHardeningError("train_state must be an object")
    if not isinstance(holdout_records, list):
        raise EvaluationHardeningError("holdout_records must be a list")
    if not isinstance(minimum_holdout_count, int) or minimum_holdout_count < 1:
        raise EvaluationHardeningError("minimum_holdout_count must be positive")

    from adaptive_learning import generate_candidates

    candidates = generate_candidates(train_state)
    grouped: dict[tuple[Any, ...], list[dict[str, Any]]] = {}
    for record in holdout_records:
        normalized = copy.deepcopy(record)
        grouped.setdefault(_pattern_key(normalized), []).append(normalized)

    results: list[dict[str, Any]] = []
    for candidate in candidates:
        policy_id = candidate.get("policyId")
        matching = [
            record
            for key, records in grouped.items()
            if key[-1] == policy_id
            for record in records
        ]
        outcomes = [
            create_descriptive_outcome(
                record,
                baseline_context=record.get("baselineContext"),
                variant_context=record.get("variantContext"),
            )
            for record in matching
        ]

        results.append({
            "policyId": policy_id,
            "trainingEvidenceCount": candidate["evidenceCount"],
            "holdoutObservationCount": len(outcomes),
            "holdoutStatus": _holdout_status(
                outcomes,
                minimum_holdout_count=minimum_holdout_count,
            ),
            "supportingTrainingEvidenceIds": list(candidate["supportingEvidenceIds"]),
            "holdoutEvidenceIds": [item["evidenceId"] for item in outcomes],
        })

    report_core = {
        "schemaVersion": EVALUATION_SCHEMA_VERSION,
        "evaluatorVersion": EVALUATOR_VERSION,
        "candidateCount": len(candidates),
        "results": results,
        "evaluationReady": bool(results),
        "causalInferenceAllowed": False,
        "executionAllowed": False,
        "policySelectionAllowed": False,
        "interpretation": (
            "descriptive_only: candidates are derived from training data and "
            "checked against a disjoint holdout; the result does not establish "
            "causal effectiveness or authorize policy selection/execution."
        ),
    }
    return {
        "evaluationId": _canonical_sha256(report_core),
        **report_core,
    }
