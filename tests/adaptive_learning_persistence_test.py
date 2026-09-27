#!/usr/bin/env python3
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "adaptive_learning.py"
spec = importlib.util.spec_from_file_location("adaptive_learning", SCRIPT)
module = importlib.util.module_from_spec(spec)
assert spec.loader
spec.loader.exec_module(module)


def record(evidence_id):
    return {
        "schemaVersion": 1,
        "evidenceId": evidence_id,
        "device": {"androidApi": 33, "manufacturer": "itel", "model": "P661N"},
        "workload": "idle",
        "classification": "NO_REGRESSION",
        "policyId": "policy-001",
        "features": {"availableRamMbPct": 1.0},
        "provenance": {"analysisId": "a-" + evidence_id},
    }


class AdaptiveLearningPersistenceTest(unittest.TestCase):
    def state(self):
        records = [module.create_learning_record(record(f"e-{i}")) for i in range(3)]
        return module.update_knowledge_state(module.empty_knowledge_state(), records)

    def test_save_and_reload_preserves_state(self):
        state = self.state()
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "knowledge.json"
            module.save_knowledge_state(path, state)
            self.assertEqual(module.load_knowledge_state(path), state)

    def test_corrupt_state_fails_closed(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "knowledge.json"
            path.write_text("{broken", encoding="utf-8")
            with self.assertRaises(module.LearningError):
                module.load_knowledge_state(path)

    def test_unsupported_schema_fails_closed(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "knowledge.json"
            state = self.state()
            state["schemaVersion"] = 99
            path.write_text(json.dumps(state), encoding="utf-8")
            with self.assertRaises(module.LearningError):
                module.load_knowledge_state(path)

    def test_resource_guard_skips_learning(self):
        state = module.empty_knowledge_state()
        result = module.process_incremental(
            state,
            [record("e-1")],
            resource_available=lambda: False,
        )
        self.assertEqual(result, state)

    def test_process_incremental_is_idempotent(self):
        state = module.empty_knowledge_state()
        records = [record("e-1"), record("e-2"), record("e-3")]
        first = module.process_incremental(state, records, resource_available=lambda: True)
        second = module.process_incremental(first, records, resource_available=lambda: True)
        self.assertEqual(first, second)


if __name__ == "__main__":
    unittest.main()
