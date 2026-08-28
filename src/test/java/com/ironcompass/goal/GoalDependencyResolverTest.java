package com.ironcompass.goal;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
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

public class GoalDependencyResolverTest
{
    @Test
    public void recursivelyReturnsFirstMissingGearDependency() throws Exception
    {
        GearCatalog catalog = gear("{\"version\":1,\"upgrades\":["
            + item("base", "Base", "", "") + ","
            + item("middle", "Middle", "base", "") + ","
            + item("goal", "Goal", "middle", "") + "]}");
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("goal");
        GearProjection projection = new GearRecommendationService(new ConditionEvaluator()).evaluate(catalog,
            AccountState.builder().bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build(), preferences,
            new InMemoryManualOverrideStore());
        GoalResolution resolution = new GoalDependencyResolver().resolve(projection, null);
        assertEquals("Obtain Base", resolution.getNextAction().getTitle());
        assertEquals(3, resolution.getDependencyPath().size());
    }

    @Test
    public void selectedGearGoalCanRedirectNextStepToRouteDependency() throws Exception
    {
        GearCatalog catalog = gear("{\"version\":1,\"upgrades\":["
            + item("goal", "Goal", "", "quest-step") + "]}");
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("goal");
        AccountState state = AccountState.builder().bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build();
        GearProjection gear = new GearRecommendationService(new ConditionEvaluator()).evaluate(catalog, state,
            preferences, new InMemoryManualOverrideStore());
        Route route = new RouteLoader(new Gson()).load(new StringReader(
            "{\"routeId\":\"r\",\"version\":1,\"name\":\"R\",\"sections\":[{\"id\":\"s\",\"name\":\"S\",\"steps\":["
                + "{\"id\":\"quest-step\",\"type\":\"MANUAL\",\"title\":\"Unlock quest\",\"instruction\":\"Do it\",\"reason\":\"Goal\",\"completion\":{\"type\":\"MANUAL_ONLY\"}}]}]}"), "route");
        RouteProjection routeProjection = new RouteEvaluator(new ConditionEvaluator()).evaluate(route, state,
            new InMemoryManualOverrideStore(), false, 2, 0);

        GoalResolution resolution = new GoalDependencyResolver().resolve(gear, routeProjection);
        assertEquals(GoalAction.Kind.ROUTE_STEP, resolution.getNextAction().getKind());
        assertEquals("Unlock quest", resolution.getNextAction().getTitle());
    }

    @Test
    public void selectedGoalStartsAtFirstUnfinishedCanonicalStep() throws Exception
    {
        GearCatalog catalog = gear("{\"version\":1,\"upgrades\":["
            + item("goal", "Barrows gloves", "", "rfd-final") + "]}");
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("goal");
        AccountState state = AccountState.builder().bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build();
        GearProjection gear = new GearRecommendationService(new ConditionEvaluator()).evaluate(catalog, state,
            preferences, new InMemoryManualOverrideStore());
        Route route = new RouteLoader(new Gson()).load(new StringReader(
            "{\"routeId\":\"r\",\"version\":1,\"name\":\"R\",\"sections\":[{\"id\":\"s\",\"name\":\"S\",\"steps\":["
                + manualStep("account-start", "Build foundations") + ","
                + manualStep("rfd-final", "Defeat the Culinaromancer") + "]}]}"), "route");
        RouteProjection routeProjection = new RouteEvaluator(new ConditionEvaluator()).evaluate(route, state,
            new InMemoryManualOverrideStore(), false, 2, 0);

        GoalResolution resolution = new GoalDependencyResolver().resolve(gear, routeProjection);
        assertEquals("Build foundations", resolution.getNextAction().getTitle());
        assertEquals(3, resolution.getDependencyPath().size());
    }

    private static String manualStep(String id, String title)
    {
        return "{\"id\":\"" + id + "\",\"type\":\"MANUAL\",\"title\":\"" + title
            + "\",\"instruction\":\"Do it\",\"reason\":\"Progress\",\"completion\":{\"type\":\"MANUAL_ONLY\"}}";
    }

    private static GearCatalog gear(String json) throws Exception
    {
        return new GearLoader(new Gson()).load(new StringReader(json), "gear");
    }

    private static String item(String id, String name, String prerequisite, String routeStep)
    {
        String prerequisites = prerequisite.isEmpty() ? "" : ",\"prerequisiteIds\":[\"" + prerequisite + "\"]";
        String route = routeStep.isEmpty() ? "" : ",\"routeStepIds\":[\"" + routeStep + "\"]";
        return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"styles\":[\"MELEE\"],"
            + "\"why\":\"why\",\"completion\":{\"type\":\"ITEM_PRESENT\",\"itemId\":1}"
            + prerequisites + route + "}";
    }
}
