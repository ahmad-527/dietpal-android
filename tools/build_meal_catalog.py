"""Rebuild DietPal's original recipe estimates from the official USDA SR Legacy CSV ZIP.

Usage: python tools/build_meal_catalog.py --source /path/to/FoodData_Central_sr_legacy_food_csv_2018-04.zip
No API credentials, network calls or third-party recipe text are required.
"""
from pathlib import Path
import argparse
import csv
import hashlib
import io
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE_URL = "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip"
# Source descriptions and cooked states are retained in the generated ingredient register.
FOODS = {
    "oats": (173904, "Rolled oats, dry", ["gluten"]),
    "yogurt": (170894, "Plain nonfat Greek yogurt", ["milk"]),
    "milk": (171269, "Skimmed milk, weighed in grams", ["milk"]),
    "chicken": (171477, "Chicken breast, fully cooked and weighed after cooking", []),
    "rice": (169704, "Brown rice, cooked weight", []),
    "quinoa": (168917, "Quinoa, cooked weight", []),
    "lentils": (172421, "Lentils, cooked and drained", []),
    "chickpeas": (173757, "Chickpeas, cooked and drained without added salt", []),
    "tofu": (172475, "Firm calcium-set tofu, drained", ["soy"]),
    "salmon": (171998, "Wild Atlantic salmon, fully cooked weight", ["fish"]),
    "tuna": (171986, "Light tuna in water, no added salt, drained", ["fish"]),
    "beans": (173735, "Black beans, cooked and drained without added salt", []),
    "bread": (172688, "Whole-wheat bread", ["gluten", "wheat"]),
    "egg": (173424, "Hard-boiled egg, peeled weight", ["egg"]),
    "peanut": (172470, "Smooth peanut butter, no added salt", ["peanut"]),
    "almond": (170567, "Unsalted almonds", ["tree nuts"]),
    "spinach": (168462, "Spinach, raw", []),
    "broccoli": (169967, "Broccoli, cooked and drained without added salt", []),
    "tomato": (170457, "Tomato, raw", []),
    "cucumber": (168409, "Cucumber with peel, raw", []),
    "pepper": (170108, "Red pepper, raw", []),
    "carrot": (170393, "Carrot, raw", []),
    "oil": (171413, "Olive oil", []),
    "banana": (173944, "Banana, peeled weight", []),
    "apple": (168204, "Gala apple with skin, core removed", []),
    "strawberry": (167762, "Strawberries, stalks removed", []),
    "blueberry": (171711, "Blueberries", []),
    "potato": (170093, "Baked potato with skin, cooked weight", []),
    "sweetpotato": (168483, "Baked sweet potato flesh, cooked weight", []),
    "cottage": (173417, "Cottage cheese, 1% milkfat", ["milk"]),
    "couscous": (169700, "Couscous, cooked weight", ["gluten", "wheat"]),
    "pasta": (168910, "Whole-wheat pasta, cooked weight", ["gluten", "wheat"]),
    "walnut": (170187, "Unsalted English walnuts", ["tree nuts"]),
    "chia": (170554, "Chia seeds", []),
    "lemon": (167747, "Lemon juice", []),
    "avocado": (171705, "Avocado flesh", []),
    "edamame": (169283, "Green soybeans, cooked, shelled and drained", ["soy"]),
}

