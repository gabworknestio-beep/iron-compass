package com.ironpath.requirement;

import com.ironpath.state.AccountMode;
import com.ironpath.state.AccountState;
import com.ironpath.state.BankSnapshot;
import com.ironpath.state.QuestProgress;
import com.ironpath.state.WorldLocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.client.game.ItemVariationMapping;

public final class ConditionEvaluator
{
    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "ALL", "ANY", "NOT", "SKILL_AT_LEAST", "SKILL_SUM_AT_LEAST", "QUEST_STATE", "ITEM_PRESENT",
        "ITEM_QUANTITY", "ITEM_ANY", "ITEM_ANY_EXACT", "EQUIPMENT_CONTAINS", "BANK_KNOWN_ITEM_QUANTITY",
        "VARBIT_EQUALS", "VARBIT_AT_LEAST", "VARP_EQUALS", "VARP_AT_LEAST", "LOCATION_REACHED",
        "ACCOUNT_TYPE", "MANUAL_ONLY");

    public static boolean supports(String type)
    {
        return SUPPORTED_TYPES.contains(upper(type));
    }

    public RequirementResult evaluate(ConditionSpec condition, AccountState state)
    {
        if (condition == null)
        {
            return result(TruthValue.TRUE, "No condition", "");
        }

        String type = upper(condition.getType());
        switch (type)
        {
            case "ALL":
                return evaluateAll(condition, state);
            case "ANY":
                return evaluateAny(condition, state);
            case "NOT":
                RequirementResult child = evaluate(condition.getChild(), state);
                return result(child.getValue().not(), label(condition, "Not " + child.getLabel()), child.getDetail());
            case "SKILL_AT_LEAST":
                int actualLevel = state.skillLevel(condition.getSkill());
                return result(actualLevel >= condition.getLevel() ? TruthValue.TRUE : TruthValue.FALSE,
                    label(condition, condition.getSkill() + " " + condition.getLevel()),
                    actualLevel + " / " + condition.getLevel());
            case "SKILL_SUM_AT_LEAST":
                int actualSum = 0;
                for (String skill : condition.getSkills())
                {
                    actualSum += state.skillLevel(skill);
                }
                return result(actualSum >= condition.getLevel() ? TruthValue.TRUE : TruthValue.FALSE,
                    label(condition, String.join(" + ", condition.getSkills()) + " " + condition.getLevel()),
                    actualSum + " / " + condition.getLevel());
            case "QUEST_STATE":
                QuestProgress actualQuest = state.questState(condition.getQuest());
                QuestProgress expectedQuest = parseQuestState(condition.getState());
                TruthValue questValue = actualQuest == QuestProgress.UNKNOWN || expectedQuest == QuestProgress.UNKNOWN
                    ? TruthValue.UNKNOWN
                    : (actualQuest == expectedQuest ? TruthValue.TRUE : TruthValue.FALSE);
                return result(questValue, label(condition, condition.getQuest()), humanize(actualQuest.name()));
            case "ITEM_PRESENT":
            case "ITEM_QUANTITY":
                return evaluateItem(condition, state, false);
            case "ITEM_ANY":
                return evaluateAnyItem(condition, state, true);
            case "ITEM_ANY_EXACT":
                return evaluateAnyItem(condition, state, false);
            case "EQUIPMENT_CONTAINS":
                return evaluateEquipment(condition, state);
            case "BANK_KNOWN_ITEM_QUANTITY":
                return evaluateItem(condition, state, true);
            case "VARBIT_EQUALS":
                return evaluateVariable(condition, state.varbit(condition.getId()), false);
            case "VARBIT_AT_LEAST":
                return evaluateVariable(condition, state.varbit(condition.getId()), true);
            case "VARP_EQUALS":
                return evaluateVariable(condition, state.varp(condition.getId()), false);
            case "VARP_AT_LEAST":
                return evaluateVariable(condition, state.varp(condition.getId()), true);
            case "LOCATION_REACHED":
                return evaluateLocation(condition, state);
            case "ACCOUNT_TYPE":
                return evaluateAccountType(condition, state);
            case "MANUAL_ONLY":
                return result(TruthValue.UNKNOWN, label(condition, "the authored milestone is complete"),
                    "Use Manage → Mark complete after verifying it");
            default:
                return result(TruthValue.UNKNOWN, label(condition, "Unknown condition"), type);
        }
    }

    public List<RequirementResult> explain(ConditionSpec condition, AccountState state)
    {
        if (condition == null)
        {
            return Collections.emptyList();
        }
        if ("ALL".equals(upper(condition.getType())))
        {
            List<RequirementResult> results = new ArrayList<>();
            for (ConditionSpec child : condition.getChildren())
            {
                results.add(evaluate(child, state));
            }
            return results;
        }
        return Collections.singletonList(evaluate(condition, state));
    }

    private RequirementResult evaluateAll(ConditionSpec condition, AccountState state)
    {
        TruthValue combined = TruthValue.TRUE;
        for (ConditionSpec child : condition.getChildren())
        {
            TruthValue value = evaluate(child, state).getValue();
            if (value == TruthValue.FALSE)
            {
                combined = TruthValue.FALSE;
                break;
            }
            if (value == TruthValue.UNKNOWN)
            {
                combined = TruthValue.UNKNOWN;
            }
        }
        return result(combined, label(condition, "All requirements"), summarize(condition, state));
    }

    private RequirementResult evaluateAny(ConditionSpec condition, AccountState state)
    {
        TruthValue combined = TruthValue.FALSE;
        for (ConditionSpec child : condition.getChildren())
        {
            TruthValue value = evaluate(child, state).getValue();
            if (value == TruthValue.TRUE)
            {
                combined = TruthValue.TRUE;
                break;
            }
            if (value == TruthValue.UNKNOWN)
            {
                combined = TruthValue.UNKNOWN;
            }
        }
        return result(combined, label(condition, "Any requirement"), summarize(condition, state));
    }

    private String summarize(ConditionSpec condition, AccountState state)
    {
        int met = 0;
        int unknown = 0;
        for (ConditionSpec child : condition.getChildren())
        {
            TruthValue value = evaluate(child, state).getValue();
            if (value == TruthValue.TRUE)
            {
                met++;
            }
            else if (value == TruthValue.UNKNOWN)
            {
                unknown++;
            }
        }
        return met + " met" + (unknown == 0 ? "" : ", " + unknown + " unknown");
    }

    private RequirementResult evaluateItem(ConditionSpec condition, AccountState state, boolean forceBank)
    {
        int needed = Math.max(1, condition.getQuantity());
        String source = forceBank ? "BANK" : upper(condition.getSource());
        int itemId = ItemVariationMapping.map(condition.getItemId());
        int carried = state.carriedQuantity(itemId);
        BankSnapshot bank = state.getBank();
        String name = label(condition, "Item " + condition.getItemId());

        switch (source)
        {
            case "INVENTORY":
                return quantityResult(name, state.inventoryQuantity(itemId), needed);
            case "EQUIPMENT":
                return quantityResult(name, state.equipmentQuantity(itemId), needed);
            case "CARRIED":
                return quantityResult(name, carried, needed);
            case "BANK":
                if (!bank.isObserved())
                {
                    return result(TruthValue.UNKNOWN, name, "Bank not checked");
                }
                return quantityResult(name, bank.quantity(itemId), needed);
            case "ANY":
            default:
                if (carried >= needed)
                {
                    return quantityResult(name, carried, needed);
                }
                if (!bank.isObserved())
                {
                    return result(TruthValue.UNKNOWN, name, carried + " carried; bank not checked");
                }
                return quantityResult(name, carried + bank.quantity(itemId), needed);
        }
    }

    private RequirementResult evaluateAnyItem(ConditionSpec condition, AccountState state, boolean canonicalize)
    {
        String source = upper(condition.getSource());
        int carried = 0;
        int banked = 0;
        Set<Integer> canonicalIds = new HashSet<>();
        for (int configuredId : condition.getItemIds())
        {
            canonicalIds.add(canonicalize ? ItemVariationMapping.map(configuredId) : configuredId);
        }
        for (int itemId : canonicalIds)
        {
            if ("INVENTORY".equals(source))
            {
                carried += canonicalize ? state.inventoryQuantity(itemId) : state.exactInventoryQuantity(itemId);
            }
            else if ("EQUIPMENT".equals(source))
            {
                carried += canonicalize ? state.equipmentQuantity(itemId) : state.exactEquipmentQuantity(itemId);
            }
            else if ("CARRIED".equals(source) || "ANY".equals(source))
            {
                carried += canonicalize ? state.carriedQuantity(itemId) : state.exactCarriedQuantity(itemId);
            }
            if (("ANY".equals(source) || "BANK".equals(source)) && state.getBank().isObserved())
            {
                banked += canonicalize ? state.getBank().quantity(itemId) : state.getBank().exactQuantity(itemId);
            }
        }
        int actual = "BANK".equals(source) ? banked : carried + ("ANY".equals(source) ? banked : 0);
        String name = label(condition, "Any listed item");
        if (("ANY".equals(source) || "BANK".equals(source)) && !state.getBank().isObserved()
            && actual < Math.max(1, condition.getQuantity()))
        {
            return result(TruthValue.UNKNOWN, name,
                carried + " carried; bank not checked");
        }
        return quantityResult(name, actual, Math.max(1, condition.getQuantity()));
    }

    private RequirementResult evaluateEquipment(ConditionSpec condition, AccountState state)
    {
        int needed = Math.max(1, condition.getQuantity());
        return quantityResult(label(condition, "Equipped item " + condition.getItemId()),
            state.equipmentQuantity(ItemVariationMapping.map(condition.getItemId())), needed);
    }

    private RequirementResult quantityResult(String name, int actual, int needed)
    {
        return result(actual >= needed ? TruthValue.TRUE : TruthValue.FALSE, name, actual + " / " + needed);
    }

    private RequirementResult evaluateVariable(ConditionSpec condition, int actual, boolean atLeast)
    {
        String name = label(condition, (atLeast ? "Variable at least " : "Variable equals ") + condition.getValue());
        if (actual == Integer.MIN_VALUE)
        {
            return result(TruthValue.UNKNOWN, name, "Not observed");
        }
        boolean matches = atLeast ? actual >= condition.getValue() : actual == condition.getValue();
        return result(matches ? TruthValue.TRUE : TruthValue.FALSE, name, actual + " / " + condition.getValue());
    }

    private RequirementResult evaluateLocation(ConditionSpec condition, AccountState state)
    {
        WorldLocation actual = state.getLocation();
        if (actual == null)
        {
            return result(TruthValue.UNKNOWN, label(condition, "Reach location"), "Location unavailable");
        }
        WorldLocation target = new WorldLocation(condition.getX(), condition.getY(), condition.getPlane());
        int distance = actual.distanceTo(target);
        return result(distance <= condition.getRadius() ? TruthValue.TRUE : TruthValue.FALSE,
            label(condition, "Reach location"), distance == Integer.MAX_VALUE ? "Different plane" : distance + " tiles away");
    }

    private RequirementResult evaluateAccountType(ConditionSpec condition, AccountState state)
    {
        AccountMode actual = state.getAccountMode();
        if (actual == AccountMode.UNKNOWN)
        {
            return result(TruthValue.UNKNOWN, label(condition, "Account type"), "Not detected");
        }
        for (String expected : condition.getAccountTypes())
        {
            if (actual.name().equals(upper(expected)))
            {
                return result(TruthValue.TRUE, label(condition, "Account type"), humanize(actual.name()));
            }
        }
        return result(TruthValue.FALSE, label(condition, "Account type"), humanize(actual.name()));
    }

    private static QuestProgress parseQuestState(String state)
    {
        try
        {
            return QuestProgress.valueOf(upper(state));
        }
        catch (IllegalArgumentException ex)
        {
            return QuestProgress.UNKNOWN;
        }
    }

    private static RequirementResult result(TruthValue value, String label, String detail)
    {
        return new RequirementResult(value, label, detail);
    }

    private static String label(ConditionSpec condition, String fallback)
    {
        return condition.getLabel() == null || condition.getLabel().trim().isEmpty() ? fallback : condition.getLabel();
    }

    private static String upper(String value)
    {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private static String humanize(String value)
    {
        return value.toLowerCase(Locale.ENGLISH).replace('_', ' ');
    }
}
