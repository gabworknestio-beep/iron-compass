package com.ironcompass.route;

import com.google.gson.Gson;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.persistence.ManualOverride;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RouteJourneyServiceTest
{
    @Test
    public void freshAccountIsPlacedInFirstOfTwelveChapters() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        RouteProjection projection = new RouteEvaluator(new ConditionEvaluator()).evaluate(route,
            AccountState.builder().accountMode(AccountMode.IRONMAN).build(),
            new InMemoryManualOverrideStore(), true, 4, 7);

        RouteJourney journey = new RouteJourneyService().project(projection);
        assertEquals(12, journey.getChapters().size());
        assertNotNull(journey.getCurrent());
        assertEquals("Account Foundations", journey.getCurrent().getChapter().getName());
        assertEquals(16, journey.getCurrent().getTotalCount());
    }

    @Test
    public void completingFirstChapterAdvancesJourney() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        RouteChapterSpec first = route.getChapters().get(0);
        String secondStart = route.getChapters().get(1).getStartStepId();
        boolean inFirst = false;
        for (RouteSection section : route.getSections())
        {
            for (RouteStep step : section.getSteps())
            {
                if (step.getId().equals(first.getStartStepId())) inFirst = true;
                if (step.getId().equals(secondStart)) inFirst = false;
                if (inFirst) overrides.put(step.getId(), ManualOverride.FORCE_COMPLETE);
            }
        }
        RouteProjection projection = new RouteEvaluator(new ConditionEvaluator()).evaluate(route,
            AccountState.builder().accountMode(AccountMode.IRONMAN).build(), overrides, true, 4, 7);

        RouteJourney journey = new RouteJourneyService().project(projection);
        assertEquals("Early Travel & Questing", journey.getCurrent().getChapter().getName());
    }
}
