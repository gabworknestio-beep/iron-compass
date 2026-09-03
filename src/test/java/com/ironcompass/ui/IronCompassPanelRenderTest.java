package com.ironcompass.ui;

import com.google.gson.Gson;
import com.ironcompass.IronCompassConfig;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalDependencyResolver;
import com.ironcompass.goal.GoalLoader;
import com.ironcompass.integration.QuestHelperBridge;
import com.ironcompass.integration.WikiBridge;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.persistence.ManualOverride;
import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.planner.GoalPlannerService;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteEvaluator;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.route.RouteSection;
import com.ironcompass.route.RouteStep;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.supply.SupplyForecastService;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IronCompassPanelRenderTest
{
    @Test
    public void persistentNavigationIsKeyboardAndScreenReaderIdentifiable() throws Exception
    {
        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), new InMemoryManualOverrideStore(), () -> { });
            JButton overview = findButton(panel, "OVERVIEW");
            JButton path = findButton(panel, "PATH");
            JButton gear = findButton(panel, "GEAR");

            assertNotNull(overview);
            assertNotNull(path);
            assertNotNull(gear);
            assertTrue(overview.isFocusable());
            assertTrue(path.isFocusable());
            assertTrue(gear.isFocusable());
            assertEquals("OVERVIEW view", overview.getAccessibleContext().getAccessibleName());
            assertEquals("PATH view", path.getAccessibleContext().getAccessibleName());
            assertEquals("GEAR view", gear.getAccessibleContext().getAccessibleName());
            assertTrue("Selected navigation remains actionable", overview.isEnabled());
            assertEquals(Boolean.TRUE, overview.getClientProperty(PremiumButtonUI.SELECTED_PROPERTY));
            assertEquals(UiComponents.ButtonStyle.NAVIGATION,
                overview.getClientProperty(PremiumButtonUI.STYLE_PROPERTY));

            path.doClick();
            assertTrue("Path remains enabled when selected", path.isEnabled());
            assertEquals(Boolean.TRUE, path.getClientProperty(PremiumButtonUI.SELECTED_PROPERTY));
            assertEquals(Boolean.FALSE, overview.getClientProperty(PremiumButtonUI.SELECTED_PROPERTY));
            assertTrue(overview.isEnabled());
            assertTrue(gear.isEnabled());
        });
    }

    @Test
    public void normalSidebarWidthRendersWithoutException() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        AccountState state = AccountState.builder().accountMode(AccountMode.IRONMAN).build();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        RouteProjection projection = new RouteEvaluator(new ConditionEvaluator())
            .evaluate(route, state, overrides, true, 4, 7);
        GearProjection gearProjection = gearProjection(state, overrides);
        BufferedImage image = new BufferedImage(242, 800, BufferedImage.TYPE_INT_ARGB);

        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.setSize(new Dimension(242, 800));
            panel.update(state, projection, gearProjection, null, null);
            for (int pass = 0; pass < 3; pass++)
            {
                layoutRecursively(panel);
            }
            assertNoHorizontalScrollbars(panel);
            Graphics2D graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
        });

        File report = new File("build/reports/iron-compass-panel.png");
        assertTrue(report.getParentFile().mkdirs() || report.getParentFile().isDirectory());
        assertTrue(ImageIO.write(image, "png", report));
        assertTrue(image.getWidth() == 242 && image.getHeight() == 800);
        assertTrue("Render must contain UI content, not only the panel background", distinctColorCount(image) > 8);
    }

    @Test
    public void accountRefreshKeepsRouteBrowserOpen() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        AccountState state = AccountState.builder()
            .loggedIn(true)
            .accountMode(AccountMode.IRONMAN)
            .build();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        RouteProjection projection = new RouteEvaluator(new ConditionEvaluator())
            .evaluate(route, state, overrides, true, 4, 7);
        GearProjection gearProjection = gearProjection(state, overrides);

        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.update(state, projection, gearProjection, null, null);

            JButton browse = findButton(panel, "PATH");
            assertTrue("Browse Route button should exist", browse != null);
            browse.doClick();

            JTextField search = findTextField(panel);
            assertTrue("Route browser should be visible after clicking Browse Route",
                search != null && search.getParent().getParent().isVisible());

            panel.update(state, projection, gearProjection, null, null);

            assertTrue("Account refresh must not return the user to the home card",
                search.getParent().getParent().isVisible());
        });
    }

    @Test
    public void pathRowsOpenCompactStepDetails() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        AccountState state = AccountState.builder().accountMode(AccountMode.IRONMAN).build();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        RouteProjection projection = new RouteEvaluator(new ConditionEvaluator())
            .evaluate(route, state, overrides, true, 4, 7);
        GearProjection gearProjection = gearProjection(state, overrides);

        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.update(state, projection, gearProjection, null, null);
            panel.showPathForTesting();
            JButton view = findVisibleButton(panel, "›");
            assertTrue("Current chapter rows should expose a detail drill-down", view != null);
            view.doClick();
            assertTrue("Step detail should provide a Back action", findVisibleButton(panel, "BACK") != null);
            assertTrue("Step detail should preserve route management", findVisibleButton(panel, "MANAGE") != null);
        });
    }

    @Test
    public void manualMilestoneHasDirectDoneAction() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        AccountState state = AccountState.builder().accountMode(AccountMode.IRONMAN).build();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        String targetId = "efficient-ironman.011.stronghold-of-security";
        outer:
        for (RouteSection section : route.getSections())
        {
            for (RouteStep step : section.getSteps())
            {
                if (targetId.equals(step.getId())) break outer;
                overrides.put(step.getId(), ManualOverride.FORCE_COMPLETE);
            }
        }
        RouteProjection projection = new RouteEvaluator(new ConditionEvaluator())
            .evaluate(route, state, overrides, true, 4, 7);
        GearProjection gearProjection = gearProjection(state, overrides);

        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.update(state, projection, gearProjection, null, null);
            JButton done = findVisibleButton(panel, "DONE");
            assertTrue("Manual milestones should expose a direct confirmation action", done != null);
            done.doClick();
            assertEquals(ManualOverride.FORCE_COMPLETE, overrides.get(targetId));
        });
    }

    @Test
    public void gearRoadmapRendersAtNormalSidebarWidth() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        AccountState state = AccountState.builder()
            .accountMode(AccountMode.IRONMAN)
            .bank(com.ironcompass.state.BankSnapshot.observed(java.util.Map.of()))
            .build();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        RouteEvaluator evaluator = new RouteEvaluator(new ConditionEvaluator());
        RouteProjection projection = evaluator.evaluate(route, state, overrides, true, 4, 7);
        GearProjection gearProjection = gearProjection(state, overrides);
        BufferedImage image = new BufferedImage(242, 900, BufferedImage.TYPE_INT_ARGB);

        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.setSize(new Dimension(242, 900));
            panel.update(state, projection, gearProjection, null, null);
            findButton(panel, "GEAR").doClick();
            for (int pass = 0; pass < 3; pass++) layoutRecursively(panel);
            Graphics2D graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
        });

        File report = new File("build/reports/iron-compass-gear-roadmap.png");
        assertTrue(report.getParentFile().mkdirs() || report.getParentFile().isDirectory());
        assertTrue(ImageIO.write(image, "png", report));
        assertTrue("Gear render must contain UI content", distinctColorCount(image) > 8);
    }

    @Test
    public void gearDetailRendersAtNormalSidebarWidth() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        AccountState state = AccountState.builder()
            .accountMode(AccountMode.IRONMAN)
            .bank(com.ironcompass.state.BankSnapshot.observed(java.util.Map.of()))
            .build();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        RouteProjection projection = new RouteEvaluator(new ConditionEvaluator())
            .evaluate(route, state, overrides, true, 4, 7);
        GearProjection gearProjection = gearProjection(state, overrides);
        BufferedImage image = new BufferedImage(242, 900, BufferedImage.TYPE_INT_ARGB);

        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.setSize(new Dimension(242, 900));
            panel.update(state, projection, gearProjection, null, null);
            findButton(panel, "GEAR").doClick();
            JButton details = findVisibleButton(panel, "DETAILS");
            assertTrue("Recommended gear details button should be visible", details != null);
            details.doClick();
            assertTrue("Gear detail must expose the goal action", findVisibleButton(panel, "SET AS GOAL") != null);
            assertTrue("Gear detail must keep low-frequency ownership controls in Manage",
                findVisibleButton(panel, "MANAGE") != null);
            for (int pass = 0; pass < 3; pass++) layoutRecursively(panel);
            Graphics2D graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
        });

        File report = new File("build/reports/iron-compass-gear-detail.png");
        assertTrue(report.getParentFile().mkdirs() || report.getParentFile().isDirectory());
        assertTrue(ImageIO.write(image, "png", report));
        assertTrue("Gear detail render must contain UI content", distinctColorCount(image) > 8);
    }

    @Test
    public void accountRefreshKeepsGearRoadmapOpen() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        AccountState state = AccountState.builder().accountMode(AccountMode.IRONMAN).build();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        RouteEvaluator evaluator = new RouteEvaluator(new ConditionEvaluator());
        RouteProjection projection = evaluator.evaluate(route, state, overrides, true, 4, 7);
        GearProjection gearProjection = gearProjection(state, overrides);

        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.update(state, projection, gearProjection, null, null);

            JButton gear = findButton(panel, "GEAR");
            assertTrue("Gear Roadmap button should exist", gear != null);
            gear.doClick();
            JButton back = findVisibleButton(panel, "OVERVIEW");
            assertTrue("Gear Roadmap should be visible after opening it", back != null);

            panel.update(state, projection, gearProjection, null, null);
            assertTrue("Account refresh must not return the user to the home card",
                findVisibleButton(panel, "OVERVIEW") != null);
        });
    }

    @Test
    public void accountInsightsRenderFromTheSameGoalSnapshot() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        GoalCatalog goals = new GoalLoader(new Gson()).loadResource("/goals/ironman-goals-2026.json");
        AccountState state = AccountState.builder().accountMode(AccountMode.IRONMAN)
            .bank(com.ironcompass.state.BankSnapshot.observed(java.util.Map.of())).build();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setPrimaryGoalId("goal.unlock.piety");
        RouteProjection projection = new RouteEvaluator(new ConditionEvaluator())
            .evaluate(route,state,overrides,true,4,7);
        GearProjection gear = new GearRecommendationService(new ConditionEvaluator())
            .evaluate(new GearLoader(new Gson()).loadResource("/gear/ironman-gear-2026.json"),
                state,preferences,overrides);
        GoalPlanProjection plan = new GoalPlannerService(new ConditionEvaluator(),new GoalDependencyResolver(),
            new SupplyForecastService()).evaluate(goals,state,gear,projection,preferences,overrides);

        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { },new WikiBridge(),null,
                new QuestHelperBridge(),overrides,() -> { });
            panel.update(state,projection,gear,null,null,plan,null);
            assertNotNull(findVisibleButton(panel,"VIEW ACCOUNT INSIGHTS"));
            assertNotNull(findLabel(panel, "ACCOUNT OVERVIEW"));
            assertNotNull(findLabel(panel, "UNLOCK SOON"));
            assertNotNull(findLabel(panel, "GOAL PACKS"));
            JPanel content = panel.accountInsightsContentForTesting();
            assertNotNull(findButton(content, "DONE"));
            assertNotNull(findLabel(content, "ACCOUNT HEALTH"));
            assertNotNull(findLabel(content, "GOAL PACKS"));
            JPanel picker = panel.goalPickerContentForTesting();
            assertNotNull(findButton(picker, "SET PRIMARY"));
            assertNotNull(findButton(picker, "ADD SECONDARY"));
        });
    }

    private static GearProjection gearProjection(AccountState state, InMemoryManualOverrideStore overrides)
        throws Exception
    {
        GearCatalog catalog = new GearLoader(new Gson()).loadResource("/gear/ironman-gear-2026.json");
        return new GearRecommendationService(new ConditionEvaluator()).evaluate(catalog, state,
            new InMemoryGearPreferenceStore(), overrides);
    }

    private static void layoutRecursively(Container container)
    {
        container.doLayout();
        for (java.awt.Component child : container.getComponents())
        {
            if (child instanceof Container)
            {
                layoutRecursively((Container) child);
            }
        }
    }

    private static int distinctColorCount(BufferedImage image)
    {
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 4)
        {
            for (int x = 0; x < image.getWidth(); x += 4)
            {
                colors.add(image.getRGB(x, y));
            }
        }
        return colors.size();
    }

    private static JButton findButton(Container container, String text)
    {
        for (java.awt.Component child : container.getComponents())
        {
            if (child instanceof JButton && text.equals(((JButton) child).getText()))
            {
                return (JButton) child;
            }
            if (child instanceof Container)
            {
                JButton match = findButton((Container) child, text);
                if (match != null)
                {
                    return match;
                }
            }
        }
        return null;
    }

    private static JButton findVisibleButton(Container container, String text)
    {
        for (java.awt.Component child : container.getComponents())
        {
            if (child instanceof JButton && text.equals(((JButton) child).getText()) && child.isVisible())
            {
                return (JButton) child;
            }
            if (child instanceof Container && child.isVisible())
            {
                JButton match = findVisibleButton((Container) child, text);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static JTextField findTextField(Container container)
    {
        for (java.awt.Component child : container.getComponents())
        {
            if (child instanceof JTextField)
            {
                return (JTextField) child;
            }
            if (child instanceof Container)
            {
                JTextField match = findTextField((Container) child);
                if (match != null)
                {
                    return match;
                }
            }
        }
        return null;
    }

    private static javax.swing.JLabel findLabel(Container container, String text)
    {
        for (java.awt.Component child : container.getComponents())
        {
            if (child instanceof javax.swing.JLabel && text.equals(((javax.swing.JLabel) child).getText()))
                return (javax.swing.JLabel) child;
            if (child instanceof Container)
            {
                javax.swing.JLabel match = findLabel((Container) child, text);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static void assertNoHorizontalScrollbars(Container container)
    {
        for (java.awt.Component child : container.getComponents())
        {
            if (child instanceof JScrollPane)
            {
                assertEquals("242 px layouts must never expose a horizontal scrollbar",
                    javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                    ((JScrollPane) child).getHorizontalScrollBarPolicy());
            }
            if (child instanceof Container) assertNoHorizontalScrollbars((Container) child);
        }
    }

}
