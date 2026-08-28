package com.ironcompass.ui;

import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GoalPickerModel
{
    public static final String SUGGESTED = "Suggested for you";
    public static final String ACTIVE = "Active";
    public static final String ALL = "All";
    public static final String COMPLETED = "Completed";
    private final ConditionEvaluator conditions = new ConditionEvaluator();

    public List<String> categories(GoalCatalog catalog)
    {
        Set<String> values = new LinkedHashSet<>();
        values.add(SUGGESTED);
        values.add(ACTIVE);
        values.add(ALL);
        for (GoalDefinition goal : catalog.getGoals()) values.add(goal.getCategory());
        values.add(COMPLETED);
        return new ArrayList<>(values);
    }

    public List<GoalDefinition> filter(GoalCatalog catalog, String query, String category, Set<String> active,
                                       AccountState state, GearProjection gear)
    {
        String search = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        String selectedCategory = category == null ? SUGGESTED : category;
        List<ScoredGoal> matched = new ArrayList<>();
        for (GoalDefinition goal : catalog.getGoals())
        {
            boolean complete = isComplete(goal, state, gear);
            if (!search.isEmpty() && !(goal.getTitle() + " " + goal.getCategory() + " "
                + String.join(" ", goal.getTags())).toLowerCase(Locale.ENGLISH).contains(search)) continue;
            if (ACTIVE.equals(selectedCategory) && !active.contains(goal.getId())) continue;
            if (COMPLETED.equals(selectedCategory) && !complete) continue;
            if (!SUGGESTED.equals(selectedCategory) && !ACTIVE.equals(selectedCategory)
                && !ALL.equals(selectedCategory) && !COMPLETED.equals(selectedCategory)
                && !selectedCategory.equals(goal.getCategory())) continue;
            if (!COMPLETED.equals(selectedCategory) && !ACTIVE.equals(selectedCategory) && complete) continue;
            int score = (goal.getImpact().name().equals("MAJOR") ? 60
                : goal.getImpact().name().equals("HIGH") ? 35 : 15) - distance(goal.getRequirements(), state);
            if (active.contains(goal.getId())) score += 200;
            matched.add(new ScoredGoal(goal, score));
        }
        matched.sort(Comparator.comparingInt(ScoredGoal::getScore).reversed()
            .thenComparing(value -> value.goal.getTitle()));
        int limit = SUGGESTED.equals(selectedCategory) && search.isEmpty() ? 8 : Integer.MAX_VALUE;
        List<GoalDefinition> result = new ArrayList<>();
        for (ScoredGoal value : matched)
        {
            if (result.size() == limit) break;
            result.add(value.goal);
        }
        return result;
    }

    private boolean isComplete(GoalDefinition goal, AccountState state, GearProjection gear)
    {
        if (goal.getGearId() != null && gear != null)
        {
            GearEvaluation evaluation = gear.find(goal.getGearId());
            return evaluation != null && evaluation.getCompletion() == TruthValue.TRUE;
        }
        return conditions.evaluate(goal.getCompletion(), state).getValue() == TruthValue.TRUE;
    }

    private static int distance(ConditionSpec condition, AccountState state)
    {
        if (condition == null) return 300;
        if ("SKILL_AT_LEAST".equalsIgnoreCase(condition.getType()))
            return Math.max(0, condition.getLevel() - state.skillLevel(condition.getSkill()));
        if ("ALL".equalsIgnoreCase(condition.getType()) || "ANY".equalsIgnoreCase(condition.getType()))
        {
            int total = 0;
            for (ConditionSpec child : condition.getChildren()) total += Math.min(100, distance(child, state));
            return total;
        }
        if ("QUEST_STATE".equalsIgnoreCase(condition.getType())) return 80;
        return 150;
    }

    private static final class ScoredGoal
    {
        private final GoalDefinition goal;
        private final int score;
        private ScoredGoal(GoalDefinition goal, int score) { this.goal = goal; this.score = score; }
        private int getScore() { return score; }
    }
}
