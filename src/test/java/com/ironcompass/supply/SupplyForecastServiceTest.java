package com.ironcompass.supply;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
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
