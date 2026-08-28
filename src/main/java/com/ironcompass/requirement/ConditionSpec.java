package com.ironcompass.requirement;

import java.util.Collections;
import java.util.List;

public final class ConditionSpec
{
    private String type;
    private String label;
    private List<ConditionSpec> children;
    private ConditionSpec child;
    private String skill;
    private List<String> skills;
    private int level;
    private String quest;
    private String state;
    private int itemId;
    private List<Integer> itemIds;
    private int quantity = 1;
    private String source;
    private int id;
    private int value;
    private int x;
    private int y;
    private int plane;
    private int radius = 4;
    private List<String> accountTypes;

    public String getType() { return type; }
    public String getLabel() { return label; }
    public List<ConditionSpec> getChildren() { return children == null ? Collections.emptyList() : children; }
    public ConditionSpec getChild() { return child; }
    public String getSkill() { return skill; }
    public List<String> getSkills() { return skills == null ? Collections.emptyList() : skills; }
    public int getLevel() { return level; }
    public String getQuest() { return quest; }
    public String getState() { return state; }
    public int getItemId() { return itemId; }
    public List<Integer> getItemIds() { return itemIds == null ? Collections.emptyList() : itemIds; }
    public int getQuantity() { return quantity; }
    public String getSource() { return source == null ? "ANY" : source; }
    public int getId() { return id; }
    public int getValue() { return value; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getPlane() { return plane; }
    public int getRadius() { return radius; }
    public List<String> getAccountTypes() { return accountTypes == null ? Collections.emptyList() : accountTypes; }
}
