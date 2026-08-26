package com.ironpath;

import com.google.gson.Gson;
import com.ironpath.gear.GearCatalog;
import com.ironpath.gear.GearLoader;
import com.ironpath.gear.GearProjection;
import com.ironpath.gear.GearRecommendationService;
import com.ironpath.gear.InMemoryGearPreferenceStore;
import com.ironpath.goal.GoalDependencyResolver;
import com.ironpath.persistence.InMemoryManualOverrideStore;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.route.Route;
import com.ironpath.route.RouteEvaluator;
import com.ironpath.route.RouteLoader;
import com.ironpath.route.RouteProjection;
import com.ironpath.state.AccountMode;
import com.ironpath.state.AccountState;
import com.ironpath.state.BankSnapshot;
import com.ironpath.supply.SupplyForecastService;
import java.util.Collections;
import java.util.Locale;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ProgressionPerformanceTest
{
    @Test
    public void fullProjectionStaysComfortablyBelowOneGameTick() throws Exception
    {
        Gson gson = new Gson();
        Route route = new RouteLoader(gson).loadResource("/routes/efficient-ironman.json");
        GearCatalog catalog = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        ConditionEvaluator conditions = new ConditionEvaluator();
        RouteEvaluator routes = new RouteEvaluator(conditions);
        GearRecommendationService gear = new GearRecommendationService(conditions);
        GoalDependencyResolver goals = new GoalDependencyResolver();
        SupplyForecastService supplies = new SupplyForecastService();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("gear.early.gloves");
        AccountState state = AccountState.builder().accountMode(AccountMode.IRONMAN)
            .skill("Attack", 70).skill("Strength", 70).skill("Defence", 70)
            .skill("Ranged", 75).skill("Magic", 75).skill("Prayer", 70)
            .bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build();

        for (int i = 0; i < 20; i++) evaluate(route, catalog, state, overrides, preferences,
            routes, gear, goals, supplies);
        long total = 0L;
        long maximum = 0L;
        int iterations = 200;
        for (int i = 0; i < iterations; i++)
        {
            long start = System.nanoTime();
            evaluate(route, catalog, state, overrides, preferences, routes, gear, goals, supplies);
            long elapsed = System.nanoTime() - start;
            total += elapsed;
            maximum = Math.max(maximum, elapsed);
        }
        double averageMs = total / 1_000_000.0 / iterations;
        double maximumMs = maximum / 1_000_000.0;
        System.out.printf(Locale.ENGLISH,
            "IronPath full projection: avg %.3f ms, max %.3f ms, %d iterations%n",
            averageMs, maximumMs, iterations);
        assertTrue("Average projection should stay under 50 ms, was " + averageMs, averageMs < 50.0);
        assertTrue("Maximum projection should stay under 250 ms, was " + maximumMs, maximumMs < 250.0);
    }

    private static void evaluate(Route route, GearCatalog catalog, AccountState state,
                                 InMemoryManualOverrideStore overrides, InMemoryGearPreferenceStore preferences,
                                 RouteEvaluator routes, GearRecommendationService gear,
                                 GoalDependencyResolver goals, SupplyForecastService supplies)
    {
        RouteProjection routeProjection = routes.evaluate(route, state, overrides, true, 4, 7);
        GearProjection gearProjection = gear.evaluate(catalog, state, preferences, overrides);
        goals.resolve(gearProjection, routeProjection);
        supplies.evaluate(gearProjection.getSelected() == null
            ? gearProjection.getRecommended() : gearProjection.getSelected(), state);
    }
}
