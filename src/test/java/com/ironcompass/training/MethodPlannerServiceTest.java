package com.ironcompass.training;

import com.google.gson.Gson;
import com.ironcompass.planner.InMemoryPlannerPreferenceStore;
import com.ironcompass.planner.PlannedAction;
import com.ironcompass.planner.Playstyle;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.state.QuestProgress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public final class MethodPlannerServiceTest
{
    private IronmanMethodCatalog catalog;
    private MethodPlannerService planner;
    private InMemoryPlannerPreferenceStore preferences;

    @Before
    public void setUp() throws Exception
    {
        catalog = new IronmanMethodLoader(new Gson()).loadResource("/methods/ironman-methods-2026.json");
        planner = new MethodPlannerService(new ConditionEvaluator());
        preferences = new InMemoryPlannerPreferenceStore();
    }

    @Test
    public void lockedMethodIsNotRecommended()
    {
        AccountState state = AccountState.builder().skill("Crafting", 61).skill("Magic", 70)
            .quest("Lunar Diplomacy", QuestProgress.NOT_STARTED).build();
        assertNull(recommend("Crafting", 70, state));
    }

    @Test
    public void multipleValidMethodsProduceOneRecommendationAndAlternatives()
    {
        Map<Integer, Integer> herbs = new HashMap<>();
        herbs.put(249, 50);
        AccountState state = AccountState.builder().skill("Herblore", 61).skill("Farming", 50)
            .quest("Children of the Sun", QuestProgress.FINISHED).bank(BankSnapshot.observed(herbs)).build();
        MethodRecommendation result = recommend("Herblore", 70, state);

        assertNotNull(result);
        assertEquals("method.herblore.banked-potions", result.getRecommended().getId());
        assertFalse(result.getAlternatives().isEmpty());
        assertEquals(MethodResourceStatus.SUFFICIENT, result.getResourceStatus());
    }

    @Test
    public void wildernessPreferenceAndHardcoreModeSelectSafePrayerMethod()
    {
        Map<Integer, Integer> bones = Collections.singletonMap(536, 20);
        AccountState normal = AccountState.builder().accountMode(AccountMode.IRONMAN).skill("Prayer", 40)
            .bank(BankSnapshot.observed(bones)).build();
        preferences.setPlaystyle(Playstyle.EFFICIENT);
        assertEquals("method.prayer.chaos-altar", recommend("Prayer", 43, normal).getRecommended().getId());

        preferences.setAvoidWilderness(true);
        assertEquals("method.prayer.gilded-altar", recommend("Prayer", 43, normal).getRecommended().getId());

        preferences.setAvoidWilderness(false);
        AccountState hardcore = AccountState.builder().accountMode(AccountMode.HARDCORE_IRONMAN).skill("Prayer", 40)
            .bank(BankSnapshot.observed(bones)).build();
        assertEquals("method.prayer.gilded-altar", recommend("Prayer", 43, hardcore).getRecommended().getId());
    }

    @Test
    public void unknownBankNeverInventsResourceAvailability()
    {
        AccountState state = AccountState.builder().skill("Herblore", 61).bank(BankSnapshot.unknown()).build();
        MethodRecommendation result = recommend("Herblore", 70, state);
        assertEquals(MethodResourceStatus.UNKNOWN, result.getResourceStatus());
        assertEquals("Resources unconfirmed — open your bank once if you want Iron Compass to include stored supplies.",
            result.getResourceSummary());
        assertFalse(result.getResourceSummary().toLowerCase().contains(" xp"));
    }

    @Test
    public void observedEmptyPartialSufficientAndCarriedResourcesRemainDistinct()
    {
        AccountState empty = craftingState(BankSnapshot.observed(Collections.emptyMap()));
        assertEquals(MethodResourceStatus.EMPTY, recommend("Crafting", 70, empty).getResourceStatus());

        AccountState partial = craftingState(BankSnapshot.observed(Collections.singletonMap(21504, 3)));
        assertEquals(MethodResourceStatus.PARTIAL, recommend("Crafting", 70, partial).getResourceStatus());

        Map<Integer, Integer> complete = new HashMap<>();
        complete.put(21504, 3);
        complete.put(1783, 18);
        AccountState sufficient = craftingState(BankSnapshot.observed(complete));
        assertEquals(MethodResourceStatus.SUFFICIENT, recommend("Crafting", 70, sufficient).getResourceStatus());

        AccountState carried = AccountState.builder().skill("Crafting", 61).skill("Magic", 77)
            .quest("Lunar Diplomacy", QuestProgress.FINISHED).bank(BankSnapshot.unknown())
            .inventoryItem(21504, 3).inventoryItem(1783, 18).build();
        assertEquals(MethodResourceStatus.SUFFICIENT, recommend("Crafting", 70, carried).getResourceStatus());
    }

    private MethodRecommendation recommend(String skill, int target, AccountState state)
    {
        PlannedAction action = new PlannedAction(PlannedAction.Kind.REQUIREMENT, "Train " + skill,
            "Test requirement", null, null, skill, target);
        return planner.recommend(catalog, action, state, preferences, Collections.emptyList());
    }

    private static AccountState craftingState(BankSnapshot bank)
    {
        return AccountState.builder().skill("Crafting", 61).skill("Magic", 77)
            .quest("Lunar Diplomacy", QuestProgress.FINISHED).bank(bank).build();
    }
}
