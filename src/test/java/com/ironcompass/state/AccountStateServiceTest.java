package com.ironcompass.state;

import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Node;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AccountStateServiceTest
{
    @Test
    public void clearSessionForProfileSwitchOrLogoutInvalidatesObservedBank()
    {
        AccountStateService service = new AccountStateService(null);
        service.observeBank(new StubContainer(new Item(11865, 1)));
        assertTrue(service.bankSnapshot().isObserved());

        service.clearSession();
        assertFalse(service.bankSnapshot().isObserved());

        service.observeBank(new StubContainer(new Item(12954, 1)));
        assertTrue(service.bankSnapshot().isObserved());
        assertTrue(service.bankSnapshot().exactQuantity(12954) == 1);
        assertTrue(service.bankSnapshot().exactQuantity(11865) == 0);
    }

    private static final class StubContainer implements ItemContainer
    {
        private final Item[] items;

        private StubContainer(Item... items) { this.items = items; }
        @Override public int getId() { return 0; }
        @Override public Item[] getItems() { return items; }
        @Override public Item getItem(int slot) { return items[slot]; }
        @Override public boolean contains(int itemId) { return find(itemId) >= 0; }
        @Override public int count(int itemId) { return contains(itemId) ? 1 : 0; }
        @Override public int size() { return items.length; }
        @Override public int count() { return items.length; }
        @Override public int find(int itemId)
        {
            for (int i = 0; i < items.length; i++) if (items[i].getId() == itemId) return i;
            return -1;
        }
        @Override public Node getNext() { return null; }
        @Override public Node getPrevious() { return null; }
        @Override public long getHash() { return 0L; }
    }
}
