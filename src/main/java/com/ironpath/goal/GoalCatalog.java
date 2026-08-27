package com.ironpath.goal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GoalCatalog
{
    private int version;
    private String auditedAt;
    private List<GoalDefinition> goals;
    private transient Map<String, GoalDefinition> byId;

    public int getVersion() { return version; }
    public String getAuditedAt() { return auditedAt; }
    public List<GoalDefinition> getGoals() { return goals == null ? Collections.emptyList() : goals; }

    public GoalDefinition find(String id)
    {
        if (byId == null)
        {
            byId = new LinkedHashMap<>();
            for (GoalDefinition goal : getGoals())
            {
                byId.put(goal.getId(), goal);
            }
        }
        return byId.get(id);
    }
}
