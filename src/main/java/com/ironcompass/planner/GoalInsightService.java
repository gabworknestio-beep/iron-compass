package com.ironcompass.planner;

import com.ironcompass.gear.GearProjection;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalCompletionEvaluation;
import com.ironcompass.goal.GoalCompletionService;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalImpact;
import com.ironcompass.goal.GoalIntent;
import com.ironcompass.goal.GoalRelationship;
import com.ironcompass.goal.GoalRelationshipIndex;
import com.ironcompass.goal.GoalRelationshipType;
import com.ironcompass.goal.GoalRequirementResolver;
import com.ironcompass.goal.GoalStatus;
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.requirement.RequirementResult;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Read-only views over the existing goal graph and the shared AccountNeedService. */
public final class GoalInsightService
{
    private static final int MAX_RESULTS = 5;
    private final ConditionEvaluator conditions;
    private final GoalCompletionService completion;
    private final AccountNeedService needs;
    private final GoalPackService packs;

    public GoalInsightService(ConditionEvaluator conditions, AccountNeedService needs)
    {
        this.conditions = conditions;
        this.completion = new GoalCompletionService(conditions);
        this.needs = needs;
        this.packs = new GoalPackService(conditions);
    }

    public GoalInsightsProjection evaluate(GoalCatalog catalog, AccountState state, GearProjection gear,
                                           GoalPlanProjection primary, ManualOverrideStore overrides)
    {
        AccountHealthProjection health = needs.health(state, gear, catalog, overrides);
        GoalRelationshipIndex relationships = new GoalRelationshipIndex(catalog);
        List<GoalProximityCandidate> candidates = proximityCandidates(catalog, state, gear, primary,
            relationships, overrides);
        List<GoalProximityCandidate> quick = candidates.stream()
            .filter(GoalProximityCandidate::isKnown)
            .filter(value -> value.getDistance() <= 6)
            .filter(value -> value.getGoal().getUsefulness() >= 4)
            .filter(value -> value.getValueScore() >= 55)
            .sorted(Comparator.comparingInt(GoalProximityCandidate::getValueScore).reversed()
                .thenComparingInt(GoalProximityCandidate::getDistance)
                .thenComparing(value -> value.getGoal().getId()))
            .limit(MAX_RESULTS).collect(java.util.stream.Collectors.toList());
        List<GoalProximityCandidate> orderedNearby = candidates.stream()
            .sorted(Comparator.comparingInt((GoalProximityCandidate value) -> value.isKnown() ? 0 : 1)
                .thenComparingInt(GoalProximityCandidate::getDistance)
                .thenComparing(Comparator.comparingInt(
                    (GoalProximityCandidate value) -> impact(value.getGoal().getImpact())).reversed())
                .thenComparing(value -> value.getGoal().getId()))
            .collect(java.util.stream.Collectors.toList());
        List<GoalProximityCandidate> nearby = new ArrayList<>();
        for (GoalProximityCandidate value : orderedNearby)
            if (value.isKnown() && nearby.size() < MAX_RESULTS - 1) nearby.add(value);
        for (GoalProximityCandidate value : orderedNearby)
            if (!value.isKnown()) { nearby.add(value); break; }

        GoalDefinition selected = primary == null ? null : primary.getGoal();
        AlternativeResult alternatives = alternatives(selected, catalog, state, gear, health, relationships,
            overrides);
        return new GoalInsightsProjection(health, quick, nearby,
            blockers(selected, primary, catalog, state, gear, relationships, overrides),
            alternatives.goals, packs.evaluate(catalog, state, gear, overrides), alternatives.explicit,
            personalPath(selected, catalog, state, gear, overrides));
    }

    private List<GoalProximityCandidate> proximityCandidates(GoalCatalog catalog, AccountState state,
                                                              GearProjection gear, GoalPlanProjection primary,
                                                              GoalRelationshipIndex relationships,
                                                              ManualOverrideStore overrides)
    {
        List<GoalProximityCandidate> values = new ArrayList<>();
        for (GoalDefinition goal : catalog.getGoals())
        {
            GoalCompletionEvaluation evaluation = completion.evaluate(goal,state,gear,overrides);
            if (evaluation.getCompletion() == TruthValue.TRUE) continue;
            Distance distance = distance(GoalRequirementResolver.effectiveRequirements(goal,gear),state);
            int value = goal.getUsefulness() * 18 + impact(goal.getImpact())
                - goal.getEffort().ordinal() * 10 - Math.min(40,distance.value * 5);
            for (GoalIntent intent : goal.getIntents())
            {
                AccountNeedLevel level = needs.evaluate(intent,state,gear,catalog,overrides).getLevel();
                if (level == AccountNeedLevel.WEAK) value += ScoringWeights.QUICK_WIN_WEAK_NEED;
                else if (level == AccountNeedLevel.DEVELOPING)
                    value += ScoringWeights.QUICK_WIN_DEVELOPING_NEED;
            }
            int accountStage = accountStage(state);
            if (Math.abs(goal.getStage().ordinal() - accountStage) <= 1)
                value += ScoringWeights.QUICK_WIN_STAGE_MATCH;
            if (primary != null) for (GoalPlanProjection active : primary.getActiveGoals())
                if (active.getGoalId() != null && relationships.supports(goal.getId(),active.getGoalId()))
                {
                    value += ScoringWeights.QUICK_WIN_ACTIVE_SYNERGY;
                    break;
                }
            values.add(new GoalProximityCandidate(goal,evaluation.getStatus(),distance.value,distance.known,
                distance.summary,value));
        }
        return values;
    }