# Every quantity is an edible gram weight for ONE base serving, including oils and toppings.
# Optional seasonings are deliberately absent: adding ingredients changes the estimate.
RECIPES = [
    ("berry-yogurt-oats", "Berry yogurt oats", "Breakfast", "vegetarian", "Mix oats and yogurt with a splash of water. Refrigerate until the oats soften. Top with berries and chia before serving.", {"oats":50,"yogurt":200,"blueberry":80,"chia":10}),
    ("apple-walnut-oats", "Apple walnut oats", "Breakfast", "vegetarian", "Simmer the oats in the weighed milk, adding water as needed. Stir until tender. Top with diced apple and chopped walnuts.", {"oats":50,"milk":180,"apple":120,"walnut":15}),
    ("banana-peanut-porridge", "Banana peanut porridge", "Breakfast", "vegan", "Cook the oats with water until soft. Stir in peanut butter, then add sliced banana and chia seeds.", {"oats":60,"banana":100,"peanut":20,"chia":10}),
    ("egg-avocado-toast", "Egg avocado toast", "Breakfast", "vegetarian", "Toast the bread. Mash avocado with lemon juice, spread on the toast and add sliced hard-boiled egg. Serve with tomatoes.", {"bread":70,"egg":100,"avocado":50,"tomato":100,"lemon":10}),
    ("cottage-berry-bowl", "Cottage cheese berry bowl", "Breakfast", "vegetarian", "Spoon cottage cheese into a bowl. Add oats, chopped strawberries and almonds. Mix just before eating.", {"cottage":200,"oats":40,"strawberry":120,"almond":15}),
    ("tofu-breakfast-toast", "Tofu breakfast toast", "Breakfast", "vegan", "Crumble the drained tofu. Warm the oil in a pan and stir the tofu with chopped pepper and spinach until hot. Serve with toasted bread.", {"tofu":150,"bread":60,"pepper":100,"spinach":50,"oil":5}),
    ("chickpea-avocado-toast", "Chickpea avocado toast", "Breakfast", "vegan", "Mash the cooked chickpeas with avocado and lemon juice. Spread over toasted bread and serve with sliced cucumber.", {"chickpeas":120,"avocado":50,"bread":70,"cucumber":100,"lemon":10}),
    ("quinoa-fruit-bowl", "Quinoa fruit bowl", "Breakfast", "vegan", "Let the cooked quinoa cool. Toss with sliced banana, blueberries, chopped walnuts and chia seeds. Add a splash of water if desired.", {"quinoa":180,"banana":80,"blueberry":80,"walnut":15,"chia":10}),
    ("chicken-rice-bowl", "Chicken brown rice bowl", "Main", "mixed", "Weigh the chicken, rice and broccoli after cooking. Combine and heat through if serving warm. Add diced pepper, olive oil and lemon juice.", {"chicken":130,"rice":180,"broccoli":150,"pepper":80,"oil":10,"lemon":15}),
    ("chicken-quinoa-salad", "Chicken quinoa salad", "Main", "mixed", "Combine the cooked chicken and quinoa with chopped tomato, cucumber and spinach. Dress with the weighed oil and lemon juice.", {"chicken":120,"quinoa":180,"tomato":120,"cucumber":100,"spinach":40,"oil":10,"lemon":15}),
    ("salmon-potato-plate", "Salmon potato plate", "Main", "mixed", "Weigh the salmon and potato after cooking. Serve with steamed broccoli and a dressing of olive oil and lemon juice.", {"salmon":140,"potato":220,"broccoli":180,"oil":8,"lemon":15}),
    ("tuna-pasta-salad", "Tuna whole-wheat pasta salad", "Main", "mixed", "Mix the cooked pasta and drained tuna with diced tomato and cucumber. Toss with oil and lemon juice.", {"tuna":120,"pasta":200,"tomato":120,"cucumber":100,"oil":10,"lemon":15}),
    ("chicken-sweetpotato", "Chicken and sweet potato", "Main", "mixed", "Combine the cooked chicken and baked sweet potato with cooked broccoli. Toss the spinach with lemon juice and olive oil and serve alongside.", {"chicken":130,"sweetpotato":230,"broccoli":150,"spinach":40,"oil":10,"lemon":15}),
    ("salmon-quinoa-bowl", "Salmon quinoa bowl", "Main", "mixed", "Arrange cooked salmon and quinoa in a bowl. Add diced cucumber and pepper. Finish with avocado and lemon juice.", {"salmon":120,"quinoa":180,"cucumber":120,"pepper":100,"avocado":50,"lemon":15}),
    ("lentil-quinoa-salad", "Lentil quinoa salad", "Main", "vegan", "Combine cooked lentils and quinoa with diced tomato, cucumber and pepper. Dress with olive oil and lemon juice.", {"lentils":180,"quinoa":160,"tomato":120,"cucumber":100,"pepper":80,"oil":10,"lemon":15}),
    ("tofu-edamame-rice", "Tofu edamame rice bowl", "Main", "vegan", "Warm the drained tofu in the weighed oil. Add cooked edamame, broccoli and rice and heat through. Finish with lemon juice.", {"tofu":180,"edamame":100,"rice":160,"broccoli":150,"oil":5,"lemon":15}),
    ("blackbean-sweetpotato", "Black bean sweet potato bowl", "Main", "vegan", "Fill a baked sweet potato with warm cooked black beans. Add diced tomato and pepper, avocado and lemon juice.", {"beans":200,"sweetpotato":220,"tomato":120,"pepper":80,"avocado":60,"lemon":15}),
    ("chickpea-couscous", "Chickpea vegetable couscous", "Main", "vegan", "Warm cooked couscous and chickpeas. Fold in grated carrot, diced pepper and spinach. Stir through oil and lemon juice.", {"chickpeas":180,"couscous":180,"carrot":100,"pepper":100,"spinach":40,"oil":10,"lemon":15}),
    ("lentil-potato-bowl", "Lentil potato bowl", "Main", "vegan", "Spoon warm cooked lentils over the baked potato. Serve with chopped tomato, spinach and the weighed olive oil and lemon juice.", {"lentils":220,"potato":220,"tomato":150,"spinach":60,"oil":10,"lemon":15}),
    ("tofu-peanut-noodles", "Tofu peanut pasta", "Main", "vegan", "Cook the tofu in the olive oil. Loosen peanut butter and lemon juice with warm water. Toss with cooked pasta, grated carrot and spinach.", {"tofu":160,"pasta":180,"peanut":15,"carrot":100,"spinach":60,"oil":5,"lemon":15}),
    ("egg-lentil-salad", "Egg lentil salad", "Main", "vegetarian", "Toss cooked lentils with chopped tomato, cucumber and spinach. Add sliced hard-boiled egg, olive oil and lemon juice.", {"egg":100,"lentils":180,"tomato":150,"cucumber":100,"spinach":40,"oil":8,"lemon":15}),
    ("cottage-potato-plate", "Cottage cheese potato plate", "Main", "vegetarian", "Split the baked potato and spoon over cottage cheese. Serve with warm broccoli, diced tomato and the weighed olive oil.", {"cottage":220,"potato":250,"broccoli":180,"tomato":100,"oil":10}),
    ("bean-quinoa-yogurt", "Bean quinoa yogurt bowl", "Main", "vegetarian", "Combine cooked black beans and quinoa with diced pepper and cucumber. Mix yogurt with lemon juice and oil and spoon over the bowl.", {"beans":150,"quinoa":150,"yogurt":150,"pepper":120,"cucumber":100,"oil":8,"lemon":15}),
    ("edamame-quinoa-salad", "Edamame quinoa salad", "Main", "vegan", "Mix cooked edamame and quinoa with diced cucumber, tomato and avocado. Add lemon juice and chopped almonds.", {"edamame":180,"quinoa":180,"cucumber":120,"tomato":120,"avocado":50,"almond":10,"lemon":15}),
    ("yogurt-berry-almond", "Yogurt berries and almonds", "Side", "vegetarian", "Spoon yogurt into a bowl. Add berries and chopped almonds.", {"yogurt":200,"strawberry":100,"almond":20}),
    ("apple-peanut", "Apple and peanut butter", "Side", "vegan", "Slice the cored apple and serve with the weighed peanut butter.", {"apple":180,"peanut":25}),
    ("banana-walnut", "Banana and walnuts", "Side", "vegan", "Slice the banana and serve with the weighed walnuts.", {"banana":120,"walnut":25}),
    ("cottage-tomato", "Cottage cheese tomato bowl", "Side", "vegetarian", "Combine cottage cheese with chopped tomato and cucumber. Stir in the weighed olive oil.", {"cottage":200,"tomato":150,"cucumber":100,"oil":5}),
    ("edamame-carrot", "Edamame carrot bowl", "Side", "vegan", "Toss cooked shelled edamame with grated carrot and lemon juice. Serve warm or chilled.", {"edamame":180,"carrot":100,"lemon":15}),
    ("chickpea-cucumber", "Lemon chickpea side salad", "Side", "vegan", "Toss cooked chickpeas with chopped cucumber and tomato. Stir in the weighed olive oil and lemon juice.", {"chickpeas":150,"cucumber":120,"tomato":100,"oil":8,"lemon":15}),
    ("yogurt-banana-chia", "Banana chia yogurt", "Side", "vegetarian", "Mix yogurt with chia seeds and sliced banana. Let the seeds soften before serving.", {"yogurt":200,"banana":100,"chia":15}),
    ("tofu-avocado-salad", "Tofu avocado side salad", "Side", "vegan", "Dice the drained tofu and avocado. Toss with tomato, cucumber and lemon juice.", {"tofu":150,"avocado":50,"tomato":120,"cucumber":100,"lemon":15}),
    ("chicken-avocado-salad", "Chicken avocado salad", "Main", "mixed", "Slice the fully cooked chicken. Toss with avocado, chopped tomato, cucumber and spinach. Dress with the weighed olive oil and lemon juice.", {"chicken":180,"avocado":70,"tomato":100,"cucumber":100,"spinach":50,"oil":8,"lemon":15}),
    ("lemon-chicken-greens", "Lemon chicken and greens", "Main", "mixed", "Weigh the chicken and broccoli after cooking and combine while warm. Add diced pepper and spinach, then the weighed oil and lemon juice.", {"chicken":200,"broccoli":220,"pepper":80,"spinach":50,"oil":10,"lemon":15}),
    ("tuna-bean-greens", "Tuna bean salad", "Main", "mixed", "Combine drained tuna and cooked black beans with chopped tomato and cucumber. Toss with the weighed olive oil and lemon juice.", {"tuna":180,"beans":100,"tomato":150,"cucumber":100,"oil":10,"lemon":15}),
    ("salmon-vegetable-plate", "Salmon vegetable plate", "Main", "mixed", "Serve fully cooked salmon with cooked broccoli and sliced pepper. Finish with the weighed olive oil and lemon juice.", {"salmon":170,"broccoli":200,"pepper":150,"oil":10,"lemon":15}),
    ("cottage-egg-salad", "Cottage cheese egg salad", "Main", "vegetarian", "Combine cottage cheese with chopped hard-boiled egg, tomato, cucumber and spinach. Add the weighed olive oil and lemon juice.", {"cottage":200,"egg":100,"tomato":150,"cucumber":100,"spinach":50,"oil":8,"lemon":15}),
    ("tofu-edamame-greens", "Tofu edamame greens", "Main", "vegan", "Warm the tofu in the weighed oil. Add cooked edamame, cooked broccoli and spinach and heat through. Finish with lemon juice.", {"tofu":220,"edamame":160,"broccoli":150,"spinach":50,"oil":5,"lemon":15}),
    ("greek-berry-bowl", "Greek yogurt berry bowl", "Side", "vegetarian", "Spoon plain Greek yogurt into a bowl and top with chopped strawberries.", {"yogurt":300,"strawberry":100}),
    ("yogurt-cottage-bowl", "Yogurt cottage cheese bowl", "Side", "vegetarian", "Mix plain Greek yogurt and cottage cheese. Add diced cucumber and lemon juice.", {"yogurt":250,"cottage":200,"cucumber":100,"lemon":10}),
]


