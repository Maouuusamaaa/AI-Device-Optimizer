#!/usr/bin/env python3
import importlib.util, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SCRIPT=ROOT/"scripts/validate-ai-cloud-contract.py"
EXAMPLE=ROOT/"cloud/contracts/example-ai-cloud-response.json"
spec=importlib.util.spec_from_file_location("ai_cloud_contract",SCRIPT); module=importlib.util.module_from_spec(spec); assert spec.loader; spec.loader.exec_module(module)
class AICloudContractTest(unittest.TestCase):
    def test_example_is_valid(self):
        report=module.validate(module.load(EXAMPLE)); self.assertTrue(report['valid']); self.assertFalse(report['executionRequested']); self.assertFalse(report['deviceMutationAllowed'])
    def test_cloud_cannot_enable_execution(self):
        value=module.load(EXAMPLE); value['recommendation']['execution']['deviceMutationAllowed']=True
        with self.assertRaises(module.ContractError): module.validate(value)
    def test_cloud_cannot_request_execution(self):
        value=module.load(EXAMPLE); value['recommendation']['execution']['requested']=True
        with self.assertRaises(module.ContractError): module.validate(value)
    def test_confidence_bounds(self):
        value=module.load(EXAMPLE); value['recommendation']['confidence']=1.1
        with self.assertRaises(module.ContractError): module.validate(value)
if __name__=="__main__": unittest.main()
