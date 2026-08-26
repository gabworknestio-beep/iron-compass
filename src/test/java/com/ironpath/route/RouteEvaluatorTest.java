package com.ironpath.route;

import com.google.gson.Gson;
import com.ironpath.persistence.InMemoryManualOverrideStore;
import com.ironpath.persistence.ManualOverride;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.state.AccountMode;
import com.ironpath.state.AccountState;
import com.ironpath.state.QuestProgress;
import java.io.StringReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RouteEvaluatorTest
{
    private final RouteEvaluator evaluator = new RouteEvaluator(new ConditionEvaluator());

    @Test
    public void freshIronmanGetsFirstUnsatisfiedStep() throws Exception
    {
        RouteProjection projection = evaluate(basicRoute(), AccountState.builder()
            .accountMode(AccountMode.IRONMAN).skill("Magic", 1).build(), new InMemoryManualOverrideStore(), false);
        assertEquals("train-magic", projection.getCurrent().getStep().getId());
    }

    @Test
    public void existingAccountSkipsSatisfiedTrainingAndQuest() throws Exception
    {
        AccountState state = AccountState.builder().accountMode(AccountMode.IRONMAN).skill("Magic", 52)
            .quest("Waterfall Quest", QuestProgress.FINISHED).build();
        RouteProjection projection = evaluate(basicRoute(), state, new InMemoryManualOverrideStore(), false);
        assertEquals("next-goal", projection.getCurrent().getStep().getId());
        assertEquals(2, projection.getCompleteCount());
    }

    @Test
    public void manualSkipAndUnskipAdvancePredictably() throws Exception
    {
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        AccountState state = AccountState.builder().skill("Magic", 1).build();
        Route route = basicRoute();
        overrides.put("train-magic", ManualOverride.SKIPPED);
        assertEquals("waterfall", evaluate(route, state, overrides, false).getCurrent().getStep().getId());
        overrides.remove("train-magic");
        assertEquals("train-magic", evaluate(route, state, overrides, false).getCurrent().getStep().getId());
    }

    @Test
    public void stableIdsPreserveOverridesAcrossInsertedRouteStep() throws Exception
    {
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        overrides.put("waterfall", ManualOverride.FORCE_COMPLETE);
        AccountState state = AccountState.builder().skill("Magic", 52).build();
        Route changed = route("{\"routeId\":\"test\",\"version\":2,\"name\":\"Test\",\"sections\":[{\"id\":\"s\",\"name\":\"S\",\"steps\":["
            + manual("inserted", "Inserted") + "," + quest("waterfall", "Waterfall Quest") + "]}]}");
        RouteProjection projection = evaluate(changed, state, overrides, false);
        StepEvaluation waterfall = projection.getSteps().stream()
            .filter(step -> "waterfall".equals(step.getStep().getId())).findFirst().orElse(null);
        assertNotNull(waterfall);
        assertEquals(StepStatus.COMPLETE, waterfall.getStatus());
    }

    @Test
    public void hcimUsesOnlyExplicitSafeAlternative() throws Exception
    {
        Route route = route("{\"routeId\":\"hc\",\"version\":1,\"name\":\"HC\",\"sections\":[{\"id\":\"s\",\"name\":\"S\",\"steps\":["
            + "{\"id\":\"wild\",\"type\":\"ACTIVITY\",\"title\":\"Wild route\",\"instruction\":\"Go\",\"reason\":\"Fast\",\"risk\":\"WILDERNESS\",\"hcimAlternativeStepId\":\"safe\",\"completion\":{\"type\":\"MANUAL_ONLY\"}},"
            + "{\"id\":\"safe\",\"alternativeForStepId\":\"wild\",\"type\":\"ACTIVITY\",\"title\":\"Safe route\",\"instruction\":\"Go safely\",\"reason\":\"Safe\",\"completion\":{\"type\":\"MANUAL_ONLY\"}}"
            + "]}]}");
        AccountState hardcore = AccountState.builder().accountMode(AccountMode.HARDCORE_IRONMAN).build();
        AccountState standard = AccountState.builder().accountMode(AccountMode.IRONMAN).build();
        assertEquals("safe", evaluate(route, hardcore, new InMemoryManualOverrideStore(), true).getCurrent().getStep().getId());
        assertEquals("wild", evaluate(route, standard, new InMemoryManualOverrideStore(), true).getCurrent().getStep().getId());
    }

    private Route basicRoute() throws Exception
    {
        return route("{\"routeId\":\"test\",\"version\":1,\"name\":\"Test\",\"sections\":[{\"id\":\"s\",\"name\":\"S\",\"steps\":["
            + "{\"id\":\"train-magic\",\"type\":\"TRAIN\",\"title\":\"Train Magic\",\"instruction\":\"Train\",\"reason\":\"Requirement\",\"completion\":{\"type\":\"SKILL_AT_LEAST\",\"skill\":\"Magic\",\"level\":37}},"
            + quest("waterfall", "Waterfall Quest") + "," + manual("next-goal", "Next Goal") + "]}]}");
    }

    private String quest(String id, String title)
    {
        return "{\"id\":\"" + id + "\",\"type\":\"QUEST\",\"title\":\"" + title
            + "\",\"instruction\":\"Complete it\",\"reason\":\"Progress\",\"completion\":{\"type\":\"QUEST_STATE\",\"quest\":\"" + title + "\",\"state\":\"FINISHED\"}}";
    }

    private String manual(String id, String title)
    {
        return "{\"id\":\"" + id + "\",\"type\":\"MANUAL\",\"title\":\"" + title
            + "\",\"instruction\":\"Do it\",\"reason\":\"Progress\",\"completion\":{\"type\":\"MANUAL_ONLY\"}}";
    }

    private Route route(String json) throws Exception
    {
        return new RouteLoader(new Gson()).load(new StringReader(json), "test");
    }

    private RouteProjection evaluate(Route route, AccountState state, InMemoryManualOverrideStore overrides, boolean safe)
    {
        return evaluator.evaluate(route, state, overrides, safe, 4, 7);
    }
}
