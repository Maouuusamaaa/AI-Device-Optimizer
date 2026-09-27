#!/usr/bin/env python3
"""Deterministic evidence validation, pairing, comparison, and provenance.

This module is measurement-only. It never executes actions or mutates source
evidence.
"""

from __future__ import annotations

import hashlib
import json
from copy import deepcopy
from typing import Any


ANALYZER_VERSION = 1
SUPPORTED_SCHEMA_VERSION = 1
CLASSIFICATIONS = (
    "NO_REGRESSION",
    "REGRESSION",
    "MIXED",
    "INSUFFICIENT_EVIDENCE",
    "INVALID_EVIDENCE",
)

_REQUIRED_TOP_LEVEL = ("schemaVersion", "timestampMs", "workload", "samples")
_REQUIRED_DEVICE = ("androidApi", "manufacturer", "model")
_REQUIRED_SAMPLE = (
    "timestampMs",
    "availableRamMb",
    "batteryPercent",
    "collectionDurationMs",
)


def _is_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def validate_evidence(record: Any) -> dict[str, Any]:
    errors: list[str] = []

    if not isinstance(record, dict):
        return {"valid": False, "errors": ["evidence must be an object"]}

    for field in _REQUIRED_TOP_LEVEL:
        if field not in record:
            errors.append(f"missing required field: {field}")

    if "schemaVersion" in record:
        if not isinstance(record["schemaVersion"], int) or isinstance(record["schemaVersion"], bool):
            errors.append("schemaVersion must be an integer")
        elif record["schemaVersion"] != SUPPORTED_SCHEMA_VERSION:
            errors.append(f"unsupported schemaVersion: {record['schemaVersion']}")

    if "timestampMs" in record and (
        not isinstance(record["timestampMs"], int)
        or isinstance(record["timestampMs"], bool)
    ):
        errors.append("timestampMs must be an integer")

    if "workload" in record and not isinstance(record["workload"], str):
        errors.append("workload must be a string")

    samples = record.get("samples")
    if not isinstance(samples, list):
        if "samples" in record:
            errors.append("samples must be an array")
    else:
        for index, sample in enumerate(samples):
            if not isinstance(sample, dict):
                errors.append(f"samples[{index}] must be an object")
                continue
            for field in _REQUIRED_SAMPLE:
                if field not in sample:
                    errors.append(f"samples[{index}] missing required field: {field}")
            for field in ("timestampMs", "availableRamMb", "collectionDurationMs"):
                if field in sample and (
                    not isinstance(sample[field], int) or isinstance(sample[field], bool)
                ):
                    errors.append(f"samples[{index}].{field} must be an integer")
            if "availableRamMb" in sample and isinstance(sample["availableRamMb"], int) and sample["availableRamMb"] < 0:
                errors.append(f"samples[{index}].availableRamMb must be >= 0")
            if "collectionDurationMs" in sample and isinstance(sample["collectionDurationMs"], int) and sample["collectionDurationMs"] < 0:
                errors.append(f"samples[{index}].collectionDurationMs must be >= 0")
            battery = sample.get("batteryPercent")
            if battery is not None and (
                not isinstance(battery, int)
                or isinstance(battery, bool)
                or battery < 0
                or battery > 100
            ):
                errors.append(f"samples[{index}].batteryPercent must be null or an integer from 0 to 100")

    device = record.get("device")
    if device is not None:
        if not isinstance(device, dict):
            errors.append("device must be an object")
        else:
            for field in _REQUIRED_DEVICE:
                if field not in device:
                    errors.append(f"device missing required field: {field}")
            if "androidApi" in device and (
                not isinstance(device["androidApi"], int)
                or isinstance(device["androidApi"], bool)
                or device["androidApi"] < 1
            ):
                errors.append("device.androidApi must be a positive integer")
            for field in ("manufacturer", "model"):
                if field in device and not isinstance(device[field], str):
                    errors.append(f"device.{field} must be a string")

    return {
        "valid": not errors,
        "errors": errors,
        "schemaVersion": record.get("schemaVersion"),
    }


