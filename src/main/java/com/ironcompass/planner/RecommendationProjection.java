package com.ironcompass.planner;

import java.util.Collections;
import java.util.List;

public final class RecommendationProjection
{
    private final ProgressionCandidate recommended;
    private final ProgressionCandidate quickWin;
    private final ProgressionCandidate longTerm;
    private final UnlockOpportunity newOpportunity;
    private final List<ProgressionCandidate> usefulBreaks;

    public RecommendationProjection(ProgressionCandidate recommended, ProgressionCandidate quickWin,
                                    ProgressionCandidate longTerm, UnlockOpportunity newOpportunity)
    {
        this(recommended, quickWin, longTerm, newOpportunity, Collections.emptyList());
    }

    public RecommendationProjection(ProgressionCandidate recommended, ProgressionCandidate quickWin,
                                    ProgressionCandidate longTerm, UnlockOpportunity newOpportunity,
                                    List<ProgressionCandidate> usefulBreaks)
    {
        this.recommended = recommended;
        this.quickWin = quickWin;
        this.longTerm = longTerm;
        this.newOpportunity = newOpportunity;
        this.usefulBreaks = Collections.unmodifiableList(usefulBreaks);
    }

    public ProgressionCandidate getRecommended() { return recommended; }
    public ProgressionCandidate getQuickWin() { return quickWin; }
    public ProgressionCandidate getLongTerm() { return longTerm; }
    public UnlockOpportunity getNewOpportunity() { return newOpportunity; }
    public List<ProgressionCandidate> getUsefulBreaks() { return usefulBreaks; }

    public RecommendationProjection withNewOpportunity(UnlockOpportunity opportunity)
    {
        return new RecommendationProjection(recommended, quickWin, longTerm, opportunity, usefulBreaks);
    }
}
