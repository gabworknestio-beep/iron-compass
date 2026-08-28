package com.ironcompass.gear;

import com.google.gson.Gson;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.state.QuestProgress;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GearRecommendationServiceTest
{
    private final GearRecommendationService service = new GearRecommendationService(new ConditionEvaluator());

    @Test
    public void lockedBowfaNeverDisplacesReachableEarlyUpgrade() throws Exception
    {
        GearCatalog catalog = catalog();
        AccountState state = AccountState.builder().accountMode(AccountMode.IRONMAN)
            .skill("Attack", 60).skill("Strength", 60).skill("Ranged", 20).skill("Agility", 1)
            .quest("Monkey Madness I", QuestProgress.FINISHED)
            .quest("Song of the Elves", QuestProgress.NOT_STARTED)
            .bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build();
        GearProjection projection = service.evaluate(catalog, state, new InMemoryGearPreferenceStore(),
            new InMemoryManualOverrideStore());

        assertNotNull(projection.getRecommended());
        assertNotEquals("gear.mid.bowfa", projection.getRecommended().getUpgrade().getId());
        assertEquals(GearStatus.LOCKED, projection.find("gear.mid.bowfa").getStatus());
    }

    @Test
    public void detectedDescendantCompletesEarlierMilestone() throws Exception
    {
        AccountState state = AccountState.builder().equipmentItem(22322, 1)
            .bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build();
        GearProjection projection = service.evaluate(catalog(), state, new InMemoryGearPreferenceStore(),
            new InMemoryManualOverrideStore());
        assertEquals(GearStatus.OWNED, projection.find("gear.early.defender").getStatus());
    }

    @Test
    public void chosenAlternativeBecomesRecommendation() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.chooseAlternative("gear.early.rune-crossbow", "gear.early.sunlight-crossbow");
        AccountState state = AccountState.builder().skill("Hunter", 72).skill("Fletching", 74)
            .bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build();
        GearProjection projection = service.evaluate(catalog(), state, preferences,
            new InMemoryManualOverrideStore());
        assertEquals("gear.early.sunlight-crossbow", projection.getRecommended().getUpgrade().getId());
    }

    @Test
    public void unknownOwnershipIsUnconfirmedAndNeverAutoRecommended() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        GearProjection projection = service.evaluate(catalog(), AccountState.builder()
            .skill("Attack", 99).skill("Strength", 99).bank(BankSnapshot.unknown()).build(),
            preferences, new InMemoryManualOverrideStore());

        assertEquals(GearStatus.UNCONFIRMED, projection.find("gear.early.defender").getStatus());
        assertNotEquals("gear.early.defender", projection.getRecommended() == null ? null
            : projection.getRecommended().getUpgrade().getId());

        preferences.setSelectedGoalId("gear.early.defender");
        projection = service.evaluate(catalog(), AccountState.builder()
            .skill("Attack", 99).skill("Strength", 99).bank(BankSnapshot.unknown()).build(),
            preferences, new InMemoryManualOverrideStore());
        assertEquals("gear.early.defender", projection.getSelected().getUpgrade().getId());
    }

    @Test
    public void skippingSelectedGoalClearsItAndUnskipDoesNotRestoreIt() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("gear.early.defender");
        preferences.setSkipped("gear.early.defender", true);
        assertNull(preferences.getSelectedGoalId());

        preferences.setSkipped("gear.early.defender", false);
        GearProjection projection = service.evaluate(catalog(), AccountState.builder()
            .skill("Attack", 99).skill("Strength", 99).bank(BankSnapshot.observed(Collections.emptyMap())).build(),
            preferences, new InMemoryManualOverrideStore());
        assertNull(projection.getSelected());
    }

    @Test
    public void observedOwnershipDistinguishesAbsentCarriedAndBankedItems() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        AccountState absent = AccountState.builder().skill("Attack", 65).skill("Strength", 65)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        assertEquals(GearStatus.RECOMMENDED,
            service.evaluate(catalog(), absent, preferences, overrides).find("gear.early.defender").getStatus());
        assertEquals(GearStatus.OWNED, service.evaluate(catalog(), AccountState.builder()
            .inventoryItem(12954, 1).bank(BankSnapshot.unknown()).build(), preferences, overrides)
            .find("gear.early.defender").getStatus());
        assertEquals(GearStatus.OWNED, service.evaluate(catalog(), AccountState.builder()
            .equipmentItem(12954, 1).bank(BankSnapshot.unknown()).build(), preferences, overrides)
            .find("gear.early.defender").getStatus());
        assertEquals(GearStatus.OWNED, service.evaluate(catalog(), AccountState.builder()
            .bank(BankSnapshot.observed(Map.of(12954, 1))).build(), preferences, overrides)
            .find("gear.early.defender").getStatus());
    }

    @Test
    public void slayerHelmetGoalOnlyAcceptsImbuedHelmetIds() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        assertEquals(GearStatus.LOCKED, service.evaluate(catalog(), AccountState.builder()
            .inventoryItem(8921, 1).bank(BankSnapshot.observed(Collections.emptyMap())).build(),
            preferences, overrides).find("gear.mid.slayer-helm").getStatus());
        assertEquals(GearStatus.LOCKED, service.evaluate(catalog(), AccountState.builder()
            .inventoryItem(11864, 1).bank(BankSnapshot.observed(Collections.emptyMap())).build(),
            preferences, overrides).find("gear.mid.slayer-helm").getStatus());
        assertEquals(GearStatus.OWNED, service.evaluate(catalog(), AccountState.builder()
            .inventoryItem(11865, 1).bank(BankSnapshot.observed(Collections.emptyMap())).build(),
            preferences, overrides).find("gear.mid.slayer-helm").getStatus());
        assertEquals(GearStatus.OWNED, service.evaluate(catalog(), AccountState.builder()
            .inventoryItem(26675, 1).bank(BankSnapshot.observed(Collections.emptyMap())).build(),
            preferences, overrides).find("gear.mid.slayer-helm").getStatus());
    }

    private static GearCatalog catalog() throws Exception
    {
        GearCatalog catalog = new GearLoader(new Gson()).loadResource("/gear/ironman-gear-2026.json");
        new GearValidator().validate(catalog);
        return catalog;
    }
}
