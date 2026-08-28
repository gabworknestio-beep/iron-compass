package com.ironcompass.gear;

public enum GearDifficulty
{
    LOW(0),
    MODERATE(4),
    HIGH(9),
    EXPERT(15);

    private final int scorePenalty;

    GearDifficulty(int scorePenalty)
    {
        this.scorePenalty = scorePenalty;
    }

    public int getScorePenalty()
    {
        return scorePenalty;
    }
}
