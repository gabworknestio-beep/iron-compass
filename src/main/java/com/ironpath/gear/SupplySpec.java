package com.ironpath.gear;

import java.util.Collections;
import java.util.List;

public final class SupplySpec
{
    private String name;
    private int requiredUnits;
    private String unitLabel;
    private boolean estimated = true;
    private List<SupplyVariant> variants;

    public String getName()
    {
        return name;
    }

    public int getRequiredUnits()
    {
        return Math.max(1, requiredUnits);
    }

    public String getUnitLabel()
    {
        return unitLabel == null || unitLabel.trim().isEmpty() ? "units" : unitLabel;
    }

    public boolean isEstimated()
    {
        return estimated;
    }

    public List<SupplyVariant> getVariants()
    {
        return variants == null ? Collections.emptyList() : variants;
    }
}
