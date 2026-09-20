#!/usr/bin/env python3
"""Run a Stage 2 controlled experiment without enabling device mutation.

The runner executes the existing read-only Rish measurement routine for the
baseline, reserved intervention, and post phases. It never invokes an Android
mutation command and requires the Stage 2 protocol to remain non-authorizing.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PROTOCOL = ROOT / "scripts" / "experiment-protocol.py"
ANALYZER = ROOT / "scripts" / "controlled-observation-analyzer.py"
RISH_BASELINE = ROOT / "scripts" / "rish-baseline.sh"


def load_module(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load module: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def run_measurement(phase_dir: Path) -> list[Path]:
    phase_dir.mkdir(parents=True, exist_ok=True)
    before = set(phase_dir.glob("rish-baseline-*"))
    subprocess.run(
        ["bash", str(RISH_BASELINE), str(phase_dir)],
        cwd=ROOT,
        check=True,
    )
    after = sorted(
        (p for p in phase_dir.glob("rish-baseline-*") if p not in before),
        key=lambda p: p.stat().st_mtime,
    )
    txt = [p for p in after if p.suffix == ".txt"]
    js = [p for p in after if p.suffix == ".json"]
    if len(txt) != 1 or len(js) != 1:
        raise RuntimeError(
            f"{phase_dir}: expected one new TXT and JSON artifact; "
            f"found txt={len(txt)} json={len(js)}"
        )
    return [txt[0], js[0]]


def write_manifest(root: Path, definition: dict) -> Path:
    phases = {}
    for phase in ("baseline", "intervention", "post"):
        phase_dir = root / phase
        txt = sorted(phase_dir.glob("rish-baseline-*.txt"))
        js = sorted(phase_dir.glob("rish-baseline-*.json"))
        if len(txt) != 1 or len(js) != 1:
            raise RuntimeError(
                f"{phase}: expected exactly one TXT and JSON artifact; "
                f"found txt={len(txt)} json={len(js)}"
            )
        phases[phase] = {
            "rawText": txt[0].name,
            "json": js[0].name,
            "purpose": definition["phases"][phase]["purpose"],
        }

    manifest = {
        "schemaVersion": 1,
        "experimentId": definition["experimentId"],
        "candidateActionId": definition["candidateAction"]["actionId"],
        "executionEnabled": False,
        "deviceMutationAllowed": False,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "phases": phases,
    }
    path = root / "manifest.json"
    path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return path


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Run a measurement-only Stage 2 controlled experiment."
    )
    parser.add_argument("definition", type=Path)
    parser.add_argument(
        "--output",
        type=Path,
        default=ROOT / "benchmarks" / "experiments" / "stage2-run",
    )
    args = parser.parse_args()

    definition = load_module(PROTOCOL, "experiment_protocol")
    protocol = definition.validate(definition.load_definition(args.definition))
    if not protocol["measurementOnly"] or protocol["executionAllowed"]:
        raise RuntimeError("Stage 2 protocol is not measurement-only")

    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)

    for phase in ("baseline", "intervention", "post"):
        print(f"== {phase}: read-only measurement ==")
        run_measurement(output / phase)

    manifest = write_manifest(output, protocol)

    subprocess.run(
        [sys.executable, str(ANALYZER), str(output), "--out-json", str(output / "controlled-report.json"),
         "--out-md", str(output / "controlled-report.md")],
        cwd=ROOT,
        check=True,
    )
    print(f"Saved: {manifest}")
    print(f"Evidence: {output}")
    print("executionEnabled=false")
    print("deviceMutationAllowed=false")
    print("intervention phase is reserved; no device mutation was executed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
