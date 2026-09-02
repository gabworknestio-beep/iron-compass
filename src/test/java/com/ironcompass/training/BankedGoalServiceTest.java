package com.ironcompass.training;

import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class BankedGoalServiceTest
{
    private final BankedGoalService service = new BankedGoalService();

    @Test
    public void unopenedBankStaysUnknown()
    {
        AccountState state = AccountState.builder().skill("Cooking", 70).bank(BankSnapshot.unknown()).build();

        BankedGoalProjection projection = service.project(state, "Cooking", 80);

        assertEquals(BankedGoalStatus.UNKNOWN, projection.getStatus());
        assertTrue(projection.getExplanation().startsWith("Open your bank"));
    }

    @Test
    public void recognizedFoodCanCoverSelectedTarget()
    {
        AccountState state = AccountState.builder().skill("Cooking", 70)
            .skillExperience("Cooking", MethodPlannerService.xpForLevel(70))
            .bank(bank(ItemID.TBWT_RAW_KARAMBWAN, 1_000)).build();

        BankedGoalProjection projection = service.project(state, "Cooking", 71);

        assertEquals(BankedGoalStatus.READY, projection.getStatus());
        assertEquals(190_000, projection.getRecognizedXp());
        assertEquals(100, projection.getProgressPercent());
        assertTrue(projection.getProjectedLevel() >= 71);
        assertEquals("Cook Karambwans", projection.getBreakdown().get(0).getLabel());
    }

    @Test
    public void herbloreIsLimitedByObservedSecondaries()
    {
        Map<Integer, Integer> items = new HashMap<>();
        items.put(ItemID.RANARRVIAL, 100);
        items.put(ItemID.SNAPE_GRASS, 25);
        AccountState state = AccountState.builder().skill("Herblore", 63)
            .bank(BankSnapshot.observed(items, 123L)).build();

        BankedGoalProjection projection = service.project(state, "Herblore", 70);

        assertEquals(BankedGoalStatus.IN_PROGRESS, projection.getStatus());
        assertEquals(2_187, projection.getRecognizedXp());
        assertEquals(25, projection.getBreakdown().get(0).getActions());
    }

    @Test
    public void higherXpRecipeGetsSharedSecondaryFirst()
    {
        Map<Integer, Integer> items = new HashMap<>();
        items.put(ItemID.GUAMVIAL, 10);
        items.put(ItemID.IRITVIAL, 10);
        items.put(ItemID.EYE_OF_NEWT, 5);
        AccountState state = AccountState.builder().skill("Herblore", 50)
            .bank(BankSnapshot.observed(items, 123L)).build();

        BankedGoalProjection projection = service.project(state, "Herblore", 60);

        assertEquals(500, projection.getRecognizedXp());
        assertEquals("Super attack potions", projection.getBreakdown().get(0).getLabel());
    }

    @Test
    public void resourcesAboveCurrentLevelAreNotPromised()
    {
        AccountState state = AccountState.builder().skill("Cooking", 70)
            .bank(bank(ItemID.RAW_SHARK, 1_000)).build();

        BankedGoalProjection projection = service.project(state, "Cooking", 85);

        assertEquals(BankedGoalStatus.NO_RESOURCES, projection.getStatus());
        assertEquals(0, projection.getRecognizedXp());
    }

    @Test
    public void activityDrivenSkillIsNotGivenFakeBankedXp()
    {
        AccountState state = AccountState.builder().skill("Hunter", 70)
            .bank(BankSnapshot.observed(new HashMap<>(), 123L)).build();

        BankedGoalProjection projection = service.project(state, "Hunter", 80);

        assertEquals(BankedGoalStatus.NOT_SUPPORTED, projection.getStatus());
        assertTrue(projection.getExplanation().contains("does not invent"));
    }

    private static BankSnapshot bank(int itemId, int quantity)
    {
        Map<Integer, Integer> items = new HashMap<>();
        items.put(itemId, quantity);
        return BankSnapshot.observed(items, 123L);
    }
}
