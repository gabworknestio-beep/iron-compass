package com.ironpath.planner;

import com.google.gson.Gson;
import com.ironpath.gear.GearCatalog;
import com.ironpath.gear.GearLoader;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearRecommendationService;
import com.ironpath.gear.GearStatus;
import com.ironpath.gear.InMemoryGearPreferenceStore;
import com.ironpath.persistence.InMemoryManualOverrideStore;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.route.Route;
import com.ironpath.route.RouteEvaluator;
import com.ironpath.route.RouteLoader;
import com.ironpath.route.RouteProjection;
import com.ironpath.state.AccountState;
import com.ironpath.state.BankSnapshot;
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
