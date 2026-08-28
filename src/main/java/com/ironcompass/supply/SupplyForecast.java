package com.ironcompass.supply;

import com.ironcompass.gear.GearEvaluation;
import java.util.Collections;
import java.util.List;

public final class SupplyForecast
{
    private final GearEvaluation goal;
    private final List<SupplyLine> lines;

    public SupplyForecast(GearEvaluation goal, List<SupplyLine> lines)
    {
        this.goal = goal;
        this.lines = Collections.unmodifiableList(lines);
    }

    public GearEvaluation getGoal() { return goal; }
    public List<SupplyLine> getLines() { return lines; }
}
