package com.ironpath.gear;

import com.ironpath.requirement.ConditionSpec;
import java.util.Collections;
import java.util.List;

public final class GearUpgrade
{
    private String id;
    private String name;
    private EquipmentSlot slot;
    private List<CombatStyle> styles;
    private int tier;
    private List<String> previousIds;
    private List<String> alternativeIds;
    private List<String> prerequisiteIds;
    private List<String> routeStepIds;
    private ConditionSpec completion;
    private ConditionSpec requirements;
    private GearSource source;
    private GearRole role = GearRole.RECOMMENDED;
    private String importance = "NORMAL";
    private GearDifficulty difficulty = GearDifficulty.MODERATE;
    private GearEffort effort = GearEffort.MEDIUM;
    private int usefulness = 3;
    private String why;
    private String notes;
    private String wikiPage;
    private List<String> tags;
    private List<SupplySpec> supplies;

    public String getId() { return id; }
    public String getName() { return name; }
    public EquipmentSlot getSlot() { return slot == null ? EquipmentSlot.UNLOCK : slot; }
    public List<CombatStyle> getStyles() { return styles == null ? Collections.emptyList() : styles; }
    public int getTier() { return tier; }
    public List<String> getPreviousIds() { return previousIds == null ? Collections.emptyList() : previousIds; }
    public List<String> getAlternativeIds() { return alternativeIds == null ? Collections.emptyList() : alternativeIds; }
    public List<String> getPrerequisiteIds() { return prerequisiteIds == null ? Collections.emptyList() : prerequisiteIds; }
    public List<String> getRouteStepIds() { return routeStepIds == null ? Collections.emptyList() : routeStepIds; }
    public ConditionSpec getCompletion() { return completion; }
    public ConditionSpec getRequirements() { return requirements; }
    public GearSource getSource() { return source; }
    public GearRole getRole() { return role == null ? GearRole.RECOMMENDED : role; }
    public String getImportance() { return importance == null ? "NORMAL" : importance; }
    public GearDifficulty getDifficulty() { return difficulty == null ? GearDifficulty.MODERATE : difficulty; }
    public GearEffort getEffort() { return effort == null ? GearEffort.MEDIUM : effort; }
    public int getUsefulness() { return Math.max(1, Math.min(5, usefulness)); }
    public String getWhy() { return why; }
    public String getNotes() { return notes; }
    public String getWikiPage() { return wikiPage; }
    public List<String> getTags() { return tags == null ? Collections.emptyList() : tags; }
    public List<SupplySpec> getSupplies() { return supplies == null ? Collections.emptyList() : supplies; }
}
