# i-Boost / Smart Panel Candidate Catalog

This directory is intentionally separate from the AI Device Optimizer build/runtime implementation.

## Purpose

Track public Smart Panel candidate releases for later compatibility and integration research on the itel P661N. This catalog records metadata and provenance only. OEM APK binaries are not committed here.

## Device baseline

- Package: `com.transsion.smartpanel`
- Installed version: `3.0.0.216`
- Installed versionCode: `5233`
- Android: 13 / API 33
- ABI: arm64-v8a
- Current APK path: `/system_ext/app/SmartPanel/SmartPanel.apk`
- Baseline SHA-256: `986709e8e131cf7a1a619e5e82e865e87affd330db02020662cce483579002ec`
- Baseline size: 56,452,303 bytes
- Baseline is OEM/system_ext and must not be replaced until signature, package identity, dependencies, and device compatibility are verified.

## Candidate policy

For each source, retain up to the three newest releases exposed by that source. The same release appearing on multiple sources is one candidate with multiple provenance records.

A candidate is not considered installable merely because an archive marks it safe. Installation requires local APK signature/certificate, manifest, package identity, versionCode, ABI, permissions, components, dependencies, and device-specific compatibility checks.

## Candidate set collected 2026-09-28

| Candidate | VersionCode | Android | ABI | Size | Date | Sources |
|---|---:|---|---|---:|---|---|
| 15.2.1.052 | 150201052 | 12+ | arm64-v8a | ~86 MB | Jul/Aug 2025 | APKPure, APKCombo, APKMirror |
| 15.2.0.117 | 150200117 | 12+ | arm64-v8a | 85.98 MB | Aug 8 2025 | APKMirror, APKCombo |
| 3.8.2.077 | 30802077 | 12+ | arm64-v8a + armeabi-v7a | 95.14 MB | May 28 2025 | APKMirror, APKCombo |

### Source observations

#### APKMirror

Current Smart Panel archive exposes:

1. 15.2.0.117 — versionCode 150200117, arm64-v8a, Android 12+, 85.98 MB.
2. 15.2.1.052 — versionCode 150201052, arm64-v8a, Android 12+, 86.03 MB.
3. 3.8.2.077 — versionCode 30802077, arm64-v8a + armeabi-v7a, Android 12+, 95.14 MB.

APKMirror also reports multiple valid signatures for 15.2.0.117 (known signatures aec8 and bfd5). This is an important compatibility checkpoint; do not infer compatibility from version alone.

#### APKPure

The currently indexed Smart Panel page exposes 15.2.1.052 as its latest version:

- versionCode 150201052
- Android 12+
- arm64-v8a
- 86.0 MB
- certificate fingerprint shown by the page: aec83f63bfa3a6ad9422086688639fea7684ef00

Its version-history page currently exposes only 15.2.1.052 in the indexed results, so fewer than three distinct candidates can be attributed to APKPure from the evidence collected here.

#### APKCombo

The current Smart Panel page exposes:

1. 15.2.1.052 — versionCode 150201052, Android 12+, arm64-v8a.
2. 15.2.0.117 — Android 12+.
3. 3.8.2.077 — Android 12+.

APKCombo identifies the package as `com.transsion.smartpanel`.

## Important provenance distinction

The public archives contain releases attributed to different Transsion-related publishers/labels across generations (for example Transsion Holdings and Shalltry Group). This must be treated as evidence to investigate, not as proof that the APK can update the OEM Smart Panel on the P661N.

## Next verification stage

Before any installation attempt:

1. Obtain the exact APK bytes for each selected candidate.
2. Verify APK v2/v3 signing and full certificate fingerprints.
3. Compare signing identity with the installed OEM Smart Panel.
4. Inspect manifest, exported components, permissions, shared UID/signature permissions, providers, services, receivers, and native libraries.
5. Compare resources/components related to Game Mode.
6. Check versionCode/update compatibility.
7. Only then classify candidates as compatible, incompatible, or requiring deeper analysis.

No candidate is approved for installation by this catalog.
