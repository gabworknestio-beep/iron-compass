package com.ironcompass.training;

public final class TrainingPlanSegment
{
    private final int fromLevel;
    private final int toLevel;
    private final MethodRecommendation recommendation;

    public TrainingPlanSegment(int fromLevel, int toLevel, MethodRecommendation recommendation)
    {
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
        this.recommendation = recommendation;
    }

    public int getFromLevel() { return fromLevel; }
    public int getToLevel() { return toLevel; }
    public MethodRecommendation getRecommendation() { return recommendation; }
}
