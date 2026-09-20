# Stage 6 — Local AI Inference Foundation

Stage 6 starts the on-device AI layer while Kaggle/AI Cloud work remains deferred until the PC workflow is available.

## Scope

This stage establishes a small, dependency-free advisory inference boundary for Android. The current backend is deliberately deterministic and is a reference implementation, not a claimed learned model.

The interface accepts normalized telemetry:
- batteryPercent
- temperatureC
- availableRamMb

It returns an advisory recommendation with confidence, abstention, and a reason.

## Safety boundary

The local AI cannot execute actions. Every output is:
- advisory-only;
- executionRequested=false;
- deviceMutationAllowed=false;
- subject to the existing local Safety Gate.

The AI therefore cannot replace diagnosis, policy simulation, measurement, or the Safety Gate.

## Device resource strategy

The Android target is an 8 GB-class device environment. We will not begin with a large language model. The first implementation is dependency-free so its overhead can be measured before introducing a compact model runtime.

The benchmark records inference wall time and process maximum RSS delta. Real-device memory and battery measurements remain the authority for model-size decisions.

## Migration path

The stable interface allows a future compact quantized model to replace the reference backend without changing the safety contract:

reference rules → compact learned model → validated local model

A model is not admitted merely because it produces predictions. It must pass held-out evaluation, recommendation validity checks, abstention checks, and real-device overhead measurements.

## Current status

Implemented:
- local AI contract;
- deterministic advisory reference backend;
- local inference overhead benchmark;
- unit tests.

Not yet implemented:
- learned local model;
- model packaging for Android;
- automatic model download;
- cloud synchronization of private memory;
- execution authority.

Those remain separate milestones.
