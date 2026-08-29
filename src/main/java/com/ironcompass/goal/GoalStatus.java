package com.ironcompass.goal;

public enum GoalStatus
{
    COMPLETE_AUTO("Complete · detected"),
    COMPLETE_MANUAL("Complete · manual"),
    INCOMPLETE_AUTO("Incomplete · detected"),
    INCOMPLETE_MANUAL("Incomplete · manual"),
    READY("Ready"),
    LOCKED("Locked"),
    UNKNOWN("Unknown");

    private final String label;

    GoalStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
