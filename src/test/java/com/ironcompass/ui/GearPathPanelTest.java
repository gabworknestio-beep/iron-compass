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
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
            panel.setSize(new Dimension(242, 900));
            panel.update(state,gear);
            for (int pass = 0; pass < 4; pass++) layoutRecursively(panel);
            assertTrue(panel.styleFilterForTesting().isOpaque());
            assertTrue(panel.statusFilterForTesting().isOpaque());
            assertNotNull(panel.styleFilterForTesting().getBorder());
            assertTrue(panel.styleFilterForTesting().getUI().getClass().getName().contains("DarkComboBoxUI"));
            assertEquals(UiTokens.SURFACE_RAISED, panel.styleFilterForTesting().getBackground());
            assertTrue("Opening the popup must retain an arrow control",
                panel.styleFilterForTesting().getComponentCount() > 0);
            assertArrowInside(panel.styleFilterForTesting());
            panel.selectStyleForTesting("MELEE");
            assertEquals("MELEE",preferences.getGearStyleFilter());
            panel.selectStyleForTesting("ALL");
            assertEquals("ALL",preferences.getGearStyleFilter());
            panel.setSearchForTesting("dragon");
        });
    }

    private static void assertArrowInside(JComboBox<?> combo)
    {
        JButton arrow = null;
        for (java.awt.Component component : combo.getComponents())
            if (component instanceof JButton) arrow = (JButton) component;
        assertNotNull(arrow);
        assertTrue("Combo arrow must remain inside its visible bounds: combo=" + combo.getBounds()
                + ", arrow=" + arrow.getBounds(),
            arrow.getWidth() >= 10 && arrow.getHeight() > 0 && arrow.getX() >= 0
                && arrow.getX() + arrow.getWidth() <= combo.getWidth());
    }

    private static void layoutRecursively(Container container)
    {
        container.doLayout();
        for (java.awt.Component child : container.getComponents())
            if (child instanceof Container) layoutRecursively((Container) child);
    }
}
