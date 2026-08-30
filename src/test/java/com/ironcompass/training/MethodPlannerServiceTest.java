package com.ironcompass.training;

import com.google.gson.Gson;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalLoader;
import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.planner.InMemoryPlannerPreferenceStore;
import com.ironcompass.planner.Playstyle;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.state.QuestProgress;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class MethodPlannerServiceTest
{
    private IronmanMethodCatalog catalog;
    private MethodPlannerService planner;
    private InMemoryPlannerPreferenceStore preferences;

    @Before
    public void setUp() throws Exception
    {
        catalog = new IronmanMethodLoader(new Gson()).loadResource("/methods/ironman-methods-2026.json");
        planner = new MethodPlannerService(new ConditionEvaluator());
        preferences = new InMemoryPlannerPreferenceStore();
    }

    @Test
    public void hunter68To75UsesRumoursAndEndsAtMoonlightMoths()
    {
        AccountState state = AccountState.builder().skill("Hunter", 68)
            .quest("Children of the Sun", QuestProgress.FINISHED).build();
        SkillTrainingPlan plan = plan("Hunter", 75, state);

        assertEquals(68, plan.getCurrentLevel());
        assertEquals(75, plan.getTargetLevel());
        assertEquals("method.hunter.rumours-adept",
            plan.getSegments().get(0).getRecommendation().getRecommended().getId());
        assertEquals("method.hunter.rumours-expert",
            plan.getSegments().get(1).getRecommendation().getRecommended().getId());
        assertTrue(plan.getMilestones().stream().anyMatch(value -> value.getLevel() == 75
            && value.getTitle().contains("Moonlight moth")));
    }

    @Test
    public void crafting52To70TransitionsIntoModernGolemCraftingWhenUnlocked()
    {
        preferences.setPlaystyle(Playstyle.EFFICIENT);
        AccountState state = AccountState.builder().skill("Crafting", 52).skill("Sailing", 62)
            .skill("Runecraft", 47).skill("Mining", 53).skill("Magic", 77)
            .quest("Fallen From Grace", QuestProgress.FINISHED)
            .quest("Lunar Diplomacy", QuestProgress.FINISHED).build();
        SkillTrainingPlan plan = plan("Crafting", 70, state);

        assertEquals(52, plan.getSegments().get(0).getFromLevel());
        assertTrue(plan.getSegments().stream().anyMatch(segment -> segment.getFromLevel() == 60
            && segment.getRecommendation().getRecommended().getId().equals("method.crafting.golem")));
        assertEquals(70, plan.getSegments().get(plan.getSegments().size() - 1).getToLevel());
    }

    @Test
    public void lockedQuestMethodRemainsVisibleAsLockedAlternative()
    {
        AccountState state = AccountState.builder().skill("Crafting", 60).skill("Sailing", 62)
            .skill("Runecraft", 47).skill("Mining", 53)
            .quest("Fallen From Grace", QuestProgress.NOT_STARTED).build();
        MethodRecommendation recommendation = plan("Crafting", 61, state).getFirstRecommendation();

        assertFalse(recommendation.getRecommended().getId().equals("method.crafting.golem"));
        assertTrue(recommendation.getLockedAlternatives().stream()
            .anyMatch(method -> method.getId().equals("method.crafting.golem")));
    }

    @Test
    public void unknownBankIsNeutralAndNeverMeansUnavailable()
    {
        AccountState state = AccountState.builder().skill("Herblore", 63)
            .quest("Children of the Sun", QuestProgress.FINISHED).bank(BankSnapshot.unknown()).build();
        MethodRecommendation recommendation = plan("Herblore", 70, state).getFirstRecommendation();

        assertNotNull(recommendation);
        assertEquals(MethodResourceStatus.UNKNOWN, recommendation.getResourceStatus());
        assertTrue(recommendation.getResourceSummary().startsWith("Resources unconfirmed"));
        assertFalse(recommendation.getResourceSummary().toLowerCase().contains("zero"));
        assertFalse(recommendation.getResourceSummary().toLowerCase().contains("unavailable"));
    }

    @Test
    public void playstyleChangesTheHerbloreRecommendation()
    {
        AccountState state = AccountState.builder().skill("Herblore", 63)
            .quest("Children of the Sun", QuestProgress.FINISHED).build();
        preferences.setPlaystyle(Playstyle.EFFICIENT);
        String efficient = plan("Herblore", 70, state).getFirstRecommendation().getRecommended().getId();

        preferences.setPlaystyle(Playstyle.PVM);
        String pvm = plan("Herblore", 70, state).getFirstRecommendation().getRecommended().getId();

        assertEquals("method.herblore.mixology", efficient);
        assertEquals("method.herblore.useful-potions", pvm);
    }

    @Test
    public void fullGuideUsesProjectedSkillLevelForLaterRequirements()
    {
        preferences.setPlaystyle(Playstyle.EFFICIENT);
        AccountState state = AccountState.builder().skill("Herblore", 1)
            .quest("Children of the Sun", QuestProgress.FINISHED).build();

        SkillTrainingPlan guide = planner.fullGuide(catalog, "Herblore", state, preferences,
            Collections.emptyList());

        assertTrue(guide.getSegments().stream().anyMatch(segment -> segment.getFromLevel() >= 60
            && segment.getRecommendation().getRecommended().getId().equals("method.herblore.mixology")));
    }

    @Test
    public void construction60To83UsesResourceEfficientContractBands()
    {
        AccountState state = AccountState.builder().skill("Construction", 60).build();
        SkillTrainingPlan plan = plan("Construction", 83, state);

        assertEquals("method.construction.mahomes-adept",
            plan.getSegments().get(0).getRecommendation().getRecommended().getId());
        assertTrue(plan.getSegments().stream().anyMatch(segment ->
            segment.getRecommendation().getRecommended().getId().equals("method.construction.mahomes-expert")));
        assertTrue(plan.getMilestones().stream().anyMatch(value -> value.getLevel() == 83));
    }

    @Test
    public void efficientSlayer72To87TransitionsIntoNechryaelBand()
    {
        preferences.setPlaystyle(Playstyle.EFFICIENT);
        AccountState state = AccountState.builder().skill("Slayer", 72).skill("Magic", 80)
            .quest("Shilo Village", QuestProgress.FINISHED)
            .quest("Desert Treasure I", QuestProgress.FINISHED).build();
        SkillTrainingPlan plan = plan("Slayer", 87, state);

        assertEquals("method.slayer.burst-dust-devils",
            plan.getSegments().get(0).getRecommendation().getRecommended().getId());
        assertTrue(plan.getSegments().stream().anyMatch(segment -> segment.getFromLevel() == 80
            && segment.getRecommendation().getRecommended().getId()
                .equals("method.slayer.barrage-nechryael")));
        assertTrue(plan.getMilestones().stream().anyMatch(value -> value.getLevel() == 87));
    }

    @Test
    public void currentAndTargetLevelsBoundEverySegment()
    {
        AccountState state = AccountState.builder().skill("Hunter", 68)
            .quest("Children of the Sun", QuestProgress.FINISHED).build();
        SkillTrainingPlan plan = plan("Hunter", 75, state);

        assertFalse(plan.getSegments().isEmpty());
        assertEquals(68, plan.getSegments().get(0).getFromLevel());
        assertEquals(75, plan.getSegments().get(plan.getSegments().size() - 1).getToLevel());
        assertTrue(plan.getSegments().stream().allMatch(segment -> segment.getFromLevel() >= 68
            && segment.getToLevel() <= 75));
    }

    @Test
    public void goalSynergyProducesDeterministicGoalReason() throws Exception
    {
        GoalCatalog goals = new GoalLoader(new Gson()).loadResource("/goals/ironman-goals-2026.json");
        GoalDefinition hunter75 = goals.find("goal.skill.hunter-75");
        GoalPlanProjection active = new GoalPlanProjection(goals, hunter75, null, TruthValue.FALSE,
            Collections.emptyList(), null, null, null, Collections.emptyList(), null, null);
        AccountState state = AccountState.builder().skill("Hunter", 68)
            .quest("Children of the Sun", QuestProgress.FINISHED).build();

        SkillTrainingPlan first = planner.plan(catalog, "Hunter", 75, state, preferences,
            Collections.singletonList(active));
        SkillTrainingPlan second = planner.plan(catalog, "Hunter", 75, state, preferences,
            Collections.singletonList(active));
        assertEquals(first.getFirstRecommendation().getRecommended().getId(),
            second.getFirstRecommendation().getRecommended().getId());
        assertTrue(first.getFirstRecommendation().getReason().contains("active goal"));
    }

    @Test
    public void fullGuidesHaveCoverageAndSearchableMetadata()
    {
        for (String skill : catalog.getFullGuideSkills())
        {
            AccountState state = AccountState.builder().skill(skill, 1).build();
            SkillTrainingPlan guide = planner.fullGuide(catalog, skill, state, preferences,
                Collections.emptyList());
            assertNotNull(skill, guide);
            assertFalse(skill, guide.getSegments().isEmpty());
            assertEquals(skill, 1, guide.getSegments().get(0).getFromLevel());
            assertEquals(skill, 99, guide.getSegments().get(guide.getSegments().size() - 1).getToLevel());
        }
        assertTrue(catalog.search("golem").stream().anyMatch(method -> method.getId().equals("method.crafting.golem")));
        assertTrue(catalog.search("prayer").stream().anyMatch(method -> method.getSkill().equals("Herblore")));
        assertTrue(catalog.search("low cost").stream().anyMatch(method -> !method.getStyles().isEmpty()));
    }

    @Test
    public void unsupportedSkillProducesNoAttachableRecommendation()
    {
        AccountState state = AccountState.builder().skill("Prayer", 43).build();
        SkillTrainingPlan unsupported = planner.plan(catalog, "Prayer", 70, state, preferences,
            Collections.emptyList());
        GoalPlanProjection empty = new GoalPlanProjection(null, null, null, TruthValue.FALSE,
            Collections.emptyList(), null, null, null, Collections.emptyList(), null, null);

        GoalPlanProjection projection = empty.withSkillTrainingPlan(unsupported);

        assertTrue(unsupported.getSegments().isEmpty());
        assertTrue(projection.getSkillTrainingPlan() == null);
        assertTrue(projection.getMethodRecommendation() == null);
    }

    private SkillTrainingPlan plan(String skill, int target, AccountState state)
    {
        return planner.plan(catalog, skill, target, state, preferences, Collections.emptyList());
    }
}
