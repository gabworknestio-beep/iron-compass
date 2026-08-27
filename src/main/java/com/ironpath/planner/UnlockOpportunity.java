package com.ironpath.planner;

public final class UnlockOpportunity
{
    private final String id;
    private final String title;
    private final String explanation;

    public UnlockOpportunity(String id, String title, String explanation)
    {
        this.id = id;
        this.title = title;
        this.explanation = explanation;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getExplanation() { return explanation; }
}
