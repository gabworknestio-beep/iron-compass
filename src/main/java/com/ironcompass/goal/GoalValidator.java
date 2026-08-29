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
import net.runelite.api.Skill;

public final class GoalValidator
{
    private static final Pattern STABLE_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{2,95}");
    private static final Set<String> ITEM_SOURCES = Set.of("INVENTORY", "EQUIPMENT", "CARRIED", "BANK", "ANY");
    private static final Set<String> QUEST_STATES = Set.of("NOT_STARTED", "IN_PROGRESS", "FINISHED");
    private static final Set<String> ACCOUNT_TYPES = Set.of("IRONMAN", "HARDCORE_IRONMAN", "ULTIMATE_IRONMAN",
        "GROUP_IRONMAN", "HARDCORE_GROUP_IRONMAN", "UNRANKED_GROUP_IRONMAN");
    private static final Set<String> SOURCE_KINDS = Set.of("FACT", "FACT_AND_GUIDANCE", "COMMUNITY_GUIDE",
        "COMMUNITY_RECOMMENDATION");
    private static final Set<String> CATEGORIES = Set.of("Account Infrastructure", "Achievement Diaries",
        "Bossing", "Clue Scrolls", "Gear", "Minigames", "Quests", "Raids", "Resources", "Skills",
        "Slayer", "Transportation");
    private static final Set<String> SKILLS = skillNames();
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
        Map<String, GoalSource> sources = new HashMap<>();
        for (GoalSource source : catalog.getSources())
        {
            if (blank(source.getId()) || sources.put(source.getId(), source) != null)
                errors.add("invalid or duplicate goal source id: " + source.getId());
            if (blank(source.getTitle()) || blank(source.getKind()) || blank(source.getConfirms())
                || blank(source.getUrl()) || !source.getUrl().startsWith("https://"))
                errors.add("goal source needs title, HTTPS URL, kind, and confirmation notes: " + source.getId());
            if (!SOURCE_KINDS.contains(upper(source.getKind())))
                errors.add("goal source has unsupported kind: " + source.getId() + " -> " + source.getKind());
        }
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
            if (blank(goal.getTitle()) || blank(goal.getDescription()) || blank(goal.getWhyItMatters())
                || blank(goal.getCategory()))
            {
                errors.add("goal needs title, description, why, and category: " + goal.getId());
            }
            if (!CATEGORIES.contains(goal.getCategory()))
                errors.add("goal has unknown category: " + goal.getId() + " -> " + goal.getCategory());
            if ((goal.getCompletion() == null && blank(goal.getGearId()))
                || goal.getImpact() == null || goal.getEffort() == null || goal.getStage() == null
                || goal.getRiskLevel() == null || goal.getCompletionMode() == null || goal.getPriority() == null
                || goal.getCommunityWeight() == null)
            {
                errors.add("goal needs completion or Gear, plus impact, effort, stage, and risk: " + goal.getId());
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
            if (goal.getBenefits().isEmpty())
                errors.add("goal needs at least one practical benefit: " + goal.getId());
            if (goal.getBenefits().equals(goal.getUnlocks()))
                errors.add("goal benefits must not duplicate unlocks: " + goal.getId());
            validateUniqueStrings(goal.getTags(), goal.getId() + ".tags", true, errors);
            validateUniqueStrings(goal.getRelatedItems(), goal.getId() + ".relatedItems", false, errors);
            validateUniqueStrings(goal.getSourceReferences(), goal.getId() + ".sourceReferences", false, errors);
            for (String skill : goal.getRelatedSkills())
                if (!SKILLS.contains(AccountState.normalize(skill)))
                    errors.add("goal has unknown related skill: " + goal.getId() + " -> " + skill);
            for (String accountType : goal.getAccountTypes())
                if (!ACCOUNT_TYPES.contains(upper(accountType)))
                    errors.add("goal has invalid account type: " + goal.getId() + " -> " + accountType);
            for (String sourceId : goal.getSourceReferences())
                if (!sources.containsKey(sourceId))
                    errors.add("goal has unknown source reference: " + goal.getId() + " -> " + sourceId);
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
            Set<String> relationships = new HashSet<>();
            for (GoalRelationship relationship : goal.getRelationships())
            {
                if (relationship == null || relationship.getType() == null || blank(relationship.getGoalId()))
                {
                    errors.add("goal has malformed relationship: " + goal.getId());
                    continue;
                }
                String key = relationship.getType() + ":" + relationship.getGoalId();
                if (!relationships.add(key)) errors.add("goal repeats relationship: " + goal.getId() + " -> " + key);
                if (goal.getId().equals(relationship.getGoalId()))
                    errors.add("goal relates to itself: " + goal.getId());
                if (!goals.containsKey(relationship.getGoalId()))
                    errors.add("goal has unknown relationship: " + goal.getId() + " -> " + relationship.getGoalId());
            }
        }
        Set<String> usedSources = new HashSet<>();
        for (GoalDefinition goal : catalog.getGoals()) usedSources.addAll(goal.getSourceReferences());
        for (String sourceId : sources.keySet())
            if (!usedSources.contains(sourceId)) errors.add("goal source is never used: " + sourceId);
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
            && (blank(condition.getSkill()) || !SKILLS.contains(AccountState.normalize(condition.getSkill()))
                || condition.getLevel() < 1 || condition.getLevel() > 99))
            errors.add("invalid skill requirement at " + path);
        if ("SKILL_SUM_AT_LEAST".equals(type)
            && (condition.getSkills().size() < 2 || condition.getSkills().stream().anyMatch(GoalValidator::blank)
                || condition.getSkills().stream().map(AccountState::normalize).anyMatch(skill -> !SKILLS.contains(skill))
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

    private static void validateUniqueStrings(List<String> values, String path, boolean canonicalTag,
                                              List<String> errors)
    {
        Set<String> unique = new HashSet<>();
        for (String value : values)
        {
            if (blank(value))
            {
                errors.add("blank value at " + path);
                continue;
            }
            if (canonicalTag && !value.matches("[a-z0-9][a-z0-9-]*"))
                errors.add("non-canonical tag at " + path + ": " + value);
            if (!unique.add(value)) errors.add("duplicate value at " + path + ": " + value);
        }
    }
    private static String upper(String value)
    {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private static Set<String> skillNames()
    {
        Set<String> names = new HashSet<>();
        for (Skill skill : Skill.values()) names.add(AccountState.normalize(skill.getName()));
        return names;
    }
}
