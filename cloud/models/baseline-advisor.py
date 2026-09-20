#!/usr/bin/env python3
from __future__ import annotations
import argparse,json
from pathlib import Path
def recommend(features):
    if any(k not in features for k in ("batteryPercent","temperatureC","availableRamMb")):
        return {"actionId":"OBSERVE_ONLY","confidence":0.0,"abstain":True,"reason":"missing required telemetry"}
    if features["temperatureC"]>=45:
        return {"actionId":"OBSERVE_ONLY","confidence":0.80,"abstain":True,"reason":"temperature requires observation before intervention"}
    return {"actionId":"OBSERVE_ONLY","confidence":0.60,"abstain":True,"reason":"no validated intervention evidence"}
def main():
    p=argparse.ArgumentParser(); p.add_argument("input",type=Path); p.add_argument("--out",type=Path); a=p.parse_args()
    d=json.loads(a.input.read_text()); rows=d.get("rows",[])
    pred=[{"rowId":r["rowId"],"recommendation":recommend(r["features"])} for r in rows]
    report={"schemaVersion":1,"modelId":"deterministic-baseline-v1","rowCount":len(pred),"abstentionRate":sum(x["recommendation"]["abstain"] for x in pred)/len(pred) if pred else 1.0,"predictions":pred,"execution":{"requested":False,"deviceMutationAllowed":False}}
    text=json.dumps(report,indent=2)+"\n"
    if a.out:a.out.write_text(text)
    print(text,end="")
if __name__=="__main__": raise SystemExit(main())
