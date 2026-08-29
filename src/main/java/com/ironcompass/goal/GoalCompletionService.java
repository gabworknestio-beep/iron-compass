package com.ironcompass.goal;

import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.persistence.ManualOverride;
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;

/** One honest completion/readiness policy shared by the picker and planner. */
public final class GoalCompletionService
{
    private static final String KEY_PREFIX = "goal:";
    private final ConditionEvaluator conditions;

    public GoalCompletionService(ConditionEvaluator conditions)
    {
        this.conditions = conditions;
    }

    public GoalCompletionEvaluation evaluate(GoalDefinition goal, AccountState state, GearProjection gear,
                                             ManualOverrideStore overrides)
    {
        ManualOverride manual = overrides == null ? null : overrides.get(key(goal.getId()));
        TruthValue readiness = readiness(goal, state, gear);
        if (manual == ManualOverride.FORCE_COMPLETE)
            return new GoalCompletionEvaluation(TruthValue.TRUE, readiness, GoalStatus.COMPLETE_MANUAL, manual,
                "You marked this goal complete for this character.");
        if (manual == ManualOverride.FORCE_INCOMPLETE)
            return new GoalCompletionEvaluation(TruthValue.FALSE, readiness, GoalStatus.INCOMPLETE_MANUAL, manual,
                "You marked this goal incomplete for this character.");

        TruthValue automatic = automatic(goal, state, gear);
        if (automatic == TruthValue.TRUE)
            return new GoalCompletionEvaluation(automatic, readiness, GoalStatus.COMPLETE_AUTO, null,
                "Iron Compass can confirm completion from local game state.");
        if (readiness == TruthValue.FALSE)
            return new GoalCompletionEvaluation(automatic, readiness, GoalStatus.LOCKED, null,
                "One or more authored requirements are not yet met.");
        if (automatic == TruthValue.FALSE && readiness == TruthValue.TRUE)
            return new GoalCompletionEvaluation(automatic, readiness, GoalStatus.READY, null,
                "Requirements are ready, but completion has not been detected.");
        if (automatic == TruthValue.FALSE)
            return new GoalCompletionEvaluation(automatic, readiness, GoalStatus.INCOMPLETE_AUTO, null,
                "Observed local game state indicates that this goal is incomplete.");
        if (readiness == TruthValue.TRUE)
            return new GoalCompletionEvaluation(automatic, readiness, GoalStatus.READY, null,
                "Requirements are ready; confirm completion manually when finished.");
        return new GoalCompletionEvaluation(TruthValue.UNKNOWN, readiness, GoalStatus.UNKNOWN, null,
            "Completion cannot be established from currently observed local game state.");
    }

    public void markComplete(String goalId, ManualOverrideStore overrides)
    {
        overrides.put(key(goalId), ManualOverride.FORCE_COMPLETE);
    }

    public void markIncomplete(String goalId, ManualOverrideStore overrides)
    {
        overrides.put(key(goalId), ManualOverride.FORCE_INCOMPLETE);
    }

    public void clear(String goalId, ManualOverrideStore overrides)
    {
        overrides.remove(key(goalId));
    }

    public static String key(String goalId) { return KEY_PREFIX + goalId; }

    private TruthValue automatic(GoalDefinition goal, AccountState state, GearProjection gear)
    {
        GearEvaluation linked = GoalRequirementResolver.linkedGear(goal, gear);
        if (linked != null) return linked.getCompletion();
        return goal.getCompletion() == null ? TruthValue.UNKNOWN
            : conditions.evaluate(goal.getCompletion(), state).getValue();
    }

    private TruthValue readiness(GoalDefinition goal, AccountState state, GearProjection gear)
    {
        ConditionSpec requirements = GoalRequirementResolver.effectiveRequirements(goal, gear);
        // Missing metadata means "not modelled", never "satisfied". This is especially important for
        // manual boss, diary, minigame, and transport goals whose access is only partially observable.
        return requirements == null ? TruthValue.UNKNOWN : conditions.evaluate(requirements, state).getValue();
    }

}
