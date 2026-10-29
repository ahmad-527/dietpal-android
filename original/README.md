# DietPal Original — repaired coursework edition

The historical interface and animated exercise guides, with account-security, Android compatibility and recipe-connection repairs. The frozen [coursework-source branch](https://github.com/ahmad-527/dietpal-android/tree/coursework-source) preserves the selected historical source separately; this runnable edition contains documented fixes.

## Run or build

Download `DietPal-Original-preview.apk` from the [two-edition release](https://github.com/ahmad-527/dietpal-android/releases/tag/two-edition-preview-20261029). It is a debug-signed portfolio review build, not an app-store release. Android 8.0/API 26 or later. Register a local demonstration account, then log in.

To build, use JDK 17, Android SDK/platform 34 and Build Tools 33.0.1. From this folder run:

```sh
./gradlew assembleDebug testDebugUnitTest
```

Windows: `gradlew.bat assembleDebug testDebugUnitTest`. Android Studio can configure your local SDK, or put `sdk.dir=/your/sdk` in a private `local.properties`. Both are excluded from Git. The wrapper pins Gradle 8.0 and its distribution checksum.

No recipe credential is needed for the public TheMealDB fallback. The original Spoonacular adapter is retained: copy `secrets.properties.example` to private `secrets.properties` and set your own key before building. Never commit that file or publish an APK containing a private key. The optional credentialed route has not been exercised in current live validation; the public fallback has.

## Verified scope

Local registration/login/logout, BMI input validation/calculation, animated exercise guide, start/pause timer, real recipe browse/search/details and useful Settings/About/Privacy/Project information. [Validation](../docs/VALIDATION.md) records exact coverage and limits. There is no cloud account, password recovery, clinical assessment or automatic exercise detection. Clear storage through Android app settings to erase local accounts.

Separate application IDs allow Original and Flagship to be installed together. Earlier preview APK signatures may require uninstalling an older preview; export or preserve needed data first.
