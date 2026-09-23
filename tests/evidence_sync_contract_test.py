from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "android/app/build.gradle.kts"
MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"
SYNC_DIR = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/sync"
WRITERS = [
    ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/BenchmarkJsonWriter.kt",
    ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/LocalInferenceBenchmarkJsonWriter.kt",
    ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/benchmark/WorkloadRecoveryBenchmarkJsonWriter.kt",
]
ACTIVITY = ROOT / "android/app/src/main/java/com/maouuusama/ai/device/optimizer/MainActivity.kt"


def main():
    build = BUILD.read_text(encoding="utf-8")
    manifest = MANIFEST.read_text(encoding="utf-8")
    activity = ACTIVITY.read_text(encoding="utf-8")

    assert 'versionName = "0.1.13"' in build
    assert "versionCode = 13" in build
    assert 'androidx.work:work-runtime-ktx:2.11.2' in build

    assert '<uses-permission android:name="android.permission.INTERNET" />' in manifest
    assert '<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />' in manifest
    assert manifest.count("android.permission.ACCESS_NETWORK_STATE") == 1

    expected = [
        "GitHubSyncConfig.kt",
        "GitHubTokenStore.kt",
        "EvidenceQueueStore.kt",
        "GitHubEvidenceClient.kt",
        "EvidenceSyncWorker.kt",
        "EvidenceSyncScheduler.kt",
        "EvidenceSyncManager.kt",
    ]
    for name in expected:
        assert (SYNC_DIR / name).exists(), name

    config = (SYNC_DIR / "GitHubSyncConfig.kt").read_text(encoding="utf-8")
    token_store = (SYNC_DIR / "GitHubTokenStore.kt").read_text(encoding="utf-8")
    queue = (SYNC_DIR / "EvidenceQueueStore.kt").read_text(encoding="utf-8")
    client = (SYNC_DIR / "GitHubEvidenceClient.kt").read_text(encoding="utf-8")
    worker = (SYNC_DIR / "EvidenceSyncWorker.kt").read_text(encoding="utf-8")
    scheduler = (SYNC_DIR / "EvidenceSyncScheduler.kt").read_text(encoding="utf-8")

    assert 'DEFAULT_REPOSITORY = "Maouuusamaaa/AI-Device-Optimizer"' in config
    assert 'DEFAULT_BRANCH = "main"' in config
    assert "AndroidKeyStore" in token_store
    assert "AES/GCM/NoPadding" in token_store
    assert "https://api.github.com" in client
    assert "X-GitHub-Api-Version" in client
    assert "PUT" in client
    assert "Base64" in client
    assert 'MessageDigest.getInstance("SHA-256")' in queue
    assert "benchmarks/results/" in queue
    assert "APPROVED_FILENAME" in queue
    assert "APPROVED_WORKLOADS" in queue
    assert "long-recovery-memory" in queue
    assert "long_recovery_memory_observation" in queue
    assert "runtime_lifecycle_memory_observation" in queue
    assert "runtime-lifecycle-memory" in queue
    assert "NetworkType.CONNECTED" in scheduler
    assert "BackoffPolicy.EXPONENTIAL" in scheduler
    assert "ExistingWorkPolicy.KEEP" in scheduler
    assert "Result.retry()" in worker
    assert "statusCode == 401 || error.statusCode == 403" in worker

    for writer in WRITERS:
        content = writer.read_text(encoding="utf-8")
        assert "EvidenceSyncManager" in content
        assert "EvidenceSyncManager.enqueue(context, file)" in content

    assert "GitHub evidence sync" in activity
    assert "Run runtime lifecycle memory diagnostic" in activity
    assert "Test GitHub connection + enable sync" in activity
    assert "Retry pending evidence uploads" in activity
    assert "GitHubTokenStore(this).save" in activity
    assert "EvidenceSyncScheduler.enqueue(this)" in activity

    # A token must never be committed to the app source.
    for path in [*SYNC_DIR.glob("*.kt"), BUILD, MANIFEST, ACTIVITY]:
        content = path.read_text(encoding="utf-8")
        assert "github_pat_" not in content
        assert "ghp_" not in content

    print("Evidence sync contract: PASS")


if __name__ == "__main__":
    main()
