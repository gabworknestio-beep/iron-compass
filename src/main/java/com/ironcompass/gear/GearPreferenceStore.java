package com.ironcompass.gear;

public interface GearPreferenceStore
{
    String getSelectedGoalId();
    void setSelectedGoalId(String goalId);
    boolean isSkipped(String goalId);
    void setSkipped(String goalId, boolean skipped);
    boolean isMarkedOptional(String goalId);
    void setMarkedOptional(String goalId, boolean optional);
    String getChosenAlternative(String goalId);
    void chooseAlternative(String goalId, String alternativeId);
    String getGearStyleFilter();
    void setGearStyleFilter(String style);
    String getGearStatusFilter();
    void setGearStatusFilter(String status);
    void resetGearPreferences();
}
