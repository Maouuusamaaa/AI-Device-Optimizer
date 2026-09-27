# Runtime Lifecycle Memory Diagnostic

This experiment isolates local AI runtime memory retention by separating model/context cleanup from the explicit backend reset boundary.

Protocol:
1. 60 seconds baseline.
2. One Qwen3 0.6B Q4_0 advisory inference using 4 threads and 64 maximum output tokens.
3. The diagnostic inference frees the sampler, context, and model but deliberately keeps llama.cpp backend-global state alive.
4. 60 seconds post-cleanup observation.
5. Invoke the explicit native runtime reset boundary, which releases the backend-global state.
6. 5 minutes post-reset observation.

Why this boundary matters:
- The normal production generation path still releases model, context, sampler, and backend state.
- The diagnostic-only path changes only the backend lifetime so that "model/context cleanup" and "explicit backend reset" are experimentally distinguishable.
- Current llama.cpp documentation describes llama_backend_init() as program-start initialization and llama_backend_free() as program-end cleanup; it is therefore not valid to treat a second backend_free() after normal cleanup as an independent lifecycle phase. citeturn0search0turn0search2

Interpretation:
- A PSS change during post-cleanup, before reset, points toward model/context destruction or allocator behavior rather than the reset call itself.
- A discrete PSS change immediately after reset, with post-cleanup stable, is evidence associated with the backend reset boundary.
- PSS remaining elevated after reset and throughout recovery is evidence of persistent retention, but does not diagnose a memory leak.
- Results must be interpreted together with RSS, Swap PSS, available RAM, temperature, and the complete time series.

Safety:
- Read-only observation.
- No device optimization or system mutation.
- No process killing or package force-stop.
- The benchmark does not change Android system settings.

Evidence is automatically queued and synchronized through the existing GitHub Evidence Sync mechanism.

## Current real-device evidence

The run `benchmarks/results/runtime-lifecycle-memory-1790261009934.json` was collected on an itel P661N running Android API 33 with the separated cleanup/reset protocol (schema v2).

Observed phase-level PSS:
- Baseline: 35,765 KiB → 49,412 KiB (+13,647 KiB at the final baseline sample).
- Post-cleanup: 49,412 KiB → 49,412 KiB (stable across the phase).
- Post-reset: 49,412 KiB → 72,779 KiB (+23,367 KiB). The jump occurred at sample 118, approximately 234 seconds into the 300-second post-reset phase, and remained stable through the final sample.

The post-cleanup stability supports the intended lifecycle separation. However, the post-reset PSS change was delayed rather than immediate at the reset boundary, so this run does not establish that the reset call caused the elevation. RSS decreased overall during post-reset, available RAM increased, and temperature decreased; therefore the observation should not be labeled as a confirmed runaway memory leak.

The investigation remains open pending at least one additional controlled run on the same device/build. VersionName remains 0.1.13 while this evidence is being reproduced and interpreted.

Tracking issue: #71 — reproduce post-reset PSS elevation on itel P661N.


## Reset-boundary timestamp instrumentation

Schema version 3 records an `events` array alongside the memory samples. The lifecycle diagnostic emits explicit wall-clock events for the baseline, inference, cleanup-observation, reset, and post-reset boundaries. In particular, `reset_before` is captured immediately before the Kotlin-to-JNI reset call, while `reset_native_completed` is returned by the native `nativeResetRuntime()` after `llama_backend_free()` completes. `post_reset_start` is captured immediately after the JNI call returns.

This makes the next real-device run capable of placing every PSS sample relative to the native reset completion instead of inferring the reset boundary only from phase order. The reset completion timestamp is observational metadata only and does not change production generation behavior.


## Reset vs no-reset control experiment

The lifecycle diagnostic now supports two explicitly labeled modes without changing the production runtime path:

- `reset_enabled`: baseline → advisory inference → post-cleanup → `llama_backend_free()` through the native reset boundary → 5-minute post-reset observation.
- `no_reset_control`: baseline → the same advisory inference → post-cleanup → 5-minute observation with the explicit reset call skipped.

Both modes use the same inference parameters and observation durations. Schema version 4 records `experiment.mode` and `experiment.resetEnabled`, while samples use `post_reset` or `post_no_reset` respectively.

Interpretation:
- A persistent PSS transition present in reset-enabled runs but absent in no-reset controls strengthens the association with the explicit reset boundary.
- A similar transition in both modes indicates that reset is not sufficient to explain the observation.
- Absence of the transition in repeated runs means the earlier observation was not reproduced under the same controlled conditions.
- None of these outcomes alone establishes a memory leak; RSS, Swap PSS, available RAM, temperature, and the complete time series remain part of the evidence.


## Fresh-process paired control experiment

The controlled experiment uses the same lifecycle in two modes while recording process identity and monotonic timing. The benchmark records the current PID and, when readable from Android, the Linux process start-time tick from /proc/<pid>/stat. If the start-time metadata cannot be read, the result explicitly records that limitation rather than claiming process freshness.

Each arm remains:

