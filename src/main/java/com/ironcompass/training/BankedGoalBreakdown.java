package com.ironcompass.training;

public final class BankedGoalBreakdown
{
    private final String label;
    private final int actions;
    private final int experience;

    BankedGoalBreakdown(String label, int actions, int experience)
    {
        this.label = label;
        this.actions = actions;
        this.experience = experience;
    }

    public String getLabel() { return label; }
    public int getActions() { return actions; }
    public int getExperience() { return experience; }
}
