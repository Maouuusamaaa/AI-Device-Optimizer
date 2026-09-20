#!/usr/bin/env python3
import json
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
adapter = ROOT / "android/local-ai/llama-adapter.py"

def test_adapter_is_safety_wrapped():
    with tempfile.TemporaryDirectory() as d:
        fake = pathlib.Path(d) / "fake-cli.py"
        fake.write_text(
            "#!/usr/bin/env python3\n"
            "print('{\"actionId\":\"CLEAR_CACHE\",\"confidence\":0.99,\"abstain\":false}')\n"
        )
        fake.chmod(0o755)
        inp = pathlib.Path(d) / "input.json"
        inp.write_text(json.dumps({"features": {
            "batteryPercent": 44, "temperatureC": 43.2, "availableRamMb": 1800
        }}))
        out = subprocess.run(
            ["python3", str(adapter), "--llama-cli", str(fake),
             "--model", "model.gguf", "--input", str(inp)],
            text=True, capture_output=True, check=True
        )
        result = json.loads(out.stdout)
        assert result["recommendation"]["actionId"] == "OBSERVE_ONLY"
        assert result["recommendation"]["abstain"] is True
        assert result["safety"]["deviceMutationAllowed"] is False

if __name__ == "__main__":
    test_adapter_is_safety_wrapped()
    print("Qwen local adapter tests passed")
