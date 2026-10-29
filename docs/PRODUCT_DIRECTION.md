# Product direction

Status: implementation plan, researched 29 October 2026. A competitor feature list establishes a product baseline; it does not independently validate demand, usability, nutrition accuracy or health outcomes.

## Audience and positioning

DietPal targets adults organising everyday eating: record portions, understand logged calories/macros, find a meal, plan ahead and take an ingredient list shopping. The flagship should work without a developer's private API key. Daily records stay on the device; online services receive only the explicit food/recipe queries needed for lookup.

## Competitive baseline

| Capability | Primary evidence | DietPal direction |
| --- | --- | --- |
| Food diary, calories and macros | [MyFitnessPal plan comparison](https://support.myfitnesspal.com/hc/en-us/articles/34889191368077-The-difference-between-Free-Premium-and-Premium), [Yazio app tutorial](https://help.yazio.com/hc/en-us/articles/11804776635281-Tutorial-of-the-Yazio-app) | Portion-aware entries, meal groups, history and editable goals. |
| Faster food entry and reusable meals | [MyFitnessPal Premium features](https://support.myfitnesspal.com/hc/en-us/articles/360032625951-MyFitnessPal-Premium-features), [Lifesum features](https://lifesum.com/features/) | Food search, barcode lookup, recent/custom foods and saved meals. Camera scanning is separate from numeric barcode lookup and must not be claimed unless verified. |
| Recipes, planning and groceries | [MyFitnessPal Meal Planner](https://support.myfitnesspal.com/hc/en-us/articles/34603055097869-How-to-use-the-Meal-Planner), [Yazio Pro features](https://help.yazio.com/hc/en-us/articles/208847005-How-does-Yazio-Pro-support-my-goals) | Complete recipe details, favourites, dated meal plans and an ingredient checklist. |
| Water, measurements and trends | [Yazio app tutorial](https://help.yazio.com/hc/en-us/articles/11804776635281-Tutorial-of-the-Yazio-app), [Lifesum features](https://lifesum.com/features/) | Daily water and weight records, history and understandable trends. |
| User control and portability | [Cronometer subscription features](https://support.cronometer.com/hc/en-us/articles/360028026971-Subscription-Types) | Clear data sources, local storage and export/delete controls. |

These references describe competitor capabilities. They do not imply complete feature parity or endorsement.

## Implementation priorities

1. Restore an independently runnable original edition and establish its actual working flows. Preserve the historical source privately and document compatibility fixes separately.
2. Repair recipes: real provider results, name search, categories, complete ingredients/instructions, favourites, offline states and retry. Verify successful live responses on Android.
3. Build a consistent flagship experience around a daily diary: calorie/macro totals, portions, custom food entries, goals and history.
4. Add practical planning: saved recipes, a dated meal plan, shopping checklist, water/weight records and progress.
5. Validate first use, empty/error/offline states, persistence, arithmetic, navigation, text scaling and the main journeys. Publish screenshots and installable builds only with accurate coverage statements.

## Data and accuracy

[TheMealDB API](https://www.themealdb.com/api.php) documents a public development/educational endpoint. Its [terms](https://www.themealdb.com/terms_of_use.php) distinguish educational use from app-store distribution. Recipe/image sources must remain attributed. Recipe results do not supply reliable calorie/macronutrient values for this app; no nutrition figures should be invented from a recipe title or photo.

[Open Food Facts](https://openfoodfacts.github.io/openfoodfacts-server/api/) offers product nutrition data under its documented database/content licences. It requires an identifying User-Agent and rate-limit handling. Missing nutrients must remain unknown, not silently become zero; users must be able to enter/check label values. Development requests must respect the documented staging workflow. No contribution/write endpoints are needed.

Food values and manually entered portions can be incomplete or inaccurate. The app will distinguish supplied label data, manual entries and calculated portion totals. Goals are user controlled. No clinical recommendations or verified health benefit are claimed.

Photo-based calorie inference, medical coaching, paid subscriptions, social feeds, wearable/cloud synchronisation and app-store distribution are not promised by this release plan. They require separate product, data, platform and validation work.
