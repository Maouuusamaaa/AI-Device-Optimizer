# Automatic GitHub Evidence Sync

## Purpose

Benchmark results are treated as evidence artifacts. After a supported benchmark writer finishes a JSON file, the app validates the file, calculates a SHA-256 digest, places the artifact in a persistent local queue, and schedules a WorkManager upload.

The sync is offline-first:

1. Benchmark writes the JSON artifact.
2. The evidence queue checks the approved app-private benchmark directory, filename allowlist, JSON schema presence, non-empty samples, and approved workload/protocol.
3. The queue records the immutable local path, repository path, file size, and SHA-256 digest.
4. WorkManager schedules a unique one-time sync with NetworkType.CONNECTED.
5. If there is no network, WorkManager keeps the work pending and retries when the constraint becomes satisfied.
6. GitHub uploads are serialized by one worker. A remote file is never overwritten when its content differs from the queued digest.
7. Successful uploads are marked SYNCED; transient network/server errors use exponential retry; authentication, validation, and content conflicts become explicit terminal queue states.

## Remote layout

Approved evidence is uploaded only to:

benchmarks/results/<generated-evidence-file>.json

The app does not accept arbitrary local paths for upload.

Supported generated evidence currently includes:

- baseline-*.json with workload monitor_foreground_idle
- workload-*.json with workload foreground_user_workload
- repeated-workload-recovery-*.json with protocol repeated_workload_recovery_memory_observation
- local-inference-*.json with workload local_qwen3_0_6b_inference

## Authentication

The first implementation uses a user-supplied fine-grained GitHub personal access token. The token is not bundled in the APK or source tree. It is encrypted using an AES-GCM key stored in Android Keystore.

For this repository, the token should be restricted to the Maouuusamaaa/AI-Device-Optimizer repository and granted only the repository Contents: write permission required by the GitHub Contents API.

The app verifies repository access before enabling automatic sync. A token is never displayed back in the UI.

A future GitHub App/OAuth flow can replace the token-entry step without changing the evidence queue or upload protocol.

## Queue states

- PENDING: validated locally and waiting for upload.
- RETRY: a transient network/server failure occurred.
- SYNCED: the exact queued bytes are known to exist remotely.
- INVALID: the local artifact failed validation or changed after queueing.
- AUTH_FAILED: GitHub rejected authentication or permissions.
- CONFLICT: the remote path already exists with different bytes or GitHub rejected the content.

## Integrity model

The queue stores SHA-256 over the exact local JSON bytes. Before upload, the worker recalculates the digest and refuses to upload if the file changed after queueing.

If the remote path already exists, the worker downloads the remote contents through the GitHub Contents API and compares SHA-256 values. Identical content is treated as already synchronized; different content is not overwritten.

## Security boundaries

The evidence sync feature is separate from the optimizer Safety Gate. It performs network I/O and repository writes only; it does not authorize device mutations.

The app uses the GitHub Contents API only for the approved evidence path. The API request is authenticated with the stored token, and upload operations are serialized by the unique WorkManager worker.

## Initial device setup

1. Create a fine-grained GitHub token restricted to Maouuusamaaa/AI-Device-Optimizer.
2. Grant repository Contents: write.
3. Enter the token once in the app's GitHub evidence sync section.
4. Keep the repository as Maouuusamaaa/AI-Device-Optimizer and branch as main.
5. Tap Test GitHub connection + enable sync.
6. From then on, benchmark JSON files are queued automatically. If the phone is offline, they remain pending until connectivity returns.

Do not commit the token to Git, paste it into source code, or include it in bug reports.

## Validation

The repository includes tests/evidence_sync_contract_test.py, which checks the Android version contract, WorkManager dependency, manifest network permissions, queue allowlists, encryption boundary, upload path, retry behavior, and automatic writer integration.
