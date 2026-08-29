package com.ironcompass.goal;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.state.AccountState;
import com.ironcompass.requirement.ConditionSpec;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class GoalCatalogValidationTest
{
    private final Gson gson = new Gson();

    @Test
    public void bundledCatalogHasExpandedRepresentativeValidatedGoals() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        validate(catalog);
        assertEquals(5, catalog.getVersion());
        assertEquals(290, catalog.getGoals().size());
        assertEquals("2026-08-29", catalog.getAuditedAt());
        assertEquals(30, catalog.getSources().size());
    }

    @Test
    public void liveVarlamoreAndSailingRequirementsAreHonest() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        assertTrue(hasQuest(catalog.find("goal.activity.vale-totems").getRequirements(),"Children of the Sun"));
        assertTrue(hasSkill(catalog.find("goal.activity.vale-totems").getRequirements(),"Fletching",20));
        assertTrue(hasQuest(catalog.find("goal.activity.hunter-rumours").getRequirements(),"Children of the Sun"));
        assertTrue(hasSkill(catalog.find("goal.activity.hunter-rumours").getRequirements(),"Hunter",46));
        assertTrue(hasQuest(catalog.find("goal.skill.hunter-75").getRequirements(),"Children of the Sun"));
        assertTrue(hasSkill(catalog.find("goal.activity.sailing-trawling").getRequirements(),"Construction",61));
        assertTrue(hasType(catalog.find("goal.activity.sailing-salvaging-station").getRequirements(),"MANUAL_ONLY"));
    }

    @Test
    public void everyBundledGoalHasRichPlayerFacingMetadata() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        assertEquals(catalog.getGoals().size(), new HashSet<>(catalog.getGoals().stream()
            .map(GoalDefinition::getId).collect(Collectors.toList())).size());
        for (GoalDefinition goal : catalog.getGoals())
        {
            org.junit.Assert.assertFalse(goal.getTitle().trim().isEmpty());
            org.junit.Assert.assertFalse(goal.getCategory().trim().isEmpty());
            org.junit.Assert.assertFalse(goal.getWhyItMatters().trim().isEmpty());
            org.junit.Assert.assertNotNull(goal.getStage());
            org.junit.Assert.assertFalse(goal.getUnlocks().isEmpty());
            org.junit.Assert.assertFalse(goal.getBenefits().isEmpty());
            org.junit.Assert.assertFalse(goal.getBenefits().equals(goal.getUnlocks()));
            org.junit.Assert.assertFalse(goal.getSourceReferences().isEmpty());
            org.junit.Assert.assertNotNull(goal.getCompletionMode());
            org.junit.Assert.assertNotNull(goal.getPriority());
            org.junit.Assert.assertNotNull(goal.getCommunityWeight());
        }
    }

    @Test
    public void bundledCatalogCoversAllStagesAndCoreCategories() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        assertEquals(GoalStage.values().length, catalog.getGoals().stream()
            .map(GoalDefinition::getStage).collect(Collectors.toSet()).size());
        Set<String> categories = catalog.getGoals().stream().map(GoalDefinition::getCategory)
            .collect(Collectors.toSet());
        for (String expected : Arrays.asList("Skills", "Resources", "Transportation", "Gear", "Slayer",
            "Achievement Diaries", "Minigames", "Bossing", "Raids", "Clue Scrolls"))
            org.junit.Assert.assertTrue(expected, categories.contains(expected));
    }

    @Test
    public void bundledCatalogCoversEveryIntentAndUsesAUsefulRelationshipGraph() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        Set<GoalIntent> intents = catalog.getGoals().stream().flatMap(goal -> goal.getIntents().stream())
            .collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(GoalIntent.values())),intents);
        long relationships = catalog.getGoals().stream().mapToLong(goal -> goal.getRelationships().size()).sum();
        org.junit.Assert.assertTrue(relationships >= 125);
        Set<GoalRelationshipType> types = catalog.getGoals().stream()
            .flatMap(goal -> goal.getRelationships().stream()).map(GoalRelationship::getType)
            .collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(GoalRelationshipType.values())),types);
    }

    @Test
    public void bundledMajorGoalsExposeAuditedDependencyChains() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        org.junit.Assert.assertTrue(catalog.find("gear.mid.bowfa").getDependencyIds()
            .contains("goal.pvm.corrupted-gauntlet"));
        org.junit.Assert.assertTrue(catalog.find("goal.resource.food-karambwans").getDependencyIds()
            .contains("goal.transport.fairy-rings"));
        org.junit.Assert.assertTrue(catalog.find("gear.endgame.infernal").getDependencyIds()
            .contains("goal.pvm.inferno-prep"));
    }

    @Test
    public void bundledCatalogCoversEveryCurrentRuneLiteSkill() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        Set<String> covered = catalog.getGoals().stream().flatMap(goal -> goal.getRelatedSkills().stream())
            .map(AccountState::normalize).collect(Collectors.toSet());
        for (Skill skill : Skill.values())
            org.junit.Assert.assertTrue(skill.getName(), covered.contains(AccountState.normalize(skill.getName())));
    }

    @Test(expected = GoalValidationException.class)
    public void duplicateGoalIdsAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.same", "", "SKILL_AT_LEAST") + ","
            + goal("goal.same", "", "SKILL_AT_LEAST") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void danglingDependenciesAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.valid", "goal.missing", "SKILL_AT_LEAST") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void dependencyCyclesAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.a", "goal.b", "SKILL_AT_LEAST") + ","
            + goal("goal.b", "goal.a", "SKILL_AT_LEAST") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void invalidConditionTypesAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.valid", "", "MADE_UP") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void persistenceSeparatorsAndMalformedWikiTitlesAreRejected() throws Exception
    {
        validate(load("[" + goal("goal:bad", "", "SKILL_AT_LEAST").replace("\"Wiki\"", "\"https://bad\"")
            + "]"));
    }

    private GoalCatalog load(String goals) throws Exception
    {
        return new GoalLoader(gson).load(new StringReader("{\"version\":1,\"goals\":" + goals + "}"), "test");
    }

    private void validate(GoalCatalog goals) throws Exception
    {
        GearCatalog gear = new GearLoader(gson).loadResource("/gear/ironman-gear-2026.json");
        Route route = new RouteLoader(gson).loadResource("/routes/efficient-ironman.json");
        Set<String> quests = Arrays.stream(Quest.values()).map(Quest::getName).collect(Collectors.toSet());
        new GoalValidator(quests).validate(goals, gear, route);
    }

    private static String goal(String id, String dependency, String conditionType)
    {
        String dependencies = dependency.isEmpty() ? "" : ",\"dependencyIds\":[\"" + dependency + "\"]";
        return "{\"id\":\"" + id + "\",\"title\":\"Title\",\"description\":\"Description\","
            + "\"category\":\"Test\",\"completion\":{\"type\":\"" + conditionType
            + "\",\"skill\":\"Magic\",\"level\":1},\"impact\":\"HIGH\",\"effort\":\"SHORT\","
            + "\"unlocks\":[\"Unlock\"],\"wikiPage\":\"Wiki\"" + dependencies + "}";
    }

    private static boolean hasQuest(ConditionSpec value, String quest)
    {
        if (value == null) return false;
        if (quest.equals(value.getQuest())) return true;
        for (ConditionSpec child : value.getChildren()) if (hasQuest(child,quest)) return true;
        return false;
    }

    private static boolean hasSkill(ConditionSpec value, String skill, int level)
    {
        if (value == null) return false;
        if (skill.equals(value.getSkill()) && value.getLevel() == level) return true;
        for (ConditionSpec child : value.getChildren()) if (hasSkill(child,skill,level)) return true;
        return false;
    }

    private static boolean hasType(ConditionSpec value, String type)
    {
        if (value == null) return false;
        if (type.equals(value.getType())) return true;
        for (ConditionSpec child : value.getChildren()) if (hasType(child,type)) return true;
        return false;
    }
}
