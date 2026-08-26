package com.ironpath.requirement;

import com.google.gson.Gson;
import com.ironpath.state.AccountState;
import com.ironpath.state.BankSnapshot;
import com.ironpath.state.QuestProgress;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConditionEvaluatorTest
{
    private final Gson gson = new Gson();
    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    @Test
    public void skillAboveRequirementIsComplete()
    {
        AccountState state = AccountState.builder().skill("Magic", 52).build();
        assertEquals(TruthValue.TRUE, evaluator.evaluate(condition(
            "{\"type\":\"SKILL_AT_LEAST\",\"skill\":\"Magic\",\"level\":37}"), state).getValue());
    }

    @Test
    public void finishedQuestIsComplete()
    {
        AccountState state = AccountState.builder().quest("Waterfall Quest", QuestProgress.FINISHED).build();
        assertEquals(TruthValue.TRUE, evaluator.evaluate(condition(
            "{\"type\":\"QUEST_STATE\",\"quest\":\"Waterfall Quest\",\"state\":\"FINISHED\"}"), state).getValue());
    }

    @Test
    public void questHelperVarpMilestoneCompletesAtOrPastBoundary()
    {
        ConditionSpec milestone = condition(
            "{\"type\":\"VARP_AT_LEAST\",\"id\":71,\"value\":2}");

        assertEquals(TruthValue.FALSE, evaluator.evaluate(milestone,
            AccountState.builder().varp(71, 1).build()).getValue());
        assertEquals(TruthValue.TRUE, evaluator.evaluate(milestone,
            AccountState.builder().varp(71, 2).build()).getValue());
        assertEquals(TruthValue.TRUE, evaluator.evaluate(milestone,
            AccountState.builder().varp(71, 3).build()).getValue());
    }

    @Test
    public void unknownBankNeverBecomesMissing()
    {
        ConditionSpec rope = condition("{\"type\":\"ITEM_QUANTITY\",\"label\":\"Rope\",\"itemId\":954,\"quantity\":1,\"source\":\"ANY\"}");
        AccountState unknown = AccountState.builder().bank(BankSnapshot.unknown()).build();
        AccountState knownEmpty = AccountState.builder().bank(BankSnapshot.observed(Map.of())).build();

        assertEquals(TruthValue.UNKNOWN, evaluator.evaluate(rope, unknown).getValue());
        assertEquals(TruthValue.FALSE, evaluator.evaluate(rope, knownEmpty).getValue());
    }

    @Test
    public void allAndAnyPropagateThreeValuedLogic()
    {
        AccountState state = AccountState.builder().skill("Magic", 52).bank(BankSnapshot.unknown()).build();
        ConditionSpec all = condition("{\"type\":\"ALL\",\"children\":["
            + "{\"type\":\"SKILL_AT_LEAST\",\"skill\":\"Magic\",\"level\":37},"
            + "{\"type\":\"ITEM_PRESENT\",\"itemId\":954,\"source\":\"BANK\"}]}");
        ConditionSpec any = condition("{\"type\":\"ANY\",\"children\":["
            + "{\"type\":\"SKILL_AT_LEAST\",\"skill\":\"Magic\",\"level\":37},"
            + "{\"type\":\"ITEM_PRESENT\",\"itemId\":954,\"source\":\"BANK\"}]}");

        assertEquals(TruthValue.UNKNOWN, evaluator.evaluate(all, state).getValue());
        assertEquals(TruthValue.TRUE, evaluator.evaluate(any, state).getValue());
    }

    @Test
    public void itemAnyAcceptsEquivalentChargedVariationAndPreservesUnknownBank()
    {
        ConditionSpec gloryFamily = condition(
            "{\"type\":\"ITEM_ANY\",\"label\":\"Glory family\",\"source\":\"ANY\",\"itemIds\":[1704,6585]}");

        AccountState chargedGlory = AccountState.builder().inventoryItem(1712, 1).build();
        AccountState unknown = AccountState.builder().bank(BankSnapshot.unknown()).build();
        AccountState knownEmpty = AccountState.builder().bank(BankSnapshot.observed(Map.of())).build();

        assertEquals(TruthValue.TRUE, evaluator.evaluate(gloryFamily, chargedGlory).getValue());
        assertEquals(TruthValue.UNKNOWN, evaluator.evaluate(gloryFamily, unknown).getValue());
        assertEquals(TruthValue.FALSE, evaluator.evaluate(gloryFamily, knownEmpty).getValue());
    }

    private ConditionSpec condition(String json)
    {
        return gson.fromJson(json, ConditionSpec.class);
    }
}
