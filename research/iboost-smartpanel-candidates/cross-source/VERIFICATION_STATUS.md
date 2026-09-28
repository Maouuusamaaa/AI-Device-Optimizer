# Candidate Verification Status

Updated 2026-09-28.

## Current result

No candidate has been approved as installable.

| Candidate | Source metadata | Binary acquired here | Signature locally verified | Installability |
|---|---|---|---|---|
| 15.2.1.052 / 150201052 | verified from public source metadata | No | No | Pending |
| 15.2.0.117 / 150200117 | verified from public source metadata | No | No | Pending |
| 3.8.2.077 / 30802077 | verified from public source metadata | No | No | Pending |

## Why binary verification is still pending

The research web environment can inspect the public release pages and source-reported hashes/certificates, but it cannot retrieve the raw APK bytes from the current download endpoints. The local container also has no outbound DNS/network access.

Therefore:

- Source-reported SHA-256 values are recorded as provenance, not as locally verified hashes.
- No candidate is marked VALID or REJECTED solely because binary acquisition failed.
- No installation attempt has been made.
- The OEM SmartPanel system APK has not been modified.
- Signature bypass or package-update bypass is not part of this research.

## Verified source observations

15.2.1.052 and 15.2.0.117 are arm64-v8a, Android 12+ releases of package `com.transsion.smartpanel`. APKMirror reports the same certificate SHA-1/SHA-256 for the aec8 variants of both releases. citeturn0search1turn0search4

3.8.2.077 is also package `com.transsion.smartpanel`, but its APKMirror listing is attributed to Shalltry Group and reports a different certificate lineage (XOS/InfinixMobility). citeturn5view0

This difference is an important compatibility checkpoint, not a conclusion that the candidate is installable or uninstallable.

## Next executable gate

When raw APK bytes become accessible, process candidates in this order:

1. 15.2.1.052
2. 15.2.0.117
3. 3.8.2.077

For each binary: SHA-256 → APK signing certificate → package/version → ABI/native libraries → manifest/permissions/components → Game Mode components → device compatibility. Only a candidate that passes all required gates may become VALID.
