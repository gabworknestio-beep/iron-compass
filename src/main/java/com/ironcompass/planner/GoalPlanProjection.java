package com.ironcompass.planner;

import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.requirement.RequirementResult;
import com.ironcompass.requirement.TruthValue;
import java.util.Collections;
import java.util.List;
import com.ironcompass.training.MethodRecommendation;

public final class GoalPlanProjection
{
    private final GoalCatalog catalog;
    private final GoalDefinition goal;
    private final GearEvaluation legacyGearGoal;
    private final TruthValue completion;
    private final List<RequirementResult> progress;
    private final PlannedAction nextAction;
    private final String whyNow;
    private final String afterThis;
    private final List<String> dependencyPath;
    private final ResourceReadiness resourceReadiness;
    private final String unavailableSelectedId;
    private final List<GoalPlanProjection> secondaryGoals;
    private final MethodRecommendation methodRecommendation;

    public GoalPlanProjection(GoalCatalog catalog, GoalDefinition goal, GearEvaluation legacyGearGoal,
                              TruthValue completion,
                              List<RequirementResult> progress, PlannedAction nextAction, String whyNow,
                              String afterThis, List<String> dependencyPath, ResourceReadiness resourceReadiness,
                              String unavailableSelectedId)
    {
        this(catalog, goal, legacyGearGoal, completion, progress, nextAction, whyNow, afterThis, dependencyPath,
            resourceReadiness, unavailableSelectedId, Collections.emptyList());
    }

    public GoalPlanProjection(GoalCatalog catalog, GoalDefinition goal, GearEvaluation legacyGearGoal,
                              TruthValue completion, List<RequirementResult> progress, PlannedAction nextAction,
                              String whyNow, String afterThis, List<String> dependencyPath,
                              ResourceReadiness resourceReadiness, String unavailableSelectedId,
                              List<GoalPlanProjection> secondaryGoals)
    {
        this(catalog, goal, legacyGearGoal, completion, progress, nextAction, whyNow, afterThis, dependencyPath,
            resourceReadiness, unavailableSelectedId, secondaryGoals, null);
    }

    private GoalPlanProjection(GoalCatalog catalog, GoalDefinition goal, GearEvaluation legacyGearGoal,
                               TruthValue completion, List<RequirementResult> progress, PlannedAction nextAction,
                               String whyNow, String afterThis, List<String> dependencyPath,
                               ResourceReadiness resourceReadiness, String unavailableSelectedId,
                               List<GoalPlanProjection> secondaryGoals, MethodRecommendation methodRecommendation)
    {
        this.catalog = catalog;
        this.goal = goal;
        this.legacyGearGoal = legacyGearGoal;
        this.completion = completion;
        this.progress = Collections.unmodifiableList(new java.util.ArrayList<>(progress));
        this.nextAction = nextAction;
        this.whyNow = whyNow;
        this.afterThis = afterThis;
        this.dependencyPath = Collections.unmodifiableList(new java.util.ArrayList<>(dependencyPath));
        this.resourceReadiness = resourceReadiness;
        this.unavailableSelectedId = unavailableSelectedId;
        this.secondaryGoals = Collections.unmodifiableList(new java.util.ArrayList<>(secondaryGoals));
        this.methodRecommendation = methodRecommendation;
    }

    public GoalCatalog getCatalog() { return catalog; }
    public GoalDefinition getGoal() { return goal; }
    public GearEvaluation getLegacyGearGoal() { return legacyGearGoal; }
    public TruthValue getCompletion() { return completion; }
    public List<RequirementResult> getProgress() { return progress; }
    public PlannedAction getNextAction() { return nextAction; }
    public String getWhyNow() { return whyNow; }
    public String getAfterThis() { return afterThis; }
    public List<String> getDependencyPath() { return dependencyPath; }
    public ResourceReadiness getResourceReadiness() { return resourceReadiness; }
    public String getUnavailableSelectedId() { return unavailableSelectedId; }
    public List<GoalPlanProjection> getSecondaryGoals() { return secondaryGoals; }
    public MethodRecommendation getMethodRecommendation() { return methodRecommendation; }
    public boolean hasSelectedGoal() { return goal != null || legacyGearGoal != null; }
    public boolean hasActiveGoals() { return hasSelectedGoal() || !secondaryGoals.isEmpty(); }
    public List<GoalPlanProjection> getActiveGoals()
    {
        java.util.ArrayList<GoalPlanProjection> active = new java.util.ArrayList<>();
        if (hasSelectedGoal()) active.add(this);
        active.addAll(secondaryGoals);
        return Collections.unmodifiableList(active);
    }

    public GoalPlanProjection withSecondaryGoals(List<GoalPlanProjection> secondary)
    {
        return new GoalPlanProjection(catalog, goal, legacyGearGoal, completion, progress, nextAction, whyNow,
            afterThis, dependencyPath, resourceReadiness, unavailableSelectedId, secondary, methodRecommendation);
    }

    public GoalPlanProjection withMethodRecommendation(MethodRecommendation recommendation)
    {
        return new GoalPlanProjection(catalog, goal, legacyGearGoal, completion, progress, nextAction, whyNow,
            afterThis, dependencyPath, resourceReadiness, unavailableSelectedId, secondaryGoals, recommendation);
    }
    public String getGoalId()
    {
        return goal != null ? goal.getId()
            : legacyGearGoal == null ? null : legacyGearGoal.getUpgrade().getId();
    }
    public String getTitle()
    {
        return goal != null ? goal.getTitle()
            : legacyGearGoal == null ? null : legacyGearGoal.getUpgrade().getName();
    }
    public String getDescription()
    {
        return goal != null ? goal.getDescription()
            : legacyGearGoal == null ? null : legacyGearGoal.getUpgrade().getWhy();
    }
    public java.util.List<String> getUnlocks()
    {
        return goal != null ? goal.getUnlocks() : legacyGearGoal == null
            ? Collections.emptyList() : Collections.singletonList(legacyGearGoal.getUpgrade().getWhy());
    }
    public String getWikiPage()
    {
        return goal != null ? goal.getWikiPage()
            : legacyGearGoal == null ? null : legacyGearGoal.getUpgrade().getWikiPage();
    }
    public java.util.List<String> getTags()
    {
        return goal != null ? goal.getTags() : legacyGearGoal == null
            ? Collections.emptyList() : legacyGearGoal.getUpgrade().getTags();
    }
    public EffortClass getEffort()
    {
        return goal != null ? goal.getEffort() : legacyGearGoal == null ? EffortClass.MEDIUM
            : EffortClass.valueOf(legacyGearGoal.getUpgrade().getEffort().name());
    }
    public String getImpactLabel()
    {
        return goal != null ? goal.getImpact().name()
            : legacyGearGoal != null && legacyGearGoal.getUpgrade().getUsefulness() >= 5 ? "HIGH" : "MEDIUM";
    }
}
