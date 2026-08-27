package com.ironpath.planner;

import com.google.gson.Gson;
import com.ironpath.gear.GearCatalog;
import com.ironpath.gear.GearLoader;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearRecommendationService;
import com.ironpath.gear.InMemoryGearPreferenceStore;
import com.ironpath.persistence.InMemoryManualOverrideStore;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.state.AccountState;
import com.ironpath.state.BankSnapshot;
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
