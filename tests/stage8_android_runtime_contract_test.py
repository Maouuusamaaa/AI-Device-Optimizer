from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "android/app/build.gradle.kts"
CMAKE = ROOT / "android/app/src/main/cpp/CMakeLists.txt"
NATIVE = ROOT / "android/app/src/main/cpp/local_ai_runtime.cpp"
MODEL = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/localai/QwenLocalModel.kt"

def main():
    build = BUILD.read_text(encoding="utf-8")
    cmake = CMAKE.read_text(encoding="utf-8")
    native = NATIVE.read_text(encoding="utf-8")
    model = MODEL.read_text(encoding="utf-8")

    assert 'versionName = "0.1.1"' in build
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
    assert '"advisoryOnly":true' in native
    assert '"executionRequested":false' in native
    assert '"deviceMutationAllowed":false' in native

    assert 'MODEL_ID = "qwen3-0.6b-q4_0"' in model
    assert "EXPECTED_SIZE_BYTES = 428970080L" in model
    assert "EXPECTED_SHA256" in model
    assert "Qwen3-0.6B-Q4_0.gguf" in model

    assert not list(ROOT.rglob("*.gguf"))
    print("Stage 8 Android runtime contract: PASS")

if __name__ == "__main__":
    main()
