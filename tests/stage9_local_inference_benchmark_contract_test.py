from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "android/app/build.gradle.kts"
BENCHMARK = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/LocalInferenceBenchmark.kt"
WRITER = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/LocalInferenceBenchmarkJsonWriter.kt"
ACTIVITY = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/MainActivity.kt"


def main():
    build = BUILD.read_text(encoding="utf-8")
    benchmark = BENCHMARK.read_text(encoding="utf-8")
    writer = WRITER.read_text(encoding="utf-8")
    activity = ACTIVITY.read_text(encoding="utf-8")

    assert 'versionName = "0.1.5"' in build
    assert 'versionCode = 5' in build

    assert "DEFAULT_THREAD_CONFIGURATIONS = listOf(2, 4)" in benchmark
    assert "DEFAULT_REPETITIONS = 2" in benchmark
    assert "DEFAULT_CONTEXT_TOKENS = 4096" in benchmark
    assert "DEFAULT_MAX_TOKENS = 64" in benchmark
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
    assert "At least two thread configurations are required" in benchmark

    assert "processCpuTime" in writer
    assert "processPss" in writer
    assert "observationId" in writer
    assert '"advisoryOnly", true' in writer
    assert '"executionRequested", false' in writer
    assert '"deviceMutationAllowed", false' in writer

    assert "Run Stage 9 inference benchmark (2 vs 4 threads)" in activity
    assert "LocalInferenceBenchmark(this)" in activity
    assert "LocalInferenceBenchmarkJsonWriter.write(this, result)" in activity

    print("Stage 9 local inference benchmark contract: PASS")


if __name__ == "__main__":
    main()
