package com.ironpath.goal;

import com.ironpath.gear.GearEvaluation;
import java.util.Collections;
import java.util.List;

public final class GoalResolution
{
    private final GearEvaluation goal;
    private final GoalAction nextAction;
    private final List<String> dependencyPath;

    public GoalResolution(GearEvaluation goal, GoalAction nextAction, List<String> dependencyPath)
    {
        this.goal = goal;
        this.nextAction = nextAction;
        this.dependencyPath = Collections.unmodifiableList(dependencyPath);
    }

    public GearEvaluation getGoal() { return goal; }
    public GoalAction getNextAction() { return nextAction; }
    public List<String> getDependencyPath() { return dependencyPath; }
}
