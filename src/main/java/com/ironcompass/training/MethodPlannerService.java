package com.ironcompass.training;

import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.planner.PlannedAction;
import com.ironcompass.planner.PlannerPreferenceStore;
import com.ironcompass.planner.Playstyle;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountMode;
import com.ironcompass.state.AccountState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * One deterministic source of truth for active goal gates, route training steps, and full skill guides.
 * It deliberately ranks total Ironman usefulness rather than XP rate alone.
 */
public final class MethodPlannerService
{
    private final ConditionEvaluator conditions;

    public MethodPlannerService(ConditionEvaluator conditions) { this.conditions = conditions; }

    /** Compatibility projection used by older callers. */
    public MethodRecommendation recommend(IronmanMethodCatalog catalog, PlannedAction action, AccountState state,
                                          PlannerPreferenceStore preferences, List<GoalPlanProjection> activeGoals)
    {
        if (action == null || action.getSkill() == null) return null;
        SkillTrainingPlan plan = plan(catalog, action.getSkill(), action.getTargetLevel(), state, preferences,
            activeGoals);
        return plan == null ? null : plan.getFirstRecommendation();
    }

    public SkillTrainingPlan plan(IronmanMethodCatalog catalog, String skill, int targetLevel, AccountState state,
                                  PlannerPreferenceStore preferences, List<GoalPlanProjection> activeGoals)
    {
        if (state == null) return null;
        int currentLevel = state.skillLevel(skill);
        if (currentLevel < 1) return null;
        return plan(catalog, skill, currentLevel, targetLevel, state, preferences, activeGoals);
    }

    public SkillTrainingPlan fullGuide(IronmanMethodCatalog catalog, String skill, AccountState state,
                                       PlannerPreferenceStore preferences, List<GoalPlanProjection> activeGoals)
    {
        return plan(catalog, skill, 1, 99, state, preferences, activeGoals);
    }

    public SkillTrainingPlan plan(IronmanMethodCatalog catalog, String skill, int currentLevel, int targetLevel,
                                  AccountState state, PlannerPreferenceStore preferences,
                                  List<GoalPlanProjection> activeGoals)
    {
        if (catalog == null || skill == null || state == null || preferences == null
            || currentLevel < 1 || currentLevel > 99 || targetLevel < 1 || targetLevel > 99) return null;
        int target = Math.max(currentLevel, targetLevel);
        List<GoalPlanProjection> goals = activeGoals == null ? Collections.emptyList() : activeGoals;
        List<TrainingPlanSegment> segments = target == currentLevel ? Collections.emptyList()
            : buildSegments(catalog, skill, currentLevel, target, state, preferences, goals);
        int xpRemaining = Math.max(0, xpForLevel(target) - xpForLevel(currentLevel));
        return new SkillTrainingPlan(canonicalSkill(catalog, skill), currentLevel, target, xpRemaining,
            estimate(segments), segments, catalog.milestonesFor(skill, currentLevel, target));
    }

    private List<TrainingPlanSegment> buildSegments(IronmanMethodCatalog catalog, String skill, int current,
                                                    int target, AccountState state,
                                                    PlannerPreferenceStore preferences,
                                                    List<GoalPlanProjection> activeGoals)
    {
        TreeSet<Integer> boundaries = new TreeSet<>();
        boundaries.add(current);
        boundaries.add(target);
        for (IronmanMethodDefinition method : catalog.getMethods())
        {
            if (!method.isTrainingMethod() || !method.getSkill().equalsIgnoreCase(skill)) continue;
            if (method.getMinLevel() > current && method.getMinLevel() < target) boundaries.add(method.getMinLevel());
            int end = method.getMaxLevel() + 1;
            if (end > current && end < target) boundaries.add(end);
        }
        List<Integer> levels = new ArrayList<>(boundaries);
        List<TrainingPlanSegment> raw = new ArrayList<>();
        for (int index = 0; index + 1 < levels.size(); index++)
        {
            int from = levels.get(index);
            int to = levels.get(index + 1);
            MethodRecommendation recommendation = choose(catalog, skill, from, state, preferences, activeGoals);
            if (recommendation != null) raw.add(new TrainingPlanSegment(from, to, recommendation));
        }
        return mergeAdjacent(raw);
    }

