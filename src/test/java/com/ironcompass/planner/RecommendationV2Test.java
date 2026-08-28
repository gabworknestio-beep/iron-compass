package com.ironcompass.planner;

import com.google.gson.Gson;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RecommendationV2Test
{
    private final ProgressionRecommendationService service = new ProgressionRecommendationService();
    private final InMemoryPlannerPreferenceStore preferences = new InMemoryPlannerPreferenceStore();
    private final AccountState state = AccountState.builder().build();

    @Test
    public void oneActionExplainsOneActiveGoal()
    {
        GoalPlanProjection root = plan(goal("goal.primary", "Primary", "MAJOR", "LONG"),
            action("Train Herblore", "Herblore", 70));
        ProgressionCandidate candidate = service.evaluate(null, null, root, state, preferences).getRecommended();

        assertEquals(1, candidate.getActiveGoalCount());
        assertTrue(candidate.getWhyLines().contains("Directly advances your primary goal"));
    }

    @Test
    public void sharedRequirementRaisesSynergyAndListsBothGoals()
    {
        PlannedAction shared = action("Train Herblore", "Herblore", 70);
        GoalPlanProjection single = plan(goal("goal.primary", "Song of the Elves", "MAJOR", "LONG"), shared);
        int singleScore = service.evaluate(null, null, single, state, preferences).getRecommended().getScore();
        GoalPlanProjection secondary = plan(goal("goal.secondary", "70 Herblore", "HIGH", "LONG"), shared);
        GoalPlanProjection combined = single.withSecondaryGoals(Collections.singletonList(secondary));

        ProgressionCandidate candidate = service.evaluate(null, null, combined, state, preferences).getRecommended();
        assertTrue(candidate.getScore() > singleScore);
        assertEquals(2, candidate.getActiveGoalCount());
        assertEquals(Arrays.asList("Song of the Elves", "70 Herblore"), candidate.getAdvancedGoals());
        assertTrue(candidate.getWhyLines().contains("Advances 2 active goals"));
    }

    @Test
    public void lowValueSecondarySynergyDoesNotOverrideCriticalPrimaryRequirement()
    {
        GoalPlanProjection primary = plan(goal("goal.primary", "Critical", "MAJOR", "LONG"),
            action("Critical quest requirement", null, 0));
        PlannedAction sharedMinor = action("Minor shared action", "Crafting", 50);
        GoalPlanProjection secondaryA = plan(goal("goal.a", "Minor A", "MEDIUM", "MEDIUM"), sharedMinor);
        GoalPlanProjection secondaryB = plan(goal("goal.b", "Minor B", "MEDIUM", "MEDIUM"), sharedMinor);

        RecommendationProjection result = service.evaluate(null, null,
            primary.withSecondaryGoals(Arrays.asList(secondaryA, secondaryB)), state, preferences);
        assertEquals(primary.getNextAction().stableKey(), result.getRecommended().getId());
    }

    @Test
    public void usefulBreakIsDifferentAndStillAdvancesAnActiveGoal()
    {
        GoalPlanProjection primary = plan(goal("goal.primary", "Long RNG Goal", "MAJOR", "VERY_LONG"),
            action("Continue the long grind", null, 0));
        GoalPlanProjection secondary = plan(goal("goal.secondary", "Slayer 87", "HIGH", "LONG"),
            action("Train Slayer", "Slayer", 87));

        RecommendationProjection result = service.evaluate(null, null,
            primary.withSecondaryGoals(Collections.singletonList(secondary)), state, preferences);
        assertFalse(result.getUsefulBreaks().isEmpty());
        assertTrue(!result.getUsefulBreaks().get(0).getId().equals(result.getRecommended().getId()));
        assertTrue(result.getUsefulBreaks().get(0).getActiveGoalCount() > 0);
    }

    private static GoalPlanProjection plan(GoalDefinition goal, PlannedAction action)
    {
        return new GoalPlanProjection(null, goal, null, TruthValue.FALSE, Collections.emptyList(), action,
            action.getExplanation(), null, Collections.emptyList(), null, null);
    }

    private static PlannedAction action(String title, String skill, int target)
    {
        return new PlannedAction(PlannedAction.Kind.REQUIREMENT, title, "Authored requirement", null, null,
            skill, target);
    }

    private static GoalDefinition goal(String id, String title, String impact, String effort)
    {
        return new Gson().fromJson("{\"id\":\"" + id + "\",\"title\":\"" + title
            + "\",\"description\":\"Test goal\",\"category\":\"Test\","
            + "\"completion\":{\"type\":\"MANUAL_ONLY\"},\"impact\":\"" + impact
            + "\",\"effort\":\"" + effort + "\",\"unlocks\":[\"Test unlock\"],"
            + "\"wikiPage\":\"Test\",\"tags\":[]}", GoalDefinition.class);
    }
}
