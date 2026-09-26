#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "android/app/build.gradle.kts"
BENCH = ROOT / "android/app/src/main/java/com/maouuusamaaa/ai/device/optimizer/benchmark/RuntimeLifecycleMemoryBenchmark.kt"
WRITER = ROOT / "android/app/src/main/java/com/maouuusamaaa/ai/device/optimizer/benchmark/RuntimeLifecycleMemoryBenchmarkJsonWriter.kt"
COORDINATOR = ROOT / "android/app/src/main/java/com/maouuusamaaa/ai/device/optimizer/benchmark/FreshProcessPairedLifecycleCoordinator.kt"
JOB_SERVICE = ROOT / "android/app/src/main/java/com/maouuusamaaa/ai/device/optimizer/benchmark/FreshProcessPairedLifecycleJobService.kt"
SERVICE = ROOT / "android/app/src/main/java/com/maouuusamaaa/ai/device/optimizer/agent/OptimizerBackgroundService.kt"
MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"
RUNTIME = ROOT / "android/app/src/main/java/com/maouuusamaaa/ai/device/optimizer/localai/LocalLlamaRuntime.kt"
NATIVE = ROOT / "android/app/src/main/cpp/local_ai_runtime.cpp"
QUEUE = ROOT / "android/app/src/main/java/com/maouuusamaaa/ai/device/optimizer/sync/EvidenceQueueStore.kt"
ACTIVITY = ROOT / "android/app/src/main/java/com/maouuusamaaa/ai/device/optimizer/MainActivity.kt"

def main():
    build = BUILD.read_text()
    bench = BENCH.read_text()
    writer = WRITER.read_text()
    coordinator = COORDINATOR.read_text()
    job_service = JOB_SERVICE.read_text()
    service = SERVICE.read_text()
    manifest = MANIFEST.read_text()
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
    assert "resetEnabled: Boolean" in bench
    assert "RuntimeLifecycleProcessMetadata" in bench
    assert "Process.myPid()" in bench
    assert "processStartTimeTicks" in bench
    assert "System.nanoTime()" in bench
    assert "no_reset_control" in bench
    assert "post_no_reset" in bench
    assert "reset_before" in bench
    assert "reset_native_completed" in bench
    assert "post_reset_start" in bench
    assert "monotonicElapsedMs" in bench
    assert 'collect("post_reset"' in bench

    assert "runtime_lifecycle_memory_observation" in writer
    assert '.put("schemaVersion", 5)' in writer
    assert '"resetEnabled"' in writer
    assert '"no_reset_control"' in writer
    assert '.put("events", events)' in writer
    assert '.put("process", JSONObject())' in writer
    assert '.put("pssTransitions", transitions)' in writer
    assert "monotonicElapsedMs" in writer
    assert "runtime-lifecycle-memory-" in writer
    assert "EvidenceSyncManager.enqueue(context, file)" in writer

    assert "FreshProcessPairedLifecycleCoordinator" in coordinator
    assert "ACTION_START" in coordinator
    assert "ACTION_CONTINUE" in coordinator
    assert "RuntimeLifecycleMemoryBenchmark(appContext)" in coordinator
    assert ".run(resetEnabled = true)" in coordinator
    assert ".run(resetEnabled = false)" in coordinator
    assert "JobScheduler" in coordinator
    assert "JobInfo.Builder" in coordinator
    assert "RESULT_SUCCESS" in coordinator
    assert "Process.killProcess" in coordinator
    assert "processSeparatedByPidAndStartTime" in coordinator
    assert "runtime-lifecycle-pair-" in coordinator
    assert "onFinished" in coordinator
    assert "Continuation scheduling failed; process will not be killed" in coordinator

    assert "JobService" in job_service
    assert "onStartJob" in job_service
    assert "jobFinished(params, false)" in job_service
    assert "continueAfterFreshProcess" in job_service

    assert "FreshProcessPairedLifecycleCoordinator.ACTION_START" in service
    assert "FreshProcessPairedLifecycleCoordinator.ACTION_CONTINUE" in service
    assert '"Run fresh-process pair"' in service
    assert ".addAction(" in service

    assert "FreshProcessPairedLifecycleJobService" in manifest
    assert 'android.permission.BIND_JOB_SERVICE' in manifest

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
    assert "Run lifecycle diagnostic (with reset)" in activity
    assert "Run lifecycle control (without reset)" in activity
    assert "resetEnabled = false" in activity
    assert "RuntimeLifecycleMemoryBenchmark(this)" in activity
    print("Runtime lifecycle memory diagnostic contract: PASS")

if __name__ == "__main__":
    main()
