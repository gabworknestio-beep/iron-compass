package com.ironcompass.goal;

import com.ironcompass.persistence.ManualOverride;
import com.ironcompass.requirement.TruthValue;

public final class GoalCompletionEvaluation
{
    private final TruthValue completion;
    private final TruthValue readiness;
    private final GoalStatus status;
    private final ManualOverride manualOverride;
    private final String explanation;

    GoalCompletionEvaluation(TruthValue completion, TruthValue readiness, GoalStatus status,
                             ManualOverride manualOverride, String explanation)
    {
        this.completion = completion;
        this.readiness = readiness;
        this.status = status;
        this.manualOverride = manualOverride;
        this.explanation = explanation;
    }

    public TruthValue getCompletion() { return completion; }
    public TruthValue getReadiness() { return readiness; }
    public GoalStatus getStatus() { return status; }
    public ManualOverride getManualOverride() { return manualOverride; }
    public String getExplanation() { return explanation; }
}
