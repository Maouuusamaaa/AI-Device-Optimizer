# Stage 7 — Qwen3 0.6B Local Model Adapter

Stage 7 introduces the first concrete learned-model candidate for on-device inference.

## Candidate
- Qwen3 0.6B
- GGUF source: ggml-org/Qwen3-0.6B-GGUF
- Initial quantization: Q4_0
- Published GGUF size: about 429 MB
- Runtime: llama.cpp
- Android target: arm64-v8a
- Initial context: 4096 tokens

The model is a candidate, not yet an admitted production model.

## Safety
The model is advisory-only. Its raw output is untrusted. The adapter always
wraps the result with OBSERVE_ONLY, abstain=true, executionRequested=false,
deviceMutationAllowed=false, and localSafetyGateRequired=true.

## Measurement gate
Before Android integration is accepted, measure on the physical device:
1. idle baseline RAM/PSS/RSS;
2. llama.cpp startup time;
3. model-load memory;
4. inference wall time;
5. peak memory/RSS delta;
6. CPU utilization;
7. battery percentage and temperature before/after repeated inference;
8. repeated-run stability.

No performance claim should be inferred from model file size alone.

## Repository policy
Do not commit the GGUF binary. Keep the model external to GitHub and record
exact model identity, quantization, runtime version, device telemetry, and
benchmark results as reproducible evidence.
