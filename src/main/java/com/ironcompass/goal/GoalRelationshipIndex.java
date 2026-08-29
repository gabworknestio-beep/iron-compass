package com.ironcompass.goal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Direction-aware relationship lookup shared by suggestions and account insights. */
public final class GoalRelationshipIndex
{
    private static final Set<GoalRelationshipType> SYMMETRIC =
        EnumSet.of(GoalRelationshipType.SYNERGY, GoalRelationshipType.ALTERNATIVE);
    private final GoalCatalog catalog;

    public GoalRelationshipIndex(GoalCatalog catalog)
    {
        this.catalog = catalog;
    }

    public boolean hasDirected(String fromId, String toId, GoalRelationshipType type)
    {
        GoalDefinition from = catalog.find(fromId);
        if (from == null) return false;
        for (GoalRelationship relationship : from.getRelationships())
            if (relationship.getType() == type && toId.equals(relationship.getGoalId())) return true;
        return false;
    }

    public boolean hasBetween(String firstId, String secondId, GoalRelationshipType type)
    {
        return hasDirected(firstId, secondId, type)
            || (SYMMETRIC.contains(type) && hasDirected(secondId, firstId, type));
    }

    /** Whether candidate materially supports the active goal under each relationship's direction. */
    public boolean supports(String candidateId, String activeId)
    {
        return hasBetween(candidateId, activeId, GoalRelationshipType.SYNERGY)
            || hasBetween(candidateId, activeId, GoalRelationshipType.ALTERNATIVE)
            || hasDirected(activeId, candidateId, GoalRelationshipType.REQUIRES)
            || hasDirected(activeId, candidateId, GoalRelationshipType.RECOMMENDED_BEFORE)
            || hasDirected(candidateId, activeId, GoalRelationshipType.LEADS_TO);
    }

    public List<GoalDefinition> related(String goalId, GoalRelationshipType type)
    {
        Set<String> ids = new LinkedHashSet<>();
        GoalDefinition source = catalog.find(goalId);
        if (source != null) for (GoalRelationship relationship : source.getRelationships())
            if (relationship.getType() == type) ids.add(relationship.getGoalId());
        if (SYMMETRIC.contains(type)) for (GoalDefinition candidate : catalog.getGoals())
            if (hasDirected(candidate.getId(), goalId, type)) ids.add(candidate.getId());
        List<GoalDefinition> values = new ArrayList<>();
        for (String id : ids)
        {
            GoalDefinition goal = catalog.find(id);
            if (goal != null) values.add(goal);
        }
        return Collections.unmodifiableList(values);
    }
}
