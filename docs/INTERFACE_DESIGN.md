# Flagship interface

The flagship uses a daily health dashboard: nutrition first, quick logging actions, separate water and movement cards, a grouped food diary, and five consistent navigation destinations. The repaired original edition retains its historical interface.

## Reference review

Reviewed on 29 October 2026:

- [Samsung Health](https://www.samsung.com/us/apps/samsung-health/): modular metric summaries, nutrition totals, coloured nutrient indicators and weekly charts.
- [Huawei Health](https://consumer.huawei.com/en/mobileservices/health/) and [card management](https://consumer.huawei.com/en/support/content/en-us01057400/): separate function cards and details for each metric.
- [Apple Fitness summary](https://support.apple.com/guide/iphone/see-your-activity-summary-iph4c34a8a95/ios): progress at a glance, clear metric hierarchy and access to history.

These references informed the information hierarchy and interaction patterns. DietPal uses its own native components, vector icons, colours and nutrition progress display; no reference-app artwork or screenshots are bundled.

## Design decisions

- Sans-serif typography, restrained headings, neutral backgrounds and white metric cards replace the earlier oversized editorial headings.
- Blue identifies primary actions and energy progress; purple, orange and teal distinguish protein, carbohydrate and fat. Text labels also identify every value.
- The energy ring shows logged intake against a user-set goal. It does not represent calories burned. No goal produces an empty ring and a target-setting action.
- Food entry, a meal photo and recipe discovery are available from the daily summary. Each diary meal group has a contextual add action.
- Water and workout cards show actual stored records. No sensor-derived steps, heart rate, sleep or energy expenditure is fabricated.
- Recipe discovery uses two columns where space permits and one column on narrow screens or at larger font settings. Dashboard cards and macro summaries also stack at larger font settings.
- Controls retain at least 48 dp touch targets. Native text exposes exact values; decorative rings, charts and icons do not duplicate screen-reader content.
- Recipe discovery retains the same bottom navigation as the main screens. Detail screens have a labelled back action.

Screenshots and Android validation evidence are recorded separately. A visual reference review does not establish usability through user testing.
