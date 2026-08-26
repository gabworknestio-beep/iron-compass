package com.ironpath.route;

import com.ironpath.persistence.ManualOverride;
import com.ironpath.persistence.ManualOverrideStore;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.requirement.RequirementResult;
import com.ironpath.requirement.TruthValue;
import com.ironpath.state.AccountState;
import com.ironpath.state.BankSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RouteEvaluator
{
    private final ConditionEvaluator conditions;

    public RouteEvaluator(ConditionEvaluator conditions)
    {
        this.conditions = conditions;
    }

    public RouteProjection evaluate(Route route, AccountState state, ManualOverrideStore overrides,
                                    boolean preferSafeAlternatives, int upcomingCount, int preparationLookahead)
    {
        List<RouteStep> effectiveSteps = effectiveSteps(route, state, preferSafeAlternatives);
        List<StepEvaluation> evaluations = new ArrayList<>();
        Map<String, StepEvaluation> byId = new HashMap<>();
        Map<String, String> alternativeAliases = new HashMap<>();
        for (RouteStep step : effectiveSteps)
        {
            if (step.getAlternativeForStepId() != null)
            {
                alternativeAliases.put(step.getAlternativeForStepId(), step.getId());
            }
            StepEvaluation evaluation = evaluateStep(step, state, overrides, byId, alternativeAliases);
            evaluations.add(evaluation);
            byId.put(step.getId(), evaluation);
        }

        int currentIndex = -1;
        for (int i = 0; i < evaluations.size(); i++)
        {
            StepStatus status = evaluations.get(i).getStatus();
            if (status != StepStatus.COMPLETE && status != StepStatus.SKIPPED_MANUALLY && status != StepStatus.OPTIONAL)
            {
                currentIndex = i;
                if (status == StepStatus.READY || status == StepStatus.UNKNOWN)
                {
                    evaluations.set(i, evaluations.get(i).withStatus(StepStatus.CURRENT));
                }
                break;
            }
        }

        StepEvaluation current = currentIndex < 0 ? null : evaluations.get(currentIndex);
        List<StepEvaluation> upcoming = new ArrayList<>();
        if (currentIndex >= 0)
        {
            for (int i = currentIndex + 1; i < evaluations.size() && upcoming.size() < upcomingCount; i++)
            {
                StepEvaluation candidate = evaluations.get(i);
                if (candidate.getStatus() != StepStatus.COMPLETE && candidate.getStatus() != StepStatus.SKIPPED_MANUALLY)
                {
                    upcoming.add(candidate);
                }
            }
        }

        int complete = 0;
        int total = 0;
        for (StepEvaluation evaluation : evaluations)
        {
            if (!evaluation.getStep().isOptional())
            {
                total++;
                if (evaluation.getStatus() == StepStatus.COMPLETE || evaluation.getStatus() == StepStatus.SKIPPED_MANUALLY)
                {
                    complete++;
                }
            }
        }

        List<PreparationEvaluation> preparation = aggregatePreparation(evaluations, currentIndex, preparationLookahead, state);
        return new RouteProjection(route, evaluations, current, upcoming, preparation, complete, total);
    }

    private StepEvaluation evaluateStep(RouteStep step, AccountState state, ManualOverrideStore overrides,
                                        Map<String, StepEvaluation> prior, Map<String, String> alternativeAliases)
    {
        ManualOverride override = overrides.get(step.getId());
        if (override == ManualOverride.FORCE_COMPLETE)
        {
            return new StepEvaluation(step, StepStatus.COMPLETE, TruthValue.TRUE, TruthValue.TRUE,
                List.of(), "Marked complete manually");
        }
        if (override == ManualOverride.SKIPPED)
        {
            return new StepEvaluation(step, StepStatus.SKIPPED_MANUALLY, TruthValue.FALSE, TruthValue.TRUE,
                List.of(), "Skipped manually");
        }

        RequirementResult completionResult = conditions.evaluate(step.getCompletion(), state);
        TruthValue completion = completionResult.getValue();
        if (override != ManualOverride.FORCE_INCOMPLETE && completion == TruthValue.TRUE)
        {
            return new StepEvaluation(step, StepStatus.COMPLETE, completion, TruthValue.TRUE,
                List.of(), completionResult.getDetail());
        }

        boolean prerequisitesMet = true;
        String blockedBy = null;
        for (String requiredId : step.getRequires())
        {
            StepEvaluation requirement = prior.get(requiredId);
            if (requirement == null && alternativeAliases.containsKey(requiredId))
            {
                requirement = prior.get(alternativeAliases.get(requiredId));
            }
            if (requirement == null || (requirement.getStatus() != StepStatus.COMPLETE
                && requirement.getStatus() != StepStatus.SKIPPED_MANUALLY))
            {
                prerequisitesMet = false;
                blockedBy = requiredId;
                break;
            }
        }

        RequirementResult readinessResult = conditions.evaluate(step.getReadiness(), state);
        TruthValue readiness = readinessResult.getValue();
        List<RequirementResult> details = conditions.explain(step.getReadiness(), state);
        StepStatus status;
        String explanation;
        if (step.isOptional())
        {
            status = StepStatus.OPTIONAL;
            explanation = "Optional route step";
        }
        else if (!prerequisitesMet)
        {
            status = StepStatus.BLOCKED;
            explanation = "Blocked by " + blockedBy;
        }
        else if (readiness == TruthValue.FALSE)
        {
            status = StepStatus.BLOCKED;
            explanation = readinessResult.getDetail();
        }
        else if (readiness == TruthValue.UNKNOWN)
        {
            status = StepStatus.UNKNOWN;
            explanation = readinessResult.getDetail();
        }
        else
        {
            status = StepStatus.READY;
            explanation = override == ManualOverride.FORCE_INCOMPLETE ? "Marked incomplete manually" : "Ready";
        }
        return new StepEvaluation(step, status, completion, readiness, details, explanation);
    }

    private List<RouteStep> effectiveSteps(Route route, AccountState state, boolean preferSafeAlternatives)
    {
        Map<String, RouteStep> byId = new HashMap<>();
        List<RouteStep> canonical = new ArrayList<>();
        for (RouteSection section : route.getSections())
        {
            for (RouteStep step : section.getSteps())
            {
                byId.put(step.getId(), step);
                canonical.add(step);
            }
        }
        List<RouteStep> effective = new ArrayList<>();
        for (RouteStep step : canonical)
        {
            if (step.getAlternativeForStepId() != null)
            {
                continue;
            }
            if (preferSafeAlternatives && state.getAccountMode().isHardcore()
                && step.getHcimAlternativeStepId() != null)
            {
                RouteStep alternative = byId.get(step.getHcimAlternativeStepId());
                if (alternative != null)
                {
                    effective.add(alternative);
                    continue;
                }
            }
            effective.add(step);
        }
        return effective;
    }

    private List<PreparationEvaluation> aggregatePreparation(List<StepEvaluation> evaluations, int currentIndex,
                                                             int lookahead, AccountState state)
    {
        if (currentIndex < 0)
        {
            return List.of();
        }
        Map<String, PreparationSpec> merged = new LinkedHashMap<>();
        Map<String, Integer> quantities = new HashMap<>();
        int meaningful = 0;
        for (int i = currentIndex; i < evaluations.size() && meaningful < lookahead; i++)
        {
            StepEvaluation evaluation = evaluations.get(i);
            if (evaluation.getStatus() == StepStatus.COMPLETE || evaluation.getStatus() == StepStatus.SKIPPED_MANUALLY)
            {
                continue;
            }
            meaningful++;
            for (PreparationSpec preparation : evaluation.getStep().getPreparation())
            {
                String key = preparationKey(preparation);
                merged.putIfAbsent(key, preparation);
                int previous = quantities.getOrDefault(key, 0);
                quantities.put(key, preparation.isConsumable()
                    ? previous + preparation.getQuantity()
                    : Math.max(previous, preparation.getQuantity()));
            }
        }

        List<PreparationEvaluation> results = new ArrayList<>();
        for (Map.Entry<String, PreparationSpec> entry : merged.entrySet())
        {
            PreparationSpec preparation = entry.getValue();
            int needed = quantities.get(entry.getKey());
            if ("SKILL".equalsIgnoreCase(preparation.getKind()))
            {
                int actual = state.skillLevel(preparation.getSkill());
                results.add(new PreparationEvaluation(preparation,
                    actual >= preparation.getLevel() ? PreparationStatus.KNOWN_PRESENT : PreparationStatus.KNOWN_MISSING,
                    actual, preparation.getLevel()));
                continue;
            }
            int carried = state.carriedQuantity(preparation.getItemId());
            String source = preparation.getSource().toUpperCase(Locale.ENGLISH);
            int actual;
            PreparationStatus status;
            if ("INVENTORY".equals(source) || "CARRIED".equals(source))
            {
                actual = carried;
                status = actual >= needed ? PreparationStatus.KNOWN_PRESENT : PreparationStatus.KNOWN_MISSING;
            }
            else
            {
                BankSnapshot bank = state.getBank();
                if (carried >= needed)
                {
                    actual = carried;
                    status = PreparationStatus.KNOWN_PRESENT;
                }
                else if (!bank.isObserved())
                {
                    actual = carried;
                    status = PreparationStatus.UNKNOWN;
                }
                else
                {
                    actual = carried + bank.quantity(preparation.getItemId());
                    status = actual >= needed ? PreparationStatus.KNOWN_PRESENT : PreparationStatus.KNOWN_MISSING;
                }
            }
            results.add(new PreparationEvaluation(preparation, status, actual, needed));
        }
        return results;
    }

    private static String preparationKey(PreparationSpec preparation)
    {
        if ("SKILL".equalsIgnoreCase(preparation.getKind()))
        {
            return "skill:" + preparation.getSkill().toLowerCase(Locale.ENGLISH);
        }
        return "item:" + preparation.getItemId() + ":" + preparation.getSource().toLowerCase(Locale.ENGLISH);
    }

}
