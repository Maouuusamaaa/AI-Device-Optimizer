# Fresh-Process Lifecycle Reset Control Experiment Design

## Status

Proposed design approved in conversation. This document defines the next controlled experiment; implementation is intentionally separate from this design.

## Objective

Determine whether the persistent PSS transitions observed in the existing runtime lifecycle diagnostic are specifically associated with the explicit native reset boundary, or whether similar transitions occur without reset.

The experiment must reduce the major confounder in the existing reset-vs-no-reset results: the two runs began with substantially different process PSS states.

## Experimental design

Use fresh-process paired runs on the same itel P661N device and the same v0.1.13 application build.

Each run follows the same lifecycle:

1. Start from a fresh application process.
2. Record a 60-second baseline.
3. Execute exactly one Qwen3 0.6B Q4_0 advisory inference using the established diagnostic settings: 4 threads and 64 maximum output tokens.
4. Observe 60 seconds after model/context cleanup.
5. Select exactly one lifecycle branch:
   - Reset-enabled: invoke the explicit native reset boundary and then observe for 5 minutes.
   - No-reset control: skip the explicit reset and observe for 5 minutes.
6. Persist all samples and lifecycle events in the existing evidence JSON format.

A paired comparison consists of one reset-enabled run and one no-reset control run performed under comparable device conditions. Multiple pairs should be collected before drawing a reset-specific conclusion.

## Process-state isolation

The benchmark should explicitly capture process identity/state sufficient to verify the fresh-process condition, including PID and process-start metadata where available.

The benchmark must not kill processes, force-stop packages, change Android settings, or otherwise mutate the device beyond launching/running the diagnostic normally. Fresh-process isolation therefore means the benchmark starts from a newly launched application process rather than programmatically terminating an existing process.

If the application lifecycle makes a truly fresh process impossible to guarantee from inside the app, the evidence must record that limitation instead of claiming isolation.

## Measurements

Retain the existing 2-second sampling interval and collect:

- PSS
- RSS
- Swap PSS
- available RAM
- temperature

Also retain wall-clock lifecycle events and the existing phase labels.

For each phase, record the first value, final value, minimum, maximum, and every discrete PSS transition detected by the analysis tooling. The analysis should report transition timestamps relative to the relevant lifecycle boundary.

Where practical, record both wall-clock timestamps and monotonic elapsed time for reset/control boundaries so clock changes cannot affect temporal correlation.

## Comparability controls

Reset-enabled and no-reset runs must use identical:

- application build/version
- model and model revision
- inference parameters
- phase durations
- sampling interval
- benchmark code
- device
- observation protocol

Runs should be collected under similar battery, temperature, and available-memory conditions. These environmental values are covariates, not reasons to discard a run after seeing its outcome.

The analysis must compare within paired runs before comparing across unrelated runs.

## Analysis and decision criteria

The primary question is whether a persistent PSS transition occurs consistently in reset-enabled runs but not in paired no-reset controls.

Interpretation:

- Reset-only reproducibility: the transition repeatedly appears in reset-enabled runs and does not appear in controls. This strengthens evidence for a reset-specific association.
- Both-mode reproducibility: comparable transitions occur in both modes. This argues against the explicit reset being a sufficient explanation.
- Neither-mode reproducibility: the transition does not recur. The earlier observation is not reproduced under the controlled protocol.
- Mixed results: evidence remains inconclusive and requires additional replication or a narrower experiment.

A memory leak must not be declared from PSS alone. Any interpretation must consider RSS, Swap PSS, available RAM, temperature, process lifetime, and the complete time series.

## Acceptance criteria

The experiment is considered complete when:

1. The fresh-process condition is either demonstrated by recorded process metadata or explicitly documented as a limitation.
2. At least two comparable reset/control pairs are collected, unless the first pair produces an implementation or instrumentation failure requiring correction.
3. All runs contain complete lifecycle events and memory samples.
4. The analysis identifies all material PSS transitions and their timing.
5. The conclusion follows the decision criteria above without labeling a leak unless independently supported.
6. No production runtime behavior is changed by the experiment beyond the existing diagnostic/control pathways.
7. VersionName remains 0.1.13 unless a separate milestone decision is made.

## Scope boundaries

This experiment does not attempt to:

- optimize memory usage;
- change llama.cpp allocator behavior;
- change production reset semantics;
- prove a specific allocator implementation defect;
- benchmark unrelated Android processes;
- introduce a new model or inference configuration.

If the experiment establishes a reset-specific association, the next investigation can target the native allocation/free path with a narrower hypothesis.

## Evidence and repository workflow

Each benchmark result remains an immutable evidence artifact under `benchmarks/results/`.

The experiment implementation should be delivered as a bounded change with contract tests, documentation updates, CI validation, and a GitHub issue/PR trail. The versionName remains 0.1.13 while the experiment is open.

## Risks and mitigations

- Process freshness may be difficult to prove from inside the app. Mitigation: record PID/process-start metadata and state the limitation if Android lifecycle behavior prevents proof.
- Device memory pressure can change between pairs. Mitigation: use paired runs and retain environmental measurements.
- PSS can change asynchronously long after reset. Mitigation: keep the full 5-minute observation and timestamp every detected transition.
- Wall-clock time can jump. Mitigation: add monotonic elapsed timing where supported.
- A single run can be anomalous. Mitigation: require repeated paired observations before a reset-specific conclusion.
