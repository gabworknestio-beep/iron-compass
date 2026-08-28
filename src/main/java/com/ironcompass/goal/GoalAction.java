package com.ironcompass.goal;

import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.route.StepEvaluation;

public final class GoalAction
{
    public enum Kind
    {
        ROUTE_STEP,
        REQUIREMENT,
        GEAR_UPGRADE,
        COMPLETE
    }

    private final Kind kind;
    private final String title;
    private final String explanation;
    private final StepEvaluation routeStep;
    private final GearEvaluation gearStep;

    public GoalAction(Kind kind, String title, String explanation, StepEvaluation routeStep,
                      GearEvaluation gearStep)
    {
        this.kind = kind;
        this.title = title;
        this.explanation = explanation;
        this.routeStep = routeStep;
        this.gearStep = gearStep;
    }

    public Kind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getExplanation() { return explanation; }
    public StepEvaluation getRouteStep() { return routeStep; }
    public GearEvaluation getGearStep() { return gearStep; }
}
