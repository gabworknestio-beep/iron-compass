package com.ironpath;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class IronPathPluginTest
{
    private IronPathPluginTest()
    {
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(IronPathPlugin.class);
        RuneLite.main(args);
    }
}
