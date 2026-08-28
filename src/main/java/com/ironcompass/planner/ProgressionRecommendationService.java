package com.ironcompass.planner;

import com.ironcompass.gear.CombatStyle;
import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.route.RiskLevel;
import com.ironcompass.route.StepEvaluation;
import com.ironcompass.route.StepStatus;
import com.ironcompass.route.StepType;
import com.ironcompass.state.AccountState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Produces explainable account actions. Scores remain internal implementation details. */
public final class ProgressionRecommendationService
{
    private static final int ROUTE_LOOKAHEAD = 10;

    public RecommendationProjection evaluate(RouteProjection route, GearProjection gear, GoalPlanProjection goals,
                                             AccountState state, PlannerPreferenceStore preferences)
    {
        List<ProgressionCandidate> pool = new ArrayList<>();
        addRouteCandidates(pool, route, state, preferences);
        addGearCandidates(pool, gear, preferences);
        pool.addAll(activeGoalCandidates(goals, preferences));

        ProgressionCandidate longTerm = primaryGoalSummary(goals, preferences);
        Set<String> used = new HashSet<>();
        ProgressionCandidate recommended = best(pool, used, false, preferences);
        if (recommended != null) used.add(recommended.getId());
        if (longTerm != null && recommended != null && longTerm.getId().equals(recommended.getId()))
            longTerm = null;

        ProgressionCandidate quickWin = best(pool, used, true, preferences);
        if (quickWin != null) used.add(quickWin.getId());
        if (longTerm == null)
        {
            longTerm = pool.stream()
                .filter(candidate -> !used.contains(candidate.getId()))
                .filter(candidate -> candidate.getEffort().ordinal() >= EffortClass.LONG.ordinal())
                .max(candidateComparator()).orElse(null);
        }
        if (longTerm != null) used.add(longTerm.getId());

        List<ProgressionCandidate> usefulBreaks = pool.stream()
            .filter(candidate -> !used.contains(candidate.getId()))
            .filter(candidate -> candidate.getScore() > -500)
            .sorted(candidateComparator().reversed())
            .limit(3)
            .collect(java.util.stream.Collectors.toList());
        return new RecommendationProjection(recommended, quickWin, longTerm, null, usefulBreaks);
    }

    private void addRouteCandidates(List<ProgressionCandidate> target, RouteProjection route, AccountState state,
                                    PlannerPreferenceStore preferences)
    {
        if (route == null) return;
        if (route.getCurrent() != null) target.add(routeCandidate(route.getCurrent(), state, preferences, 0));
        int offset = 1;
        for (StepEvaluation step : route.getUpcoming())
        {
            if (offset > ROUTE_LOOKAHEAD) break;
            if (step.getStatus() != StepStatus.COMPLETE && step.getStatus() != StepStatus.SKIPPED_MANUALLY
                && step.getStatus() != StepStatus.OPTIONAL)
                target.add(routeCandidate(step, state, preferences, offset));
            offset++;
        }
    }

    private void addGearCandidates(List<ProgressionCandidate> target, GearProjection gear,
                                   PlannerPreferenceStore preferences)
    {
        if (gear == null) return;
        for (GearEvaluation item : gear.getEvaluations())
        {
            if (item.getStatus() == GearStatus.RECOMMENDED || item.getStatus() == GearStatus.AVAILABLE)
                target.add(gearCandidate(item, preferences));
        }
    }

    private List<ProgressionCandidate> activeGoalCandidates(GoalPlanProjection root,
                                                            PlannerPreferenceStore preferences)
    {
        Map<String, GoalCandidateGroup> grouped = new LinkedHashMap<>();
        if (root == null) return new ArrayList<>();
        List<GoalPlanProjection> active = root.getActiveGoals();
        for (int i = 0; i < active.size(); i++)
        {
            GoalPlanProjection plan = active.get(i);
            PlannedAction action = plan.getNextAction();
            if (action == null || action.getKind() == PlannedAction.Kind.COMPLETE) continue;
            grouped.computeIfAbsent(action.stableKey(), ignored -> new GoalCandidateGroup(action))
                .add(plan, i == 0 && root.hasSelectedGoal());
        }
        List<ProgressionCandidate> result = new ArrayList<>();
        for (Map.Entry<String, GoalCandidateGroup> entry : grouped.entrySet())
            result.add(entry.getValue().toCandidate(entry.getKey(), preferences));
        return result;
    }