    private MethodRecommendation choose(IronmanMethodCatalog catalog, String skill, int level, AccountState state,
                                        PlannerPreferenceStore preferences,
                                        List<GoalPlanProjection> activeGoals)
    {
        AccountState projectedState = state.skillLevel(skill) >= level ? state : state.withSkillLevel(skill, level);
        List<ScoredMethod> candidates = new ArrayList<>();
        List<ScoredMethod> locked = new ArrayList<>();
        for (IronmanMethodDefinition method : catalog.getMethods())
        {
            if (!method.isTrainingMethod() || !method.getSkill().equalsIgnoreCase(skill)
                || level < method.getMinLevel() || level > method.getMaxLevel()
                || !supportsAccount(method, state.getAccountMode())) continue;
            boolean wilderness = "WILDERNESS".equalsIgnoreCase(method.getRisk());
            if (wilderness && (preferences.isAvoidWilderness() || state.getAccountMode().isHardcore())) continue;
            TruthValue requirements = conditions.evaluate(method.getRequirements(), projectedState).getValue();
            MethodResourceStatus resources = resourceStatus(method, state);
            int related = relatedGoals(method, activeGoals);
            ScoredMethod scored = new ScoredMethod(method,
                score(method, requirements, resources, related, preferences), resources, requirements, related);
            if (requirements == TruthValue.FALSE) locked.add(scored);
            else candidates.add(scored);
        }
        Comparator<ScoredMethod> order = Comparator.comparingInt(ScoredMethod::getScore).reversed()
            .thenComparing(candidate -> candidate.method.getId());
        candidates.sort(order);
        locked.sort(order);
        if (candidates.isEmpty()) return null;
        ScoredMethod best = candidates.get(0);
        List<IronmanMethodDefinition> alternatives = new ArrayList<>();
        for (int index = 1; index < candidates.size() && alternatives.size() < 2; index++)
            alternatives.add(candidates.get(index).method);
        List<IronmanMethodDefinition> lockedAlternatives = new ArrayList<>();
        for (ScoredMethod candidate : locked)
            if (lockedAlternatives.size() < 2) lockedAlternatives.add(candidate.method);
        return new MethodRecommendation(best.method, alternatives, reason(best, preferences), best.resources,
            resourceSummary(best.method, best.resources, state), best.requirements, lockedAlternatives,
            rateSummary(best.method));
    }

    private static int score(IronmanMethodDefinition method, TruthValue requirements,
                             MethodResourceStatus resources, int relatedGoals,
                             PlannerPreferenceStore preferences)
    {
        int score = method.getRecommendationPriority() + method.getIronmanValue() * 3;
        if (requirements == TruthValue.TRUE) score += 12;
        if (method.getSessionEffort().fits(preferences.getSessionLength())) score += 8;
        else score -= 5;
        if (contains(method.getPlaystyles(), preferences.getPlaystyle().name())) score += 14;
        if (contains(method.getStyles(), "RESOURCE_POSITIVE")) score += 7;
        if (contains(method.getStyles(), "LOW_COST")) score += 5;
        if (preferences.getPlaystyle() == Playstyle.EFFICIENT && contains(method.getStyles(), "EFFICIENT"))
            score += 10;
        if (preferences.getPlaystyle() == Playstyle.SKILLING
            && (contains(method.getPlaystyles(), "SKILLING") || contains(method.getStyles(), "CHILL")))
            score += 7;
        if (preferences.getPlaystyle() == Playstyle.PVM
            && (contains(method.getPlaystyles(), "PVM") || contains(method.getTags(), "combat")))
            score += 9;
        score += relatedGoals * 12;
        if (resources == MethodResourceStatus.SUFFICIENT) score += 16;
        else if (resources == MethodResourceStatus.PARTIAL) score -= 4;
        else if (resources == MethodResourceStatus.EMPTY) score -= 14;
        // UNKNOWN is intentionally neutral: an unopened bank is not an empty bank.
        return score;
    }

    private static String reason(ScoredMethod method, PlannerPreferenceStore preferences)
    {
        if (method.relatedGoals > 1) return "This method advances several of your active goals at once.";
        if (method.relatedGoals == 1) return "This method directly supports your active goal.";
        if (method.resources == MethodResourceStatus.SUFFICIENT)
            return "Your observed inputs are enough to begin this method; this is not a banked-XP promise.";
        if (contains(method.method.getStyles(), "RESOURCE_POSITIVE"))
            return "This route trains the skill while producing resources useful elsewhere on an Ironman.";
        if (contains(method.method.getPlaystyles(), preferences.getPlaystyle().name()))
            return "This method matches your selected playstyle and current unlocks.";
        if (method.method.getSessionEffort().fits(preferences.getSessionLength()))
            return "This is a practical fit for the target and selected session length.";
        return "This is the strongest available fit for this level band and account state.";
    }

