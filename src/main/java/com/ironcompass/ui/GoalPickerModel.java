package com.ironcompass.ui;

import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalCommunityWeight;
import com.ironcompass.goal.GoalCompletionEvaluation;
import com.ironcompass.goal.GoalCompletionService;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalImpact;
import com.ironcompass.goal.GoalIntent;
import com.ironcompass.goal.GoalPriority;
import com.ironcompass.goal.GoalRelationshipIndex;
import com.ironcompass.goal.GoalRequirementResolver;
import com.ironcompass.goal.GoalStage;
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.planner.AccountNeedEvaluation;
import com.ironcompass.planner.AccountNeedLevel;
import com.ironcompass.planner.AccountNeedService;
import com.ironcompass.planner.EffortClass;
import com.ironcompass.planner.ScoringWeights;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.route.RiskLevel;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.QuestProgress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Account-aware, deterministic projection for the compact Goal Picker. */
public final class GoalPickerModel
{
    public static final String SUGGESTED = "Suggested for you";
    public static final String POPULAR = "Popular";
    public static final String ACTIVE = "Active";
    public static final String ALL = "All";
    public static final String COMPLETED = "Completed";
    public static final String ANY_STAGE = "Any stage";
    private static final int SUGGESTION_LIMIT = 10;
    private final ConditionEvaluator conditions = new ConditionEvaluator();
    private final GoalCompletionService completionService = new GoalCompletionService(conditions);
    private final AccountNeedService accountNeeds = new AccountNeedService();
    private final ManualOverrideStore overrides;

    public GoalPickerModel()
    {
        this(null);
    }

    public GoalPickerModel(ManualOverrideStore overrides)
    {
        this.overrides = overrides;
    }

    public List<String> categories(GoalCatalog catalog)
    {
        Set<String> values = new LinkedHashSet<>();
        values.add(SUGGESTED);
        values.add(POPULAR);
        values.add(ACTIVE);
        values.add(ALL);
        for (GoalDefinition goal : catalog.getGoals()) values.add(goal.getCategory());
        values.add(COMPLETED);
        return new ArrayList<>(values);
    }

    public List<String> stages()
    {
        List<String> stages = new ArrayList<>();
        stages.add(ANY_STAGE);
        for (GoalStage stage : GoalStage.values()) stages.add(stage.getLabel());
        return stages;
    }

    public List<GoalDefinition> filter(GoalCatalog catalog, String query, String category, Set<String> active,
                                       AccountState state, GearProjection gear)
    {
        return filter(catalog, query, category, ANY_STAGE, active, state, gear);
    }

    public List<GoalDefinition> filter(GoalCatalog catalog, String query, String category, String stage,
                                       Set<String> active, AccountState state, GearProjection gear)
    {
        List<GoalDefinition> goals = new ArrayList<>();
        for (GoalSuggestion suggestion : suggestions(catalog, query, category, stage, active, state, gear))
            goals.add(suggestion.getGoal());
        return goals;
    }

    public List<GoalSuggestion> suggestions(GoalCatalog catalog, String query, String category, String stage,
                                             Set<String> active, AccountState state, GearProjection gear)
    {
        return suggestions(catalog, query, category, stage, active, state, gear, -1.0);
    }

