package com.ironpath.goal;

import com.ironpath.gear.GearEvaluation;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearStatus;
import com.ironpath.requirement.RequirementResult;
import com.ironpath.requirement.TruthValue;
import com.ironpath.route.RouteProjection;
import com.ironpath.route.StepEvaluation;
import com.ironpath.route.StepStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GoalDependencyResolver
{
    public GoalResolution resolve(GearProjection gear, RouteProjection route)
    {
        GearEvaluation selected = gear == null ? null : gear.getSelected();
        return resolve(selected, gear, route);
    }

    public GoalResolution resolve(GearEvaluation selected, GearProjection gear, RouteProjection route)
    {
        if (selected == null || selected.getStatus() == GearStatus.SKIPPED)
        {
            return null;
        }
        List<String> path = new ArrayList<>();
        GoalAction action = resolve(selected, gear, route, path, new HashSet<>());
        return new GoalResolution(selected, action, path);
    }

    private GoalAction resolve(GearEvaluation target, GearProjection gear, RouteProjection route,
                               List<String> path, Set<String> visiting)
    {
        if (!visiting.add(target.getUpgrade().getId()))
        {
            return new GoalAction(GoalAction.Kind.REQUIREMENT, "Review dependency cycle",
                "The selected gear dependency chain is invalid.", null, target);
        }
        path.add(target.getUpgrade().getName());
        if (target.getStatus() == GearStatus.OWNED)
        {
            return new GoalAction(GoalAction.Kind.COMPLETE, target.getUpgrade().getName() + " complete",
                "The selected goal is detected on this account.", null, target);
        }

        for (String prerequisiteId : target.getUpgrade().getPrerequisiteIds())
        {
            GearEvaluation prerequisite = gear.find(prerequisiteId);
            if (prerequisite != null && prerequisite.getStatus() != GearStatus.OWNED)
            {
                return resolve(prerequisite, gear, route, path, visiting);
            }
        }

        for (String routeStepId : target.getUpgrade().getRouteStepIds())
        {
            StepEvaluation routeStep = findRouteStep(route, routeStepId);
            if (routeStep != null && routeStep.getStatus() != StepStatus.COMPLETE
                && routeStep.getStatus() != StepStatus.SKIPPED_MANUALLY)
            {
                StepEvaluation firstUnfinished = firstUnfinishedThrough(route, routeStepId);
                StepEvaluation actionStep = firstUnfinished == null ? routeStep : firstUnfinished;
                addPath(path, routeStep.getStep().getTitle());
                addPath(path, actionStep.getStep().getTitle());
                String explanation = actionStep == routeStep
                    ? "Completes the route unlock for " + target.getUpgrade().getName() + "."
                    : "First unfinished route step on the way to " + routeStep.getStep().getTitle()
                        + " and " + target.getUpgrade().getName() + ".";
                return new GoalAction(GoalAction.Kind.ROUTE_STEP, actionStep.getStep().getTitle(),
                    explanation, actionStep, target);
            }
        }

        for (RequirementResult requirement : target.getReadinessDetails())
        {
            if (requirement.getValue() != TruthValue.TRUE)
            {
                String detail = requirement.getDetail() == null || requirement.getDetail().isEmpty()
                    ? "" : " (" + requirement.getDetail() + ")";
                return new GoalAction(GoalAction.Kind.REQUIREMENT, "Meet: " + requirement.getLabel(),
                    "Direct requirement for " + target.getUpgrade().getName() + detail, null, target);
            }
        }

        return new GoalAction(GoalAction.Kind.GEAR_UPGRADE, "Obtain " + target.getUpgrade().getName(),
            target.getUpgrade().getWhy(), null, target);
    }

    private static StepEvaluation findRouteStep(RouteProjection route, String id)
    {
        if (route == null)
        {
            return null;
        }
        for (StepEvaluation evaluation : route.getSteps())
        {
            if (id.equals(evaluation.getStep().getId()))
            {
                return evaluation;
            }
        }
        return null;
    }

    private static StepEvaluation firstUnfinishedThrough(RouteProjection route, String targetId)
    {
        if (route == null)
        {
            return null;
        }
        for (StepEvaluation evaluation : route.getSteps())
        {
            boolean target = targetId.equals(evaluation.getStep().getId());
            StepStatus status = evaluation.getStatus();
            if (status != StepStatus.COMPLETE && status != StepStatus.SKIPPED_MANUALLY
                && status != StepStatus.OPTIONAL)
            {
                return evaluation;
            }
            if (target)
            {
                break;
            }
        }
        return null;
    }

    private static void addPath(List<String> path, String value)
    {
        if (value != null && !value.isEmpty() && (path.isEmpty() || !value.equals(path.get(path.size() - 1))))
        {
            path.add(value);
        }
    }
}
