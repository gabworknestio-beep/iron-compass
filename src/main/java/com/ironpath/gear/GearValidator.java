package com.ironpath.gear;

import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.requirement.ConditionSpec;
import com.ironpath.route.Route;
import com.ironpath.route.RouteSection;
import com.ironpath.route.RouteStep;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Validates both reference integrity and progression-graph semantics. */
public final class GearValidator
{
    private static final Pattern STABLE_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{2,95}");

    public void validate(GearCatalog catalog) throws GearValidationException
    {
        validate(catalog, null);
    }

    public void validate(GearCatalog catalog, Route route) throws GearValidationException
    {
        if (catalog.getVersion() < 1 || catalog.getUpgrades().isEmpty())
        {
            throw new GearValidationException("Gear catalog needs a positive version and at least one upgrade");
        }
        Set<String> ids = new HashSet<>();
        for (GearUpgrade upgrade : catalog.getUpgrades())
        {
            if (blank(upgrade.getId()) || !STABLE_ID.matcher(upgrade.getId()).matches()
                || blank(upgrade.getName()) || upgrade.getCompletion() == null
                || upgrade.getStyles().isEmpty() || upgrade.getTier() < 1)
            {
                throw new GearValidationException("Gear upgrade is missing id, name, positive tier, style, or completion: "
                    + upgrade.getId());
            }
            if (!ids.add(upgrade.getId()))
            {
                throw new GearValidationException("Duplicate gear upgrade id: " + upgrade.getId());
            }
            validateCondition(upgrade.getCompletion(), upgrade.getId() + ".completion");
            validateCondition(upgrade.getRequirements(), upgrade.getId() + ".requirements");
            if (!blank(upgrade.getWikiPage())
                && (upgrade.getWikiPage().contains("\n") || upgrade.getWikiPage().contains("://")))
            {
                throw new GearValidationException("Malformed Gear Wiki page title: " + upgrade.getId());
            }
        }

        Set<String> routeIds = route == null ? null : routeIds(route);

        for (GearUpgrade upgrade : catalog.getUpgrades())
        {
            validateReferences(upgrade, upgrade.getPreviousIds(), ids, "previous");
            validateReferences(upgrade, upgrade.getAlternativeIds(), ids, "alternative");
            validateReferences(upgrade, upgrade.getPrerequisiteIds(), ids, "prerequisite");
            validateDistinctRelations(upgrade);
            if (routeIds != null)
            {
                for (String routeStepId : upgrade.getRouteStepIds())
                {
                    if (!routeIds.contains(routeStepId))
                        throw new GearValidationException(upgrade.getId() + " has unknown route step " + routeStepId);
                }
            }
            for (String previousId : upgrade.getPreviousIds())
            {
                GearUpgrade previous = catalog.find(previousId);
                if (previous.getTier() >= upgrade.getTier())
                {
                    throw new GearValidationException("Tier regression: " + upgrade.getId() + " (tier "
                        + upgrade.getTier() + ") follows " + previousId + " (tier " + previous.getTier() + ")");
                }
            }
        }

        detectCycles(catalog, "prerequisite", GearUpgrade::getPrerequisiteIds);
        detectCycles(catalog, "previous", GearUpgrade::getPreviousIds);
        for (GearUpgrade upgrade : catalog.getUpgrades())
        {
            for (String alternativeId : upgrade.getAlternativeIds())
            {
                GearUpgrade alternative = catalog.find(alternativeId);
                if (reachable(catalog, upgrade, alternative.getId(), new HashSet<>())
                    || reachable(catalog, alternative, upgrade.getId(), new HashSet<>()))
                {
                    throw new GearValidationException("Reverse alternative: " + upgrade.getId() + " and "
                        + alternativeId + " are also on the same dependency/upgrade path");
                }
            }
        }
    }

    private static void validateReferences(GearUpgrade owner, List<String> references, Set<String> ids,
                                           String kind) throws GearValidationException
    {
        Set<String> unique = new HashSet<>();
        for (String reference : references)
        {
            if (!ids.contains(reference))
            {
                throw new GearValidationException(owner.getId() + " has unknown " + kind + " id " + reference);
            }
            if (owner.getId().equals(reference))
            {
                throw new GearValidationException(owner.getId() + " has a self " + kind + " reference");
            }
            if (!unique.add(reference))
            {
                throw new GearValidationException(owner.getId() + " repeats " + kind + " id " + reference);
            }
        }
    }