    public List<GoalSuggestion> suggestions(GoalCatalog catalog, String query, String category, String stage,
                                             Set<String> active, AccountState state, GearProjection gear,
                                             double routeProgressPercent)
    {
        String search = normalize(query);
        String selectedCategory = category == null ? SUGGESTED : category;
        GoalStage selectedStage = parseStage(stage);
        Set<GoalIntent> activeIntents = activeIntents(catalog, active);
        GoalRelationshipIndex relationships = new GoalRelationshipIndex(catalog);
        List<GoalSuggestion> matched = new ArrayList<>();
        for (GoalDefinition goal : catalog.getGoals())
        {
            GoalCompletionEvaluation evaluation = completionService.evaluate(goal, state, gear, overrides);
            TruthValue completion = evaluation.getCompletion();
            if (!search.isEmpty() && !searchText(goal).contains(search)) continue;
            if (selectedStage != null && selectedStage != goal.getStage()) continue;
            if (ACTIVE.equals(selectedCategory) && !active.contains(goal.getId())) continue;
            if (COMPLETED.equals(selectedCategory) && completion != TruthValue.TRUE) continue;
            if (POPULAR.equals(selectedCategory) && !goal.isPopular()) continue;
            if (!isSpecial(selectedCategory) && !selectedCategory.equals(goal.getCategory())) continue;
            if (!COMPLETED.equals(selectedCategory) && !ACTIVE.equals(selectedCategory)
                && completion == TruthValue.TRUE) continue;
            if (SUGGESTED.equals(selectedCategory) && !supportsAccount(goal, state.getAccountMode())) continue;
            matched.add(score(goal, active.contains(goal.getId()), evaluation, state, gear, routeProgressPercent,
                active, activeIntents, relationships, catalog));
        }
        matched.sort(Comparator.comparingInt(GoalSuggestion::getScore).reversed()
            .thenComparing(value -> value.getGoal().getTitle())
            .thenComparing(value -> value.getGoal().getId()));
        if (SUGGESTED.equals(selectedCategory) && search.isEmpty() && matched.size() > SUGGESTION_LIMIT)
            return new ArrayList<>(matched.subList(0, SUGGESTION_LIMIT));
        return matched;
    }

    public GoalStage accountStage(AccountState state)
    {
        int total = 0;
        int count = 0;
        for (int level : state.getSkills().values())
        {
            total += Math.max(1, level);
            count++;
        }
        if (count == 0) return GoalStage.VERY_EARLY;
        if (total < 500) return GoalStage.VERY_EARLY;
        if (total < 900) return GoalStage.EARLY;
        if (total < 1300) return GoalStage.EARLY_MID;
        if (total < 1650) return GoalStage.MID;
        if (total < 1900) return GoalStage.MID_LATE;
        if (total < 2150) return GoalStage.LATE;
        return GoalStage.ENDGAME;
    }

