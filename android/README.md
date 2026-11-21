# Practix Android application

This guide covers the native Android application in this directory. It is
separate from the Dentara Next.js web application documented in
[`../docs/WEB.md`](../docs/WEB.md).

## Project

- Android Gradle project, Kotlin and Jetpack Compose.
- Application ID: `com.dentara.dental`.
- Min SDK 24, target SDK 36 (configured in `app/build.gradle.kts`).
- SQLCipher database and native whisper.cpp integration are present in source.

## Build

Use Android Studio with the configured Android SDK, JDK 17, pinned NDK and CMake
versions. From this directory:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Connected tests require a device or emulator. Native speech performance and
recognition accuracy require real-device evaluation with a known model file and
consented, de-identified recordings.

## Evidence and current limits

[`STATUS.md`](STATUS.md) is the current feature/evidence matrix. It distinguishes
code presence from unit tests, device checks and production evidence. The older
[`FEATURES.md`](FEATURES.md) is retained as an inventory/reference document;
its feature descriptions are not verification or readiness claims.

Do not use the application with real patient records based on source presence
or a successful build alone. Security, migration recovery, clinical accuracy,
backup/export and device workflows require separate validation.
