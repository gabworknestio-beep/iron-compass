package com.ironcompass.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable account-aware projection for one skill and one target. */
public final class SkillTrainingPlan
{
    private final String skill;
    private final int currentLevel;
    private final int targetLevel;
    private final int xpRemaining;
    private final String estimatedTime;
    private final List<TrainingPlanSegment> segments;
    private final List<TrainingMilestone> milestones;

    public SkillTrainingPlan(String skill, int currentLevel, int targetLevel, int xpRemaining,
                             String estimatedTime, List<TrainingPlanSegment> segments,
                             List<TrainingMilestone> milestones)
    {
        this.skill = skill;
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
        this.xpRemaining = xpRemaining;
        this.estimatedTime = estimatedTime;
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
        this.milestones = Collections.unmodifiableList(new ArrayList<>(milestones));
    }

    public String getSkill() { return skill; }
    public int getCurrentLevel() { return currentLevel; }
    public int getTargetLevel() { return targetLevel; }
    public int getXpRemaining() { return xpRemaining; }
    public String getEstimatedTime() { return estimatedTime; }
    public List<TrainingPlanSegment> getSegments() { return segments; }
    public List<TrainingMilestone> getMilestones() { return milestones; }
    public boolean isComplete() { return currentLevel >= targetLevel; }
    public boolean isFullGuide() { return currentLevel == 1 && targetLevel == 99; }
    public MethodRecommendation getFirstRecommendation()
    {
        return segments.isEmpty() ? null : segments.get(0).getRecommendation();
    }
}
