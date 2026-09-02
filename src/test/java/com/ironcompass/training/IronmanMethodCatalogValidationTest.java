package com.ironcompass.training;

import com.google.gson.Gson;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class IronmanMethodCatalogValidationTest
{
    private final IronmanMethodLoader loader = new IronmanMethodLoader(new Gson());

    @Test
    public void bundledPilotCatalogHasValidUniqueMethodsMilestonesAndReferences() throws Exception
    {
        IronmanMethodCatalog catalog = loader.loadResource("/methods/ironman-methods-2026.json");
        assertEquals(24, catalog.getFullGuideSkills().size());
        assertTrue(catalog.getMethods().size() >= 110);
        assertTrue(catalog.getMilestones().size() >= 130);
        Set<String> ids = new HashSet<>();
        for (IronmanMethodDefinition method : catalog.getMethods())
        {
            assertTrue(ids.add(method.getId()));
            assertTrue(method.getMinLevel() <= method.getMaxLevel());
            assertTrue(method.getSourceReferences().stream().allMatch(value -> value.startsWith("https://")));
        }
        for (TrainingMilestone milestone : catalog.getMilestones())
        {
            assertTrue(ids.add(milestone.getId()));
            assertTrue(milestone.getSourceReferences().stream().allMatch(value -> value.startsWith("https://")));
        }
    }

    @Test
    public void rejectsDuplicateIdsInvalidSkillsAndInvalidRanges()
    {
        assertRejected("{\"version\":2,\"methods\":["
            + method("same", "Hunter", 1, 99) + "," + method("same", "Hunter", 1, 99) + "]}");
        assertRejected("{\"version\":2,\"methods\":[" + method("bad", "Sailingboat", 1, 99) + "]}");
        assertRejected("{\"version\":2,\"methods\":[" + method("bad", "Hunter", 80, 70) + "]}");
    }

    @Test
    public void rejectsClaimedFullGuideWithGap()
    {
        assertRejected("{\"version\":2,\"fullGuideSkills\":[\"Hunter\"],\"methods\":["
            + method("early", "Hunter", 1, 40) + "," + method("late", "Hunter", 42, 99) + "]}");
    }

    @Test
    public void rejectsMissingOrMalformedReferences()
    {
        String missing = method("missing", "Hunter", 1, 99)
            .replace(",\"sourceReferences\":[\"https://example.com\"]", "");
        assertRejected("{\"version\":2,\"methods\":[" + missing + "]}");
        assertRejected("{\"version\":2,\"methods\":["
            + method("bad-ref", "Hunter", 1, 99).replace("https://example.com", "http://example.com") + "]}");
    }

    private void assertRejected(String json)
    {
        try
        {
            loader.load(new StringReader(json), "test");
            fail("Expected catalog rejection");
        }
        catch (MethodLoadException expected)
        {
            assertTrue(expected.getMessage().contains("test"));
        }
    }

    private static String method(String id, String skill, int min, int max)
    {
        return "{\"id\":\"" + id + "\",\"skill\":\"" + skill + "\",\"minLevel\":" + min
            + ",\"maxLevel\":" + max + ",\"title\":\"Method\",\"description\":\"Description\""
            + ",\"wikiPage\":\"Hunter\",\"sourceReferences\":[\"https://example.com\"]}";
    }
}
