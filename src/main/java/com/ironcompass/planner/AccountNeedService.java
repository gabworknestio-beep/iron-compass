package com.ironcompass.planner;

import com.ironcompass.gear.CombatStyle;
import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.goal.GoalIntent;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalCompletionService;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.QuestProgress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.gameval.ItemID;

/** The single account-need evaluator used by health, suggestions, and goal alternatives. */
public final class AccountNeedService
{
    private final GoalCompletionService completion = new GoalCompletionService(new ConditionEvaluator());
    private static final List<GoalIntent> HEALTH_INTENTS = Arrays.asList(
        GoalIntent.PRAYER_SUSTAIN, GoalIntent.FOOD_SUSTAIN, GoalIntent.TRANSPORT_NETWORK,
        GoalIntent.MELEE_POWER, GoalIntent.RANGED_POWER, GoalIntent.MAGIC_POWER,
        GoalIntent.RUNE_SUPPLY, GoalIntent.POH_NETWORK, GoalIntent.BOSSING_READINESS);

    public AccountHealthProjection health(AccountState state, GearProjection gear)
    {
        return health(state, gear, null, null);
    }

    public AccountHealthProjection health(AccountState state, GearProjection gear, GoalCatalog catalog,
                                           ManualOverrideStore overrides)
    {
        List<AccountNeedEvaluation> values = new ArrayList<>();
        for (GoalIntent intent : HEALTH_INTENTS) values.add(evaluate(intent, state, gear, catalog, overrides));
        return new AccountHealthProjection(values);
    }

    public AccountNeedEvaluation evaluate(GoalIntent intent, AccountState state, GearProjection gear)
    {
        return evaluate(intent, state, gear, null, null);
    }

    public AccountNeedEvaluation evaluate(GoalIntent intent, AccountState state, GearProjection gear,
                                          GoalCatalog catalog, ManualOverrideStore overrides)
    {
        switch (intent)
        {
            case PRAYER_SUSTAIN: return prayer(state);
            case FOOD_SUSTAIN: return food(state);
            case MELEE_POWER: return combat(intent, CombatStyle.MELEE, "Attack", "Strength", state, gear);
            case RANGED_POWER: return combat(intent, CombatStyle.RANGED, "Ranged", null, state, gear);
            case MAGIC_POWER: return magic(state, gear);
            case BOSSING_READINESS: return bossing(state, gear);
            case RAID_READINESS: return raids(state, gear);
            case TRANSPORT_NETWORK: return transport(state, catalog, overrides);
            case CLUE_SUPPORT: return clues(state);
            case ACCOUNT_INFRASTRUCTURE: return infrastructure(state, gear);
            case RUNE_SUPPLY: return runes(state);
            case HERB_SUPPLY: return herbs(state);
            case GP_SUSTAIN: return gp(state);
            case AMMO_SUPPLY: return ammo(state, gear);
            case CRAFTING_SUPPLY: return skillNeed(intent, "Crafting", 70, 85, state);
            case POH_NETWORK: return skillNeed(intent, "Construction", 50, 83, state);
            case SLAYER_PROGRESS: return skillNeed(intent, "Slayer", 58, 87, state);
            default: return result(intent, AccountNeedLevel.UNKNOWN,
                "This account need is not observable from the current local snapshot.");
        }
    }

    private AccountNeedEvaluation prayer(AccountState state)
    {
        int renewable = (state.skillLevel("Herblore") >= 38 ? 1 : 0)
            + (state.skillLevel("Farming") >= 32 ? 1 : 0)
            + (state.skillLevel("Hunter") >= 75 && finished(state,"Children of the Sun") ? 1 : 0);
        if (!state.getBank().isObserved())
        {
            AccountNeedLevel level = renewable >= 2 ? AccountNeedLevel.GOOD
                : renewable == 1 ? AccountNeedLevel.DEVELOPING : AccountNeedLevel.UNKNOWN;
            return result(GoalIntent.PRAYER_SUSTAIN, level,
                "Your bank has not been observed, so Prayer reserves are not treated as empty.",
                renewable + " renewable Prayer supply path" + (renewable == 1 ? " is" : "s are") + " available.");
        }
        int supplies = quantity(state, ItemID._4DOSEPRAYERRESTORE) + quantity(state, ItemID._4DOSE2RESTORE)
            + quantity(state, ItemID.HUNTGUIDE_MOONLIGHT_MOTH)
            + quantity(state, ItemID.HUNTER_MIX_MOONMOTH_2DOSE);
        if (supplies < 8 && renewable < 2)
            return result(GoalIntent.PRAYER_SUSTAIN, AccountNeedLevel.WEAK,
                "Your observed Prayer supplies are limited and renewable options are still locked.");
        if (supplies < 20 || renewable < 2)
            return result(GoalIntent.PRAYER_SUSTAIN, AccountNeedLevel.DEVELOPING,
                "Your observed Prayer reserves or renewable production paths are still developing.");
        return result(GoalIntent.PRAYER_SUSTAIN, supplies >= 100 && renewable >= 2
            ? AccountNeedLevel.STRONG : AccountNeedLevel.GOOD,
            "Observed Prayer supplies are backed by renewable account progression.");
    }

