package com.ironcompass.planner;

import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalStatus;

public final class GoalProximityCandidate
{
    private final GoalDefinition goal;
    private final GoalStatus status;
    private final int distance;
    private final boolean known;
    private final String summary;
    private final int valueScore;

    GoalProximityCandidate(GoalDefinition goal, GoalStatus status, int distance, boolean known,
                           String summary, int valueScore)
    {
        this.goal = goal;
        this.status = status;
        this.distance = distance;
        this.known = known;
        this.summary = summary;
        this.valueScore = valueScore;
    }

    public GoalDefinition getGoal() { return goal; }
    public GoalStatus getStatus() { return status; }
    public int getDistance() { return distance; }
    public boolean isKnown() { return known; }
    public String getSummary() { return summary; }
    int getValueScore() { return valueScore; }
}
