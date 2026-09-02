package com.ironcompass.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.gameval.ItemID;

/**
 * Curated, conservative bank-to-goal conversions. Each value is XP per successful action.
 * Recipes intentionally cover common bankable Ironman resources instead of pretending every skill is bankable.
 */
final class BankedGoalRecipes
{
    private static final List<BankedGoalRecipe> RECIPES = build();

    private BankedGoalRecipes() {}

    static List<BankedGoalRecipe> all() { return RECIPES; }

    private static List<BankedGoalRecipe> build()
    {
        List<BankedGoalRecipe> values = new ArrayList<>();

        // Construction: standard product XP per plank. Coins/log conversion are deliberately excluded.
        values.add(recipe("Construction", "Regular plank products", 1, 29, one("Regular planks", ItemID.WOODPLANK)));
        values.add(recipe("Construction", "Oak plank products", 15, 60, one("Oak planks", ItemID.PLANK_OAK)));
        values.add(recipe("Construction", "Teak plank products", 35, 90, one("Teak planks", ItemID.PLANK_TEAK)));
        values.add(recipe("Construction", "Mahogany plank products", 40, 140, one("Mahogany planks", ItemID.PLANK_MAHOGANY)));

        // Herblore: useful potion choices. Vials of water are cheap infrastructure and are not counted.
        values.add(potion("Attack potions", 3, 25,
            any("Guam", ItemID.GUAM_LEAF, ItemID.UNIDENTIFIED_GUAM, ItemID.GUAMVIAL),
            one("Eye of newt", ItemID.EYE_OF_NEWT)));
        values.add(potion("Strength potions", 12, 50,
            any("Tarromin", ItemID.TARROMIN, ItemID.UNIDENTIFIED_TARROMIN, ItemID.TARROMINVIAL),
            one("Limpwurt root", ItemID.LIMPWURT_ROOT)));
        values.add(potion("Energy potions", 26, 67.5,
            any("Harralander", ItemID.HARRALANDER, ItemID.UNIDENTIFIED_HARRALANDER, ItemID.HARRALANDERVIAL),
            one("Chocolate dust", ItemID.CHOCOLATE_DUST)));
        values.add(potion("Prayer potions", 38, 87.5,
            any("Ranarr", ItemID.RANARR_WEED, ItemID.UNIDENTIFIED_RANARR, ItemID.RANARRVIAL),
            one("Snape grass", ItemID.SNAPE_GRASS)));
        values.add(potion("Super attack potions", 45, 100,
            any("Irit", ItemID.IRIT_LEAF, ItemID.UNIDENTIFIED_IRIT, ItemID.IRITVIAL),
            one("Eye of newt", ItemID.EYE_OF_NEWT)));
        values.add(potion("Super energy potions", 52, 117.5,
            any("Avantoe", ItemID.AVANTOE, ItemID.UNIDENTIFIED_AVANTOE, ItemID.AVANTOEVIAL),
            one("Mort myre fungus", ItemID.MORTMYREMUSHROOM)));
        values.add(potion("Super strength potions", 55, 125,
            any("Kwuarm", ItemID.KWUARM, ItemID.UNIDENTIFIED_KWUARM, ItemID.KWUARMVIAL),
            one("Limpwurt root", ItemID.LIMPWURT_ROOT)));
        values.add(potion("Prayer regeneration potions", 58, 132,
            any("Huasca", ItemID.HUASCA, ItemID.UNIDENTIFIED_HUASCA, ItemID.HUASCAVIAL),
            one("Aldarium", ItemID.ALDARIUM)));
        values.add(potion("Super restore potions", 63, 142.5,
            any("Snapdragon", ItemID.SNAPDRAGON, ItemID.UNIDENTIFIED_SNAPDRAGON, ItemID.SNAPDRAGONVIAL),
            one("Red spiders' eggs", ItemID.RED_SPIDERS_EGGS)));
        values.add(potion("Super defence potions", 66, 150,
            any("Cadantine", ItemID.CADANTINE, ItemID.UNIDENTIFIED_CADANTINE, ItemID.CADANTINEVIAL),
            one("White berries", ItemID.WHITE_BERRIES)));
        values.add(potion("Antifire potions", 69, 157.5,
            any("Lantadyme", ItemID.LANTADYME, ItemID.UNIDENTIFIED_LANTADYME, ItemID.LANTADYMEVIAL),
            one("Dragon scale dust", ItemID.DRAGON_SCALE_DUST)));
        values.add(potion("Ranging potions", 72, 162.5,
            any("Dwarf weed", ItemID.DWARF_WEED, ItemID.UNIDENTIFIED_DWARF_WEED, ItemID.DWARFWEEDVIAL),
            one("Wine of Zamorak", ItemID.WINE_OF_ZAMORAK)));
        values.add(potion("Magic potions", 76, 172.5,
            any("Lantadyme", ItemID.LANTADYME, ItemID.UNIDENTIFIED_LANTADYME, ItemID.LANTADYMEVIAL),
            one("Potato cactus", ItemID.CACTUS_POTATO)));
        values.add(potion("Zamorak brews", 78, 175,
            any("Torstol", ItemID.TORSTOL, ItemID.UNIDENTIFIED_TORSTOL, ItemID.TORSTOLVIAL),
            one("Jangerberries", ItemID.JANGERBERRIES)));
        values.add(potion("Saradomin brews", 81, 180,
            any("Toadflax", ItemID.TOADFLAX, ItemID.UNIDENTIFIED_TOADFLAX, ItemID.TOADFLAXVIAL),
            one("Crushed nests", ItemID.CRUSHED_BIRD_NEST)));

        // Prayer: safe Ectofuntus multiplier; worship supplies and travel time remain an estimate.
        values.add(prayer("Bones at Ectofuntus", 1, 18, ItemID.BONES));
        values.add(prayer("Big bones at Ectofuntus", 1, 60, ItemID.BIG_BONES));
        values.add(prayer("Babydragon bones at Ectofuntus", 1, 120, ItemID.BABYDRAGON_BONES));
        values.add(prayer("Wyrm bones at Ectofuntus", 1, 200, ItemID.WYRM_BONES));
        values.add(prayer("Dragon bones at Ectofuntus", 1, 288, ItemID.DRAGON_BONES));
        values.add(prayer("Wyvern bones at Ectofuntus", 1, 288, ItemID.WYVERN_BONES));
        values.add(prayer("Drake bones at Ectofuntus", 1, 320, ItemID.DRAKE_BONES));
        values.add(prayer("Frost dragon bones at Ectofuntus", 1, 400, ItemID.FROST_DRAGON_BONES));
        values.add(prayer("Hydra bones at Ectofuntus", 1, 440, ItemID.HYDRA_BONES));
        values.add(prayer("Dagannoth bones at Ectofuntus", 1, 500, ItemID.DAGANNOTH_KING_BONES));
        values.add(prayer("Superior dragon bones at Ectofuntus", 70, 600, ItemID.DRAGON_BONES_SUPERIOR));
        values.add(recipe("Prayer", "Blessed bone shards with blessed wine", 30, 5,
            one("Blessed bone shards", ItemID.BLESSED_BONE_SHARD)));

        // Cooking: successful-cook XP. Burn chance is why the projection is explicitly an estimate.
        values.add(cook("Shrimps", 1, 30, ItemID.RAW_SHRIMP));
        values.add(cook("Trout", 15, 70, ItemID.RAW_TROUT));
        values.add(cook("Salmon", 25, 90, ItemID.RAW_SALMON));
        values.add(cook("Tuna", 30, 100, ItemID.RAW_TUNA));
        values.add(cook("Karambwans", 30, 190, ItemID.TBWT_RAW_KARAMBWAN));
        values.add(cook("Lobsters", 40, 120, ItemID.RAW_LOBSTER));
        values.add(cook("Swordfish", 45, 140, ItemID.RAW_SWORDFISH));
        values.add(cook("Monkfish", 62, 150, ItemID.RAW_MONKFISH));
        values.add(cook("Sunlight antelope", 68, 175, ItemID.HUNTING_ANTELOPESUN_MEAT));
        values.add(cook("Sharks", 80, 210, ItemID.RAW_SHARK));
        values.add(cook("Anglerfish", 84, 230, ItemID.RAW_ANGLERFISH));
        values.add(cook("Dark crabs", 90, 215, ItemID.RAW_DARK_CRAB));
        values.add(cook("Manta rays", 91, 216.2, ItemID.RAW_MANTARAY));
        values.add(cook("Moonlight antelope", 92, 220, ItemID.HUNTING_ANTELOPEMOON_MEAT));

        // Crafting: best common glass product currently unlocked plus cut gems.
        values.add(craft("Beer glasses", 1, 17.5, ItemID.MOLTEN_GLASS));
        values.add(craft("Vials", 33, 35, ItemID.MOLTEN_GLASS));
        values.add(craft("Unpowered orbs", 46, 52.5, ItemID.MOLTEN_GLASS));
        values.add(craft("Lantern lenses", 49, 55, ItemID.MOLTEN_GLASS));
        values.add(craft("Empty light orbs", 87, 70, ItemID.MOLTEN_GLASS));
        values.add(craft("Cut opals", 1, 15, ItemID.UNCUT_OPAL));
        values.add(craft("Cut jades", 13, 20, ItemID.UNCUT_JADE));
        values.add(craft("Cut red topaz", 16, 25, ItemID.UNCUT_RED_TOPAZ));
        values.add(craft("Cut sapphires", 20, 50, ItemID.UNCUT_SAPPHIRE));
        values.add(craft("Cut emeralds", 27, 67.5, ItemID.UNCUT_EMERALD));
        values.add(craft("Cut rubies", 34, 85, ItemID.UNCUT_RUBY));
        values.add(craft("Cut diamonds", 43, 107.5, ItemID.UNCUT_DIAMOND));
        values.add(craft("Cut dragonstones", 55, 137.5, ItemID.UNCUT_DRAGONSTONE));
        values.add(craft("Cut onyx", 67, 167.5, ItemID.UNCUT_ONYX));
        values.add(craft("Cut zenyte", 89, 200, ItemID.UNCUT_ZENYTE));

        // Smithing: anvil XP per bar. Ores are excluded because coal/catalyst allocation changes the result.
        values.add(smith("Bronze bars", 1, 12.5, ItemID.BRONZE_BAR));
        values.add(smith("Iron bars", 15, 25, ItemID.IRON_BAR));
        values.add(smith("Steel bars", 30, 37.5, ItemID.STEEL_BAR));
        values.add(smith("Mithril bars", 50, 50, ItemID.MITHRIL_BAR));
        values.add(smith("Adamantite bars", 70, 62.5, ItemID.ADAMANTITE_BAR));
        values.add(smith("Runite bars", 85, 75, ItemID.RUNITE_BAR));

        // Farming: planting plus health-check XP for common tree saplings; survival/payment varies.
        values.add(farm("Oak saplings", 15, 481.3, ItemID.PLANTPOT_OAK_SAPLING));
        values.add(farm("Willow saplings", 30, 1481.5, ItemID.PLANTPOT_WILLOW_SAPLING));
        values.add(farm("Maple saplings", 45, 3448.4, ItemID.PLANTPOT_MAPLE_SAPLING));
        values.add(farm("Teak saplings", 35, 7325, ItemID.PLANTPOT_TEAK_SAPLING));
        values.add(farm("Mahogany saplings", 55, 15783, ItemID.PLANTPOT_MAHOGANY_SAPLING));
        values.add(farm("Yew saplings", 60, 7150.9, ItemID.PLANTPOT_YEW_SAPLING));
        values.add(farm("Papaya saplings", 57, 6380.4, ItemID.PLANTPOT_PAPAYA_SAPLING));
        values.add(farm("Palm saplings", 68, 10509.6, ItemID.PLANTPOT_PALM_SAPLING));
        values.add(farm("Crystal saplings", 74, 13366, ItemID.PLANTPOT_CRYSTAL_TREE_SAPLING));
        values.add(farm("Magic saplings", 75, 13913.8, ItemID.PLANTPOT_MAGIC_TREE_SAPLING));
        values.add(farm("Dragonfruit saplings", 81, 17825, ItemID.PLANTPOT_DRAGONFRUIT_SAPLING));
        values.add(farm("Spirit saplings", 83, 19500, ItemID.PLANTPOT_SPIRIT_TREE_SAPLING));
        values.add(farm("Celastrus saplings", 85, 14404.5, ItemID.PLANTPOT_CELASTRUS_TREE_SAPLING));
        values.add(farm("Redwood saplings", 90, 22680, ItemID.PLANTPOT_REDWOOD_TREE_SAPLING));

        // Fletching: best common unstrung product currently unlocked for each log family.
        values.add(fletch("Arrow shafts from logs", 1, 5, ItemID.LOGS));
        values.add(fletch("Longbows (u)", 10, 10, ItemID.LOGS));
        values.add(fletch("Oak arrow shafts", 15, 10, ItemID.OAK_LOGS));
        values.add(fletch("Oak longbows (u)", 25, 25, ItemID.OAK_LOGS));
        values.add(fletch("Willow arrow shafts", 30, 15, ItemID.WILLOW_LOGS));
        values.add(fletch("Willow longbows (u)", 40, 41.5, ItemID.WILLOW_LOGS));
        values.add(fletch("Maple arrow shafts", 45, 20, ItemID.MAPLE_LOGS));
        values.add(fletch("Maple longbows (u)", 55, 58.3, ItemID.MAPLE_LOGS));
        values.add(fletch("Yew arrow shafts", 60, 25, ItemID.YEW_LOGS));
        values.add(fletch("Yew longbows (u)", 70, 75, ItemID.YEW_LOGS));
        values.add(fletch("Magic arrow shafts", 75, 30, ItemID.MAGIC_LOGS));
        values.add(fletch("Magic longbows (u)", 85, 91.5, ItemID.MAGIC_LOGS));
        values.add(fletch("Redwood arrow shafts", 90, 35, ItemID.REDWOOD_LOGS));
        values.add(fletch("Redwood shields", 92, 108, ItemID.REDWOOD_LOGS));

        // Firemaking: direct log burning.
        values.add(fire("Logs", 1, 40, ItemID.LOGS));
        values.add(fire("Oak logs", 15, 60, ItemID.OAK_LOGS));
        values.add(fire("Willow logs", 30, 90, ItemID.WILLOW_LOGS));
        values.add(fire("Teak logs", 35, 105, ItemID.TEAK_LOGS));
        values.add(fire("Maple logs", 45, 135, ItemID.MAPLE_LOGS));
        values.add(fire("Mahogany logs", 50, 157.5, ItemID.MAHOGANY_LOGS));
        values.add(fire("Yew logs", 60, 202.5, ItemID.YEW_LOGS));
        values.add(fire("Magic logs", 75, 303.8, ItemID.MAGIC_LOGS));
        values.add(fire("Redwood logs", 90, 350, ItemID.REDWOOD_LOGS));

        return Collections.unmodifiableList(values);
    }

