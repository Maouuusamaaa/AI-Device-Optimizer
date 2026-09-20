#!/usr/bin/env python3
from __future__ import annotations
import importlib.util, json, tempfile, unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "experiment-protocol.py"
SPEC = importlib.util.spec_from_file_location("experiment_protocol", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)

def definition():
    return {
        "schemaVersion": 1,
        "experimentId": "stage2.memory-observation-001",
        "candidateAction": {
            "actionId": "observe.memory_pressure",
            "description": "Record a memory-pressure condition.",
            "risk": "LOW",
            "requiredPermission": "none",
            "reversible": True,
            "measurement": "RAM and startup timing",
            "rollback": "Not applicable; observation-only.",
        },
        "execution": {"enabled": False, "deviceMutationAllowed": False},
        "phases": {
            "baseline": {"purpose": "Measure before the candidate intervention."},
            "intervention": {"purpose": "Hold the intervention slot without mutating the device."},
            "post": {"purpose": "Measure after the intervention slot."},
        },
        "measurementPlan": {
            "metrics": ["availableRamMb", "startupMs", "batteryTemperatureC"],
            "primaryMetric": "startupMs",
        },
    }

class TestExperimentProtocol(unittest.TestCase):
    def test_valid_definition_is_non_authorizing(self):
        report = MODULE.validate(definition())
        self.assertTrue(report["measurementOnly"])
        self.assertFalse(report["executionAllowed"])
        self.assertFalse(report["policySelectionAllowed"])
        self.assertEqual(report["execution"]["status"], "BLOCKED_PENDING_REAL_ACTION_ENGINE")

    def test_execution_enabled_rejected(self):
        value = definition()
        value["execution"]["enabled"] = True
        with self.assertRaises(MODULE.ExperimentProtocolError):
            MODULE.validate(value)

    def test_mutation_permission_rejected(self):
        value = definition()
        value["execution"]["deviceMutationAllowed"] = True
        with self.assertRaises(MODULE.ExperimentProtocolError):
            MODULE.validate(value)

    def test_missing_phase_rejected(self):
        value = definition()
        del value["phases"]["post"]
        with self.assertRaises(MODULE.ExperimentProtocolError):
            MODULE.validate(value)

    def test_missing_metric_rejected(self):
        value = definition()
        value["measurementPlan"]["metrics"] = []
        with self.assertRaises(MODULE.ExperimentProtocolError):
            MODULE.validate(value)

    def test_report_is_deterministic_for_same_definition(self):
        value = definition()
        self.assertEqual(MODULE.validate(value), MODULE.validate(value))

    def test_file_round_trip(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "experiment.json"
            path.write_text(json.dumps(definition()), encoding="utf-8")
            loaded = MODULE.load_definition(path)
            self.assertEqual(loaded["experimentId"], "stage2.memory-observation-001")

if __name__ == "__main__":
    unittest.main()
