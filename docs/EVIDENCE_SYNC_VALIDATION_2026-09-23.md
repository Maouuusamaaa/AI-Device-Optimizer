# GitHub Evidence Sync — physical-device validation (2026-09-23)

## Scope

This record documents the first physical-device validation of the GitHub Evidence Sync pipeline in AI Device Optimizer 0.1.11.

Device: ITEL itel P661N, Android API 33.

## Configuration

- Repository: Maouuusamaaa/AI-Device-Optimizer
- Branch: main
- GitHub sync: enabled
- Token: configured in the Android app; token value is not recorded here
- Upload policy: benchmark JSON is validated, SHA-256 hashed locally, queued locally, and uploaded through WorkManager when network connectivity is available.

## Validation sequence

1. A 60-second read-only baseline benchmark completed on the physical device.
2. With network connectivity available, the resulting evidence was uploaded successfully. The app reported `Synced: 1`, with `Pending: 0` and `Failed: 0`.
3. A second 60-second read-only baseline was executed while the device was offline.
4. The app placed the new evidence in the local pending queue.
5. Wi-Fi was enabled again.
6. The pending evidence was automatically uploaded without pressing the manual retry control, and the user observed the queue clear after connectivity returned.

## Repository-side verification

The first uploaded benchmark is present on `main` at:

`benchmarks/results/baseline-monitor_foreground_idle-1790164000328.json`

Repository commit:

`ba33cd1bebef5b0232d72cde3740f25b6375cf60`

Commit message:

`benchmarks: add baseline-monitor_foreground_idle-1790164000328.json`

The repository file is a valid schemaVersion 2 benchmark containing 30 samples for workload `monitor_foreground_idle` on ITEL itel P661N / Android API 33.

## Integrity design

The Android sync implementation validates the approved benchmark directory, filename allowlist, JSON schema/sample presence, and approved protocol/workload before queueing. The queued SHA-256 is checked again immediately before upload. The GitHub client uploads the exact local file bytes and, if the destination already exists, compares the remote bytes' SHA-256 with the queued SHA-256 before treating the evidence as identical.

## Result

Status: CONFIRMED for the tested offline-first flow.

Confirmed behaviors:
- benchmark evidence is persisted locally;
- evidence enters the pending queue while offline;
- network restoration triggers automatic upload through WorkManager;
- the successful upload is reflected as synced in the Android UI;
- uploaded evidence is present in the GitHub repository.

This record does not claim that the entire sync implementation is exhaustively tested under every GitHub/API failure mode.
