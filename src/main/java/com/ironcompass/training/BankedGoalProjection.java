package com.ironcompass.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BankedGoalProjection
{
    private final BankedGoalStatus status;
    private final String skill;
    private final int currentLevel;
    private final int targetLevel;
    private final int xpRemaining;
    private final int recognizedXp;
    private final int projectedLevel;
    private final int progressPercent;
    private final List<BankedGoalBreakdown> breakdown;
    private final String explanation;

    BankedGoalProjection(BankedGoalStatus status, String skill, int currentLevel, int targetLevel,
                         int xpRemaining, int recognizedXp, int projectedLevel, int progressPercent,
                         List<BankedGoalBreakdown> breakdown, String explanation)
    {
        this.status = status;
        this.skill = skill;
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
        this.xpRemaining = xpRemaining;
        this.recognizedXp = recognizedXp;
        this.projectedLevel = projectedLevel;
        this.progressPercent = progressPercent;
        this.breakdown = Collections.unmodifiableList(new ArrayList<>(breakdown));
        this.explanation = explanation;
    }

    public BankedGoalStatus getStatus() { return status; }
    public String getSkill() { return skill; }
    public int getCurrentLevel() { return currentLevel; }
    public int getTargetLevel() { return targetLevel; }
    public int getXpRemaining() { return xpRemaining; }
    public int getRecognizedXp() { return recognizedXp; }
    public int getProjectedLevel() { return projectedLevel; }
    public int getProgressPercent() { return progressPercent; }
    public List<BankedGoalBreakdown> getBreakdown() { return breakdown; }
    public String getExplanation() { return explanation; }
}
