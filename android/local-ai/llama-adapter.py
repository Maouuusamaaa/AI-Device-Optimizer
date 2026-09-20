#!/usr/bin/env python3
"""Run a local GGUF advisor through llama.cpp without granting action authority."""
from __future__ import annotations

import argparse
import json
import math
import pathlib
import subprocess

MODEL_ID = "qwen3-0.6b-q4_0"
MIN_CONTEXT = 256
MAX_CONTEXT = 8192
MIN_MAX_TOKENS = 1
MAX_MAX_TOKENS = 512
SUBPROCESS_TIMEOUT_SECONDS = 120
MAX_MODEL_OUTPUT_CHARS = 16384


def finite_number(value: object) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def validate_features(features: object) -> tuple[bool, str]:
    if not isinstance(features, dict):
        return False, "features must be an object"

    required = ("batteryPercent", "temperatureC", "availableRamMb")
    for key in required:
        if key not in features:
            return False, f"missing feature: {key}"
        if not finite_number(features[key]):
            return False, f"feature must be finite numeric: {key}"

    if not 0 <= float(features["batteryPercent"]) <= 100:
        return False, "batteryPercent out of range"
    if not -20 <= float(features["temperatureC"]) <= 100:
        return False, "temperatureC out of range"
    if float(features["availableRamMb"]) < 0:
        return False, "availableRamMb must be non-negative"
    return True, ""


def safe_result(reason: str, model_output: str = "", model_output_valid: bool = False) -> dict:
    return {
        "modelId": MODEL_ID,
        "recommendation": {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.0,
            "abstain": True,
            "reason": reason,
        },
        "modelOutput": model_output[:MAX_MODEL_OUTPUT_CHARS],
        "modelOutputValid": model_output_valid,
        "safety": {
            "advisoryOnly": True,
            "executionRequested": False,
            "deviceMutationAllowed": False,
            "localSafetyGateRequired": True,
        },
    }


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--llama-cli", required=True)
    p.add_argument("--model", required=True)
    p.add_argument("--input", required=True)
    p.add_argument("--context", type=int, default=4096)
    p.add_argument("--max-tokens", type=int, default=128)
    a = p.parse_args()

    if not MIN_CONTEXT <= a.context <= MAX_CONTEXT:
        print(json.dumps(safe_result("context is outside the supported safety bounds"), allow_nan=False))
        return 2
    if not MIN_MAX_TOKENS <= a.max_tokens <= MAX_MAX_TOKENS:
        print(json.dumps(safe_result("max-tokens is outside the supported safety bounds"), allow_nan=False))
        return 2

    try:
        payload = json.loads(pathlib.Path(a.input).read_text(encoding="utf-8"))
        features = payload.get("features", payload) if isinstance(payload, dict) else payload
        valid, error = validate_features(features)
        if not valid:
            print(json.dumps(safe_result(f"invalid telemetry: {error}"), allow_nan=False))
            return 2
        telemetry = json.dumps(features, separators=(",", ":"), allow_nan=False)
    except (OSError, json.JSONDecodeError, TypeError, ValueError) as exc:
        print(json.dumps(safe_result(f"invalid input: {exc}"), allow_nan=False))
        return 2

    prompt = (
        "Return ONLY JSON with actionId, confidence, abstain, reason. "
        "actionId must be OBSERVE_ONLY; abstain must be true; never request "
        "device mutation. Telemetry: " + telemetry
    )

    try:
        proc = subprocess.run(
            [
                a.llama_cli, "-m", a.model, "-c", str(a.context),
                "-n", str(a.max_tokens), "--temp", "0", "-p", prompt,
            ],
            text=True,
            capture_output=True,
            check=True,
            timeout=SUBPROCESS_TIMEOUT_SECONDS,
        )
    except (OSError, subprocess.CalledProcessError, subprocess.TimeoutExpired) as exc:
        print(json.dumps(safe_result(f"llama.cpp invocation failed: {type(exc).__name__}"), allow_nan=False))
        return 3

    raw_output = proc.stdout.strip()
    # The model output is deliberately treated as untrusted text. We only
    # record whether it is a JSON object; no model field can grant authority.
    model_output_valid = False
    try:
        parsed = json.loads(raw_output)
        model_output_valid = isinstance(parsed, dict)
    except json.JSONDecodeError:
        pass

    result = safe_result(
        "Model output is advisory-only; local Safety Gate remains authoritative.",
        raw_output,
        model_output_valid,
    )
    print(json.dumps(result, indent=2, ensure_ascii=False, allow_nan=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
