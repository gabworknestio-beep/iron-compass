package com.ironpath.planner;

public enum SessionLength
{
    ANY("Any session", EffortClass.VERY_LONG),
    FIFTEEN_MINUTES("15 min", EffortClass.QUICK),
    THIRTY_MINUTES("30 min", EffortClass.SHORT),
    ONE_HOUR("1 hour", EffortClass.MEDIUM),
    TWO_HOURS_PLUS("2h+", EffortClass.LONG);

    private final String label;
    private final EffortClass maximumEffort;

    SessionLength(String label, EffortClass maximumEffort)
    {
        this.label = label;
        this.maximumEffort = maximumEffort;
    }

    public String getLabel() { return label; }
    public EffortClass getMaximumEffort() { return maximumEffort; }
}
