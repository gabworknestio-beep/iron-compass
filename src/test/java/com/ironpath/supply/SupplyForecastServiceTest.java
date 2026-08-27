package com.ironpath.supply;

import com.google.gson.Gson;
import com.ironpath.gear.GearCatalog;
import com.ironpath.gear.GearLoader;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearRecommendationService;
import com.ironpath.gear.GearStatus;
import com.ironpath.gear.InMemoryGearPreferenceStore;
import com.ironpath.persistence.InMemoryManualOverrideStore;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.requirement.TruthValue;
import com.ironpath.state.AccountState;
import com.ironpath.state.BankSnapshot;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SupplyForecastServiceTest
{
    @Test
    public void countsPotionDosesAcrossCarriedAndObservedBank() throws Exception
    {
        GearProjection gear = projection();
        Map<Integer, Integer> bank = new HashMap<>();
        bank.put(2434, 1);
        AccountState state = AccountState.builder().inventoryItem(139, 2)
            .bank(BankSnapshot.observed(bank, 1L)).build();
        SupplyLine line = new SupplyForecastService().evaluate(gear.getRecommended(), state).getLines().get(0);
        assertEquals(10, line.getActualUnits());
        assertEquals(TruthValue.TRUE, line.getStatus());
    }

    @Test
    public void unopenedBankProducesUnknownInsteadOfZeroClaim() throws Exception
    {
        SupplyLine line = new SupplyForecastService().evaluate(projection().getRecommended(),
            AccountState.builder().build()).getLines().get(0);
        assertEquals(TruthValue.UNKNOWN, line.getStatus());
    }

    @Test
    public void skippedGoalNeverProducesSupplyForecast() throws Exception
    {
        assertNull(new SupplyForecastService().evaluate(
            projection().getRecommended().withStatus(GearStatus.SKIPPED), AccountState.builder().build()));
    }

    private static GearProjection projection() throws Exception
    {
        String json = "{\"version\":1,\"upgrades\":[{\"id\":\"g\",\"name\":\"Goal\",\"styles\":[\"PRAYER\"],"
            + "\"why\":\"why\",\"completion\":{\"type\":\"ITEM_PRESENT\",\"itemId\":999},"
            + "\"supplies\":[{\"name\":\"Prayer restore\",\"requiredUnits\":10,\"unitLabel\":\"doses\","
            + "\"variants\":[{\"itemId\":2434,\"units\":4},{\"itemId\":139,\"units\":3}]}]}]}";
        GearCatalog catalog = new GearLoader(new Gson()).load(new StringReader(json), "supply");
        return new GearRecommendationService(new ConditionEvaluator()).evaluate(catalog,
            AccountState.builder().bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build(),
            new InMemoryGearPreferenceStore(), new InMemoryManualOverrideStore());
    }
}
