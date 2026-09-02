package com.ironcompass.state;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.game.ItemVariationMapping;

public final class AccountState
{
    private final boolean loggedIn;
    private final AccountMode accountMode;
    private final Map<String, Integer> skills;
    private final Map<String, Integer> skillExperiences;
    private final Map<String, QuestProgress> quests;
    private final Map<Integer, Integer> inventory;
    private final Map<Integer, Integer> equipment;
    private final Map<Integer, Integer> exactInventory;
    private final Map<Integer, Integer> exactEquipment;
    private final BankSnapshot bank;
    private final Map<Integer, Integer> varbits;
    private final Map<Integer, Integer> varps;
    private final WorldLocation location;
    private final int questPoints;

    private AccountState(Builder builder)
    {
        loggedIn = builder.loggedIn;
        accountMode = builder.accountMode;
        skills = immutableCopy(builder.skills);
        skillExperiences = immutableCopy(builder.skillExperiences);
        quests = Collections.unmodifiableMap(new HashMap<>(builder.quests));
        inventory = immutableCopy(builder.inventory);
        equipment = immutableCopy(builder.equipment);
        exactInventory = immutableCopy(builder.exactInventory);
        exactEquipment = immutableCopy(builder.exactEquipment);
        bank = builder.bank;
        varbits = immutableCopy(builder.varbits);
        varps = immutableCopy(builder.varps);
        location = builder.location;
        questPoints = builder.questPoints;
    }

    private static <K> Map<K, Integer> immutableCopy(Map<K, Integer> source)
    {
        return Collections.unmodifiableMap(new HashMap<>(source));
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static AccountState loggedOut()
    {
        return builder().loggedIn(false).build();
    }

    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    public AccountMode getAccountMode()
    {
        return accountMode;
    }

    public int skillLevel(String skill)
    {
        return skills.getOrDefault(normalize(skill), 0);
    }

    /** Exact XP observed from RuneLite, or -1 when a synthetic/offline snapshot only supplied a level. */
    public int skillExperience(String skill)
    {
        return skillExperiences.getOrDefault(normalize(skill), -1);
    }

    public QuestProgress questState(String quest)
    {
        return quests.getOrDefault(normalize(quest), QuestProgress.UNKNOWN);
    }

    public int inventoryQuantity(int itemId)
    {
        return inventory.getOrDefault(itemId, 0);
    }

    public int equipmentQuantity(int itemId)
    {
        return equipment.getOrDefault(itemId, 0);
    }

    public int carriedQuantity(int itemId)
    {
        return inventoryQuantity(itemId) + equipmentQuantity(itemId);
    }

    public int exactInventoryQuantity(int itemId)
    {
        return exactInventory.getOrDefault(itemId, 0);
    }

    public int exactEquipmentQuantity(int itemId)
    {
        return exactEquipment.getOrDefault(itemId, 0);
    }

    public int exactCarriedQuantity(int itemId)
    {
        return exactInventoryQuantity(itemId) + exactEquipmentQuantity(itemId);
    }

    public BankSnapshot getBank()
    {
        return bank;
    }

    public int varbit(int id)
    {
        return varbits.getOrDefault(id, Integer.MIN_VALUE);
    }

    public int varp(int id)
    {
        return varps.getOrDefault(id, Integer.MIN_VALUE);
    }

    public WorldLocation getLocation()
    {
        return location;
    }

    public int getQuestPoints()
    {
        return questPoints;
    }

    public Map<String, Integer> getSkills()
    {
        return skills;
    }

    public Map<String, QuestProgress> getQuests()
    {
        return quests;
    }

    /** Returns an immutable projection with one skill level changed and all other observations preserved. */
    public AccountState withSkillLevel(String skill, int level)
    {
        Builder projected = new Builder();
        projected.loggedIn = loggedIn;
        projected.accountMode = accountMode;
        projected.skills.putAll(skills);
        projected.skills.put(normalize(skill), level);
        projected.skillExperiences.putAll(skillExperiences);
        projected.quests.putAll(quests);
        projected.inventory.putAll(inventory);
        projected.equipment.putAll(equipment);
        projected.exactInventory.putAll(exactInventory);
        projected.exactEquipment.putAll(exactEquipment);
        projected.bank = bank;
        projected.varbits.putAll(varbits);
        projected.varps.putAll(varps);
        projected.location = location;
        projected.questPoints = questPoints;
        return projected.build();
    }

    public static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH)
            .replace('\u2019', '\'');
    }

    public static final class Builder
    {
        private boolean loggedIn = true;
        private AccountMode accountMode = AccountMode.UNKNOWN;
        private final Map<String, Integer> skills = new HashMap<>();
        private final Map<String, Integer> skillExperiences = new HashMap<>();
        private final Map<String, QuestProgress> quests = new HashMap<>();
        private final Map<Integer, Integer> inventory = new HashMap<>();
        private final Map<Integer, Integer> equipment = new HashMap<>();
        private final Map<Integer, Integer> exactInventory = new HashMap<>();
        private final Map<Integer, Integer> exactEquipment = new HashMap<>();
        private BankSnapshot bank = BankSnapshot.unknown();
        private final Map<Integer, Integer> varbits = new HashMap<>();
        private final Map<Integer, Integer> varps = new HashMap<>();
        private WorldLocation location;
        private int questPoints;

        public Builder loggedIn(boolean value)
        {
            loggedIn = value;
            return this;
        }

        public Builder accountMode(AccountMode value)
        {
            accountMode = value == null ? AccountMode.UNKNOWN : value;
            return this;
        }

        public Builder skill(String name, int level)
        {
            skills.put(normalize(name), level);
            return this;
        }

        public Builder skillExperience(String name, int experience)
        {
            skillExperiences.put(normalize(name), Math.max(0, experience));
            return this;
        }

        public Builder quest(String name, QuestProgress progress)
        {
            quests.put(normalize(name), progress);
            return this;
        }

        public Builder inventoryItem(int itemId, int quantity)
        {
            exactInventory.merge(itemId, quantity, Integer::sum);
            inventory.merge(ItemVariationMapping.map(itemId), quantity, Integer::sum);
            return this;
        }

        public Builder equipmentItem(int itemId, int quantity)
        {
            exactEquipment.merge(itemId, quantity, Integer::sum);
            equipment.merge(ItemVariationMapping.map(itemId), quantity, Integer::sum);
            return this;
        }

        public Builder bank(BankSnapshot value)
        {
            bank = value == null ? BankSnapshot.unknown() : value;
            return this;
        }

        public Builder varbit(int id, int value)
        {
            varbits.put(id, value);
            return this;
        }

        public Builder varp(int id, int value)
        {
            varps.put(id, value);
            return this;
        }

        public Builder location(WorldLocation value)
        {
            location = value;
            return this;
        }

        public Builder questPoints(int value)
        {
            questPoints = value;
            return this;
        }

        public AccountState build()
        {
            return new AccountState(this);
        }
    }
}
