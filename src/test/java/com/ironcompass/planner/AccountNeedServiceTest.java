package com.ironcompass.planner;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.goal.GoalIntent;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalCompletionService;
import com.ironcompass.goal.GoalLoader;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.state.QuestProgress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class AccountNeedServiceTest
{
    private final AccountNeedService service = new AccountNeedService();

    @Test
    public void lowObservedPrayerSupplyIsWeakAndMoonlightMothsRemainRelevant()
    {
        AccountState state = AccountState.builder().skill("Hunter",72).skill("Herblore",55)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        assertEquals(AccountNeedLevel.WEAK,
            service.evaluate(GoalIntent.PRAYER_SUSTAIN,state,null).getLevel());
    }

    @Test
    public void unobservedBankNeverClaimsPrayerSuppliesAreLow()
    {
        AccountState state = AccountState.builder().skill("Hunter",72).skill("Herblore",55)
            .bank(BankSnapshot.unknown()).build();
        AccountNeedEvaluation result = service.evaluate(GoalIntent.PRAYER_SUSTAIN,state,null);
        assertEquals(AccountNeedLevel.DEVELOPING,result.getLevel());
        assertFalse(result.getPrimaryExplanation().contains("limited"));
    }

    @Test
    public void largeObservedFoodReserveOutweighsMediocreProductionSkills()
    {
        Map<Integer,Integer> bank = new HashMap<>();
        bank.put(ItemID.SHARK,2000);
        AccountState state = AccountState.builder().skill("Fishing",45).skill("Cooking",50)
            .bank(BankSnapshot.observed(bank)).build();
        assertEquals(AccountNeedLevel.STRONG,
            service.evaluate(GoalIntent.FOOD_SUSTAIN,state,null).getLevel());
    }

    @Test
    public void CombatStatsAheadOfDetectedGearExposePowerWeakness() throws Exception
    {
        AccountState state = AccountState.builder().skill("Attack",75).skill("Strength",80)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        GearCatalog catalog = new GearLoader(new Gson()).loadResource("/gear/ironman-gear-2026.json");
        GearProjection gear = new GearRecommendationService(new ConditionEvaluator()).evaluate(catalog,state,
            new InMemoryGearPreferenceStore(),new InMemoryManualOverrideStore());
        assertEquals(AccountNeedLevel.WEAK,service.evaluate(GoalIntent.MELEE_POWER,state,gear).getLevel());
    }

    @Test
    public void FreshAccountIsNotRaidReady()
    {
        assertEquals(AccountNeedLevel.WEAK,service.evaluate(GoalIntent.RAID_READINESS,
            AccountState.builder().build(),null).getLevel());
    }

    @Test
    public void fairytaleInProgressIsOnlyPartialButManualFairyRingConfirmationIsStrong() throws Exception
    {
        GoalCatalog goals = new GoalLoader(new Gson()).loadResource("/goals/ironman-goals-2026.json");
        AccountState state = AccountState.builder()
            .quest("Fairytale II - Cure a Queen",QuestProgress.IN_PROGRESS).build();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        assertEquals(AccountNeedLevel.WEAK,service.evaluate(GoalIntent.TRANSPORT_NETWORK,state,null,
            goals,overrides).getLevel());

        new GoalCompletionService(new ConditionEvaluator()).markComplete("goal.transport.fairy-rings",overrides);
        assertEquals(AccountNeedLevel.DEVELOPING,service.evaluate(GoalIntent.TRANSPORT_NETWORK,state,null,
            goals,overrides).getLevel());
    }
}
