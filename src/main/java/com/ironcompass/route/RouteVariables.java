package com.ironcompass.route;

import com.ironcompass.requirement.ConditionSpec;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class RouteVariables
{
    private final Set<Integer> varbits = new HashSet<>();
    private final Set<Integer> varps = new HashSet<>();

    public RouteVariables(Route... routes)
    {
        for (Route route : routes)
        {
            if (route == null)
            {
                continue;
            }
            for (RouteSection section : route.getSections())
            {
                for (RouteStep step : section.getSteps())
                {
                    collect(step.getCompletion());
                    collect(step.getReadiness());
                }
            }
        }
    }

    public Set<Integer> getVarbits()
    {
        return Collections.unmodifiableSet(varbits);
    }

    public Set<Integer> getVarps()
    {
        return Collections.unmodifiableSet(varps);
    }

    private void collect(ConditionSpec condition)
    {
        if (condition == null)
        {
            return;
        }
        String type = condition.getType() == null ? "" : condition.getType();
        if (type.startsWith("VARBIT_"))
        {
            varbits.add(condition.getId());
        }
        else if (type.startsWith("VARP_"))
        {
            varps.add(condition.getId());
        }
        for (ConditionSpec child : condition.getChildren())
        {
            collect(child);
        }
        collect(condition.getChild());
    }
}
