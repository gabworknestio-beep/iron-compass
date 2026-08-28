package com.ironcompass.route;

import java.util.Collections;
import java.util.List;

public final class RouteChapterProgress
{
    private final RouteChapterSpec chapter;
    private final List<StepEvaluation> steps;
    private final int completeCount;
    private final int totalCount;
    private final boolean current;

    public RouteChapterProgress(RouteChapterSpec chapter, List<StepEvaluation> steps, int completeCount,
                                int totalCount, boolean current)
    {
        this.chapter = chapter;
        this.steps = Collections.unmodifiableList(steps);
        this.completeCount = completeCount;
        this.totalCount = totalCount;
        this.current = current;
    }

    public RouteChapterSpec getChapter() { return chapter; }
    public List<StepEvaluation> getSteps() { return steps; }
    public int getCompleteCount() { return completeCount; }
    public int getTotalCount() { return totalCount; }
    public boolean isCurrent() { return current; }
    public boolean isComplete() { return totalCount > 0 && completeCount == totalCount; }
}
