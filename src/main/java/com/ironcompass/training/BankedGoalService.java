package com.ironcompass.training;

import com.ironcompass.state.AccountState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Converts an observed bank into a conservative, local estimate toward one selected skill target. */
public final class BankedGoalService
{
    public BankedGoalProjection project(AccountState state, String skill, int targetLevel)
    {
        if (state == null || skill == null) return unsupported(skill, 0, targetLevel);
        int currentLevel = Math.max(1, state.skillLevel(skill));
        int target = Math.max(currentLevel, Math.min(99, targetLevel));
        int currentXp = state.skillExperience(skill);
        if (currentXp < 0) currentXp = MethodPlannerService.xpForLevel(currentLevel);
        int targetXp = MethodPlannerService.xpForLevel(target);
        int remaining = Math.max(0, targetXp - currentXp);

        List<BankedGoalRecipe> skillRecipes = recipesFor(skill);
        if (remaining == 0)
            return projection(BankedGoalStatus.COMPLETE, skill, currentLevel, target, 0, 0,
                currentLevel, 100, Collections.emptyList(), "This target is already complete.");
        if (skillRecipes.isEmpty()) return unsupported(skill, currentLevel, target);
        if (!state.getBank().isObserved())
            return projection(BankedGoalStatus.UNKNOWN, skill, currentLevel, target, remaining, 0,
                currentLevel, 0, Collections.emptyList(),
                "Open your bank once this session to calculate recognized resources toward this target.");

        Map<Integer, Integer> stock = observedStock(state, skillRecipes);
        List<BankedGoalRecipe> selected = bestCurrentlyUnlockedRecipes(skillRecipes, currentLevel);
        List<BankedGoalBreakdown> breakdown = new ArrayList<>();
        double recognized = 0.0;
        for (BankedGoalRecipe recipe : selected)
        {
            int actions = availableActions(recipe, stock);
            if (actions <= 0) continue;
            consume(recipe, stock, actions);
            int experience = (int) Math.floor(actions * recipe.getXpPerAction());
            if (experience <= 0) continue;
            recognized += actions * recipe.getXpPerAction();
            breakdown.add(new BankedGoalBreakdown(recipe.getLabel(), actions, experience));
        }
        breakdown.sort(Comparator.comparingInt(BankedGoalBreakdown::getExperience).reversed()
            .thenComparing(BankedGoalBreakdown::getLabel));
        int recognizedXp = (int) Math.floor(recognized);
        if (recognizedXp <= 0)
            return projection(BankedGoalStatus.NO_RESOURCES, skill, currentLevel, target, remaining, 0,
                currentLevel, 0, Collections.emptyList(),
                "No currently usable resources from the curated " + skill + " conversions were found.");

        int projectedLevel = levelForXp(Math.min(MethodPlannerService.xpForLevel(99), currentXp + recognizedXp));
        int percent = Math.min(100, (int) Math.round(recognizedXp * 100.0 / remaining));
        BankedGoalStatus status = recognizedXp >= remaining ? BankedGoalStatus.READY
            : BankedGoalStatus.IN_PROGRESS;
        return projection(status, skill, currentLevel, target, remaining, recognizedXp, projectedLevel,
            percent, breakdown, explanation(skill));
    }

    public boolean supports(String skill)
    {
        return !recipesFor(skill).isEmpty();
    }

    private static List<BankedGoalRecipe> recipesFor(String skill)
    {
        List<BankedGoalRecipe> result = new ArrayList<>();
        for (BankedGoalRecipe recipe : BankedGoalRecipes.all())
            if (recipe.getSkill().equalsIgnoreCase(skill)) result.add(recipe);
        return result;
    }

    private static Map<Integer, Integer> observedStock(AccountState state, List<BankedGoalRecipe> recipes)
    {
        Map<Integer, Integer> result = new HashMap<>();
        for (BankedGoalRecipe recipe : recipes)
            for (BankedGoalIngredient ingredient : recipe.getIngredients())
                for (int itemId : ingredient.getItemIds())
                    result.putIfAbsent(itemId, state.exactCarriedQuantity(itemId)
                        + state.getBank().exactQuantity(itemId));
        return result;
    }

