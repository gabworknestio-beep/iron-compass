package com.ironpath.gear;

import com.google.gson.Gson;
import com.ironpath.persistence.InMemoryManualOverrideStore;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.state.AccountMode;
import com.ironpath.state.AccountState;
import com.ironpath.state.BankSnapshot;
import com.ironpath.state.QuestProgress;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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

    private static GearCatalog catalog() throws Exception
    {
        GearCatalog catalog = new GearLoader(new Gson()).loadResource("/gear/ironman-gear-2026.json");
        new GearValidator().validate(catalog);
        return catalog;
    }
}
