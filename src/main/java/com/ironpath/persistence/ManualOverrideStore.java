package com.ironpath.persistence;

import java.util.Map;

public interface ManualOverrideStore
{
    ManualOverride get(String stepId);

    void put(String stepId, ManualOverride override);

    void remove(String stepId);

    void clear();

    Map<String, ManualOverride> snapshot();
}
