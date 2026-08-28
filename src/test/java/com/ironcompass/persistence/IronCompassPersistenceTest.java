package com.ironcompass.persistence;

import com.ironcompass.planner.Playstyle;
import com.ironcompass.planner.SessionLength;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class IronCompassPersistenceTest
{
    @Test
    public void profileSwitchReloadsOnlyTheNewCharactersPreferences()
    {
        FakeProfileConfig config = new FakeProfileConfig();
        IronCompassPersistence persistence = new IronCompassPersistence(config);

        config.use("A");
        persistence.setSelectedGoalId("gear.a");
        persistence.setSkipped("gear.skip-a", true);
        persistence.put("step.a", ManualOverride.FORCE_COMPLETE);
        persistence.setPlaystyle(Playstyle.PVM);
        persistence.setAvoidWilderness(true);
        persistence.setSessionLength(SessionLength.THIRTY_MINUTES);

        config.use("B");
        persistence.profileChanged();
        assertNull(persistence.getSelectedGoalId());
        assertFalse(persistence.isSkipped("gear.skip-a"));
        assertNull(persistence.get("step.a"));
        assertEquals(Playstyle.BALANCED, persistence.getPlaystyle());
        assertFalse(persistence.isAvoidWilderness());
        assertEquals(SessionLength.ANY, persistence.getSessionLength());
        persistence.setSelectedGoalId("gear.b");

        config.use("A");
        persistence.profileChanged();
        assertEquals("gear.a", persistence.getSelectedGoalId());
        assertTrue(persistence.isSkipped("gear.skip-a"));
        assertEquals(ManualOverride.FORCE_COMPLETE, persistence.get("step.a"));
        assertEquals(Playstyle.PVM, persistence.getPlaystyle());
        assertTrue(persistence.isAvoidWilderness());
        assertEquals(SessionLength.THIRTY_MINUTES, persistence.getSessionLength());
    }

    @Test
    public void skipClearsSelectedGoalAndUnskipDoesNotReselectIt()
    {
        FakeProfileConfig config = new FakeProfileConfig();
        IronCompassPersistence persistence = new IronCompassPersistence(config);
        persistence.setSelectedGoalId("gear.goal");

        persistence.setSkipped("gear.goal", true);
        assertNull(persistence.getSelectedGoalId());
        persistence.setSkipped("gear.goal", false);
        assertNull(persistence.getSelectedGoalId());
    }

    @Test
    public void corruptEntriesAreIgnoredWithoutDiscardingValidEntries()
    {
        FakeProfileConfig config = new FakeProfileConfig();
        config.set(IronCompassPersistence.CONFIG_GROUP, IronCompassPersistence.OVERRIDES_KEY,
            "broken;step.good:FORCE_COMPLETE;step.bad:NOT_A_VALUE");
        config.set(IronCompassPersistence.CONFIG_GROUP, IronCompassPersistence.SKIPPED_GEAR_KEY,
            ";gear.one;;gear.two;");
        IronCompassPersistence persistence = new IronCompassPersistence(config);

        assertEquals(ManualOverride.FORCE_COMPLETE, persistence.get("step.good"));
        assertNull(persistence.get("step.bad"));
        assertTrue(persistence.isSkipped("gear.one"));
        assertTrue(persistence.isSkipped("gear.two"));
    }

    @Test
    public void unknownFuturePlannerValuesFallBackSafely()
    {
        FakeProfileConfig config = new FakeProfileConfig();
        config.set(IronCompassPersistence.CONFIG_GROUP, IronCompassPersistence.PLAYSTYLE_KEY, "FUTURE_STYLE");
        config.set(IronCompassPersistence.CONFIG_GROUP, IronCompassPersistence.SESSION_LENGTH_KEY, "EXACTLY_42_MINUTES");
        config.set(IronCompassPersistence.CONFIG_GROUP, IronCompassPersistence.AVOID_WILDERNESS_KEY, "corrupt");
        IronCompassPersistence persistence = new IronCompassPersistence(config);

        assertEquals(Playstyle.BALANCED, persistence.getPlaystyle());
        assertEquals(SessionLength.ANY, persistence.getSessionLength());
        assertFalse(persistence.isAvoidWilderness());
    }

    private static final class FakeProfileConfig implements ProfileConfigAccess
    {
        private final Map<String, Map<String, Object>> profiles = new HashMap<>();
        private String active = "default";

        private void use(String profile)
        {
            active = profile;
        }

        @Override
        public String get(String group, String key)
        {
            Object value = values().get(group + "." + key);
            return value == null ? null : String.valueOf(value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String group, String key, Type type)
        {
            return (T) values().get(group + "." + key);
        }

        @Override
        public void set(String group, String key, Object value)
        {
            values().put(group + "." + key, value);
        }

        private Map<String, Object> values()
        {
            return profiles.computeIfAbsent(active, ignored -> new HashMap<>());
        }
    }
}
