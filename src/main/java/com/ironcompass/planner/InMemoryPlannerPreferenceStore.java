package com.ironcompass.planner;

public final class InMemoryPlannerPreferenceStore implements PlannerPreferenceStore
{
    private Playstyle playstyle = Playstyle.BALANCED;
    private boolean avoidWilderness;
    private SessionLength sessionLength = SessionLength.ANY;

    @Override public Playstyle getPlaystyle() { return playstyle; }
    @Override public void setPlaystyle(Playstyle value) { playstyle = value == null ? Playstyle.BALANCED : value; }
    @Override public boolean isAvoidWilderness() { return avoidWilderness; }
    @Override public void setAvoidWilderness(boolean value) { avoidWilderness = value; }
    @Override public SessionLength getSessionLength() { return sessionLength; }
    @Override public void setSessionLength(SessionLength value)
    {
        sessionLength = value == null ? SessionLength.ANY : value;
    }
}
