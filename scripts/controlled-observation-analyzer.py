#!/usr/bin/env python3
"""Analyze a three-phase controlled observation.

This tool is measurement-only. It validates the observation structure, compares the
external Rish metrics descriptively, and writes JSON/Markdown reports. It never
executes device commands or makes optimization decisions.
"""
from __future__ import annotations
import argparse, json, math, statistics
from pathlib import Path
from typing import Any

PHASES = ("baseline", "intervention", "post")
METRICS = (
    ("startupMs", "average"), ("startupMs", "median"), ("startupMs", "stdev"),
    ("waitMs", "average"), ("waitMs", "median"), ("waitMs", "stdev"),
    ("memoryKb", "pss"), ("memoryKb", "rss"), ("memoryKb", "swapPss"),
)

class ObservationError(ValueError):
    pass

def load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ObservationError(f"Cannot read JSON: {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise ObservationError(f"JSON root must be an object: {path}")
    return value

def resolve_phase_json(root: Path, phase: str, manifest_phase: dict[str, Any]) -> Path:
    phase_dir = root / phase
    if not phase_dir.is_dir():
        raise ObservationError(f"Missing phase directory: {phase_dir}")
    raw_text = manifest_phase.get("rawText")
    if not isinstance(raw_text, str) or not raw_text.strip():
        raise ObservationError(f"Manifest phase {phase!r} has no rawText path")
    raw_path = phase_dir / Path(raw_text).name
    if not raw_path.is_file():
        raise ObservationError(f"Manifest rawText does not exist in {phase}: {raw_path}")
    candidates = sorted(phase_dir.glob("*.json"))
    if len(candidates) != 1:
        raise ObservationError(f"Expected exactly one JSON artifact in {phase_dir}, found {len(candidates)}")
    return candidates[0]

def finite_number(value: Any, label: str) -> float | int:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ObservationError(f"{label} must be numeric")
    if not math.isfinite(float(value)):
        raise ObservationError(f"{label} must be finite")
    return value

def validate_summary_against_samples(phase: str, name: str, section: dict[str, Any], samples: list[Any]) -> None:
    expected = {
        "average": statistics.mean(samples),
        "median": statistics.median(samples),
        "stdev": statistics.stdev(samples),
        "min": min(samples),
        "max": max(samples),
    }
    for key, actual in expected.items():
        observed = section[key]
        if not math.isclose(float(observed), float(actual), rel_tol=1e-9, abs_tol=1e-9):
            raise ObservationError(
                f"{phase}: {name}.{key} does not match samples "
                f"(reported={observed!r}, expected={actual!r})"
            )


def validate_phase(phase: str, data: dict[str, Any]) -> dict[str, Any]:
    external = data.get("externalRish")
    if not isinstance(external, dict):
        raise ObservationError(f"{phase}: missing externalRish")
    device = external.get("device")
    if not isinstance(device, dict):
        raise ObservationError(f"{phase}: missing externalRish.device")
    for key in ("manufacturer", "model", "androidApi"):
        if device.get(key) in (None, ""):
            raise ObservationError(f"{phase}: missing device.{key}")
    startup, wait, memory = external.get("startupMs"), external.get("waitMs"), external.get("memoryKb")
    if not all(isinstance(value, dict) for value in (startup, wait, memory)):
        raise ObservationError(f"{phase}: incomplete external metric sections")
    for name, section in (("startupMs", startup), ("waitMs", wait)):
        samples = section.get("samples")
        if not isinstance(samples, list) or len(samples) < 2:
            raise ObservationError(f"{phase}: {name}.samples must contain at least 2 values")
        for i, sample in enumerate(samples):
            finite_number(sample, f"{phase}:{name}.samples[{i}]")
        for key in ("average", "median", "stdev", "min", "max", "sampleCount"):
            finite_number(section.get(key), f"{phase}:{name}.{key}")
        if not isinstance(section["sampleCount"], int) or isinstance(section["sampleCount"], bool):
            raise ObservationError(f"{phase}: {name}.sampleCount must be an integer")
        if section["sampleCount"] != len(samples):
            raise ObservationError(f"{phase}: {name}.sampleCount does not match samples length")
        validate_summary_against_samples(phase, name, section, samples)
    for key in ("pss", "rss", "swapPss"):
        finite_number(memory.get(key), f"{phase}:memoryKb.{key}")
    for key in ("batteryPercent", "temperatureC"):
        if external.get(key) is not None:
            finite_number(external[key], f"{phase}:{key}")
    return external

def percent_delta(before: float | int | None, after: float | int | None) -> float | None:
    if before is None or after is None or before == 0:
        return None
    return ((float(after) - float(before)) / float(before)) * 100.0

def compare_metric(baseline, experiment, post, section, key):
    values = {"baseline": baseline[section][key], "experiment": experiment[section][key], "post": post[section][key]}
    base = values["baseline"]
    return {
        "metric": f"{section}.{key}", **values,
        "interventionVsBaseline": {"absoluteDelta": values["intervention"] - base, "percentDelta": percent_delta(base, values["intervention"])},
        "postVsBaseline": {"absoluteDelta": values["post"] - base, "percentDelta": percent_delta(base, values["post"])},
    }

def analyze(root: Path) -> dict[str, Any]:
    manifest_path = root / "manifest.json"
    if not manifest_path.is_file():
        raise ObservationError(f"Missing manifest: {manifest_path}")
    manifest = load_json(manifest_path)
    if manifest.get("schemaVersion") != 1:
        raise ObservationError(f"Unsupported manifest schemaVersion: {manifest.get('schemaVersion')!r}")
    if manifest.get("executionEnabled") is not False:
        raise ObservationError("Controlled observation must have executionEnabled=false")
    if manifest.get("deviceMutationAllowed") is not False:
        raise ObservationError("Controlled observation must have deviceMutationAllowed=false")
    phases = manifest.get("phases")
    if not isinstance(phases, dict):
        raise ObservationError("Manifest phases must be an object")
    missing = [p for p in PHASES if p not in phases]
    if missing:
        raise ObservationError(f"Missing phases: {', '.join(missing)}")

    data = {}
    for phase in PHASES:
        if not isinstance(phases[phase], dict):
            raise ObservationError(f"Manifest phase {phase!r} must be an object")
        path = resolve_phase_json(root, phase, phases[phase])
        data[phase] = {"json": str(path.relative_to(root)), "external": validate_phase(phase, load_json(path))}

    devices = [
        (data[p]["external"]["device"]["manufacturer"], data[p]["external"]["device"]["model"], data[p]["external"]["device"]["androidApi"])
        for p in PHASES
    ]
    if len(set(devices)) != 1:
        raise ObservationError("Device identity differs between controlled-observation phases")

    sample_counts = {p: data[p]["external"]["startupMs"]["sampleCount"] for p in PHASES}
    wait_counts = {p: data[p]["external"]["waitMs"]["sampleCount"] for p in PHASES}
    if len(set(sample_counts.values())) != 1:
        raise ObservationError("Startup sample counts differ between phases")
    if wait_counts != sample_counts:
        raise ObservationError("Startup and wait sample counts differ")

    baseline, intervention, post = (data[p]["external"] for p in PHASES)
    return {
        "reportVersion": 1,
        "measurementOnly": True,
        "source": str(root),
        "manifest": {
            "schemaVersion": 1,
            "candidateActionId": manifest.get("candidateActionId"),
            "executionEnabled": False,
            "deviceMutationAllowed": False,
        },
        "validation": {
            "requiredPhasesPresent": True,
            "deviceConsistent": True,
            "device": {"manufacturer": devices[0][0], "model": devices[0][1], "androidApi": devices[0][2]},
            "sampleCounts": sample_counts,
            "waitSampleCounts": wait_counts,
        },
        "phases": {
            p: {
                "json": data[p]["json"],
                "timestamp": data[p]["external"].get("timestamp"),
                "batteryPercent": data[p]["external"].get("batteryPercent"),
                "temperatureC": data[p]["external"].get("temperatureC"),
                "startupMs": data[p]["external"]["startupMs"],
                "waitMs": data[p]["external"]["waitMs"],
                "memoryKb": data[p]["external"]["memoryKb"],
            } for p in PHASES
        },
        "comparisons": {
            "metrics": [compare_metric(baseline, experiment, post, s, k) for s, k in METRICS],
            "batteryPercent": {p: data[p]["external"].get("batteryPercent") for p in PHASES},
            "temperatureC": {p: data[p]["external"].get("temperatureC") for p in PHASES},
        },
        "interpretation": {
            "conclusion": "descriptive_only",
            "text": "The report compares repeated read-only observations. It does not attribute observed differences to an optimization action because all phases use the same measurement routine.",
        },
    }

def fmt(value):
    if value is None: return "n/a"
    return f"{value:.3f}" if isinstance(value, float) else str(value)

def markdown_report(report):
    v = report["validation"]
    lines = [
        "# Controlled Observation Report", "",
        "Measurement-only comparison of the three read-only phases.", "",
        "## Validation", "",
        f"- Device: {v['device']['manufacturer']} {v['device']['model']}",
        f"- Android API: {v['device']['androidApi']}",
        f"- Device consistent: {v['deviceConsistent']}",
        f"- Execution enabled: {report['manifest']['executionEnabled']}",
        f"- Device mutation allowed: {report['manifest']['deviceMutationAllowed']}", "",
        "## Phase measurements", "",
        "| Metric | Baseline | Intervention | Post |", "|---|---:|---:|---:|",
    ]
    for m in report["comparisons"]["metrics"]:
        lines.append(f"| {m['metric']} | {fmt(m['baseline'])} | {fmt(m['intervention'])} | {fmt(m['post'])} |")
    lines += [
        "", "## Device conditions", "",
        "| Condition | Baseline | Experiment | Post |", "|---|---:|---:|---:|",
        f"| Battery (%) | {fmt(report['comparisons']['batteryPercent']['baseline'])} | {fmt(report['comparisons']['batteryPercent']['experiment'])} | {fmt(report['comparisons']['batteryPercent']['post'])} |",
        f"| Temperature (°C) | {fmt(report['comparisons']['temperatureC']['baseline'])} | {fmt(report['comparisons']['temperatureC']['experiment'])} | {fmt(report['comparisons']['temperatureC']['post'])} |",
        "", "## Interpretation", "", report["interpretation"]["text"], "",
        "No optimization-effect, causality, or performance-improvement claim is generated by this tool.", "",
    ]
    return "\n".join(lines)

def main():
    parser = argparse.ArgumentParser(description="Analyze a controlled three-phase observation.")
    parser.add_argument("root", type=Path)
    parser.add_argument("--out-json", type=Path)
    parser.add_argument("--out-md", type=Path)
    args = parser.parse_args()
    try:
        report = analyze(args.root)
    except ObservationError as exc:
        parser.error(str(exc))
    rendered = json.dumps(report, indent=2, ensure_ascii=False) + "\n"
    if args.out_json:
        args.out_json.parent.mkdir(parents=True, exist_ok=True)
        args.out_json.write_text(rendered, encoding="utf-8")
    if args.out_md:
        args.out_md.parent.mkdir(parents=True, exist_ok=True)
        args.out_md.write_text(markdown_report(report), encoding="utf-8")
    if not args.out_json and not args.out_md:
        print(rendered, end="")
    else:
        if args.out_json: print(f"Saved: {args.out_json}")
        if args.out_md: print(f"Saved: {args.out_md}")

if __name__ == "__main__":
    raise SystemExit(main())
