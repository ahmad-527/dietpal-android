# Nutrition and personalised planning

Status: development preview with automated calculation, persistence and Android integration checks. This document distinguishes product rules, food-data calculations and research findings; it does not establish clinical or nutrition effectiveness for DietPal.

## What informs the design

The [100-record screening register](research/SCREENING.md) reports its method and exclusions. Titles and selected abstract sentences were screened; this is not 100 full-text appraisals, an independent systematic review, or 100 supporting studies. Fifteen records were retained as product-relevant candidates, 23 for context and 62 excluded from product-rule evidence. Publication-type metadata alone does not verify every journal's editorial peer-review process.

Focused primary-source checks also informed the following decisions:

| Decision | Evidence and its limits | Product implication |
| --- | --- | --- |
| Meal frequency is a preference | [2020 network meta-analysis](https://pmc.ncbi.nlm.nih.gov/articles/PMC7490164/) found limited robust evidence for reducing frequency. [2024 review](https://pubmed.ncbi.nlm.nih.gov/39485353/) included 29 trials and reported small effects, substantial bias concerns and heterogeneity. | Offer one to six eating occasions without claiming one pattern is universally healthier or a fasting treatment. |
| Personalisation needs user goals and feedback | [2021 systematic review](https://pubmed.ncbi.nlm.nih.gov/33313795/) found promising dietary-intake results across 11 trials. [2022 review](https://pubmed.ncbi.nlm.nih.gov/36235756/) found inconsistent benefits for genetic personalisation. These are different intervention definitions. | Support practical diet/food/schedule preferences; do not infer medical, genetic or metabolic prescriptions. |
| Plan ahead and retain variety | [Ducrot et al.](https://pubmed.ncbi.nlm.nih.gov/28153017/) observed associations with diet quality and variety in 40,554 adults. Cross-sectional, self-reported exposure and volunteer selection prevent causal conclusions. | Provide dated plans, different recipes and easier preparation. Do not claim the app causes weight loss or improves diet quality. |
| Avoid a universal winning macro split | [Two-year trial](https://pubmed.ncbi.nlm.nih.gov/19246357/) found similar weight loss across four assigned macro patterns within its supported intervention. The [DIETFITS trial](https://pubmed.ncbi.nlm.nih.gov/29466592/) compared healthy low-fat and low-carbohydrate interventions over 12 months. Neither validates an arbitrary app target for everyone. | Fit user-entered targets; expose differences instead of presenting one automatic ratio as correct for all users. |
| Planning and actual eating are separate | Dietary-record and adherence research has measurement limitations; a generated menu is not evidence it was eaten. | Planned food only enters the diary after an explicit review/log action. |

These decisions are evidence-informed product hypotheses and user requirements. DietPal still needs independent dietitian assessment, usability testing and prospective evaluation before any effectiveness claim.

## Existing-app comparison

[Eat This Much's official guide](https://www.eatthismuch.com/how-to/) describes custom nutrition profiles, meal counts/layouts, food exclusions, recurring favourites, serving adjustments and regeneration. [MyFitnessPal's official planner guide](https://support.myfitnesspal.com/hc/en-us/articles/34603055097869-How-to-use-the-Meal-Planner) describes dated planning, preferences, meal selection, swapping and logging. These establish useful interaction patterns, not scientific proof or a claim of feature parity.

## Recipe calculations

- DietPal includes 40 original gram-specified recipe compositions using 37 foods from the official [USDA SR Legacy April 2018 download](https://fdc.nal.usda.gov/download-datasets/). The [FoodData Central API guide](https://fdc.nal.usda.gov/api-guide/) identifies the data as public-domain/CC0. The frozen release is reproducible; it is not presented as current branded-product composition.
- `tools/build_meal_catalog.py` creates the bundled ingredient register and recipe catalog. The register retains FDC IDs, source descriptions, per-100-g values, source URLs and archive SHA-256.
- Each base serving sums edible ingredient grams × the supplied per-100-g energy, protein, carbohydrate and fat. Fibre is also shown. Raw/cooked/drained states are explicit. No cooking-retention or yield adjustments are inferred.
- These are ingredient-based estimates, not laboratory analysis of finished meals. Brand, preparation, substitutions, portion measurement and natural variation affect values. Added ingredients must be logged separately.
- External TheMealDB recipes without calculated nutrition remain browsable but are excluded from macro optimisation. Missing nutrition is not zero.
- The balanced collection uses combinations of vegetables/fruit, grains, legumes, protein foods and measured fats. “Balanced” is a collection description, not certification that each meal or a macro-matched year supplies every necessary nutrient.

## Planner rules

1. The user chooses a start date, week/month/year period, mixed/vegetarian/vegan preference, one to six eating occasions, exclusions and their own exact, ranged, minimum or maximum calorie/macro targets (unset dimensions remain optional).
2. Diet and recorded ingredient/allergen exclusions are hard filters. Product labels and preparation still need checking for cross-contact or ingredients not represented by this catalog.
3. The numerical fitter chooses different recipes and adjusts base servings between 0.5 and 2 in increments of 0.05. A meal occasion can contain multiple dishes; selecting one occasion does not secretly create four meal times.
4. Target fit is prioritised before variety/favourite bonuses. Recent use discourages repetition; a recipe cannot repeat within a generated day. This is a bounded heuristic, not a proof of a globally optimal or always-feasible menu.
5. The preview compares totals with the user's constraints. Explicit ranges and minimum/maximum limits are strict. Single-number point goals use a clearly disclosed 10% planning tolerance; a strict range can also have identical endpoints. The tolerance is an engineering choice, not a clinical threshold. Any unmet constraint remains visible. The app does not relabel an impossible target as met.
6. Month/year periods use calendar arithmetic, including month ends and leap years. Longer schedules remain editable, organisational plans; no study establishes that a fixed DietPal annual menu suits a person's changing needs.
7. Saving replaces only the previewed dates, within a transaction. Other dates and the food diary remain separate. Changing next week does not rewrite the previous week.
8. Swaps keep the remaining dishes fixed, respect the saved profile's restrictions and rank alternatives by the resulting whole-day target fit. Totals and gaps update after a change.
9. A saved recipe can be logged as eaten only through the editable food-entry form. Planned portions scale nutrient values; recipe details scale their displayed ingredient weights and estimates to the selected planned servings. The base-serving basis remains explicit.

Micronutrient adequacy, therapeutic diets, clinical contraindications, medical calorie prescriptions and safe treatment of eating disorders are outside the verified planning scope. Macro matching alone must not be represented as complete personalised nutrition care.

## Label-defined supplements and energy constraints

Users may include a supplement they choose, using its actual product-label serving and daily quantity. The planner keeps that chosen quantity fixed and counts all supplied calories, protein, carbohydrate and fat before fitting the remaining food. No generic whey nutrition, brand, dose, timing or medical need is invented. Milk/soy source declarations and additional recorded label allergens cannot override dietary exclusions.

When a protein target remains unmet, the app can show protein-rich recipe options respecting the profile and an optional route to enter a supplement label. This is a planning option, not a recommendation that every user needs a supplement. The [NIH Office of Dietary Supplements](https://ods.od.nih.gov/factsheets/ExerciseAndAthleticPerformance-HealthProfessional/) discusses protein from both food and supplements and the limits of generalising athlete guidance. That guidance does not validate an individual DietPal target or product.

The [USDA Food and Nutrition Information Center](https://www.nal.usda.gov/programs/fnic) describes the general energy factors of 4 kcal/g for protein/carbohydrate and 9 kcal/g for fat. Thus exactly 180 g protein, 100–150 g carbohydrate and 60 g fat correspond to roughly 1,660–1,860 kcal. A calorie minimum above that range may conflict with exact macro allowances. Ingredient-table/label energy and rounding can differ from this rough calculation. The app retains the requested constraints and exposes an unmet fit instead of adding uncounted supplement calories or silently increasing macros.

Tracking goals are effective-dated. A future target change does not alter earlier dates, and editing a generated day's target snapshot does not reshuffle its meals automatically.

## Carbohydrate and liquid-label basis

The planner's carbohydrate goals use **total carbohydrate including fibre**, matching the USDA SR Legacy carbohydrate-by-difference values. Open Food Facts may supply available carbohydrate separately. Its [official nutrition schema](https://openfoodfacts.github.io/documentation/docs/Product-Opener/schemas/schemas/product_nutrition/) distinguishes `carbohydrates` from `carbohydrates-total`. DietPal uses an explicit total when present, or adds available carbohydrate and fibre only when both are supplied. Missing fibre is not silently assumed to be zero. Manual food and supplement forms expose that label-basis choice.

The parser supports both legacy `nutriments` and current `nutrition.aggregated_set` responses. Structured values must have a recognised 100 g, 100 ml or serving basis and compatible units. Liquids scale by ml; the app does not invent a g/ml density conversion. Declared kcal are preferred, with explicit kJ/4.184 conversion where appropriate. Unknown-basis or inequality-modified values remain unknown. The actual development-staging fixture and native lookup check are recorded in validation; community records can change.

Recorded allergen tags are conservative filters, not certified absence claims. Oats carry a gluten precaution in this collection; the source food record does not establish a gluten-free product or kitchen. The user still needs ingredient and cross-contact information.
