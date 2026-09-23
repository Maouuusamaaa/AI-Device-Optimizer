from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "android/app/build.gradle.kts"
CMAKE = ROOT / "android/app/src/main/cpp/CMakeLists.txt"
NATIVE = ROOT / "android/app/src/main/cpp/local_ai_runtime.cpp"
RUNTIME = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/localai/LocalLlamaRuntime.kt"
MODEL = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/localai/QwenLocalModel.kt"


def main():
    build = BUILD.read_text(encoding="utf-8")
    cmake = CMAKE.read_text(encoding="utf-8")
    native = NATIVE.read_text(encoding="utf-8")
    runtime = RUNTIME.read_text(encoding="utf-8")
    model = MODEL.read_text(encoding="utf-8")

    assert 'versionName = "0.1.12"' in build
    assert 'versionCode = 12' in build
    assert 'ndkVersion = "27.2.12479018"' in build
    assert 'path = file("src/main/cpp/CMakeLists.txt")' in build
    assert 'version = "3.22.1"' in build

    assert "b10982.tar.gz" in cmake
    assert "GGML_NATIVE OFF" in cmake
    assert "GGML_OPENMP OFF" in cmake
    assert "LLAMA_OPENSSL OFF" in cmake

    assert "llama_model_load_from_file" in native
    assert "llama_decode" in native
    assert "llama_sampler_sample" in native
    assert '"promptTokens":' in native
    assert '"generatedTokens":' in native
    assert '"loadMs":' in native
    assert '"promptDecodeMs":' in native
    assert '"generationMs":' in native
    assert '"generationTokensPerSecond":' in native
    assert '"totalNativeMs":' in native
    assert '"modelOutputContainsThink":' in native
    assert '"nonThinkingGuard":true' in native
    assert "NON_THINKING_TAG = \"<think>\"" in native
    assert "llama_sampler_init_logit_bias" in native
    assert "jint threads_value" in native
    assert "context_params.n_threads = threads" in native
    assert "context_params.n_threads_batch = threads" in native
    assert '"advisoryOnly":true' in native
    assert '"executionRequested":false' in native
    assert '"deviceMutationAllowed":false' in native

    assert "DEFAULT_THREADS = 4" in runtime
    assert "threads: Int = DEFAULT_THREADS" in runtime
    assert "nativeGenerate(modelFile.absolutePath, prompt, contextTokens, maxTokens, threads)" in runtime

    assert 'MODEL_ID = "qwen3-0.6b-q4_0"' in model
    assert "EXPECTED_SIZE_BYTES = 428970080L" in model
    assert "EXPECTED_SHA256" in model
    assert "Qwen3-0.6B-Q4_0.gguf" in model

    assert not list(ROOT.rglob("*.gguf"))
    print("Stage 8 Android inference instrumentation contract: PASS")


if __name__ == "__main__":
    main()
