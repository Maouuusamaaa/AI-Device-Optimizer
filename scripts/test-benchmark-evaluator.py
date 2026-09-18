#!/usr/bin/env python3
"""Unit tests for benchmarks/evaluator.py."""

from __future__ import annotations

import importlib.util
import unittest


SCRIPT = __import__("pathlib").Path(__file__).resolve().parents[1] / "benchmarks" / "evaluator.py"


def load_module():
    spec = importlib.util.spec_from_file_location("benchmark_evaluator", SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError("Unable to load benchmarks/evaluator.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


evaluator = load_module()


def observation(run, workload="idle", ram=2000, cpu=1.0, collection=3.0, battery_delta=-1, temp_delta=0.5):
    return {
        "workload": workload,
        "device": {"manufacturer": "ITEL", "model": "itel P661N", "androidApi": 33},
        "availableRamMb": {"average": ram},
        "processCpuTimeMs": {"percentOfWallTime": cpu},
        "collectionDurationMs": {"average": collection},
        "batteryPercent": {"delta": battery_delta},
        "temperatureC": {"delta": temp_delta},
    }


def report(values):
    return {"internalObservations": values}


class BenchmarkEvaluatorTest(unittest.TestCase):
    def test_identical_runs_are_unchanged(self):
        runs = [observation(i) for i in range(3)]
        result = evaluator.evaluate(report(runs), report(runs))
        self.assertEqual(result["runCount"], 3)
        self.assertEqual(result["summary"]["availableRamMb"]["averageDelta"], 0)
        self.assertEqual(result["summary"]["availableRamMb"]["directionCounts"]["unchanged"], 3)

    def test_ram_and_cpu_deltas(self):
        before = [observation(i, ram=1800, cpu=2.0) for i in range(3)]
        after = [observation(i, ram=2000, cpu=1.0) for i in range(3)]
        result = evaluator.evaluate(report(before), report(after))
        self.assertEqual(result["summary"]["availableRamMb"]["averageDelta"], 200)
        self.assertEqual(result["summary"]["availableRamMb"]["directionCounts"]["improved"], 3)
        self.assertEqual(result["summary"]["processCpuPercentOfWallTime"]["averageDelta"], -1.0)
        self.assertEqual(result["summary"]["processCpuPercentOfWallTime"]["directionCounts"]["improved"], 3)

    def test_insufficient_runs_rejected(self):
        runs = [observation(i) for i in range(2)]
        with self.assertRaises(ValueError):
            evaluator.evaluate(report(runs), report(runs), min_runs=3)

    def test_workload_mismatch_rejected(self):
        before = [observation(i, workload="idle") for i in range(3)]
        after = [observation(i, workload="gaming") for i in range(3)]
        with self.assertRaises(ValueError):
            evaluator.evaluate(report(before), report(after))

    def test_device_mismatch_rejected(self):
        before = [observation(i) for i in range(3)]
        after = [observation(i) for i in range(3)]
        after[1]["device"]["model"] = "other"
        with self.assertRaises(ValueError):
            evaluator.evaluate(report(before), report(after))


if __name__ == "__main__":
    unittest.main()
