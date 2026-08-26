package com.ironpath.persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InMemoryManualOverrideStore implements ManualOverrideStore
{
    private final Map<String, ManualOverride> overrides = new LinkedHashMap<>();

    @Override
    public ManualOverride get(String stepId)
    {
        return overrides.get(stepId);
    }

    @Override
    public void put(String stepId, ManualOverride override)
    {
        if (override == null)
        {
            remove(stepId);
        }
        else
        {
            overrides.put(stepId, override);
        }
    }

    @Override
    public void remove(String stepId)
    {
        overrides.remove(stepId);
    }

    @Override
    public void clear()
    {
        overrides.clear();
    }

    @Override
    public Map<String, ManualOverride> snapshot()
    {
        return Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
    }
}
