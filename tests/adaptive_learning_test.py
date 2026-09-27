#!/usr/bin/env python3
import copy
import importlib.util
import unittest

ROOT = __import__("pathlib").Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "adaptive_learning.py"

spec = importlib.util.spec_from_file_location("adaptive_learning", SCRIPT)
module = importlib.util.module_from_spec(spec)
assert spec.loader
spec.loader.exec_module(module)


def learning_record(evidence_id, classification="NO_REGRESSION", policy_id="policy-001"):
    return {
        "schemaVersion": 1,
        "evidenceId": evidence_id,
        "device": {"androidApi": 33, "manufacturer": "itel", "model": "P661N"},
        "workload": "idle",
        "classification": classification,
        "policyId": policy_id,
        "features": {
            "availableRamMbPct": 1.0,
            "availableRamMbDelta": 10.0,
            "pssKbPct": -1.0,
            "pssKbDelta": -500.0,
        },
        "provenance": {"analysisId": "analysis-" + evidence_id},
    }


class AdaptiveLearningTest(unittest.TestCase):
    def test_learning_record_is_deterministic_and_non_mutating(self):
        source = learning_record("e-1")
        original = copy.deepcopy(source)
        first = module.create_learning_record(source)
        second = module.create_learning_record(copy.deepcopy(source))
        self.assertEqual(first, second)
        self.assertEqual(source, original)
        self.assertEqual(first["featureExtractorVersion"], module.FEATURE_EXTRACTOR_VERSION)

    def test_invalid_evidence_is_rejected(self):
        with self.assertRaises(module.LearningError):
            module.create_learning_record(learning_record("bad", "INVALID_EVIDENCE"))

    def test_insufficient_evidence_becomes_abstention(self):
        record = module.create_learning_record(
            learning_record("insufficient", "INSUFFICIENT_EVIDENCE")
        )
        self.assertEqual(record["learningDisposition"], "ABSTAIN")

    def test_duplicate_records_are_idempotent(self):
        records = [module.create_learning_record(learning_record("e-1"))]
        first = module.update_knowledge_state(module.empty_knowledge_state(), records)
        second = module.update_knowledge_state(first, records)
        self.assertEqual(first, second)

    def test_insufficient_samples_abstain(self):
        records = [
            module.create_learning_record(learning_record("e-1")),
            module.create_learning_record(learning_record("e-2")),
        ]
        state = module.update_knowledge_state(module.empty_knowledge_state(), records)
        self.assertEqual(state["patterns"][0]["eligible"], False)
        self.assertEqual(module.generate_candidates(state), [])

    def test_sufficient_consistent_records_create_pattern(self):
        records = [
            module.create_learning_record(learning_record("e-1")),
            module.create_learning_record(learning_record("e-2")),
            module.create_learning_record(learning_record("e-3")),
        ]
        state = module.update_knowledge_state(module.empty_knowledge_state(), records)
        self.assertEqual(len(state["patterns"]), 1)
        candidates = module.generate_candidates(state)
        self.assertEqual(len(candidates), 1)
        self.assertNotIn("execute", candidates[0])
        self.assertNotIn("bypassSafetyGate", candidates[0])
        self.assertTrue(candidates[0]["provenance"])

    def test_state_is_deterministic(self):
        records = [
            module.create_learning_record(learning_record("e-1")),
            module.create_learning_record(learning_record("e-2")),
            module.create_learning_record(learning_record("e-3")),
        ]
        first = module.update_knowledge_state(module.empty_knowledge_state(), records)
        second = module.update_knowledge_state(module.empty_knowledge_state(), list(reversed(records)))
        self.assertEqual(first, second)


if __name__ == "__main__":
    unittest.main()
