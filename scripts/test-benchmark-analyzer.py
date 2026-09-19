#!/usr/bin/env python3
"""Unit tests for scripts/benchmark-analyzer.py."""

from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parent / "benchmark-analyzer.py"


def load_module():
    spec = importlib.util.spec_from_file_location("benchmark_analyzer", SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError("Unable to load benchmark-analyzer.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


analyzer = load_module()


RISH_SAMPLE = """AI Device Optimizer - external Termux/Shizuku baseline
timestamp=2026-09-18T14:01:36+07:00
rish=/data/data/com.termux/files/home/rish

[device]
ITEL
itel P661N
33

[battery]
  AC powered: false
  USB powered: false
  Wireless powered: false
  status: 3
  level: 32
  temperature: 357

[startup]
run=1
TotalTime: 358
WaitTime: 370
run=2
TotalTime: 330
WaitTime: 335
run=3
TotalTime: 321
WaitTime: 327
run=4
TotalTime: 323
WaitTime: 328
run=5
TotalTime: 333
WaitTime: 337

[app-memory]
        TOTAL    54280    36048     4144      267   160364    28063    19640     3562
           Java Heap:     9308                          21800
         Native Heap:    13276                          14852
         Graphics:       4896                           4896
         Private Other:  8336
         System:        14088
         TOTAL PSS:    54280            TOTAL RSS:   160364       TOTAL SWAP PSS:      267
"""


class BenchmarkAnalyzerTest(unittest.TestCase):
    def test_parse_rish(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "rish.txt"
            path.write_text(RISH_SAMPLE, encoding="utf-8")
            result = analyzer.parse_rish(path)

        self.assertEqual(result["device"], {
            "manufacturer": "ITEL",
            "model": "itel P661N",
            "androidApi": 33,
        })
        self.assertEqual(result["batteryPercent"], 32)
        self.assertEqual(result["temperatureC"], 35.7)
        self.assertEqual(result["startupMs"]["samples"], [358, 330, 321, 323, 333])
        self.assertEqual(result["startupMs"]["average"], 333)
        self.assertEqual(result["waitMs"]["average"], 339.4)
        self.assertEqual(result["memoryKb"]["pss"], 54280)
        self.assertEqual(result["memoryKb"]["rss"], 160364)
        self.assertEqual(result["memoryKb"]["swapPss"], 267)

    def test_parse_internal(self):
        data = {
            "schemaVersion": 1,
            "timestampMs": 1000,
            "workload": "monitor_foreground_idle",
            "device": {
                "androidApi": 33,
                "manufacturer": "ITEL",
                "model": "itel P661N",
            },
            "samples": [
                {
                    "timestampMs": 1000,
                    "availableRamMb": 1800,
                    "totalRamMb": 5634,
                    "batteryPercent": 44,
                    "temperatureC": 29.0,
                    "storageAvailableMb": 39980,
                    "processCpuTimeMs": 100,
                    "collectionDurationMs": 2,
                },
                {
                    "timestampMs": 3000,
                    "availableRamMb": 1900,
                    "totalRamMb": 5634,
                    "batteryPercent": 44,
                    "temperatureC": 29.8,
                    "storageAvailableMb": 39980,
                    "processCpuTimeMs": 120,
                    "collectionDurationMs": 4,
                },
            ],
        }

        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "baseline.json"
            path.write_text(json.dumps(data), encoding="utf-8")
            result = analyzer.parse_internal(path)

        self.assertEqual(result["sampleCount"], 2)
        self.assertEqual(result["durationMs"], 2000)
        self.assertEqual(result["availableRamMb"]["average"], 1850)
        self.assertAlmostEqual(result["availableRamMb"]["percentOfTotalAverage"], 32.83635, places=4)
        self.assertEqual(result["collectionDurationMs"]["average"], 3)
        self.assertEqual(result["batteryPercent"]["delta"], 0)
        self.assertAlmostEqual(result["temperatureC"]["delta"], 0.8, places=6)
        self.assertEqual(result["processCpuTimeMs"]["delta"], 20)
        self.assertAlmostEqual(result["processCpuTimeMs"]["percentOfWallTime"], 1.0)

    def test_build_unified_keeps_observations_separate(self):
        internal = [
            {"device": {"manufacturer": "ITEL", "model": "itel P661N", "androidApi": 33}},
            {"device": {"manufacturer": "ITEL", "model": "itel P661N", "androidApi": 33}},
        ]
        external = {"device": {"manufacturer": "ITEL", "model": "itel P661N", "androidApi": 33}}

        result = analyzer.build_unified(internal, external)

        self.assertEqual(result["internalObservationCount"], 2)
        self.assertEqual(len(result["internalObservations"]), 2)
        self.assertEqual(result["externalRish"], external)
        self.assertTrue(result["deviceConsistency"]["allMatch"])
        self.assertEqual(len(result["comparisonNotes"]), 6)

    def test_device_consistency_detects_mismatch(self):
        internal = [
            {"device": {"manufacturer": "ITEL", "model": "itel P661N", "androidApi": 33}},
            {"device": {"manufacturer": "Other", "model": "other", "androidApi": 34}},
        ]

        result = analyzer.build_unified(internal, None)

        self.assertFalse(result["deviceConsistency"]["allMatch"])


if __name__ == "__main__":
    unittest.main()