    private List<GoalBlocker> blockers(GoalDefinition selected, GoalPlanProjection plan, GoalCatalog catalog,
                                       AccountState state, GearProjection gear,
                                       GoalRelationshipIndex relationships, ManualOverrideStore overrides)
    {
        List<GoalBlocker> values = new ArrayList<>();
        if (selected == null) return values;
        for (String id : selected.getDependencyIds())
        {
            GoalDefinition dependency = catalog.find(id);
            if (dependency != null && completion.evaluate(dependency,state,gear,overrides).getCompletion()
                != TruthValue.TRUE)
                values.add(new GoalBlocker(GoalBlocker.Kind.HARD_REQUIREMENT,dependency.getTitle(),
                    "This authored dependency must be completed first."));
        }
        if (plan != null) for (RequirementResult result : plan.getProgress())
        {
            if (result.getValue() == TruthValue.FALSE)
                values.add(new GoalBlocker(GoalBlocker.Kind.HARD_REQUIREMENT,result.getLabel(),result.getDetail()));
            else if (result.getValue() == TruthValue.UNKNOWN)
                values.add(new GoalBlocker(GoalBlocker.Kind.UNKNOWN_OR_MANUAL,result.getLabel(),
                    "This requirement cannot be confirmed from the current local snapshot."));
        }
        for (GoalDefinition related : relationships.related(selected.getId(),
            GoalRelationshipType.RECOMMENDED_BEFORE))
            if (completion.evaluate(related,state,gear,overrides).getCompletion()
                    != TruthValue.TRUE)
                values.add(new GoalBlocker(GoalBlocker.Kind.RECOMMENDED_PREPARATION,related.getTitle(),
                    "Helpful preparation, not a hard requirement."));
        GoalCompletionEvaluation selectedStatus = completion.evaluate(selected,state,gear,overrides);
        if (values.isEmpty() && selectedStatus.getCompletion() == TruthValue.UNKNOWN)
            values.add(new GoalBlocker(GoalBlocker.Kind.UNKNOWN_OR_MANUAL,selected.getTitle(),
                selectedStatus.getExplanation()));
        return values;
    }

    private AlternativeResult alternatives(GoalDefinition selected, GoalCatalog catalog, AccountState state,
                                           GearProjection gear, AccountHealthProjection health,
                                           GoalRelationshipIndex relationships,
                                           ManualOverrideStore overrides)
    {
        Set<GoalIntent> targetIntents = new LinkedHashSet<>();
        if (selected != null) targetIntents.addAll(selected.getIntents());
        if (targetIntents.isEmpty()) for (AccountNeedEvaluation value : health.getEvaluations())
            if (value.getLevel() == AccountNeedLevel.WEAK) targetIntents.add(value.getIntent());
        Set<String> explicit = new HashSet<>();
        if (selected != null) for (GoalDefinition goal : relationships.related(selected.getId(),
            GoalRelationshipType.ALTERNATIVE)) explicit.add(goal.getId());

        List<GoalDefinition> result = new ArrayList<>();
        for (GoalDefinition goal : catalog.getGoals())
        {
            if (selected != null && goal.getId().equals(selected.getId())) continue;
            if (completion.evaluate(goal,state,gear,overrides).getCompletion() == TruthValue.TRUE) continue;
            boolean shared = goal.getIntents().stream().anyMatch(targetIntents::contains);
            if (!explicit.isEmpty() ? explicit.contains(goal.getId()) : shared) result.add(goal);
        }
        result.sort(Comparator.comparingInt((GoalDefinition goal) -> alternativeScore(goal,state,targetIntents))
            .reversed().thenComparing(GoalDefinition::getId));
        List<GoalDefinition> limited = result.size() <= MAX_RESULTS ? result
            : new ArrayList<>(result.subList(0,MAX_RESULTS));
        return new AlternativeResult(limited,!explicit.isEmpty());
    }

