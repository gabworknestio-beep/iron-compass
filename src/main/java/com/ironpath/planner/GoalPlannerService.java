package com.ironpath.planner;

import com.ironpath.gear.GearEvaluation;
import com.ironpath.gear.GearPreferenceStore;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearStatus;
import com.ironpath.goal.GoalAction;
import com.ironpath.goal.GoalCatalog;
import com.ironpath.goal.GoalDefinition;
import com.ironpath.goal.GoalDependencyResolver;
import com.ironpath.goal.GoalResolution;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.requirement.ConditionSpec;
import com.ironpath.requirement.RequirementResult;
import com.ironpath.requirement.TruthValue;
import com.ironpath.route.RouteProjection;
import com.ironpath.route.StepEvaluation;
import com.ironpath.route.StepStatus;
import com.ironpath.state.AccountState;
import com.ironpath.supply.SupplyForecast;
import com.ironpath.supply.SupplyForecastService;
import com.ironpath.supply.SupplyLine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GoalPlannerService
{
    private final ConditionEvaluator conditions;
    private final GoalDependencyResolver gearResolver;
    private final SupplyForecastService supplies;

    public GoalPlannerService(ConditionEvaluator conditions, GoalDependencyResolver gearResolver,
                              SupplyForecastService supplies)
    {
        this.conditions = conditions;
        this.gearResolver = gearResolver;
        this.supplies = supplies;
    }

    public GoalPlanProjection evaluate(GoalCatalog catalog, AccountState state, GearProjection gear,
                                       RouteProjection route, GearPreferenceStore preferences)
    {
        String selectedId = preferences.getSelectedGoalId();
        if (selectedId == null)
        {
            return empty(catalog, null);
        }
        GoalDefinition selected = catalog.find(selectedId);
        if (selected == null)
        {
            GearEvaluation legacyGear = gear == null ? null : gear.find(selectedId);
            return legacyGear == null || legacyGear.getStatus() == GearStatus.SKIPPED
                ? empty(catalog, selectedId) : legacy(catalog, legacyGear, state, gear, route);
        }
        GearEvaluation selectedGear = gear == null || selected.getGearId() == null
            ? null : gear.find(selected.getGearId());
        if (selectedGear != null && selectedGear.getStatus() == GearStatus.SKIPPED)
        {
            return empty(catalog, selectedId);
        }

        List<String> path = new ArrayList<>();
        Resolved resolved = resolve(selected, catalog, state, gear, route, path, new HashSet<>());
        TruthValue selectedCompletion = completion(selected, state, selectedGear);
        List<RequirementResult> progress = selectedCompletion == TruthValue.TRUE
            ? new ArrayList<>() : progress(selected, state, selectedGear);
        ResourceReadiness resources = resourceReadiness(selectedGear, state);
        return new GoalPlanProjection(catalog, selected, null, selectedCompletion, progress,
            resolved.action, resolved.action == null ? null : resolved.action.getExplanation(),
            afterThis(progress, selected, resolved.action), path, resources, null);
    }

    private GoalPlanProjection legacy(GoalCatalog catalog, GearEvaluation selected, AccountState state,
                                      GearProjection gear, RouteProjection route)
    {
        GoalResolution resolution = gearResolver.resolve(selected, gear, route);
        PlannedAction action = resolution == null || resolution.getNextAction() == null ? null
            : fromGearAction(resolution.getNextAction());
        List<String> path = resolution == null ? new ArrayList<>() : resolution.getDependencyPath();
        String after = action != null && action.getKind() == PlannedAction.Kind.COMPLETE
            ? "Choose another goal when you are ready."
            : "Then continue toward " + selected.getUpgrade().getName() + ".";
        return new GoalPlanProjection(catalog, null, selected, selected.getCompletion(),
            selected.getReadinessDetails(), action, action == null ? null : action.getExplanation(),
            after, path, resourceReadiness(selected, state), null);
    }

    private Resolved resolve(GoalDefinition goal, GoalCatalog catalog, AccountState state, GearProjection gear,
                             RouteProjection route, List<String> path, Set<String> visiting)
    {
        if (!visiting.add(goal.getId()))
        {
            return new Resolved(new PlannedAction(PlannedAction.Kind.REQUIREMENT,
                "Review goal dependencies", "This goal dependency chain contains a cycle.", null, null));
        }
        path.add(goal.getTitle());
        GearEvaluation linkedGear = gear == null || goal.getGearId() == null ? null : gear.find(goal.getGearId());
        TruthValue completion = completion(goal, state, linkedGear);
        if (completion == TruthValue.TRUE)
        {
            return new Resolved(new PlannedAction(PlannedAction.Kind.COMPLETE, goal.getTitle() + " complete",
                "IronPath can confirm that this goal is complete on the current character.", null, linkedGear));
        }
        if (completion == TruthValue.UNKNOWN && linkedGear != null)
        {
            return new Resolved(new PlannedAction(PlannedAction.Kind.GEAR,
                "Confirm ownership of " + goal.getTitle(),
                "Ownership is unconfirmed. Open your bank once if you want IronPath to check stored gear.",
                null, linkedGear));
        }

        for (String dependencyId : goal.getDependencyIds())
        {
            GoalDefinition dependency = catalog.find(dependencyId);
            if (dependency != null)
            {
                GearEvaluation dependencyGear = gear == null || dependency.getGearId() == null
                    ? null : gear.find(dependency.getGearId());
                if (completion(dependency, state, dependencyGear) != TruthValue.TRUE)
                {
                    return resolve(dependency, catalog, state, gear, route, path, visiting);
                }
            }
        }

        ConditionSpec requirements = requirements(goal, linkedGear);
        List<RequirementTarget> targets = requirementTargets(requirements, state);
        RequirementTarget nearest = targets.stream()
            .filter(target -> target.result.getValue() != TruthValue.TRUE)
            .min(Comparator.comparingInt(target -> distance(target.condition, target.result, state)))
            .orElse(null);
        if (nearest != null)
        {
            return new Resolved(requirementAction(nearest, goal, route, state));
        }

        if (linkedGear != null)
        {
            GoalResolution resolution = gearResolver.resolve(linkedGear, gear, route);
            if (resolution != null && resolution.getNextAction() != null)
            {
                return new Resolved(fromGearAction(resolution.getNextAction()));
            }
        }

        StepEvaluation anchor = findRouteStep(route, goal.getRouteAnchorId());
        if (anchor != null && anchor.getStatus() != StepStatus.COMPLETE
            && anchor.getStatus() != StepStatus.SKIPPED_MANUALLY)
        {
            return new Resolved(new PlannedAction(PlannedAction.Kind.ROUTE_STEP, anchor.getStep().getTitle(),
                "This is the authored route milestone that completes " + goal.getTitle() + ".", anchor, linkedGear));
        }

        return new Resolved(new PlannedAction(PlannedAction.Kind.GEAR, "Work toward " + goal.getTitle(),
            goal.getDescription(), null, linkedGear));
    }

    private PlannedAction requirementAction(RequirementTarget target, GoalDefinition goal,
                                            RouteProjection route, AccountState state)
    {
        ConditionSpec condition = target.condition;
        String type = upper(condition.getType());
        if ("SKILL_AT_LEAST".equals(type))
        {
            int actual = state.skillLevel(condition.getSkill());
            return new PlannedAction(PlannedAction.Kind.REQUIREMENT,
                "Train " + condition.getSkill() + " " + actual + " → " + condition.getLevel(),
                condition.getSkill() + " is the closest unfinished skill requirement for " + goal.getTitle() + ".",
                null, null);
        }
        if ("SKILL_SUM_AT_LEAST".equals(type))
        {
            int actual = condition.getSkills().stream().mapToInt(state::skillLevel).sum();
            return new PlannedAction(PlannedAction.Kind.REQUIREMENT,
                "Raise " + String.join(" + ", condition.getSkills()) + " to " + condition.getLevel(),
                "The current combined level is " + actual + "; reaching " + condition.getLevel()
                    + " unlocks this requirement for " + goal.getTitle() + ".", null, null);
        }
        if ("QUEST_STATE".equals(type))
        {
            StepEvaluation quest = findQuestStep(route, condition.getQuest());
            return new PlannedAction(quest == null ? PlannedAction.Kind.REQUIREMENT : PlannedAction.Kind.ROUTE_STEP,
                quest == null ? "Complete " + condition.getQuest() : quest.getStep().getTitle(),
                "This quest is an unfinished prerequisite for " + goal.getTitle() + ".", quest, null);
        }
        if (target.result.getValue() == TruthValue.UNKNOWN)
        {
            return new PlannedAction(PlannedAction.Kind.REQUIREMENT, "Confirm: " + target.result.getLabel(),
                "This requirement cannot be confirmed until the relevant local account state is observed.", null, null);
        }
        return new PlannedAction(PlannedAction.Kind.REQUIREMENT, "Meet: " + target.result.getLabel(),
            "This is the closest unfinished direct requirement for " + goal.getTitle() + ".", null, null);
    }

    private List<RequirementResult> progress(GoalDefinition goal, AccountState state, GearEvaluation linkedGear)
    {
        List<RequirementResult> results = new ArrayList<>();
        for (RequirementTarget target : requirementTargets(requirements(goal, linkedGear), state))
        {
            results.add(target.result);
        }
        return results;
    }

    private List<RequirementTarget> requirementTargets(ConditionSpec condition, AccountState state)
    {
        List<RequirementTarget> targets = new ArrayList<>();
        if (condition == null) return targets;
        String type = upper(condition.getType());
        if ("ALL".equals(type))
        {
            for (ConditionSpec child : condition.getChildren()) targets.addAll(requirementTargets(child, state));
        }
        else if ("ANY".equals(type) && conditions.evaluate(condition, state).getValue() != TruthValue.TRUE)
        {
            for (ConditionSpec child : condition.getChildren()) targets.addAll(requirementTargets(child, state));
        }
        else
        {
            targets.add(new RequirementTarget(condition, conditions.evaluate(condition, state)));
        }
        return targets;
    }

    private int distance(ConditionSpec condition, RequirementResult result, AccountState state)
    {
        if (result.getValue() == TruthValue.UNKNOWN) return 10_000;
        String type = upper(condition.getType());
        if ("SKILL_AT_LEAST".equals(type))
            return Math.max(0, condition.getLevel() - state.skillLevel(condition.getSkill()));
        if ("SKILL_SUM_AT_LEAST".equals(type))
            return Math.max(0, condition.getLevel()
                - condition.getSkills().stream().mapToInt(state::skillLevel).sum());
        if ("QUEST_STATE".equals(type)) return 500;
        return 250;
    }

    private ResourceReadiness resourceReadiness(GearEvaluation gear, AccountState state)
    {
        if (gear == null || gear.getUpgrade().getSupplies().isEmpty()) return null;
        SupplyForecast forecast = supplies.evaluate(gear, state);
        if (forecast == null) return null;
        if (!state.getBank().isObserved())
        {
            return new ResourceReadiness(TruthValue.UNKNOWN,
                "Some required resources cannot be confirmed until your bank is opened.", forecast);
        }
        boolean missing = false;
        for (SupplyLine line : forecast.getLines())
        {
            if (line.getStatus() == TruthValue.FALSE) missing = true;
        }
        return new ResourceReadiness(missing ? TruthValue.FALSE : TruthValue.TRUE,
            missing ? "Some authored supply targets are still missing."
                : "Your observed bank and carried supplies meet the authored targets.", forecast);
    }

    private static ConditionSpec requirements(GoalDefinition goal, GearEvaluation gear)
    {
        return goal.getRequirements() != null ? goal.getRequirements()
            : gear == null ? null : gear.getUpgrade().getRequirements();
    }

    private TruthValue completion(GoalDefinition goal, AccountState state, GearEvaluation gear)
    {
        return gear == null ? conditions.evaluate(goal.getCompletion(), state).getValue() : gear.getCompletion();
    }

    private static PlannedAction fromGearAction(GoalAction action)
    {
        PlannedAction.Kind kind = action.getKind() == GoalAction.Kind.COMPLETE ? PlannedAction.Kind.COMPLETE
            : action.getKind() == GoalAction.Kind.ROUTE_STEP ? PlannedAction.Kind.ROUTE_STEP
            : action.getKind() == GoalAction.Kind.GEAR_UPGRADE ? PlannedAction.Kind.GEAR
            : PlannedAction.Kind.REQUIREMENT;
        return new PlannedAction(kind, action.getTitle(), action.getExplanation(), action.getRouteStep(),
            action.getGearStep());
    }

    private static String afterThis(List<RequirementResult> progress, GoalDefinition goal, PlannedAction action)
    {
        List<RequirementResult> missing = new ArrayList<>();
        for (RequirementResult result : progress)
        {
            if (result.getValue() != TruthValue.TRUE) missing.add(result);
        }
        missing.sort(Comparator.comparingInt(GoalPlannerService::progressDistance));
        if (missing.size() > 1) return "Then work on " + missing.get(1).getLabel() + ".";
        return action != null && action.getKind() == PlannedAction.Kind.COMPLETE
            ? "Choose another goal when you are ready." : "Then complete " + goal.getTitle() + ".";
    }

    private static int progressDistance(RequirementResult result)
    {
        if (result.getValue() == TruthValue.UNKNOWN) return 10_000;
        String detail = result.getDetail();
        if (detail != null)
        {
            String[] parts = detail.split("/");
            if (parts.length == 2)
            {
                try
                {
                    return Math.max(0, Integer.parseInt(parts[1].trim()) - Integer.parseInt(parts[0].trim()));
                }
                catch (NumberFormatException ignored)
                {
                    // Non-numeric progress is ordered after numeric skill progress.
                }
            }
        }
        return 500;
    }

    private static StepEvaluation findRouteStep(RouteProjection route, String id)
    {
        if (route == null || id == null) return null;
        for (StepEvaluation step : route.getSteps()) if (id.equals(step.getStep().getId())) return step;
        return null;
    }

    private static StepEvaluation findQuestStep(RouteProjection route, String quest)
    {
        if (route == null || quest == null) return null;
        for (StepEvaluation step : route.getSteps())
        {
            if (containsQuest(step.getStep().getCompletion(), quest)) return step;
        }
        return null;
    }

    private static boolean containsQuest(ConditionSpec condition, String quest)
    {
        if (condition == null) return false;
        if ("QUEST_STATE".equalsIgnoreCase(condition.getType()) && quest.equals(condition.getQuest())) return true;
        for (ConditionSpec child : condition.getChildren()) if (containsQuest(child, quest)) return true;
        return containsQuest(condition.getChild(), quest);
    }

    private static GoalPlanProjection empty(GoalCatalog catalog, String unavailable)
    {
        return new GoalPlanProjection(catalog, null, null, TruthValue.UNKNOWN, new ArrayList<>(), null, null,
            null, new ArrayList<>(), null, unavailable);
    }

    private static String upper(String value)
    {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private static final class RequirementTarget
    {
        private final ConditionSpec condition;
        private final RequirementResult result;

        private RequirementTarget(ConditionSpec condition, RequirementResult result)
        {
            this.condition = condition;
            this.result = result;
        }
    }

    private static final class Resolved
    {
        private final PlannedAction action;
        private Resolved(PlannedAction action) { this.action = action; }
    }
}
