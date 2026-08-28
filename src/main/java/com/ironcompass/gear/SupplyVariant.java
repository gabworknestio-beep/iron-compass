package com.ironcompass.gear;

public final class SupplyVariant
{
    private int itemId;
    private int units = 1;

    public int getItemId()
    {
        return itemId;
    }

    public int getUnits()
    {
        return Math.max(1, units);
    }
}
