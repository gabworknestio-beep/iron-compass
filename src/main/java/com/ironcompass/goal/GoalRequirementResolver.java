package com.ironcompass.goal;

import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.requirement.ConditionSpec;

/** Single source of truth for a goal's authored or linked Gear requirements. */
public final class GoalRequirementResolver
{
    private GoalRequirementResolver() { }

    public static ConditionSpec effectiveRequirements(GoalDefinition goal, GearProjection gear)
    {
        if (goal == null) return null;
        if (goal.getRequirements() != null) return goal.getRequirements();
        GearEvaluation linked = linkedGear(goal, gear);
        return linked == null ? null : linked.getUpgrade().getRequirements();
    }

    public static GearEvaluation linkedGear(GoalDefinition goal, GearProjection gear)
    {
        return goal == null || gear == null || goal.getGearId() == null ? null : gear.find(goal.getGearId());
    }
}
