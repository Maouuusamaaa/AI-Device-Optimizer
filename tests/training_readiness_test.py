#!/usr/bin/env python3
import json, subprocess, sys, tempfile, unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "cloud/dataset/training-readiness.py"
DATASET = ROOT / "cloud/dataset/example-training-dataset.json"

class TrainingReadinessTest(unittest.TestCase):
    def test_fixture_is_blocked_without_label_diversity(self):
        with tempfile.TemporaryDirectory() as td:
            out = Path(td) / "report.json"
            result = subprocess.run(
                [sys.executable, str(SCRIPT), str(DATASET), "--out", str(out)],
                text=True, capture_output=True
            )
            self.assertEqual(result.returncode, 2)
            report = json.loads(out.read_text())
            self.assertFalse(report["trainingReady"])
            self.assertEqual(report["status"], "TRAINING_BLOCKED_INSUFFICIENT_LABEL_DIVERSITY")
            self.assertEqual(report["uniqueLabelCount"], 1)
            self.assertFalse(report["policy"]["deviceMutationAllowed"])

if __name__ == "__main__":
    unittest.main()
