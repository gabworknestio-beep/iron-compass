package com.ironcompass.planner;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public final class UnlockRadarServiceTest
{
    @Test
    public void onlyReportsNewMeaningfulAvailabilityTransitions() throws Exception
    {
        GearCatalog catalog = new GearLoader(new Gson()).loadResource("/gear/ironman-gear-2026.json");
        GearRecommendationService recommendations = new GearRecommendationService(new ConditionEvaluator());
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        AccountState before = AccountState.builder().skill("Attack", 60).skill("Strength", 60)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        AccountState after = AccountState.builder().skill("Attack", 65).skill("Strength", 65)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        GearProjection beforeProjection = recommendations.evaluate(catalog, before, preferences, overrides);
        GearProjection afterProjection = recommendations.evaluate(catalog, after, preferences, overrides);
        UnlockRadarService radar = new UnlockRadarService();

        assertNull(radar.evaluate(beforeProjection, null));
        UnlockOpportunity opportunity = radar.evaluate(afterProjection, null);
        assertNotNull(opportunity);
        assertEquals("Dragon defender available", opportunity.getTitle());
        assertNull(radar.evaluate(afterProjection, null));
    }
}