    private ProgressionCandidate routeCandidate(StepEvaluation evaluation, AccountState state,
                                                PlannerPreferenceStore preferences, int routeOffset)
    {
        EffortClass effort = routeEffort(evaluation, state);
        int score = 68 + ("MAJOR".equalsIgnoreCase(evaluation.getStep().getImportance()) ? 20 : 0)
            - effort.ordinal() * 7 - routeOffset * 3;
        List<String> why = new ArrayList<>();
        why.add(routeOffset == 0 ? "Current canonical route step" : "Upcoming canonical route progress");
        if ("MAJOR".equalsIgnoreCase(evaluation.getStep().getImportance())) why.add("High-impact unlock");
        if (effort.fits(preferences.getSessionLength())) { score += 10; why.add("Fits your selected session"); }
        if (preferences.getPlaystyle() == Playstyle.EFFICIENT) score += 16;
        if (preferences.getPlaystyle() == Playstyle.SKILLING && evaluation.getStep().getType() == StepType.TRAIN)
            score += 22;
        if (preferences.getPlaystyle() == Playstyle.PVM && contains(evaluation.getStep().getTags(), "pvm"))
            score += 18;
        if (preferences.isAvoidWilderness() && evaluation.getStep().getRisk() == RiskLevel.WILDERNESS) score -= 1000;
        return new ProgressionCandidate(evaluation.getStep().getId(), evaluation.getStep().getTitle(),
            evaluation.getStep().getReason(), evaluation.getStep().getReason(), effort,
            "MAJOR".equalsIgnoreCase(evaluation.getStep().getImportance()) ? "High impact" : "Route progress",
            ProgressionCandidate.Source.ROUTE, evaluation, null, null, score, why, new ArrayList<>());
    }

    private ProgressionCandidate gearCandidate(GearEvaluation evaluation, PlannerPreferenceStore preferences)
    {
        int score = evaluation.getScore();
        List<String> why = new ArrayList<>();
        why.add(evaluation.getUpgrade().getUsefulness() >= 5 ? "High-impact gear upgrade" : "Useful gear upgrade");
        if (preferences.getPlaystyle() == Playstyle.PVM
            && (contains(evaluation.getUpgrade().getTags(), "pvm")
                || evaluation.getUpgrade().getStyles().stream().anyMatch(style -> style != CombatStyle.SKILLING)))
            score += 18;
        if (preferences.getPlaystyle() == Playstyle.SKILLING
            && evaluation.getUpgrade().getStyles().contains(CombatStyle.SKILLING)) score += 20;
        if (preferences.getPlaystyle() == Playstyle.EFFICIENT
            && contains(evaluation.getUpgrade().getTags(), "deterministic")) score += 14;
        if (preferences.isAvoidWilderness() && isWilderness(evaluation)) score -= 1000;
        EffortClass effort = EffortClass.valueOf(evaluation.getUpgrade().getEffort().name());
        if (effort.fits(preferences.getSessionLength())) { score += 8; why.add("Fits your selected session"); }
        return new ProgressionCandidate(evaluation.getUpgrade().getId(), evaluation.getUpgrade().getName(),
            evaluation.getUpgrade().getWhy(), evaluation.getUpgrade().getWhy(), effort,
            evaluation.getUpgrade().getUsefulness() >= 5 ? "High impact" : "Useful upgrade",
            ProgressionCandidate.Source.GEAR, null, evaluation, null, score, why, new ArrayList<>());
    }

    private ProgressionCandidate primaryGoalSummary(GoalPlanProjection root, PlannerPreferenceStore preferences)
    {
        if (root == null || !root.hasSelectedGoal() || root.getNextAction() == null
            || root.getNextAction().getKind() == PlannedAction.Kind.COMPLETE) return null;
        int score = 100 + impactBonus(root);
        if (preferences.isAvoidWilderness() && contains(root.getTags(), "wilderness")) score -= 1000;
        List<String> why = new ArrayList<>();
        why.add("Your primary long-term goal");
        why.add("Next: " + root.getNextAction().getTitle());
        return new ProgressionCandidate(root.getGoalId(), root.getTitle(), "Next: " + root.getNextAction().getTitle(),
            firstUnlock(root), root.getEffort(), impactLabel(root), ProgressionCandidate.Source.GOAL,
            root.getNextAction().getRouteStep(), root.getNextAction().getGearStep(), root.getGoal(), score, why,
            java.util.Collections.singletonList(root.getTitle()));
    }

    private static ProgressionCandidate best(List<ProgressionCandidate> candidates, Set<String> used,
                                             boolean quickOnly, PlannerPreferenceStore preferences)
    {
        return candidates.stream()
            .filter(candidate -> !used.contains(candidate.getId()))
            .filter(candidate -> !quickOnly || candidate.getEffort().ordinal() <= EffortClass.SHORT.ordinal())
            .filter(candidate -> !quickOnly || candidate.getEffort().fits(preferences.getSessionLength()))
            .max(candidateComparator()).orElse(null);
    }

