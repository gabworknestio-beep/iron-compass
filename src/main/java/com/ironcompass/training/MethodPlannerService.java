package com.ironcompass.training;

import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.planner.PlannedAction;
import com.ironcompass.planner.PlannerPreferenceStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MethodPlannerService
{
    private final ConditionEvaluator conditions;

    public MethodPlannerService(ConditionEvaluator conditions) { this.conditions = conditions; }

    public MethodRecommendation recommend(IronmanMethodCatalog catalog, PlannedAction action, AccountState state,
                                          PlannerPreferenceStore preferences, List<GoalPlanProjection> activeGoals)
    {
        if (catalog == null || action == null || action.getSkill() == null || action.getTargetLevel() < 1) return null;
        List<ScoredMethod> candidates = new ArrayList<>();
        int current = state.skillLevel(action.getSkill());
        for (IronmanMethodDefinition method : catalog.getMethods())
        {
            if (!method.getSkill().equalsIgnoreCase(action.getSkill()) || current < method.getMinLevel()
                || current > method.getMaxLevel() || !supportsAccount(method, state.getAccountMode())) continue;
            TruthValue requirements = conditions.evaluate(method.getRequirements(), state).getValue();
            if (requirements == TruthValue.FALSE) continue;
            boolean wilderness = "WILDERNESS".equalsIgnoreCase(method.getRisk());
            if (wilderness && (preferences.isAvoidWilderness() || state.getAccountMode().isHardcore())) continue;
            int score = 50 + (requirements == TruthValue.TRUE ? 20 : -15);
            if (method.getSessionEffort().fits(preferences.getSessionLength())) score += 12;
            if (contains(method.getPlaystyles(), preferences.getPlaystyle().name())) score += 12;
            int related = 0;
            for (GoalPlanProjection goal : activeGoals)
                if (contains(method.getRelatedGoals(), goal.getGoalId()) || contains(method.getTags(), normalized(goal.getTitle())))
                    related++;
            score += related * 15;
            MethodResourceStatus resources = resourceStatus(method, state);
            if (resources == MethodResourceStatus.SUFFICIENT) score += 18;
            else if (resources == MethodResourceStatus.EMPTY) score -= 16;
            candidates.add(new ScoredMethod(method, score, resources, related));
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingInt(ScoredMethod::getScore).reversed()
            .thenComparing(candidate -> candidate.method.getId()));
        ScoredMethod best = candidates.get(0);
        List<IronmanMethodDefinition> alternatives = new ArrayList<>();
        for (int i = 1; i < candidates.size() && alternatives.size() < 2; i++) alternatives.add(candidates.get(i).method);
        return new MethodRecommendation(best.method, alternatives, reason(best, preferences), best.resources,
            resourceSummary(best.method, best.resources, state));
    }

    private static String reason(ScoredMethod method, PlannerPreferenceStore preferences)
    {
        if (method.relatedGoals > 1) return "This method supports more than one of your active goals.";
        if (method.resources == MethodResourceStatus.SUFFICIENT)
            return "Your observed carried and banked inputs meet this method's authored starting threshold.";
        if (method.method.getSessionEffort().fits(preferences.getSessionLength()))
            return "This is a good fit for your current goals and selected session length.";
        return "This is a good fit for the current skill gate and account unlocks.";
    }

    private static MethodResourceStatus resourceStatus(IronmanMethodDefinition method, AccountState state)
    {
        if (method.getResourceInputs().isEmpty()) return MethodResourceStatus.NOT_APPLICABLE;
        boolean any = false;
        boolean all = true;
        for (MethodResourceGroup group : method.getResourceInputs())
        {
            int quantity = 0;
            for (int itemId : group.getItemIds())
            {
                quantity += state.carriedQuantity(itemId);
                if (state.getBank().isObserved()) quantity += state.getBank().quantity(itemId);
            }
            any |= quantity > 0;
            all &= quantity >= group.getMinimumUsefulQuantity();
        }
        if (all) return MethodResourceStatus.SUFFICIENT;
        if (!state.getBank().isObserved()) return MethodResourceStatus.UNKNOWN;
        return any ? MethodResourceStatus.PARTIAL : MethodResourceStatus.EMPTY;
    }

    private static String resourceSummary(IronmanMethodDefinition method, MethodResourceStatus status,
                                          AccountState state)
    {
        switch (status)
        {
            case UNKNOWN:
                return "Resources unconfirmed — open your bank once if you want Iron Compass to include stored supplies.";
            case EMPTY:
                return "No authored starting inputs were observed. Use the listed acquisition sources first.";
            case PARTIAL:
                return "Some useful inputs are observed, but at least one authored starting input is still missing.";
            case SUFFICIENT:
                return "Observed inputs meet the method's starting threshold; this is not a banked-XP estimate.";
            default:
                return state.getBank().isObserved() ? "No specific stored input is required for this method."
                    : "This method does not require Iron Compass to inspect stored inputs.";
        }
    }

    private static boolean supportsAccount(IronmanMethodDefinition method, AccountMode mode)
    {
        return method.getAccountTypes().isEmpty() || contains(method.getAccountTypes(), mode.name());
    }

    private static boolean contains(List<String> values, String expected)
    {
        if (expected == null) return false;
        for (String value : values) if (expected.equalsIgnoreCase(value)) return true;
        return false;
    }

    private static String normalized(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH).replace(' ', '-');
    }

    private static final class ScoredMethod
    {
        private final IronmanMethodDefinition method;
        private final int score;
        private final MethodResourceStatus resources;
        private final int relatedGoals;

        private ScoredMethod(IronmanMethodDefinition method, int score, MethodResourceStatus resources,
                             int relatedGoals)
        {
            this.method = method;
            this.score = score;
            this.resources = resources;
            this.relatedGoals = relatedGoals;
        }

        private int getScore() { return score; }
    }
}
