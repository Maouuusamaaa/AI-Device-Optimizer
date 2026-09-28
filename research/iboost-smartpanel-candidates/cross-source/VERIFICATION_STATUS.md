# Candidate Verification Status

Updated 2026-09-28.

## Final binary-audit result for the current queue

| Candidate | Binary acquisition | SHA-256 | Package/version | Signing certificate | Binary inspection | Device/install status |
|---|---|---|---|---|---|---|
| 15.2.1.052 / 150201052 | PASS | PASS | PASS | PASS | PASS | Compatibility/install approval pending |
| 15.2.0.117 / 150200117 | blocked | not locally verified | not inspected | not locally verified | not inspected | Pending binary acquisition |
| 3.8.2.077 / 30802077 | blocked | not locally verified | not inspected | not locally verified | not inspected | Pending binary acquisition |

## 15.2.1.052 binary evidence

GitHub Actions run 36420243125 / job 108920936368 successfully acquired the APK through the APKPure direct endpoint and inspected the temporary binary. The APK was deleted from the runner after inspection and was not committed to Git.

- Package: com.transsion.smartpanel
- versionCode: 150201052
- binary size: 90,204,267 bytes
- SHA-256: 4abd7143d0e60df6fc5fa5de0a4549670fa52c37c7402bb858971b67d0773997
- source-reported SHA-256: same value
- signing certificate SHA-256: 40e4400c5c90f79d8f390584eebad893ac9bdba0ff1507b126d4c9db547929da
- signing certificate SHA-1: aec83f63bfa3a6ad9422086688639fea7684ef00
- certificate subject: EMAILADDRESS=hios@tecno-mobile.com, CN=HiOS, OU=HiOS, O=TecnoMobile, L=Shanghai, ST=Shanghai, C=CN
- APK ZIP integrity: passed
- native libraries under lib/arm64-v8a or lib/armeabi-v7a: none found in this APK
- manifest inspection: passed; the APK contains providers, activities, services, receivers, and activity-alias entries
- Game Mode resource/string inspection: positive evidence found for Game Mode, GameMode_PanelView, Please select an app to run in Game Mode, Set apps to enable Game mode, and com.transsion.gamespace.activity.GameSpaceActivity
- permission inspection: the binary declares Game Mode app-list permissions including com.transsion.gamemode.permission.READ_APP_LIST and com.transsion.gamemode.permission.WRITE_APP_LIST; it also declares many privileged/system-oriented permissions and Smart Panel custom permissions.

These findings establish that 15.2.1.052 is a real, internally consistent Smart Panel binary with explicit Game Mode-related resources and permissions. They do not by themselves establish that it can replace the P661N system APK.

## Signature compatibility gate

The current P661N baseline is com.transsion.smartpanel version 3.0.0.216 / versionCode 5233, stored at /system_ext/app/SmartPanel/SmartPanel.apk.

The baseline APK SHA-256 is recorded as:

986709e8e131cf7a1a619e5e82e865e87affd330db02020662cce483579002ec

The baseline signing certificate has now been measured directly from the device with Android's apksigner tooling:

- certificate SHA-256: 7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb
- certificate SHA-1: be8cb9f95bcb5bfb04045034e5182634a2fca1fa
- certificate MD5: 3d2cbb77572e5a14f98b575bb759498b

The baseline certificate subject is EMAILADDRESS=yong.chen@itel-mobile.com, CN=RoyChen, OU=Itel, O=TRANSSION, L=Shenzhen, ST=Guangdong, C=CN.

This establishes that 15.2.1.052 is NOT signature-compatible with the installed P661N SmartPanel baseline: its certificate SHA-256 is 40e4400c5c90f79d8f390584eebad893ac9bdba0ff1507b126d4c9db547929da. Therefore it cannot be treated as a normal in-place package update candidate for this device.

No package-update or signature bypass was attempted.

## 15.2.0.117

The public source metadata identifies package com.transsion.smartpanel, versionCode 150200117, arm64-v8a, Android 12+, and the same aec8 certificate lineage reported for 15.2.1.052. APKMirror also reports SHA-256 0bae2238489b0f514af6ec10f55406dfe44c91acc31228cf15ed5f0a610eddbc. citeturn8search1

The CI runner could not resolve an APKMirror direct-download URL for this candidate, so no binary-level claim is made.

## 3.8.2.077

The public source metadata identifies package com.transsion.smartpanel, versionCode 30802077, arm64-v8a + armeabi-v7a, Android 12+, and a different reported certificate lineage (e03765... / a2f153...) attributed to Shalltry Group. APKMirror reports SHA-256 df884f9cb1499c5c7ccfaca835ce8d3fbd65d0fbe9cf0160822ed77fffbf4ed9. citeturn0search2

The CI runner could not resolve an APKMirror direct-download URL for this candidate, so no binary-level claim is made.

## Classification

### Candidate that passed binary research/inspection

15.2.1.052 / versionCode 150201052

This candidate passed the binary research gates:
1. exact binary acquisition,
2. ZIP/APK integrity,
3. SHA-256 equality to the recorded source hash,
4. package identity,
5. versionCode,
6. APK signing certificate extraction,
7. manifest/component inspection,
8. native-library inventory,
9. Game Mode-related resource/string inspection.

It is now classified as **signature-incompatible with the installed P661N baseline**. Its binary is internally valid, but its signing certificate differs from the device's installed SmartPanel certificate. It is therefore not eligible for a normal in-place update path.

### Device-side signature gate result

Installed P661N SmartPanel:
- versionName: 3.0.0.216
- versionCode: 5233
- package: com.transsion.smartpanel
- baseline certificate SHA-256: 7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb
- baseline certificate SHA-1: be8cb9f95bcb5bfb04045034e5182634a2fca1fa
- subject: EMAILADDRESS=yong.chen@itel-mobile.com, CN=RoyChen, OU=Itel, O=TRANSSION, L=Shenzhen, ST=Guangdong, C=CN

15.2.1.052:
- certificate SHA-256: 40e4400c5c90f79d8f390584eebad893ac9bdba0ff1507b126d4c9db547929da
- result: **FAIL — certificate mismatch**

The comparison is decisive for the normal Android package-update path; no signing bypass is appropriate.

### Candidates not yet classified as failed

- 15.2.0.117: binary acquisition blocked.
- 3.8.2.077: binary acquisition blocked.

A download limitation is not treated as evidence of an invalid APK.

## Safety boundary

The OEM /system_ext/app/SmartPanel/SmartPanel.apk has not been modified. No uninstall, forced privileged grant, signature bypass, package-manager bypass, or system partition modification was performed.
