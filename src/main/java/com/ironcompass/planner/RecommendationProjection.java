package com.ironcompass.planner;

public final class RecommendationProjection
{
    private final ProgressionCandidate recommended;
    private final ProgressionCandidate quickWin;
    private final ProgressionCandidate longTerm;
    private final UnlockOpportunity newOpportunity;

    public RecommendationProjection(ProgressionCandidate recommended, ProgressionCandidate quickWin,
                                    ProgressionCandidate longTerm, UnlockOpportunity newOpportunity)
    {
        this.recommended = recommended;
        this.quickWin = quickWin;
        this.longTerm = longTerm;
        this.newOpportunity = newOpportunity;
    }

    public ProgressionCandidate getRecommended() { return recommended; }
    public ProgressionCandidate getQuickWin() { return quickWin; }
    public ProgressionCandidate getLongTerm() { return longTerm; }
    public UnlockOpportunity getNewOpportunity() { return newOpportunity; }

    public RecommendationProjection withNewOpportunity(UnlockOpportunity opportunity)
    {
        return new RecommendationProjection(recommended, quickWin, longTerm, opportunity);
    }
}
