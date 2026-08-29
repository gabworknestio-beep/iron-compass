package com.ironcompass.ui;

import com.google.gson.Gson;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalLoader;
import com.ironcompass.goal.GoalStage;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.QuestProgress;
import com.ironcompass.state.BankSnapshot;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class GoalPickerModelTest
{
    private GoalCatalog catalog;
    private GoalPickerModel picker;

    @Before
    public void setUp() throws Exception
    {
        catalog = new GoalLoader(new Gson()).loadResource("/goals/ironman-goals-2026.json");
        picker = new GoalPickerModel();
    }

    @Test
    public void searchFindsGoalsByTitle()
    {
        List<GoalDefinition> results = picker.filter(catalog, "herblore", GoalPickerModel.ALL,
            Collections.emptySet(), AccountState.builder().build(), null);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(goal -> goal.getId().equals("goal.skill.herblore-70")));
    }

    @Test
    public void searchFindsGoalsByTagAndBenefit()
    {
        List<GoalDefinition> prayer = picker.filter(catalog, "prayer restoration", GoalPickerModel.ALL,
            Collections.emptySet(), AccountState.builder().build(), null);
        assertTrue(prayer.stream().anyMatch(goal -> goal.getId().equals("goal.skill.hunter-75")));
        List<GoalDefinition> transport = picker.filter(catalog, "fairy rings", GoalPickerModel.ALL,
            Collections.emptySet(), AccountState.builder().build(), null);
        assertTrue(transport.stream().anyMatch(goal -> goal.getId().equals("goal.transport.fairy-rings")));
    }

    @Test
    public void categoryLimitsTheLargeCatalog()
    {
        List<GoalDefinition> results = picker.filter(catalog, "", "Account Infrastructure",
            Collections.emptySet(), AccountState.builder().build(), null);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(goal -> "Account Infrastructure".equals(goal.getCategory())));
    }

    @Test
    public void stageFilterIsIndependentFromCategory()
    {
        List<GoalDefinition> results = picker.filter(catalog, "", GoalPickerModel.ALL, "Endgame",
            Collections.emptySet(), AccountState.builder().build(), null);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(goal -> goal.getStage() == GoalStage.ENDGAME));
    }

    @Test
    public void activeAndCompletedFiltersRemainExplicit()
    {
        Set<String> active = new HashSet<>();
        active.add("goal.skill.herblore-70");
        AccountState state = AccountState.builder().skill("Herblore", 70).build();

        List<GoalDefinition> activeResults = picker.filter(catalog, "", GoalPickerModel.ACTIVE,
            active, state, null);
        assertEquals(1, activeResults.size());
        assertEquals("goal.skill.herblore-70", activeResults.get(0).getId());
        List<GoalDefinition> completed = picker.filter(catalog, "Herblore", GoalPickerModel.COMPLETED,
            active, state, null);
        assertTrue(completed.stream().anyMatch(goal -> goal.getId().equals("goal.skill.herblore-70")));
    }

    @Test
    public void suggestionsStayCompactAndNeverAutoActivateAnything()
    {
        List<GoalDefinition> results = picker.filter(catalog, "", GoalPickerModel.SUGGESTED,
            Collections.emptySet(), AccountState.builder().build(), null);
        assertTrue(results.size() <= 10);
        assertTrue(results.size() >= 3);
    }

    @Test
    public void completedGoalsAreExcludedButUnknownGoalsRemainEligible()
    {
        AccountState state = AccountState.builder().skill("Hunter", 75)
            .quest("Children of the Sun",QuestProgress.FINISHED).build();
        List<GoalDefinition> suggested = picker.filter(catalog, "moonlight", GoalPickerModel.SUGGESTED,
            Collections.emptySet(), state, null);
        assertFalse(suggested.stream().anyMatch(goal -> goal.getId().equals("goal.skill.hunter-75")));
        List<GoalDefinition> manual = picker.filter(catalog, "reliable prayer sustain", GoalPickerModel.SUGGESTED,
            Collections.emptySet(), state, null);
        assertTrue(manual.stream().anyMatch(goal -> goal.getId().equals("goal.resource.prayer-sustain")));
    }

    @Test
    public void proximityRaisesMoonlightMothRelevance()
    {
        int far = score("goal.skill.hunter-75", AccountState.builder().skill("Hunter", 30).build());
        int near = score("goal.skill.hunter-75", AccountState.builder().skill("Hunter", 72).build());
        assertTrue("near=" + near + " far=" + far, near > far);
    }

    @Test
    public void lowObservedPrayerSupplyCreatesAnExplainableSignal()
    {
        AccountState state = AccountState.builder().skill("Hunter", 72).skill("Herblore", 55)
            .bank(BankSnapshot.observed(Collections.emptyMap(), 1L)).build();
        GoalSuggestion result = suggestion("goal.skill.hunter-75", state);
        assertTrue(result.getReasons().stream().anyMatch(reason -> reason.contains("Prayer supplies")));
        assertTrue(result.getReasons().stream().anyMatch(reason -> reason.contains("3 Hunter levels")));
    }

    @Test
    public void freshAccountsDoNotReceiveObviouslyLateGoalsInCompactSuggestions()
    {
        List<GoalDefinition> results = picker.filter(catalog, "", GoalPickerModel.SUGGESTED,
            Collections.emptySet(), AccountState.builder().build(), null);
        assertTrue(results.stream().noneMatch(goal -> goal.getStage() == GoalStage.LATE
            || goal.getStage() == GoalStage.ENDGAME));
    }

    @Test
    public void accountTypeRestrictionsApplyToSuggestions()
    {
        AccountState iron = AccountState.builder().accountMode(AccountMode.IRONMAN).build();
        assertTrue(picker.filter(catalog, "UIM POH Storage", GoalPickerModel.SUGGESTED,
            Collections.emptySet(), iron, null).isEmpty());
        AccountState uim = AccountState.builder().accountMode(AccountMode.ULTIMATE_IRONMAN).build();
        assertFalse(picker.filter(catalog, "UIM POH Storage", GoalPickerModel.SUGGESTED,
            Collections.emptySet(), uim, null).isEmpty());
    }

    @Test
    public void recommendationOrderingIsDeterministic()
    {
        AccountState state = AccountState.builder().skill("Hunter", 71).skill("Herblore", 55).build();
        List<GoalDefinition> first = picker.filter(catalog, "prayer", GoalPickerModel.SUGGESTED,
            Collections.emptySet(), state, null);
        List<GoalDefinition> second = picker.filter(catalog, "prayer", GoalPickerModel.SUGGESTED,
            Collections.emptySet(), state, null);
        assertEquals(first.stream().map(GoalDefinition::getId).collect(java.util.stream.Collectors.toList()),
            second.stream().map(GoalDefinition::getId).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void observedAccountNeedRaisesRelevantGoalsWithoutTreatingUnknownAsEmpty()
    {
        AccountState weak = AccountState.builder().skill("Hunter",72).skill("Herblore",55)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        Map<Integer,Integer> stockedItems = new HashMap<>();
        stockedItems.put(ItemID._4DOSEPRAYERRESTORE,150);
        AccountState stocked = AccountState.builder().skill("Hunter",72).skill("Herblore",70)
            .skill("Farming",65).bank(BankSnapshot.observed(stockedItems)).build();
        assertTrue(suggestion("goal.skill.hunter-75",weak).getScore()
            > suggestion("goal.skill.hunter-75",stocked).getScore());
    }

    @Test
    public void activeGoalAndHardcoreRiskChangeOrderingSignals()
    {
        AccountState iron = AccountState.builder().accountMode(AccountMode.IRONMAN).skill("Hunter",60).build();
        Set<String> active = Collections.singleton("goal.skill.hunter-75");
        int inactive = suggestion("goal.skill.hunter-75",iron).getScore();
        int activeScore = suggestion("goal.skill.hunter-75",iron,active).getScore();
        assertTrue(activeScore > inactive);

        int normalRisk = suggestion("goal.skill.hunter-67",iron).getScore();
        AccountState hardcore = AccountState.builder().accountMode(AccountMode.HARDCORE_IRONMAN)
            .skill("Hunter",60).build();
        int hardcoreRisk = suggestion("goal.skill.hunter-67",hardcore).getScore();
        assertTrue(hardcoreRisk < normalRisk);
    }

    @Test
    public void scoringTheExpandedCatalogRemainsInteractive()
    {
        AccountState state = AccountState.builder().skill("Hunter", 71).skill("Herblore", 55)
            .skill("Farming", 65).skill("Crafting", 70).build();
        long start = System.nanoTime();
        for (int i = 0; i < 250; i++)
            picker.suggestions(catalog, i % 2 == 0 ? "" : "prayer", GoalPickerModel.SUGGESTED,
                GoalPickerModel.ANY_STAGE, Collections.emptySet(), state, null, 35.0);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;
        assertTrue("Expanded catalog scoring took " + elapsedMillis + " ms", elapsedMillis < 2500L);
    }

    private int score(String id, AccountState state)
    {
        return suggestion(id, state).getScore();
    }

    private GoalSuggestion suggestion(String id, AccountState state)
    {
        return suggestion(id,state,Collections.emptySet());
    }

    private GoalSuggestion suggestion(String id, AccountState state, Set<String> active)
    {
        return picker.suggestions(catalog, "", GoalPickerModel.ALL, GoalPickerModel.ANY_STAGE,
            active, state, null).stream()
            .filter(value -> value.getGoal().getId().equals(id)).findFirst().orElseThrow(AssertionError::new);
    }
}
