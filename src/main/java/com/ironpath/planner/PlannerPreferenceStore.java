package com.ironpath.planner;

public interface PlannerPreferenceStore
{
    Playstyle getPlaystyle();
    void setPlaystyle(Playstyle playstyle);
    boolean isAvoidWilderness();
    void setAvoidWilderness(boolean avoid);
    SessionLength getSessionLength();
    void setSessionLength(SessionLength sessionLength);
}
