#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "controlled-experiment-runner.py"
SPEC = importlib.util.spec_from_file_location("controlled_experiment_runner", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


def protocol_definition():
    return {
        "schemaVersion": 1,
        "experimentId": "stage2.runner-test-001",
        "candidateAction": {
            "actionId": "observe.measure_only",
            "description": "Read-only measurement candidate.",
            "risk": "LOW",
            "requiredPermission": "none",
            "reversible": True,
            "measurement": "startup timing",
            "rollback": "Not applicable.",
        },
        "execution": {"enabled": False, "deviceMutationAllowed": False},
        "phases": {
            "baseline": {"purpose": "Measure before the reserved intervention slot."},
            "intervention": {"purpose": "Reserved slot; no mutation is allowed."},
            "post": {"purpose": "Measure after the reserved intervention slot."},
        },
        "measurementPlan": {
            "metrics": ["startupMs"],
            "primaryMetric": "startupMs",
        },
    }


class TestRunner(unittest.TestCase):
    def test_manifest_preserves_non_authorizing_contract(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            definition = protocol_definition()
            for phase in ("baseline", "intervention", "post"):
                phase_dir = root / phase
                phase_dir.mkdir()
                (phase_dir / f"rish-baseline-{phase}.txt").write_text("raw\n", encoding="utf-8")
                (phase_dir / f"rish-baseline-{phase}.json").write_text("{}\n", encoding="utf-8")

            manifest_path = MODULE.write_manifest(root, definition)
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

            self.assertFalse(manifest["executionEnabled"])
            self.assertFalse(manifest["deviceMutationAllowed"])
            self.assertEqual(
                list(manifest["phases"]),
                ["baseline", "intervention", "post"],
            )
            self.assertEqual(
                manifest["phases"]["intervention"]["purpose"],
                definition["phases"]["intervention"]["purpose"],
            )

    def test_manifest_rejects_ambiguous_artifact_count(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            definition = protocol_definition()
            for phase in ("baseline", "intervention", "post"):
                phase_dir = root / phase
                phase_dir.mkdir()
                (phase_dir / f"rish-baseline-{phase}.txt").write_text("raw\n", encoding="utf-8")
                (phase_dir / f"rish-baseline-{phase}-extra.txt").write_text("raw\n", encoding="utf-8")
                (phase_dir / f"rish-baseline-{phase}.json").write_text("{}\n", encoding="utf-8")

            with self.assertRaises(RuntimeError):
                MODULE.write_manifest(root, definition)


if __name__ == "__main__":
    unittest.main()
