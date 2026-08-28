package com.ironcompass;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.persistence.ManualOverride;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteEvaluator;
import com.ironcompass.route.RouteJourney;
import com.ironcompass.route.RouteJourneyService;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.route.RouteSection;
import com.ironcompass.route.RouteStep;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SyntheticAccountProfilesTest
{
    @Test
    public void profilesAToEAlwaysReceivePositionActionAndGearDirection() throws Exception
    {
        Gson gson = new Gson();
        Route route = new RouteLoader(gson).loadResource("/routes/efficient-ironman.json");
        GearCatalog catalog = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        ConditionEvaluator conditions = new ConditionEvaluator();
        int[] completedSteps = {0, 48, 150, 255, 330};
        int[] skillLevels = {1, 40, 60, 75, 90};

        for (int profile = 0; profile < completedSteps.length; profile++)
        {
            InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
            completeFirst(route, overrides, completedSteps[profile]);
            AccountState state = stateAt(skillLevels[profile], profile == 0
                ? BankSnapshot.unknown() : BankSnapshot.observed(Collections.emptyMap()));
            RouteProjection routeProjection = new RouteEvaluator(conditions).evaluate(route, state, overrides,
                true, 4, 7);
            GearProjection gearProjection = new GearRecommendationService(conditions).evaluate(catalog, state,
                new InMemoryGearPreferenceStore(), overrides);
            RouteJourney journey = new RouteJourneyService().project(routeProjection);

            assertNotNull("Profile " + (char) ('A' + profile) + " needs a next action", routeProjection.getCurrent());
            assertNotNull("Profile " + (char) ('A' + profile) + " needs a chapter", journey.getCurrent());
            if (profile == 0)
            {
                assertTrue("Unscanned profile A must expose unconfirmed ownership",
                    gearProjection.getEvaluations().stream()
                        .anyMatch(evaluation -> evaluation.getStatus() == GearStatus.UNCONFIRMED));
            }
            else
            {
                assertNotNull("Profile " + (char) ('A' + profile) + " needs a gear direction",
                    gearProjection.getRecommended());
            }
            assertTrue(journey.getCurrent().getTotalCount() > 0);
        }
    }

    private static AccountState stateAt(int level, BankSnapshot bank)
    {
        AccountState.Builder state = AccountState.builder().accountMode(AccountMode.IRONMAN).bank(bank);
        String[] skills = {"Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft",
            "Construction", "Hitpoints", "Agility", "Herblore", "Thieving", "Crafting", "Fletching",
            "Slayer", "Hunter", "Mining", "Smithing", "Fishing", "Cooking", "Firemaking", "Woodcutting",
            "Farming", "Sailing"};
        for (String skill : skills) state.skill(skill, level);
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
}
