package com.ironcompass.goal;

import com.google.gson.Gson;
import com.ironcompass.gear.GearCatalog;
import com.ironcompass.gear.GearLoader;
import com.ironcompass.route.Route;
import com.ironcompass.route.RouteLoader;
import com.ironcompass.planner.EffortClass;
import com.ironcompass.state.AccountState;
import com.ironcompass.requirement.ConditionSpec;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
        assertEquals(6, catalog.getVersion());
        assertTrue(catalog.getGoals().size() >= 500 && catalog.getGoals().size() <= 600);
        assertEquals("2026-08-29", catalog.getAuditedAt());
        assertTrue(catalog.getSources().size() >= 45);
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

    @Test
    public void bundledCatalogHasMeaningfulCoverageAcrossEverySkill() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        Map<String, Integer> counts = new HashMap<>();
        for (GoalDefinition goal : catalog.getGoals())
            for (String skill : goal.getRelatedSkills())
                counts.merge(AccountState.normalize(skill), 1, Integer::sum);
        for (Skill skill : Skill.values())
            assertTrue(skill.getName(), counts.getOrDefault(AccountState.normalize(skill.getName()), 0) >= 4);
    }

    @Test
    public void bundledCatalogKeepsBroadCategoryAndModernContentCoverage() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        Map<String, Long> categoryCounts = catalog.getGoals().stream()
            .collect(Collectors.groupingBy(GoalDefinition::getCategory, Collectors.counting()));
        assertTrue(categoryCounts.getOrDefault("Skills", 0L) >= 180);
        assertTrue(categoryCounts.getOrDefault("Resources", 0L) >= 35);
        assertTrue(categoryCounts.getOrDefault("Transportation", 0L) >= 20);
        assertTrue(categoryCounts.getOrDefault("Quests", 0L) >= 50);
        assertTrue(categoryCounts.getOrDefault("Bossing", 0L) >= 35);
        for (String id : Arrays.asList("goal.skill.sailing-15", "goal.skill.sailing-50",
            "goal.skill.sailing-87", "goal.activity.hunter-rumours", "goal.activity.vale-totems",
            "goal.activity.golem-crafting", "goal.resource.moth-mixes", "goal.resource.food-karambwans",
            "goal.skill.herblore-38", "goal.transport.fairy-rings", "goal.unlock.piety",
            "gear.early.gloves", "goal.account.strong-poh", "gear.early.fire-cape",
            "goal.pvm.perilous-moons-loop", "gear.mid.bowfa", "goal.pvm.zulrah",
            "goal.raid.toa-normal", "gear.endgame.infernal", "goal.pvm.colosseum-prep",
            "gear.endgame.quiver", "goal.transport.sailors-amulet"))
            assertTrue(id, catalog.find(id) != null);
    }

    @Test
    public void bundledCatalogAvoidsOverusingOneEffortOrImpactBand() throws Exception
    {
        GoalCatalog catalog = new GoalLoader(gson).loadResource("/goals/ironman-goals-2026.json");
        long total = catalog.getGoals().size();
        long mediumEffort = catalog.getGoals().stream().filter(goal -> goal.getEffort() == EffortClass.MEDIUM).count();
        long highImpact = catalog.getGoals().stream().filter(goal -> goal.getImpact() == GoalImpact.HIGH).count();
        assertTrue(mediumEffort * 100 < total * 60);
        assertTrue(highImpact * 100 < total * 70);
    }

    @Test(expected = GoalValidationException.class)
    public void duplicateGoalIdsAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.same", "", "SKILL_AT_LEAST") + ","
            + goal("goal.same", "", "SKILL_AT_LEAST") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void duplicateNormalizedTitlesAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.first", "", "SKILL_AT_LEAST") + ","
            + goal("goal.second", "", "SKILL_AT_LEAST").replace("\"Title\"", "\" title! \"") + "]"));
    }

    @Test(expected = GoalValidationException.class)
    public void invalidRelatedQuestsAreRejected() throws Exception
    {
        validate(load("[" + goal("goal.valid", "", "SKILL_AT_LEAST")
            .replace("\"impact\"", "\"relatedQuests\":[\"Imaginary Quest\"],\"impact\"") + "]"));
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
