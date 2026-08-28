package com.ironcompass.planner;

import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.requirement.RequirementResult;
import com.ironcompass.requirement.TruthValue;
import java.util.HashSet;
import java.util.Set;

public final class UnlockRadarService
{
    private Set<String> availableGear;
    private Set<String> completedGoalRequirements;
    private String selectedGoalId;

    public UnlockOpportunity evaluate(GearProjection gear, GoalPlanProjection goal)
    {
        Set<String> currentGear = availableGear(gear);
        Set<String> currentRequirements = completedRequirements(goal);
        String currentGoalId = goal == null ? null : goal.getGoalId();
        if (availableGear == null)
        {
            availableGear = currentGear;
            completedGoalRequirements = currentRequirements;
            selectedGoalId = currentGoalId;
            return null;
        }

        UnlockOpportunity opportunity = null;
        for (GearEvaluation evaluation : gear == null ? java.util.Collections.<GearEvaluation>emptyList()
            : gear.getEvaluations())
        {
            String id = evaluation.getUpgrade().getId();
            if (currentGear.contains(id) && !availableGear.contains(id))
            {
                opportunity = new UnlockOpportunity("gear:" + id,
                    evaluation.getUpgrade().getName() + " available",
                    "Your account now meets the known readiness requirements for this gear objective.");
                break;
            }
        }
        if (opportunity == null && same(selectedGoalId, currentGoalId) && goal != null)
        {
            for (RequirementResult result : goal.getProgress())
            {
                String key = currentGoalId + ":" + result.getLabel();
                if (currentRequirements.contains(key) && !completedGoalRequirements.contains(key))
                {
                    opportunity = new UnlockOpportunity("goal:" + key,
                        result.getLabel() + " completed",
                        "This removes one requirement from your selected goal.");
                    break;
                }
            }
        }

        availableGear = currentGear;
        completedGoalRequirements = currentRequirements;
        selectedGoalId = currentGoalId;
        return opportunity;
    }

    public void reset()
    {
        availableGear = null;
        completedGoalRequirements = null;
        selectedGoalId = null;
    }

    private static Set<String> availableGear(GearProjection gear)
    {
        Set<String> values = new HashSet<>();
        if (gear != null)
        {
            for (GearEvaluation evaluation : gear.getEvaluations())
            {
                if (evaluation.getStatus() == GearStatus.AVAILABLE
                    || evaluation.getStatus() == GearStatus.RECOMMENDED)
                    values.add(evaluation.getUpgrade().getId());
            }
        }
        return values;
    }

    private static Set<String> completedRequirements(GoalPlanProjection goal)
    {
        Set<String> values = new HashSet<>();
        if (goal != null && goal.hasSelectedGoal())
        {
            for (RequirementResult result : goal.getProgress())
                if (result.getValue() == TruthValue.TRUE)
                    values.add(goal.getGoalId() + ":" + result.getLabel());
        }
        return values;
    }

    private static boolean same(String first, String second)
    {
        return first == null ? second == null : first.equals(second);
    }
}
