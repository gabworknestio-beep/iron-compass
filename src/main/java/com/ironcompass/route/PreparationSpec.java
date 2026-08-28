package com.ironcompass.route;

public final class PreparationSpec
{
    private String kind;
    private String name;
    private int itemId;
    private int quantity;
    private String skill;
    private int level;
    private String source;
    private boolean consumable;

    public String getKind() { return kind; }
    public String getName() { return name; }
    public int getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
    public String getSkill() { return skill; }
    public int getLevel() { return level; }
    public String getSource() { return source == null ? "ANY" : source; }
    public boolean isConsumable() { return consumable; }
}