    private GoalSuggestion score(GoalDefinition goal, boolean active, GoalCompletionEvaluation evaluation,
                                 AccountState state, GearProjection gear, double routeProgressPercent,
                                 Set<String> activeGoals, Set<GoalIntent> activeIntents,
                                 GoalRelationshipIndex relationships, GoalCatalog catalog)
    {
        List<String> reasons = new ArrayList<>();
        int score = goal.getUsefulness() * 20 + impact(goal.getImpact());
        score += community(goal.getCommunityWeight());
        score += priority(goal.getPriority());
        score -= effort(goal.getEffort());
        if (goal.isPopular()) score += 6;
        if (active)
        {
            score += ScoringWeights.ACTIVE_GOAL;
            reasons.add("This is already one of your active goals.");
        }

        int stageGap = Math.abs(goal.getStage().ordinal() - effectiveStage(state, routeProgressPercent).ordinal());
        score -= Math.max(0, stageGap - 1) * ScoringWeights.STAGE_MISMATCH;
        if (stageGap <= 1) reasons.add("It fits your current account stage.");

        Proximity proximity = proximity(GoalRequirementResolver.effectiveRequirements(goal, gear), state);
        score += proximity.score;
        if (proximity.reason != null) reasons.add(proximity.reason);

        if (goal.getGearId() != null && gear != null)
        {
            GearEvaluation gearEvaluation = gear.find(goal.getGearId());
            if (gearEvaluation != null && gearEvaluation.getCompletion() == TruthValue.FALSE)
            {
                score += 18;
                reasons.add("The linked gear upgrade is not yet owned.");
            }
        }

        for (GoalIntent intent : goal.getIntents())
        {
            AccountNeedEvaluation signal = accountNeeds.evaluate(intent, state, gear, catalog, overrides);
            score += signal.recommendationBonus();
            if (signal.getLevel() == AccountNeedLevel.WEAK || signal.getLevel() == AccountNeedLevel.DEVELOPING)
                reasons.add("Account need: " + signal.getPrimaryExplanation());
            if (activeIntents.contains(intent) && !active)
            {
                score += ScoringWeights.ACTIVE_INTENT_SYNERGY;
                reasons.add("It supports the same account need as one of your active goals.");
            }
        }

        for (String activeGoal : activeGoals)
            if (relationships.supports(goal.getId(), activeGoal))
            {
                score += ScoringWeights.DIRECT_RELATIONSHIP;
                reasons.add("It is directly related to one of your active goals.");
                break;
            }

        AccountMode mode = state.getAccountMode();
        if (mode.isHardcore() && goal.getRiskLevel() != RiskLevel.SAFE)
        {
            int penalty = goal.getRiskLevel() == RiskLevel.WILDERNESS
                ? ScoringWeights.HCIM_WILDERNESS : ScoringWeights.HCIM_DANGEROUS;
            score -= penalty;
            reasons.add("Your Hardcore status makes this a higher-risk choice.");
        }
        if (mode.isUltimate() && hasTag(goal, "bank-heavy"))
        {
            score -= ScoringWeights.UIM_BANK_HEAVY;
            reasons.add("This is storage-heavy for an Ultimate Ironman.");
        }
        if (goal.isRng() || goal.getPriority() == GoalPriority.RNG_GRIND)
            reasons.add("This is an optional RNG grind; no drop timing is assumed.");
        if (evaluation.getCompletion() == TruthValue.UNKNOWN)
            reasons.add("Completion cannot be confirmed from currently observed account data.");
        if (reasons.isEmpty()) reasons.add(goal.getWhyItMatters());
        return new GoalSuggestion(goal, score, evaluation, reasons);
    }

    private GoalStage effectiveStage(AccountState state, double routeProgressPercent)
    {
        GoalStage observed = accountStage(state);
        if (routeProgressPercent < 0.0) return observed;
        int routeOrdinal = routeProgressPercent < 10 ? 0 : routeProgressPercent < 25 ? 1
            : routeProgressPercent < 40 ? 2 : routeProgressPercent < 58 ? 3
            : routeProgressPercent < 72 ? 4 : routeProgressPercent < 88 ? 5 : 6;
        int blended = (int) Math.round((observed.ordinal() + routeOrdinal) / 2.0);
        return GoalStage.values()[Math.max(0, Math.min(GoalStage.values().length - 1, blended))];
    }

    private static boolean supportsAccount(GoalDefinition goal, AccountMode mode)
    {
        if (goal.getAccountTypes().isEmpty() || mode == AccountMode.UNKNOWN) return true;
        for (String accountType : goal.getAccountTypes())
            if (mode.name().equalsIgnoreCase(accountType)) return true;
        return false;
    }

