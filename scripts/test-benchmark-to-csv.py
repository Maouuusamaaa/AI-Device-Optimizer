#!/usr/bin/env python3
"""Unit tests for scripts/benchmark-to-csv.py."""

from __future__ import annotations

import csv
import importlib.util
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "benchmark-to-csv.py"

def load_module():
    spec = importlib.util.spec_from_file_location("benchmark_to_csv", SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError("Unable to load benchmark-to-csv.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

converter = load_module()

class BenchmarkCsvTest(unittest.TestCase):
    def test_external_row(self):
        row = converter.row_external({
            "source": "baseline.txt",
            "timestamp": "2026-09-20T06:37:23+07:00",
            "device": {"manufacturer": "ITEL", "model": "itel P661N", "androidApi": 33},
            "startupMs": {"sampleCount": 5, "average": 651.4, "median": 643, "stdev": 40.4, "min": 601, "max": 693},
            "waitMs": {"average": 657.6},
            "memoryKb": {"pss": 55469, "rss": 180388, "swapPss": 77},
            "batteryPercent": 39,
            "temperatureC": 43.1,
        })
        self.assertEqual(row["startupAverageMs"], 651.4)
        self.assertEqual(row["pssKb"], 55469)
        self.assertEqual(row["model"], "itel P661N")

    def test_csv_round_trip(self):
        data = {
            "externalRish": {
                "source": "baseline.txt",
                "timestamp": "now",
                "device": {"manufacturer": "ITEL", "model": "itel P661N", "androidApi": 33},
                "startupMs": {"sampleCount": 5, "average": 651.4, "median": 643, "stdev": 40.4, "min": 601, "max": 693},
                "waitMs": {"average": 657.6},
                "memoryKb": {"pss": 55469, "rss": 180388, "swapPss": 77},
                "batteryPercent": 39,
                "temperatureC": 43.1,
            },
            "internalObservations": [],
        }
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "out.csv"
            with path.open("w", newline="", encoding="utf-8") as handle:
                writer = csv.DictWriter(handle, fieldnames=converter.FIELDS)
                writer.writeheader()
                writer.writerow(converter.row_external(data["externalRish"]))
            rows = list(csv.DictReader(path.open(encoding="utf-8")))
        self.assertEqual(rows[0]["pssKb"], "55469")

if __name__ == "__main__":
    unittest.main()