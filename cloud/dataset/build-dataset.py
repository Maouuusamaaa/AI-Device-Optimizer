#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path
REQUIRED=("batteryPercent","temperatureC","availableRamMb")
def row_from_observation(obs, source, index):
    samples=obs.get("samples") or []
    if not samples: raise ValueError(f"{source}: samples are empty")
    first=samples[0]
    for key in REQUIRED:
        if not isinstance(first.get(key),(int,float)): raise ValueError(f"{source}: missing {key}")
    features={k:first[k] for k in REQUIRED}
    for k in ("processCpuTimeMs","storageAvailableMb"):
        if isinstance(first.get(k),(int,float)): features[k]=first[k]
    digest=hashlib.sha256((source+":"+str(index)).encode()).hexdigest()[:16]
    return {"schemaVersion":1,"rowId":f"row-{digest}","observationRef":source,"features":features,"label":{"outcomeClass":"INSUFFICIENT_EVIDENCE"},"provenance":{"source":source,"split":"unassigned"}}
def main():
    p=argparse.ArgumentParser(); p.add_argument("inputs",nargs="+",type=Path); p.add_argument("--out",required=True,type=Path); a=p.parse_args()
    rows=[]
    for path in a.inputs:
        data=json.loads(path.read_text(encoding="utf-8"))
        if "samples" in data: rows.append(row_from_observation(data,str(path),len(rows)))
        elif isinstance(data.get("internalObservations"),list):
            for i,obs in enumerate(data["internalObservations"]): rows.append(row_from_observation(obs,f"{path}#internal-{i}",i))
        else: raise ValueError(f"{path}: unsupported benchmark artifact")
    payload={"datasetSchemaVersion":1,"rowCount":len(rows),"rows":rows,"policy":{"labelsAreConservative":True,"privateDataMustRemainOutsideRepository":True}}
    a.out.parent.mkdir(parents=True,exist_ok=True); a.out.write_text(json.dumps(payload,indent=2)+"\n",encoding="utf-8"); print(json.dumps({"rowCount":len(rows),"output":str(a.out)},indent=2))
if __name__=="__main__": raise SystemExit(main())
