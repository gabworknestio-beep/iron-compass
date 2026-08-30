package com.ironcompass.training;

import java.util.Collections;
import java.util.List;

/** A small, Ironman-relevant level unlock. It may link back to an existing Goal Planner goal. */
public final class TrainingMilestone
{
    private String id;
    private String skill;
    private int level;
    private String title;
    private String ironmanValue;
    private String goalId;
    private String wikiPage;
    private List<String> sourceReferences;

    public String getId() { return id; }
    public String getSkill() { return skill; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public String getIronmanValue() { return ironmanValue; }
    public String getGoalId() { return goalId; }
    public String getWikiPage() { return wikiPage; }
    public List<String> getSourceReferences()
    {
        return sourceReferences == null ? Collections.emptyList() : sourceReferences;
    }
}
