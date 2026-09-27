#!/usr/bin/env python3
import copy
import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/validate-ai-cloud-contract.py"
EXAMPLE = ROOT / "cloud/contracts/example-ai-cloud-response.json"
ADVISOR_EXAMPLE = ROOT / "cloud/contracts/example-cloud-advisor-response.json"

spec = importlib.util.spec_from_file_location("ai_cloud_contract", SCRIPT)
module = importlib.util.module_from_spec(spec)
assert spec.loader
spec.loader.exec_module(module)


class AICloudContractTest(unittest.TestCase):
    def test_example_is_valid(self):
        report = module.validate(module.load(EXAMPLE))
        self.assertTrue(report["valid"])
        self.assertFalse(report["executionRequested"])
        self.assertFalse(report["deviceMutationAllowed"])

    def test_cloud_cannot_enable_execution(self):
        value = module.load(EXAMPLE)
        value["recommendation"]["execution"]["deviceMutationAllowed"] = True
        with self.assertRaises(module.ContractError):
            module.validate(value)

    def test_cloud_cannot_request_execution(self):
        value = module.load(EXAMPLE)
        value["recommendation"]["execution"]["requested"] = True
        with self.assertRaises(module.ContractError):
            module.validate(value)

    def test_confidence_bounds(self):
        value = module.load(EXAMPLE)
        value["recommendation"]["confidence"] = 1.1
        with self.assertRaises(module.ContractError):
            module.validate(value)

    def test_advisor_example_is_valid(self):
        report = module.validate_advisor_response(module.load(ADVISOR_EXAMPLE))
        self.assertTrue(report["valid"])
        self.assertTrue(report["advisoryOnly"])
        self.assertEqual(report["schemaVersion"], 1)
        self.assertEqual(report["recommendationCount"], 1)

    def test_advisor_unsupported_schema_is_rejected(self):
        value = module.load(ADVISOR_EXAMPLE)
        value["schemaVersion"] = 2
        with self.assertRaises(module.ContractError):
            module.validate_advisor_response(value)

    def test_advisor_missing_required_field_is_rejected(self):
        value = module.load(ADVISOR_EXAMPLE)
        del value["recommendations"]
        with self.assertRaises(module.ContractError):
            module.validate_advisor_response(value)

    def test_advisor_empty_action_id_is_rejected(self):
        value = module.load(ADVISOR_EXAMPLE)
        value["recommendations"][0]["actionType"] = " "
        with self.assertRaises(module.ContractError):
            module.validate_advisor_response(value)

    def test_advisor_confidence_must_be_finite_and_bounded(self):
        value = module.load(ADVISOR_EXAMPLE)
        value["recommendations"][0]["confidence"] = 1.1
        with self.assertRaises(module.ContractError):
            module.validate_advisor_response(value)

    def test_advisor_execution_authority_field_is_rejected(self):
        value = module.load(ADVISOR_EXAMPLE)
        value["execution"] = {"requested": True, "deviceMutationAllowed": True}
        with self.assertRaises(module.ContractError):
            module.validate_advisor_response(value)

    def test_advisor_unknown_field_is_rejected(self):
        value = module.load(ADVISOR_EXAMPLE)
        value["recommendations"][0]["bypassSafetyGate"] = True
        with self.assertRaises(module.ContractError):
            module.validate_advisor_response(value)

    def test_advisor_validation_does_not_mutate_input(self):
        value = module.load(ADVISOR_EXAMPLE)
        original = copy.deepcopy(value)
        module.validate_advisor_response(value)
        self.assertEqual(value, original)


if __name__ == "__main__":
    unittest.main()
