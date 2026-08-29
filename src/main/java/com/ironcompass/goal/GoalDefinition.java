package com.ironcompass.goal;

import com.ironcompass.planner.EffortClass;
import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.route.RiskLevel;
import java.util.Collections;
import java.util.List;

public final class GoalDefinition
{
    private String id;
    private String title;
    private String description;
    private String whyItMatters;
    private String category;
    private GoalStage stage = GoalStage.MID;
    private ConditionSpec completion;
    private ConditionSpec requirements;
    private GoalCompletionMode completionMode = GoalCompletionMode.AUTO;
    private GoalPriority priority = GoalPriority.RECOMMENDED;
    private GoalCommunityWeight communityWeight = GoalCommunityWeight.NOTABLE;
    private List<GoalIntent> intents;
    private List<GoalRelationship> relationships;
    private List<String> dependencyIds;
    private String routeAnchorId;
    private String gearId;
    private GoalImpact impact = GoalImpact.HIGH;
    private EffortClass effort = EffortClass.MEDIUM;
    private List<String> unlocks;
    private List<String> benefits;
    private List<String> relatedItems;
    private List<String> relatedSkills;
    private List<String> relatedQuests;
    private List<String> relatedActivities;
    private List<String> accountTypes;
    private List<String> sourceReferences;
    private RiskLevel riskLevel = RiskLevel.SAFE;
    private int usefulness = 3;
    private boolean popular;
    private boolean rng;
    private String wikiPage;
    private List<String> tags;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getWhyItMatters() { return whyItMatters == null ? description : whyItMatters; }
    public String getCategory() { return category; }
    public GoalStage getStage() { return stage; }
    public ConditionSpec getCompletion() { return completion; }
    public ConditionSpec getRequirements() { return requirements; }
    public GoalCompletionMode getCompletionMode() { return completionMode; }
    public GoalPriority getPriority() { return priority; }
    public GoalCommunityWeight getCommunityWeight() { return communityWeight; }
    public List<GoalIntent> getIntents() { return intents == null ? Collections.emptyList() : intents; }
    public List<GoalRelationship> getRelationships()
    {
        return relationships == null ? Collections.emptyList() : relationships;
    }
    public List<String> getDependencyIds()
    {
        return dependencyIds == null ? Collections.emptyList() : dependencyIds;
    }
    public String getRouteAnchorId() { return routeAnchorId; }
    public String getGearId() { return gearId; }
    public GoalImpact getImpact() { return impact; }
    public EffortClass getEffort() { return effort; }
    public List<String> getUnlocks() { return unlocks == null ? Collections.emptyList() : unlocks; }
    public List<String> getBenefits() { return benefits == null ? Collections.emptyList() : benefits; }
    public List<String> getRelatedItems() { return relatedItems == null ? Collections.emptyList() : relatedItems; }
    public List<String> getRelatedSkills() { return relatedSkills == null ? Collections.emptyList() : relatedSkills; }
    public List<String> getRelatedQuests() { return relatedQuests == null ? Collections.emptyList() : relatedQuests; }
    public List<String> getRelatedActivities() { return relatedActivities == null ? Collections.emptyList() : relatedActivities; }
    public List<String> getAccountTypes() { return accountTypes == null ? Collections.emptyList() : accountTypes; }
    public List<String> getSourceReferences() { return sourceReferences == null ? Collections.emptyList() : sourceReferences; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public int getUsefulness() { return Math.max(1, Math.min(5, usefulness)); }
    public boolean isPopular() { return popular; }
    public boolean isRng() { return rng; }
    public String getWikiPage() { return wikiPage; }
    public List<String> getTags() { return tags == null ? Collections.emptyList() : tags; }

    void freeze()
    {
        dependencyIds = immutable(dependencyIds);
        intents = immutable(intents);
        relationships = immutable(relationships);
        unlocks = immutable(unlocks);
        benefits = immutable(benefits);
        relatedItems = immutable(relatedItems);
        relatedSkills = immutable(relatedSkills);
        relatedQuests = immutable(relatedQuests);
        relatedActivities = immutable(relatedActivities);
        accountTypes = immutable(accountTypes);
        sourceReferences = immutable(sourceReferences);
        tags = immutable(tags);
    }

    private static <T> List<T> immutable(List<T> values)
    {
        return values == null ? Collections.emptyList()
            : Collections.unmodifiableList(new java.util.ArrayList<>(values));
    }
}
