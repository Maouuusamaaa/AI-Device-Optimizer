#!/usr/bin/env python3
import importlib.util
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
path = ROOT / "android/local-ai/local-ai-advisor.py"
spec = importlib.util.spec_from_file_location("local_ai_advisor", path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

def test_normal_telemetry_abstains():
    result = module.recommend({"batteryPercent": 50, "temperatureC": 35, "availableRamMb": 1800})
    assert result["actionId"] == "OBSERVE_ONLY"
    assert result["abstain"] is True

def test_missing_telemetry_abstains():
    result = module.recommend({"batteryPercent": 50})
    assert result["abstain"] is True
    assert result["confidence"] == 0.0

def test_safety_invariant_is_static():
    sample = module.recommend({"batteryPercent": 20, "temperatureC": 46, "availableRamMb": 1000})
    assert sample["actionId"] == "OBSERVE_ONLY"
    assert sample["abstain"] is True

if __name__ == "__main__":
    test_normal_telemetry_abstains()
    test_missing_telemetry_abstains()
    test_safety_invariant_is_static()
    print("local AI tests passed")
