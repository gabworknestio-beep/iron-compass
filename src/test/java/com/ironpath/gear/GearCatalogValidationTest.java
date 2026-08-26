package com.ironpath.gear;

import com.google.gson.Gson;
import java.io.StringReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GearCatalogValidationTest
{
    @Test
    public void bundledCatalogHasFortyReviewedObjectives() throws Exception
    {
        GearCatalog catalog = new GearLoader(new Gson()).loadResource("/gear/ironman-gear-2026.json");
        new GearValidator().validate(catalog);
        assertEquals(40, catalog.getUpgrades().size());
        assertEquals("2026-08-26", catalog.getAuditedAt());
    }

    @Test(expected = GearValidationException.class)
    public void prerequisiteCyclesAreRejected() throws Exception
    {
        String json = "{\"version\":1,\"upgrades\":["
            + upgrade("a", "b") + "," + upgrade("b", "a") + "]}";
        GearCatalog catalog = new GearLoader(new Gson()).load(new StringReader(json), "cycle");
        new GearValidator().validate(catalog);
    }

    @Test(expected = GearValidationException.class)
    public void selfReferencesAreRejected() throws Exception
    {
        GearCatalog catalog = load("{\"version\":1,\"upgrades\":[" + valid("a", 1,
            "\"alternativeIds\":[\"a\"]") + "]}");
        new GearValidator().validate(catalog);
    }

    @Test(expected = GearValidationException.class)
    public void previousTierRegressionsAreRejected() throws Exception
    {
        GearCatalog catalog = load("{\"version\":1,\"upgrades\":["
            + valid("later", 3, "") + "," + valid("earlier", 2,
            "\"previousIds\":[\"later\"]") + "]}");
        new GearValidator().validate(catalog);
    }

    @Test(expected = GearValidationException.class)
    public void alternativesCannotPointBackIntoDependencyPath() throws Exception
    {
        GearCatalog catalog = load("{\"version\":1,\"upgrades\":["
            + valid("base", 1, "\"alternativeIds\":[\"upgrade\"]") + ","
            + valid("upgrade", 2, "\"previousIds\":[\"base\"]") + "]}");
        new GearValidator().validate(catalog);
    }

    @Test(expected = GearValidationException.class)
    public void previousCyclesAreRejected() throws Exception
    {
        GearCatalog catalog = load("{\"version\":1,\"upgrades\":["
            + valid("a", 1, "\"previousIds\":[\"b\"]") + ","
            + valid("b", 2, "\"previousIds\":[\"a\"]") + "]}");
        new GearValidator().validate(catalog);
    }

    private static GearCatalog load(String json) throws Exception
    {
        return new GearLoader(new Gson()).load(new StringReader(json), "test");
    }

    private static String valid(String id, int tier, String relation)
    {
        String suffix = relation.isEmpty() ? "" : "," + relation;
        return "{\"id\":\"" + id + "\",\"name\":\"" + id + "\",\"styles\":[\"MELEE\"],"
            + "\"tier\":" + tier + ",\"completion\":{\"type\":\"ITEM_PRESENT\",\"itemId\":1}" + suffix + "}";
    }

    private static String upgrade(String id, String prerequisite)
    {
        return "{\"id\":\"" + id + "\",\"name\":\"" + id + "\",\"styles\":[\"MELEE\"],"
            + "\"tier\":1,"
            + "\"prerequisiteIds\":[\"" + prerequisite + "\"],"
            + "\"completion\":{\"type\":\"ITEM_PRESENT\",\"itemId\":1}}";
    }
}
