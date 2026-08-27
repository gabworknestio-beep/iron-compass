package com.ironpath.planner;

public enum EffortClass
{
    QUICK,
    SHORT,
    MEDIUM,
    LONG,
    VERY_LONG;

    public boolean fits(SessionLength session)
    {
        return ordinal() <= session.getMaximumEffort().ordinal();
    }
}
