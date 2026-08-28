package com.ironcompass.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.vars.AccountType;

public final class AccountStateService
{
    private final Client client;
    private BankSnapshot bank = BankSnapshot.unknown();
    private final Map<String, QuestProgress> quests = new HashMap<>();

    @Inject
    public AccountStateService(Client client)
    {
        this.client = client;
    }

    public AccountState capture(Set<Integer> varbitIds, Set<Integer> varpIds, boolean refreshQuests)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return AccountState.loggedOut();
        }

        AccountState.Builder builder = AccountState.builder()
            .loggedIn(true)
            .accountMode(accountMode());

        for (Skill skill : Skill.values())
        {
            builder.skill(skill.getName(), client.getRealSkillLevel(skill));
        }
        if (refreshQuests || quests.isEmpty())
        {
            quests.clear();
            for (Quest quest : Quest.values())
            {
                quests.put(quest.getName(), questProgress(quest.getState(client)));
            }
        }
        for (Map.Entry<String, QuestProgress> quest : quests.entrySet())
        {
            builder.quest(quest.getKey(), quest.getValue());
        }

        addItems(builder, client.getItemContainer(InventoryID.INV), false);
        addItems(builder, client.getItemContainer(InventoryID.WORN), true);
        builder.bank(bank);

        for (int id : varbitIds)
        {
            builder.varbit(id, client.getVarbitValue(id));
        }
        for (int id : varpIds)
        {
            builder.varp(id, client.getVarpValue(id));
        }

        WorldPoint point = client.getLocalPlayer().getWorldLocation();
        builder.location(new WorldLocation(point.getX(), point.getY(), point.getPlane()));
        return builder.build();
    }

    public void observeBank(ItemContainer container)
    {
        if (container == null)
        {
            return;
        }
        Map<Integer, Integer> quantities = new HashMap<>();
        for (Item item : container.getItems())
        {
            if (item.getId() > 0 && item.getQuantity() > 0)
            {
                quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
            }
        }
        bank = BankSnapshot.observed(quantities);
    }

    public void clearSession()
    {
        bank = BankSnapshot.unknown();
        quests.clear();
    }

    BankSnapshot bankSnapshot()
    {
        return bank;
    }

    private static void addItems(AccountState.Builder builder, ItemContainer container, boolean equipment)
    {
        if (container == null)
        {
            return;
        }
        for (Item item : container.getItems())
        {
            if (item.getId() <= 0 || item.getQuantity() <= 0)
            {
                continue;
            }
            if (equipment)
            {
                builder.equipmentItem(item.getId(), item.getQuantity());
            }
            else
            {
                builder.inventoryItem(item.getId(), item.getQuantity());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private AccountMode accountMode()
    {
        AccountType type = client.getAccountType();
        if (type == null)
        {
            return AccountMode.UNKNOWN;
        }
        switch (type.name())
        {
            case "NORMAL": return AccountMode.REGULAR;
            case "IRONMAN": return AccountMode.IRONMAN;
            case "ULTIMATE_IRONMAN": return AccountMode.ULTIMATE_IRONMAN;
            case "HARDCORE_IRONMAN": return AccountMode.HARDCORE_IRONMAN;
            case "GROUP_IRONMAN": return AccountMode.GROUP_IRONMAN;
            case "HARDCORE_GROUP_IRONMAN": return AccountMode.HARDCORE_GROUP_IRONMAN;
            default: return AccountMode.UNKNOWN;
        }
    }

    private static QuestProgress questProgress(QuestState state)
    {
        if (state == QuestState.FINISHED) return QuestProgress.FINISHED;
        if (state == QuestState.IN_PROGRESS) return QuestProgress.IN_PROGRESS;
        if (state == QuestState.NOT_STARTED) return QuestProgress.NOT_STARTED;
        return QuestProgress.UNKNOWN;
    }
}