def build(source: Path):
    wanted = {str(v[0]) for v in FOODS.values()}
    with zipfile.ZipFile(source) as archive:
        def rows(name):
            member = next(n for n in archive.namelist() if n.endswith('/' + name))
            return csv.DictReader(io.TextIOWrapper(archive.open(member), encoding='utf-8-sig'))
        descriptions = {r['fdc_id']: r['description'] for r in rows('food.csv') if r['fdc_id'] in wanted}
        nutrients = {r['id']: r for r in rows('nutrient.csv')}
        ids = {'1008':'kcal','1003':'protein','1005':'carbs','1004':'fat','1079':'fiber'}
        for code in ids:
            assert nutrients[code]['unit_name'] == ('KCAL' if code == '1008' else 'G')
        values = {key:{} for key in wanted}
        for r in rows('food_nutrient.csv'):
            if r['fdc_id'] in wanted and r['nutrient_id'] in ids:
                values[r['fdc_id']][ids[r['nutrient_id']]] = float(r['amount'])
    ingredients = {}
    for key,(fdc,label,allergens) in FOODS.items():
        assert set(values[str(fdc)]) == set(ids.values()), (key, values[str(fdc)])
        assert all(v >= 0 for v in values[str(fdc)].values())
        ingredients[key] = dict(fdcId=fdc,name=label,usdaDescription=descriptions[str(fdc)],
            per100g=values[str(fdc)],allergens=allergens,
            sourceUrl=f'https://fdc.nal.usda.gov/food-details/{fdc}/nutrients')
    recipes = []
    for slug,title,slot,diet,instructions,amounts in RECIPES:
        totals = {n:sum(ingredients[key]['per100g'][n]*grams/100 for key,grams in amounts.items()) for n in ids.values()}
        allergens = sorted({a for key in amounts for a in ingredients[key]['allergens']})
        recipes.append(dict(id='balanced-'+slug,title=title,category='Balanced',area='',image='',
            sourceUrl='https://fdc.nal.usda.gov/',provider='balanced',diet=diet,mealType=slot,
            ingredients=[f'{grams:g} g {ingredients[key]["name"]}' for key,grams in amounts.items()],
            ingredientAmounts=[dict(food=key,grams=grams) for key,grams in amounts.items()],
            steps=[instructions, 'Quantities are for one base serving. Weigh edible ingredients in the stated cooked or raw form. Scale all ingredients together for a different serving size.'],
            allergens=allergens,nutrition=totals))
    assets = ROOT/'flagship/app/src/main/assets'
    data = dict(source='USDA FoodData Central, SR Legacy April 2018 CSV release',licence='CC0-1.0',
        sourceUrl=SOURCE_URL,sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
        basis='Ingredient estimates per 100 g edible food; one base serving per recipe. No cooking loss or nutrient-retention adjustments.',
        ingredients=ingredients)
    for name,value in [('balanced_recipes.json',recipes),('nutrition_ingredients.json',data)]:
        (assets/name).write_text(json.dumps(value,indent=2,ensure_ascii=True)+'\n',encoding='utf-8')
    print(f'Built {len(recipes)} original recipes from {len(ingredients)} source foods')


if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,required=True)
    build(parser.parse_args().source)
