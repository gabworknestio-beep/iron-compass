package com.ironpath.goal;

import com.ironpath.planner.EffortClass;
import com.ironpath.requirement.ConditionSpec;
import java.util.Collections;
import java.util.List;

public final class GoalDefinition
{
    private String id;
    private String title;
    private String description;
    private String category;
    private ConditionSpec completion;
    private ConditionSpec requirements;
    private List<String> dependencyIds;
    private String routeAnchorId;
    private String gearId;
    private GoalImpact impact = GoalImpact.HIGH;
    private EffortClass effort = EffortClass.MEDIUM;
    private List<String> unlocks;
    private String wikiPage;
    private List<String> tags;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public ConditionSpec getCompletion() { return completion; }
    public ConditionSpec getRequirements() { return requirements; }
    public List<String> getDependencyIds()
    {
        return dependencyIds == null ? Collections.emptyList() : dependencyIds;
    }
    public String getRouteAnchorId() { return routeAnchorId; }
    public String getGearId() { return gearId; }
    public GoalImpact getImpact() { return impact; }
    public EffortClass getEffort() { return effort; }
    public List<String> getUnlocks() { return unlocks == null ? Collections.emptyList() : unlocks; }
    public String getWikiPage() { return wikiPage; }
    public List<String> getTags() { return tags == null ? Collections.emptyList() : tags; }
}
