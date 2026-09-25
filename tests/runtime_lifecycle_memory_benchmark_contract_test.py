#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "android/app/build.gradle.kts"
BENCH = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/RuntimeLifecycleMemoryBenchmark.kt"
WRITER = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/RuntimeLifecycleMemoryBenchmarkJsonWriter.kt"
RUNTIME = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/localai/LocalLlamaRuntime.kt"
NATIVE = ROOT / "android/app/src/main/cpp/local_ai_runtime.cpp"
QUEUE = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/sync/EvidenceQueueStore.kt"
ACTIVITY = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/MainActivity.kt"

def main():
    build = BUILD.read_text()
    bench = BENCH.read_text()
    writer = WRITER.read_text()
    runtime = RUNTIME.read_text()
    native = NATIVE.read_text()
    queue = QUEUE.read_text()
    activity = ACTIVITY.read_text()

    assert 'versionName = "0.1.13"' in build
    assert "versionCode = 13" in build
    assert "BASELINE_DURATION_MS = 60_000L" in bench
    assert "POST_CLEANUP_DURATION_MS = 60_000L" in bench
    assert "POST_RESET_DURATION_MS = 300_000L" in bench
    assert 'collect("baseline"' in bench
    assert 'collect("post_cleanup"' in bench
    assert "generateForLifecycleDiagnostic" in bench
    assert "runtime.resetRuntime()" in bench
    assert "reset_before" in bench
    assert "reset_native_completed" in bench
    assert "post_reset_start" in bench
    assert "post_cleanup" in bench
    assert 'collect("post_reset"' in bench
    assert "runtime_lifecycle_memory_observation" in writer
    assert '.put("schemaVersion", 3)' in writer
    assert '.put("events", events)' in writer
    assert "runtime-lifecycle-memory-" in writer
    assert "EvidenceSyncManager.enqueue(context, file)" in writer
    assert "nativeResetRuntime" in runtime
    assert "nativeResetRuntime(): Long" in runtime
    assert "generateForLifecycleDiagnostic" in runtime
    assert "nativeResetRuntime" in native
    assert "JNIEXPORT jlong" in native
    assert "system_clock" in native
    assert "keep_backend_alive" in native
    assert "cleanup_backend" in native
    assert "llama_backend_free()" in native
    assert "runtime_lifecycle_memory_observation" in queue
    assert "runtime-lifecycle-memory" in queue
    assert "Run runtime lifecycle memory diagnostic" in activity
    assert "RuntimeLifecycleMemoryBenchmark(this)" in activity
    print("Runtime lifecycle memory diagnostic contract: PASS")

if __name__ == "__main__":
    main()
