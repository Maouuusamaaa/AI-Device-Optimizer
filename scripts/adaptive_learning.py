#!/usr/bin/env python3
"""Deterministic local adaptive-learning foundation.

This module consumes validated learning records derived from existing evidence
and evaluation layers. It maintains a bounded, persistent-friendly knowledge
state and produces advisory candidates only. It never executes actions,
selects policies, changes safety rules, or requires network access.
"""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
from copy import deepcopy
from pathlib import Path
from typing import Any, Callable

FEATURE_EXTRACTOR_VERSION = 1
LEARNER_SCHEMA_VERSION = 1
LEARNER_VERSION = 1
MINIMUM_SAMPLE_COUNT = 3
SUPPORTED_CLASSIFICATIONS = {
    "NO_REGRESSION",
    "REGRESSION",
    "MIXED",
    "INSUFFICIENT_EVIDENCE",
}
LEARNABLE_CLASSIFICATIONS = {"NO_REGRESSION", "REGRESSION", "MIXED"}


class LearningError(ValueError):
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
        raise LearningError(f"{field} must be a non-empty string")
    return value


def extract_features(record: dict[str, Any]) -> dict[str, Any]:
    features = record.get("features")
    if not isinstance(features, dict):
        raise LearningError("features must be an object")

    allowed = (
        "availableRamMbPct",
        "availableRamMbDelta",
        "pssKbPct",
        "pssKbDelta",
        "processCpuTimeMsPct",
        "processCpuTimeMsDelta",
    )
    extracted: dict[str, Any] = {}
    for name in allowed:
        value = features.get(name)
        if value is None:
            continue
        if not isinstance(value, (int, float)) or isinstance(value, bool):
            raise LearningError(f"features.{name} must be numeric when present")
        extracted[name] = value
    return extracted


def create_learning_record(evaluation_record: dict[str, Any]) -> dict[str, Any]:
    """Validate and normalize one already-classified evaluation record."""
    source = deepcopy(evaluation_record)
    if not isinstance(source, dict):
        raise LearningError("learning input must be an object")
    if source.get("schemaVersion") != LEARNER_SCHEMA_VERSION:
        raise LearningError("learning record schemaVersion must be 1")

    evidence_id = _require_string(source.get("evidenceId"), "evidenceId")
    classification = _require_string(source.get("classification"), "classification")
    if classification not in SUPPORTED_CLASSIFICATIONS:
        raise LearningError(f"unsupported classification: {classification}")
    if classification == "INVALID_EVIDENCE":
        raise LearningError("INVALID_EVIDENCE cannot enter learning")

    device = source.get("device")
    if not isinstance(device, dict):
        raise LearningError("device must be an object")
    manufacturer = _require_string(device.get("manufacturer"), "device.manufacturer")
    model = _require_string(device.get("model"), "device.model")
    api = device.get("androidApi")
    if not isinstance(api, int) or isinstance(api, bool) or api < 1:
        raise LearningError("device.androidApi must be a positive integer")

    workload = _require_string(source.get("workload"), "workload")
    policy_id = source.get("policyId")
    if policy_id is not None:
        policy_id = _require_string(policy_id, "policyId")

    provenance = source.get("provenance")
    if not isinstance(provenance, dict) or not provenance:
        raise LearningError("provenance must be a non-empty object")

    features = extract_features(source)
    disposition = "ABSTAIN" if classification == "INSUFFICIENT_EVIDENCE" else "LEARN"

    record = {
        "schemaVersion": LEARNER_SCHEMA_VERSION,
        "featureExtractorVersion": FEATURE_EXTRACTOR_VERSION,
        "learnerVersion": LEARNER_VERSION,
        "evidenceId": evidence_id,
        "device": {
            "androidApi": api,
            "manufacturer": manufacturer,
            "model": model,
        },
        "workload": workload,
        "policyId": policy_id,
        "classification": classification,
        "features": features,
        "provenance": deepcopy(provenance),
        "learningDisposition": disposition,
    }
    record["recordFingerprint"] = _canonical_sha256(record)
    return record


def _pattern_key(record: dict[str, Any]) -> tuple[Any, ...]:
    device = record["device"]
    return (
        device["manufacturer"],
        device["model"],
        device["androidApi"],
        record["workload"],
        record.get("policyId"),
    )


def empty_knowledge_state() -> dict[str, Any]:
    state = {
        "schemaVersion": LEARNER_SCHEMA_VERSION,
        "learnerVersion": LEARNER_VERSION,
        "featureExtractorVersion": FEATURE_EXTRACTOR_VERSION,
        "processedEvidenceIds": [],
        "processedEvidenceCount": 0,
        "abstentionCount": 0,
        "patterns": [],
    }
    state["stateFingerprint"] = _canonical_sha256(state)
    return state


def _sorted_unique(values: list[str]) -> list[str]:
    return sorted(set(values))


def _recompute_fingerprint(state: dict[str, Any]) -> dict[str, Any]:
    output = deepcopy(state)
    output.pop("stateFingerprint", None)
    output["processedEvidenceIds"] = _sorted_unique(output["processedEvidenceIds"])
    output["patterns"] = sorted(
        output["patterns"],
        key=lambda p: (
            p["device"]["manufacturer"],
            p["device"]["model"],
            p["device"]["androidApi"],
            p["workload"],
            p.get("policyId") or "",
        ),
    )
    for pattern in output["patterns"]:
        pattern["supportingEvidenceIds"] = _sorted_unique(pattern["supportingEvidenceIds"])
        pattern["classifications"] = sorted(set(pattern["classifications"]))
        pattern["featureAverages"] = dict(sorted(pattern["featureAverages"].items()))
    output["stateFingerprint"] = _canonical_sha256(output)
    return output


