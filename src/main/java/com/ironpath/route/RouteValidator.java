package com.ironpath.route;

import com.ironpath.requirement.ConditionSpec;
import com.ironpath.state.AccountMode;
import com.ironpath.state.AccountState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RouteValidator
{
    private static final Set<String> CONDITION_TYPES = Set.of(
        "ALL", "ANY", "NOT", "SKILL_AT_LEAST", "QUEST_STATE", "ITEM_PRESENT", "ITEM_QUANTITY",
        "EQUIPMENT_CONTAINS", "BANK_KNOWN_ITEM_QUANTITY", "VARBIT_EQUALS", "VARBIT_AT_LEAST",
        "VARP_EQUALS", "VARP_AT_LEAST", "LOCATION_REACHED", "ACCOUNT_TYPE", "MANUAL_ONLY", "ITEM_ANY");
    private static final Set<String> ITEM_SOURCES = Set.of("INVENTORY", "EQUIPMENT", "CARRIED", "BANK", "ANY");
    private static final Set<String> QUEST_STATES = Set.of("NOT_STARTED", "IN_PROGRESS", "FINISHED");
    private static final Set<String> IMPORTANCE_LEVELS = Set.of("NORMAL", "MAJOR");

    private final Set<String> validQuestNames;

    public RouteValidator()
    {
        this(Set.of());
    }

    public RouteValidator(Set<String> validQuestNames)
    {
        this.validQuestNames = new HashSet<>();
        for (String name : validQuestNames)
        {
            this.validQuestNames.add(AccountState.normalize(name));
        }
    }

    public void validate(Route route) throws RouteValidationException
    {
        List<String> errors = new ArrayList<>();
        if (blank(route.getRouteId())) errors.add("routeId is required");
        if (route.getVersion() < 1) errors.add("route version must be positive");
        if (blank(route.getName())) errors.add("route name is required");
        if (route.getSections().isEmpty()) errors.add("route must contain at least one section");

        Set<String> sectionIds = new HashSet<>();
        Map<String, RouteStep> steps = new HashMap<>();
        List<String> orderedStepIds = new ArrayList<>();
        for (RouteSection section : route.getSections())
        {
            if (blank(section.getId()) || !sectionIds.add(section.getId()))
            {
                errors.add("duplicate or missing section id: " + section.getId());
            }
            if (blank(section.getName()))
            {
                errors.add("section name is required for " + section.getId());
            }
            for (RouteStep step : section.getSteps())
            {
                if (blank(step.getId()) || steps.put(step.getId(), step) != null)
                {
                    errors.add("duplicate or missing step id: " + step.getId());
                }
                orderedStepIds.add(step.getId());
                if (step.getType() == null) errors.add("missing step type: " + step.getId());
                if (blank(step.getTitle())) errors.add("missing step title: " + step.getId());
                if (blank(step.getInstruction())) errors.add("missing instruction: " + step.getId());
                else if (step.getInstruction().trim().toLowerCase(Locale.ENGLISH)
                    .startsWith("finish this route milestone"))
                {
                    errors.add("generic instruction must be replaced: " + step.getId());
                }
                if (blank(step.getReason())) errors.add("missing reason: " + step.getId());
                if (step.getRisk() == null) errors.add("invalid risk level: " + step.getId());
                if (!IMPORTANCE_LEVELS.contains(step.getImportance().toUpperCase(Locale.ENGLISH)))
                {
                    errors.add("invalid importance: " + step.getId());
                }
                if (step.getCompletion() == null) errors.add("missing completion condition: " + step.getId());
                if (step.getType() == StepType.MANUAL && !isDirectManualCondition(step.getCompletion()))
                {
                    errors.add("manual step must explicitly use MANUAL_ONLY completion: " + step.getId());
                }
                validateCondition(step.getCompletion(), step.getId() + ".completion", errors);
                validateCondition(step.getReadiness(), step.getId() + ".readiness", errors);
                for (PreparationSpec preparation : step.getPreparation())
                {
                    String kind = upper(preparation.getKind());
                    String source = upper(preparation.getSource());
                    if (blank(preparation.getName()) || !("ITEM".equals(kind) || "SKILL".equals(kind)))
                    {
                        errors.add("invalid preparation kind/name: " + step.getId());
                    }
                    if ("SKILL".equals(kind) && (blank(preparation.getSkill())
                        || preparation.getLevel() < 1 || preparation.getLevel() > 99))
                    {
                        errors.add("invalid preparation skill requirement: " + step.getId());
                    }
                    if ("ITEM".equals(kind) && (preparation.getItemId() <= 0 || preparation.getQuantity() < 1
                        || !ITEM_SOURCES.contains(source)))
                    {
                        errors.add("invalid preparation item requirement: " + step.getId());
                    }
                    if (preparation.getQuantity() < 0 || preparation.getLevel() < 0)
                    {
                        errors.add("negative preparation requirement: " + step.getId());
                    }
                }
            }
        }

        validateChapters(route, steps, orderedStepIds, errors);

        for (RouteStep step : steps.values())
        {
            for (String required : step.getRequires())
            {
                if (!steps.containsKey(required)) errors.add("missing prerequisite " + required + " from " + step.getId());
            }
            if (!blank(step.getHcimAlternativeStepId()) && !steps.containsKey(step.getHcimAlternativeStepId()))
            {
                errors.add("missing HCIM alternative " + step.getHcimAlternativeStepId() + " from " + step.getId());
            }
            if (!blank(step.getAlternativeForStepId()) && !steps.containsKey(step.getAlternativeForStepId()))
            {
                errors.add("missing alternative target " + step.getAlternativeForStepId() + " from " + step.getId());
            }
        }

        detectCycles(steps, errors);
        for (Route.RouteMigration migration : route.getMigrations())
        {
            if (blank(migration.getFromStepId()) || blank(migration.getToStepId()) || !steps.containsKey(migration.getToStepId()))
            {
                errors.add("invalid route migration from " + migration.getFromStepId() + " to " + migration.getToStepId());
            }
        }
        if (!errors.isEmpty())
        {
            throw new RouteValidationException(errors);
        }
    }

    private static void validateChapters(Route route, Map<String, RouteStep> steps, List<String> orderedStepIds,
                                         List<String> errors)
    {
        if (route.getChapters().isEmpty())
        {
            return;
        }
        Set<String> chapterIds = new HashSet<>();
        int previousIndex = -1;
        for (RouteChapterSpec chapter : route.getChapters())
        {
            if (blank(chapter.getId()) || !chapterIds.add(chapter.getId()))
            {
                errors.add("duplicate or missing chapter id: " + chapter.getId());
            }
            if (blank(chapter.getName()) || blank(chapter.getDescription()))
            {
                errors.add("chapter name and description are required for " + chapter.getId());
            }
            if (!steps.containsKey(chapter.getStartStepId()))
            {
                errors.add("chapter " + chapter.getId() + " starts at missing step " + chapter.getStartStepId());
                continue;
            }
            int index = orderedStepIds.indexOf(chapter.getStartStepId());
            if (index <= previousIndex)
            {
                errors.add("chapter starts must follow canonical route order: " + chapter.getId());
            }
            previousIndex = index;
        }
        if (!orderedStepIds.isEmpty() && !route.getChapters().isEmpty()
            && !orderedStepIds.get(0).equals(route.getChapters().get(0).getStartStepId()))
        {
            errors.add("first chapter must start at the first route step");
        }
    }

    private void validateCondition(ConditionSpec condition, String path, List<String> errors)
    {
        if (condition == null)
        {
            return;
        }
        String type = condition.getType() == null ? "" : condition.getType().toUpperCase(Locale.ENGLISH);
        if (!CONDITION_TYPES.contains(type))
        {
            errors.add("invalid condition type at " + path + ": " + condition.getType());
            return;
        }
        if (("ALL".equals(type) || "ANY".equals(type)) && condition.getChildren().isEmpty())
        {
            errors.add(type + " requires children at " + path);
        }
        if ("NOT".equals(type) && condition.getChild() == null)
        {
            errors.add("NOT requires a child at " + path);
        }
        if ("SKILL_AT_LEAST".equals(type) && (blank(condition.getSkill()) || condition.getLevel() < 1 || condition.getLevel() > 99))
        {
            errors.add("invalid skill requirement at " + path);
        }
        if ("QUEST_STATE".equals(type))
        {
            if (blank(condition.getQuest()))
            {
                errors.add("quest name is required at " + path);
            }
            else if (!validQuestNames.isEmpty() && !validQuestNames.contains(AccountState.normalize(condition.getQuest())))
            {
                errors.add("unknown RuneLite quest name at " + path + ": " + condition.getQuest());
            }
            if (!QUEST_STATES.contains(upper(condition.getState())))
            {
                errors.add("invalid quest state at " + path + ": " + condition.getState());
            }
        }
        if (("ITEM_PRESENT".equals(type) || "ITEM_QUANTITY".equals(type)
            || "EQUIPMENT_CONTAINS".equals(type) || "BANK_KNOWN_ITEM_QUANTITY".equals(type))
            && (condition.getItemId() <= 0 || condition.getQuantity() < 1
                || (!"EQUIPMENT_CONTAINS".equals(type) && !ITEM_SOURCES.contains(upper(condition.getSource())))))
        {
            errors.add("invalid item requirement at " + path);
        }
        if ("ITEM_ANY".equals(type)
            && (condition.getItemIds().isEmpty() || condition.getItemIds().stream().anyMatch(id -> id == null || id <= 0)
                || condition.getQuantity() < 1 || !ITEM_SOURCES.contains(upper(condition.getSource()))))
        {
            errors.add("invalid ITEM_ANY requirement at " + path);
        }
        if (("VARBIT_EQUALS".equals(type) || "VARBIT_AT_LEAST".equals(type)
            || "VARP_EQUALS".equals(type) || "VARP_AT_LEAST".equals(type))
            && condition.getId() < 0)
        {
            errors.add("invalid variable id at " + path);
        }
        if ("LOCATION_REACHED".equals(type) && condition.getRadius() < 0)
        {
            errors.add("invalid location radius at " + path);
        }
        if ("ACCOUNT_TYPE".equals(type))
        {
            if (condition.getAccountTypes().isEmpty())
            {
                errors.add("ACCOUNT_TYPE requires accountTypes at " + path);
            }
            for (String accountType : condition.getAccountTypes())
            {
                try
                {
                    AccountMode.valueOf(upper(accountType));
                }
                catch (IllegalArgumentException ex)
                {
                    errors.add("invalid account type at " + path + ": " + accountType);
                }
            }
        }
        if ("MANUAL_ONLY".equals(type))
        {
            String label = condition.getLabel() == null ? "" : condition.getLabel().trim();
            String normalized = label.toLowerCase(Locale.ENGLISH);
            if (label.isEmpty() || "manual confirmation".equals(normalized)
                || "diary completion".equals(normalized) || "milestone reached".equals(normalized))
            {
                errors.add("MANUAL_ONLY requires a specific confirmation label at " + path);
            }
        }
        int index = 0;
        for (ConditionSpec child : condition.getChildren())
        {
            validateCondition(child, path + ".children[" + index++ + "]", errors);
        }
        if (condition.getChild() != null)
        {
            validateCondition(condition.getChild(), path + ".child", errors);
        }
    }

    private void detectCycles(Map<String, RouteStep> steps, List<String> errors)
    {
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String id : steps.keySet())
        {
            if (cycle(id, steps, visited, visiting))
            {
                errors.add("cyclic prerequisite graph involving " + id);
                return;
            }
        }
    }

    private boolean cycle(String id, Map<String, RouteStep> steps, Set<String> visited, Set<String> visiting)
    {
        if (visited.contains(id)) return false;
        if (!visiting.add(id)) return true;
        RouteStep step = steps.get(id);
        if (step != null)
        {
            for (String required : step.getRequires())
            {
                if (cycle(required, steps, visited, visiting)) return true;
            }
        }
        visiting.remove(id);
        visited.add(id);
        return false;
    }

    private static boolean blank(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private static String upper(String value)
    {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private static boolean isDirectManualCondition(ConditionSpec condition)
    {
        return condition != null && "MANUAL_ONLY".equalsIgnoreCase(condition.getType());
    }
}
