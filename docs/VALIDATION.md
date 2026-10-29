# Current two-edition validation

29 October 2026. Coverage below applies to the exact review APK hashes in [builds.json](validation/builds.json) and the release's SHA256SUMS.txt. Tests used synthetic demonstration records on a dedicated Android 14/API 34 x86_64 emulator. No physical phone, user health record or paid/cloud AI call was used.

## Builds and checks

| Edition | Host checks | Native Android checks |
| --- | --- | --- |
| Original | 12 passed, zero failures/skips | 2 journeys passed: registration/login, invalid/valid BMI, animated workout/start/pause, real recipe browse/search/details, logout; repaired information pages and recreation. |
| Flagship | 30 passed, zero failures/skips | 13 core checks plus 3 offline checks passed, zero skips. The obsolete account/BMI tests remain in Original; they are not counted as flagship coverage. |

Flagship host coverage includes portion arithmetic and unknown nutrients, persistence/export/clear, calculated ingredient totals, dietary/allergen exclusions, one-to-six real eating occasions, calendar/leap dates, target ranges/limits, fixed supplement accounting, infeasible fits, swaps, dated goals, hidden recipes, current/legacy food-API schemas and reminder scheduling/cancellation.

The 13 native core checks cover real TheMealDB browse/search/details/image loading/save/shopping; actual food/water logging and recreation; navigation and 150% system font; week, next-week and 366-day planning; literal `>2000` target persistence; user-defined whey calories/macros in every planned day; live Open Food Facts staging lookup; animated GIF/frame decoding, pause/recreation/completion/save-once; opt-in notification posting; food-entry draft recovery; actual exported JSON bytes and cancellation of the delete confirmation. Test fixture records/preferences are restored or removed.

Reminder scheduling/cancellation is checked using Android's scheduling API under Robolectric. Native tests check enabled preferences and notification posting through the receiver. Delivery at a real future wall-clock deadline under battery restrictions is **not** established. Alarms are intentionally inexact; Android settings can delay them. The native permission test uses the owned emulator's granted notification permission, rather than claiming every permission-denial/settings path was exercised.

## Offline photo and recipes

The final APK's two real-model checks run without an active default network. They exercise pinned-model verification, actual CPU image inference, the photo screen's local decoding/EXIF/resize path, editable review and explicit diary save. Inference itself never logs food. A separate offline recipe check opens the bundled balanced catalog and its calculated ingredients/nutrition without a provider connection.

Model: official LiteRT Community Gemma 4 E2B instruct conversion, revision `1caa527c1e88b55f046fcd8d2b4265606680663d`, 2,588,147,712 bytes, SHA-256 `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`. Runtime 0.18.1. The 6 GB x86_64 emulator completed an initial real inference in about 32 seconds, identifying pasta from one private meal-image fixture. Repeated integration checks use that same fixture, not independent accuracy samples. This is **not** a calorie/portion benchmark, physical-phone performance result or validation on non-food images and diverse meals.

The model is optional and excluded from Git/APKs. Engineering gates require a supported 64-bit ABI, roughly 6 GB total RAM, 2.5 GB available memory and 2.7 GB free storage; 8 GB RAM is recommended. ARM64 execution, sustained use, battery/thermal behaviour, interrupted downloads and broad camera/picker implementations still need device testing. Interrupted downloads restart. Keep the screen open during download/analysis. Editable AI estimates are clearly identified as estimates in diary provenance.

## Lint and remaining limits

Both `lintDebug` tasks complete with no reported lint errors, but **are not clean audits**. Original retains 537 warnings, largely historical layout/text/accessibility issues. Flagship reports 307 warnings, mostly inherited unused value resources plus localisation/icon/GIF checks. Flagship's embedded lint analyser additionally logs a Kotlin metadata mismatch against its newer LiteRT runtime dependencies; library analysis is therefore incomplete. Build success and runtime tests do not resolve that toolchain-analysis gap. No new broad suppression hides it; the narrow Picasso unused-notification-target exclusion is documented in app/lint.xml.

The optional privately configured Spoonacular route is retained in Original but is not live-tested with a private credential. The default no-private-key TheMealDB path is live-tested. Food lookup uses official development staging in integration tests, and its records are mutable; missing values remain unknown. No camera barcode scanning is claimed: a barcode number can be entered for lookup.

Other unestablished areas include exhaustive accessibility/RTL/localisation, multiple Android/device versions, medical/therapeutic suitability, complete micronutrient adequacy, clinical outcomes, broad photo accuracy, wearable/cloud synchronisation and app-store compliance. Meal fitting is bounded and may leave unmet constraints; point tolerance is disclosed and explicit ranges/limits remain strict. Research screening is not clinical validation.

## Reproduce

From each edition, use its documented JDK/SDK and run `./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` (Windows: gradlew.bat). Native tests intentionally require an emulator. Core tests make documented live read requests; use the project's disposable test environment and provider staging practices. The real-model tests require the separately downloaded, checksum-verified model and a private meal-photo fixture; provisioning is deliberately excluded from normal CI and never replaced with a mock-success result.

The previous portfolio-v1 record is preserved under [archive/PORTFOLIO_V1_VALIDATION.md](archive/PORTFOLIO_V1_VALIDATION.md) and does not validate these current builds. Current publication independently checks source/APK hashes, credential exclusions and the historical source manifest before release.
