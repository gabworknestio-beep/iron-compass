package com.ironpath.route;

import com.google.gson.Gson;
import java.io.StringReader;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RouteValidationTest
{
    private final Gson gson = new Gson();

    @Test
    public void malformedJsonFailsWithUsefulMessage()
    {
        try
        {
            new RouteLoader(gson).load(new StringReader("{ nope"), "broken.json");
            fail("Expected malformed route to fail");
        }
        catch (RouteLoadException ex)
        {
            assertTrue(ex.getMessage().contains("Malformed route JSON"));
            assertTrue(ex.getMessage().contains("broken.json"));
        }
    }

    @Test
    public void duplicateStepIdsFailValidation() throws Exception
    {
        assertInvalid(routeWithSteps(manual("same", "One") + "," + manual("same", "Two")), "duplicate");
    }

    @Test
    public void missingPrerequisiteFailsValidation() throws Exception
    {
        String step = "{\"id\":\"a\",\"type\":\"MANUAL\",\"title\":\"A\",\"requires\":[\"missing\"],\"completion\":{\"type\":\"MANUAL_ONLY\"}}";
        assertInvalid(routeWithSteps(step), "missing prerequisite");
    }

    @Test
    public void cyclicPrerequisitesFailValidation() throws Exception
    {
        String a = "{\"id\":\"a\",\"type\":\"MANUAL\",\"title\":\"A\",\"requires\":[\"b\"],\"completion\":{\"type\":\"MANUAL_ONLY\"}}";
        String b = "{\"id\":\"b\",\"type\":\"MANUAL\",\"title\":\"B\",\"requires\":[\"a\"],\"completion\":{\"type\":\"MANUAL_ONLY\"}}";
        assertInvalid(routeWithSteps(a + "," + b), "cyclic");
    }

    @Test
    public void manualStepMustDeclareManualOnlyCompletion() throws Exception
    {
        String step = "{\"id\":\"a\",\"type\":\"MANUAL\",\"title\":\"A\",\"completion\":{\"type\":\"SKILL_AT_LEAST\",\"skill\":\"Magic\",\"level\":1}}";
        assertInvalid(routeWithSteps(step), "explicitly use manual_only");
    }

    @Test
    public void manualCompletionMustSayWhatThePlayerConfirms() throws Exception
    {
        String step = "{\"id\":\"a\",\"type\":\"MANUAL\",\"title\":\"A\",\"completion\":{\"type\":\"MANUAL_ONLY\",\"label\":\"Manual confirmation\"}}";
        assertInvalid(routeWithSteps(step), "specific confirmation label");
    }

    @Test
    public void playerInstructionCannotUseGeneratorPlaceholder() throws Exception
    {
        String step = "{\"id\":\"a\",\"type\":\"MANUAL\",\"title\":\"A\","
            + "\"instruction\":\"Finish this route milestone: A.\",\"reason\":\"Test.\","
            + "\"completion\":{\"type\":\"MANUAL_ONLY\",\"label\":\"A is complete\"}}";
        assertInvalid(routeWithSteps(step), "generic instruction");
    }

    private void assertInvalid(String json, String expected) throws Exception
    {
        Route route = new RouteLoader(gson).load(new StringReader(json), "test");
        try
        {
            new RouteValidator().validate(route);
            fail("Expected invalid route");
        }
        catch (RouteValidationException ex)
        {
            assertTrue(ex.getMessage().toLowerCase().contains(expected));
        }
    }

    private String routeWithSteps(String steps)
    {
        return "{\"routeId\":\"test\",\"version\":1,\"name\":\"Test\",\"sections\":[{\"id\":\"s\",\"name\":\"S\",\"steps\":[" + steps + "]}]}";
    }

    private String manual(String id, String title)
    {
        return "{\"id\":\"" + id + "\",\"type\":\"MANUAL\",\"title\":\"" + title + "\",\"completion\":{\"type\":\"MANUAL_ONLY\"}}";
    }
}
