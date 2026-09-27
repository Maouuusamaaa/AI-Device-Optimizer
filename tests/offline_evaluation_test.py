#!/usr/bin/env python3
import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "offline_evaluation.py"

spec = importlib.util.spec_from_file_location("offline_evaluation", SCRIPT)
module = importlib.util.module_from_spec(spec)
assert spec.loader
spec.loader.exec_module(module)


def evidence(evidence_id, ram_values, pss_values=None):
    samples = []
    for i, ram in enumerate(ram_values):
        sample = {
            "timestampMs": 1000 + i,
            "availableRamMb": ram,
            "batteryPercent": 50,
            "collectionDurationMs": 100,
        }
        if pss_values is not None:
            sample["pssKb"] = pss_values[i]
        samples.append(sample)
    return {
        "schemaVersion": 2,
        "timestampMs": 1000,
        "workload": "idle",
        "device": {
            "androidApi": 33,
            "manufacturer": "itel",
            "model": "P661N",
        },
        "evidenceId": evidence_id,
        "samples": samples,
    }


class OfflineEvaluationTest(unittest.TestCase):
    def case(self, case_id="case-1"):
        return {
            "caseId": case_id,
            "policyId": "policy-observe-memory-pressure-001",
            "baseline": evidence("baseline-" + case_id, [1800, 1790], [50000, 51000]),
            "variant": evidence("variant-" + case_id, [1780, 1770], [50000, 51000]),
        }

    def test_replay_is_deterministic_and_non_authorizing(self):
        dataset = {"schemaVersion": 1, "cases": [self.case()]}
        first = module.evaluate_dataset(dataset)
        second = module.evaluate_dataset(copy.deepcopy(dataset))

        self.assertEqual(first, second)
        self.assertEqual(first["caseCount"], 1)
        self.assertEqual(first["classificationCounts"]["NO_REGRESSION"], 1)
        self.assertFalse(first["executionAllowed"])
        self.assertFalse(first["policySelectionAllowed"])
        self.assertTrue(first["datasetFingerprint"])

    def test_regression_and_insufficient_evidence_are_preserved(self):
        regression = self.case("regression")
        regression["variant"] = evidence("variant-regression", [1600, 1590], [70000, 71000])
        insufficient = self.case("insufficient")
        del insufficient["variant"]["device"]["model"]

        report = module.evaluate_dataset({
            "schemaVersion": 1,
            "cases": [regression, insufficient],
        })

        self.assertEqual(report["classificationCounts"]["REGRESSION"], 1)
        self.assertEqual(report["classificationCounts"]["INSUFFICIENT_EVIDENCE"], 1)

    def test_duplicate_case_ids_are_rejected(self):
        first = self.case("same")
        second = self.case("same")
        with self.assertRaises(module.EvaluationError):
            module.evaluate_dataset({"schemaVersion": 1, "cases": [first, second]})

    def test_input_is_not_mutated(self):
        dataset = {"schemaVersion": 1, "cases": [self.case()]}
        original = copy.deepcopy(dataset)
        module.evaluate_dataset(dataset)
        self.assertEqual(dataset, original)

    def test_history_is_append_only_and_deduplicated(self):
        dataset = {"schemaVersion": 1, "cases": [self.case()]}
        report = module.evaluate_dataset(dataset)

        with tempfile.TemporaryDirectory() as td:
            history = Path(td) / "evaluation-history.jsonl"
            first = module.append_evaluation_history(history, report)
            second = module.append_evaluation_history(history, report)

            self.assertTrue(first["appended"])
            self.assertTrue(second["duplicate"])
            self.assertEqual(len(history.read_text(encoding="utf-8").splitlines()), 1)

    def test_malformed_dataset_is_rejected(self):
        with self.assertRaises(module.EvaluationError):
            module.evaluate_dataset({"schemaVersion": 2, "cases": []})


if __name__ == "__main__":
    unittest.main()
