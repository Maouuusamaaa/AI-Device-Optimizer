import json
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/analyze-local-inference-benchmark.py"


def main():
    payload = {
        "schemaVersion": 1,
        "benchmarkId": "synthetic-fixture-not-device-evidence",
        "model": {"modelId": "fixture", "runtimeVersion": "fixture"},
        "samples": [
            {
                "observationId": "a",
                "threadCount": 2,
                "repetition": 1,
                "wallTimeMs": 1000,
                "processCpuTimeMs": 900,
                "processPssBeforeKb": 100,
                "processPssAfterKb": 120,
                "processPssDeltaKb": 20,
                "promptTokens": 10,
                "generatedTokens": 20,
                "loadMs": 500,
                "tokenizationMs": 5,
                "contextInitMs": 10,
                "promptDecodeMs": 100,
                "generationMs": 300,
                "generationTokensPerSecond": 66.0,
                "totalNativeMs": 915,
                "temperatureBeforeC": 35.0,
                "temperatureAfterC": 35.5,
                "batteryBeforePercent": 50,
                "batteryAfterPercent": 49,
                "modelOutputContainsThink": False,
                "advisoryOnly": True,
                "executionRequested": False,
                "deviceMutationAllowed": False,
            },
            {
                "observationId": "b",
                "threadCount": 4,
                "repetition": 1,
                "wallTimeMs": 800,
                "processCpuTimeMs": 700,
                "processPssBeforeKb": 100,
                "processPssAfterKb": 130,
                "processPssDeltaKb": 30,
                "promptTokens": 10,
                "generatedTokens": 20,
                "loadMs": 450,
                "tokenizationMs": 5,
                "contextInitMs": 10,
                "promptDecodeMs": 90,
                "generationMs": 240,
                "generationTokensPerSecond": 83.333,
                "totalNativeMs": 795,
                "temperatureBeforeC": 35.0,
                "temperatureAfterC": 36.0,
                "batteryBeforePercent": 50,
                "batteryAfterPercent": 49,
                "modelOutputContainsThink": True,
                "advisoryOnly": True,
                "executionRequested": False,
                "deviceMutationAllowed": False,
            },
        ],
    }

    with tempfile.TemporaryDirectory() as tmp:
        source = Path(tmp) / "fixture.json"
        source.write_text(json.dumps(payload), encoding="utf-8")
        completed = subprocess.run(
            [sys.executable, str(SCRIPT), str(source)],
            capture_output=True,
            text=True,
            check=True,
        )
        result = json.loads(completed.stdout)

    assert result["sampleCount"] == 2
    assert result["threadConfigurations"] == [2, 4]
    assert result["safetyContractIntact"] is True
    assert result["byThreadConfiguration"]["2"]["sampleCount"] == 1
    assert result["byThreadConfiguration"]["4"]["sampleCount"] == 1
    assert result["byThreadConfiguration"]["2"]["generationTokensPerSecond"]["mean"] == 66.0
    print("Stage 9 benchmark analyzer contract: PASS")


if __name__ == "__main__":
    main()
