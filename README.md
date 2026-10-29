# DietPal

An undergraduate Android project evolving into a practical meal-planning and nutrition-tracking app.

## Two editions

| Edition | Purpose | Current status |
| --- | --- | --- |
| **Original** | Preserve the coursework interface, workout GIFs, photos and application flows. | Restoration and functional verification in progress. Required credential redactions and runtime fixes will be documented. |
| **Flagship** | A redesigned app for everyday meal logging, nutrition tracking and planning. | In development. Features will be marked complete only after implementation and verification. |

Both editions will be available from this repository, with separate build/install instructions and screenshots. They will use different Android application IDs so they can be installed together.

## Current public source: portfolio v1

The source currently at the repository root is the **initial portfolio preparation**, published before the two-edition upgrade. It includes changes to the coursework app and is **not the untouched original or the completed flagship**. Its frozen snapshot is available at [portfolio-v1](https://github.com/ahmad-527/dietpal-android/tree/portfolio-v1).

**Recipe limitation:** this initial version requires a developer-supplied Spoonacular key. Its recipe browsing and details were not verified against live provider responses. Testing the missing-key message did not establish that the feature works end to end. The replacement recipe flow is being implemented with a documented educational API and an offline collection.

The initial build, 17 host tests and one Android 14 emulator workflow passed; that workflow checked the missing-key state, not successful recipe browsing. Its [validation record](docs/VALIDATION.md) applies only to portfolio v1. It has 197 lint warnings and no physical-device validation.

## Flagship direction

The target user is an adult who wants a simple daily food diary and a practical way to plan meals. The competitive baseline and implementation priorities are recorded in [Product direction](docs/PRODUCT_DIRECTION.md).

Planned core: calorie and macro logging, portion-aware food entry, food and barcode lookup, recipe browsing/search/details, saved meals, meal planning, shopping lists, water tracking, progress history and local data controls. These are development targets, not claims about the current build.

The original coursework was developed at **Majan University College** for **Mobile Application Development, 2023–24**. The later portfolio and flagship work is documented separately. Credentials, local SDK paths and private academic records are excluded. Upstream components retain their terms; no repository-wide source licence has been selected.
