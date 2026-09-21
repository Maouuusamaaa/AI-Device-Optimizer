#!/usr/bin/env python3
import argparse
import json
import statistics
from collections import defaultdict
from pathlib import Path


REQUIRED_SAMPLE_FIELDS = {
    "observationId",
    "threadCount",
    "repetition",
    "wallTimeMs",
    "processCpuTimeMs",
    "processPssBeforeKb",
    "processPssAfterKb",
    "processPssDeltaKb",
    "promptTokens",
    "generatedTokens",
    "loadMs",
    "tokenizationMs",
    "contextInitMs",
    "promptDecodeMs",
    "generationMs",
    "generationTokensPerSecond",
    "totalNativeMs",
    "advisoryOnly",
    "executionRequested",
    "deviceMutationAllowed",
}


def mean(values):
    return statistics.fmean(values) if values else None


def summarize(samples):
    groups = defaultdict(list)
    for sample in samples:
        groups[int(sample["threadCount"])].append(sample)

    by_threads = {}
    for thread_count, group in sorted(groups.items()):
        by_threads[str(thread_count)] = {
            "sampleCount": len(group),
            "repetitions": sorted(int(s["repetition"]) for s in group),
            "wallTimeMs": {
                "mean": mean([s["wallTimeMs"] for s in group]),
                "median": statistics.median([s["wallTimeMs"] for s in group]),
            },
            "processCpuTimeMs": {
                "mean": mean([s["processCpuTimeMs"] for s in group]),
            },
            "processPssDeltaKb": {
                "mean": mean([s["processPssDeltaKb"] for s in group]),
            },
            "loadMs": {
                "mean": mean([s["loadMs"] for s in group]),
            },
            "promptDecodeMs": {
                "mean": mean([s["promptDecodeMs"] for s in group]),
            },
            "generationMs": {
                "mean": mean([s["generationMs"] for s in group]),
            },
            "generationTokensPerSecond": {
                "mean": mean([s["generationTokensPerSecond"] for s in group]),
                "median": statistics.median([s["generationTokensPerSecond"] for s in group]),
            },
            "temperatureBeforeC": {
                "mean": mean([s["temperatureBeforeC"] for s in group if s.get("temperatureBeforeC") is not None]),
            },
            "temperatureAfterC": {
                "mean": mean([s["temperatureAfterC"] for s in group if s.get("temperatureAfterC") is not None]),
            },
            "batteryDeltaPercent": {
                "mean": mean([
                    s["batteryAfterPercent"] - s["batteryBeforePercent"]
                    for s in group
                    if s.get("batteryBeforePercent") is not None and s.get("batteryAfterPercent") is not None
                ]),
            },
            "thinkOutputCount": sum(1 for s in group if s.get("modelOutputContainsThink") is True),
        }

    safety_ok = all(
        sample.get("advisoryOnly") is True
        and sample.get("executionRequested") is False
        and sample.get("deviceMutationAllowed") is False
        for sample in samples
    )

    return {
        "schemaVersion": 1,
        "benchmarkId": None,
        "sampleCount": len(samples),
        "threadConfigurations": sorted(groups),
        "safetyContractIntact": safety_ok,
        "byThreadConfiguration": by_threads,
        "interpretation": "descriptive_measurements_only",
    }


def load_and_validate(path):
    root = json.loads(Path(path).read_text(encoding="utf-8"))
    if root.get("schemaVersion") != 1:
        raise ValueError("Unsupported benchmark schemaVersion")
    samples = root.get("samples")
    if not isinstance(samples, list) or not samples:
        raise ValueError("samples must be a non-empty list")
    for index, sample in enumerate(samples):
        missing = REQUIRED_SAMPLE_FIELDS - set(sample)
        if missing:
            raise ValueError(f"sample {index} missing fields: {sorted(missing)}")
        if not sample["observationId"]:
            raise ValueError(f"sample {index} has empty observationId")
        if not isinstance(sample["threadCount"], int):
            raise ValueError(f"sample {index} threadCount must be int")
        if sample["threadCount"] < 1 or sample["threadCount"] > 8:
            raise ValueError(f"sample {index} threadCount outside 1..8")
        if not isinstance(sample["generationTokensPerSecond"], (int, float)):
            raise ValueError(f"sample {index} generationTokensPerSecond must be numeric")
        if not sample["advisoryOnly"] or sample["executionRequested"] or sample["deviceMutationAllowed"]:
            raise ValueError(f"sample {index} violates safety contract")
    return root, samples


def main():
    parser = argparse.ArgumentParser(description="Analyze Stage 9 local inference benchmark results.")
    parser.add_argument("input", help="Stage 9 JSON result file")
    parser.add_argument("--output", help="Optional output JSON path")
    args = parser.parse_args()

    root, samples = load_and_validate(args.input)
    summary = summarize(samples)
    summary["benchmarkId"] = root.get("benchmarkId")
    summary["modelId"] = root.get("model", {}).get("modelId")
    summary["runtimeVersion"] = root.get("model", {}).get("runtimeVersion")

    rendered = json.dumps(summary, indent=2, sort_keys=True)
    if args.output:
        Path(args.output).write_text(rendered + "\n", encoding="utf-8")
    else:
        print(rendered)


if __name__ == "__main__":
    main()
