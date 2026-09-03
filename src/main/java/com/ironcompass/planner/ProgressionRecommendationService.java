package com.ironcompass.planner;

import com.ironcompass.gear.CombatStyle;
import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalIntent;
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
    private static final AccountNeedService ACCOUNT_NEEDS = new AccountNeedService();

    public RecommendationProjection evaluate(RouteProjection route, GearProjection gear, GoalPlanProjection goals,
                                             AccountState state, PlannerPreferenceStore preferences)
    {
        List<ProgressionCandidate> pool = new ArrayList<>();
        GoalCatalog catalog = goals == null ? null : goals.getCatalog();
        addRouteCandidates(pool, route, state, gear, catalog, preferences);
        addGearCandidates(pool, gear, state, catalog, preferences);
        pool.addAll(activeGoalCandidates(goals, state, gear, catalog, preferences));

        ProgressionCandidate longTerm = primaryGoalSummary(goals, state, gear, catalog, preferences);
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
                                    GearProjection gear, GoalCatalog catalog, PlannerPreferenceStore preferences)
    {
        if (route == null) return;
        if (route.getCurrent() != null) target.add(routeCandidate(route.getCurrent(), state, gear, catalog,
            preferences, 0));
        int offset = 1;
        for (StepEvaluation step : route.getUpcoming())
        {
            if (offset > ROUTE_LOOKAHEAD) break;
            if (step.getStatus() != StepStatus.COMPLETE && step.getStatus() != StepStatus.SKIPPED_MANUALLY
                && step.getStatus() != StepStatus.OPTIONAL)
                target.add(routeCandidate(step, state, gear, catalog, preferences, offset));
            offset++;
        }
    }

    private void addGearCandidates(List<ProgressionCandidate> target, GearProjection gear, AccountState state,
                                   GoalCatalog catalog, PlannerPreferenceStore preferences)
    {
        if (gear == null) return;
        for (GearEvaluation item : gear.getEvaluations())
        {
            if (item.getStatus() == GearStatus.RECOMMENDED || item.getStatus() == GearStatus.AVAILABLE)
                target.add(gearCandidate(item, gear, state, catalog, preferences));
        }
    }

    private List<ProgressionCandidate> activeGoalCandidates(GoalPlanProjection root, AccountState state,
                                                            GearProjection gear, GoalCatalog catalog,
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
            result.add(entry.getValue().toCandidate(entry.getKey(), state, gear, catalog, preferences));
        return result;
    }

    private ProgressionCandidate routeCandidate(StepEvaluation evaluation, AccountState state,
                                                GearProjection gear, GoalCatalog catalog,
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
        NeedScore need = needScore(routeIntents(evaluation), state, gear, catalog);
        score += need.bonus;
        why.addAll(need.whyLines);
        return new ProgressionCandidate(evaluation.getStep().getId(), evaluation.getStep().getTitle(),
            evaluation.getStep().getReason(), evaluation.getStep().getReason(), effort,
            "MAJOR".equalsIgnoreCase(evaluation.getStep().getImportance()) ? "High impact" : "Route progress",
            ProgressionCandidate.Source.ROUTE, evaluation, null, null, score, why, new ArrayList<>());
    }

    private ProgressionCandidate gearCandidate(GearEvaluation evaluation, GearProjection gear, AccountState state,
                                               GoalCatalog catalog, PlannerPreferenceStore preferences)
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
        NeedScore need = needScore(gearIntents(evaluation), state, gear, catalog);
        score += need.bonus;
        why.addAll(need.whyLines);
        return new ProgressionCandidate(evaluation.getUpgrade().getId(), evaluation.getUpgrade().getName(),
            evaluation.getUpgrade().getWhy(), evaluation.getUpgrade().getWhy(), effort,
            evaluation.getUpgrade().getUsefulness() >= 5 ? "High impact" : "Useful upgrade",
            ProgressionCandidate.Source.GEAR, null, evaluation, null, score, why, new ArrayList<>());
    }

    private ProgressionCandidate primaryGoalSummary(GoalPlanProjection root, AccountState state, GearProjection gear,
                                                    GoalCatalog catalog, PlannerPreferenceStore preferences)
    {
        if (root == null || !root.hasSelectedGoal() || root.getNextAction() == null
            || root.getNextAction().getKind() == PlannedAction.Kind.COMPLETE) return null;
        int score = 100 + impactBonus(root);
        if (preferences.isAvoidWilderness() && contains(root.getTags(), "wilderness")) score -= 1000;
        List<String> why = new ArrayList<>();
        why.add("Your primary long-term goal");
        why.add("Next: " + root.getNextAction().getTitle());
        NeedScore need = needScore(root.getGoal() == null ? java.util.Collections.emptyList()
            : root.getGoal().getIntents(), state, gear, catalog);
        score += need.bonus;
        why.addAll(need.whyLines);
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

        private ProgressionCandidate toCandidate(String id, AccountState state, GearProjection gear,
                                                 GoalCatalog catalog, PlannerPreferenceStore preferences)
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
            int sharedGoalSynergy = sharedGoalSynergy(plans.size());
            score += highestImpact + sharedGoalSynergy - effort.ordinal() * 4;
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
            NeedScore need = needScoreForPlans(plans, state, gear, catalog);
            score += need.bonus;
            why.addAll(need.whyLines);
            String reason = why.get(0) + ". " + action.getExplanation();
            return new ProgressionCandidate(id, action.getTitle(), reason, String.join(", ", titles), effort,
                highestImpact >= 30 ? "High impact" : "Goal progress", ProgressionCandidate.Source.GOAL,
                action.getRouteStep(), action.getGearStep(), plans.get(0).getGoal(), score, why, titles);
        }
    }

    static int sharedGoalSynergy(int planCount)
    {
        return Math.min(ScoringWeights.MAX_SHARED_GOAL_SYNERGY,Math.max(0,planCount - 1) * 38);
    }

    private static NeedScore needScoreForPlans(List<GoalPlanProjection> plans, AccountState state,
                                               GearProjection gear, GoalCatalog catalog)
    {
        Set<GoalIntent> intents = new HashSet<>();
        for (GoalPlanProjection plan : plans)
        {
            if (plan.getGoal() != null) intents.addAll(plan.getGoal().getIntents());
        }
        return needScore(new ArrayList<>(intents), state, gear, catalog);
    }

    private static NeedScore needScore(List<GoalIntent> intents, AccountState state, GearProjection gear,
                                       GoalCatalog catalog)
    {
        int bonus = 0;
        List<String> why = new ArrayList<>();
        Set<GoalIntent> seen = new HashSet<>();
        for (GoalIntent intent : intents)
        {
            if (!seen.add(intent)) continue;
            AccountNeedEvaluation need = ACCOUNT_NEEDS.evaluate(intent, state, gear, catalog, null);
            if (need.getLevel() == AccountNeedLevel.WEAK)
            {
                bonus += ScoringWeights.ACCOUNT_NEED_WEAK;
                if (why.size() < 2) why.add("Addresses weak " + needLabel(intent) + " need");
            }
            else if (need.getLevel() == AccountNeedLevel.DEVELOPING)
            {
                bonus += ScoringWeights.ACCOUNT_NEED_DEVELOPING;
                if (why.size() < 2) why.add("Improves developing " + needLabel(intent) + " need");
            }
        }
        return new NeedScore(bonus, why);
    }

    private static List<GoalIntent> gearIntents(GearEvaluation evaluation)
    {
        List<GoalIntent> result = new ArrayList<>();
        for (CombatStyle style : evaluation.getUpgrade().getStyles())
        {
            if (style == CombatStyle.MELEE) result.add(GoalIntent.MELEE_POWER);
            else if (style == CombatStyle.RANGED) result.add(GoalIntent.RANGED_POWER);
            else if (style == CombatStyle.MAGIC) result.add(GoalIntent.MAGIC_POWER);
            else if (style == CombatStyle.SKILLING) result.add(GoalIntent.ACCOUNT_INFRASTRUCTURE);
        }
        if (contains(evaluation.getUpgrade().getTags(), "bolts")
            || contains(evaluation.getUpgrade().getTags(), "ammo")) result.add(GoalIntent.AMMO_SUPPLY);
        return result;
    }

    private static List<GoalIntent> routeIntents(StepEvaluation evaluation)
    {
        List<GoalIntent> result = new ArrayList<>();
        if (evaluation.getStep().getType() == StepType.TRAIN)
        {
            ConditionSpec target = evaluation.getStep().getCompletion();
            if (target != null) addSkillIntent(result, target);
        }
        if (contains(evaluation.getStep().getTags(), "pvm")) result.add(GoalIntent.BOSSING_READINESS);
        if (contains(evaluation.getStep().getTags(), "transport")
            || contains(evaluation.getStep().getCategory(), "transport")) result.add(GoalIntent.TRANSPORT_NETWORK);
        if (contains(evaluation.getStep().getTags(), "quest")
            || contains(evaluation.getStep().getCategory(), "quest")) result.add(GoalIntent.ACCOUNT_INFRASTRUCTURE);
        return result;
    }

    private static void addSkillIntent(List<GoalIntent> result, ConditionSpec condition)
    {
        if (condition == null) return;
        if ("SKILL_AT_LEAST".equalsIgnoreCase(condition.getType()))
        {
            String skill = condition.getSkill();
            if ("Prayer".equalsIgnoreCase(skill)) result.add(GoalIntent.PRAYER_SUSTAIN);
            else if ("Herblore".equalsIgnoreCase(skill)) result.add(GoalIntent.HERB_SUPPLY);
            else if ("Farming".equalsIgnoreCase(skill)) result.add(GoalIntent.HERB_SUPPLY);
            else if ("Construction".equalsIgnoreCase(skill)) result.add(GoalIntent.POH_NETWORK);
            else if ("Crafting".equalsIgnoreCase(skill)) result.add(GoalIntent.CRAFTING_SUPPLY);
            else if ("Slayer".equalsIgnoreCase(skill)) result.add(GoalIntent.SLAYER_PROGRESS);
            else if ("Ranged".equalsIgnoreCase(skill)) result.add(GoalIntent.RANGED_POWER);
            else if ("Magic".equalsIgnoreCase(skill)) result.add(GoalIntent.MAGIC_POWER);
            else if ("Attack".equalsIgnoreCase(skill) || "Strength".equalsIgnoreCase(skill))
                result.add(GoalIntent.MELEE_POWER);
        }
        for (ConditionSpec child : condition.getChildren()) addSkillIntent(result, child);
        addSkillIntent(result, condition.getChild());
    }

    private static boolean contains(String value, String expected)
    {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(expected.toLowerCase(Locale.ENGLISH));
    }

    private static String needLabel(GoalIntent intent)
    {
        return intent.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
    }

    private static final class NeedScore
    {
        private final int bonus;
        private final List<String> whyLines;

        private NeedScore(int bonus, List<String> whyLines)
        {
            this.bonus = bonus;
            this.whyLines = whyLines;
        }
    }
}
