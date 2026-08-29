package com.ironcompass.planner;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalDependencyResolver;
import com.ironcompass.goal.GoalLoader;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteEvaluator;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.state.QuestProgress;
import com.ironcompass.supply.SupplyForecastService;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class GoalPlannerServiceTest
{
    private final Gson gson = new Gson();
    private final ConditionEvaluator conditions = new ConditionEvaluator();
    private final GoalPlannerService planner = new GoalPlannerService(conditions,
        new GoalDependencyResolver(), new SupplyForecastService());

    @Test
    public void selectedSongOfTheElvesGoalChoosesNearestMissingRequirement() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("goal.quest.song-of-the-elves");
        AccountState state = songState(QuestProgress.NOT_STARTED);
        GoalPlanProjection plan = project(state, preferences);

        assertEquals("Song of the Elves", plan.getGoal().getTitle());
        assertEquals("Train Farming 69 → 70", plan.getNextAction().getTitle());
        assertTrue(plan.getWhyNow().contains("closest unfinished skill"));
        assertTrue(plan.getAfterThis().contains("Herblore 70"));
        assertTrue(plan.getGoal().getUnlocks().contains("Prifddinas"));
    }

    @Test
    public void completedGoalProducesCompleteAction() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("goal.quest.song-of-the-elves");
        GoalPlanProjection plan = project(songState(QuestProgress.FINISHED), preferences);
        assertEquals(TruthValue.TRUE, plan.getCompletion());
        assertEquals(PlannedAction.Kind.COMPLETE, plan.getNextAction().getKind());
    }

    @Test
    public void routeOnlyGoalUsesItsAuthoredAnchor() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("goal.quest.perilous-moons");
        AccountState state = AccountState.builder().quest("Perilous Moons", QuestProgress.NOT_STARTED)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        GoalPlanProjection plan = project(state, preferences);
        assertEquals(PlannedAction.Kind.ROUTE_STEP, plan.getNextAction().getKind());
        assertEquals("Perilous Moons", plan.getNextAction().getTitle());
    }

    @Test
    public void bowfaWalksThroughSongOfTheElvesDependency() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("gear.mid.bowfa");
        GoalPlanProjection plan = project(songState(QuestProgress.NOT_STARTED), preferences);
        assertEquals(4, plan.getDependencyPath().size());
        assertEquals("Song of the Elves", plan.getDependencyPath().get(3));
        assertEquals("Train Farming 69 → 70", plan.getNextAction().getTitle());
    }

    @Test
    public void missingOldSelectedIdFailsSafe() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("goal.removed.from.catalog");
        GoalPlanProjection plan = project(songState(QuestProgress.NOT_STARTED), preferences);
        assertNull(plan.getGoal());
        assertNull(plan.getNextAction());
        assertEquals("goal.removed.from.catalog", plan.getUnavailableSelectedId());
    }

    @Test
    public void newlyCuratedGearGoalUsesTheSamePersistentId() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("gear.early.ava");
        AccountState state = AccountState.builder().quest("Animal Magnetism", QuestProgress.NOT_STARTED)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        GoalPlanProjection plan = project(state, preferences);

        assertTrue(plan.hasSelectedGoal());
        assertNotNull(plan.getGoal());
        assertNull(plan.getLegacyGearGoal());
        assertEquals("Ava's Accumulator", plan.getTitle());
        assertNull(plan.getUnavailableSelectedId());
    }

    @Test
    public void resourceReadinessPreservesUnknownBank() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("gear.early.fire-cape");
        AccountState state = AccountState.builder().skill("Ranged", 61).skill("Prayer", 43)
            .bank(BankSnapshot.unknown()).build();
        GoalPlanProjection plan = project(state, preferences);
        assertNotNull(plan.getResourceReadiness());
        assertEquals(TruthValue.UNKNOWN, plan.getResourceReadiness().getValue());
        assertTrue(plan.getNextAction().getTitle().startsWith("Confirm ownership"));
    }

    @Test
    public void recommendationProjectionDoesNotDuplicateSelectedGoal() throws Exception
    {
        InMemoryGearPreferenceStore selected = new InMemoryGearPreferenceStore();
        selected.setSelectedGoalId("gear.early.defender");
        AccountState state = AccountState.builder().skill("Attack", 65).skill("Strength", 65)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        Fixture fixture = fixture(state, selected);
        GoalPlanProjection plan = planner.evaluate(fixture.goals, state, fixture.gear, fixture.route, selected);
        RecommendationProjection recommendations = new ProgressionRecommendationService().evaluate(
            fixture.route, fixture.gear, plan, state, new InMemoryPlannerPreferenceStore());
        assertNotNull(recommendations.getLongTerm());
        if (recommendations.getRecommended() != null)
            assertTrue(!recommendations.getLongTerm().getId().equals(recommendations.getRecommended().getId()));
        if (recommendations.getQuickWin() != null)
            assertTrue(!recommendations.getLongTerm().getId().equals(recommendations.getQuickWin().getId()));
    }

    @Test
    public void primaryAndSecondaryGoalsAreProjectedWithoutDuplicates() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setPrimaryGoalId("goal.quest.song-of-the-elves");
        assertTrue(preferences.addSecondaryGoalId("goal.skill.herblore-70"));
        assertTrue(!preferences.addSecondaryGoalId("goal.quest.song-of-the-elves"));
        AccountState state = songState(QuestProgress.NOT_STARTED);
        state = AccountState.builder().bank(BankSnapshot.observed(Collections.emptyMap()))
            .quest("Song of the Elves", QuestProgress.NOT_STARTED)
            .quest("Mourning's End Part II", QuestProgress.FINISHED)
            .quest("Making History", QuestProgress.FINISHED)
            .quest("Druidic Ritual", QuestProgress.FINISHED)
            .skill("Agility", 70).skill("Construction", 70).skill("Farming", 70).skill("Herblore", 61)
            .skill("Hunter", 70).skill("Mining", 70).skill("Smithing", 70).skill("Woodcutting", 70).build();

        GoalPlanProjection plan = project(state, preferences);
        assertEquals("Song of the Elves", plan.getTitle());
        assertEquals(1, plan.getSecondaryGoals().size());
        assertEquals("70 Herblore", plan.getSecondaryGoals().get(0).getTitle());
        assertEquals("skill:herblore:70", plan.getNextAction().stableKey());
        assertEquals(plan.getNextAction().stableKey(),
            plan.getSecondaryGoals().get(0).getNextAction().stableKey());
    }

    @Test
    public void skippedSecondaryGoalIsNotProjected() throws Exception
    {
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setPrimaryGoalId("goal.quest.song-of-the-elves");
        preferences.addSecondaryGoalId("goal.skill.herblore-70");
        preferences.setSkipped("goal.skill.herblore-70", true);

        assertTrue(project(songState(QuestProgress.NOT_STARTED), preferences).getSecondaryGoals().isEmpty());
    }

    private GoalPlanProjection project(AccountState state, InMemoryGearPreferenceStore preferences) throws Exception
    {
        Fixture fixture = fixture(state, preferences);
        return planner.evaluate(fixture.goals, state, fixture.gear, fixture.route, preferences);
    }

    private Fixture fixture(AccountState state, InMemoryGearPreferenceStore preferences) throws Exception
    {
        Route route = new RouteLoader(gson).loadResource("/routes/efficient-ironman.json");
        GearCatalog gearCatalog = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        GoalCatalog goals = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        RouteProjection routeProjection = new RouteEvaluator(conditions).evaluate(route, state, overrides,
            false, 4, 7);
        GearProjection gear = new GearRecommendationService(conditions).evaluate(gearCatalog, state,
            preferences, overrides);
        return new Fixture(goals, routeProjection, gear);
    }

    private static AccountState songState(QuestProgress song)
    {
        AccountState.Builder state = AccountState.builder().bank(BankSnapshot.observed(Collections.emptyMap()))
            .quest("Song of the Elves", song)
            .quest("Mourning's End Part II", QuestProgress.FINISHED)
            .quest("Making History", QuestProgress.FINISHED)
            .quest("Druidic Ritual", QuestProgress.FINISHED)
            .skill("Agility", 70).skill("Construction", 70).skill("Farming", 69).skill("Herblore", 61)
            .skill("Hunter", 70).skill("Mining", 70).skill("Smithing", 70).skill("Woodcutting", 70);
        return state.build();
    }

    private static final class Fixture
    {
        private final GoalCatalog goals;
        private final RouteProjection route;
        private final GearProjection gear;
        private Fixture(GoalCatalog goals, RouteProjection route, GearProjection gear)
        {
            this.goals = goals;
            this.route = route;
            this.gear = gear;
        }
    }
}
