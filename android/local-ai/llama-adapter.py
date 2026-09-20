#!/usr/bin/env python3
"""Run a local GGUF advisor through llama.cpp without granting action authority."""
from __future__ import annotations
import argparse
import json
import pathlib
import subprocess

def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--llama-cli", required=True)
    p.add_argument("--model", required=True)
    p.add_argument("--input", required=True)
    p.add_argument("--context", type=int, default=4096)
    p.add_argument("--max-tokens", type=int, default=128)
    a = p.parse_args()

    payload = json.loads(pathlib.Path(a.input).read_text())
    features = payload.get("features", payload)
    prompt = (
        "Return ONLY JSON with actionId, confidence, abstain, reason. "
        "actionId must be OBSERVE_ONLY; abstain must be true; never request "
        "device mutation. Telemetry: "
        + json.dumps(features, separators=(",", ":"), allow_nan=False)
    )
    proc = subprocess.run(
        [a.llama_cli, "-m", a.model, "-c", str(a.context),
         "-n", str(a.max_tokens), "--temp", "0", "-p", prompt],
        text=True, capture_output=True, check=True
    )
    result = {
        "modelId": "qwen3-0.6b-q4_0",
        "recommendation": {
            "actionId": "OBSERVE_ONLY",
            "confidence": 0.0,
            "abstain": True,
            "reason": "Model output is advisory-only and requires structured validation."
        },
        "modelOutput": proc.stdout.strip(),
        "safety": {
            "advisoryOnly": True,
            "executionRequested": False,
            "deviceMutationAllowed": False,
            "localSafetyGateRequired": True
        }
    }
    print(json.dumps(result, indent=2, ensure_ascii=False, allow_nan=False))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
