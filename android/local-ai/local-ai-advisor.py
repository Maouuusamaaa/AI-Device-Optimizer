#!/usr/bin/env python3
"""Dependency-free local advisory model.

This is a reference inference backend, not a learned model. It establishes the
stable on-device interface that a compact learned model can implement later.
It never executes device actions.
"""
from __future__ import annotations
import json
import math
import sys

REQUIRED = ("batteryPercent", "temperatureC", "availableRamMb")

def _number(features: dict, key: str):
    try:
        value = float(features[key])
    except (KeyError, TypeError, ValueError):
        return None
    return value if math.isfinite(value) else None

def recommend(features: dict) -> dict:
    if not isinstance(features, dict):
        return {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.0,
            "abstain": True,
            "reason": "Telemetry features must be an object.",
        }

    missing = [k for k in REQUIRED if k not in features]
    if missing:
        return {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.0,
            "abstain": True,
            "reason": "Missing required telemetry: " + ", ".join(missing),
        }

    battery = _number(features, "batteryPercent")
    temp = _number(features, "temperatureC")
    ram = _number(features, "availableRamMb")
    if battery is None or temp is None or ram is None:
        return {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.0,
            "abstain": True,
            "reason": "Required telemetry must contain finite numeric values.",
        }

    if not 0.0 <= battery <= 100.0 or temp < -50.0 or temp > 100.0 or ram < 0.0:
        return {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.0,
            "abstain": True,
            "reason": "Telemetry value is outside the accepted validation range.",
        }

    if temp >= 45.0:
        return {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.80,
            "abstain": True,
            "reason": "Elevated temperature requires observation; no local mutation is authorized.",
        }
    if battery <= 10.0:
        return {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.75,
            "abstain": True,
            "reason": "Low battery requires observation; no local mutation is authorized.",
        }
    if ram < 512.0:
        return {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.70,
            "abstain": True,
            "reason": "Low available RAM requires observation; no local mutation is authorized.",
        }
    return {
        "actionId": "OBSERVE_ONLY",
        "confidence": 0.60,
        "abstain": True,
        "reason": "Telemetry is within the reference observation range.",
    }

def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, UnicodeDecodeError) as exc:
        json.dump({
            "modelId": "local-reference-advisor-v1",
            "recommendation": {
                "actionId": "OBSERVE_ONLY",
                "confidence": 0.0,
                "abstain": True,
                "reason": f"Invalid JSON input: {exc}",
            },
            "safety": {
                "advisoryOnly": True,
                "executionRequested": False,
                "deviceMutationAllowed": False,
                "localSafetyGateRequired": True,
            },
        }, sys.stdout, indent=2)
        sys.stdout.write("\n")
        return 0

    features = payload.get("features", payload) if isinstance(payload, dict) else payload
    result = {
        "modelId": "local-reference-advisor-v1",
        "recommendation": recommend(features),
        "safety": {
            "advisoryOnly": True,
            "executionRequested": False,
            "deviceMutationAllowed": False,
            "localSafetyGateRequired": True,
        },
    }
    json.dump(result, sys.stdout, indent=2, allow_nan=False)
    sys.stdout.write("\n")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
