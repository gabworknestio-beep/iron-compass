package com.ironcompass.planner;

public final class GoalBlocker
{
    public enum Kind { HARD_REQUIREMENT, RECOMMENDED_PREPARATION, UNKNOWN_OR_MANUAL }

    private final Kind kind;
    private final String title;
    private final String explanation;

    public GoalBlocker(Kind kind, String title, String explanation)
    {
        this.kind = kind;
        this.title = title;
        this.explanation = explanation;
    }

    public Kind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getExplanation() { return explanation; }
}
