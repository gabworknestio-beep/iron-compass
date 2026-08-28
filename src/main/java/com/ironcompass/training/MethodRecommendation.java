package com.ironcompass.training;

import java.util.Collections;
import java.util.List;

public final class MethodRecommendation
{
    private final IronmanMethodDefinition recommended;
    private final List<IronmanMethodDefinition> alternatives;
    private final String reason;
    private final MethodResourceStatus resourceStatus;
    private final String resourceSummary;

    public MethodRecommendation(IronmanMethodDefinition recommended,
                                List<IronmanMethodDefinition> alternatives, String reason,
                                MethodResourceStatus resourceStatus, String resourceSummary)
    {
        this.recommended = recommended;
        this.alternatives = Collections.unmodifiableList(alternatives);
        this.reason = reason;
        this.resourceStatus = resourceStatus;
        this.resourceSummary = resourceSummary;
    }

    public IronmanMethodDefinition getRecommended() { return recommended; }
    public List<IronmanMethodDefinition> getAlternatives() { return alternatives; }
    public String getReason() { return reason; }
    public MethodResourceStatus getResourceStatus() { return resourceStatus; }
    public String getResourceSummary() { return resourceSummary; }
}