    private AccountNeedEvaluation food(AccountState state)
    {
        int renewable = (state.skillLevel("Fishing") >= 65 ? 1 : 0)
            + (state.skillLevel("Hunter") >= 72 && finished(state,"Children of the Sun") ? 1 : 0)
            + (state.skillLevel("Cooking") >= 70 ? 1 : 0);
        if (!state.getBank().isObserved())
            return result(GoalIntent.FOOD_SUSTAIN, renewable >= 2 ? AccountNeedLevel.GOOD
                : renewable == 1 ? AccountNeedLevel.DEVELOPING : AccountNeedLevel.UNKNOWN,
                "Your bank has not been observed, so food reserves are unknown rather than empty.");
        int food = quantity(state, ItemID.TBWT_COOKED_KARAMBWAN) + quantity(state, ItemID.SHARK)
            + quantity(state, ItemID.ANGLERFISH) + quantity(state, ItemID.ANTELOPESUN_COOKED)
            + quantity(state, ItemID.ANTELOPEMOON_COOKED);
        if (food >= 1000) return result(GoalIntent.FOOD_SUSTAIN, AccountNeedLevel.STRONG,
            "A large reserve of useful combat food was observed in your bank or inventory.");
        if (food >= 150 || renewable >= 2) return result(GoalIntent.FOOD_SUSTAIN, AccountNeedLevel.GOOD,
            "Useful combat food or multiple renewable food paths are available.");
        if (food < 25 && renewable == 0) return result(GoalIntent.FOOD_SUSTAIN, AccountNeedLevel.WEAK,
            "Very little useful combat food was observed and renewable methods are still limited.");
        return result(GoalIntent.FOOD_SUSTAIN, AccountNeedLevel.DEVELOPING,
            "Some food is available, but reserves or renewable methods can still improve.");
    }

    private AccountNeedEvaluation combat(GoalIntent intent, CombatStyle style, String firstSkill,
                                         String secondSkill, AccountState state, GearProjection gear)
    {
        int level = state.skillLevel(firstSkill);
        if (secondSkill != null) level = (level + state.skillLevel(secondSkill)) / 2;
        int tier = ownedTier(gear, style);
        if (level >= 70 && tier < 2) return result(intent, AccountNeedLevel.WEAK,
            "Your combat stats are ahead of your detected " + style.name().toLowerCase() + " gear tier.");
        if (level < 40 && tier < 2) return result(intent, AccountNeedLevel.DEVELOPING,
            "Both levels and detected gear are still in the early progression band.");
        if (tier >= 4 && level >= 80) return result(intent, AccountNeedLevel.STRONG,
            "High combat levels and a strong detected gear tier support this style.");
        if (tier >= 2 && level >= 60) return result(intent, AccountNeedLevel.GOOD,
            "Detected gear and combat levels form a practical current baseline.");
        return result(intent, AccountNeedLevel.DEVELOPING,
            "A nearby level or gear upgrade would strengthen this combat style.");
    }

    private AccountNeedEvaluation magic(AccountState state, GearProjection gear)
    {
        AccountNeedEvaluation base = combat(GoalIntent.MAGIC_POWER, CombatStyle.MAGIC, "Magic", null, state, gear);
        int spellbooks = (finished(state,"Desert Treasure I") ? 1 : 0)
            + (finished(state,"Lunar Diplomacy") ? 1 : 0) + (finished(state,"A Kingdom Divided") ? 1 : 0);
        if (base.getLevel() == AccountNeedLevel.DEVELOPING && spellbooks == 0)
            return result(GoalIntent.MAGIC_POWER, AccountNeedLevel.WEAK,
                "Magic gear is developing and no major alternate spellbook is confirmed unlocked.");
        return base;
    }

    private AccountNeedEvaluation bossing(AccountState state, GearProjection gear)
    {
        int combat = combatAverage(state);
        int styles = strongStyles(gear, 2);
        AccountNeedLevel supplies = weaker(prayer(state).getLevel(), food(state).getLevel());
        if (combat < 45 || styles == 0) return result(GoalIntent.BOSSING_READINESS, AccountNeedLevel.WEAK,
            "Combat levels or detected gear are below a dependable bossing baseline.");
        if (combat >= 80 && styles >= 2 && supplies.ordinal() >= AccountNeedLevel.GOOD.ordinal())
            return result(GoalIntent.BOSSING_READINESS, AccountNeedLevel.GOOD,
                "Combat levels, multiple gear styles, and supply systems support repeatable bossing.");
        return result(GoalIntent.BOSSING_READINESS, AccountNeedLevel.DEVELOPING,
            "Some bossing foundations exist, but combat, gear, or supplies still need development.");
    }

