# i-Boost / Smart Panel Candidate Catalog

This directory is intentionally separate from the AI Device Optimizer build/runtime implementation.

## Device baseline

- Package: `com.transsion.smartpanel`
- Installed version: `3.0.0.216`
- Installed versionCode: `5233`
- Android: 13 / API 33
- ABI: arm64-v8a
- Current APK path: `/system_ext/app/SmartPanel/SmartPanel.apk`
- Baseline SHA-256: `986709e8e131cf7a1a619e5e82e865e87affd330db02020662cce483579002ec`
- Baseline size: 56,452,303 bytes

The OEM/system_ext APK must not be replaced until package identity, signing identity, dependencies, and device compatibility are verified.

## Queue policy

Each source has its own folder. The source folder contains URLs/metadata only; APK binaries are temporary research inputs and are not committed.

Up to three newest distinct releases exposed by each source are queued. The same release across multiple sources is one logical candidate in `cross-source/`.

A newer release replaces an older release only after the newer release passes the required verification gates. A failed newer release does not invalidate an older release that has already passed.

## Current logical candidates

| Candidate | VersionCode | Android | ABI | Sources | Queue status |
|---|---:|---|---|---|---|
| 15.2.1.052 | 150201052 | 12+ | arm64-v8a | APKMirror, APKPure, APKCombo | acquisition/verification pending |
| 15.2.0.117 | 150200117 | 12+ | arm64-v8a | APKMirror, APKCombo | acquisition/verification pending |
| 3.8.2.077 | 30802077 | 12+ | arm64-v8a + armeabi-v7a | APKMirror, APKCombo | acquisition/verification pending |

## Source queue

### APKMirror

The current archive evidence exposes these three newest relevant releases:

1. 15.2.0.117 — uploaded 2025-08-08.
2. 15.2.1.052 — uploaded 2025-07-29.
3. 3.8.2.077 — uploaded 2025-05-28.

APKMirror reports multiple valid signature variants for some releases, so the exact downloaded binary must be verified rather than inferred from the release page.

### APKPure

The currently indexed evidence exposes 15.2.1.052 as the latest distinct Smart Panel release. Fewer than three candidates are therefore queued from this source until additional distinct releases can be verified from its indexed history.

### APKCombo

The current evidence exposes:

1. 15.2.1.052
2. 15.2.0.117
3. 3.8.2.077

## Verification gates

For each candidate:

1. Acquire the exact APK bytes from its recorded source URL.
2. Compute the binary SHA-256.
3. Verify APK signing scheme and certificate fingerprints.
4. Compare signing identity with the installed OEM Smart Panel.
5. Verify package name and versionCode.
6. Verify ABI/native libraries.
7. Inspect manifest, exported components, permissions, providers, services, and receivers.
8. Inspect Game Mode/i-Boost-related components and resources.
9. Assess device-specific compatibility on the P661N.
10. Only then classify as valid, rejected, or requiring deeper analysis.

No candidate is approved for installation by this catalog.
