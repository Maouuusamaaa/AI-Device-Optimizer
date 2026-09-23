# Runtime Lifecycle Memory Diagnostic

This experiment isolates local AI runtime memory retention from the earlier foreground workload experiments.

Protocol:
1. 60 seconds baseline.
2. One Qwen3 0.6B Q4_0 advisory inference using 4 threads and 64 maximum output tokens.
3. 60 seconds post-inference observation.
4. Invoke the explicit native runtime reset boundary.
5. 5 minutes post-reset observation.

The inference path already releases the llama.cpp model, context, sampler, and backend after generation. The explicit reset is therefore a diagnostic boundary, not an optimization action. It must not be interpreted as proof that all native allocations are returned to the operating system.

Interpretation:
- A substantial PSS reduction immediately after reset is evidence that retained runtime state is releasable at that boundary.
- PSS remaining elevated after reset and throughout recovery is evidence of persistent retention outside a currently live model/context handle, but does not diagnose a leak.
- A continued PSS increase after reset is a signal to investigate other process/native allocations and lifecycle behavior.
- Results must be interpreted together with RSS, Swap PSS, available RAM, temperature, and the complete time series.

Safety:
- Read-only observation.
- No device optimization or system mutation.
- No process killing or package force-stop.
- The benchmark does not change Android system settings.

Evidence is automatically queued and synchronized through the existing GitHub Evidence Sync mechanism.
