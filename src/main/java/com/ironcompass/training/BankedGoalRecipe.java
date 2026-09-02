package com.ironcompass.training;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class BankedGoalRecipe
{
    private final String skill;
    private final String label;
    private final int minimumLevel;
    private final double xpPerAction;
    private final List<BankedGoalIngredient> ingredients;

    BankedGoalRecipe(String skill, String label, int minimumLevel, double xpPerAction,
                     BankedGoalIngredient... ingredients)
    {
        this.skill = skill;
        this.label = label;
        this.minimumLevel = minimumLevel;
        this.xpPerAction = xpPerAction;
        this.ingredients = Collections.unmodifiableList(Arrays.asList(ingredients));
    }

    String getSkill() { return skill; }
    String getLabel() { return label; }
    int getMinimumLevel() { return minimumLevel; }
    double getXpPerAction() { return xpPerAction; }
    List<BankedGoalIngredient> getIngredients() { return ingredients; }
    String primaryStockKey() { return ingredients.get(0).stockKey(); }
}
