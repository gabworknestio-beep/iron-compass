package com.ironcompass.gear;

public enum GearEffort
{
    SHORT(0),
    MEDIUM(6),
    LONG(14),
    VERY_LONG(24);

    private final int scorePenalty;

    GearEffort(int scorePenalty)
    {
        this.scorePenalty = scorePenalty;
    }

    public int getScorePenalty()
    {
        return scorePenalty;
    }
}
