package com.ironcompass.training;

import java.util.Arrays;

final class BankedGoalIngredient
{
    private final String label;
    private final int unitsPerAction;
    private final int[] itemIds;

    BankedGoalIngredient(String label, int unitsPerAction, int... itemIds)
    {
        this.label = label;
        this.unitsPerAction = Math.max(1, unitsPerAction);
        this.itemIds = Arrays.copyOf(itemIds, itemIds.length);
    }

    String getLabel() { return label; }
    int getUnitsPerAction() { return unitsPerAction; }
    int[] getItemIds() { return Arrays.copyOf(itemIds, itemIds.length); }

    String stockKey()
    {
        int[] sorted = Arrays.copyOf(itemIds, itemIds.length);
        Arrays.sort(sorted);
        return Arrays.toString(sorted);
    }
}
