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
3. Persist the reset arm filename, PID, and Linux process start-time ticks.
4. Schedule the continuation through Android `AlarmManager`.
5. Terminate the current application process.
6. Android starts the foreground service for the continuation action in a new application process.
7. Run the no-reset control arm in that new process.
8. Write a pair manifest linking both JSON files and recording both process identities.
9. Set `freshProcessVerified=true` only when both PID and process-start ticks differ.
10. Queue the manifest through the existing evidence synchronization mechanism.

The pair manifest is named `runtime-lifecycle-pair-<pairId>.json` and uses protocol `fresh_process_paired_runtime_lifecycle`.

This deliberately uses an explicit Android system `PendingIntent`/alarm boundary rather than assuming that an Activity recreation creates a new process. Android can keep an application process alive after Activity lifecycle changes, so process identity must be measured rather than inferred. The runner therefore treats PID plus process-start ticks as the freshness evidence.

At least two comparable reset/control pairs are required before treating the experiment as reproducibly informative. PSS transitions remain observational evidence and must be interpreted together with RSS, Swap PSS, available RAM, temperature, process lifetime, and the raw time series. A memory leak is not diagnosed from PSS alone.

VersionName remains 0.1.13 while this investigation is open.
