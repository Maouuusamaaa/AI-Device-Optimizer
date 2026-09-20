#!/usr/bin/env python3
"""Validate a Stage 2 controlled optimization experiment definition.

This module is measurement-only. It defines the protocol needed for a future
real intervention, but it refuses to authorize or execute device mutation.
"""
from __future__ import annotations
import argparse, json
from pathlib import Path
from typing import Any

PHASES = ("baseline", "intervention", "post")
REQUIRED_ACTION_FIELDS = ("actionId","description","risk","requiredPermission","reversible","measurement","rollback")

class ExperimentProtocolError(ValueError):
    pass

def load_definition(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ExperimentProtocolError(f"Cannot read experiment definition: {exc}") from exc
    if not isinstance(value, dict):
        raise ExperimentProtocolError("Experiment definition root must be an object")
    return value

def validate(definition: dict[str, Any]) -> dict[str, Any]:
    if definition.get("schemaVersion") != 1:
        raise ExperimentProtocolError("Unsupported schemaVersion; expected 1")
    experiment_id = definition.get("experimentId")
    if not isinstance(experiment_id, str) or not experiment_id.strip():
        raise ExperimentProtocolError("experimentId must be a non-empty string")
    action = definition.get("candidateAction")
    if not isinstance(action, dict):
        raise ExperimentProtocolError("candidateAction must be an object")
    for field in REQUIRED_ACTION_FIELDS:
        if field not in action:
            raise ExperimentProtocolError(f"candidateAction missing {field!r}")
    if action["risk"] not in {"LOW","MEDIUM","HIGH"}:
        raise ExperimentProtocolError("candidateAction.risk must be LOW, MEDIUM, or HIGH")
    if not isinstance(action["reversible"], bool):
        raise ExperimentProtocolError("candidateAction.reversible must be boolean")
    execution = definition.get("execution")
    if not isinstance(execution, dict):
        raise ExperimentProtocolError("execution must be an object")
    if execution.get("enabled") is not False:
        raise ExperimentProtocolError("Stage 2 protocol requires execution.enabled=false")
    if execution.get("deviceMutationAllowed") is not False:
        raise ExperimentProtocolError("Stage 2 protocol requires execution.deviceMutationAllowed=false")
    phases = definition.get("phases")
    if not isinstance(phases, dict):
        raise ExperimentProtocolError("phases must be an object")
    missing = [phase for phase in PHASES if phase not in phases]
    if missing:
        raise ExperimentProtocolError(f"Missing phases: {', '.join(missing)}")
    for phase in PHASES:
        value = phases[phase]
        if not isinstance(value, dict):
            raise ExperimentProtocolError(f"Phase {phase!r} must be an object")
        if not isinstance(value.get("purpose"), str) or not value["purpose"].strip():
            raise ExperimentProtocolError(f"Phase {phase!r} needs a non-empty purpose")
    measurement = definition.get("measurementPlan")
    if not isinstance(measurement, dict):
        raise ExperimentProtocolError("measurementPlan must be an object")
    metrics = measurement.get("metrics")
    if not isinstance(metrics, list) or not metrics:
        raise ExperimentProtocolError("measurementPlan.metrics must be a non-empty list")
    if any(not isinstance(metric, str) or not metric.strip() for metric in metrics):
        raise ExperimentProtocolError("measurementPlan.metrics must contain strings")
    return {
        "protocolVersion": 1,
        "measurementOnly": True,
        "experimentId": experiment_id,
        "candidateAction": action,
        "execution": {"enabled": False, "deviceMutationAllowed": False, "status": "BLOCKED_PENDING_REAL_ACTION_ENGINE"},
        "phases": {phase: phases[phase] for phase in PHASES},
        "measurementPlan": {"metrics": metrics, "primaryMetric": measurement.get("primaryMetric")},
        "interpretation": "descriptive_only: this protocol validates experiment structure and measurement coverage; it does not authorize, execute, rank, or attribute an optimization action.",
        "policySelectionAllowed": False,
        "executionAllowed": False,
    }

def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a Stage 2 experiment definition.")
    parser.add_argument("definition", type=Path)
    parser.add_argument("--out-json", type=Path)
    args = parser.parse_args()
    try:
        report = validate(load_definition(args.definition))
    except ExperimentProtocolError as exc:
        parser.error(str(exc))
    rendered = json.dumps(report, indent=2, ensure_ascii=False) + "\n"
    if args.out_json:
        args.out_json.parent.mkdir(parents=True, exist_ok=True)
        args.out_json.write_text(rendered, encoding="utf-8")
        print(f"Saved: {args.out_json}")
    else:
        print(rendered, end="")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
