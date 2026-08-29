package com.ironcompass.goal;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class GoalRelationshipIndexTest
{
    private GoalRelationshipIndex index;

    @Before
    public void setUp() throws Exception
    {
        index = new GoalRelationshipIndex(new GoalLoader(new Gson())
            .loadResource("/goals/ironman-goals-2026.json"));
    }

    @Test
    public void symmetricRelationshipsAreDiscoverableFromEitherSide()
    {
        assertTrue(index.hasBetween("goal.skill.hunter-75","goal.resource.prayer-sustain",
            GoalRelationshipType.ALTERNATIVE));
        assertTrue(index.hasBetween("goal.resource.prayer-sustain","goal.skill.hunter-75",
            GoalRelationshipType.ALTERNATIVE));
        assertTrue(index.hasBetween("goal.pvm.inferno-prep","goal.account.strong-poh",
            GoalRelationshipType.SYNERGY));
    }

    @Test
    public void requiresAndRecommendedBeforeKeepTheirDirection()
    {
        assertTrue(index.hasBetween("goal.transport.fairy-rings","goal.quest.fairytale-i",
            GoalRelationshipType.REQUIRES));
        assertFalse(index.hasBetween("goal.quest.fairytale-i","goal.transport.fairy-rings",
            GoalRelationshipType.REQUIRES));
        assertTrue(index.supports("goal.unlock.piety","gear.early.fire-cape"));
        assertFalse(index.supports("gear.early.fire-cape","goal.unlock.piety"));
    }
}
