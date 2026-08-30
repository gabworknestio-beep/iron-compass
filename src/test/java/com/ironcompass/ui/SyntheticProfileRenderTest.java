package com.ironcompass.ui;

import com.google.gson.Gson;
import com.ironcompass.IronCompassConfig;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearPreferenceStore;
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
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.planner.GoalPlannerService;
import com.ironcompass.planner.InMemoryPlannerPreferenceStore;
import com.ironcompass.planner.PlannerPreferenceStore;
import com.ironcompass.planner.Playstyle;
import com.ironcompass.planner.ProgressionRecommendationService;
import com.ironcompass.planner.RecommendationProjection;
import com.ironcompass.planner.SessionLength;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteEvaluator;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.route.RouteSection;
import com.ironcompass.route.RouteStep;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.state.QuestProgress;
import com.ironcompass.supply.SupplyForecastService;
import com.ironcompass.training.IronmanMethodCatalog;
import com.ironcompass.training.IronmanMethodLoader;
import com.ironcompass.training.MethodPlannerService;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
            IronCompassPanel::showPathForTesting);
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

        TestPreferences earlyMulti = new TestPreferences();
        earlyMulti.setPrimaryGoalId("goal.skill.prayer-43");
        earlyMulti.addSecondaryGoalId("goal.unlock.fossil-island");
        earlyMulti.addSecondaryGoalId("goal.unlock.farming-guild");
        render("profile-l-multi-goal-early.png", route,
            AccountState.builder().accountMode(AccountMode.IRONMAN).skill("Prayer", 31)
                .skill("Farming", 38).bank(BankSnapshot.unknown()).build(), earlyMulti, panel -> { });

        TestPreferences midMulti = new TestPreferences();
        midMulti.setPrimaryGoalId("goal.quest.song-of-the-elves");
        midMulti.addSecondaryGoalId("goal.skill.herblore-70");
        midMulti.addSecondaryGoalId("goal.account.strong-poh");
        render("profile-m-multi-goal-midgame.png", route, songGoal, midMulti, panel -> { });
        render("profile-n-sote-method-planner.png", route, songGoal, midMulti, panel -> { });

        TestPreferences resourceShort = new TestPreferences();
        resourceShort.setPrimaryGoalId("goal.skill.herblore-70");
        AccountState emptyHerbs = AccountState.builder().accountMode(AccountMode.IRONMAN).skill("Herblore", 61)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        render("profile-o-resource-short.png", route, emptyHerbs, resourceShort, panel -> { });

        TestPreferences bowfaBreak = new TestPreferences();
        bowfaBreak.setPrimaryGoalId("gear.mid.bowfa");
        bowfaBreak.addSecondaryGoalId("goal.skill.slayer-87");
        bowfaBreak.addSecondaryGoalId("goal.skill.herblore-70");
        render("profile-p-bowfa-useful-break.png", route, midgame, bowfaBreak,
            IronCompassPanel::showUsefulBreaksForTesting);

        TestPreferences shortSession = new TestPreferences();
        shortSession.setPrimaryGoalId("goal.quest.song-of-the-elves");
        shortSession.addSecondaryGoalId("goal.skill.herblore-70");
        shortSession.setSessionLength(SessionLength.FIFTEEN_MINUTES);
        render("profile-q-15-minute-session.png", route, songGoal, shortSession, panel -> { });

        TestPreferences threeSecondary = new TestPreferences();
        threeSecondary.setPrimaryGoalId("goal.quest.song-of-the-elves");
        threeSecondary.addSecondaryGoalId("goal.skill.herblore-70");
        threeSecondary.addSecondaryGoalId("goal.account.strong-poh");
        threeSecondary.addSecondaryGoalId("gear.mid.bowfa");
        render("profile-r-primary-three-secondary.png", route, songGoal, threeSecondary, panel -> { });
        renderDialog("profile-s-goal-picker.png", route, songGoal, threeSecondary,
            IronCompassPanel::goalPickerContentForTesting, 470, 650);
        renderDialog("profile-t-account-insights.png", route, songGoal, threeSecondary,
            IronCompassPanel::accountInsightsContentForTesting, 430, 650);
        render("profile-u-path-detail.png", route, midgame, midOverrides,
            IronCompassPanel::showCurrentPathDetailForTesting);
        renderDialog("profile-v-account-insights-lower.png", route, songGoal, threeSecondary,
            IronCompassPanel::accountInsightsContentForTesting, 430, 650, 430);
        renderSkillPlanner("profile-w-skill-planner-hunter.png",
            AccountState.builder().accountMode(AccountMode.IRONMAN).skill("Hunter", 68)
                .quest("Children of the Sun", QuestProgress.FINISHED).build(),
            new TestPreferences(), "Hunter", 75, false);
        renderSkillPlanner("profile-x-skill-guide-hunter-1-99.png",
            AccountState.builder().accountMode(AccountMode.IRONMAN).skill("Hunter", 68)
                .quest("Children of the Sun", QuestProgress.FINISHED).build(),
            new TestPreferences(), "Hunter", 99, true);
    }

    private void renderDialog(String name, Route route, AccountState state, TestPreferences overrides,
                              Function<IronCompassPanel, JPanel> content, int width, int height) throws Exception
    {
        renderDialog(name, route, state, overrides, content, width, height, 0);
    }

    private void renderDialog(String name, Route route, AccountState state, TestPreferences overrides,
                              Function<IronCompassPanel, JPanel> content, int width, int height,
                              int scrollPosition) throws Exception
    {
        RouteProjection routeProjection = new RouteEvaluator(conditions).evaluate(route, state, overrides, true, 4, 7);
        GearCatalog catalog = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        GearProjection gearProjection = new GearRecommendationService(conditions).evaluate(catalog, state,
            overrides, overrides);
        GoalCatalog goals = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        GoalPlanProjection baseGoalPlan = new GoalPlannerService(conditions, new GoalDependencyResolver(),
            new SupplyForecastService()).evaluate(goals, state, gearProjection, routeProjection, overrides);
        IronmanMethodCatalog methods = new IronmanMethodLoader(gson)
            .loadResource("/methods/ironman-methods-2026.json");
        MethodPlannerService methodPlanner = new MethodPlannerService(conditions);
        GoalPlanProjection goalPlan = baseGoalPlan.withSkillTrainingPlan(baseGoalPlan.getNextAction() == null
            || baseGoalPlan.getNextAction().getSkill() == null ? null
            : methodPlanner.plan(methods, baseGoalPlan.getNextAction().getSkill(),
                baseGoalPlan.getNextAction().getTargetLevel(), state, overrides, baseGoalPlan.getActiveGoals()));
        RecommendationProjection recommendations = new ProgressionRecommendationService().evaluate(
            routeProjection, gearProjection, goalPlan, state, overrides);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.setSkillPlanner(methods, methodPlanner);
            panel.update(state, routeProjection, gearProjection, null, null, goalPlan, recommendations);
            JPanel root = content.apply(panel);
            root.setSize(new Dimension(width, height));
            for (int pass = 0; pass < 5; pass++) layoutRecursively(root);
            if (scrollPosition > 0)
            {
                JScrollPane scroll = findScrollPane(root);
                if (scroll != null) scroll.getVerticalScrollBar().setValue(scrollPosition);
                for (int pass = 0; pass < 2; pass++) layoutRecursively(root);
            }
            Graphics2D graphics = image.createGraphics();
            root.printAll(graphics);
            graphics.dispose();
        });
        File report = new File("build/reports/progression-ux/" + name);
        assertTrue(report.getParentFile().mkdirs() || report.getParentFile().isDirectory());
        assertTrue(ImageIO.write(image, "png", report));
        assertTrue(report.getName(), distinctColorCount(image) > 8);
    }

    private void render(String name, Route route, AccountState state, TestPreferences overrides,
                        Consumer<IronCompassPanel> view) throws Exception
    {
        RouteProjection routeProjection = new RouteEvaluator(conditions).evaluate(route, state, overrides, true, 4, 7);
        GearCatalog catalog = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        GearProjection gearProjection = new GearRecommendationService(conditions).evaluate(catalog, state,
            overrides, overrides);
        GoalCatalog goals = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        SupplyForecastService supplies = new SupplyForecastService();
        GoalPlanProjection baseGoalPlan = new GoalPlannerService(conditions, new GoalDependencyResolver(), supplies)
            .evaluate(goals, state, gearProjection, routeProjection, overrides);
        IronmanMethodCatalog methods = new IronmanMethodLoader(gson)
            .loadResource("/methods/ironman-methods-2026.json");
        MethodPlannerService methodPlanner = new MethodPlannerService(conditions);
        GoalPlanProjection goalPlan = baseGoalPlan.withSkillTrainingPlan(baseGoalPlan.getNextAction() == null
            || baseGoalPlan.getNextAction().getSkill() == null ? null
            : methodPlanner.plan(methods, baseGoalPlan.getNextAction().getSkill(),
                baseGoalPlan.getNextAction().getTargetLevel(), state, overrides, baseGoalPlan.getActiveGoals()));
        RecommendationProjection recommendations = new ProgressionRecommendationService().evaluate(
            routeProjection, gearProjection, goalPlan, state, overrides);
        BufferedImage image = new BufferedImage(242, 900, BufferedImage.TYPE_INT_ARGB);
        SwingUtilities.invokeAndWait(() ->
        {
            IronCompassPanel panel = new IronCompassPanel(new IronCompassConfig() { }, new WikiBridge(), null,
                new QuestHelperBridge(), overrides, () -> { });
            panel.setSkillPlanner(methods, methodPlanner);
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

    private void renderSkillPlanner(String name, AccountState state, TestPreferences preferences,
                                    String skill, int target, boolean fullGuide) throws Exception
    {
        IronmanMethodCatalog methods = new IronmanMethodLoader(gson)
            .loadResource("/methods/ironman-methods-2026.json");
        MethodPlannerService methodPlanner = new MethodPlannerService(conditions);
        BufferedImage image = new BufferedImage(470, 680, BufferedImage.TYPE_INT_ARGB);
        SwingUtilities.invokeAndWait(() ->
        {
            SkillPlannerDialog dialog = new SkillPlannerDialog(new JPanel(), methods, methodPlanner, state,
                preferences, Collections.emptyList(), new WikiBridge(), skill, target, fullGuide);
            JPanel root = dialog.contentForTesting();
            root.setSize(new Dimension(470, 680));
            for (int pass = 0; pass < 5; pass++) layoutRecursively(root);
            Graphics2D graphics = image.createGraphics();
            root.printAll(graphics);
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

    private static JScrollPane findScrollPane(Container container)
    {
        for (java.awt.Component child : container.getComponents())
        {
            if (child instanceof JScrollPane) return (JScrollPane) child;
            if (child instanceof Container)
            {
                JScrollPane found = findScrollPane((Container) child);
                if (found != null) return found;
            }
        }
        return null;
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
        @Override public String getPrimaryGoalId() { return gear.getPrimaryGoalId(); }
        @Override public void setPrimaryGoalId(String id) { gear.setPrimaryGoalId(id); }
        @Override public java.util.List<String> getSecondaryGoalIds() { return gear.getSecondaryGoalIds(); }
        @Override public boolean addSecondaryGoalId(String id) { return gear.addSecondaryGoalId(id); }
        @Override public void removeSecondaryGoalId(String id) { gear.removeSecondaryGoalId(id); }
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
