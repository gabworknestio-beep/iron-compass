package com.ironcompass.training;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import com.ironcompass.requirement.TruthValue;

public final class MethodRecommendation
{
    private final IronmanMethodDefinition recommended;
    private final List<IronmanMethodDefinition> alternatives;
    private final String reason;
    private final MethodResourceStatus resourceStatus;
    private final String resourceSummary;
    private final TruthValue requirementStatus;
    private final List<IronmanMethodDefinition> lockedAlternatives;
    private final String xpRateSummary;

    public MethodRecommendation(IronmanMethodDefinition recommended,
                                List<IronmanMethodDefinition> alternatives, String reason,
                                MethodResourceStatus resourceStatus, String resourceSummary)
    {
        this(recommended, alternatives, reason, resourceStatus, resourceSummary, TruthValue.TRUE,
            Collections.emptyList(), rateSummary(recommended));
    }

    public MethodRecommendation(IronmanMethodDefinition recommended,
                                List<IronmanMethodDefinition> alternatives, String reason,
                                MethodResourceStatus resourceStatus, String resourceSummary,
                                TruthValue requirementStatus,
                                List<IronmanMethodDefinition> lockedAlternatives, String xpRateSummary)
    {
        this.recommended = recommended;
        this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
        this.reason = reason;
        this.resourceStatus = resourceStatus;
        this.resourceSummary = resourceSummary;
        this.requirementStatus = requirementStatus;
        this.lockedAlternatives = Collections.unmodifiableList(new ArrayList<>(lockedAlternatives));
        this.xpRateSummary = xpRateSummary;
    }

    public IronmanMethodDefinition getRecommended() { return recommended; }
    public List<IronmanMethodDefinition> getAlternatives() { return alternatives; }
    public String getReason() { return reason; }
    public MethodResourceStatus getResourceStatus() { return resourceStatus; }
    public String getResourceSummary() { return resourceSummary; }
    public TruthValue getRequirementStatus() { return requirementStatus; }
    public List<IronmanMethodDefinition> getLockedAlternatives() { return lockedAlternatives; }
    public String getXpRateSummary() { return xpRateSummary; }

    private static String rateSummary(IronmanMethodDefinition method)
    {
        if (method == null || method.getXpRateMin() <= 0 || method.getXpRateMax() <= 0) return "Rate varies";
        return "~" + compact(method.getXpRateMin()) + "–" + compact(method.getXpRateMax()) + " XP/h";
    }

    private static String compact(int value)
    {
        if (value >= 1000 && value % 1000 == 0) return (value / 1000) + "k";
        if (value >= 1000) return String.format(java.util.Locale.ENGLISH, "%.1fk", value / 1000.0);
        return Integer.toString(value);
    }
}
