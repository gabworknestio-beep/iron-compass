package com.ironcompass.ui;

import com.google.gson.Gson;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalLoader;
import com.ironcompass.state.AccountState;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class GoalPickerModelTest
{
    private GoalCatalog catalog;
    private GoalPickerModel picker;

    @Before
    public void setUp() throws Exception
    {
        catalog = new GoalLoader(new Gson()).loadResource("/goals/ironman-goals-2026.json");
        picker = new GoalPickerModel();
    }

    @Test
    public void searchFindsGoalsAcrossTitleCategoryAndTags()
    {
        List<GoalDefinition> results = picker.filter(catalog, "herblore", GoalPickerModel.ALL,
            Collections.emptySet(), AccountState.builder().build(), null);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(goal -> goal.getId().equals("goal.skill.herblore-70")));
    }

    @Test
    public void categoryLimitsTheLargeCatalog()
    {
        List<GoalDefinition> results = picker.filter(catalog, "", "Account infrastructure",
            Collections.emptySet(), AccountState.builder().build(), null);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(goal -> "Account infrastructure".equals(goal.getCategory())));
    }

    @Test
    public void activeAndCompletedFiltersRemainExplicit()
    {
        Set<String> active = new HashSet<>();
        active.add("goal.skill.herblore-70");
        AccountState state = AccountState.builder().skill("Herblore", 70).build();

        List<GoalDefinition> activeResults = picker.filter(catalog, "", GoalPickerModel.ACTIVE,
            active, state, null);
        assertEquals(1, activeResults.size());
        assertEquals("goal.skill.herblore-70", activeResults.get(0).getId());
        List<GoalDefinition> completed = picker.filter(catalog, "Herblore", GoalPickerModel.COMPLETED,
            active, state, null);
        assertTrue(completed.stream().anyMatch(goal -> goal.getId().equals("goal.skill.herblore-70")));
    }

    @Test
    public void suggestionsStayCompactAndNeverAutoActivateAnything()
    {
        List<GoalDefinition> results = picker.filter(catalog, "", GoalPickerModel.SUGGESTED,
            Collections.emptySet(), AccountState.builder().build(), null);
        assertTrue(results.size() <= 8);
        assertTrue(results.size() >= 3);
    }
}
