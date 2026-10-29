# Validation

Validation performed on the October 2026 portfolio edition using an isolated Eclipse Temurin JDK 17.0.20.1, Gradle 8.0, Android Gradle Plugin 8.1.2, API 34 platform and build-tools 33.0.1.

| Check | Result |
| --- | --- |
| `assembleDebug` | Passed; debug APK generated. |
| `testDebugUnitTest` | 17 tests passed; zero failures, errors or skipped tests. |
| `lintDebug` | Passed with zero errors and 197 warnings. |
| `connectedDebugAndroidTest` | One complete workflow passed on an AOSP Android 14/API 34 x86_64 emulator using a Pixel 4 device profile. |

## What was exercised

Host tests cover BMI arithmetic and invalid inputs; salted password records, incorrect passwords and malformed records; SQLite authentication, literal SQL metacharacters, duplicate registration and legacy password migration; successful registration/login and logout navigation; all four recipe endpoints with an empty key; loading-dialog dismissal on the recipe screen; invalid recipe/exercise IDs; all fifteen workout layouts; and prevention of timer navigation after leaving a workout.

The emulator test registers a synthetic account, signs in, calculates BMI after rejecting a zero height, opens the workout list and first exercise, starts/pauses its timer, checks the unconfigured recipe screen and logs out. It removes only its own synthetic account. The screenshots were captured from the tested app on that emulator; the successful workflow was repeated outside Gradle's automatic uninstall solely to retain the screenshot files.

## Static analysis

Remaining warnings are predominantly unused resources, legacy layout/performance suggestions, right-to-left layout attributes, autofill hints and newer dependency-version suggestions. Lint passing is not a statement that the project has no warnings.

`app/lint.xml` suppresses only the NotificationPermission message identifying Picasso's unused RemoteViews notification target. This app loads into image views and does not call a notification-posting API; adding a notification permission to satisfy that unused library path would misrepresent the app's behaviour. No blanket lint baseline or error suppression is used.

## Practical limits

- One emulator configuration was exercised; no physical-device, broad API-level or tablet compatibility assessment was performed.
- No real Spoonacular key was used. Live authentication, API quotas, provider responses and remote image availability remain unverified.
- Local account hashing and migration checks are engineering verification, not a penetration test or a production authentication design.
- The project has no verified calorie logging, diet recommendation model, account synchronisation or BMI-history feature. Original settings/profile placeholders are not promoted as implemented features.
- Historical pins and deprecated Android APIs remain. This is coursework portfolio source, not a Play Store release assessment.
- The original assignment report is private; its screenshots and feature descriptions were not silently substituted with this edition's results.
