package com.ironcompass.training;

import java.util.Collections;
import java.util.List;

public final class IronmanMethodCatalog
{
    private int version;
    private String auditedAt;
    private List<IronmanMethodDefinition> methods;

    public int getVersion() { return version; }
    public String getAuditedAt() { return auditedAt; }
    public List<IronmanMethodDefinition> getMethods()
    {
        return methods == null ? Collections.emptyList() : methods;
    }
}