def _identity(record: dict[str, Any]) -> tuple[Any, ...] | None:
    device = record.get("device")
    if not isinstance(device, dict):
        return None
    if any(field not in device for field in _REQUIRED_DEVICE):
        return None
    if not all(isinstance(device.get(field), str) for field in ("manufacturer", "model")):
        return None
    if not isinstance(device.get("androidApi"), int) or isinstance(device.get("androidApi"), bool):
        return None
    return (
        device["manufacturer"],
        device["model"],
        device["androidApi"],
        record.get("schemaVersion"),
        record.get("workload"),
    )


def resolve_pair(baseline: dict[str, Any], variant: dict[str, Any]) -> dict[str, Any]:
    base_validation = validate_evidence(baseline)
    variant_validation = validate_evidence(variant)
    if not base_validation["valid"] or not variant_validation["valid"]:
        return {
            "comparable": False,
            "reason": "INVALID_EVIDENCE",
            "baselineErrors": base_validation["errors"],
            "variantErrors": variant_validation["errors"],
        }

    base_device = baseline.get("device", {})
    variant_device = variant.get("device", {})
    if not isinstance(base_device, dict) or not isinstance(variant_device, dict):
        return {"comparable": False, "reason": "INSUFFICIENT_EVIDENCE"}

    if _identity(baseline) is None or _identity(variant) is None:
        return {"comparable": False, "reason": "INSUFFICIENT_EVIDENCE"}

    if (
        base_device["manufacturer"],
        base_device["model"],
        base_device["androidApi"],
    ) != (
        variant_device["manufacturer"],
        variant_device["model"],
        variant_device["androidApi"],
    ):
        return {"comparable": False, "reason": "INCOMPATIBLE_DEVICE"}

    if baseline.get("workload") != variant.get("workload"):
        return {"comparable": False, "reason": "INCOMPATIBLE_WORKLOAD"}

    if baseline.get("schemaVersion") != variant.get("schemaVersion"):
        return {"comparable": False, "reason": "INCOMPATIBLE_SCHEMA"}

    return {
        "comparable": True,
        "baseline": deepcopy(baseline),
        "variant": deepcopy(variant),
        "reason": "COMPARABLE",
    }


def _average(samples: list[dict[str, Any]], field: str) -> float | None:
    values = [
        sample[field]
        for sample in samples
        if isinstance(sample.get(field), (int, float))
        and not isinstance(sample.get(field), bool)
    ]
    return sum(values) / len(values) if values else None


def _first_last(samples: list[dict[str, Any]], field: str) -> tuple[Any, Any]:
    values = [sample.get(field) for sample in samples if field in sample]
    return (values[0], values[-1]) if values else (None, None)


def _percent_change(baseline: float | None, variant: float | None) -> float | None:
    if baseline in (None, 0) or variant is None:
        return None
    return ((variant - baseline) / baseline) * 100.0


def compare_pair(pair: dict[str, Any]) -> dict[str, Any]:
    if not pair.get("comparable"):
        return {
            "comparable": False,
            "reason": pair.get("reason", "INSUFFICIENT_EVIDENCE"),
        }

    baseline = pair["baseline"]
    variant = pair["variant"]
    base_samples = baseline["samples"]
    variant_samples = variant["samples"]

    base_ram = _average(base_samples, "availableRamMb")
    variant_ram = _average(variant_samples, "availableRamMb")

    base_pss = _average(base_samples, "pssKb")
    variant_pss = _average(variant_samples, "pssKb")

    base_cpu_start, base_cpu_end = _first_last(base_samples, "processCpuTimeMs")
    variant_cpu_start, variant_cpu_end = _first_last(variant_samples, "processCpuTimeMs")

    base_cpu_delta = (
        base_cpu_end - base_cpu_start
        if _is_number(base_cpu_start) and _is_number(base_cpu_end)
        else None
    )
    variant_cpu_delta = (
        variant_cpu_end - variant_cpu_start
        if _is_number(variant_cpu_start) and _is_number(variant_cpu_end)
        else None
    )

    return {
        "comparable": True,
        "reason": "COMPARABLE",
        "metrics": {
            "availableRamMbPct": _percent_change(base_ram, variant_ram),
            "availableRamMbDelta": (
                variant_ram - base_ram
                if base_ram is not None and variant_ram is not None
                else None
            ),
            "pssKbPct": _percent_change(base_pss, variant_pss),
            "pssKbDelta": (
                variant_pss - base_pss
                if base_pss is not None and variant_pss is not None
                else None
            ),
            "processCpuTimeMsPct": _percent_change(base_cpu_delta, variant_cpu_delta),
            "processCpuTimeMsDelta": (
                variant_cpu_delta - base_cpu_delta
                if base_cpu_delta is not None and variant_cpu_delta is not None
                else None
            ),
        },
    }


