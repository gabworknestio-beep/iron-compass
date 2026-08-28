package com.ironcompass.planner;

import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.route.StepEvaluation;

public final class PlannedAction
{
    public enum Kind { COMPLETE, REQUIREMENT, ROUTE_STEP, GEAR }

    private final Kind kind;
    private final String title;
    private final String explanation;
    private final StepEvaluation routeStep;
    private final GearEvaluation gearStep;
    private final String skill;
    private final int targetLevel;
    private final EffortClass effort;

    public PlannedAction(Kind kind, String title, String explanation, StepEvaluation routeStep,
                         GearEvaluation gearStep)
    {
        this(kind, title, explanation, routeStep, gearStep, null, 0, null);
    }

    public PlannedAction(Kind kind, String title, String explanation, StepEvaluation routeStep,
                         GearEvaluation gearStep, String skill, int targetLevel)
    {
        this(kind, title, explanation, routeStep, gearStep, skill, targetLevel, null);
    }

    public PlannedAction(Kind kind, String title, String explanation, StepEvaluation routeStep,
                         GearEvaluation gearStep, String skill, int targetLevel, EffortClass effort)
    {
        this.kind = kind;
        this.title = title;
        this.explanation = explanation;
        this.routeStep = routeStep;
        this.gearStep = gearStep;
        this.skill = skill;
        this.targetLevel = targetLevel;
        this.effort = effort;
    }

    public Kind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getExplanation() { return explanation; }
    public StepEvaluation getRouteStep() { return routeStep; }
    public GearEvaluation getGearStep() { return gearStep; }
    public String getSkill() { return skill; }
    public int getTargetLevel() { return targetLevel; }
    public EffortClass getEffort() { return effort; }

    public String stableKey()
    {
        if (skill != null && targetLevel > 0) return "skill:" + skill.toLowerCase(java.util.Locale.ENGLISH)
            + ":" + targetLevel;
        if (routeStep != null) return routeStep.getStep().getId();
        if (gearStep != null) return gearStep.getUpgrade().getId();
        return kind.name().toLowerCase(java.util.Locale.ENGLISH) + ":"
            + title.toLowerCase(java.util.Locale.ENGLISH).replaceAll("[^a-z0-9]+", "-");
    }
}
