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
| 15.2.1.052 | 150201052 | 12+ | arm64-v8a | APKMirror, APKPure, APKCombo | **REJECTED — certificate mismatch** |
| 15.2.0.117 | 150200117 | 12+ | arm64-v8a | APKMirror, APKCombo | **REJECTED — published certificate mismatch** |
| 3.8.2.077 | 30802077 | 12+ | arm64-v8a + armeabi-v7a | APKMirror, APKCombo | **REJECTED — published certificate mismatch** |

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

## Device signature gate result

The installed P661N SmartPanel baseline was measured directly with apksigner:

- certificate SHA-256: 7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb
- certificate SHA-1: be8cb9f95bcb5bfb04045034e5182634a2fca1fa
- subject: EMAILADDRESS=yong.chen@itel-mobile.com, CN=RoyChen, OU=Itel, O=TRANSSION, L=Shenzhen, ST=Guangdong, C=CN

The inspected 15.2.1.052 binary uses SHA-256 40e4400c5c90f79d8f390584eebad893ac9bdba0ff1507b126d4c9db547929da, so it fails the normal Android update-signature gate.

Public APKMirror metadata also reports the same 40e440... certificate for 15.2.0.117, while 3.8.2.077 reports a different a2f153... certificate. Neither matches the P661N baseline. These source-level certificate mismatches are sufficient to reject those candidates for a normal in-place package update, even though their binaries were not locally acquired in the CI audit. citeturn2search2turn2search5

## Firmware-lineage research

Public firmware indexes expose P661N-specific builds including P661N-H334IJKLN-T-GL-241113V437, 250106V612, 250514V641, and 250723V660. These are provenance leads for locating a P661N-signed SmartPanel binary, but the indexed pages do not expose the SmartPanel APK or its certificate fingerprint. Therefore no firmware package is treated as verified or installable from this catalog. citeturn0search1turn0search3turn0search5

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

## P661N OTA lineage lead — 250723V660

A P661N-specific OTA from build 250514V641 to 250723V660 was identified. The tracker records:

- OTA title: `P661N-H334IJKLN-T-GL-250514V641-250723V660_20250723184746`
- update version: `P661N-H334IJKLN-T-GL-250723V660`
- size: 99.9 MB
- fingerprint: `Itel/P661N-GL/itel-P661N:13/TP1A.220624.014/250723V660:user/release-keys`
- OTA payload URL resolved to the Google OTA host: `https://android.googleapis.com/packages/ota-api/package/4ce43b9f817a9755533a0a137693f3371958f81f.zip`

The tracker is explicitly not affiliated with Transsion/itel, so its metadata is treated as a discovery/provenance lead rather than authoritative OEM evidence. The direct payload could not be downloaded in the current research environment, so no APK contents or SmartPanel certificate can yet be claimed from this OTA.

The next safe binary gate is to obtain the OTA payload bytes, inspect the ZIP without flashing it, identify whether it contains `com.transsion.smartpanel` / `SmartPanel.apk`, and compare that APK's signing certificate with the device baseline `7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb`. No installation or firmware flashing is part of this step.


