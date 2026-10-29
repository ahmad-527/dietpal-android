# Third-party components and data

Both APKs include an Open-source notices screen and retained licence text in `assets/licenses/`. This notice records provenance; it does not grant an overall source licence for DietPal.

## Gradle and libraries

The official Gradle 8.0 wrapper JAR/scripts use Apache 2.0. Wrapper SHA-256: `91941f522fbfd4431cf57e445fc3d5200c85f957bda2de5251353cf11174f4b5`. Distribution versions/checksums are separately pinned per edition. [Gradle checksums](https://gradle.org/release-checksums/) · [Full Apache 2.0 text](../licenses/GRADLE-APACHE-2.0.txt).

Main libraries, fetched from their declared Google Maven/Maven Central repositories:

- [AndroidX](https://developer.android.com/jetpack/androidx) and [Material Components](https://github.com/material-components/material-components-android): Apache 2.0.
- [Retrofit](https://github.com/square/retrofit), [OkHttp](https://github.com/square/okhttp), [Okio](https://github.com/square/okio) and [Picasso](https://github.com/square/picasso): Apache 2.0.
- [Gson](https://github.com/google/gson), [Kotlin](https://github.com/JetBrains/kotlin) and [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines): Apache 2.0.
- [android-gif-drawable](https://github.com/koral--/android-gif-drawable): MIT with included Skia/GIFLIB/ReLinker notices; [retained upstream text](../licenses/ANDROID-GIF-NOTICES.txt). Original uses 1.2.25; Flagship uses 1.2.32.
- Flagship: [Google AI Edge LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM), Apache 2.0, Android runtime 0.18.1. [Retained runtime third-party notices](../licenses/LITERT-LM-THIRD-PARTY-NOTICES.txt).
- Test-only: [JUnit 4](https://github.com/junit-team/junit4) and [Robolectric](https://github.com/robolectric/robolectric), under their respective upstream licences. Test APKs are not release downloads.

JDK/SDK/emulator/system images and build caches are private external tools, excluded from Git and release assets. Their installation terms remain separate.

## Optional model

[Gemma 4 E2B instruct, official LiteRT Community conversion](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm), declared Apache 2.0 by its model card. Revision `1caa527c1e88b55f046fcd8d2b4265606680663d`; file SHA-256 `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`.

The actual enforced SHA-256 is recorded in `LocalModel.java` and the validation receipt. Model weights are downloaded only after an explicit user action and are not redistributed in Git or the APK. No cloud AI service is called. Model licence and runtime correctness do not establish nutrition accuracy.

## Food and recipes

- [USDA FoodData Central](https://fdc.nal.usda.gov/api-guide/): SR Legacy April 2018 ingredient nutrient records, public domain/CC0. `nutrition_ingredients.json` retains food IDs, source descriptions, values and source archive SHA-256. Original recipe compositions and calculations are reproducible with `tools/build_meal_catalog.py`; they are ingredient estimates, not laboratory results.
- [TheMealDB API](https://www.themealdb.com/api.php) supplies live recipe content and images. Its [terms](https://www.themealdb.com/terms_of_use.php) allow educational/development API use and require a paid supporter arrangement for app-store publication. Data/artwork remain attributed in the app. No downloaded provider image collection is bundled. The initial offline starter content uses API-returned recipe information with TheMealDB attribution; the balanced collection is separate.
- [Open Food Facts](https://world.openfoodfacts.org/terms-of-use) supplies community product data under ODbL/DbCL. Users should check product labels. Lookups are read-only with an identifying User-Agent and bounded request frequency. The small staging response fixture is retained with Open Food Facts attribution for parser regression coverage; this repository does not grant a different licence to that data. No OFF product-image dataset is redistributed.
- Original optionally retains the [Spoonacular](https://spoonacular.com/food-api) adapter. A developer must provide their own private credential and follow its terms. No private key is distributed, and the default public build uses TheMealDB.

Research metadata and original screening notes are in `docs/research/`; full third-party abstracts and papers are not republished. Source links describe evidence provenance and do not imply endorsement.