    private static Proximity proximity(ConditionSpec condition, AccountState state)
    {
        if (condition == null) return new Proximity(0, null);
        String type = normalize(condition.getType()).toUpperCase(Locale.ENGLISH);
        if ("SKILL_AT_LEAST".equals(type))
        {
            int current = state.skillLevel(condition.getSkill());
            int gap = Math.max(0, condition.getLevel() - current);
            if (gap == 0) return new Proximity(14, condition.getSkill() + " requirement is ready.");
            if (gap <= 3) return new Proximity(42 - gap, "Only " + gap + " " + condition.getSkill() + " level"
                + (gap == 1 ? "" : "s") + " away.");
            if (gap <= 8) return new Proximity(26 - gap, "The key skill milestone is within " + gap + " levels.");
            if (gap <= 15) return new Proximity(8, null);
            return new Proximity(-Math.min(45, gap / 2), null);
        }
        if ("QUEST_STATE".equals(type))
        {
            QuestProgress quest = state.questState(condition.getQuest());
            if (quest == QuestProgress.FINISHED) return new Proximity(15, condition.getQuest() + " is complete.");
            if (quest == QuestProgress.IN_PROGRESS) return new Proximity(12, condition.getQuest() + " is in progress.");
            return new Proximity(quest == QuestProgress.UNKNOWN ? 0 : -6, null);
        }
        if ("ALL".equals(type) || "ANY".equals(type))
        {
            int score = 0;
            String reason = null;
            for (ConditionSpec child : condition.getChildren())
            {
                Proximity childValue = proximity(child, state);
                score += childValue.score;
                if (reason == null && childValue.reason != null) reason = childValue.reason;
            }
            return new Proximity(Math.max(-70, Math.min(45, score)), reason);
        }
        return new Proximity(0, null);
    }

    private static String searchText(GoalDefinition goal)
    {
        return normalize(String.join(" ", goal.getTitle(), goal.getDescription(), goal.getWhyItMatters(),
            goal.getCategory(), goal.getStage().getLabel(), String.join(" ", goal.getUnlocks()),
            String.join(" ", goal.getBenefits()), String.join(" ", goal.getTags()),
            String.join(" ", goal.getRelatedItems()), String.join(" ", goal.getRelatedSkills()),
            String.join(" ", goal.getRelatedQuests()), String.join(" ", goal.getRelatedActivities()),
            goal.getPriority().name(), goal.getCompletionMode().name(), goal.getCommunityWeight().name(),
            goal.getIntents().toString()));
    }

    private static boolean hasTag(GoalDefinition goal, String tag)
    {
        for (String value : goal.getTags()) if (tag.equalsIgnoreCase(value)) return true;
        return false;
    }

    private static boolean isSpecial(String category)
    {
        return SUGGESTED.equals(category) || POPULAR.equals(category) || ACTIVE.equals(category)
            || ALL.equals(category) || COMPLETED.equals(category);
    }

    private static GoalStage parseStage(String value)
    {
        if (value == null || ANY_STAGE.equals(value)) return null;
        for (GoalStage stage : GoalStage.values())
            if (stage.getLabel().equals(value) || stage.name().equalsIgnoreCase(value)) return stage;
        return null;
    }

    private static int impact(GoalImpact impact)
    {
        return impact == GoalImpact.MAJOR ? 48 : impact == GoalImpact.HIGH ? 30 : 14;
    }

    private static int community(GoalCommunityWeight weight)
    {
        return weight == GoalCommunityWeight.VERY_COMMON ? 18 : weight == GoalCommunityWeight.COMMON ? 11
            : weight == GoalCommunityWeight.NOTABLE ? 4 : 0;
    }

    private static int priority(GoalPriority value)
    {
        switch (value)
        {
            case CORE: return 24;
            case RECOMMENDED: return 10;
            case OPTIONAL: return -10;
            case COLLECTION: return -22;
            case RNG_GRIND: return -30;
            case PRESTIGE: return -36;
            default: return 0;
        }
    }

    private static int effort(EffortClass value)
    {
        switch (value)
        {
            case QUICK: return 0;
            case SHORT: return 4;
            case MEDIUM: return 10;
            case LONG: return 20;
            case VERY_LONG: return 32;
            default: return 10;
        }
    }

    private static Set<GoalIntent> activeIntents(GoalCatalog catalog, Set<String> active)
    {
        Set<GoalIntent> intents = new LinkedHashSet<>();
        for (String id : active)
        {
            GoalDefinition goal = catalog.find(id);
            if (goal != null) intents.addAll(goal.getIntents());
        }
        return intents;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    private static final class Proximity
    {
        private final int score;
        private final String reason;
        private Proximity(int score, String reason) { this.score = score; this.reason = reason; }
    }

}
