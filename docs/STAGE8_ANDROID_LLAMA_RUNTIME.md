# Stage 8 — Android llama.cpp Local Runtime

## Scope

Stage 8 connects the Android application to a native llama.cpp runtime for the first physical-device local-model experiment.

The runtime is pinned to llama.cpp b10982 and built for Android arm64-v8a through Gradle ExternalNativeBuild/CMake. The repository does not contain the Qwen3 GGUF binary.

## Model

Model:
- Qwen3 0.6B
- GGUF
- Q4_0
- file: Qwen3-0.6B-Q4_0.gguf
- expected size: 428970080 bytes
- expected SHA-256: da2572f16c06133561ce56accaa822216f2391ef4d37fba427801cd6736417d4

The verified upstream artifact is currently reported as 429 MB by Hugging Face. The exact byte count used by the Android downloader is 428970080 bytes. The model is downloaded to the app-private model directory only after the user starts the download. The downloader supports resumable transfers and verifies the full SHA-256 before replacing the final model file.

## Runtime safety

The native runtime is advisory-only:
- executionRequested=false
- deviceMutationAllowed=false
- local Safety Gate remains authoritative
- the model output is never passed to the Action Engine
- no system mutation is performed by the local-model path

The model is therefore not an optimizer actuator. It is an inference component used to produce evidence/analysis for later local policy processing.

## Inference instrumentation

The physical inference path now reports phase-level timing and generation metrics in its JSON result:
- promptTokens
- generatedTokens
- threads
- loadMs
- tokenizationMs
- contextInitMs
- promptDecodeMs
- generationMs
- generationTokensPerSecond
- totalNativeMs
- modelOutputContainsThink

The runtime exposes a controlled CPU thread count from 1 through 8. The current diagnostic default is 4 threads. This is measurement infrastructure, not an assumption that 4 threads is optimal.

The Android caller should additionally measure:
- wall-clock inference duration
- process elapsed CPU time
- sampled process PSS before and after inference
- battery percentage before and after
- battery temperature before and after
- the exact observation timestamp and model/runtime identity

Android's Process API provides elapsed CPU time for the process, while PSS is a sampled memory metric; these measurements should therefore be reported with their measurement semantics rather than treated as exact peak memory. urlAndroid Process APIhttps://developer.android.com/reference/android/os/Process

## Interpretation rules

The first physical result showed a 174733 ms wall time. That is evidence that inference completed, but it is not enough to diagnose the bottleneck.

The next measurement must separate:
1. model loading time;
2. tokenization;
3. context initialization;
4. prompt evaluation;
5. token generation;
6. cleanup/overall native overhead.

A large loadMs relative to generationMs indicates model-load overhead rather than token-generation throughput. A low generationTokensPerSecond indicates a decode-side bottleneck. Thread-count comparisons must be performed on the same device under comparable thermal and battery conditions.

llama.cpp documents separate prompt-processing and generation throughput metrics and recommends measuring different thread counts rather than assuming that more threads are faster. urlllama.cpp performance tipshttps://github.com/ggml-org/llama.cpp/blob/master/docs/development/token_generation_performance_tips.md

## Build

The Android module uses:
- NDK 27.2.12479018
- CMake 3.22.1
- llama.cpp b10982
- GGML_NATIVE=OFF
- GGML_OPENMP=OFF
- GGML_LLAMAFILE=OFF
- LLAMA_OPENSSL=OFF

The native library is packaged only for the ABI selected by the Android build. The current physical target is arm64-v8a.

## Admission gate

Stage 8 does not claim production admission.

Required physical evidence:
1. APK installs successfully on the ITEL P661N.
2. Native library loads.
3. Qwen3 model download completes and SHA-256 matches.
4. A real local inference completes.
5. Repeated inference measurements cover phase timings, tokens/sec, available RAM/PSS/RSS where available, CPU, battery, temperature, and stability.
6. At least two controlled thread configurations are compared under comparable conditions.
7. The model-output contract remains advisory-only.
8. Safety contract remains intact during and after inference.

Only after those measurements can the model be evaluated for admission to the optimizer local reasoning path.
