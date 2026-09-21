# Stable CI APK Signing

This project uses a stable CI signing key for APK artifacts produced from `main` so later APKs can update an existing installation without changing the application identity.

## GitHub Actions secrets

Configure these repository Actions secrets:

- `ANDROID_SIGNING_KEYSTORE_B64` — base64-encoded JKS bytes
- `ANDROID_SIGNING_STORE_PASSWORD` — JKS password
- `ANDROID_SIGNING_KEY_ALIAS` — signing alias
- `ANDROID_SIGNING_KEY_PASSWORD` — key password

The private keystore must not be committed to the repository. GitHub Actions secrets are the intended storage mechanism for sensitive signing material.

## Artifact policy

Pull requests still compile and validate the Android project, but stable APK artifacts are uploaded only from `main` after signature verification. This prevents a PR build with a transient debug key from being mistaken for an update-compatible release artifact.

## One-time migration

If a device already has an APK signed with an older CI-generated debug key, a newly generated stable key cannot update that installation. Android requires the same signing certificate (or valid proof-of-rotation). Preserve any required app data before removing the old installation, then install the first stable-signed APK. All subsequent stable-signed builds can be updated normally.