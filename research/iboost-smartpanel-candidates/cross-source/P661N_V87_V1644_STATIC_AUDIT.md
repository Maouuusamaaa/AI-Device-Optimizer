# P661N SmartPanel V87 → V1644 Static Integration Audit

## Scope

Read-only comparison of the verified P661N SmartPanel baseline and the newer P661N V1644 `system_ext` extraction.

No APK was installed, flashed, patched, resigned, or modified.

## Verified lineage

| Point | Build | SmartPanel | VersionCode | APK SHA-256 | Certificate SHA-256 |
|---|---|---|---:|---|---|
| V87 | `P661N-H334IJKL-T-GL-240716V87` | 3.0.0.216 | 5233 | `986709e8e131cf7a1a619e5e82e865e87affd330db02020662cce483579002ec` | `7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb` |
| V1644 system_ext | `P661N-H334IJKLN-T-BASE-250723V1644` | 3.0.0.224 | 5241 | `34965f3e4d5154cd5b1dcfdcc5a9b92b1343ac501de8acbe33655243d3cf47dd` | `7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb` |

V1644 certificate SHA-1:

`be8cb9f95bcb5bfb04045034e5182634a2fca1fa`

V1644 certificate subject:

`C=CN, ST=Guangdong, L=Shenzhen, O=TRANSSION, OU=Itel, CN=RoyChen, emailAddress=yong.chen@itel-mobile.com`

## Game Mode integration surface

The V1644 static extraction contains the following relevant classes/components:

- `com.transsion.gamemode.activity.MainSettingsActivity`
- `com.transsion.gamemode.service.GameModeService`
- `com.transsion.router.gameaccelerator.IGameAcceleratorService`
- `com.transsion.smartpanel.commands.GameAccelerateCommand`
- `com.transsion.gamemode.provider.AppListProvider`
- `com.transsion.gamemode.provider.GameSearchProvider`
- `com.transsion.gamemode.video.provider.VideoListProvider`
- `com.transsion.gamemode.activity.GameManageActivity`
- `com.transsion.gamemode.activity.PqeSettingsActivity`

The static strings/classes also reference:

- `content://com.transsion.gamemode.provider`
- `content://com.transsion.gamemode.provider.video`
- `/proc/game_state`
- `/proc/main_game_state`
- `/gameaccelerator/GameAccelerateService`

The Game Mode UI exposes the action:

`com.transsion.gamemode.SETTINGS_ACTIVITY`

and the game-management action:

`com.transsion.gamemode.GAME_MANAGE`

## Provider and permission surface

The verified SmartPanel runtime evidence exposes:

- authority: `com.transsion.gamemode.provider`
- provider: `com.transsion.smartpanel/com.transsion.gamemode.provider.AppListProvider`
- video authority: `com.transsion.gamemode.provider.video`
- provider: `com.transsion.smartpanel/com.transsion.gamemode.video.provider.VideoListProvider`
- search provider: `com.transsion.smartpanel/com.transsion.gamemode.provider.GameSearchProvider`

SmartPanel declares:

- `com.transsion.gamemode.permission.READ_APP_LIST` — normal
- `com.transsion.gamemode.permission.WRITE_APP_LIST` — signature|privileged
- `com.transsion.gamemode.permission.READ_RESTRICTED_LIST` — normal
- `com.transsion.gamemode.permission.WRITE_RESTRICTED_LIST` — normal

This confirms that the read-only AppListProvider path used by AI-Device-Optimizer is a real integration surface on the P661N SmartPanel lineage.

The write permission remains protected and must not be used by the optimizer.

## What changed from the current device baseline

The strongest verified change is the P661N SmartPanel package version:

`3.0.0.216 / 5233 → 3.0.0.224 / 5241`

while the certificate remains identical.

The available static evidence does **not** establish a new externally callable Game Mode control API in V1644. In particular, presence of `IGameAcceleratorService`, `GameAccelerateCommand`, `GameModeService`, or the `/proc` strings is not proof that a third-party ordinary application can safely invoke those mechanisms.

Therefore the current supported integration remains:

`AI Device Optimizer → read-only SmartPanel AppListProvider → Game Mode evidence`

rather than:

`AI Device Optimizer → direct Game Mode mutation/control`

## Important versionCode collision finding

A public APKMirror listing also exposes Smart Panel 3.0.0.224 / versionCode 5241, but that public APK is signed by Shalltry Group with certificate SHA-256:

`a2f1535b2e2e6b707412f8732a08d7911c0cb7b81d061504eba75da32ca3492f`

This differs from the P661N V1644 certificate:

`7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb`

Therefore:

1. versionName/versionCode cannot be used alone to identify a P661N-compatible SmartPanel binary;
2. the P661N certificate must remain the primary signing-lineage gate;
3. the public 3.0.0.224 APK must not be treated as the V1644 P661N APK;
4. no normal in-place update should be attempted using the differently signed public APK.

## V631 / V641 / V660 status

No public extracted `system_ext/app/SmartPanel/SmartPanel.apk` newer than V1644 has been verified.

The P661N OTA lineage around V641/V660 remains a provenance lead, but without extracted SmartPanel bytes and certificate evidence it cannot establish a newer P661N SmartPanel binary.

## Engineering conclusion

V1644 does not justify changing the current AI-Device-Optimizer integration architecture.

The correct implementation boundary remains:

- read SmartPanel Game Mode evidence;
- record provider availability and checked game packages;
- use the evidence in diagnosis/policy simulation;
- keep Safety Gate authoritative;
- do not write to the Game Mode provider;
- do not invoke undocumented accelerator services;
- do not replace SmartPanel;
- do not depend on third-party SmartPanel APKs with a different signing lineage.

The next meaningful research target is a P661N-signed SmartPanel binary from a newer P631/V641/V660-era filesystem extraction. Until such a binary is obtained, V1644 is the latest verified P661N reference point.
