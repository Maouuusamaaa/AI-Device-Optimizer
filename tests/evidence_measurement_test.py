#!/usr/bin/env python3
"""Contract tests for the 0.1.14 evidence/measurement boundary."""

from __future__ import annotations

import copy
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))

from evidence_measurement import (  # noqa: E402
    CLASSIFICATIONS,
    classify_comparison,
    compare_pair,
    create_provenance_record,
    resolve_pair,
    validate_evidence,
    append_history_record,
)


def evidence(workload="idle", model="P661N", api=33, pss=10000, timestamp=1000, schema_version=1):
    return {
        "schemaVersion": schema_version,
        "timestampMs": timestamp,
        "workload": workload,
        "device": {"androidApi": api, "manufacturer": "itel", "model": model},
        "samples": [
            {
                "timestampMs": timestamp,
                "availableRamMb": 1800,
                "batteryPercent": 50,
                "collectionDurationMs": 10,
                "processCpuTimeMs": 100,
                "pssKb": pss,
            },
            {
                "timestampMs": timestamp + 2000,
                "availableRamMb": 1790,
                "batteryPercent": 49,
                "collectionDurationMs": 11,
                "processCpuTimeMs": 110,
                "pssKb": pss + 100,
            },
        ],
    }


def test_valid_contract():
    result = validate_evidence(evidence())
    assert result["valid"] is True
    assert result["errors"] == []

    result = validate_evidence(evidence(schema_version=2))
    assert result["valid"] is True
    assert result["errors"] == []


def test_invalid_contracts_fail_closed():
    invalid = evidence()
    del invalid["schemaVersion"]
    result = validate_evidence(invalid)
    assert result["valid"] is False
    assert any("schemaVersion" in error for error in result["errors"])

    unsupported = evidence()
    unsupported["schemaVersion"] = 999
    result = validate_evidence(unsupported)
    assert result["valid"] is False

    malformed = evidence()
    malformed["samples"][0]["availableRamMb"] = True
    result = validate_evidence(malformed)
    assert result["valid"] is False


def test_pairing_requires_comparable_identity():
    base = evidence()
    variant = evidence(timestamp=3000)
    decision = resolve_pair(base, variant)
    assert decision["comparable"] is True

    different_device = evidence(model="other-device", timestamp=3000)
    assert resolve_pair(base, different_device)["reason"] == "INCOMPATIBLE_DEVICE"

    different_workload = evidence(workload="gaming", timestamp=3000)
    assert resolve_pair(base, different_workload)["reason"] == "INCOMPATIBLE_WORKLOAD"

    missing_identity = evidence(timestamp=3000)
    del missing_identity["device"]["model"]
    assert resolve_pair(base, missing_identity)["reason"] == "INSUFFICIENT_EVIDENCE"


def test_comparison_does_not_mutate_inputs():
    base = evidence()
    variant = evidence(timestamp=3000, pss=11000)
    before_base = copy.deepcopy(base)
    before_variant = copy.deepcopy(variant)

    decision = resolve_pair(base, variant)
    comparison = compare_pair(decision)
    assert comparison["comparable"] is True
    assert base == before_base
    assert variant == before_variant


def test_classifier_fail_closed_and_deterministic():
    assert set(CLASSIFICATIONS) == {
        "NO_REGRESSION",
        "REGRESSION",
        "MIXED",
        "INSUFFICIENT_EVIDENCE",
        "INVALID_EVIDENCE",
    }

    assert classify_comparison({"comparable": False}, {})["classification"] == "INSUFFICIENT_EVIDENCE"
    assert classify_comparison({"comparable": True, "metrics": {"availableRamMbPct": -1}}, {"version": 1, "rules": {}})["classification"] == "NO_REGRESSION"
    assert classify_comparison({"comparable": True, "metrics": {"availableRamMbPct": -6, "pssKbPct": 25}}, {"version": 1, "rules": {}})["classification"] == "REGRESSION"
    assert classify_comparison({"comparable": True, "metrics": {"availableRamMbPct": -6, "pssKbPct": -25}}, {"version": 1, "rules": {}})["classification"] == "MIXED"


def test_provenance_is_audit_record_only():
    base = evidence()
    variant = evidence(timestamp=3000, pss=11000)
    pair = resolve_pair(base, variant)
    comparison = compare_pair(pair)
    classification = classify_comparison(comparison, {"version": 1, "rules": {}})
    record = create_provenance_record(base, variant, comparison, classification, rules_version=1)

    assert record["recordType"] == "evidence-analysis"
    assert record["schemaVersion"] == 1
    assert record["analysisId"]
    assert record["classification"] == classification["classification"]
    assert record["sourceEvidence"]["baselineSha256"]
    assert record["sourceEvidence"]["variantSha256"]
    assert "action" not in record
    assert "command" not in record
    assert "authorization" not in record

    history_path = Path(__file__).resolve().parent / "_tmp_evidence_history.jsonl"
    try:
        first = append_history_record(str(history_path), record)
        second = append_history_record(str(history_path), record)
        assert first["appended"] is True
        assert second["duplicate"] is True
        assert len(history_path.read_text(encoding="utf-8").splitlines()) == 1
    finally:
        history_path.unlink(missing_ok=True)


if __name__ == "__main__":
    tests = [
        test_valid_contract,
        test_invalid_contracts_fail_closed,
        test_pairing_requires_comparable_identity,
        test_comparison_does_not_mutate_inputs,
        test_classifier_fail_closed_and_deterministic,
        test_provenance_is_audit_record_only,
    ]
    for test in tests:
        test()
        print(f"PASS {test.__name__}")
    print(f"PASS {len(tests)} evidence measurement tests")