def update_knowledge_state(
    state: dict[str, Any],
    records: list[dict[str, Any]],
    *,
    minimum_sample_count: int = MINIMUM_SAMPLE_COUNT,
) -> dict[str, Any]:
    """Apply new records idempotently and return a deterministic state."""
    if not isinstance(state, dict):
        raise LearningError("knowledge state must be an object")
    if state.get("schemaVersion") != LEARNER_SCHEMA_VERSION:
        raise LearningError("unsupported knowledge state schemaVersion")
    if not isinstance(minimum_sample_count, int) or minimum_sample_count < 1:
        raise LearningError("minimum_sample_count must be a positive integer")

    output = deepcopy(state)
    output.pop("stateFingerprint", None)
    output.setdefault("processedEvidenceIds", [])
    output.setdefault("processedEvidenceCount", len(output["processedEvidenceIds"]))
    output.setdefault("abstentionCount", 0)
    output.setdefault("patterns", [])

    processed = set(output["processedEvidenceIds"])
    pattern_map: dict[tuple[Any, ...], dict[str, Any]] = {
        _pattern_key(pattern): pattern for pattern in output["patterns"]
    }

    for record in records:
        normalized = create_learning_record(record) if "recordFingerprint" not in record else deepcopy(record)
        evidence_id = _require_string(normalized.get("evidenceId"), "evidenceId")
        if evidence_id in processed:
            continue
        processed.add(evidence_id)
        output["processedEvidenceCount"] += 1

        if normalized["learningDisposition"] == "ABSTAIN":
            output["abstentionCount"] += 1
            continue

        key = _pattern_key(normalized)
        pattern = pattern_map.get(key)
        if pattern is None:
            device = normalized["device"]
            pattern = {
                "device": deepcopy(device),
                "workload": normalized["workload"],
                "policyId": normalized.get("policyId"),
                "sampleCount": 0,
                "classifications": [],
                "supportingEvidenceIds": [],
                "featureAverages": {},
                "eligible": False,
            }
            pattern_map[key] = pattern
            output["patterns"].append(pattern)

        pattern["sampleCount"] += 1
        pattern["classifications"].append(normalized["classification"])
        pattern["supportingEvidenceIds"].append(evidence_id)

        for name, value in normalized["features"].items():
            current = pattern["featureAverages"].get(name)
            if current is None:
                pattern["featureAverages"][name] = value
            else:
                n = pattern["sampleCount"]
                pattern["featureAverages"][name] = ((current * (n - 1)) + value) / n

    for pattern in output["patterns"]:
        unique_classes = set(pattern["classifications"])
        pattern["eligible"] = (
            pattern["sampleCount"] >= minimum_sample_count
            and len(unique_classes) == 1
            and next(iter(unique_classes)) == "NO_REGRESSION"
            and bool(pattern["supportingEvidenceIds"])
        )

    output["processedEvidenceIds"] = sorted(processed)
    return _recompute_fingerprint(output)


def generate_candidates(state: dict[str, Any]) -> list[dict[str, Any]]:
    """Generate advisory candidates; never authorizes execution or selection."""
    if not isinstance(state, dict) or state.get("schemaVersion") != LEARNER_SCHEMA_VERSION:
        raise LearningError("unsupported knowledge state")
    candidates: list[dict[str, Any]] = []
    for pattern in state.get("patterns", []):
        if not pattern.get("eligible"):
            continue
        candidate = {
            "policyId": pattern.get("policyId"),
            "reason": "descriptive candidate supported by consistent local evidence",
            "knowledgeVersion": state["stateFingerprint"],
            "evidenceCount": pattern["sampleCount"],
            "supportingEvidenceIds": list(pattern["supportingEvidenceIds"]),
            "provenance": {
                "learnerVersion": LEARNER_VERSION,
                "featureExtractorVersion": FEATURE_EXTRACTOR_VERSION,
                "knowledgeStateFingerprint": state["stateFingerprint"],
            },
        }
        candidates.append(candidate)
    return candidates


def validate_knowledge_state(state: dict[str, Any]) -> dict[str, Any]:
    if not isinstance(state, dict):
        raise LearningError("knowledge state must be an object")
    if state.get("schemaVersion") != LEARNER_SCHEMA_VERSION:
        raise LearningError("unsupported knowledge state schemaVersion")
    expected = deepcopy(state)
    fingerprint = expected.pop("stateFingerprint", None)
    expected = _recompute_fingerprint(expected)
    if fingerprint != expected["stateFingerprint"]:
        raise LearningError("knowledge state fingerprint mismatch")
    return deepcopy(expected)


def load_knowledge_state(path: Path) -> dict[str, Any]:
    target = Path(path)
    try:
        payload = json.loads(target.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise LearningError(f"cannot load knowledge state: {exc}") from exc
    return validate_knowledge_state(payload)


def save_knowledge_state(path: Path, state: dict[str, Any]) -> None:
    target = Path(path)
    validated = validate_knowledge_state(state)
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=f".{target.name}.", dir=target.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(validated, handle, ensure_ascii=False, indent=2, sort_keys=True)
            handle.write("\n")
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp_name, target)
    except OSError as exc:
        try:
            os.unlink(temp_name)
        except OSError:
            pass
        raise LearningError(f"cannot save knowledge state: {exc}") from exc


def process_incremental(
    state: dict[str, Any],
    records: list[dict[str, Any]],
    *,
    resource_available: Callable[[], bool] | None = None,
) -> dict[str, Any]:
    """Process a bounded local batch when the supplied resource guard permits it."""
    if resource_available is not None and not resource_available():
        return deepcopy(state)
    return update_knowledge_state(state, records)
