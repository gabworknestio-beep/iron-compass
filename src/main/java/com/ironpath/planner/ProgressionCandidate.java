package com.ironpath.planner;

import com.ironpath.gear.GearEvaluation;
import com.ironpath.goal.GoalDefinition;
import com.ironpath.route.StepEvaluation;

public final class ProgressionCandidate
{
    public enum Source { ROUTE, GEAR, GOAL }

    private final String id;
    private final String title;
    private final String reason;
    private final String unlockSummary;
    private final EffortClass effort;
    private final String impact;
    private final Source source;
    private final StepEvaluation routeStep;
    private final GearEvaluation gearStep;
    private final GoalDefinition goal;
    private final int score;

    public ProgressionCandidate(String id, String title, String reason, String unlockSummary,
                                EffortClass effort, String impact, Source source, StepEvaluation routeStep,
                                GearEvaluation gearStep, GoalDefinition goal, int score)
    {
        this.id = id;
        this.title = title;
        this.reason = reason;
        this.unlockSummary = unlockSummary;
        this.effort = effort;
        this.impact = impact;
        this.source = source;
        this.routeStep = routeStep;
        this.gearStep = gearStep;
        this.goal = goal;
        this.score = score;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getReason() { return reason; }
    public String getUnlockSummary() { return unlockSummary; }
    public EffortClass getEffort() { return effort; }
    public String getImpact() { return impact; }
    public Source getSource() { return source; }
    public StepEvaluation getRouteStep() { return routeStep; }
    public GearEvaluation getGearStep() { return gearStep; }
    public GoalDefinition getGoal() { return goal; }
    public int getScore() { return score; }
}
