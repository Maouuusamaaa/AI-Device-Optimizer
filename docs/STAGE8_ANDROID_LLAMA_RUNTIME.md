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
- expected size: 449839104 bytes
- expected SHA-256: da2572f16c06133561ce56accaa822216f2391ef4d37fba427801cd6736417d4

The model is downloaded to the app-private model directory only after the user starts the download. The downloader supports resumable transfers and verifies the full SHA-256 before replacing the final model file.

## Runtime safety

The native runtime is advisory-only:
- executionRequested=false
- deviceMutationAllowed=false
- local Safety Gate remains authoritative
- the model output is never passed to the Action Engine
- no system mutation is performed by the local-model path

The model is therefore not an optimizer actuator. It is an inference component used to produce evidence/analysis for later local policy processing.

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
5. Repeated inference measurements cover wall time, available RAM/PSS/RSS where available, CPU, battery, temperature, and stability.
6. Safety contract remains intact during and after inference.

Only after those measurements can the model be evaluated for admission to the optimizer local reasoning path.
