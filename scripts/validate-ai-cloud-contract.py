#!/usr/bin/env python3
"""Validate the Stage 3 AI Cloud Foundation contract without external packages."""
from __future__ import annotations
import argparse, json
from pathlib import Path
from typing import Any

class ContractError(ValueError): pass

def load(path: Path) -> dict[str, Any]:
    try: value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc: raise ContractError(f"Cannot read JSON: {exc}") from exc
    if not isinstance(value, dict): raise ContractError("Root must be an object")
    return value

def nonempty(value: Any, name: str) -> None:
    if not isinstance(value, str) or not value.strip(): raise ContractError(f"{name} must be a non-empty string")

def validate(value: dict[str, Any]) -> dict[str, Any]:
    if value.get("schemaVersion") != 1: raise ContractError("schemaVersion must be 1")
    obs, rec = value.get("observation"), value.get("recommendation")
    if not isinstance(obs, dict) or not isinstance(rec, dict): raise ContractError("observation and recommendation must be objects")
    nonempty(obs.get("observationId"), "observation.observationId"); nonempty(obs.get("capturedAt"), "observation.capturedAt")
    device, telemetry = obs.get("device"), obs.get("telemetry")
    if not isinstance(device, dict) or not isinstance(telemetry, dict): raise ContractError("device and telemetry are required")
    if not isinstance(device.get("apiLevel"), int) or device["apiLevel"] < 21: raise ContractError("device.apiLevel must be >= 21")
    nonempty(device.get("architecture"), "device.architecture")
    for field in ("batteryPercent","temperatureC","availableRamMb"):
        if not isinstance(telemetry.get(field), (int,float)): raise ContractError(f"telemetry.{field} must be numeric")
    if not 0 <= telemetry["batteryPercent"] <= 100: raise ContractError("batteryPercent must be 0..100")
    if telemetry["availableRamMb"] < 0: raise ContractError("availableRamMb must be >= 0")
    nonempty(rec.get("recommendationId"), "recommendation.recommendationId")
    if rec.get("mode") != "ADVISORY_ONLY": raise ContractError("mode must be ADVISORY_ONLY")
    nonempty(rec.get("diagnosis"), "recommendation.diagnosis")
    if not isinstance(rec.get("confidence"), (int,float)) or not 0 <= rec["confidence"] <= 1: raise ContractError("confidence must be 0..1")
    refs=rec.get("evidenceRefs")
    if not isinstance(refs,list) or not refs or any(not isinstance(x,str) or not x.strip() for x in refs): raise ContractError("evidenceRefs must be non-empty strings")
    action=rec.get("candidateAction")
    if not isinstance(action,dict): raise ContractError("candidateAction must be an object")
    nonempty(action.get("actionId"), "candidateAction.actionId"); nonempty(action.get("reason"), "candidateAction.reason")
    if not isinstance(action.get("reversible"), bool): raise ContractError("candidateAction.reversible must be boolean")
    if action.get("risk") not in {"LOW","MEDIUM","HIGH","NONE"}: raise ContractError("candidateAction.risk is invalid")
    execution=rec.get("execution")
    if not isinstance(execution,dict): raise ContractError("execution must be an object")
    if execution.get("requested") is not False: raise ContractError("Cloud recommendation cannot request execution")
    if execution.get("deviceMutationAllowed") is not False: raise ContractError("Cloud recommendation cannot allow device mutation")
    return {"valid":True,"schemaVersion":1,"advisoryOnly":True,"executionRequested":False,"deviceMutationAllowed":False}

def main() -> int:
    parser=argparse.ArgumentParser(); parser.add_argument("document",type=Path); args=parser.parse_args()
    try: report=validate(load(args.document))
    except ContractError as exc: parser.error(str(exc))
    print(json.dumps(report,indent=2)+"\n",end=""); return 0

if __name__=="__main__": raise SystemExit(main())
