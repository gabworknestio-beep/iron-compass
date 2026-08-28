package com.ironcompass.planner;

import com.ironcompass.gear.CombatStyle;
import com.ironcompass.gear.GearEffort;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ProgressionRecommendationService
{
    public RecommendationProjection evaluate(RouteProjection route, GearProjection gear, GoalPlanProjection goal,
                                             AccountState state, PlannerPreferenceStore preferences)
    {
        ProgressionCandidate longTerm = goalCandidate(goal, preferences);
        Set<String> used = new HashSet<>();
        if (longTerm != null) used.add(longTerm.getId());

        List<ProgressionCandidate> general = new ArrayList<>();
        if (route != null && route.getCurrent() != null)
            general.add(routeCandidate(route.getCurrent(), state, preferences));
        if (gear != null && gear.getRecommended() != null
            && (gear.getRecommended().getStatus() == GearStatus.RECOMMENDED
                || gear.getRecommended().getStatus() == GearStatus.AVAILABLE))
            general.add(gearCandidate(gear.getRecommended(), preferences));
        ProgressionCandidate recommended = best(general, used, false, preferences);
        if (recommended != null) used.add(recommended.getId());

        List<ProgressionCandidate> quick = new ArrayList<>();
        if (route != null)
        {
            if (route.getCurrent() != null) quick.add(routeCandidate(route.getCurrent(), state, preferences));
            for (StepEvaluation step : route.getUpcoming()) quick.add(routeCandidate(step, state, preferences));
        }
        if (gear != null)
        {
            for (GearEvaluation item : gear.getEvaluations())
            {
                if ((item.getStatus() == GearStatus.AVAILABLE || item.getStatus() == GearStatus.RECOMMENDED)
                    && item.getUpgrade().getEffort() == GearEffort.SHORT)
                    quick.add(gearCandidate(item, preferences));
            }
        }
        ProgressionCandidate quickWin = best(quick, used, true, preferences);
        return new RecommendationProjection(recommended, quickWin, longTerm, null);
    }

    private ProgressionCandidate routeCandidate(StepEvaluation evaluation, AccountState state,
                                                PlannerPreferenceStore preferences)
    {
        EffortClass effort = routeEffort(evaluation, state);
        int score = 58 + ("MAJOR".equalsIgnoreCase(evaluation.getStep().getImportance()) ? 20 : 0)
            - effort.ordinal() * 7;
        if (preferences.getPlaystyle() == Playstyle.EFFICIENT) score += 16;
        if (preferences.getPlaystyle() == Playstyle.SKILLING && evaluation.getStep().getType() == StepType.TRAIN)
            score += 22;
        if (preferences.getPlaystyle() == Playstyle.PVM && contains(evaluation.getStep().getTags(), "pvm"))
            score += 18;
        if (preferences.isAvoidWilderness() && evaluation.getStep().getRisk() == RiskLevel.WILDERNESS) score -= 1000;
        String unlock = evaluation.getStep().getReason();
        return new ProgressionCandidate(evaluation.getStep().getId(), evaluation.getStep().getTitle(),
            evaluation.getStep().getReason(), unlock, effort,
            "MAJOR".equalsIgnoreCase(evaluation.getStep().getImportance()) ? "High impact" : "Route progress",
            ProgressionCandidate.Source.ROUTE, evaluation, null, null, score);
    }

    private ProgressionCandidate gearCandidate(GearEvaluation evaluation, PlannerPreferenceStore preferences)
    {
        int score = evaluation.getScore();
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
        return new ProgressionCandidate(evaluation.getUpgrade().getId(), evaluation.getUpgrade().getName(),
            evaluation.getUpgrade().getWhy(), evaluation.getUpgrade().getWhy(), effort,
            evaluation.getUpgrade().getUsefulness() >= 5 ? "High impact" : "Useful upgrade",
            ProgressionCandidate.Source.GEAR, null, evaluation, null, score);
    }

    private ProgressionCandidate goalCandidate(GoalPlanProjection projection, PlannerPreferenceStore preferences)
    {
        if (projection == null || !projection.hasSelectedGoal() || projection.getNextAction() == null
            || projection.getNextAction().getKind() == PlannedAction.Kind.COMPLETE)
            return null;
        int score = 65 + ("MAJOR".equals(projection.getImpactLabel()) ? 30
            : "HIGH".equals(projection.getImpactLabel()) ? 15 : 0);
        if (preferences.getPlaystyle() == Playstyle.PVM && contains(projection.getTags(), "pvm"))
            score += 15;
        if (preferences.getPlaystyle() == Playstyle.SKILLING && contains(projection.getTags(), "skilling"))
            score += 15;
        if (preferences.isAvoidWilderness() && contains(projection.getTags(), "wilderness")) score -= 1000;
        String unlock = projection.getUnlocks().isEmpty() ? projection.getDescription()
            : projection.getUnlocks().get(0);
        return new ProgressionCandidate(projection.getGoalId(), projection.getTitle(),
            "Next: " + projection.getNextAction().getTitle(), unlock, projection.getEffort(),
            projection.getImpactLabel().toLowerCase(Locale.ENGLISH).replace('_', ' '),
            ProgressionCandidate.Source.GOAL, projection.getNextAction().getRouteStep(),
            projection.getNextAction().getGearStep(), projection.getGoal(), score);
    }

    private static ProgressionCandidate best(List<ProgressionCandidate> candidates, Set<String> used,
                                             boolean quickOnly, PlannerPreferenceStore preferences)
    {
        return candidates.stream()
            .filter(candidate -> !used.contains(candidate.getId()))
            .filter(candidate -> !quickOnly || candidate.getEffort().ordinal() <= EffortClass.SHORT.ordinal())
            .filter(candidate -> !quickOnly || candidate.getEffort().fits(preferences.getSessionLength()))
            .max(Comparator.comparingInt(ProgressionCandidate::getScore)
                .thenComparing(ProgressionCandidate::getId, Comparator.reverseOrder()))
            .orElse(null);
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
            case QUEST:
            case DIARY:
            case ACTIVITY:
            case MANUAL:
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
        for (String value : values) if (expected.equalsIgnoreCase(value)) return true;
        return false;
    }
}
