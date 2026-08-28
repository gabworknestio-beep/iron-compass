package com.ironcompass.persistence;

import com.ironcompass.IronCompassConfig;
import com.ironcompass.planner.Playstyle;
import com.ironcompass.planner.SessionLength;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class IronCompassConfigMigrationTest
{
    @Test
    public void copiesOnlyKnownKeysAndNeverDeletesOldValues()
    {
        FakeConfig config = new FakeConfig();
        Map<String, String> settings = new HashMap<>();
        settings.put("preferSafeAlternatives", "false");
        settings.put("wikiActions", "false");
        settings.put("shortestPath", "true");
        settings.put("completionNotifications", "false");
        settings.put("preparationLookahead", "9");
        for (Map.Entry<String, String> setting : settings.entrySet())
        {
            config.set(IronCompassConfigMigration.OLD_CONFIG_GROUP, setting.getKey(), setting.getValue());
        }
        config.set(IronCompassConfigMigration.OLD_CONFIG_GROUP, "foreignPluginSetting", "keep-me");

        Map<String, String> profileSettings = new HashMap<>();
        profileSettings.put(IronCompassPersistence.OVERRIDES_KEY, "step.done:FORCE_COMPLETE");
        profileSettings.put(IronCompassPersistence.ROUTE_VERSION_KEY, "3");
        profileSettings.put(IronCompassPersistence.SELECTED_GEAR_GOAL_KEY, "gear.target");
        profileSettings.put(IronCompassPersistence.SKIPPED_GEAR_KEY, "gear.skipped");
        profileSettings.put(IronCompassPersistence.OPTIONAL_GEAR_KEY, "gear.optional");
        profileSettings.put(IronCompassPersistence.ALTERNATIVE_GEAR_KEY, "gear.target:gear.alternative");
        profileSettings.put(IronCompassPersistence.GEAR_STYLE_FILTER_KEY, "RANGED");
        profileSettings.put(IronCompassPersistence.GEAR_STATUS_FILTER_KEY, "AVAILABLE");
        profileSettings.put(IronCompassPersistence.PLAYSTYLE_KEY, "PVM");
        profileSettings.put(IronCompassPersistence.AVOID_WILDERNESS_KEY, "true");
        profileSettings.put(IronCompassPersistence.SESSION_LENGTH_KEY, "THIRTY_MINUTES");
        for (Map.Entry<String, String> setting : profileSettings.entrySet())
        {
            config.setProfile(IronCompassConfigMigration.OLD_PROFILE_GROUP,
                setting.getKey(), setting.getValue());
        }
        config.setProfile(IronCompassConfigMigration.OLD_PROFILE_GROUP, "foreignProgress", "keep-me-too");

        new IronCompassConfigMigration(config).migrate();

        for (Map.Entry<String, String> setting : settings.entrySet())
        {
            assertEquals(setting.getValue(), config.get(IronCompassConfig.GROUP, setting.getKey()));
            assertEquals(setting.getValue(),
                config.get(IronCompassConfigMigration.OLD_CONFIG_GROUP, setting.getKey()));
        }
        for (Map.Entry<String, String> setting : profileSettings.entrySet())
        {
            assertEquals(setting.getValue(),
                config.getProfile(IronCompassPersistence.CONFIG_GROUP, setting.getKey()));
            assertEquals(setting.getValue(),
                config.getProfile(IronCompassConfigMigration.OLD_PROFILE_GROUP, setting.getKey()));
        }
        assertNull(config.get(IronCompassConfig.GROUP, "foreignPluginSetting"));
        assertEquals("keep-me", config.get(IronCompassConfigMigration.OLD_CONFIG_GROUP,
            "foreignPluginSetting"));
        assertNull(config.getProfile(IronCompassPersistence.CONFIG_GROUP, "foreignProgress"));
        assertEquals("keep-me-too", config.getProfile(IronCompassConfigMigration.OLD_PROFILE_GROUP,
            "foreignProgress"));

        IronCompassPersistence persistence = new IronCompassPersistence(config.profileAccess());
        assertEquals("gear.target", persistence.getSelectedGoalId());
        assertTrue(persistence.isSkipped("gear.skipped"));
        assertEquals(ManualOverride.FORCE_COMPLETE, persistence.get("step.done"));
        assertEquals(Playstyle.PVM, persistence.getPlaystyle());
        assertTrue(persistence.isAvoidWilderness());
        assertEquals(SessionLength.THIRTY_MINUTES, persistence.getSessionLength());
    }

    @Test
    public void existingIronCompassValuesAlwaysWin()
    {
        FakeConfig config = new FakeConfig();
        config.set(IronCompassConfigMigration.OLD_CONFIG_GROUP, "preparationLookahead", "10");
        config.set(IronCompassConfig.GROUP, "preparationLookahead", "4");
        config.setProfile(IronCompassConfigMigration.OLD_PROFILE_GROUP,
            IronCompassPersistence.AVOID_WILDERNESS_KEY, "true");
        config.setProfile(IronCompassPersistence.CONFIG_GROUP,
            IronCompassPersistence.AVOID_WILDERNESS_KEY, "false");

        new IronCompassConfigMigration(config).migrate();

        assertEquals("4", config.get(IronCompassConfig.GROUP, "preparationLookahead"));
        assertEquals("false", config.getProfile(IronCompassPersistence.CONFIG_GROUP,
            IronCompassPersistence.AVOID_WILDERNESS_KEY));
        assertEquals(IronCompassConfigMigration.MIGRATION_VERSION,
            config.get(IronCompassConfig.GROUP, IronCompassConfigMigration.MIGRATION_MARKER_KEY));
        assertEquals(IronCompassConfigMigration.MIGRATION_VERSION,
            config.getProfile(IronCompassPersistence.CONFIG_GROUP,
                IronCompassConfigMigration.MIGRATION_MARKER_KEY));
    }

    @Test
    public void migrationMarkerMakesEachNamespaceOneTime()
    {
        FakeConfig config = new FakeConfig();
        IronCompassConfigMigration migration = new IronCompassConfigMigration(config);
        migration.migrate();

        config.set(IronCompassConfigMigration.OLD_CONFIG_GROUP, "shortestPath", "false");
        config.setProfile(IronCompassConfigMigration.OLD_PROFILE_GROUP,
            IronCompassPersistence.OPTIONAL_GEAR_KEY, "gear.late");
        migration.migrate();

        assertNull(config.get(IronCompassConfig.GROUP, "shortestPath"));
        assertNull(config.getProfile(IronCompassPersistence.CONFIG_GROUP,
            IronCompassPersistence.OPTIONAL_GEAR_KEY));
    }

    private static final class FakeConfig implements ConfigMigrationAccess
    {
        private final Map<String, Object> global = new HashMap<>();
        private final Map<String, Object> profile = new HashMap<>();

        @Override
        public String get(String group, String key)
        {
            return string(global.get(fullKey(group, key)));
        }

        @Override
        public void set(String group, String key, Object value)
        {
            global.put(fullKey(group, key), value);
        }

        @Override
        public String getProfile(String group, String key)
        {
            return string(profile.get(fullKey(group, key)));
        }

        @Override
        public void setProfile(String group, String key, Object value)
        {
            profile.put(fullKey(group, key), value);
        }

        private ProfileConfigAccess profileAccess()
        {
            return new ProfileConfigAccess()
            {
                @Override
                public String get(String group, String key)
                {
                    return getProfile(group, key);
                }

                @SuppressWarnings("unchecked")
                @Override
                public <T> T get(String group, String key, Type type)
                {
                    Object value = profile.get(fullKey(group, key));
                    if (value == null)
                    {
                        return null;
                    }
                    if (type == Integer.class && value instanceof String)
                    {
                        return (T) Integer.valueOf((String) value);
                    }
                    return (T) value;
                }

                @Override
                public void set(String group, String key, Object value)
                {
                    setProfile(group, key, value);
                }
            };
        }

        private static String fullKey(String group, String key)
        {
            return group + "." + key;
        }

        private static String string(Object value)
        {
            return value == null ? null : String.valueOf(value);
        }
    }
}
