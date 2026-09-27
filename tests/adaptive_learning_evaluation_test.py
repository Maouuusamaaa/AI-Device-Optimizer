#!/usr/bin/env python3
import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADAPTIVE = ROOT / "scripts" / "adaptive_learning.py"
EVAL = ROOT / "scripts" / "adaptive_learning_evaluation.py"

spec = importlib.util.spec_from_file_location("adaptive_learning", ADAPTIVE)
adaptive = importlib.util.module_from_spec(spec)
assert spec.loader
spec.loader.exec_module(adaptive)

spec2 = importlib.util.spec_from_file_location("adaptive_learning_evaluation", EVAL)
evaluation = importlib.util.module_from_spec(spec2)
assert spec2.loader
spec2.loader.exec_module(evaluation)


def record(evidence_id, classification="NO_REGRESSION", policy_id="policy-001"):
    return {
        "schemaVersion": 1,
        "evidenceId": evidence_id,
        "device": {"androidApi": 33, "manufacturer": "itel", "model": "P661N"},
        "workload": "game",
        "classification": classification,
        "policyId": policy_id,
        "features": {"availableRamMbPct": 1.0},
        "provenance": {"analysisId": "analysis-" + evidence_id},
        "baselineContext": {
            "interactive": True,
            "charging": False,
            "thermalStatus": 0,
            "networkTransport": "WIFI",
            "gameModeChecked": True,
        },
        "variantContext": {
            "interactive": True,
            "charging": False,
            "thermalStatus": 0,
            "networkTransport": "WIFI",
            "gameModeChecked": True,
        },
    }


class AdaptiveLearningEvaluationTest(unittest.TestCase):
    def test_outcome_labels_are_explicit_and_non_causal(self):
        expected = {
            "NO_REGRESSION": "NON_DEGRADING_DESCRIPTIVE",
            "REGRESSION": "DEGRADING_DESCRIPTIVE",
            "MIXED": "MIXED_DESCRIPTIVE",
            "INSUFFICIENT_EVIDENCE": "UNRESOLVED",
        }
        for classification, label in expected.items():
            outcome = evaluation.create_descriptive_outcome(
                record("e-" + classification, classification)
            )
            self.assertEqual(outcome["descriptiveOutcomeClass"], label)
            self.assertFalse(outcome["causalInferenceAllowed"])
            self.assertFalse(outcome["executionAllowed"])
            self.assertFalse(outcome["policySelectionAllowed"])

    def test_context_mismatch_is_explicit(self):
        baseline = {"interactive": True, "charging": False, "thermalStatus": 0, "networkTransport": "WIFI", "gameModeChecked": True}
        variant = {"interactive": True, "charging": False, "thermalStatus": 2, "networkTransport": "WIFI", "gameModeChecked": True}
        comparison = evaluation.compare_context(baseline, variant)
        self.assertEqual(comparison["status"], "MISMATCHED")
        self.assertEqual(comparison["mismatchedKeys"], ["thermalStatus"])

    def test_context_missing_is_not_treated_as_matched(self):
        comparison = evaluation.compare_context(
            {"interactive": True},
            {"interactive": True},
        )
        self.assertEqual(comparison["status"], "INCOMPLETE")
        self.assertFalse(comparison["matched"])

    def test_unknown_context_field_is_rejected(self):
        with self.assertRaises(evaluation.EvaluationHardeningError):
            evaluation.normalize_context({"privateUserContent": "x"})

    def test_train_derived_candidate_is_checked_on_holdout(self):
        train_records = [
            adaptive.create_learning_record(record(f"train-{i}"))
            for i in range(3)
        ]
        train_state = adaptive.update_knowledge_state(
            adaptive.empty_knowledge_state(),
            train_records,
        )

        holdout = [
            record("holdout-1"),
            record("holdout-2"),
        ]
        report = evaluation.evaluate_holdout(train_state, holdout)
        self.assertEqual(report["candidateCount"], 1)
        self.assertEqual(report["results"][0]["holdoutStatus"], "SUPPORTED_DESCRIPTIVELY")
        self.assertTrue(report["evaluationReady"])
        self.assertFalse(report["causalInferenceAllowed"])
        self.assertFalse(report["executionAllowed"])
        self.assertFalse(report["policySelectionAllowed"])

    def test_holdout_with_regression_is_not_supported(self):
        train_records = [
            adaptive.create_learning_record(record(f"train-{i}"))
            for i in range(3)
        ]
        train_state = adaptive.update_knowledge_state(
            adaptive.empty_knowledge_state(),
            train_records,
        )
        report = evaluation.evaluate_holdout(
            train_state,
            [record("holdout-1"), record("holdout-2", "REGRESSION")],
        )
        self.assertEqual(
            report["results"][0]["holdoutStatus"],
            "CONTRADICTED_DESCRIPTIVELY",
        )

    def test_incomplete_holdout_context_fails_closed(self):
        train_records = [
            adaptive.create_learning_record(record(f"train-{i}"))
            for i in range(3)
        ]
        train_state = adaptive.update_knowledge_state(
            adaptive.empty_knowledge_state(),
            train_records,
        )
        incomplete = record("holdout-1")
        incomplete.pop("variantContext")
        report = evaluation.evaluate_holdout(
            train_state,
            [incomplete, record("holdout-2")],
        )
        self.assertEqual(
            report["results"][0]["holdoutStatus"],
            "CONTEXT_INCOMPLETE",
        )


    def test_holdout_matching_requires_full_pattern_identity(self):
        train_records = [
            adaptive.create_learning_record(record(f"train-{i}"))
            for i in range(3)
        ]
        train_state = adaptive.update_knowledge_state(
            adaptive.empty_knowledge_state(),
            train_records,
        )
        different_workload = record("holdout-1")
        different_workload["workload"] = "idle"
        different_workload["variantContext"]["gameModeChecked"] = False
        different_workload_2 = record("holdout-2")
        different_workload_2["workload"] = "idle"
        different_workload_2["variantContext"]["gameModeChecked"] = False
        report = evaluation.evaluate_holdout(
            train_state,
            [different_workload, different_workload_2],
        )
        self.assertEqual(
            report["results"][0]["holdoutStatus"],
            "INSUFFICIENT_HOLDOUT",
        )

    def test_holdout_minimum_is_enforced(self):
        train_records = [
            adaptive.create_learning_record(record(f"train-{i}"))
            for i in range(3)
        ]
        train_state = adaptive.update_knowledge_state(
            adaptive.empty_knowledge_state(),
            train_records,
        )
        report = evaluation.evaluate_holdout(train_state, [record("holdout-1")])
        self.assertEqual(
            report["results"][0]["holdoutStatus"],
            "INSUFFICIENT_HOLDOUT",
        )


if __name__ == "__main__":
    unittest.main()
