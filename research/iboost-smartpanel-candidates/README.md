# i-Boost / Smart Panel Candidate Queue

This research area is isolated from the AI Device Optimizer build/runtime tree.

## Workflow

1. Store source URLs and candidate metadata in Git.
2. Process one source folder at a time.
3. Download the APK temporarily for binary inspection.
4. Verify package identity, versionCode, ABI, signing certificate, manifest, permissions, components, native libraries, and relevant Game Mode integration points.
5. Classify the candidate.
6. Retain only valid candidates needed for the next stage; remove superseded or failed queue entries when the evidence supports removal.
7. Never bypass Android signature or package-update checks.

## Device baseline

- Package: `com.transsion.smartpanel`
- Installed version: `3.0.0.216`
- Installed versionCode: `5233`
- Android: 13 / API 33
- ABI: arm64-v8a
- OEM APK: `/system_ext/app/SmartPanel/SmartPanel.apk`

## Status values

- `PENDING`
- `DOWNLOAD_BLOCKED`
- `VERIFYING`
- `VALID`
- `REJECTED`
- `SUPERSEDED`

APK binaries are temporary research inputs and are not committed to this repository.