def classify_comparison(
    comparison: dict[str, Any],
    rules: dict[str, Any],
) -> dict[str, Any]:
    if not comparison.get("comparable"):
        reason = comparison.get("reason", "INSUFFICIENT_EVIDENCE")
        classification = (
            "INVALID_EVIDENCE" if reason == "INVALID_EVIDENCE" else "INSUFFICIENT_EVIDENCE"
        )
        return {
            "classification": classification,
            "ruleVersion": rules.get("version"),
            "reasons": [reason],
        }

    configured = rules.get("rules", {})
    ram_threshold = float(configured.get("availableRamDecreasePct", -5.0))
    pss_threshold = float(configured.get("pssIncreasePct", 20.0))

    metrics = comparison.get("metrics", {})
    bad_signals: list[str] = []
    good_signals: list[str] = []
    reasons: list[str] = []

    ram = metrics.get("availableRamMbPct")
    if _is_number(ram):
        if ram <= ram_threshold:
            bad_signals.append("available_ram_regression")
            reasons.append(f"availableRamMbPct={ram:.4f} <= {ram_threshold:.4f}")
        elif ram >= abs(ram_threshold):
            good_signals.append("available_ram_improvement")

    pss = metrics.get("pssKbPct")
    if _is_number(pss):
        if pss >= pss_threshold:
            bad_signals.append("pss_regression")
            reasons.append(f"pssKbPct={pss:.4f} >= {pss_threshold:.4f}")
        elif pss <= -pss_threshold:
            good_signals.append("pss_improvement")

    if bad_signals and good_signals:
        classification = "MIXED"
    elif bad_signals:
        classification = "REGRESSION"
    else:
        classification = "NO_REGRESSION"

    return {
        "classification": classification,
        "ruleVersion": rules.get("version"),
        "reasons": reasons or ["no configured regression threshold crossed"],
        "signals": bad_signals + good_signals,
        "badSignals": bad_signals,
        "goodSignals": good_signals,
    }


def _canonical_sha256(record: dict[str, Any]) -> str:
    payload = json.dumps(record, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def create_provenance_record(
    baseline: dict[str, Any],
    variant: dict[str, Any],
    comparison: dict[str, Any],
    classification: dict[str, Any],
    *,
    rules_version: int,
) -> dict[str, Any]:
    baseline_sha = _canonical_sha256(baseline)
    variant_sha = _canonical_sha256(variant)
    analysis_id = _canonical_sha256({
        "baselineSha256": baseline_sha,
        "variantSha256": variant_sha,
        "rulesVersion": rules_version,
        "classification": classification["classification"],
    })
    return {
        "recordType": "evidence-analysis",
        "analysisId": analysis_id,
        "analyzerVersion": ANALYZER_VERSION,
        "schemaVersion": SUPPORTED_SCHEMA_VERSION,
        "rulesVersion": rules_version,
        "classification": classification["classification"],
        "reasons": list(classification.get("reasons", [])),
        "sourceEvidence": {
            "baselineSha256": _canonical_sha256(baseline),
            "variantSha256": _canonical_sha256(variant),
        },
        "comparison": deepcopy(comparison),
    }

    
def append_history_record(path: str, record: dict[str, Any]) -> dict[str, Any]:
    """Append an analysis record without overwriting an existing analysisId."""
    from pathlib import Path

    target = Path(path)
    existing_ids: set[str] = set()
    if target.exists():
        for line in target.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            existing = json.loads(line)
            if existing.get("analysisId"):
                existing_ids.add(existing["analysisId"])

    analysis_id = record.get("analysisId")
    if not isinstance(analysis_id, str) or not analysis_id:
        raise ValueError("history record requires analysisId")
    if analysis_id in existing_ids:
        return {"appended": False, "duplicate": True, "analysisId": analysis_id}

    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(record, sort_keys=True, ensure_ascii=False) + "\n")
    return {"appended": True, "duplicate": False, "analysisId": analysis_id}
