package com.ironcompass.integration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WikiBridgeTest
{
    @Test
    public void preservesWikiFragmentAsFragment()
    {
        assertEquals(
            "https://oldschool.runescape.wiki/w/Recipe_for_Disaster#Defeating_the_Culinaromancer",
            WikiBridge.urlFor("Recipe for Disaster#Defeating the Culinaromancer"));
    }
}
