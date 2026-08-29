package com.ironcompass.planner;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteEvaluator;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import java.io.StringReader;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public final class ProgressionRecommendationServiceTest
{
    private final Gson gson = new Gson();
    private final ConditionEvaluator conditions = new ConditionEvaluator();

    @Test
    public void avoidWildernessChangesRankingWithoutChangingRequirements() throws Exception
    {
        AccountState state = AccountState.builder().skill("Magic", 1)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        ProjectionFixture fixture = fixture(state, 1);
        InMemoryPlannerPreferenceStore preferences = new InMemoryPlannerPreferenceStore();
        ProgressionRecommendationService service = new ProgressionRecommendationService();

        RecommendationProjection balanced = service.evaluate(fixture.route, fixture.gear, null, state, preferences);
        assertEquals("gear.wilderness", balanced.getRecommended().getId());

        preferences.setAvoidWilderness(true);
        RecommendationProjection safe = service.evaluate(fixture.route, fixture.gear, null, state, preferences);
        assertNotEquals("gear.wilderness", safe.getRecommended().getId());
        assertEquals(GearStatus.RECOMMENDED, fixture.gear.find("gear.wilderness").getStatus());
    }

    @Test
    public void rankingPreferencesNeverPromoteLockedGear() throws Exception
    {
        AccountState state = AccountState.builder().skill("Magic", 1)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        ProjectionFixture fixture = fixture(state, 99);
        InMemoryPlannerPreferenceStore preferences = new InMemoryPlannerPreferenceStore();
        preferences.setPlaystyle(Playstyle.PVM);
        RecommendationProjection projection = new ProgressionRecommendationService().evaluate(
            fixture.route, fixture.gear, null, state, preferences);

        assertEquals(GearStatus.LOCKED, fixture.gear.find("gear.wilderness").getStatus());
        assertEquals("route.train", projection.getRecommended().getId());
    }

    @Test
    public void sessionPreferenceOnlyFiltersQuickWins() throws Exception
    {
        AccountState state = AccountState.builder().skill("Magic", 1)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        ProjectionFixture fixture = fixture(state, 1);
        InMemoryPlannerPreferenceStore preferences = new InMemoryPlannerPreferenceStore();
        preferences.setSessionLength(SessionLength.FIFTEEN_MINUTES);
        RecommendationProjection projection = new ProgressionRecommendationService().evaluate(
            fixture.route, fixture.gear, null, state, preferences);
        assertEquals(EffortClass.QUICK, projection.getQuickWin().getEffort());
    }

    @Test
    public void sharedGoalSynergySaturates()
    {
        assertEquals(0,ProgressionRecommendationService.sharedGoalSynergy(1));
        assertEquals(ScoringWeights.MAX_SHARED_GOAL_SYNERGY,
            ProgressionRecommendationService.sharedGoalSynergy(3));
        assertEquals(ScoringWeights.MAX_SHARED_GOAL_SYNERGY,
            ProgressionRecommendationService.sharedGoalSynergy(20));
    }

    private ProjectionFixture fixture(AccountState state, int magicRequirement) throws Exception
    {
        Route route = new RouteLoader(gson).load(new StringReader(
            "{\"routeId\":\"r\",\"version\":1,\"name\":\"R\",\"sections\":[{\"id\":\"s\","
                + "\"name\":\"S\",\"steps\":[{\"id\":\"route.train\",\"type\":\"TRAIN\","
                + "\"title\":\"Train Magic\",\"instruction\":\"Train\",\"reason\":\"Useful skilling\","
                + "\"completion\":{\"type\":\"SKILL_AT_LEAST\",\"skill\":\"Magic\",\"level\":2}}]}]}"),
            "route");
        GearCatalog catalog = new GearLoader(gson).load(new StringReader(
            "{\"version\":1,\"upgrades\":[{\"id\":\"gear.wilderness\",\"name\":\"Wilderness gear\","
                + "\"styles\":[\"MAGIC\"],\"tier\":1,\"completion\":{\"type\":\"ITEM_PRESENT\","
                + "\"itemId\":1,\"source\":\"ANY\"},\"requirements\":{\"type\":\"SKILL_AT_LEAST\","
                + "\"skill\":\"Magic\",\"level\":" + magicRequirement + "},\"role\":\"RECOMMENDED\","
                + "\"importance\":\"MAJOR\",\"difficulty\":\"LOW\",\"effort\":\"SHORT\","
                + "\"usefulness\":5,\"why\":\"Strong PvM unlock\",\"tags\":[\"pvm\",\"wilderness\"],"
                + "\"source\":{\"method\":\"Drop\",\"activity\":\"Boss\",\"region\":\"Wilderness\"}}]}"),
            "gear");
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        RouteProjection routeProjection = new RouteEvaluator(conditions).evaluate(route, state, overrides,
            false, 4, 0);
        GearProjection gearProjection = new GearRecommendationService(conditions).evaluate(catalog, state,
            new InMemoryGearPreferenceStore(), overrides);
        return new ProjectionFixture(routeProjection, gearProjection);
    }

    private static final class ProjectionFixture
    {
        private final RouteProjection route;
        private final GearProjection gear;
        private ProjectionFixture(RouteProjection route, GearProjection gear)
        {
            this.route = route;
            this.gear = gear;
        }
    }
}
