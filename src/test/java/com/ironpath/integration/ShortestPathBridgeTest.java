package com.ironpath.integration;

import com.google.gson.Gson;
import com.ironpath.route.LocationSpec;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShortestPathBridgeTest
{
    @Test
    public void postsDocumentedPathContract()
    {
        EventBus eventBus = new EventBus();
        AtomicReference<PluginMessage> received = new AtomicReference<>();
        eventBus.register(PluginMessage.class, received::set, 0);
        LocationSpec location = new Gson().fromJson(
            new StringReader("{\"x\":3081,\"y\":3421,\"plane\":0}"), LocationSpec.class);

        new ShortestPathBridge(eventBus, null).pathTo(location);

        assertEquals("shortestpath", received.get().getNamespace());
        assertEquals("path", received.get().getName());
        assertEquals(new WorldPoint(3081, 3421, 0), received.get().getData().get("target"));
    }
}
