package com.ironcompass.training;

import java.util.Collections;
import java.util.List;

public final class MethodResourceGroup
{
    private String label;
    private List<Integer> itemIds;
    private int minimumUsefulQuantity = 1;

    public String getLabel() { return label; }
    public List<Integer> getItemIds() { return itemIds == null ? Collections.emptyList() : itemIds; }
    public int getMinimumUsefulQuantity() { return Math.max(1, minimumUsefulQuantity); }
}
