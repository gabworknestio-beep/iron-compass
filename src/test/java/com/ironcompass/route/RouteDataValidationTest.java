package com.ironcompass.route;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.api.Quest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RouteDataValidationTest
{
    @Test
    public void bundledRouteIsCompleteAndValid() throws Exception
    {
        Route route = new RouteLoader(new Gson()).loadResource("/routes/efficient-ironman.json");
        Set<String> questNames = Arrays.stream(Quest.values()).map(Quest::getName).collect(Collectors.toSet());
        new RouteValidator(questNames).validate(route);

        long steps = route.getSections().stream().flatMap(section -> section.getSteps().stream()).count();
        long training = route.getSections().stream().flatMap(section -> section.getSteps().stream())
            .filter(step -> step.getType() == StepType.TRAIN).count();
        assertEquals(341, steps);
        assertEquals(111, training);
        assertEquals(12, route.getChapters().size());
        assertTrue(hasTitle(route, "Learning the Ropes"));
        assertTrue(hasTitle(route, "Nature Spirit"));
        assertTrue(hasTitle(route, "Death Plateau"));
        assertTrue(hasTitle(route, "The Blood Moon Rises"));
        assertTrue(hasTitle(route, "The Corsair Curse"));

        RouteStep pirateMilestone = findStep(route, "Partial completion of Pirate's Treasure");
        assertEquals("VARP_AT_LEAST", pirateMilestone.getCompletion().getType());
        assertEquals(71, pirateMilestone.getCompletion().getId());
        assertEquals(2, pirateMilestone.getCompletion().getValue());
        assertEquals("Pirate's Treasure", pirateMilestone.getQuestHelperKey());
        assertTrue(new RouteVariables(route).getVarps().contains(71));
        assertTrue(hasTitle(route, "Demon Slayer — collect all three keys"));
        assertTrue(hasTitle(route, "Enter the Abyss — charge the scrying orb"));
        assertTrue(hasTitle(route, "Dwarf Cannon — repair the railings"));
        assertTrue(hasTitle(route, "Fairytale II — unlock fairy rings"));
    }

    @Test
    public void everyPublicRouteFileParsesAndValidates() throws Exception
    {
        Path directory = Paths.get("src", "main", "resources", "routes");
        List<Path> routeFiles;
        try (Stream<Path> files = Files.list(directory))
        {
            routeFiles = files.filter(path -> path.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());
        }
        assertTrue("Expected at least one public route JSON file", !routeFiles.isEmpty());

        Set<String> questNames = Arrays.stream(Quest.values()).map(Quest::getName).collect(Collectors.toSet());
        for (Path routeFile : routeFiles)
        {
            try (InputStream input = Files.newInputStream(routeFile);
                 InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8))
            {
                Route route = new RouteLoader(new Gson()).load(reader, routeFile.toString());
                new RouteValidator(questNames).validate(route);
            }
        }
    }

    @Test
    public void gearRoadmapHasCoreAndOptionalModernMilestones() throws Exception
    {
        Route gear = new RouteLoader(new Gson()).loadResource("/routes/gear-roadmap.json");
        new RouteValidator().validate(gear);

        List<RouteStep> steps = gear.getSections().stream()
            .flatMap(section -> section.getSteps().stream())
            .collect(Collectors.toList());
        assertEquals(46, steps.size());
        assertEquals(25, steps.stream().filter(step -> !step.isOptional()).count());
        assertEquals(21, steps.stream().filter(RouteStep::isOptional).count());
        assertTrue(hasTitle(gear, "Zombie axe bridge"));
        assertTrue(hasTitle(gear, "Bowfa and Crystal armour branch"));
        assertTrue(hasTitle(gear, "Twinflame staff from Royal Titans"));
        assertTrue(hasTitle(gear, "Doom of Mokhaiotl upgrades"));
        assertTrue(hasTitle(gear, "Soulflame horn"));
    }

    private boolean hasTitle(Route route, String title)
    {
        return route.getSections().stream().flatMap(section -> section.getSteps().stream())
            .anyMatch(step -> title.equals(step.getTitle()));
    }

    private RouteStep findStep(Route route, String title)
    {
        return route.getSections().stream().flatMap(section -> section.getSteps().stream())
            .filter(step -> title.equals(step.getTitle()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing route step: " + title));
    }
}
