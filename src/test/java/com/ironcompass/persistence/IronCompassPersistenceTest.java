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
    public void legacySelectedGoalMigratesToPrimaryOncePerProfile()
    {
        FakeProfileConfig config = new FakeProfileConfig();
        config.set(IronCompassPersistence.CONFIG_GROUP, IronCompassPersistence.SELECTED_GEAR_GOAL_KEY,
            "goal.legacy");
        IronCompassPersistence persistence = new IronCompassPersistence(config);

        assertEquals("goal.legacy", persistence.getPrimaryGoalId());
        assertTrue(persistence.getSecondaryGoalIds().isEmpty());
        assertEquals("goal.legacy", config.get(IronCompassPersistence.CONFIG_GROUP,
            IronCompassPersistence.PRIMARY_GOAL_KEY));
        assertEquals(IronCompassPersistence.GOAL_QUEUE_MIGRATION_VERSION,
            config.get(IronCompassPersistence.CONFIG_GROUP, IronCompassPersistence.GOAL_QUEUE_MIGRATION_KEY));

        config.set(IronCompassPersistence.CONFIG_GROUP, IronCompassPersistence.SELECTED_GEAR_GOAL_KEY,
            "goal.changed-legacy");
        persistence.profileChanged();
        assertEquals("goal.legacy", persistence.getPrimaryGoalId());
    }

    @Test
    public void queuePreventsDuplicatesAndCapsSecondaryGoalsAtThree()
    {
        IronCompassPersistence persistence = new IronCompassPersistence(new FakeProfileConfig());
        persistence.setPrimaryGoalId("goal.primary");

        assertFalse(persistence.addSecondaryGoalId("goal.primary"));
        assertTrue(persistence.addSecondaryGoalId("goal.one"));
        assertFalse(persistence.addSecondaryGoalId("goal.one"));
        assertTrue(persistence.addSecondaryGoalId("goal.two"));
        assertTrue(persistence.addSecondaryGoalId("goal.three"));
        assertFalse(persistence.addSecondaryGoalId("goal.four"));
        assertEquals(3, persistence.getSecondaryGoalIds().size());

        persistence.setPrimaryGoalId("goal.two");
        assertFalse(persistence.getSecondaryGoalIds().contains("goal.two"));
    }

    @Test
    public void skippedGoalIsRemovedFromEveryActiveRole()
    {
        IronCompassPersistence persistence = new IronCompassPersistence(new FakeProfileConfig());
        persistence.setPrimaryGoalId("goal.primary");
        persistence.addSecondaryGoalId("goal.secondary");

        persistence.setSkipped("goal.secondary", true);
        assertFalse(persistence.getSecondaryGoalIds().contains("goal.secondary"));
        persistence.setSkipped("goal.primary", true);
        assertNull(persistence.getPrimaryGoalId());
    }

    @Test
    public void profileSwitchReloadsOnlyTheNewCharactersPreferences()
    {
        FakeProfileConfig config = new FakeProfileConfig();
        IronCompassPersistence persistence = new IronCompassPersistence(config);

        config.use("A");
        persistence.setSelectedGoalId("gear.a");
        persistence.addSecondaryGoalId("goal.a-secondary");
        persistence.setSkipped("gear.skip-a", true);
        persistence.put("step.a", ManualOverride.FORCE_COMPLETE);
        persistence.setPlaystyle(Playstyle.PVM);
        persistence.setAvoidWilderness(true);
        persistence.setSessionLength(SessionLength.THIRTY_MINUTES);

        config.use("B");
        persistence.profileChanged();
        assertNull(persistence.getSelectedGoalId());
        assertTrue(persistence.getSecondaryGoalIds().isEmpty());
        assertFalse(persistence.isSkipped("gear.skip-a"));
        assertNull(persistence.get("step.a"));
        assertEquals(Playstyle.BALANCED, persistence.getPlaystyle());
        assertFalse(persistence.isAvoidWilderness());
        assertEquals(SessionLength.ANY, persistence.getSessionLength());
        persistence.setSelectedGoalId("gear.b");

        config.use("A");
        persistence.profileChanged();
        assertEquals("gear.a", persistence.getSelectedGoalId());
        assertEquals(java.util.Collections.singletonList("goal.a-secondary"), persistence.getSecondaryGoalIds());
        assertTrue(persistence.isSkipped("gear.skip-a"));
        assertEquals(ManualOverride.FORCE_COMPLETE, persistence.get("step.a"));
        assertEquals(Playstyle.PVM, persistence.getPlaystyle());
        assertTrue(persistence.isAvoidWilderness());
        assertEquals(SessionLength.THIRTY_MINUTES, persistence.getSessionLength());
    }

    @Test
    public void goalOverridesWithNamespacedKeysPersistPerProfile()
    {
        FakeProfileConfig config = new FakeProfileConfig();
        IronCompassPersistence persistence = new IronCompassPersistence(config);
        String key = "goal:goal.transport.fairy-rings";

        config.use("A");
        persistence.put(key,ManualOverride.FORCE_COMPLETE);
        persistence.profileChanged();
        assertEquals(ManualOverride.FORCE_COMPLETE,persistence.get(key));

        config.use("B");
        persistence.profileChanged();
        assertNull(persistence.get(key));
        persistence.put(key,ManualOverride.FORCE_INCOMPLETE);

        config.use("A");
        persistence.profileChanged();
        assertEquals(ManualOverride.FORCE_COMPLETE,persistence.get(key));
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
