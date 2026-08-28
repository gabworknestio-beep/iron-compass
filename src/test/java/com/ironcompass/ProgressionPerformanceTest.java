package com.ironcompass;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.goal.GoalDependencyResolver;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalLoader;
import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.planner.GoalPlannerService;
import com.ironcompass.planner.InMemoryPlannerPreferenceStore;
import com.ironcompass.planner.ProgressionRecommendationService;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteEvaluator;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.supply.SupplyForecastService;
import com.ironcompass.training.IronmanMethodCatalog;
import com.ironcompass.training.IronmanMethodLoader;
import com.ironcompass.training.MethodPlannerService;
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
        GoalCatalog goalCatalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        IronmanMethodCatalog methodCatalog = new IronmanMethodLoader(gson)
            .loadResource("/methods/ironman-methods-2026.json");
        ConditionEvaluator conditions = new ConditionEvaluator();
        RouteEvaluator routes = new RouteEvaluator(conditions);
        GearRecommendationService gear = new GearRecommendationService(conditions);
        GoalDependencyResolver goals = new GoalDependencyResolver();
        SupplyForecastService supplies = new SupplyForecastService();
        GoalPlannerService planner = new GoalPlannerService(conditions, goals, supplies);
        ProgressionRecommendationService recommendations = new ProgressionRecommendationService();
        MethodPlannerService methods = new MethodPlannerService(conditions);
        InMemoryPlannerPreferenceStore plannerPreferences = new InMemoryPlannerPreferenceStore();
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setSelectedGoalId("gear.early.gloves");
        AccountState state = AccountState.builder().accountMode(AccountMode.IRONMAN)
            .skill("Attack", 70).skill("Strength", 70).skill("Defence", 70)
            .skill("Ranged", 75).skill("Magic", 75).skill("Prayer", 70)
            .bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build();

        for (int i = 0; i < 20; i++) evaluate(route, catalog, goalCatalog, methodCatalog, state, overrides,
            preferences, plannerPreferences, routes, gear, goals, supplies, planner, methods, recommendations);
        long total = 0L;
        long maximum = 0L;
        long routeTotal = 0L;
        long gearTotal = 0L;
        long goalTotal = 0L;
        long methodTotal = 0L;
        long recommendationTotal = 0L;
        long supplyTotal = 0L;
        int iterations = 200;
        for (int i = 0; i < iterations; i++)
        {
            long start = System.nanoTime();
            long stageStart = start;
            RouteProjection routeProjection = routes.evaluate(route, state, overrides, true, 4, 7);
            routeTotal += System.nanoTime() - stageStart;

            stageStart = System.nanoTime();
            GearProjection gearProjection = gear.evaluate(catalog, state, preferences, overrides);
            gearTotal += System.nanoTime() - stageStart;

            stageStart = System.nanoTime();
            goals.resolve(gearProjection, routeProjection);
            GoalPlanProjection goalPlan = planner.evaluate(goalCatalog, state, gearProjection, routeProjection,
                preferences);
            goalTotal += System.nanoTime() - stageStart;

            stageStart = System.nanoTime();
            goalPlan = goalPlan.withMethodRecommendation(methods.recommend(methodCatalog,
                goalPlan.getNextAction(), state, plannerPreferences, goalPlan.getActiveGoals()));
            methodTotal += System.nanoTime() - stageStart;

            stageStart = System.nanoTime();
            recommendations.evaluate(routeProjection, gearProjection, goalPlan, state, plannerPreferences);
            recommendationTotal += System.nanoTime() - stageStart;

            stageStart = System.nanoTime();
            supplies.evaluate(gearProjection.getSelected() == null
                ? gearProjection.getRecommended() : gearProjection.getSelected(), state);
            supplyTotal += System.nanoTime() - stageStart;
            long elapsed = System.nanoTime() - start;
            total += elapsed;
            maximum = Math.max(maximum, elapsed);
        }
        double averageMs = total / 1_000_000.0 / iterations;
        double maximumMs = maximum / 1_000_000.0;
        double routeMs = routeTotal / 1_000_000.0 / iterations;
        double gearMs = gearTotal / 1_000_000.0 / iterations;
        double goalMs = goalTotal / 1_000_000.0 / iterations;
        double methodMs = methodTotal / 1_000_000.0 / iterations;
        double recommendationMs = recommendationTotal / 1_000_000.0 / iterations;
        double supplyMs = supplyTotal / 1_000_000.0 / iterations;
        System.out.printf(Locale.ENGLISH,
            "Iron Compass full projection: avg %.3f ms, max %.3f ms; route %.3f ms; gear %.3f ms; "
                + "goal %.3f ms; methods %.3f ms; candidates/scoring %.3f ms; supplies %.3f ms; %d iterations%n",
            averageMs, maximumMs, routeMs, gearMs, goalMs, methodMs, recommendationMs, supplyMs, iterations);
        assertTrue("Average projection should stay under 50 ms, was " + averageMs, averageMs < 50.0);
        assertTrue("Maximum projection should stay under 250 ms, was " + maximumMs, maximumMs < 250.0);
        assertTrue("Average route projection should stay under 40 ms, was " + routeMs, routeMs < 40.0);
        assertTrue("Average Gear projection should stay under 30 ms, was " + gearMs, gearMs < 30.0);
        assertTrue("Average Goal projection should stay under 20 ms, was " + goalMs, goalMs < 20.0);
        assertTrue("Average method projection should stay under 20 ms, was " + methodMs, methodMs < 20.0);
        assertTrue("Average candidate generation/scoring should stay under 20 ms, was "
            + recommendationMs, recommendationMs < 20.0);
        assertTrue("Average supply projection should stay under 20 ms, was " + supplyMs, supplyMs < 20.0);
    }

    private static void evaluate(Route route, GearCatalog catalog, GoalCatalog goalsCatalog,
                                 IronmanMethodCatalog methodCatalog, AccountState state,
                                 InMemoryManualOverrideStore overrides, InMemoryGearPreferenceStore preferences,
                                 InMemoryPlannerPreferenceStore plannerPreferences,
                                 RouteEvaluator routes, GearRecommendationService gear,
                                 GoalDependencyResolver goals, SupplyForecastService supplies,
                                 GoalPlannerService planner, MethodPlannerService methods,
                                 ProgressionRecommendationService recommendations)
    {
        RouteProjection routeProjection = routes.evaluate(route, state, overrides, true, 4, 7);
        GearProjection gearProjection = gear.evaluate(catalog, state, preferences, overrides);
        goals.resolve(gearProjection, routeProjection);
        GoalPlanProjection goalPlan = planner.evaluate(goalsCatalog, state, gearProjection, routeProjection,
            preferences);
        goalPlan = goalPlan.withMethodRecommendation(methods.recommend(methodCatalog, goalPlan.getNextAction(),
            state, plannerPreferences, goalPlan.getActiveGoals()));
        recommendations.evaluate(routeProjection, gearProjection, goalPlan, state, plannerPreferences);
        supplies.evaluate(gearProjection.getSelected() == null
            ? gearProjection.getRecommended() : gearProjection.getSelected(), state);
    }
}
