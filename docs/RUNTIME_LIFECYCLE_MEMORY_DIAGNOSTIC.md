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
