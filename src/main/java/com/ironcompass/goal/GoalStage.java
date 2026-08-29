package com.ironcompass.goal;

public enum GoalStage
{
    VERY_EARLY("Very early"),
    EARLY("Early"),
    EARLY_MID("Early-mid"),
    MID("Mid game"),
    MID_LATE("Mid-late"),
    LATE("Late game"),
    ENDGAME("Endgame");

    private final String label;

    GoalStage(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }
}