    private AccountNeedEvaluation raids(AccountState state, GearProjection gear)
    {
        int combat = combatAverage(state);
        int styles = strongStyles(gear, 3);
        if (combat < 70 || styles < 2 || state.skillLevel("Prayer") < 70)
            return result(GoalIntent.RAID_READINESS, AccountNeedLevel.WEAK,
                "A fresh or single-style account is not treated as raid-ready.");
        if (combat >= 85 && styles >= 3)
            return result(GoalIntent.RAID_READINESS, AccountNeedLevel.GOOD,
                "All three combat styles and high combat levels form a raid-ready baseline.");
        return result(GoalIntent.RAID_READINESS, AccountNeedLevel.DEVELOPING,
            "The account is approaching a broad three-style raid baseline.");
    }

    private AccountNeedEvaluation transport(AccountState state)
    {
        return transport(state, null, null);
    }

    private AccountNeedEvaluation transport(AccountState state, GoalCatalog catalog,
                                             ManualOverrideStore overrides)
    {
        int systems = 0;
        if (finished(state,"Tree Gnome Village")) systems++;
        if (finished(state,"The Grand Tree")) systems++;
        boolean fairyConfirmed = finished(state,"Fairytale II - Cure a Queen");
        if (!fairyConfirmed && catalog != null)
        {
            GoalDefinition fairy = catalog.find("goal.transport.fairy-rings");
            fairyConfirmed = fairy != null && completion.evaluate(fairy,state,null,overrides)
                .getCompletion() == TruthValue.TRUE;
        }
        if (fairyConfirmed) systems += 2;
        else if (state.questState("Fairytale II - Cure a Queen") == QuestProgress.IN_PROGRESS) systems++;
        if (state.skillLevel("Magic") >= 45) systems++;
        if (state.skillLevel("Construction") >= 50) systems++;
        if (finished(state,"A Taste of Hope")) systems++;
        return result(GoalIntent.TRANSPORT_NETWORK, systems >= 6 ? AccountNeedLevel.STRONG
            : systems >= 4 ? AccountNeedLevel.GOOD : systems >= 2 ? AccountNeedLevel.DEVELOPING
            : AccountNeedLevel.WEAK, systems + " major travel-system signals are confirmed locally.");
    }


    private AccountNeedEvaluation clues(AccountState state)
    {
        AccountNeedLevel travel = transport(state).getLevel();
        int support = (state.skillLevel("Hunter") >= 50 ? 1 : 0)
            + (state.skillLevel("Construction") >= 42 ? 1 : 0) + (state.skillLevel("Crafting") >= 50 ? 1 : 0);
        return result(GoalIntent.CLUE_SUPPORT, support >= 2 && travel.ordinal() >= AccountNeedLevel.GOOD.ordinal()
            ? AccountNeedLevel.GOOD : support == 0 && travel == AccountNeedLevel.WEAK
            ? AccountNeedLevel.WEAK : AccountNeedLevel.DEVELOPING,
            "Clue support combines travel, impling access, Crafting, and Construction signals.");
    }

    private AccountNeedEvaluation infrastructure(AccountState state, GearProjection gear)
    {
        List<AccountNeedLevel> parts = Arrays.asList(prayer(state).getLevel(), food(state).getLevel(),
            transport(state).getLevel(), runes(state).getLevel(), herbs(state).getLevel(),
            skillNeed(GoalIntent.POH_NETWORK,"Construction",50,83,state).getLevel());
        int known = 0, weak = 0, good = 0;
        for (AccountNeedLevel part : parts)
        {
            if (part == AccountNeedLevel.UNKNOWN) continue;
            known++;
            if (part == AccountNeedLevel.WEAK) weak++;
            if (part == AccountNeedLevel.GOOD || part == AccountNeedLevel.STRONG) good++;
        }
        if (known < 3) return result(GoalIntent.ACCOUNT_INFRASTRUCTURE, AccountNeedLevel.UNKNOWN,
            "Too few underlying supply and travel systems can be evaluated honestly.");
        return result(GoalIntent.ACCOUNT_INFRASTRUCTURE, weak >= 2 ? AccountNeedLevel.WEAK
            : good >= 5 ? AccountNeedLevel.STRONG : good >= 3 ? AccountNeedLevel.GOOD
            : AccountNeedLevel.DEVELOPING,
            "This summary aggregates Prayer, food, transport, runes, herbs, and POH needs.");
    }

