package com.ironcompass.route;

import java.util.Collections;
import java.util.List;

public final class RouteProjection
{
    private final Route route;
    private final List<StepEvaluation> steps;
    private final StepEvaluation current;
    private final List<StepEvaluation> upcoming;
    private final List<PreparationEvaluation> preparation;
    private final int completeCount;
    private final int totalCount;

    public RouteProjection(Route route, List<StepEvaluation> steps, StepEvaluation current,
                           List<StepEvaluation> upcoming, List<PreparationEvaluation> preparation,
                           int completeCount, int totalCount)
    {
        this.route = route;
        this.steps = Collections.unmodifiableList(steps);
        this.current = current;
        this.upcoming = Collections.unmodifiableList(upcoming);
        this.preparation = Collections.unmodifiableList(preparation);
        this.completeCount = completeCount;
        this.totalCount = totalCount;
    }

    public Route getRoute() { return route; }
    public List<StepEvaluation> getSteps() { return steps; }
    public StepEvaluation getCurrent() { return current; }
    public List<StepEvaluation> getUpcoming() { return upcoming; }
    public List<PreparationEvaluation> getPreparation() { return preparation; }
    public int getCompleteCount() { return completeCount; }
    public int getTotalCount() { return totalCount; }

    public double getProgressPercent()
    {
        return totalCount == 0 ? 100.0 : completeCount * 100.0 / totalCount;
    }
}
