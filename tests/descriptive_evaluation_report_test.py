import json
import subprocess
import sys
import tempfile
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "descriptive-evaluation-report.py"


class DescriptiveEvaluationReportTest(unittest.TestCase):
    def run_report(self, records):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            dataset = root / "dataset.json"
            output = root / "report.json"
            dataset.write_text(json.dumps(records), encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(SCRIPT), str(dataset), "--out-json", str(output)],
                capture_output=True, text=True
            )
            return result, json.loads(output.read_text(encoding="utf-8")) if output.exists() else None

    def record(self, oid, timestamp, outcome=None):
        return {
            "observationId": oid,
            "timestampMs": timestamp,
            "conditionIds": ["device.normal"],
            "actionIds": [],
            "metadata": {
                "charging": False,
                "batteryTemperatureC": 35.0,
                "thermalStatus": 0,
                "networkTransport": "wifi",
                "networkValidated": True,
                "interactive": True,
                "workload": "idle",
            },
            "outcomeLabel": outcome,
        }

    def test_report_is_deterministic_and_non_authorizing(self):
        records = [self.record("a", 1), self.record("b", 2, "no_change")]
        first, report = self.run_report(records)
        second, report2 = self.run_report(records)
        self.assertEqual(first.returncode, 0)
        self.assertEqual(second.returncode, 0)
        self.assertEqual(report["datasetFingerprint"], report2["datasetFingerprint"])
        self.assertEqual(report["observationCount"], 2)
        self.assertEqual(report["outcomeLabelCoverage"], {"no_change": 1})
        self.assertTrue(report["measurementOnly"])
        self.assertFalse(report["policySelectionAllowed"])
        self.assertFalse(report["executionAllowed"])

    def test_duplicate_ids_are_rejected(self):
        result, _ = self.run_report([self.record("a", 1), self.record("a", 2)])
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("duplicate observationId", result.stderr)

    def test_non_chronological_records_are_rejected(self):
        result, _ = self.run_report([self.record("a", 2), self.record("b", 1)])
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("chronological", result.stderr)

    def test_non_finite_temperature_is_rejected(self):
        records = [self.record("a", 1)]
        records[0]["metadata"]["batteryTemperatureC"] = float("nan")
        result, _ = self.run_report(records)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("finite", result.stderr)


if __name__ == "__main__":
    unittest.main()