    private List<GoalPathNode> personalPath(GoalDefinition selected, GoalCatalog catalog, AccountState state,
                                            GearProjection gear, ManualOverrideStore overrides)
    {
        List<GoalPathNode> path = new ArrayList<>();
        if (selected != null) path(selected,catalog,state,gear,overrides,new HashSet<>(),new HashSet<>(),path);
        return path;
    }

    private void path(GoalDefinition goal, GoalCatalog catalog, AccountState state, GearProjection gear,
                      ManualOverrideStore overrides, Set<String> visiting, Set<String> added,
                      List<GoalPathNode> result)
    {
        if (!visiting.add(goal.getId())) return;
        for (String dependencyId : goal.getDependencyIds())
        {
            GoalDefinition dependency = catalog.find(dependencyId);
            if (dependency != null) path(dependency,catalog,state,gear,overrides,visiting,added,result);
        }
        visiting.remove(goal.getId());
        if (added.add(goal.getId())) result.add(new GoalPathNode(goal,
            completion.evaluate(goal,state,gear,overrides).getStatus()));
    }

    private Distance distance(ConditionSpec condition, AccountState state)
    {
        if (condition == null) return new Distance(10_000,false,"Requirements are not modelled.");
        String type = upper(condition.getType());
        if ("SKILL_AT_LEAST".equals(type))
        {
            int gap = Math.max(0,condition.getLevel() - state.skillLevel(condition.getSkill()));
            return new Distance(gap,true,gap == 0 ? "Known skill requirement met"
                : gap + " " + condition.getSkill() + " level" + (gap == 1 ? "" : "s") + " away");
        }
        if ("QUEST_STATE".equals(type))
        {
            RequirementResult value = conditions.evaluate(condition,state);
            return value.getValue() == TruthValue.UNKNOWN ? new Distance(10_000,false,"Quest state unknown")
                : new Distance(value.getValue() == TruthValue.TRUE ? 0 : 1,true,
                    value.getValue() == TruthValue.TRUE ? "Known quest requirement met" : "1 quest requirement away");
        }
        if ("ALL".equals(type))
        {
            int total = 0; boolean known = true; int remaining = 0;
            for (ConditionSpec child : condition.getChildren())
            {
                Distance part = distance(child,state);
                known &= part.known;
                if (part.value > 0) remaining++;
                total += Math.min(100,part.value);
            }
            return new Distance(known ? total : 10_000,known,known
                ? remaining + " known requirement" + (remaining == 1 ? "" : "s") + " remaining"
                : "Some requirements are unknown");
        }
        if ("ANY".equals(type))
        {
            Distance best = null;
            for (ConditionSpec child : condition.getChildren())
            {
                Distance part = distance(child,state);
                if (best == null || part.value < best.value) best = part;
            }
            return best == null ? new Distance(10_000,false,"Alternative requirements are not modelled") : best;
        }
        RequirementResult result = conditions.evaluate(condition,state);
        if (result.getValue() == TruthValue.UNKNOWN) return new Distance(10_000,false,result.getLabel() + " unknown");
        return new Distance(result.getValue() == TruthValue.TRUE ? 0 : 1,true,
            result.getValue() == TruthValue.TRUE ? "Known requirement met" : "1 known requirement away");
    }

    private static int alternativeScore(GoalDefinition goal, AccountState state, Set<GoalIntent> intents)
    {
        int score = goal.getUsefulness() * 20 - goal.getEffort().ordinal() * 9;
        for (GoalIntent intent : goal.getIntents()) if (intents.contains(intent)) score += 12;
        int total = state.getSkills().values().stream().mapToInt(Integer::intValue).sum();
        int stage = total < 500 ? 0 : total < 900 ? 1 : total < 1300 ? 2 : total < 1650 ? 3
            : total < 1900 ? 4 : total < 2150 ? 5 : 6;
        score -= Math.abs(goal.getStage().ordinal() - stage) * 10;
        return score;
    }

    private static int impact(GoalImpact value)
    {
        return value == GoalImpact.MAJOR ? 35 : value == GoalImpact.HIGH ? 20 : 8;
    }

    private static int accountStage(AccountState state)
    {
        int total = state.getSkills().values().stream().mapToInt(Integer::intValue).sum();
        return total < 500 ? 0 : total < 900 ? 1 : total < 1300 ? 2 : total < 1650 ? 3
            : total < 1900 ? 4 : total < 2150 ? 5 : 6;
    }

    private static String upper(String value)
    {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private static final class Distance
    {
        private final int value;
        private final boolean known;
        private final String summary;
        private Distance(int value, boolean known, String summary)
        {
            this.value = value; this.known = known; this.summary = summary;
        }
    }

    private static final class AlternativeResult
    {
        private final List<GoalDefinition> goals;
        private final boolean explicit;
        private AlternativeResult(List<GoalDefinition> goals, boolean explicit)
        {
            this.goals = goals;
            this.explicit = explicit;
        }
    }
}
