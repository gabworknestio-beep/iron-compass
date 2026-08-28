package com.ironcompass.training;

import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.route.RouteStep;
import com.ironcompass.route.StepType;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.BankSnapshot;
import java.util.Locale;

public final class SkillTrainingAdvisor
{
    public TrainingAdvice advise(RouteStep step, AccountState state)
    {
        if (step == null || step.getType() != StepType.TRAIN)
        {
            return null;
        }
        ConditionSpec target = skillTarget(step.getCompletion());
        if (target == null)
        {
            return null;
        }
        String skill = target.getSkill();
        int level = target.getLevel();
        String key = skill.toLowerCase(Locale.ENGLISH);
        Methods methods = methods(key, level, state);
        return new TrainingAdvice(methods.primary, methods.alternative, bankContext(key, state.getBank()));
    }

    private static ConditionSpec skillTarget(ConditionSpec condition)
    {
        if (condition == null) return null;
        if ("SKILL_AT_LEAST".equalsIgnoreCase(condition.getType())) return condition;
        for (ConditionSpec child : condition.getChildren())
        {
            ConditionSpec target = skillTarget(child);
            if (target != null) return target;
        }
        return skillTarget(condition.getChild());
    }

    private static Methods methods(String skill, int target, AccountState state)
    {
        switch (skill)
        {
            case "attack":
            case "strength":
            case "defence":
            case "hitpoints":
                return new Methods(target <= 70
                    ? "Gemstone Crab after Children of the Sun: continuous low-defence melee training."
                    : "Train through Slayer tasks so combat XP also advances drops, points, and supplies.",
                    "Scurrius is a more active alternative that teaches PvM movement and prayer.");
            case "ranged":
                return new Methods(target <= 70
                    ? "Use a bone/rune crossbow or cheap darts at Scurrius or Gemstone Crab."
                    : "Use Slayer and saved chinchompas; avoid consuming scarce boss ammunition for routine XP.",
                    "Hunter rumours feed the Sunlight crossbow route; Eclipse atlatl is a gear bridge, not only training gear.");
            case "magic":
                return new Methods(target < 70
                    ? "Prioritise quest XP, teleports, alchemy, and other useful spells before paid combat casting."
                    : "Burst or barrage multi-target Slayer after Desert Treasure I while banking useful drops.",
                    "Frost crabs are the low-attention combat option; Gemstone Crab has no magic weakness.");
            case "prayer":
                return new Methods("Use Varlamore's libation bowl for a safe resource-efficient route after Children of the Sun.",
                    state.getAccountMode().isHardcore()
                        ? "Avoid the Wilderness Chaos Altar while Hardcore; use safe shards, fossils, or a gilded altar."
                        : "Chaos Altar is faster per bone but carries Wilderness risk; choose it explicitly.");
            case "agility":
                return new Methods(target < 52 ? "Use rooftop courses and bank Marks of grace."
                    : "Use Hallowed Sepulchre after Sins of the Father for XP plus useful loot.",
                    "Rooftops remain the lower-attention Graceful/stamina route.");
            case "construction":
                return new Methods("Mahogany Homes gives much more XP per plank and reduces the Ironman cash burden.",
                    "Traditional furniture is faster when planks and coins are already banked.");
            case "cooking":
                return new Methods("Cook banked fish at the Hosidius range or another low-burn range.",
                    "Tempoross can gather Fishing supplies first; wines are faster but consume shop resources.");
            case "crafting":
                return new Methods(target < 61
                    ? "Use quest XP, charter-shop glass, and useful jewellery until Superglass Make."
                    : "Farm giant seaweed, mine sandstone, then use Superglass Make after Lunar Diplomacy and 77 Magic.",
                    "Process gems and battlestaves from PvM when they also solve jewellery or cash needs.");
            case "farming":
                return new Methods("Run Farming Guild contracts and preplant common assignments; add herb runs for Herblore.",
                    "Birdhouses feed tree seeds; prioritise valuable herbs in disease-free patches.");
            case "firemaking":
                return new Methods("Wintertodt supplies early cash and skilling resources; stop when its value to the account falls.",
                    "Burn or use gathered logs when a short quest requirement does not justify a dedicated grind.");
            case "fishing":
                return new Methods(target < 48 ? "Tempoross provides food, planks, jewellery and early Fishing XP."
                    : "Barbarian Fishing adds passive Agility and Strength; bank food when PvM supplies are the bottleneck.",
                    "Sailing's Deep Sea Trawling is a branch when its rewards also advance your chosen path.");
            case "fletching":
                return new Methods("Fletch logs already gathered through Woodcutting, Kingdom, or PvM; make useful ammunition first.",
                    "At 74, Varlamore can sustain Atlatl darts and the Sunlight-crossbow route.");
            case "herblore":
                return new Methods(target < 60 ? "Use quest rewards, herb runs, and potions with readily available secondaries."
                    : "Mastering Mixology gives more XP per herb after Children of the Sun, but is slower than ordinary potions.",
                    "Keep critical ranarr, snapdragon, toadflax, and antifire supplies instead of converting every herb to XP.");
            case "hunter":
                return new Methods(target < 46 ? "Run the highest birdhouse available every 50 minutes."
                    : "Hunter rumours at 46/57/72/91 provide Hunter XP plus herbs, logs, nests, meat, and Prayer resources.",
                    "Chinchompas are the focused Ranged-XP route; antelopes sustain Sunlight crossbow ammunition.");
            case "mining":
                return new Methods(target >= 41 ? "Calcified rocks after starting Perilous Moons provide low-attention XP and Prayer shards."
                    : "Use quest XP, iron, or Motherlode Mine according to whether ore or attention is the bottleneck.",
                    "Sandstone is preferred when Crafting glass is the real account goal.");
            case "runecraft":
                return new Methods(target >= 27 ? "Guardians of the Rift supplies runes, outfit progress, and steady Runecraft XP."
                    : "Use quest XP and Temple of the Eye to reach Guardians of the Rift.",
                    "ZMI or true blood runes become alternatives when pure XP or blood supply is the priority.");
            case "sailing":
                return new Methods("Complete Pandemonium, then combine courier/port tasks with encounters while upgrading the boat only as needed.",
                    "Deep Sea Trawling and Slayer-at-sea are branches; do not force Sailing beyond the selected unlock.");
            case "slayer":
                return new Methods("Use the best practical Slayer master, prioritise high-value unlocks, and train combat on-task.",
                    "A PvM-rush route can postpone the long Slayer climb; Warped sceptre, Moons, and Titans cover several gaps.");
            case "smithing":
                return new Methods("Giants' Foundry is slower but cheap, accessible, and offers excellent XP per bar.",
                    "Blast Furnace is faster when ore and cash are available; early Smithing quests avoid slow levels.");
            case "thieving":
                return new Methods(target < 50 ? "Use quest XP and stalls, then move to the method matching your attention level."
                    : "Stealing valuables gives lower-attention XP, cash, and some Prayer shards in Varlamore.",
                    "Blackjacking is faster and strong early cash, but much higher attention.");
            case "woodcutting":
                return new Methods("Use Forestry or teaks while retaining logs that solve Construction and Fletching goals.",
                    "Sulliusceps provide strong XP and fossils that convert into Prayer XP.");
            default:
                return new Methods("Use the current OSRS Wiki Ironman method for the shortest missing level block.",
                    "Prefer a method that also advances supplies or the selected gear goal.");
        }
    }

