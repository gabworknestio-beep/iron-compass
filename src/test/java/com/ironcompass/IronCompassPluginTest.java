package com.ironcompass;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class IronCompassPluginTest
{
    private IronCompassPluginTest()
    {
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(IronCompassPlugin.class);
        RuneLite.main(args);
    }
}
