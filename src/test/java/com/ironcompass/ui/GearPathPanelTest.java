package com.ironcompass.ui;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.integration.WikiBridge;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.state.AccountState;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class GearPathPanelTest
{
    @Test
    public void sharedDropdownStylingStaysOpaqueAndSelectionsStillPersist() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        AccountState state = AccountState.builder().loggedIn(true).build();
        GearCatalog catalog = new GearLoader(new Gson()).loadResource("/gear/ironman-gear-2026.json");
        GearProjection gear = new GearRecommendationService(new ConditionEvaluator())
            .evaluate(catalog,state,preferences,overrides);
        SwingUtilities.invokeAndWait(() ->
        {
            GearPathPanel panel = new GearPathPanel(new WikiBridge(),preferences,overrides,() -> { });
            panel.update(state,gear);
            assertTrue(panel.styleFilterForTesting().isOpaque());
            assertTrue(panel.statusFilterForTesting().isOpaque());
            assertNotNull(panel.styleFilterForTesting().getBorder());
            panel.selectStyleForTesting("MELEE");
            assertEquals("MELEE",preferences.getGearStyleFilter());
            panel.selectStyleForTesting("ALL");
            assertEquals("ALL",preferences.getGearStyleFilter());
            panel.setSearchForTesting("dragon");
        });
    }
}
