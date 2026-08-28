package com.ironcompass.route;

import java.util.Collections;
import java.util.List;

public final class RouteSection
{
    private String id;
    private String name;
    private String description;
    private List<RouteStep> steps;

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public List<RouteStep> getSteps()
    {
        return steps == null ? Collections.emptyList() : steps;
    }
}
