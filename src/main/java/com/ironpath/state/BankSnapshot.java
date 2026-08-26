package com.ironpath.state;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.game.ItemVariationMapping;

public final class BankSnapshot
{
    private static final BankSnapshot UNKNOWN = new BankSnapshot(false, Collections.emptyMap(),
        Collections.emptyMap(), 0L);

    private final boolean observed;
    private final Map<Integer, Integer> quantities;
    private final Map<Integer, Integer> exactQuantities;
    private final long observedAtEpochMillis;

    private BankSnapshot(boolean observed, Map<Integer, Integer> quantities, Map<Integer, Integer> exactQuantities,
                         long observedAtEpochMillis)
    {
        this.observed = observed;
        this.quantities = Collections.unmodifiableMap(new HashMap<>(quantities));
        this.exactQuantities = Collections.unmodifiableMap(new HashMap<>(exactQuantities));
        this.observedAtEpochMillis = observedAtEpochMillis;
    }

    public static BankSnapshot unknown()
    {
        return UNKNOWN;
    }

    public static BankSnapshot observed(Map<Integer, Integer> quantities)
    {
        return observed(quantities, System.currentTimeMillis());
    }

    public static BankSnapshot observed(Map<Integer, Integer> quantities, long observedAtEpochMillis)
    {
        Map<Integer, Integer> canonical = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet())
        {
            canonical.merge(ItemVariationMapping.map(entry.getKey()), entry.getValue(), Integer::sum);
        }
        return new BankSnapshot(true, canonical, quantities, observedAtEpochMillis);
    }

    public boolean isObserved()
    {
        return observed;
    }

    public int quantity(int itemId)
    {
        return quantities.getOrDefault(itemId, 0);
    }

    public int exactQuantity(int itemId)
    {
        return exactQuantities.getOrDefault(itemId, 0);
    }

    public long getObservedAtEpochMillis()
    {
        return observedAtEpochMillis;
    }

    public Map<Integer, Integer> getQuantities()
    {
        return quantities;
    }
}
