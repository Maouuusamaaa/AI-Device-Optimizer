#!/usr/bin/env python3
import json, tempfile, unittest
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("health",ROOT/"scripts/health-snapshot-observation.py")
mod=importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)

def snap(ram,battery,temp,charging=False):
    return {"schemaVersion":3,"timestampMs":1,"androidApi":33,"manufacturer":"ITEL","model":"itel P661N",
            "totalRamMb":5634,"availableRamMb":ram,"batteryPercent":battery,"isCharging":charging,
            "batteryTemperatureC":temp,"thermalStatus":0,"storageTotalBytes":1000,"storageFreeBytes":400,
            "networkTransport":"WIFI","networkValidated":True,"isInteractive":False,"uptimeMs":100}

class TestHealthObservation(unittest.TestCase):
    def test_summary_and_identity(self):
        with tempfile.TemporaryDirectory() as d:
            paths=[]
            for i,(ram,bat,temp) in enumerate([(1800,50,40.0),(1900,51,41.0),(1850,49,40.5)]):
                p=Path(d)/f"{i}.json"; p.write_text(json.dumps(snap(ram,bat,temp))); paths.append(p)
            report=mod.analyze(paths)
            self.assertEqual(report["observationCount"],3)
            self.assertEqual(report["fields"]["availableRamMb"]["average"],1850.0)
            self.assertEqual(report["fields"]["batteryPercent"]["median"],50.0)
            self.assertTrue(report["measurementOnly"])
    def test_rejects_mismatched_device(self):
        with tempfile.TemporaryDirectory() as d:
            a=Path(d)/"a.json"; b=Path(d)/"b.json"
            a.write_text(json.dumps(snap(1800,50,40)))
            x=snap(1800,50,40); x["model"]="other"; b.write_text(json.dumps(x))
            with self.assertRaises(ValueError): mod.analyze([a,b])

if __name__=="__main__": unittest.main()
