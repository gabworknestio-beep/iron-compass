package com.ironpath.gear;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InMemoryGearPreferenceStore implements GearPreferenceStore
{
    private String selected;
    private final Set<String> skipped = new HashSet<>();
    private final Set<String> optional = new HashSet<>();
    private final Map<String, String> alternatives = new HashMap<>();
    private String style = "ALL";
    private String status = "ALL";

    @Override public String getSelectedGoalId() { return selected; }
    @Override public void setSelectedGoalId(String goalId) { selected = goalId; }
    @Override public boolean isSkipped(String goalId) { return skipped.contains(goalId); }
    @Override public void setSkipped(String goalId, boolean value)
    {
        if (value)
        {
            skipped.add(goalId);
            if (goalId != null && goalId.equals(selected)) selected = null;
        }
        else skipped.remove(goalId);
    }
    @Override public boolean isMarkedOptional(String goalId) { return optional.contains(goalId); }
    @Override public void setMarkedOptional(String goalId, boolean value) { if (value) optional.add(goalId); else optional.remove(goalId); }
    @Override public String getChosenAlternative(String goalId) { return alternatives.get(goalId); }
    @Override public void chooseAlternative(String goalId, String alternativeId) { if (alternativeId == null) alternatives.remove(goalId); else alternatives.put(goalId, alternativeId); }
    @Override public String getGearStyleFilter() { return style; }
    @Override public void setGearStyleFilter(String value) { style = value; }
    @Override public String getGearStatusFilter() { return status; }
    @Override public void setGearStatusFilter(String value) { status = value; }
    @Override public void resetGearPreferences() { selected = null; skipped.clear(); optional.clear(); alternatives.clear(); style = "ALL"; status = "ALL"; }
}
