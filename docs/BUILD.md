# Android Build and CI

The Android module uses Android Gradle Plugin 9.4.0, Kotlin 2.3.21, compileSdk 36, and requires Gradle 9.6.0 or newer within the AGP 9.4 compatibility range.

## Local build

From the repository root:

1. Change into `android/`.
2. Use a JDK 17 installation.
3. Run `gradle testDebug`.
4. Run `gradle assembleDebug`.

The repository currently uses the Gradle executable provided by the development environment and GitHub Actions rather than committing a Gradle Wrapper binary.

## CI

`.github/workflows/android.yml` runs on pushes and pull requests targeting `main`.

CI performs:

- JDK 17 setup
- Gradle setup and version verification
- Android unit tests
- Debug APK compilation
- Debug APK artifact upload

A green CI run is required before treating the Android module as build-verified.

## Safety

The CI build does not install the APK on a physical device and does not execute optimizer actions. The application remains dry-run only.

## Next physical-device validation

After CI passes, install the debug APK on a physical Android device and compare the displayed RAM and battery telemetry against independent Android measurements. Record the device model, Android API level, battery/charging state, workload, and observed values.
