package com.ironcompass.route;

import com.ironcompass.requirement.RequirementResult;
import com.ironcompass.requirement.TruthValue;
import java.util.Collections;
import java.util.List;

public final class StepEvaluation
{
    private final RouteStep step;
    private final StepStatus status;
    private final TruthValue completion;
    private final TruthValue readiness;
    private final List<RequirementResult> readinessDetails;
    private final String explanation;

    public StepEvaluation(RouteStep step, StepStatus status, TruthValue completion, TruthValue readiness,
                          List<RequirementResult> readinessDetails, String explanation)
    {
        this.step = step;
        this.status = status;
        this.completion = completion;
        this.readiness = readiness;
        this.readinessDetails = Collections.unmodifiableList(readinessDetails);
        this.explanation = explanation;
    }

    public RouteStep getStep() { return step; }
    public StepStatus getStatus() { return status; }
    public TruthValue getCompletion() { return completion; }
    public TruthValue getReadiness() { return readiness; }
    public List<RequirementResult> getReadinessDetails() { return readinessDetails; }
    public String getExplanation() { return explanation; }

    public StepEvaluation withStatus(StepStatus replacement)
    {
        return new StepEvaluation(step, replacement, completion, readiness, readinessDetails, explanation);
    }
}
