package com.ironpath.ui;

import com.google.gson.Gson;
import com.ironpath.IronPathConfig;
import com.ironpath.gear.GearCatalog;
import com.ironpath.gear.GearLoader;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearRecommendationService;
import com.ironpath.gear.InMemoryGearPreferenceStore;
import com.ironpath.integration.QuestHelperBridge;
import com.ironpath.integration.WikiBridge;
import com.ironpath.persistence.InMemoryManualOverrideStore;
import com.ironpath.persistence.ManualOverride;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.route.Route;
import com.ironpath.route.RouteEvaluator;
import com.ironpath.route.RouteLoader;
import com.ironpath.route.RouteProjection;
import com.ironpath.route.RouteSection;
import com.ironpath.route.RouteStep;
import com.ironpath.state.AccountMode;
import com.ironpath.state.AccountState;
import com.ironpath.state.BankSnapshot;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SyntheticProfileRenderTest
{
    private final Gson gson = new Gson();
    private final ConditionEvaluator conditions = new ConditionEvaluator();

    @Test
    public void rendersRequestedSyntheticProfilesAtRuneLiteSidebarWidth() throws Exception
    {
        Route route = new RouteLoader(gson).loadResource("/routes/efficient-ironman.json");

        AccountState freshUnknown = AccountState.builder().accountMode(AccountMode.IRONMAN).build();
        InMemoryManualOverrideStore freshOverrides = new InMemoryManualOverrideStore();
        render("profile-a-overview-fresh.png", route, freshUnknown, freshOverrides, panel -> { });
        render("profile-e-bank-unknown.png", route, freshUnknown, freshOverrides,
            panel -> panel.showGearForTesting("ALL"));

        InMemoryManualOverrideStore manualOverrides = new InMemoryManualOverrideStore();
        completeFirst(route, manualOverrides, 10);
        render("profile-f-manual-confirmation.png", route, freshUnknown, manualOverrides, panel -> { });

        AccountState midgame = skilledState(BankSnapshot.observed(Collections.emptyMap()));
        InMemoryManualOverrideStore midOverrides = new InMemoryManualOverrideStore();
        completeFirst(route, midOverrides, 228);
        render("profile-b-overview-midgame.png", route, midgame, midOverrides, panel -> { });
        render("profile-c-path-current-chapter.png", route, midgame, midOverrides,
            IronPathPanel::showPathForTesting);
        render("profile-d-gear-melee.png", route, midgame, midOverrides,
            panel -> panel.showGearForTesting("MELEE"));
        render("profile-e-bank-scanned.png", route, midgame, midOverrides,
            panel -> panel.showGearForTesting("ALL"));

        AccountState scannedFresh = AccountState.builder().accountMode(AccountMode.IRONMAN)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        render("profile-e-gear-locked-detail.png", route, scannedFresh, new InMemoryManualOverrideStore(),
            panel -> panel.showGearObjectiveForTesting("gear.late.avernic"));
        AccountState available = AccountState.builder().accountMode(AccountMode.IRONMAN)
            .skill("Attack", 40).skill("Strength", 40)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        render("profile-e-gear-available-detail.png", route, available, new InMemoryManualOverrideStore(),
            panel -> panel.showGearObjectiveForTesting("gear.early.strength-body"));
    }

    private void render(String name, Route route, AccountState state, InMemoryManualOverrideStore overrides,
                        Consumer<IronPathPanel> view) throws Exception
    {
        RouteProjection routeProjection = new RouteEvaluator(conditions).evaluate(route, state, overrides, true, 4, 7);
        GearCatalog catalog = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        GearProjection gearProjection = new GearRecommendationService(conditions).evaluate(catalog, state,
            new InMemoryGearPreferenceStore(), overrides);
        BufferedImage image = new BufferedImage(242, 900, BufferedImage.TYPE_INT_ARGB);
        SwingUtilities.invokeAndWait(() ->
        {
            IronPathPanel panel = new IronPathPanel(new IronPathConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.setSize(new Dimension(242, 900));
            panel.update(state, routeProjection, gearProjection, null, null);
            view.accept(panel);
            for (int pass = 0; pass < 4; pass++) layoutRecursively(panel);
            Graphics2D graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
        });

        File report = new File("build/reports/progression-ux/" + name);
        assertTrue(report.getParentFile().mkdirs() || report.getParentFile().isDirectory());
        assertTrue(ImageIO.write(image, "png", report));
        assertTrue(report.getName(), distinctColorCount(image) > 8);
    }

    private static AccountState skilledState(BankSnapshot bank)
    {
        AccountState.Builder state = AccountState.builder().accountMode(AccountMode.IRONMAN).bank(bank);
        String[] skills = {"Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft",
            "Construction", "Hitpoints", "Agility", "Herblore", "Thieving", "Crafting", "Fletching",
            "Slayer", "Hunter", "Mining", "Smithing", "Fishing", "Cooking", "Firemaking", "Woodcutting",
            "Farming", "Sailing"};
        for (String skill : skills) state.skill(skill, 75);
        return state.build();
    }

    private static void completeFirst(Route route, InMemoryManualOverrideStore overrides, int count)
    {
        int marked = 0;
        for (RouteSection section : route.getSections())
        {
            for (RouteStep step : section.getSteps())
            {
                if (marked++ >= count) return;
                overrides.put(step.getId(), ManualOverride.FORCE_COMPLETE);
            }
        }
    }

    private static void layoutRecursively(Container container)
    {
        container.doLayout();
        for (java.awt.Component child : container.getComponents())
        {
            if (child instanceof Container) layoutRecursively((Container) child);
        }
    }

    private static int distinctColorCount(BufferedImage image)
    {
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 4)
            for (int x = 0; x < image.getWidth(); x += 4) colors.add(image.getRGB(x, y));
        return colors.size();
    }
}
