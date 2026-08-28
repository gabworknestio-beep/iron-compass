package com.ironcompass.integration;

import javax.inject.Inject;

public final class QuestHelperBridge
{
    @Inject
    public QuestHelperBridge()
    {
    }

    public boolean canLaunch()
    {
        return false;
    }

    public IntegrationStatus status()
    {
        return IntegrationStatus.PARTIAL;
    }

    public String explanation()
    {
        return "Iron Compass can track verified Quest Helper quest-state boundaries, but Quest Helper has no merged public launch message contract yet. Open the named quest from its sidebar.";
    }
}
