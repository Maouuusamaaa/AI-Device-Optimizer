# Candidate Verification Status

Updated 2026-09-29.

## Final binary-audit result for the current queue

| Candidate | Binary acquisition | SHA-256 | Package/version | Signing certificate | Binary inspection | Device/install status |
|---|---|---|---|---|---|---|
| 15.2.1.052 / 150201052 | PASS | PASS | PASS | PASS | PASS | **Rejected for normal P661N in-place update — certificate mismatch** |
| 15.2.0.117 / 150200117 | blocked | not locally verified | not inspected | not locally verified | not inspected | Pending binary acquisition |
| 3.8.2.077 / 30802077 | blocked | not locally verified | not inspected | not locally verified | not inspected | Pending binary acquisition |

## P661N firmware-lineage search — 2026-09-29

A fresh public-source search was performed for extracted P661N SmartPanel binaries associated with V641 and V660.

Results:

- No new public GitLab result exposing P661N V641/V660 SmartPanel.apk was found.
- No new public GitHub result exposing P661N V641/V660 SmartPanel.apk was found.
- The existing Rama-Firmware-Dumps P661N repository remains the V87 reference and exposes a system_ext tree.
- Public firmware indexes confirm that P661N V641 and V660 firmware packages exist, but the indexed pages do not expose an extracted SmartPanel.apk or its signing certificate.
- The known V641-to-V660 Google OTA payload URL was tested as a binary download in the research environment. The download failed before payload bytes were obtained. Therefore no claim is made about the SmartPanel contents of that OTA.

The search therefore does not produce a newer P661N-signed SmartPanel binary than the already verified V1644 system_ext extraction.

## Current verified P661N reference

V1644 remains the latest verified P661N system_ext SmartPanel reference:

- SmartPanel: 3.0.0.224
- versionCode: 5241
- APK SHA-256: 34965f3e4d5154cd5b1dcfdcc5a9b92b1343ac501de8acbe33655243d3cf47dd
- certificate SHA-256: 7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb

Its static audit found no newly established third-party-callable Game Mode mutation API. The supported optimizer boundary remains read-only Game Mode evidence through AppListProvider.

## Candidate disposition

15.2.1.052 / versionCode 150201052 was binary-researched successfully but rejected for a normal P661N in-place update because its certificate SHA-256 (40e4400c5c90f79d8f390584eebad893ac9bdba0ff1507b126d4c9db547929da) differs from the installed P661N certificate (7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb).

15.2.0.117 and 3.8.2.077 remain unclassified because their binaries could not be acquired by the available CI download path. A download limitation is not treated as evidence of an invalid APK.

## Safety boundary

The OEM /system_ext/app/SmartPanel/SmartPanel.apk has not been modified. No uninstall, forced privileged grant, signature bypass, package-manager bypass, or system partition modification was performed.

## Research conclusion

The firmware-search stage is exhausted for the currently accessible public indexed sources. There is currently no verified V641/V660 SmartPanel binary that can justify changing the integration architecture.

The next engineering step is therefore not SmartPanel replacement. The existing read-only SmartPanel integration should remain the boundary; future work can consume verified Game Mode evidence in measurement and policy simulation without invoking undocumented control APIs.
