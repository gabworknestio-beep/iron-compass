package com.ironpath.gear;

import com.ironpath.persistence.ManualOverride;
import com.ironpath.persistence.ManualOverrideStore;
import com.ironpath.requirement.ConditionEvaluator;
import com.ironpath.requirement.RequirementResult;
import com.ironpath.requirement.TruthValue;
import com.ironpath.state.AccountState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GearRecommendationService
{
    private final ConditionEvaluator conditions;

    public GearRecommendationService(ConditionEvaluator conditions)
    {
        this.conditions = conditions;
    }

    public GearProjection evaluate(GearCatalog catalog, AccountState state, GearPreferenceStore preferences,
                                   ManualOverrideStore overrides)
    {
        Map<String, BaseEvaluation> base = new LinkedHashMap<>();
        for (GearUpgrade upgrade : catalog.getUpgrades())
        {
            RequirementResult completionResult = conditions.evaluate(upgrade.getCompletion(), state);
            TruthValue completion = completionResult.getValue();
            ManualOverride override = overrides.get(upgrade.getId());
            if (override == ManualOverride.FORCE_COMPLETE)
            {
                completion = TruthValue.TRUE;
            }
            else if (override == ManualOverride.FORCE_INCOMPLETE)
            {
                completion = TruthValue.FALSE;
            }
            List<RequirementResult> details = conditions.explain(upgrade.getRequirements(), state);
            TruthValue readiness = conditions.evaluate(upgrade.getRequirements(), state).getValue();
            base.put(upgrade.getId(), new BaseEvaluation(upgrade, completion, readiness, details, override));
        }

        List<GearEvaluation> results = new ArrayList<>();
        String selectedId = preferences.getSelectedGoalId();
        for (BaseEvaluation item : base.values())
        {
            List<String> missing = new ArrayList<>();
            TruthValue readiness = item.readiness;
            for (RequirementResult detail : item.details)
            {
                if (detail.getValue() != TruthValue.TRUE)
                {
                    missing.add(detail.getLabel() + detailSuffix(detail));
                }
            }
            for (String prerequisiteId : item.upgrade.getPrerequisiteIds())
            {
                BaseEvaluation prerequisite = base.get(prerequisiteId);
                if (prerequisite.completion != TruthValue.TRUE)
                {
                    readiness = TruthValue.FALSE;
                    missing.add("Prerequisite: " + prerequisite.upgrade.getName());
                }
            }

            boolean skipped = preferences.isSkipped(item.upgrade.getId())
                || item.override == ManualOverride.SKIPPED;
            GearStatus status;
            if (item.completion == TruthValue.TRUE)
            {
                status = GearStatus.OWNED;
            }
            else if (skipped)
            {
                status = GearStatus.SKIPPED;
            }
            else if (item.completion == TruthValue.UNKNOWN)
            {
                status = GearStatus.UNCONFIRMED;
            }
            else if (readiness == TruthValue.FALSE)
            {
                status = GearStatus.LOCKED;
            }
            else if (isOptional(item.upgrade, preferences))
            {
                status = GearStatus.OPTIONAL;
            }
            else
            {
                status = GearStatus.AVAILABLE;
            }

            Score score = score(item.upgrade, item.completion, readiness, missing.size(), selectedId,
                preferences, catalog);
            results.add(new GearEvaluation(item.upgrade, status, item.completion, readiness, item.details,
                missing, score.value, score.explanation, item.upgrade.getId().equals(selectedId)));
        }

        GearEvaluation recommended = selectRecommendation(results, selectedId);
        if (recommended != null && recommended.getStatus() == GearStatus.AVAILABLE)
        {
            int index = results.indexOf(recommended);
            recommended = recommended.withStatus(GearStatus.RECOMMENDED);
            results.set(index, recommended);
        }
        GearEvaluation selected = null;
        int owned = 0;
        for (GearEvaluation evaluation : results)
        {
            if (evaluation.getStatus() == GearStatus.OWNED) owned++;
                if (evaluation.getUpgrade().getId().equals(selectedId)
                    && evaluation.getStatus() != GearStatus.SKIPPED) selected = evaluation;
        }
        return new GearProjection(catalog, results, recommended, selected, owned);
    }

    private static GearEvaluation selectRecommendation(List<GearEvaluation> evaluations, String selectedId)
    {
        if (selectedId != null)
        {
            for (GearEvaluation evaluation : evaluations)
            {
                if (selectedId.equals(evaluation.getUpgrade().getId())
                    && evaluation.getStatus() == GearStatus.AVAILABLE)
                {
                    return evaluation;
                }
            }
        }
        return evaluations.stream()
            .filter(evaluation -> evaluation.getStatus() == GearStatus.AVAILABLE)
            .max(Comparator.comparingInt(GearEvaluation::getScore)
                .thenComparingInt(evaluation -> -evaluation.getUpgrade().getTier()))
            .orElseGet(() -> evaluations.stream()
                .filter(evaluation -> evaluation.getStatus() == GearStatus.LOCKED
                    && evaluation.getUpgrade().getRole() == GearRole.RECOMMENDED)
                .max(Comparator.comparingInt(GearEvaluation::getScore)).orElse(null));
    }

    private static Score score(GearUpgrade upgrade, TruthValue completion, TruthValue readiness, int missing,
                               String selectedId, GearPreferenceStore preferences, GearCatalog catalog)
    {
        int importance = "MAJOR".equalsIgnoreCase(upgrade.getImportance()) ? 35
            : "MINOR".equalsIgnoreCase(upgrade.getImportance()) ? 12 : 24;
        int role = roleScore(upgrade.getRole());
        int accessible = readiness == TruthValue.TRUE ? 28 : readiness == TruthValue.UNKNOWN ? 7 : -16;
        int usefulness = upgrade.getUsefulness() * 6;
        int distance = missing * 8;
        int effort = upgrade.getEffort().getScorePenalty();
        int difficulty = upgrade.getDifficulty().getScorePenalty();
        int preference = upgrade.getId().equals(selectedId) ? 60 : 0;
        for (GearUpgrade owner : catalog.getUpgrades())
        {
            if (upgrade.getId().equals(preferences.getChosenAlternative(owner.getId())))
            {
                preference += 45;
            }
        }
        int unknownOwnership = completion == TruthValue.UNKNOWN ? 5 : 0;
        int value = importance + role + accessible + usefulness + preference
            - distance - effort - difficulty - unknownOwnership;
        String explanation = String.format(Locale.ENGLISH,
            "value %d + role %d + access %d + usefulness %d + preference %d − distance %d − effort %d − difficulty %d%s = %d",
            importance, role, accessible, usefulness, preference, distance, effort, difficulty,
            unknownOwnership == 0 ? "" : " − bank uncertainty " + unknownOwnership, value);
        return new Score(value, explanation);
    }

    private static int roleScore(GearRole role)
    {
        switch (role)
        {
            case RECOMMENDED: return 22;
            case ALTERNATIVE: return 8;
            case OPTIONAL: return -8;
            case NICHE: return -15;
            case LONG_TERM: return -18;
            default: return 0;
        }
    }

    private static boolean isOptional(GearUpgrade upgrade, GearPreferenceStore preferences)
    {
        return preferences.isMarkedOptional(upgrade.getId()) || upgrade.getRole() == GearRole.OPTIONAL
            || upgrade.getRole() == GearRole.NICHE || upgrade.getRole() == GearRole.LONG_TERM;
    }

    private static String detailSuffix(RequirementResult detail)
    {
        return detail.getDetail() == null || detail.getDetail().isEmpty() ? "" : " — " + detail.getDetail();
    }

    private static final class BaseEvaluation
    {
        private final GearUpgrade upgrade;
        private final TruthValue completion;
        private final TruthValue readiness;
        private final List<RequirementResult> details;
        private final ManualOverride override;

        private BaseEvaluation(GearUpgrade upgrade, TruthValue completion, TruthValue readiness,
                               List<RequirementResult> details, ManualOverride override)
        {
            this.upgrade = upgrade;
            this.completion = completion;
            this.readiness = readiness;
            this.details = details;
            this.override = override;
        }
    }

    private static final class Score
    {
        private final int value;
        private final String explanation;

        private Score(int value, String explanation)
        {
            this.value = value;
            this.explanation = explanation;
        }
    }
}