    private static BankedGoalRecipe potion(String label, int level, double xp,
                                            BankedGoalIngredient herb, BankedGoalIngredient secondary)
    {
        return recipe("Herblore", label, level, xp, herb, secondary);
    }

    private static BankedGoalRecipe prayer(String label, int level, double xp, int itemId)
    {
        return recipe("Prayer", label, level, xp, one(label, itemId));
    }

    private static BankedGoalRecipe cook(String label, int level, double xp, int itemId)
    {
        return recipe("Cooking", "Cook " + label, level, xp, one(label, itemId));
    }

    private static BankedGoalRecipe craft(String label, int level, double xp, int itemId)
    {
        return recipe("Crafting", label, level, xp, one(label, itemId));
    }

    private static BankedGoalRecipe smith(String label, int level, double xp, int itemId)
    {
        return recipe("Smithing", "Smith " + label, level, xp, one(label, itemId));
    }

    private static BankedGoalRecipe farm(String label, int level, double xp, int itemId)
    {
        return recipe("Farming", "Grow " + label, level, xp, one(label, itemId));
    }

    private static BankedGoalRecipe fletch(String label, int level, double xp, int itemId)
    {
        return recipe("Fletching", label, level, xp, one(label, itemId));
    }

    private static BankedGoalRecipe fire(String label, int level, double xp, int itemId)
    {
        return recipe("Firemaking", "Burn " + label, level, xp, one(label, itemId));
    }

    private static BankedGoalRecipe recipe(String skill, String label, int level, double xp,
                                           BankedGoalIngredient... ingredients)
    {
        return new BankedGoalRecipe(skill, label, level, xp, ingredients);
    }

    private static BankedGoalIngredient one(String label, int itemId)
    {
        return new BankedGoalIngredient(label, 1, itemId);
    }

    private static BankedGoalIngredient any(String label, int... itemIds)
    {
        return new BankedGoalIngredient(label, 1, itemIds);
    }
}
