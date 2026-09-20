#!/usr/bin/env python3
"""Analyze repeated normalized device health snapshots.

Measurement-only: this script never executes Android commands and never recommends
or performs an optimization action.
"""
from __future__ import annotations
import argparse, json, math, statistics
from pathlib import Path

NUMERIC = (
    "availableRamMb", "batteryPercent", "batteryTemperatureC",
    "storageFreeBytes", "storageTotalBytes", "uptimeMs"
)

def finite(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool) and math.isfinite(float(v))

def load(path):
    data=json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("snapshot root must be an object")
    if data.get("schemaVersion") != 3:
        raise ValueError("expected schemaVersion 3")
    return data

def summarize(values):
    values=[float(v) for v in values if finite(v)]
    if not values:
        return {"count":0,"average":None,"median":None,"stdev":None,"min":None,"max":None}
    return {
        "count":len(values),
        "average":statistics.mean(values),
        "median":statistics.median(values),
        "stdev":statistics.stdev(values) if len(values)>1 else 0.0,
        "min":min(values),"max":max(values)
    }

def analyze(paths):
    snaps=[load(p) for p in paths]
    identity={(s.get("manufacturer"),s.get("model"),s.get("androidApi")) for s in snaps}
    if len(identity)!=1:
        raise ValueError("device identity differs across snapshots")
    report={
        "reportVersion":1,
        "measurementOnly":True,
        "observationCount":len(snaps),
        "device":{"manufacturer":snaps[0].get("manufacturer"),"model":snaps[0].get("model"),"androidApi":snaps[0].get("androidApi")},
        "fields":{},
        "conditions":{
            "charging":[s.get("isCharging") for s in snaps],
            "networkTransport":[s.get("networkTransport") for s in snaps],
            "networkValidated":[s.get("networkValidated") for s in snaps],
            "thermalStatus":[s.get("thermalStatus") for s in snaps],
            "interactive":[s.get("isInteractive") for s in snaps],
        },
        "interpretation":"descriptive_only"
    }
    for key in NUMERIC:
        report["fields"][key]=summarize([s.get(key) for s in snaps])
    return report

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("snapshots",nargs="+",type=Path)
    ap.add_argument("--out-json",type=Path)
    args=ap.parse_args()
    if len(args.snapshots)<2:
        ap.error("at least two snapshots are required")
    try: report=analyze(args.snapshots)
    except (OSError,ValueError,json.JSONDecodeError) as e: ap.error(str(e))
    text=json.dumps(report,indent=2,ensure_ascii=False)+"\n"
    if args.out_json:
        args.out_json.parent.mkdir(parents=True,exist_ok=True)
        args.out_json.write_text(text,encoding="utf-8")
    else: print(text,end="")

if __name__=="__main__":
    main()
