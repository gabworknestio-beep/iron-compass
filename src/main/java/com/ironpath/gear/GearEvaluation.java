package com.ironpath.gear;

import com.ironpath.requirement.RequirementResult;
import com.ironpath.requirement.TruthValue;
import java.util.Collections;
import java.util.List;

public final class GearEvaluation
{
    private final GearUpgrade upgrade;
    private final GearStatus status;
    private final TruthValue completion;
    private final TruthValue readiness;
    private final List<RequirementResult> readinessDetails;
    private final List<String> missingReasons;
    private final int score;
    private final String scoreExplanation;
    private final boolean selectedGoal;

    public GearEvaluation(GearUpgrade upgrade, GearStatus status, TruthValue completion, TruthValue readiness,
                          List<RequirementResult> readinessDetails, List<String> missingReasons, int score,
                          String scoreExplanation, boolean selectedGoal)
    {
        this.upgrade = upgrade;
        this.status = status;
        this.completion = completion;
        this.readiness = readiness;
        this.readinessDetails = Collections.unmodifiableList(readinessDetails);
        this.missingReasons = Collections.unmodifiableList(missingReasons);
        this.score = score;
        this.scoreExplanation = scoreExplanation;
        this.selectedGoal = selectedGoal;
    }

    public GearUpgrade getUpgrade() { return upgrade; }
    public GearStatus getStatus() { return status; }
    public TruthValue getCompletion() { return completion; }
    public TruthValue getReadiness() { return readiness; }
    public List<RequirementResult> getReadinessDetails() { return readinessDetails; }
    public List<String> getMissingReasons() { return missingReasons; }
    public int getScore() { return score; }
    public String getScoreExplanation() { return scoreExplanation; }
    public boolean isSelectedGoal() { return selectedGoal; }

    public GearEvaluation withStatus(GearStatus replacement)
    {
        return new GearEvaluation(upgrade, replacement, completion, readiness, readinessDetails, missingReasons,
            score, scoreExplanation, selectedGoal);
    }
}
