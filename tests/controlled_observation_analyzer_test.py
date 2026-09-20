#!/usr/bin/env python3
from __future__ import annotations
import importlib.util, json, statistics, tempfile, unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "controlled-observation-analyzer.py"
SPEC = importlib.util.spec_from_file_location("controlled_observation_analyzer", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)

def write_phase(root, phase, startup, pss):
    d = root / phase
    d.mkdir()
    raw = d / f"rish-baseline-{phase}.txt"
    raw.write_text("raw\n", encoding="utf-8")
    samples = startup
    payload = {"externalRish": {
        "source": str(raw), "timestamp": "2026-09-20T11:46:08+07:00",
        "device": {"manufacturer": "ITEL", "model": "itel P661N", "androidApi": 33},
        "batteryPercent": 26, "temperatureC": 43.0,
        "startupMs": {"average": statistics.mean(samples), "median": statistics.median(samples),
                      "stdev": statistics.stdev(samples), "min": min(samples), "max": max(samples),
                      "sampleCount": len(samples), "samples": samples},
        "waitMs": {"average": statistics.mean([99,100,100,100,101]), "median": statistics.median([99,100,100,100,101]), "stdev": statistics.stdev([99,100,100,100,101]), "min": 99.0,
                   "max": 101.0, "sampleCount": 5, "samples": [99,100,100,100,101]},
        "memoryKb": {"pss": pss, "rss": 175000, "swapPss": 150},
    }}
    (d / f"{phase}.json").write_text(json.dumps(payload), encoding="utf-8")

class TestAnalyzer(unittest.TestCase):
    def root(self):
        r = Path(tempfile.mkdtemp())
        (r/"manifest.json").write_text(json.dumps({
            "schemaVersion": 1, "candidateActionId": "observe.remeasure_baseline",
            "executionEnabled": False, "deviceMutationAllowed": False,
            "phases": {p: {"rawText": f"/storage/{p}/rish-baseline-{p}.txt"} for p in ("baseline","intervention","post")}
        }), encoding="utf-8")
        write_phase(r, "baseline", [600,610,620,630,640], 57000)
        write_phase(r, "intervention", [610,620,630,640,650], 58000)
        write_phase(r, "post", [590,600,610,620,630], 57500)
        return r

    def test_valid(self):
        report = MODULE.analyze(self.root())
        self.assertTrue(report["measurementOnly"])
        self.assertTrue(report["validation"]["deviceConsistent"])
        m = next(x for x in report["comparisons"]["metrics"] if x["metric"] == "startupMs.average")
        self.assertAlmostEqual(m["interventionVsBaseline"]["absoluteDelta"], 10.0)
        self.assertAlmostEqual(m["postVsBaseline"]["absoluteDelta"], -10.0)

    def test_execution_rejected(self):
        r = self.root()
        p = json.loads((r/"manifest.json").read_text())
        p["executionEnabled"] = True
        (r/"manifest.json").write_text(json.dumps(p))
        with self.assertRaises(MODULE.ObservationError):
            MODULE.analyze(r)

    def test_device_mismatch_rejected(self):
        r = self.root()
        path = r / "intervention" / "intervention.json"
        payload = json.loads(path.read_text())
        payload["externalRish"]["device"]["model"] = "different-model"
        path.write_text(json.dumps(payload))
        with self.assertRaises(MODULE.ObservationError):
            MODULE.analyze(r)

    def test_sample_count_mismatch_rejected(self):
        r = self.root()
        path = r / "post" / "post.json"
        payload = json.loads(path.read_text())
        payload["externalRish"]["startupMs"]["sampleCount"] = 4
        path.write_text(json.dumps(payload))
        with self.assertRaises(MODULE.ObservationError):
            MODULE.analyze(r)

    def test_reported_statistics_mismatch_rejected(self):
        r = self.root()
        path = r / "intervention" / "intervention.json"
        payload = json.loads(path.read_text())
        payload["externalRish"]["startupMs"]["average"] = 999.0
        path.write_text(json.dumps(payload))
        with self.assertRaises(MODULE.ObservationError):
            MODULE.analyze(r)

    def test_missing_phase_rejected(self):
        r = self.root()
        p = json.loads((r/"manifest.json").read_text())
        del p["phases"]["post"]
        (r/"manifest.json").write_text(json.dumps(p))
        with self.assertRaises(MODULE.ObservationError):
            MODULE.analyze(r)

if __name__ == "__main__":
    unittest.main()
