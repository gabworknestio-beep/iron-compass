package com.ironpath.supply;

import com.ironpath.requirement.TruthValue;

public final class SupplyLine
{
    private final String name;
    private final int actualUnits;
    private final int requiredUnits;
    private final String unitLabel;
    private final boolean estimated;
    private final TruthValue status;

    public SupplyLine(String name, int actualUnits, int requiredUnits, String unitLabel, boolean estimated,
                      TruthValue status)
    {
        this.name = name;
        this.actualUnits = actualUnits;
        this.requiredUnits = requiredUnits;
        this.unitLabel = unitLabel;
        this.estimated = estimated;
        this.status = status;
    }

    public String getName() { return name; }
    public int getActualUnits() { return actualUnits; }
    public int getRequiredUnits() { return requiredUnits; }
    public String getUnitLabel() { return unitLabel; }
    public boolean isEstimated() { return estimated; }
    public TruthValue getStatus() { return status; }
}
