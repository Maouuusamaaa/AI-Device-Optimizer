#!/usr/bin/env python3
"""Smoke tests for scripts/aggregate-rish-baselines.py."""

from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "aggregate-rish-baselines.py"


def load_module():
    spec = importlib.util.spec_from_file_location("aggregate_rish", SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError("Unable to load aggregate-rish-baselines.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


aggregate_rish = load_module()


SAMPLE = """AI Device Optimizer - external Termux/Shizuku baseline
timestamp=2026-09-20T06:00:00+07:00
rish=/data/data/com.termux/files/home/rish

[device]
ITEL
itel P661N
33

[battery]
  level: 50
  temperature: 400

[startup]
run=1
TotalTime: 600
WaitTime: 610
run=2
TotalTime: 700
WaitTime: 710

[app-memory]
        TOTAL    55000    37000     7000       80   180000
           Java Heap:     9000
         Native Heap:    13000
         Graphics:     4896
         Private Other:  8800
         System:    11000
           TOTAL PSS:    55000            TOTAL RSS:   180000       TOTAL SWAP PSS:       80
"""


class AggregateRishTest(unittest.TestCase):
    def test_aggregate_multiple_runs(self):
        with tempfile.TemporaryDirectory() as tmp:
            first = Path(tmp) / "a.txt"
            second = Path(tmp) / "b.txt"
            first.write_text(SAMPLE, encoding="utf-8")
            second.write_text(SAMPLE.replace("06:00:00", "06:05:00"), encoding="utf-8")
            result = aggregate_rish.aggregate([str(first), str(second)])

        self.assertEqual(result["observationCount"], 2)
        self.assertTrue(result["deviceConsistency"]["allMatch"])
        self.assertEqual(result["crossRun"]["startupAverageMs"]["average"], 650)
        self.assertEqual(result["crossRun"]["startupAverageMs"]["sampleCount"], 2)
        self.assertEqual(result["crossRun"]["pssKb"]["average"], 55000)


if __name__ == "__main__":
    unittest.main()