    private static String bankContext(String skill, BankSnapshot bank)
    {
        if (!bank.isObserved()) return "Banked materials unknown until the bank is opened this session.";
        switch (skill)
        {
            case "crafting":
                return "Bank inputs: " + sum(bank, 1783) + " sand, " + sum(bank, 21504) + " giant seaweed.";
            case "construction":
                return "Bank inputs: " + sum(bank, 960, 8778, 8780) + " planks across supported tiers.";
            case "herblore":
                return "Bank inputs: " + sum(bank, 199, 201, 203, 205, 207, 209, 211, 213, 215, 217, 219, 2485, 3049)
                    + " common grimy herbs; no XP total is guessed.";
            case "smithing":
                return "Bank inputs: " + sum(bank, 2349, 2351, 2353, 2359, 2361, 2363) + " metal bars.";
            case "fletching":
            case "firemaking":
            case "woodcutting":
                return "Bank inputs: " + sum(bank, 1511, 1521, 1519, 1517, 1515, 1513, 6333, 6332) + " logs.";
            case "prayer":
                return "Bank inputs: " + sum(bank, 526, 532, 536, 534, 4812, 6729, 29381) + " recognised bones/shard stacks.";
            case "cooking":
                return "Bank inputs: " + sum(bank, 317, 335, 331, 359, 371, 383, 389, 3142) + " recognised raw fish.";
            default:
                return "Bank scanned; Iron Compass does not invent an XP conversion for mixed materials.";
        }
    }

    private static int sum(BankSnapshot bank, int... itemIds)
    {
        int total = 0;
        for (int itemId : itemIds) total += bank.quantity(itemId);
        return total;
    }

    private static final class Methods
    {
        private final String primary;
        private final String alternative;

        private Methods(String primary, String alternative)
        {
            this.primary = primary;
            this.alternative = alternative;
        }
    }
}
