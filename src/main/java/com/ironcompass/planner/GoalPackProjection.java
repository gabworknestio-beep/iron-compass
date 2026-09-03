package com.ironcompass.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GoalPackProjection
{
    public enum Status { READY, CLOSE, BUILDING, UNKNOWN }

    private final String id;
    private final String title;
    private final String summary;
    private final Status status;
    private final int completeCount;
    private final int totalCount;
    private final List<GoalPathNode> goals;
    private final List<GoalBlocker> blockers;

    public GoalPackProjection(String id, String title, String summary, Status status, int completeCount,
                              int totalCount, List<GoalPathNode> goals, List<GoalBlocker> blockers)
    {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.status = status;
        this.completeCount = completeCount;
        this.totalCount = totalCount;
        this.goals = immutable(goals);
        this.blockers = immutable(blockers);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public Status getStatus() { return status; }
    public int getCompleteCount() { return completeCount; }
    public int getTotalCount() { return totalCount; }
    public List<GoalPathNode> getGoals() { return goals; }
    public List<GoalBlocker> getBlockers() { return blockers; }

    private static <T> List<T> immutable(List<T> values)
    {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
