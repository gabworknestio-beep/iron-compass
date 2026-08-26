package com.ironpath.integration;

import javax.inject.Inject;

public final class WikiSyncBridge
{
    @Inject
    public WikiSyncBridge()
    {
    }

    public IntegrationStatus status()
    {
        return IntegrationStatus.PARTIAL;
    }

    public String explanation()
    {
        return "IronPath uses local RuneLite state. WikiSync remains an optional future boundary because it currently adds no stable capability needed by the route engine.";
    }
}
