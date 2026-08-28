package com.ironcompass.goal;

import com.ironcompass.gear.GearCatalog;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteSection;
import com.ironcompass.route.RouteStep;
import com.ironcompass.state.AccountState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class GoalValidator
{
    private static final Pattern STABLE_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{2,95}");
    private static final Set<String> ITEM_SOURCES = Set.of("INVENTORY", "EQUIPMENT", "CARRIED", "BANK", "ANY");
    private static final Set<String> QUEST_STATES = Set.of("NOT_STARTED", "IN_PROGRESS", "FINISHED");
    private final Set<String> validQuestNames = new HashSet<>();

    public GoalValidator(Set<String> validQuestNames)
    {
        for (String name : validQuestNames)
        {
            this.validQuestNames.add(AccountState.normalize(name));
        }
    }

    public void validate(GoalCatalog catalog, GearCatalog gear, Route route) throws GoalValidationException
    {
        List<String> errors = new ArrayList<>();
        if (catalog.getVersion() < 1) errors.add("goal catalog version must be positive");
        if (catalog.getGoals().isEmpty()) errors.add("goal catalog must contain goals");

        Map<String, GoalDefinition> goals = new HashMap<>();
        Set<String> routeSteps = routeStepIds(route);
        for (GoalDefinition goal : catalog.getGoals())
        {
            if (blank(goal.getId()) || !STABLE_ID.matcher(goal.getId()).matches())
            {
                errors.add("invalid persistent goal id: " + goal.getId());
            }
            else if (goals.put(goal.getId(), goal) != null)
            {
                errors.add("duplicate goal id: " + goal.getId());
            }
            if (blank(goal.getTitle()) || blank(goal.getDescription()) || blank(goal.getCategory()))
            {
                errors.add("goal needs title, description, and category: " + goal.getId());
            }
            if ((goal.getCompletion() == null && blank(goal.getGearId()))
                || goal.getImpact() == null || goal.getEffort() == null)
            {
                errors.add("goal needs completion or a linked Gear objective, plus impact and effort: " + goal.getId());
            }
            validateCondition(goal.getCompletion(), goal.getId() + ".completion", errors);
            validateCondition(goal.getRequirements(), goal.getId() + ".requirements", errors);
            if (!blank(goal.getGearId()) && gear.find(goal.getGearId()) == null)
            {
                errors.add("goal has unknown gear id: " + goal.getId() + " -> " + goal.getGearId());
            }
            if (!blank(goal.getRouteAnchorId()) && !routeSteps.contains(goal.getRouteAnchorId()))
            {
                errors.add("goal has unknown route anchor: " + goal.getId() + " -> " + goal.getRouteAnchorId());
            }
            if (goal.getUnlocks().isEmpty())
            {
                errors.add("goal needs at least one audited unlock: " + goal.getId());
            }
            if (blank(goal.getWikiPage()) || goal.getWikiPage().contains("\n")
                || goal.getWikiPage().contains("://"))
            {
                errors.add("goal has malformed Wiki page title: " + goal.getId());
            }
        }

        for (GoalDefinition goal : catalog.getGoals())
        {
            Set<String> unique = new HashSet<>();
            for (String dependency : goal.getDependencyIds())
            {
                if (!unique.add(dependency)) errors.add("goal repeats dependency: " + goal.getId());
                if (!goals.containsKey(dependency))
                {
                    errors.add("goal has unknown dependency: " + goal.getId() + " -> " + dependency);
                }
            }
        }
        detectCycles(goals, errors);
        if (!errors.isEmpty()) throw new GoalValidationException(errors);
    }

    private void validateCondition(ConditionSpec condition, String path, List<String> errors)
    {
        if (condition == null) return;
        String type = upper(condition.getType());
        if (!ConditionEvaluator.supports(type))
        {
            errors.add("invalid condition type at " + path + ": " + condition.getType());
            return;
        }
        if (("ALL".equals(type) || "ANY".equals(type)) && condition.getChildren().isEmpty())
            errors.add(type + " requires children at " + path);
        if ("NOT".equals(type) && condition.getChild() == null) errors.add("NOT requires child at " + path);
        if ("SKILL_AT_LEAST".equals(type)
            && (blank(condition.getSkill()) || condition.getLevel() < 1 || condition.getLevel() > 99))
            errors.add("invalid skill requirement at " + path);
        if ("SKILL_SUM_AT_LEAST".equals(type)
            && (condition.getSkills().size() < 2 || condition.getSkills().stream().anyMatch(GoalValidator::blank)
                || condition.getLevel() < 1 || condition.getLevel() > condition.getSkills().size() * 99))
            errors.add("invalid skill-sum requirement at " + path);
        if ("QUEST_STATE".equals(type))
        {
            if (blank(condition.getQuest())
                || (!validQuestNames.isEmpty() && !validQuestNames.contains(AccountState.normalize(condition.getQuest()))))
                errors.add("unknown quest at " + path + ": " + condition.getQuest());
            if (!QUEST_STATES.contains(upper(condition.getState()))) errors.add("invalid quest state at " + path);
        }
        if (("ITEM_PRESENT".equals(type) || "ITEM_QUANTITY".equals(type)
            || "EQUIPMENT_CONTAINS".equals(type) || "BANK_KNOWN_ITEM_QUANTITY".equals(type))
            && (condition.getItemId() <= 0 || condition.getQuantity() < 1))
            errors.add("invalid item requirement at " + path);
        if (("ITEM_ANY".equals(type) || "ITEM_ANY_EXACT".equals(type))
            && (condition.getItemIds().isEmpty() || condition.getItemIds().stream().anyMatch(id -> id == null || id <= 0)
                || condition.getQuantity() < 1 || !ITEM_SOURCES.contains(upper(condition.getSource()))))
            errors.add("invalid item family at " + path);
        for (int i = 0; i < condition.getChildren().size(); i++)
            validateCondition(condition.getChildren().get(i), path + ".children[" + i + "]", errors);
        if (condition.getChild() != null) validateCondition(condition.getChild(), path + ".child", errors);
    }

    private static Set<String> routeStepIds(Route route)
    {
        Set<String> ids = new HashSet<>();
        for (RouteSection section : route.getSections())
            for (RouteStep step : section.getSteps()) ids.add(step.getId());
        return ids;
    }

    private static void detectCycles(Map<String, GoalDefinition> goals, List<String> errors)
    {
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String id : goals.keySet())
        {
            if (cycle(id, goals, visited, visiting))
            {
                errors.add("goal dependency cycle involving " + id);
                return;
            }
        }
    }

    private static boolean cycle(String id, Map<String, GoalDefinition> goals, Set<String> visited,
                                 Set<String> visiting)
    {
        if (visited.contains(id)) return false;
        if (!visiting.add(id)) return true;
        GoalDefinition goal = goals.get(id);
        if (goal != null)
        {
            for (String dependency : goal.getDependencyIds())
                if (cycle(dependency, goals, visited, visiting)) return true;
        }
        visiting.remove(id);
        visited.add(id);
        return false;
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static String upper(String value)
    {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }
}
