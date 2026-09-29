#!/usr/bin/env python3
from __future__ import annotations
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
errors = []
required = [
"PROJECT_RULES.md","AGENTS.md","CONTRIBUTING.md","PROJECT_STATE.json",
"SECURITY.md","PRIVACY.md","docs/protocols/RESEARCH_PROTOCOL.md",
"docs/protocols/EXPERIMENT_PROTOCOL.md","docs/protocols/HANDOFF_PROTOCOL.md",
"docs/traceability/README.md","docs/adr/README.md","findings/README.md",
"experiments/README.md","risks/README.md","handoffs/README.md",
".github/CODEOWNERS",".github/PULL_REQUEST_TEMPLATE.md"
]
for rel in required:
    if not (ROOT / rel).is_file():
        errors.append(f"missing required governance file: {rel}")
try:
    state = json.loads((ROOT/"PROJECT_STATE.json").read_text(encoding="utf-8"))
except Exception as exc:
    errors.append(f"PROJECT_STATE.json invalid: {exc}")
else:
    for key in ("schema_version","governance_version","status","current_milestone","required_reading"):
        if key not in state:
            errors.append(f"PROJECT_STATE.json missing key: {key}")
    if state.get("governance_version") != "1.0":
        errors.append("unexpected governance_version")
rules = (ROOT/"PROJECT_RULES.md").read_text(encoding="utf-8") if (ROOT/"PROJECT_RULES.md").is_file() else ""
for phrase in ["canonical record","AI output is untrusted","local Safety Gate","Change classification","Negative, failed, inconclusive","No fabricated results"]:
    if phrase not in rules:
        errors.append(f"PROJECT_RULES.md missing control: {phrase}")
if errors:
    print("Governance validation FAILED")
    for e in errors: print("-", e)
    sys.exit(1)
print(f"Governance validation PASSED: {len(required)} paths checked.")
