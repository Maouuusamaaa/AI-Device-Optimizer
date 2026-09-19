# Android Build and CI

The Android module uses Android Gradle Plugin 9.4.0 with AGP's built-in Kotlin support, compileSdk 36, and Gradle 9.7.1 through the checked-in wrapper.

## Local build

From the repository root:

1. Change into `android/`.
2. Use a JDK 17 installation.
3. Run `./gradlew testDebug`.
4. Run `./gradlew assembleDebug`.

The wrapper distribution is the official Gradle 9.7.1 binary distribution. Its wrapper JAR and distribution URL are committed so local and CI builds use the same Gradle version.

## CI

`.github/workflows/android.yml` runs on pushes and pull requests targeting `main`.

CI performs:

- JDK 17 setup
- Gradle setup and version verification
- Android unit tests
- Debug APK compilation
- Debug APK artifact upload

A green CI run is required before treating the Android module as build-verified.

## Benchmark artifacts

Running `scripts/rish-baseline.sh` from Termux creates a raw TXT capture, a structured JSON report, and a flattened CSV row under `benchmarks/results/`. The analyzer reports mean, median, sample standard deviation, minimum, and maximum for repeated startup measurements.

## Evaluation safety

`benchmarks/evaluator.py` performs measurement-only, like-for-like comparisons and requires at least three runs by default. It does not execute or authorize optimizer actions.

## Safety

The CI build does not install the APK on a physical device and does not execute optimizer actions. The application remains dry-run only.

## Next physical-device validation

After CI passes, install the debug APK on a physical Android device and compare the displayed RAM and battery telemetry against independent Android measurements. Record the device model, Android API level, battery/charging state, workload, and observed values.