    private static List<BankedGoalRecipe> bestCurrentlyUnlockedRecipes(List<BankedGoalRecipe> recipes,
                                                                        int currentLevel)
    {
        Map<String, BankedGoalRecipe> bestByPrimaryResource = new LinkedHashMap<>();
        for (BankedGoalRecipe recipe : recipes)
        {
            if (recipe.getMinimumLevel() > currentLevel) continue;
            BankedGoalRecipe current = bestByPrimaryResource.get(recipe.primaryStockKey());
            if (current == null || recipe.getXpPerAction() > current.getXpPerAction())
                bestByPrimaryResource.put(recipe.primaryStockKey(), recipe);
        }
        List<BankedGoalRecipe> selected = new ArrayList<>(bestByPrimaryResource.values());
        selected.sort(Comparator.comparingDouble(BankedGoalRecipe::getXpPerAction).reversed()
            .thenComparing(BankedGoalRecipe::getLabel));
        return selected;
    }

    private static int availableActions(BankedGoalRecipe recipe, Map<Integer, Integer> stock)
    {
        int actions = Integer.MAX_VALUE;
        for (BankedGoalIngredient ingredient : recipe.getIngredients())
        {
            long available = 0;
            for (int itemId : ingredient.getItemIds()) available += stock.getOrDefault(itemId, 0);
            actions = Math.min(actions, (int) Math.min(Integer.MAX_VALUE,
                available / ingredient.getUnitsPerAction()));
        }
        return actions == Integer.MAX_VALUE ? 0 : actions;
    }

    private static void consume(BankedGoalRecipe recipe, Map<Integer, Integer> stock, int actions)
    {
        for (BankedGoalIngredient ingredient : recipe.getIngredients())
        {
            int needed = actions * ingredient.getUnitsPerAction();
            for (int itemId : ingredient.getItemIds())
            {
                int available = stock.getOrDefault(itemId, 0);
                int used = Math.min(available, needed);
                stock.put(itemId, available - used);
                needed -= used;
                if (needed == 0) break;
            }
        }
    }

    private static int levelForXp(int experience)
    {
        int level = 1;
        for (int candidate = 2; candidate <= 99; candidate++)
        {
            if (MethodPlannerService.xpForLevel(candidate) > experience) break;
            level = candidate;
        }
        return level;
    }

    private static String explanation(String skill)
    {
        switch (skill.toLowerCase(Locale.ENGLISH))
        {
            case "herblore":
                return "Estimate uses recognized herb/potion inputs and observed secondaries; vials, water and alternate potion choices can change the result.";
            case "prayer":
                return "Estimate uses the safe Ectofuntus rate for bones and blessed-wine rates for shards; slime, pots, wine and travel are not counted.";
            case "cooking":
                return "Estimate assumes successful cooks. Burns and recipe choices can reduce the result.";
            case "farming":
                return "Estimate uses planting plus health-check XP for observed saplings. Survival, protection payments and patch availability are not guaranteed.";
            case "crafting":
                return "Estimate chooses the highest-XP curated use currently unlocked for each recognized material.";
            case "fletching":
                return "Estimate chooses the highest-XP curated unstrung product currently unlocked for each observed log type.";
            case "construction":
                return "Estimate uses standard product XP from observed planks; coins, logs and Mahogany Homes bonuses are excluded.";
            case "smithing":
                return "Estimate uses anvil XP from observed bars. Ores, coal allocation and Giants' Foundry are excluded.";
            default:
                return "Estimate uses recognized items and common conversions available at your current level.";
        }
    }

    private static BankedGoalProjection unsupported(String skill, int currentLevel, int targetLevel)
    {
        return projection(BankedGoalStatus.NOT_SUPPORTED, skill == null ? "" : skill, currentLevel,
            targetLevel, 0, 0, currentLevel, 0, Collections.emptyList(),
            "This skill is driven by activity, combat or unbanked inputs, so Iron Compass does not invent a banked-XP total.");
    }

    private static BankedGoalProjection projection(BankedGoalStatus status, String skill, int currentLevel,
                                                    int targetLevel, int remaining, int recognized,
                                                    int projectedLevel, int percent,
                                                    List<BankedGoalBreakdown> breakdown, String explanation)
    {
        return new BankedGoalProjection(status, skill, currentLevel, targetLevel, remaining, recognized,
            projectedLevel, percent, breakdown, explanation);
    }
}
