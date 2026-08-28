package com.ironcompass.training;

import com.google.gson.Gson;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.route.RouteStep;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SkillTrainingAdvisorTest
{
    private final SkillTrainingAdvisor advisor = new SkillTrainingAdvisor();

    @Test
    public void modernHunterMilestoneUsesRumours() throws Exception
    {
        TrainingAdvice advice = advisor.advise(step("Hunter", 72), AccountState.builder().build());
        assertTrue(advice.getPrimaryMethod().contains("Hunter rumours"));
    }

    @Test
    public void hardcorePrayerAdviceDoesNotDefaultToWilderness() throws Exception
    {
        TrainingAdvice advice = advisor.advise(step("Prayer", 70), AccountState.builder()
            .accountMode(AccountMode.HARDCORE_IRONMAN).build());
        assertTrue(advice.getAlternativeMethod().contains("Avoid the Wilderness Chaos Altar"));
    }

    @Test
    public void craftingAdviceReportsObservedInputsWithoutInventingXp() throws Exception
    {
        Map<Integer, Integer> bank = new HashMap<>();
        bank.put(1783, 2500);
        bank.put(21504, 400);
        TrainingAdvice advice = advisor.advise(step("Crafting", 70), AccountState.builder()
            .bank(BankSnapshot.observed(bank, 1L)).build());
        assertTrue(advice.getBankContext().contains("2500 sand"));
        assertTrue(advice.getBankContext().contains("400 giant seaweed"));
    }

    private static RouteStep step(String skill, int level) throws Exception
    {
        String json = "{\"routeId\":\"r\",\"version\":1,\"name\":\"R\",\"sections\":[{\"id\":\"s\",\"name\":\"S\",\"steps\":["
            + "{\"id\":\"train\",\"type\":\"TRAIN\",\"title\":\"Train\",\"instruction\":\"Train\",\"reason\":\"Need it\","
            + "\"completion\":{\"type\":\"SKILL_AT_LEAST\",\"skill\":\"" + skill + "\",\"level\":" + level + "}}]}]}";
        Route route = new RouteLoader(new Gson()).load(new StringReader(json), "training");
        return route.getSections().get(0).getSteps().get(0);
    }
}