    private static Comparator<ProgressionCandidate> candidateComparator()
    {
        return Comparator.comparingInt(ProgressionCandidate::getScore)
            .thenComparing(ProgressionCandidate::getId, Comparator.reverseOrder());
    }

    private static int impactBonus(GoalPlanProjection plan)
    {
        return "MAJOR".equals(plan.getImpactLabel()) ? 30 : "HIGH".equals(plan.getImpactLabel()) ? 15 : 5;
    }

    private static String impactLabel(GoalPlanProjection plan)
    {
        return plan.getImpactLabel().toLowerCase(Locale.ENGLISH).replace('_', ' ');
    }

    private static String firstUnlock(GoalPlanProjection plan)
    {
        return plan.getUnlocks().isEmpty() ? plan.getDescription() : plan.getUnlocks().get(0);
    }

    private static EffortClass routeEffort(StepEvaluation evaluation, AccountState state)
    {
        if (evaluation.getStep().getType() == StepType.TRAIN)
        {
            ConditionSpec completion = evaluation.getStep().getCompletion();
            if (completion != null && "SKILL_AT_LEAST".equalsIgnoreCase(completion.getType()))
            {
                int gap = Math.max(0, completion.getLevel() - state.skillLevel(completion.getSkill()));
                if (gap <= 2) return EffortClass.QUICK;
                if (gap <= 5) return EffortClass.SHORT;
                if (gap <= 12) return EffortClass.MEDIUM;
                return EffortClass.LONG;
            }
        }
        switch (evaluation.getStep().getType())
        {
            case BUY:
            case TRAVEL:
            case PREPARE: return EffortClass.QUICK;
            case COLLECT:
            case UNLOCK:
            case EQUIP: return EffortClass.SHORT;
            default: return EffortClass.MEDIUM;
        }
    }

    private static boolean isWilderness(GearEvaluation evaluation)
    {
        return contains(evaluation.getUpgrade().getTags(), "wilderness")
            || (evaluation.getUpgrade().getSource() != null
                && evaluation.getUpgrade().getSource().getRegion() != null
                && evaluation.getUpgrade().getSource().getRegion().toLowerCase(Locale.ENGLISH).contains("wilderness"));
    }

    private static boolean contains(List<String> values, String expected)
    {
        if (values == null) return false;
        for (String value : values) if (expected.equalsIgnoreCase(value)) return true;
        return false;
    }

    private static final class GoalCandidateGroup
    {
        private final PlannedAction action;
        private final List<GoalPlanProjection> plans = new ArrayList<>();
        private boolean primary;

        private GoalCandidateGroup(PlannedAction action) { this.action = action; }

        private void add(GoalPlanProjection plan, boolean isPrimary)
        {
            plans.add(plan);
            primary |= isPrimary;
        }

        private ProgressionCandidate toCandidate(String id, PlannerPreferenceStore preferences)
        {
            int score = primary ? 145 : 88;
            int highestImpact = 0;
            EffortClass effort = action.getEffort() == null ? plans.get(0).getEffort() : action.getEffort();
            List<String> titles = new ArrayList<>();
            for (GoalPlanProjection plan : plans)
            {
                highestImpact = Math.max(highestImpact, impactBonus(plan));
                if (action.getEffort() == null && plan.getEffort().ordinal() < effort.ordinal()) effort = plan.getEffort();
                titles.add(plan.getTitle());
            }
            score += highestImpact + (plans.size() - 1) * 38 - effort.ordinal() * 4;
            List<String> why = new ArrayList<>();
            why.add(plans.size() > 1 ? "Advances " + plans.size() + " active goals"
                : primary ? "Directly advances your primary goal" : "Advances a secondary goal");
            if (highestImpact >= 30) why.add("High-impact unlock");
            if (effort.fits(preferences.getSessionLength())) { score += 12; why.add("Fits your selected session"); }
            if (preferences.isAvoidWilderness() && plans.stream().anyMatch(plan -> contains(plan.getTags(), "wilderness")))
                score -= 1000;
            if (preferences.getPlaystyle() == Playstyle.PVM
                && plans.stream().anyMatch(plan -> contains(plan.getTags(), "pvm"))) score += 12;
            if (preferences.getPlaystyle() == Playstyle.SKILLING
                && (action.getSkill() != null || plans.stream().anyMatch(plan -> contains(plan.getTags(), "skilling"))))
                score += 12;
            String reason = why.get(0) + ". " + action.getExplanation();
            return new ProgressionCandidate(id, action.getTitle(), reason, String.join(", ", titles), effort,
                highestImpact >= 30 ? "High impact" : "Goal progress", ProgressionCandidate.Source.GOAL,
                action.getRouteStep(), action.getGearStep(), plans.get(0).getGoal(), score, why, titles);
        }
    }
}
