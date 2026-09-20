#!/usr/bin/env python3
from __future__ import annotations
import argparse,json
from pathlib import Path
def main():
    p=argparse.ArgumentParser(); p.add_argument("dataset",type=Path); p.add_argument("--out",type=Path); a=p.parse_args()
    d=json.loads(a.dataset.read_text()); rows=d.get("rows",[])
    correct=sum(r["label"]["outcomeClass"]=="INSUFFICIENT_EVIDENCE" for r in rows)
    report={"schemaVersion":1,"modelId":"deterministic-baseline-v1","sampleCount":len(rows),"metrics":{"coverage":1 if rows else 0,"abstentionRate":1.0,"labelAgreement":correct/len(rows) if rows else None},"safety":{"executionRequested":False,"deviceMutationAllowed":False},"interpretation":"Descriptive offline baseline only; no device benefit or causal claim."}
    text=json.dumps(report,indent=2)+"\n"
    if a.out:a.out.write_text(text)
    print(text,end="")
if __name__=="__main__": raise SystemExit(main())
