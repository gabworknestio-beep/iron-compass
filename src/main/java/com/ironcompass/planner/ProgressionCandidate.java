package com.ironcompass.planner;

import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.route.StepEvaluation;
import java.util.Collections;
import java.util.List;

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
    private final List<String> whyLines;
    private final List<String> advancedGoals;

    public ProgressionCandidate(String id, String title, String reason, String unlockSummary,
                                EffortClass effort, String impact, Source source, StepEvaluation routeStep,
                                GearEvaluation gearStep, GoalDefinition goal, int score)
    {
        this(id, title, reason, unlockSummary, effort, impact, source, routeStep, gearStep, goal, score,
            Collections.singletonList(reason), Collections.emptyList());
    }

    public ProgressionCandidate(String id, String title, String reason, String unlockSummary,
                                EffortClass effort, String impact, Source source, StepEvaluation routeStep,
                                GearEvaluation gearStep, GoalDefinition goal, int score,
                                List<String> whyLines, List<String> advancedGoals)
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
        this.whyLines = Collections.unmodifiableList(whyLines);
        this.advancedGoals = Collections.unmodifiableList(advancedGoals);
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
    public List<String> getWhyLines() { return whyLines; }
    public List<String> getAdvancedGoals() { return advancedGoals; }
    public int getActiveGoalCount() { return advancedGoals.size(); }
}
