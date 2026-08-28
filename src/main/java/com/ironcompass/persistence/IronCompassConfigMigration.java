package com.ironcompass.persistence;

import com.ironcompass.IronCompassConfig;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

/**
 * Copies only Iron Compass's explicitly known settings out of the former shared namespace.
 * Old values are intentionally retained because another plugin also uses the old group.
 */
public final class IronCompassConfigMigration
{
    static final String OLD_CONFIG_GROUP = "ironpath";
    static final String OLD_PROFILE_GROUP = "ironpath-progress";
    static final String MIGRATION_MARKER_KEY = "configMigrationVersion";
    static final String MIGRATION_VERSION = "1";

    private static final List<String> CONFIG_KEYS = Arrays.asList(
        "preferSafeAlternatives",
        "wikiActions",
        "shortestPath",
        "completionNotifications",
        "preparationLookahead"
    );

    private static final List<String> PROFILE_KEYS = Arrays.asList(
        IronCompassPersistence.OVERRIDES_KEY,
        IronCompassPersistence.ROUTE_VERSION_KEY,
        IronCompassPersistence.SELECTED_GEAR_GOAL_KEY,
        IronCompassPersistence.SKIPPED_GEAR_KEY,
        IronCompassPersistence.OPTIONAL_GEAR_KEY,
        IronCompassPersistence.ALTERNATIVE_GEAR_KEY,
        IronCompassPersistence.GEAR_STYLE_FILTER_KEY,
        IronCompassPersistence.GEAR_STATUS_FILTER_KEY,
        IronCompassPersistence.PLAYSTYLE_KEY,
        IronCompassPersistence.AVOID_WILDERNESS_KEY,
        IronCompassPersistence.SESSION_LENGTH_KEY
    );

    private final ConfigMigrationAccess config;

    @Inject
    public IronCompassConfigMigration(ConfigManager configManager)
    {
        this(new ConfigMigrationAccess()
        {
            @Override
            public String get(String group, String key)
            {
                return configManager.getConfiguration(group, key);
            }

            @Override
            public void set(String group, String key, Object value)
            {
                configManager.setConfiguration(group, key, value);
            }

            @Override
            public String getProfile(String group, String key)
            {
                return configManager.getRSProfileConfiguration(group, key);
            }

            @Override
            public void setProfile(String group, String key, Object value)
            {
                configManager.setRSProfileConfiguration(group, key, value);
            }
        });
    }

    IronCompassConfigMigration(ConfigMigrationAccess config)
    {
        this.config = config;
    }

    public void migrate()
    {
        migrateGlobalConfiguration();
        migrateCurrentProfile();
    }

    public void migrateGlobalConfiguration()
    {
        if (MIGRATION_VERSION.equals(config.get(IronCompassConfig.GROUP, MIGRATION_MARKER_KEY)))
        {
            return;
        }
        for (String key : CONFIG_KEYS)
        {
            copyIfMissing(OLD_CONFIG_GROUP, IronCompassConfig.GROUP, key);
        }
        config.set(IronCompassConfig.GROUP, MIGRATION_MARKER_KEY, MIGRATION_VERSION);
    }

    public void migrateCurrentProfile()
    {
        if (MIGRATION_VERSION.equals(
            config.getProfile(IronCompassPersistence.CONFIG_GROUP, MIGRATION_MARKER_KEY)))
        {
            return;
        }
        for (String key : PROFILE_KEYS)
        {
            copyProfileIfMissing(OLD_PROFILE_GROUP, IronCompassPersistence.CONFIG_GROUP, key);
        }
        config.setProfile(IronCompassPersistence.CONFIG_GROUP, MIGRATION_MARKER_KEY, MIGRATION_VERSION);
    }

    private void copyIfMissing(String oldGroup, String newGroup, String key)
    {
        if (config.get(newGroup, key) != null)
        {
            return;
        }
        String oldValue = config.get(oldGroup, key);
        if (oldValue != null)
        {
            config.set(newGroup, key, oldValue);
        }
    }

    private void copyProfileIfMissing(String oldGroup, String newGroup, String key)
    {
        if (config.getProfile(newGroup, key) != null)
        {
            return;
        }
        String oldValue = config.getProfile(oldGroup, key);
        if (oldValue != null)
        {
            config.setProfile(newGroup, key, oldValue);
        }
    }
}
