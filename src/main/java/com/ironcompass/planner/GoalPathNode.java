package com.ironcompass.planner;

import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalStatus;

public final class GoalPathNode
{
    private final GoalDefinition goal;
    private final GoalStatus status;

    GoalPathNode(GoalDefinition goal, GoalStatus status)
    {
        this.goal = goal;
        this.status = status;
    }

    public GoalDefinition getGoal() { return goal; }
    public GoalStatus getStatus() { return status; }
    public boolean isRng() { return goal.isRng(); }
    public boolean isManual() { return goal.getCompletionMode() != com.ironcompass.goal.GoalCompletionMode.AUTO; }
}
