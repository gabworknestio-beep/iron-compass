package com.ironpath.planner;

import com.ironpath.requirement.TruthValue;
import com.ironpath.supply.SupplyForecast;

public final class ResourceReadiness
{
    private final TruthValue value;
    private final String summary;
    private final SupplyForecast forecast;

    public ResourceReadiness(TruthValue value, String summary, SupplyForecast forecast)
    {
        this.value = value;
        this.summary = summary;
        this.forecast = forecast;
    }

    public TruthValue getValue() { return value; }
    public String getSummary() { return summary; }
    public SupplyForecast getForecast() { return forecast; }
}
