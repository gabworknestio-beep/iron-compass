package com.ironcompass.goal;

import com.google.gson.Gson;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.persistence.ManualOverride;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.state.QuestProgress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class GoalCompletionServiceTest
{
    private GoalCatalog catalog;
    private GoalCompletionService service;
    private InMemoryManualOverrideStore overrides;

    @Before
    public void setUp() throws Exception
    {
        catalog = new GoalLoader(new Gson()).loadResource("/goals/ironman-goals-2026.json");
        service = new GoalCompletionService(new ConditionEvaluator());
        overrides = new InMemoryManualOverrideStore();
    }

    @Test
    public void manualActionsPersistUnderAnIsolatedGoalKeyAndClearRestoresAutomaticState()
    {
        GoalDefinition goal = catalog.find("goal.skill.herblore-70");
        AccountState state = AccountState.builder().skill("Herblore", 70).build();
        assertEquals(GoalStatus.COMPLETE_AUTO, service.evaluate(goal, state, null, overrides).getStatus());

        service.markIncomplete(goal.getId(), overrides);
        assertEquals(ManualOverride.FORCE_INCOMPLETE, overrides.get("goal:" + goal.getId()));
        assertEquals(GoalStatus.INCOMPLETE_MANUAL, service.evaluate(goal, state, null, overrides).getStatus());

        service.markComplete(goal.getId(), overrides);
        assertEquals(GoalStatus.COMPLETE_MANUAL, service.evaluate(goal, state, null, overrides).getStatus());
        service.clear(goal.getId(), overrides);
        assertNull(overrides.get("goal:" + goal.getId()));
        assertEquals(GoalStatus.COMPLETE_AUTO, service.evaluate(goal, state, null, overrides).getStatus());
    }

    @Test
    public void itemOwnershipPreservesUnknownBankAndDetectsCarriedOrObservedItems()
    {
        GoalDefinition fishBarrel = catalog.find("goal.qol.fish-barrel");
        GoalCompletionEvaluation unknown = service.evaluate(fishBarrel,
            AccountState.builder().bank(BankSnapshot.unknown()).build(), null, overrides);
        assertEquals(TruthValue.UNKNOWN, unknown.getCompletion());
        assertEquals(GoalStatus.UNKNOWN, unknown.getStatus());

        GoalCompletionEvaluation absent = service.evaluate(fishBarrel,
            AccountState.builder().bank(BankSnapshot.observed(Collections.emptyMap())).build(), null, overrides);
        assertEquals(TruthValue.FALSE, absent.getCompletion());

        GoalCompletionEvaluation carried = service.evaluate(fishBarrel,
            AccountState.builder().inventoryItem(25582, 1).build(), null, overrides);
        assertEquals(GoalStatus.COMPLETE_AUTO, carried.getStatus());

        Map<Integer, Integer> items = new HashMap<>();
        items.put(25582, 1);
        GoalCompletionEvaluation banked = service.evaluate(fishBarrel,
            AccountState.builder().bank(BankSnapshot.observed(items)).build(), null, overrides);
        assertEquals(GoalStatus.COMPLETE_AUTO, banked.getStatus());
    }

    @Test
    public void readinessDoesNotPretendPietyOrFairyRingsAreComplete()
    {
        AccountState pietyReady = AccountState.builder().skill("Prayer", 70).skill("Defence", 70)
            .quest("King's Ransom", QuestProgress.FINISHED).build();
        GoalCompletionEvaluation piety = service.evaluate(catalog.find("goal.unlock.piety"),
            pietyReady, null, overrides);
        assertEquals(TruthValue.TRUE, piety.getReadiness());
        assertEquals(TruthValue.UNKNOWN, piety.getCompletion());
        assertEquals(GoalStatus.READY, piety.getStatus());

        AccountState ringsReady = AccountState.builder()
            .quest("Fairytale I - Growing Pains", QuestProgress.FINISHED)
            .quest("Fairytale II - Cure a Queen", QuestProgress.IN_PROGRESS).build();
        GoalCompletionEvaluation rings = service.evaluate(catalog.find("goal.transport.fairy-rings"),
            ringsReady, null, overrides);
        assertEquals(TruthValue.TRUE, rings.getReadiness());
        assertEquals(TruthValue.UNKNOWN, rings.getCompletion());
        assertEquals(GoalStatus.READY, rings.getStatus());
    }

    @Test
    public void missingRequirementsAreUnknownAndKnownFalseRequirementsAreLocked()
    {
        GoalCompletionEvaluation nex = service.evaluate(catalog.find("goal.pvm.nex"),
            AccountState.builder().build(), null, overrides);
        assertEquals(TruthValue.UNKNOWN, nex.getReadiness());
        assertEquals(GoalStatus.UNKNOWN, nex.getStatus());

        GoalCompletionEvaluation pietyLocked = service.evaluate(catalog.find("goal.unlock.piety"),
            AccountState.builder().skill("Prayer",70).skill("Defence",67)
                .quest("King's Ransom",QuestProgress.FINISHED).build(), null, overrides);
        assertEquals(TruthValue.FALSE, pietyLocked.getReadiness());
        assertEquals(GoalStatus.LOCKED, pietyLocked.getStatus());
    }

    @Test
    public void gearLinkedGoalUsesTheSameEffectiveRequirements() throws Exception
    {
        AccountState state = AccountState.builder().skill("Attack",59)
            .quest("Monkey Madness I",QuestProgress.FINISHED)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        GearCatalog gearCatalog = new GearLoader(new Gson())
            .loadResource("/gear/ironman-gear-2026.json");
        GearProjection gear = new GearRecommendationService(new ConditionEvaluator()).evaluate(gearCatalog,state,
            new InMemoryGearPreferenceStore(),overrides);
        GoalDefinition goal = catalog.find("gear.early.melee-weapon");
        assertNull(goal.getRequirements());
        assertEquals("ALL",GoalRequirementResolver.effectiveRequirements(goal,gear).getType());
        assertEquals(TruthValue.FALSE,service.evaluate(goal,state,gear,overrides).getReadiness());
    }
}
