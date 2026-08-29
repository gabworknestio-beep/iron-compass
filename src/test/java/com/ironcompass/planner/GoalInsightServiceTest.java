package com.ironcompass.planner;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearRecommendationService;
import com.ironcompass.gear.InMemoryGearPreferenceStore;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalDependencyResolver;
import com.ironcompass.goal.GoalLoader;
import com.ironcompass.goal.GoalStatus;
import com.ironcompass.persistence.InMemoryManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteEvaluator;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import com.ironcompass.state.QuestProgress;
import com.ironcompass.supply.SupplyForecastService;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class GoalInsightServiceTest
{
    private final Gson gson = new Gson();
    private final ConditionEvaluator conditions = new ConditionEvaluator();
    private GoalCatalog goals;
    private GearCatalog gearCatalog;
    private Route route;
    private GoalInsightService insights;

    @Before
    public void setUp() throws Exception
    {
        goals = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        gearCatalog = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        route = new RouteLoader(gson).loadResource("/routes/efficient-ironman.json");
        insights = new GoalInsightService(conditions,new AccountNeedService());
    }

    @Test
    public void quickWinsRewardRemainingProximityAndExcludeCompletedAndHugeGrinds() throws Exception
    {
        GoalInsightsProjection near = project(AccountState.builder().skill("Hunter",74)
            .quest("Children of the Sun",QuestProgress.FINISHED).build(),null);
        assertTrue(ids(near.getQuickWins()).contains("goal.skill.hunter-75"));
        assertFalse(ids(near.getQuickWins()).contains("gear.endgame.twisted-bow"));

        GoalInsightsProjection complete = project(AccountState.builder().skill("Hunter",75)
            .quest("Children of the Sun",QuestProgress.FINISHED).build(),null);
        assertFalse(ids(complete.getQuickWins()).contains("goal.skill.hunter-75"));
    }

    @Test
    public void unlockRadarSortsKnownProximityAndStillLabelsUnknownMetadata() throws Exception
    {
        GoalInsightsProjection result = project(AccountState.builder().skill("Hunter",74)
            .quest("Children of the Sun",QuestProgress.FINISHED).build(),null);
        int near = ids(result.getQuickWins()).indexOf("goal.skill.hunter-75");
        int far = ids(result.getNearbyUnlocks()).indexOf("goal.skill.hunter-91");
        assertTrue(near >= 0);
        assertTrue(far < 0 || near < far);
        assertTrue(result.getNearbyUnlocks().stream().anyMatch(value -> !value.isKnown()
            && value.getStatus() != GoalStatus.READY));
    }

    @Test
    public void gearLinkedRequirementsDriveRadarAndPrimaryBlockers() throws Exception
    {
        AccountState state = AccountState.builder().skill("Attack",59)
            .quest("Monkey Madness I",QuestProgress.FINISHED)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        GoalInsightsProjection result = project(state,"gear.early.melee-weapon");
        assertTrue(ids(result.getQuickWins()).contains("gear.early.melee-weapon"));
        assertTrue(result.getBlockers().stream().anyMatch(value -> value.getTitle().contains("Attack")));
    }

    @Test
    public void quickWinsUseAccountStageAndPrimaryGoalSynergy() throws Exception
    {
        AccountState base = AccountState.builder().skill("Hunter",74)
            .quest("Children of the Sun",QuestProgress.FINISHED)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        int baseline = quickScore(project(base,null),"goal.skill.hunter-75");
        int synergistic = quickScore(project(base,"goal.resource.prayer-sustain"),"goal.skill.hunter-75");
        assertTrue("primary synergy should raise quick-win value",synergistic > baseline);

        AccountState.Builder staged = AccountState.builder().skill("Hunter",74)
            .quest("Children of the Sun",QuestProgress.FINISHED)
            .bank(BankSnapshot.observed(Collections.emptyMap()));
        for (String skill : java.util.Arrays.asList("Attack","Strength","Defence","Ranged","Magic",
            "Hitpoints","Runecraft","Construction","Agility","Thieving","Crafting","Fletching",
            "Slayer","Mining","Smithing","Woodcutting","Firemaking","Fishing","Cooking","Sailing"))
            staged.skill(skill,65);
        int stageMatched = quickScore(project(staged.build(),null),"goal.skill.hunter-75");
        assertTrue("stage relevance should raise quick-win value",stageMatched > baseline);
    }

    @Test
    public void blockersKeepHardRecommendedAndManualStatesDistinct() throws Exception
    {
        AccountState defenceGap = AccountState.builder().skill("Prayer",70).skill("Defence",67)
            .quest("King's Ransom",QuestProgress.FINISHED)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        GoalInsightsProjection piety = project(defenceGap,"goal.unlock.piety");
        assertTrue(piety.getBlockers().stream().anyMatch(value -> value.getKind()
            == GoalBlocker.Kind.HARD_REQUIREMENT && value.getTitle().contains("Defence")));

        AccountState ready = AccountState.builder().skill("Prayer",70).skill("Defence",70)
            .quest("King's Ransom",QuestProgress.FINISHED)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build();
        GoalInsightsProjection manual = project(ready,"goal.unlock.piety");
        assertTrue(manual.getBlockers().stream().anyMatch(value -> value.getKind()
            == GoalBlocker.Kind.UNKNOWN_OR_MANUAL));

        GoalInsightsProjection fireCape = project(defenceGap,"gear.early.fire-cape");
        assertTrue(fireCape.getBlockers().stream().anyMatch(value -> value.getKind()
            == GoalBlocker.Kind.RECOMMENDED_PREPARATION));
    }

    @Test
    public void alternativesShareIntentWithoutBecomingDependencies() throws Exception
    {
        GoalInsightsProjection result = project(AccountState.builder().skill("Hunter",72)
            .bank(BankSnapshot.observed(Collections.emptyMap())).build(),"goal.resource.prayer-sustain");
        assertTrue(result.getAlternatives().size() >= 2);
        assertTrue(result.getAlternatives().stream().anyMatch(goal -> goal.getId().equals("goal.skill.hunter-75")));
        assertTrue(goals.find("goal.resource.prayer-sustain").getDependencyIds().isEmpty());
        assertTrue(result.getPersonalPath().size() == 1);
    }

    @Test
    public void personalPathUsesHardDependencyOrderAndMarksRngNode() throws Exception
    {
        GoalInsightsProjection result = project(AccountState.builder()
            .bank(BankSnapshot.observed(Collections.emptyMap())).build(),"gear.mid.bowfa");
        List<String> path = result.getPersonalPath().stream().map(value -> value.getGoal().getId())
            .collect(Collectors.toList());
        assertTrue(path.indexOf("goal.quest.song-of-the-elves") < path.indexOf("goal.pvm.gauntlet"));
        assertTrue(path.indexOf("goal.pvm.gauntlet") < path.indexOf("goal.pvm.corrupted-gauntlet"));
        assertTrue(path.indexOf("goal.pvm.corrupted-gauntlet") < path.indexOf("gear.mid.bowfa"));
        assertTrue(result.getPersonalPath().get(result.getPersonalPath().size() - 1).isRng());
    }

    private GoalInsightsProjection project(AccountState state, String selected) throws Exception
    {
        InMemoryManualOverrideStore overrides = new InMemoryManualOverrideStore();
        InMemoryGearPreferenceStore preferences = new InMemoryGearPreferenceStore();
        preferences.setPrimaryGoalId(selected);
        RouteProjection routeProjection = new RouteEvaluator(conditions).evaluate(route,state,overrides,false,4,7);
        GearProjection gear = new GearRecommendationService(conditions).evaluate(gearCatalog,state,preferences,overrides);
        GoalPlanProjection plan = new GoalPlannerService(conditions,new GoalDependencyResolver(),
            new SupplyForecastService()).evaluate(goals,state,gear,routeProjection,preferences,overrides);
        return insights.evaluate(goals,state,gear,plan,overrides);
    }

    private static List<String> ids(List<GoalProximityCandidate> values)
    {
        return values.stream().map(value -> value.getGoal().getId()).collect(Collectors.toList());
    }

    private static int quickScore(GoalInsightsProjection projection, String id)
    {
        return projection.getQuickWins().stream().filter(value -> value.getGoal().getId().equals(id))
            .findFirst().orElseThrow(() -> new AssertionError("Missing quick win " + id)).getValueScore();
    }
}
