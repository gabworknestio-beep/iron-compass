package com.ironpath.integration;

import com.ironpath.route.LocationSpec;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

public final class ShortestPathBridge
{
    private static final String NAMESPACE = "shortestpath";
    private static final String PATH = "path";
    private static final String CLEAR = "clear";
    private final EventBus eventBus;
    private final PluginManager pluginManager;

    @Inject
    public ShortestPathBridge(EventBus eventBus, PluginManager pluginManager)
    {
        this.eventBus = eventBus;
        this.pluginManager = pluginManager;
    }

    public void pathTo(LocationSpec location)
    {
        if (location == null)
        {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("target", new WorldPoint(location.getX(), location.getY(), location.getPlane()));
        eventBus.post(new PluginMessage(NAMESPACE, PATH, data));
    }

    public void clear()
    {
        eventBus.post(new PluginMessage(NAMESPACE, CLEAR, Map.of()));
    }

    public IntegrationStatus status()
    {
        return IntegrationStatus.WORKING;
    }

    public boolean isAvailable()
    {
        for (Plugin plugin : pluginManager.getPlugins())
        {
            if ("Shortest Path".equals(plugin.getName()) && pluginManager.isPluginActive(plugin))
            {
                return true;
            }
        }
        return false;
    }
}