1. 60-second baseline.
2. One Qwen3 0.6B Q4_0 advisory inference with 4 threads and 64 maximum output tokens.
3. 60-second post-cleanup observation.
4. Either the explicit native reset followed by 5 minutes of post-reset observation, or a 5-minute no-reset control observation.

Every lifecycle event and sample has a monotonic elapsed timestamp in addition to the existing wall-clock timestamp. Schema version 5 also records process metadata and a `pssTransitions` array containing every observed change in the benchmark process PSS, including phase, timestamps, before/after PSS, and delta.

The purpose is to compare paired arms that begin from separate application process instances. The benchmark itself does not force-stop the process.

### Automated paired runner

The latest implementation adds a fresh-process pair runner to the existing foreground-service notification. The action is:

`Run fresh-process pair`

The runner performs the following sequence automatically:

1. Generate a new `pairId`.
2. Run the reset-enabled arm in the current application process.
3. Persist the reset arm filename, PID, and Linux process start-time ticks using synchronous durable state.
4. Schedule the continuation through Android JobScheduler, using the expedited path when supported and a regular fallback otherwise.
5. Terminate the current application process only after continuation state and the job handoff are durably committed.
6. The new application process resumes the pending continuation and runs the no-reset control arm.
7. Write a pair manifest linking both JSON files and recording both process identities.
8. Set `freshProcessVerified=true` only when both PID and process-start ticks differ.
9. Queue the manifest through the existing evidence synchronization mechanism.

The pair manifest is named `runtime-lifecycle-pair-<pairId>.json` and uses protocol `fresh_process_paired_runtime_lifecycle`.

This deliberately uses an explicit Android system `PendingIntent`/alarm boundary rather than assuming that an Activity recreation creates a new process. Android can keep an application process alive after Activity lifecycle changes, so process identity must be measured rather than inferred. The runner therefore treats PID plus process-start ticks as the freshness evidence.

At least two comparable reset/control pairs are required before treating the experiment as reproducibly informative. PSS transitions remain observational evidence and must be interpreted together with RSS, Swap PSS, available RAM, temperature, process lifetime, and the raw time series. A memory leak is not diagnosed from PSS alone.

VersionName remains 0.1.13 while this investigation is open.

## Final paired evidence — 2026-09-27

Four schema-v5 real-device evidence files now provide two fresh-process reset/no-reset pairs on the itel P661N / Android API 33. The four processes have distinct PID and Linux process-start tick values:

- `benchmarks/results/runtime-lifecycle-memory-1790466960859.json` — reset-enabled, PID 16128, start ticks 1877438.
- `benchmarks/results/runtime-lifecycle-memory-1790467459890.json` — no-reset control, PID 18822, start ticks 1929948.
- `benchmarks/results/runtime-lifecycle-memory-1790468135617.json` — reset-enabled, PID 20987, start ticks 1997518.
- `benchmarks/results/runtime-lifecycle-memory-1790468785315.json` — no-reset control, PID 24878, start ticks 2062486.

The chronological pairings are the first two files and the second two files. This pairing is based on the runner's sequential execution order and evidence timestamps; the four uploaded JSON files do not include a separately uploaded pair-manifest file.

Observed PSS transitions:

| Arm | Baseline PSS | Main transition | Final-phase transition |
| --- | ---: | ---: | ---: |
| Pair 1 reset | 11,957 KiB | post-reset: +63,478 KiB (+61.99 MiB) | — |
| Pair 1 no-reset | 13,027 KiB | post-cleanup: +31,624 KiB (+30.88 MiB) | post-no-reset: +157 KiB |
| Pair 2 reset | 13,170 KiB | post-cleanup: +24,540 KiB (+23.96 MiB) | post-reset: +158 KiB |
| Pair 2 no-reset | 12,883 KiB | post-cleanup: +24,005 KiB (+23.44 MiB) | post-no-reset: +556 KiB |

Pair 1 reset reproduces a large delayed PSS transition during post-reset: 11,957 → 75,435 KiB, about 150 seconds after `post_reset_start`. Pair 2 does not reproduce a reset-phase elevation: after reset its PSS changes only 37,710 → 37,868 KiB, while its major +24,540 KiB transition occurs during post-cleanup before reset. Both no-reset controls also show substantial post-cleanup transitions followed by only small final-phase changes.

### Final classification

**MIXED — lifecycle-dependent PSS transitions are reproducible, but reset-specific causality is not established.**

The fresh-process controls show that substantial PSS transitions can occur during the shared post-cleanup lifecycle, including in no-reset controls. Therefore the data do not establish the explicit reset boundary as the unique cause, and they do not establish a memory leak.

RSS, Swap PSS, available RAM, temperature, and the complete sample series remain supporting observations; PSS elevation alone is not a leak diagnosis.

### Investigation disposition

The reproduction objective is complete: the original observation was reproduced under fresh-process conditions and compared against two no-reset controls. No production behavior change is justified by this evidence. The lifecycle diagnostic remains useful as a regression/observability experiment, while this investigation is closed as **mixed / inconclusive for reset-specific causality**.

VersionName remains 0.1.13. No version bump is required because this investigation did not establish a new validated production milestone.
