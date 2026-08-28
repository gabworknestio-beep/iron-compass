package com.ironcompass.goal;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteLoader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class GoalCatalogValidationTest
{
    private final Gson gson = new Gson();

    @Test
    public void bundledCatalogHasElevenRepresentativeValidatedGoals() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        validate(catalog);
        assertEquals(11, catalog.getGoals().size());
        assertEquals("2026-08-27", catalog.getAuditedAt());
    }

    @Test(expected = GoalValidationException.class)
    public void duplicateGoalIdsAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.same", "", "SKILL_AT_LEAST") + ","
            + goal("goal.same", "", "SKILL_AT_LEAST") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void danglingDependenciesAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.valid", "goal.missing", "SKILL_AT_LEAST") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void dependencyCyclesAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.a", "goal.b", "SKILL_AT_LEAST") + ","
            + goal("goal.b", "goal.a", "SKILL_AT_LEAST") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void invalidConditionTypesAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.valid", "", "MADE_UP") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void persistenceSeparatorsAndMalformedWikiTitlesAreRejected() throws Exception
    {
        validate(load("[" + goal("goal:bad", "", "SKILL_AT_LEAST").replace("\"Wiki\"", "\"https://bad\"")
            + "]"));
    }

    private GoalCatalog load(String goals) throws Exception
    {
        return new GoalLoader(gson).load(new StringReader("{\"version\":1,\"goals\":" + goals + "}"), "test");
    }

    private void validate(GoalCatalog goals) throws Exception
    {
        GearCatalog gear = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        Route route = new RouteLoader(gson).loadResource("/routes/efficient-ironman.json");
        Set<String> quests = Arrays.stream(Quest.values()).map(Quest::getName).collect(Collectors.toSet());
        new GoalValidator(quests).validate(goals, gear, route);
    }

    private static String goal(String id, String dependency, String conditionType)
    {
        String dependencies = dependency.isEmpty() ? "" : ",\"dependencyIds\":[\"" + dependency + "\"]";
        return "{\"id\":\"" + id + "\",\"title\":\"Title\",\"description\":\"Description\","
            + "\"category\":\"Test\",\"completion\":{\"type\":\"" + conditionType
            + "\",\"skill\":\"Magic\",\"level\":1},\"impact\":\"HIGH\",\"effort\":\"SHORT\","
            + "\"unlocks\":[\"Unlock\"],\"wikiPage\":\"Wiki\"" + dependencies + "}";
    }
}
