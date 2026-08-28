package com.ironcompass;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(IronCompassConfig.GROUP)
public interface IronCompassConfig extends Config
{
    String GROUP = "ironcompass";

    @ConfigItem(
        keyName = "preferSafeAlternatives",
        name = "Prefer HCIM-safe alternatives",
        description = "Use an explicitly authored safe alternative when the route provides one"
    )
    default boolean preferSafeAlternatives()
    {
        return true;
    }

    @ConfigItem(
        keyName = "wikiActions",
        name = "Wiki actions",
        description = "Show contextual links to the OSRS Wiki"
    )
    default boolean wikiActions()
    {
        return true;
    }

    @ConfigItem(
        keyName = "shortestPath",
        name = "Shortest Path actions",
        description = "Allow route buttons to send supported locations to the Shortest Path plugin"
    )
    default boolean shortestPath()
    {
        return true;
    }

    @ConfigItem(
        keyName = "completionNotifications",
        name = "Completion notifications",
        description = "Notify when Iron Compass advances after a meaningful completed step"
    )
    default boolean completionNotifications()
    {
        return true;
    }

    @Range(min = 3, max = 10)
    @ConfigItem(
        keyName = "preparationLookahead",
        name = "Preparation lookahead",
        description = "How many pending route steps are included in Prep Soon"
    )
    default int preparationLookahead()
    {
        return 7;
    }
}
