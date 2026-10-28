# Third-party components

This repository contains DietPal coursework source, documented portfolio changes and original replacement vector artwork. The original GIFs/photos with unconfirmed reuse permissions are not distributed.

## Gradle wrapper

The Gradle 8.0 wrapper JAR and scripts originate from the Gradle project and are covered by the Apache License 2.0. The wrapper JAR was checked against Gradle's published SHA-256 value `91941f522fbfd4431cf57e445fc3d5200c85f957bda2de5251353cf11174f4b5`. The full upstream licence is retained in [licenses/GRADLE-APACHE-2.0.txt](../licenses/GRADLE-APACHE-2.0.txt). Copyright notices in the wrapper scripts are retained.

Primary sources: [Gradle checksums](https://gradle.org/release-checksums/), [wrapper verification](https://docs.gradle.org/current/userguide/gradle_wrapper.html), [Gradle 8.0 source](https://github.com/gradle/gradle/tree/v8.0.0).

## Dependencies fetched by the build

Dependency source/binaries are not vendored here; Gradle resolves the declared versions from Google Maven and Maven Central. Their respective upstream terms apply:

- [AndroidX](https://developer.android.com/jetpack/androidx) — appcompat, constraintlayout and test libraries.
- [Material Components for Android](https://github.com/material-components/material-components-android).
- [Retrofit](https://github.com/square/retrofit) and its Gson converter.
- [Picasso](https://github.com/square/picasso).
- [JUnit 4](https://github.com/junit-team/junit4).
- [Robolectric](https://github.com/robolectric/robolectric).

The Android SDK, JDK, emulator/system images and Gradle caches used during validation are external tools and are not part of this repository.

## Recipe service

Recipe information and remote images are optional runtime content from [Spoonacular](https://spoonacular.com/food-api). A developer must supply their own key and comply with the service's terms. No provider dataset, API credential or downloaded recipe-image collection is redistributed.

No repository-wide licence is granted by this notice; it records provenance and does not replace upstream licences or create permission for excluded historical assets.
