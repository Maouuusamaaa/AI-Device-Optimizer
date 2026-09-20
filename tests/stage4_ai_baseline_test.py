#!/usr/bin/env python3
import importlib.util,json,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("advisor",ROOT/"cloud/models/baseline-advisor.py"); mod=importlib.util.module_from_spec(spec); assert spec.loader; spec.loader.exec_module(mod)
class Stage4Test(unittest.TestCase):
    def test_baseline_abstains(self):
        r=mod.recommend({"batteryPercent":50,"temperatureC":35,"availableRamMb":1800}); self.assertTrue(r["abstain"]); self.assertEqual(r["actionId"],"OBSERVE_ONLY")
    def test_high_temperature_abstains(self):
        r=mod.recommend({"batteryPercent":50,"temperatureC":50,"availableRamMb":1800}); self.assertTrue(r["abstain"]); self.assertEqual(r["actionId"],"OBSERVE_ONLY")
    def test_fixture_splits(self):
        d=json.loads((ROOT/"cloud/dataset/example-training-dataset.json").read_text()); self.assertEqual(d["rowCount"],len(d["rows"])); self.assertEqual({x["provenance"]["split"] for x in d["rows"]},{"train","validation","test"})
if __name__=="__main__": unittest.main()
