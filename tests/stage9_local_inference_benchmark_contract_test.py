from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "android/app/build.gradle.kts"
BENCHMARK = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/LocalInferenceBenchmark.kt"
WRITER = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/LocalInferenceBenchmarkJsonWriter.kt"
ACTIVITY = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/MainActivity.kt"
MODEL = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/localai/QwenLocalModel.kt"


def main():
    build = BUILD.read_text(encoding="utf-8")
    benchmark = BENCHMARK.read_text(encoding="utf-8")
    writer = WRITER.read_text(encoding="utf-8")
    activity = ACTIVITY.read_text(encoding="utf-8")
    model = MODEL.read_text(encoding="utf-8")

    assert 'versionName = "0.1.13"' in build
    assert 'versionCode = 13' in build
    assert 'isProfileable = true' in build
    assert 'signingConfig = signingConfigs.getByName("ciStable")' in build

    assert "DEFAULT_THREAD_CONFIGURATIONS = listOf(2, 4)" in benchmark
    assert "DEFAULT_REPETITIONS = 2" in benchmark
    assert "DEFAULT_CONTEXT_TOKENS = 4096" in benchmark
    assert "DEFAULT_MAX_TOKENS = 64" in benchmark
    assert "DEFAULT_DELAY_BETWEEN_RUNS_MS = 15_000L" in benchmark
    assert "MAX_START_TEMPERATURE_C = 39.0" in benchmark
    assert "buildBalancedSchedule" in benchmark
    assert "threadConfigurations[0],\n            threadConfigurations[1],\n            threadConfigurations[1],\n            threadConfigurations[0]" in benchmark
    assert "awaitControlledStart" in benchmark
    assert "QwenModelDownloader(context).isInstalled()" in benchmark
    assert "Process.getElapsedCpuTime()" in benchmark
    assert "Debug.getPss()" in benchmark
    assert "DeviceMonitor(context).collectSnapshot" in benchmark
    assert "loadMs" in benchmark
    assert "promptDecodeMs" in benchmark
    assert "generationMs" in benchmark
    assert "generationTokensPerSecond" in benchmark
    assert "processPssDeltaKb" in benchmark
    assert "advisoryOnly" in benchmark
    assert "executionRequested" in benchmark
    assert "deviceMutationAllowed" in benchmark

    prompt_function = model[model.index("fun prompt"):]
    assert "<think>\\n\\n</think>\\n\\n" in prompt_function
    assert "/no_think" not in prompt_function
    assert "Stage 9 hardened benchmark requires exactly two thread configurations" in benchmark

    assert "processCpuTime" in writer
    assert "processPss" in writer
    assert "observationId" in writer
    assert '"advisoryOnly", true' in writer
    assert '"executionRequested", false' in writer
    assert '"deviceMutationAllowed", false' in writer
    assert "MediaStore.Downloads" in writer
    assert "SHARED_EXPORT_RELATIVE_PATH" in writer
    assert "exportToSharedDownloads" in writer

    assert "Run Stage 9 inference benchmark (2 vs 4 threads)" in activity
    assert "LocalInferenceBenchmark(this)" in activity
    assert "LocalInferenceBenchmarkJsonWriter.write(this, result)" in activity
    assert "LocalInferenceBenchmarkJsonWriter.exportToSharedDownloads(this, file)" in activity
    assert "/storage/emulated/0/" in activity
    assert "ScrollView(this)" in activity

    print("Stage 9 local inference benchmark contract: PASS")


if __name__ == "__main__":
    main()
