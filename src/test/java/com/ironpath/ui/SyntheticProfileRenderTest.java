package com.ironpath.ui;

import com.google.gson.Gson;
import com.ironpath.IronPathConfig;
import com.ironpath.gear.GearCatalog;
import com.ironpath.gear.GearLoader;
import com.ironpath.gear.GearPreferenceStore;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearRecommendationService;
import com.ironpath.gear.InMemoryGearPreferenceStore;
import com.ironpath.goal.GoalCatalog;
import com.ironpath.goal.GoalDependencyResolver;
import com.ironpath.goal.GoalLoader;
import com.ironpath.integration.QuestHelperBridge;
import com.ironpath.integration.WikiBridge;
import com.ironpath.persistence.InMemoryManualOverrideStore;
import com.ironpath.persistence.ManualOverride;
import com.ironpath.persistence.ManualOverrideStore;
import com.ironpath.planner.GoalPlanProjection;
import com.ironpath.planner.GoalPlannerService;
import com.ironpath.planner.InMemoryPlannerPreferenceStore;
import com.ironpath.planner.PlannerPreferenceStore;
import com.ironpath.planner.Playstyle;
import com.ironpath.planner.ProgressionRecommendationService;
import com.ironpath.planner.RecommendationProjection;
import com.ironpath.planner.SessionLength;
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
import com.ironpath.state.QuestProgress;
import com.ironpath.supply.SupplyForecastService;
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
        TestPreferences freshOverrides = new TestPreferences();
        render("profile-a-overview-fresh.png", route, freshUnknown, freshOverrides, panel -> { });
        render("profile-e-bank-unknown.png", route, freshUnknown, freshOverrides,
            panel -> panel.showGearForTesting("ALL"));

        TestPreferences manualOverrides = new TestPreferences();
        completeFirst(route, manualOverrides, 10);
        render("profile-f-manual-confirmation.png", route, freshUnknown, manualOverrides, panel -> { });

        AccountState midgame = skilledState(BankSnapshot.observed(Collections.emptyMap()));
        TestPreferences midOverrides = new TestPreferences();
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
        render("profile-e-gear-locked-detail.png", route, scannedFresh, new TestPreferences(),
            panel -> panel.showGearObjectiveForTesting("gear.late.avernic"));
        AccountState available = AccountState.builder().accountMode(AccountMode.IRONMAN)
            .skill("Attack", 40).skill("Strength", 40)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        render("profile-e-gear-available-detail.png", route, available, new TestPreferences(),
            panel -> panel.showGearObjectiveForTesting("gear.early.strength-body"));

        TestPreferences selectedGoal = new TestPreferences();
        selectedGoal.setSelectedGoalId("goal.quest.song-of-the-elves");
        AccountState songGoal = songGoalState(QuestProgress.NOT_STARTED);
        render("profile-g-goal-selected.png", route, songGoal, selectedGoal, panel -> { });
        render("profile-g-goal-long-text-density.png", route, songGoal, selectedGoal, panel -> { });

        TestPreferences completedGoal = new TestPreferences();
        completedGoal.setSelectedGoalId("goal.quest.song-of-the-elves");
        render("profile-h-goal-completed.png", route, songGoalState(QuestProgress.FINISHED), completedGoal,
            panel -> { });

        TestPreferences threeRecommendations = new TestPreferences();
        threeRecommendations.setSelectedGoalId("gear.early.defender");
        render("profile-i-three-recommendations.png", route, midgame, threeRecommendations, panel -> { });
        render("profile-j-path-search.png", route, midgame, new TestPreferences(),
            panel -> panel.showPathSearchForTesting("Song of the Elves"));
        render("profile-k-gear-search.png", route, midgame, new TestPreferences(),
            panel -> panel.showGearSearchForTesting("slayer"));
    }

    private void render(String name, Route route, AccountState state, TestPreferences overrides,
                        Consumer<IronPathPanel> view) throws Exception
    {
        RouteProjection routeProjection = new RouteEvaluator(conditions).evaluate(route, state, overrides, true, 4, 7);
        GearCatalog catalog = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        GearProjection gearProjection = new GearRecommendationService(conditions).evaluate(catalog, state,
            overrides, overrides);
        GoalCatalog goals = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        SupplyForecastService supplies = new SupplyForecastService();
        GoalPlanProjection goalPlan = new GoalPlannerService(conditions, new GoalDependencyResolver(), supplies)
            .evaluate(goals, state, gearProjection, routeProjection, overrides);
        RecommendationProjection recommendations = new ProgressionRecommendationService().evaluate(
            routeProjection, gearProjection, goalPlan, state, overrides);
        BufferedImage image = new BufferedImage(242, 900, BufferedImage.TYPE_INT_ARGB);
        SwingUtilities.invokeAndWait(() ->
        {
            IronPathPanel panel = new IronPathPanel(new IronPathConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.setSize(new Dimension(242, 900));
            panel.update(state, routeProjection, gearProjection, null, null, goalPlan, recommendations);
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

    private static AccountState songGoalState(QuestProgress song)
    {
        return AccountState.builder().accountMode(AccountMode.IRONMAN)
            .bank(BankSnapshot.observed(Collections.emptyMap()))
            .quest("Song of the Elves", song)
            .quest("Mourning's End Part II", QuestProgress.FINISHED)
            .quest("Making History", QuestProgress.FINISHED)
            .quest("Druidic Ritual", QuestProgress.FINISHED)
            .skill("Agility", 70).skill("Construction", 70).skill("Farming", 69).skill("Herblore", 61)
            .skill("Hunter", 70).skill("Mining", 70).skill("Smithing", 70).skill("Woodcutting", 70)
            .build();
    }

    private static void completeFirst(Route route, ManualOverrideStore overrides, int count)
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

    private static final class TestPreferences
        implements ManualOverrideStore, GearPreferenceStore, PlannerPreferenceStore
    {
        private final InMemoryManualOverrideStore manual = new InMemoryManualOverrideStore();
        private final InMemoryGearPreferenceStore gear = new InMemoryGearPreferenceStore();
        private final InMemoryPlannerPreferenceStore planner = new InMemoryPlannerPreferenceStore();

        @Override public ManualOverride get(String id) { return manual.get(id); }
        @Override public void put(String id, ManualOverride value) { manual.put(id, value); }
        @Override public void remove(String id) { manual.remove(id); }
        @Override public void clear() { manual.clear(); }
        @Override public java.util.Map<String, ManualOverride> snapshot() { return manual.snapshot(); }
        @Override public String getSelectedGoalId() { return gear.getSelectedGoalId(); }
        @Override public void setSelectedGoalId(String id) { gear.setSelectedGoalId(id); }
        @Override public boolean isSkipped(String id) { return gear.isSkipped(id); }
        @Override public void setSkipped(String id, boolean value) { gear.setSkipped(id, value); }
        @Override public boolean isMarkedOptional(String id) { return gear.isMarkedOptional(id); }
        @Override public void setMarkedOptional(String id, boolean value) { gear.setMarkedOptional(id, value); }
        @Override public String getChosenAlternative(String id) { return gear.getChosenAlternative(id); }
        @Override public void chooseAlternative(String id, String alternative)
        {
            gear.chooseAlternative(id, alternative);
        }
        @Override public String getGearStyleFilter() { return gear.getGearStyleFilter(); }
        @Override public void setGearStyleFilter(String value) { gear.setGearStyleFilter(value); }
        @Override public String getGearStatusFilter() { return gear.getGearStatusFilter(); }
        @Override public void setGearStatusFilter(String value) { gear.setGearStatusFilter(value); }
        @Override public void resetGearPreferences() { gear.resetGearPreferences(); }
        @Override public Playstyle getPlaystyle() { return planner.getPlaystyle(); }
        @Override public void setPlaystyle(Playstyle value) { planner.setPlaystyle(value); }
        @Override public boolean isAvoidWilderness() { return planner.isAvoidWilderness(); }
        @Override public void setAvoidWilderness(boolean value) { planner.setAvoidWilderness(value); }
        @Override public SessionLength getSessionLength() { return planner.getSessionLength(); }
        @Override public void setSessionLength(SessionLength value) { planner.setSessionLength(value); }
    }
}
