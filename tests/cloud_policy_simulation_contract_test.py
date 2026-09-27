#!/usr/bin/env python3
import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/validate-ai-cloud-contract.py"
EXAMPLE = ROOT / "cloud/contracts/example-cloud-advisor-response.json"
SCHEMA = ROOT / "cloud/contracts/cloud-advisor-schema.json"

spec = importlib.util.spec_from_file_location("ai_cloud_contract", SCRIPT)
module = importlib.util.module_from_spec(spec)
assert spec.loader
spec.loader.exec_module(module)


class CloudPolicySimulationContractTest(unittest.TestCase):
    def test_schema_and_example_are_valid_json(self):
        schema = json.loads(SCHEMA.read_text(encoding="utf-8"))
        example = json.loads(EXAMPLE.read_text(encoding="utf-8"))
        self.assertEqual(schema["properties"]["schemaVersion"]["const"], 1)
        self.assertEqual(example["schemaVersion"], 1)

    def test_advisor_provenance_links_evidence_and_policy(self):
        value = module.load(EXAMPLE)
        report = module.validate_advisor_response(value)
        self.assertEqual(report["evidenceId"], value["evidenceId"])
        self.assertEqual(report["advisorVersion"], value["advisorVersion"])
        self.assertEqual(report["recommendationCount"], len(value["recommendations"]))
        self.assertEqual(value["recommendations"][0]["policyId"], "policy-observe-memory-pressure-001")

    def test_advisor_is_explicitly_non_authoritative(self):
        value = module.load(EXAMPLE)
        report = module.validate_advisor_response(value)
        self.assertTrue(report["advisoryOnly"])
        self.assertFalse(report["executionRequested"])
        self.assertFalse(report["deviceMutationAllowed"])

    def test_bypass_and_authorization_fields_cannot_reach_validated_output(self):
        value = module.load(EXAMPLE)
        value["recommendations"][0]["bypassSafetyGate"] = True
        with self.assertRaises(module.ContractError):
            module.validate_advisor_response(value)

        value = module.load(EXAMPLE)
        value["authorization"] = {"allow": True}
        with self.assertRaises(module.ContractError):
            module.validate_advisor_response(value)

    def test_invalid_candidate_never_mutates_source(self):
        value = module.load(EXAMPLE)
        original = copy.deepcopy(value)
        value["recommendations"][0]["confidence"] = 2.0
        with self.assertRaises(module.ContractError):
            module.validate_advisor_response(value)
        self.assertEqual(value, {**original, "recommendations": [{**original["recommendations"][0], "confidence": 2.0}]})

    def test_existing_p661n_evidence_remains_a_measurement_fixture(self):
        path = ROOT / "benchmarks" / "results" / "baseline-monitor_foreground_idle-1790164000328.json"
        record = json.loads(path.read_text(encoding="utf-8"))
        self.assertEqual(record["schemaVersion"], 2)
        self.assertEqual(record["device"]["androidApi"], 33)
        self.assertEqual(record["device"]["model"], "itel P661N")


if __name__ == "__main__":
    unittest.main()
