package com.ironpath.gear;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GearCatalog
{
    private int version;
    private String auditedAt;
    private List<GearUpgrade> upgrades;
    private transient Map<String, GearUpgrade> byId;

    public int getVersion() { return version; }
    public String getAuditedAt() { return auditedAt; }
    public List<GearUpgrade> getUpgrades() { return upgrades == null ? Collections.emptyList() : upgrades; }

    public GearUpgrade find(String id)
    {
        if (byId == null)
        {
            byId = new LinkedHashMap<>();
            for (GearUpgrade upgrade : getUpgrades())
            {
                byId.put(upgrade.getId(), upgrade);
            }
        }
        return byId.get(id);
    }
}
