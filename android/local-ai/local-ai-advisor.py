#!/usr/bin/env python3
"""Dependency-free local advisory model.

This is a reference inference backend, not a learned model. It establishes the
stable on-device interface that a compact learned model can implement later.
It never executes device actions.
"""
from __future__ import annotations
import json
import sys

REQUIRED = ("batteryPercent", "temperatureC", "availableRamMb")

def recommend(features: dict) -> dict:
    missing = [k for k in REQUIRED if k not in features]
    if missing:
        return {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.0,
            "abstain": True,
            "reason": "Missing required telemetry: " + ", ".join(missing),
        }

    temp = float(features["temperatureC"])
    battery = float(features["batteryPercent"])
    ram = float(features["availableRamMb"])

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
    payload = json.load(sys.stdin)
    features = payload.get("features", payload)
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
    json.dump(result, sys.stdout, indent=2)
    sys.stdout.write("\n")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
