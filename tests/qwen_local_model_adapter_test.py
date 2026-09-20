#!/usr/bin/env python3
import json
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
adapter = ROOT / "android/local-ai/llama-adapter.py"


def run_adapter(fake_cli: pathlib.Path, payload: object, *extra: str):
    inp = fake_cli.parent / "input.json"
    inp.write_text(json.dumps(payload), encoding="utf-8")
    return subprocess.run(
        ["python3", str(adapter), "--llama-cli", str(fake_cli),
         "--model", "model.gguf", "--input", str(inp), *extra],
        text=True, capture_output=True
    )


def test_adapter_is_safety_wrapped():
    with tempfile.TemporaryDirectory() as d:
        root = pathlib.Path(d)
        fake = root / "fake-cli.py"
        fake.write_text(
            '''#!/usr/bin/env python3
print('{"actionId":"CLEAR_CACHE","confidence":0.99,"abstain":false}')
''',
            encoding="utf-8",
        )
        fake.chmod(0o755)
        out = run_adapter(fake, {"features": {
            "batteryPercent": 44, "temperatureC": 43.2, "availableRamMb": 1800
        }})
        assert out.returncode == 0, out.stderr
        result = json.loads(out.stdout)
        assert result["recommendation"]["actionId"] == "OBSERVE_ONLY"
        assert result["recommendation"]["abstain"] is True
        assert result["safety"]["deviceMutationAllowed"] is False
        assert result["modelOutputValid"] is True


def test_malformed_model_output_stays_safe():
    with tempfile.TemporaryDirectory() as d:
        root = pathlib.Path(d)
        fake = root / "fake-cli.py"
        fake.write_text(
            '''#!/usr/bin/env python3
print("not json")
''',
            encoding="utf-8",
        )
        fake.chmod(0o755)
        out = run_adapter(fake, {"features": {
            "batteryPercent": 50, "temperatureC": 40, "availableRamMb": 2000
        }})
        assert out.returncode == 0, out.stderr
        result = json.loads(out.stdout)
        assert result["recommendation"]["actionId"] == "OBSERVE_ONLY"
        assert result["recommendation"]["abstain"] is True
        assert result["modelOutputValid"] is False


def test_invalid_telemetry_does_not_invoke_model():
    with tempfile.TemporaryDirectory() as d:
        root = pathlib.Path(d)
        fake = root / "must-not-run.py"
        fake.write_text(
            '''#!/usr/bin/env python3
raise SystemExit(99)
''',
            encoding="utf-8",
        )
        fake.chmod(0o755)
        out = run_adapter(fake, {"features": {
            "batteryPercent": 140, "temperatureC": 40, "availableRamMb": 2000
        }})
        assert out.returncode == 2
        result = json.loads(out.stdout)
        assert result["recommendation"]["abstain"] is True
        assert result["safety"]["deviceMutationAllowed"] is False


def test_resource_bounds_are_rejected():
    with tempfile.TemporaryDirectory() as d:
        root = pathlib.Path(d)
        fake = root / "must-not-run.py"
        fake.write_text(
            '''#!/usr/bin/env python3
raise SystemExit(99)
''',
            encoding="utf-8",
        )
        fake.chmod(0o755)
        payload = {"features": {
            "batteryPercent": 50, "temperatureC": 40, "availableRamMb": 2000
        }}
        out = run_adapter(fake, payload, "--context", "999999")
        assert out.returncode == 2
        result = json.loads(out.stdout)
        assert result["recommendation"]["abstain"] is True


if __name__ == "__main__":
    test_adapter_is_safety_wrapped()
    test_malformed_model_output_stays_safe()
    test_invalid_telemetry_does_not_invoke_model()
    test_resource_bounds_are_rejected()
    print("Qwen local adapter tests passed")
