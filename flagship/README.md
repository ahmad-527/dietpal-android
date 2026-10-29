# DietPal Flagship — development preview

A native Android diary and dated meal planner with custom calorie/macro constraints, diet preferences, label-defined supplements, recipes, water/weight records and guided movement.

## Run or build

Download `DietPal-Flagship-preview.apk` from the [two-edition release](https://github.com/ahmad-527/dietpal-android/releases/tag/two-edition-preview-20261029). Debug-signed portfolio review build; no account required. Android 8.0/API 26 or later. The APK contains ARM64 and x86_64 local-inference binaries; physical-device compatibility is not established by emulator checks.

Build with JDK 21, Android SDK/platform 34 and Build Tools 34.0.0:

```sh
./gradlew assembleDebug testDebugUnitTest
```

Windows: `gradlew.bat assembleDebug testDebugUnitTest`. Set your own private `local.properties` SDK path or configure Android Studio. Gradle 8.9 and its distribution checksum are pinned; Android Gradle Plugin 8.7.3 is declared. No private API key is needed.

## Explore

Today logs food and water; Recipes starts with 40 bundled nutrient-calculated compositions and has live discovery; Plan generates a preview for chosen dates; Progress shows recorded measurements and movement; Profile contains dated goals, reminders, export, deletion, privacy and upstream licences. A planned meal is separate from eaten food until explicitly logged.

For goals, use a number (`180`), range (`100-150`), minimum (`>2000` or `>=2000`) or maximum (`<=2000`). Leave an unused dimension blank. Point goals use a stated 10% fitting tolerance; ranges/limits are strict. A single eating occasion can include a main and sides. Longer schedules remain editable; the app does not establish clinical suitability or annual nutrient adequacy.

Enter a supplement's actual **per-label-serving** calories/macros and the daily quantity you choose. It is counted before fitting the remaining meals. Total carbohydrate includes fibre; available/net carbohydrate requires fibre for conversion. The app does not automatically prescribe doses or brands.

## Optional local photo estimates

Choose a photo or use an installed camera app. The separate, free Gemma 4 E2B model downloads **2,588,147,712 bytes** from the official LiteRT Community repository under Apache 2.0. Its revision and SHA-256 are pinned in `LocalModel.java` and verified before inference. It is not bundled in Git or the APK.

Capability gate: ARM64/x86_64, approximately 6 GB total RAM minimum (8 GB recommended), at least 2.5 GB available RAM and about 2.7 GB free model storage. These are engineering gates, not a physical-phone support guarantee. Smaller devices retain manual logging, recipes and planning. Interrupted model downloads currently restart; keep the screen open during download/analysis.

One real offline x86_64 emulator inference completed in about 32 seconds. That is runtime integration evidence, **not calorie/portion accuracy validation or a phone benchmark**. Hidden ingredients and serving sizes can be wrong. Review and edit each result before saving; AI estimates remain identified in diary provenance. Photos are not sent to an AI service.

[Validation and known limits](../docs/VALIDATION.md) · [Nutrition/planning methods](../docs/NUTRITION_AND_PLANNING.md) · [Data and component notices](../docs/THIRD_PARTY_NOTICES.md)
