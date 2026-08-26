package com.ironpath.gear;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Validates both reference integrity and progression-graph semantics. */
public final class GearValidator
{
    public void validate(GearCatalog catalog) throws GearValidationException
    {
        if (catalog.getVersion() < 1 || catalog.getUpgrades().isEmpty())
        {
            throw new GearValidationException("Gear catalog needs a positive version and at least one upgrade");
        }
        Set<String> ids = new HashSet<>();
        for (GearUpgrade upgrade : catalog.getUpgrades())
        {
            if (blank(upgrade.getId()) || blank(upgrade.getName()) || upgrade.getCompletion() == null
                || upgrade.getStyles().isEmpty() || upgrade.getTier() < 1)
            {
                throw new GearValidationException("Gear upgrade is missing id, name, positive tier, style, or completion: "
                    + upgrade.getId());
            }
            if (!ids.add(upgrade.getId()))
            {
                throw new GearValidationException("Duplicate gear upgrade id: " + upgrade.getId());
            }
        }

        for (GearUpgrade upgrade : catalog.getUpgrades())
        {
            validateReferences(upgrade, upgrade.getPreviousIds(), ids, "previous");
            validateReferences(upgrade, upgrade.getAlternativeIds(), ids, "alternative");
            validateReferences(upgrade, upgrade.getPrerequisiteIds(), ids, "prerequisite");
            validateDistinctRelations(upgrade);
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

    private static boolean blank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
