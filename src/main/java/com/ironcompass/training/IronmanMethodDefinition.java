package com.ironcompass.training;

import com.ironcompass.planner.EffortClass;
import com.ironcompass.requirement.ConditionSpec;
import java.util.Collections;
import java.util.List;

public final class IronmanMethodDefinition
{
    private String id;
    private String skill;
    private int minLevel = 1;
    private int maxLevel = 99;
    private String title;
    private String description;
    private ConditionSpec requirements;
    private String risk = "SAFE";
    private String attention = "MEDIUM";
    private EffortClass sessionEffort = EffortClass.MEDIUM;
    private String speed = "MODERATE";
    private String resourceEfficiency = "MODERATE";
    private boolean trainingMethod = true;
    private int recommendationPriority = 50;
    private int xpRateMin;
    private int xpRateMax;
    private int ironmanValue = 3;
    private List<String> styles;
    private List<String> recommendedRequirements;
    private List<String> costs;
    private List<String> consumes;
    private List<MethodResourceGroup> resourceInputs;
    private List<String> usefulOutputs;
    private List<String> acquisitionSources;
    private List<String> unlocks;
    private List<String> benefits;
    private List<String> tags;
    private List<String> playstyles;
    private List<String> accountTypes;
    private List<String> relatedGoals;
    private String wikiPage;
    private List<String> sourceReferences;

    public String getId() { return id; }
    public String getSkill() { return skill; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ConditionSpec getRequirements() { return requirements; }
    public String getRisk() { return risk == null ? "SAFE" : risk; }
    public String getAttention() { return attention == null ? "MEDIUM" : attention; }
    public EffortClass getSessionEffort() { return sessionEffort == null ? EffortClass.MEDIUM : sessionEffort; }
    public String getSpeed() { return speed == null ? "MODERATE" : speed; }
    public String getResourceEfficiency() { return resourceEfficiency == null ? "MODERATE" : resourceEfficiency; }
    public boolean isTrainingMethod() { return trainingMethod; }
    public int getRecommendationPriority() { return recommendationPriority; }
    public int getXpRateMin() { return xpRateMin; }
    public int getXpRateMax() { return xpRateMax; }
    public int getIronmanValue() { return ironmanValue; }
    public List<String> getStyles() { return styles == null ? Collections.emptyList() : styles; }
    public List<String> getRecommendedRequirements()
    {
        return recommendedRequirements == null ? Collections.emptyList() : recommendedRequirements;
    }
    public List<String> getCosts() { return costs == null ? Collections.emptyList() : costs; }
    public List<String> getConsumes() { return consumes == null ? Collections.emptyList() : consumes; }
    public List<MethodResourceGroup> getResourceInputs()
    {
        return resourceInputs == null ? Collections.emptyList() : resourceInputs;
    }
    public List<String> getUsefulOutputs() { return usefulOutputs == null ? Collections.emptyList() : usefulOutputs; }
    public List<String> getAcquisitionSources()
    {
        return acquisitionSources == null ? Collections.emptyList() : acquisitionSources;
    }
    public List<String> getUnlocks() { return unlocks == null ? Collections.emptyList() : unlocks; }
    public List<String> getBenefits() { return benefits == null ? Collections.emptyList() : benefits; }
    public List<String> getTags() { return tags == null ? Collections.emptyList() : tags; }
    public List<String> getPlaystyles() { return playstyles == null ? Collections.emptyList() : playstyles; }
    public List<String> getAccountTypes() { return accountTypes == null ? Collections.emptyList() : accountTypes; }
    public List<String> getRelatedGoals() { return relatedGoals == null ? Collections.emptyList() : relatedGoals; }
    public String getWikiPage() { return wikiPage; }
    public List<String> getSourceReferences()
    {
        return sourceReferences == null ? Collections.emptyList() : sourceReferences;
    }
}