    private static int relatedGoals(IronmanMethodDefinition method, List<GoalPlanProjection> activeGoals)
    {
        int related = 0;
        for (GoalPlanProjection goal : activeGoals)
        {
            if (contains(method.getRelatedGoals(), goal.getGoalId()))
            {
                related++;
                continue;
            }
            String title = normalized(goal.getTitle());
            for (String tag : method.getTags())
                if (!tag.isEmpty() && title.contains(normalized(tag)))
                {
                    related++;
                    break;
                }
        }
        return related;
    }

    private static List<TrainingPlanSegment> mergeAdjacent(List<TrainingPlanSegment> raw)
    {
        List<TrainingPlanSegment> merged = new ArrayList<>();
        for (TrainingPlanSegment segment : raw)
        {
            if (!merged.isEmpty())
            {
                TrainingPlanSegment previous = merged.get(merged.size() - 1);
                IronmanMethodDefinition previousMethod = previous.getRecommendation().getRecommended();
                IronmanMethodDefinition method = segment.getRecommendation().getRecommended();
                if (previous.getToLevel() == segment.getFromLevel() && previousMethod != null && method != null
                    && previousMethod.getId().equals(method.getId()))
                {
                    merged.set(merged.size() - 1, new TrainingPlanSegment(previous.getFromLevel(),
                        segment.getToLevel(), previous.getRecommendation()));
                    continue;
                }
            }
            merged.add(segment);
        }
        return Collections.unmodifiableList(merged);
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
                return "Resources unconfirmed — open your bank once to include stored supplies.";
            case EMPTY:
                return "No authored starting inputs were observed. Use the listed supply chain first.";
            case PARTIAL:
                return "Some useful inputs are observed, but at least one starting input is missing.";
            case SUFFICIENT:
                return "Observed inputs meet the starting threshold; this is not a banked-XP estimate.";
            default:
                return state.getBank().isObserved() ? "No specific stored input is required."
                    : "This method does not require a bank check.";
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
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH).replace('_', '-').replace(' ', '-');
    }

    private static String canonicalSkill(IronmanMethodCatalog catalog, String skill)
    {
        for (String value : catalog.getFullGuideSkills()) if (value.equalsIgnoreCase(skill)) return value;
        for (IronmanMethodDefinition method : catalog.getMethods())
            if (method.getSkill().equalsIgnoreCase(skill)) return method.getSkill();
        return skill;
    }

    private static String rateSummary(IronmanMethodDefinition method)
    {
        if (method.getXpRateMin() <= 0 || method.getXpRateMax() <= 0) return "Rate varies by account and execution";
        return "~" + compact(method.getXpRateMin()) + "–" + compact(method.getXpRateMax()) + " XP/h";
    }

    private static String estimate(List<TrainingPlanSegment> segments)
    {
        if (segments.isEmpty()) return "Complete";
        double lowHours = 0.0;
        double highHours = 0.0;
        for (TrainingPlanSegment segment : segments)
        {
            IronmanMethodDefinition method = segment.getRecommendation().getRecommended();
            if (method == null || method.getXpRateMin() <= 0 || method.getXpRateMax() <= 0)
                return "Varies — at least one method has no stable XP range";
            int xp = xpForLevel(segment.getToLevel()) - xpForLevel(segment.getFromLevel());
            lowHours += xp / (double) method.getXpRateMax();
            highHours += xp / (double) method.getXpRateMin();
        }
        return "~" + time(lowHours) + "–" + time(highHours) + " (rough)";
    }

    private static String time(double hours)
    {
        if (hours < 1.0) return Math.max(5, (int) Math.round(hours * 60 / 5.0) * 5) + " min";
        if (hours < 10.0) return String.format(Locale.ENGLISH, "%.1f h", hours);
        return Math.round(hours) + " h";
    }

    private static String compact(int value)
    {
        if (value >= 1000 && value % 1000 == 0) return (value / 1000) + "k";
        if (value >= 1000) return String.format(Locale.ENGLISH, "%.1fk", value / 1000.0);
        return Integer.toString(value);
    }

    static int xpForLevel(int level)
    {
        int points = 0;
        for (int current = 1; current < Math.max(1, Math.min(99, level)); current++)
            points += (int) Math.floor(current + 300.0 * Math.pow(2.0, current / 7.0));
        return points / 4;
    }

    private static final class ScoredMethod
    {
        private final IronmanMethodDefinition method;
        private final int score;
        private final MethodResourceStatus resources;
        private final TruthValue requirements;
        private final int relatedGoals;

        private ScoredMethod(IronmanMethodDefinition method, int score, MethodResourceStatus resources,
                             TruthValue requirements, int relatedGoals)
        {
            this.method = method;
            this.score = score;
            this.resources = resources;
            this.requirements = requirements;
            this.relatedGoals = relatedGoals;
        }

        private int getScore() { return score; }
    }
}