    private AccountNeedEvaluation runes(AccountState state)
    {
        if (!state.getBank().isObserved()) return result(GoalIntent.RUNE_SUPPLY, AccountNeedLevel.UNKNOWN,
            "Your bank has not been observed, so rune reserves cannot be evaluated.");
        int runes = quantity(state,ItemID.CHAOSRUNE) + quantity(state,ItemID.DEATHRUNE)
            + quantity(state,ItemID.BLOODRUNE) + quantity(state,ItemID.LAWRUNE) + quantity(state,ItemID.NATURERUNE);
        return result(GoalIntent.RUNE_SUPPLY, runes >= 10000 ? AccountNeedLevel.STRONG
            : runes >= 1500 ? AccountNeedLevel.GOOD : runes >= 250 ? AccountNeedLevel.DEVELOPING
            : AccountNeedLevel.WEAK, "The result uses observed utility and combat rune reserves.");
    }

    private AccountNeedEvaluation herbs(AccountState state)
    {
        int maturity = (state.skillLevel("Farming") >= 65 ? 1 : 0) + (state.skillLevel("Herblore") >= 63 ? 1 : 0);
        return result(GoalIntent.HERB_SUPPLY, maturity == 2 ? AccountNeedLevel.GOOD
            : maturity == 1 ? AccountNeedLevel.DEVELOPING : AccountNeedLevel.WEAK,
            "Farming contracts and useful potion levels indicate renewable herb maturity.");
    }

    private AccountNeedEvaluation gp(AccountState state)
    {
        if (!state.getBank().isObserved()) return result(GoalIntent.GP_SUSTAIN, AccountNeedLevel.UNKNOWN,
            "Your bank has not been observed, so cash reserves are unknown.");
        int coins = quantity(state, ItemID.COINS);
        return result(GoalIntent.GP_SUSTAIN, coins >= 2_000_000 ? AccountNeedLevel.STRONG
            : coins >= 300_000 ? AccountNeedLevel.GOOD : coins >= 50_000 ? AccountNeedLevel.DEVELOPING
            : AccountNeedLevel.WEAK, "The result uses observed coins and does not assume an unobserved bank is empty.");
    }

    private AccountNeedEvaluation ammo(AccountState state, GearProjection gear)
    {
        int tier = ownedTier(gear, CombatStyle.RANGED);
        int production = Math.max(state.skillLevel("Fletching"), state.skillLevel("Hunter"));
        return result(GoalIntent.AMMO_SUPPLY, tier >= 3 && production < 55 ? AccountNeedLevel.WEAK
            : production >= 72 ? AccountNeedLevel.GOOD : production >= 55 ? AccountNeedLevel.DEVELOPING
            : AccountNeedLevel.WEAK,
            "Ranged gear is compared with renewable Fletching or Hunter ammunition paths.");
    }

    private AccountNeedEvaluation skillNeed(GoalIntent intent, String skill, int developing, int good,
                                            AccountState state)
    {
        int level = state.skillLevel(skill);
        return result(intent, level >= good ? AccountNeedLevel.GOOD : level >= developing
            ? AccountNeedLevel.DEVELOPING : AccountNeedLevel.WEAK,
            skill + " is " + level + "; practical milestones begin around " + developing + ".");
    }

    private static int quantity(AccountState state, int itemId)
    {
        return state.carriedQuantity(itemId) + state.getBank().quantity(itemId);
    }

    private static int ownedTier(GearProjection gear, CombatStyle style)
    {
        int tier = 0;
        if (gear != null) for (GearEvaluation value : gear.getEvaluations())
            if (value.getCompletion() == TruthValue.TRUE && value.getUpgrade().getStyles().contains(style))
                tier = Math.max(tier, value.getUpgrade().getTier());
        return tier;
    }

    private static int strongStyles(GearProjection gear, int minimumTier)
    {
        int count = 0;
        for (CombatStyle style : Arrays.asList(CombatStyle.MELEE,CombatStyle.RANGED,CombatStyle.MAGIC))
            if (ownedTier(gear, style) >= minimumTier) count++;
        return count;
    }

    private static int combatAverage(AccountState state)
    {
        return (state.skillLevel("Attack") + state.skillLevel("Strength") + state.skillLevel("Defence")
            + state.skillLevel("Ranged") + state.skillLevel("Magic") + state.skillLevel("Hitpoints")) / 6;
    }

    private static boolean finished(AccountState state, String quest)
    {
        return state.questState(quest) == QuestProgress.FINISHED;
    }

    private static AccountNeedLevel weaker(AccountNeedLevel first, AccountNeedLevel second)
    {
        if (first == AccountNeedLevel.UNKNOWN) return second;
        if (second == AccountNeedLevel.UNKNOWN) return first;
        return first.ordinal() < second.ordinal() ? first : second;
    }

    private static AccountNeedEvaluation result(GoalIntent intent, AccountNeedLevel level, String... explanations)
    {
        return new AccountNeedEvaluation(intent, level, Arrays.asList(explanations));
    }
}