    private static void validateDistinctRelations(GearUpgrade upgrade) throws GearValidationException
    {
        Set<String> seen = new HashSet<>();
        seen.addAll(upgrade.getPreviousIds());
        for (String id : upgrade.getPrerequisiteIds())
        {
            if (!seen.add(id))
            {
                throw new GearValidationException(upgrade.getId() + " uses " + id
                    + " as both previous and prerequisite; choose one graph meaning");
            }
        }
        for (String id : upgrade.getAlternativeIds())
        {
            if (!seen.add(id))
            {
                throw new GearValidationException(upgrade.getId() + " uses " + id
                    + " as an alternative and a dependency/previous step");
            }
        }
    }

    private static void detectCycles(GearCatalog catalog, String kind,
                                     Function<GearUpgrade, List<String>> edges) throws GearValidationException
    {
        Set<String> visited = new HashSet<>();
        for (GearUpgrade upgrade : catalog.getUpgrades())
        {
            detectCycle(catalog, upgrade, kind, edges, new HashSet<>(), visited);
        }
    }

    private static void detectCycle(GearCatalog catalog, GearUpgrade current, String kind,
                                    Function<GearUpgrade, List<String>> edges, Set<String> visiting,
                                    Set<String> visited) throws GearValidationException
    {
        if (visited.contains(current.getId())) return;
        if (!visiting.add(current.getId()))
        {
            throw new GearValidationException("Gear " + kind + " cycle at " + current.getId());
        }
        for (String id : edges.apply(current))
        {
            detectCycle(catalog, catalog.find(id), kind, edges, visiting, visited);
        }
        visiting.remove(current.getId());
        visited.add(current.getId());
    }

    private static boolean reachable(GearCatalog catalog, GearUpgrade from, String target, Set<String> visited)
    {
        if (!visited.add(from.getId())) return false;
        for (String id : from.getPreviousIds())
        {
            if (target.equals(id) || reachable(catalog, catalog.find(id), target, visited)) return true;
        }
        for (String id : from.getPrerequisiteIds())
        {
            if (target.equals(id) || reachable(catalog, catalog.find(id), target, visited)) return true;
        }
        return false;
    }

    private static void validateCondition(ConditionSpec condition, String path) throws GearValidationException
    {
        if (condition == null) return;
        if (!ConditionEvaluator.supports(condition.getType()))
            throw new GearValidationException("Invalid Gear condition type at " + path + ": " + condition.getType());
        String type = condition.getType().toUpperCase(java.util.Locale.ENGLISH);
        if (("ALL".equals(type) || "ANY".equals(type)) && condition.getChildren().isEmpty())
            throw new GearValidationException(type + " requires children at " + path);
        if ("NOT".equals(type) && condition.getChild() == null)
            throw new GearValidationException("NOT requires child at " + path);
        if ("SKILL_AT_LEAST".equals(type)
            && (blank(condition.getSkill()) || condition.getLevel() < 1 || condition.getLevel() > 99))
            throw new GearValidationException("Invalid Gear skill condition at " + path);
        if ("SKILL_SUM_AT_LEAST".equals(type)
            && (condition.getSkills().size() < 2 || condition.getLevel() < 1))
            throw new GearValidationException("Invalid Gear skill-sum condition at " + path);
        if (("ITEM_ANY".equals(type) || "ITEM_ANY_EXACT".equals(type))
            && (condition.getItemIds().isEmpty()
                || condition.getItemIds().stream().anyMatch(id -> id == null || id <= 0)))
            throw new GearValidationException("Invalid Gear item family at " + path);
        for (int i = 0; i < condition.getChildren().size(); i++)
            validateCondition(condition.getChildren().get(i), path + ".children[" + i + "]");
        validateCondition(condition.getChild(), path + ".child");
    }

    private static Set<String> routeIds(Route route)
    {
        Set<String> ids = new HashSet<>();
        for (RouteSection section : route.getSections())
            for (RouteStep step : section.getSteps()) ids.add(step.getId());
        return ids;
    }

    private static boolean blank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
