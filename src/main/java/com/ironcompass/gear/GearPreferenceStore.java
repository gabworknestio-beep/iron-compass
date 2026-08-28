package com.ironcompass.gear;

import java.util.Collections;
import java.util.List;

public interface GearPreferenceStore
{
    String getSelectedGoalId();
    void setSelectedGoalId(String goalId);
    default String getPrimaryGoalId() { return getSelectedGoalId(); }
    default void setPrimaryGoalId(String goalId) { setSelectedGoalId(goalId); }
    default List<String> getSecondaryGoalIds() { return Collections.emptyList(); }
    default boolean addSecondaryGoalId(String goalId) { return false; }
    default void removeSecondaryGoalId(String goalId) { }
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
