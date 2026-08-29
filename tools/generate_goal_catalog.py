"""Build the bundled, reviewed Iron Compass goal catalog. Run locally; never at plugin runtime."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/goals/ironman-goals-2026.json"
goals = []

SOURCES = [
    {"id":"wiki-ironman","title":"OSRS Wiki — Ironman guide","url":"https://oldschool.runescape.wiki/w/Ironman_guide","kind":"FACT_AND_GUIDANCE","confirms":"Common account foundations, modern midgame equipment, supplies, and transport unlocks."},
    {"id":"wiki-hunter","title":"OSRS Wiki — Ironman Hunter","url":"https://oldschool.runescape.wiki/w/Ironman_Guide/Hunter","kind":"FACT","confirms":"Bird houses, implings, rumours, antelopes, and the Hunter levels used by Ironmen."},
    {"id":"wiki-herblore","title":"OSRS Wiki — Ironman Herblore","url":"https://oldschool.runescape.wiki/w/Ironman_Guide/Herblore","kind":"FACT","confirms":"Potion level milestones and sustainable ingredient paths."},
    {"id":"wiki-farming","title":"OSRS Wiki — Ironman Farming","url":"https://oldschool.runescape.wiki/w/Ironman_Guide/Farming","kind":"FACT","confirms":"Farming contracts, seeds, bird houses, Master Farmers, and herb supply loops."},
    {"id":"wiki-crafting","title":"OSRS Wiki — Ironman Crafting","url":"https://oldschool.runescape.wiki/w/Ironman_Guide/Crafting","kind":"FACT","confirms":"Glass progression and jewellery utility for Ironmen."},
    {"id":"wiki-fishing","title":"OSRS Wiki — Ironman Fishing","url":"https://oldschool.runescape.wiki/w/Ironman_Guide/Fishing","kind":"FACT","confirms":"Tempoross, karambwans, minnows, and useful food progression."},
    {"id":"wiki-agility","title":"OSRS Wiki — Ironman Agility","url":"https://oldschool.runescape.wiki/w/Ironman_Guide/Agility","kind":"FACT","confirms":"Run energy, Graceful, shortcuts, and Hallowed Sepulchre milestones."},
    {"id":"wiki-runecraft","title":"OSRS Wiki — Guardians of the Rift strategies","url":"https://oldschool.runescape.wiki/w/Guardians_of_the_Rift/Strategies","kind":"FACT","confirms":"GOTR access, pouches, Raiments, and rune-production utility."},
    {"id":"wiki-construction","title":"OSRS Wiki — Ironman Construction","url":"https://oldschool.runescape.wiki/w/Ironman_Guide/Construction","kind":"FACT","confirms":"POH progression, Mahogany Homes, plank sourcing, and storage utility."},
    {"id":"wiki-slayer","title":"OSRS Wiki — Slayer level-up table","url":"https://oldschool.runescape.wiki/w/Slayer_level_up_table","kind":"FACT","confirms":"Exact Slayer levels for monsters and bosses."},
    {"id":"wiki-diaries","title":"OSRS Wiki — Achievement Diary rewards","url":"https://oldschool.runescape.wiki/w/Achievement_Diary/Rewards","kind":"FACT","confirms":"Diary transports, resource bonuses, and account conveniences."},
    {"id":"wiki-clues","title":"OSRS Wiki — Treasure Trails full guide","url":"https://oldschool.runescape.wiki/w/Treasure_Trails/Full_guide/All","kind":"FACT","confirms":"Clue tiers, access needs, travel tools, and optional reward paths."},
    {"id":"wiki-sailing","title":"OSRS Wiki — Ironman Sailing","url":"https://oldschool.runescape.wiki/w/Ironman_Guide/Sailing","kind":"FACT","confirms":"Sailing unlock, task ports, Barracuda Trials, and Ironman material considerations."},
    {"id":"wiki-gear","title":"Yazi's Ironman Gear Progression 2025","url":"https://oldschool.runescape.wiki/w/Guide:Yazi%27s_Ironman_Gear_Progression_2025","kind":"COMMUNITY_GUIDE","confirms":"A current gear ladder cross-checked against item and activity pages."},
    {"id":"wiki-mootrius","title":"Mootrius Ironman Guide","url":"https://oldschool.runescape.wiki/w/Guide:Mootrius_Ironman_Guide","kind":"COMMUNITY_GUIDE","confirms":"Modern Varlamore, Moons, Royal Titans, and Prayer-sustain progression patterns."},
    {"id":"reddit-stages","title":"r/ironscape — What defines account stages?","url":"https://www.reddit.com/r/ironscape/comments/1jvudlg/","kind":"COMMUNITY_RECOMMENDATION","confirms":"Stages are flexible; common milestones include gloves, fire cape, Bowfa, raids, Inferno, and maxing."},
    {"id":"reddit-goals","title":"r/ironscape — Account goals discussion","url":"https://www.reddit.com/r/ironscape/comments/1jp0tcw/","kind":"COMMUNITY_RECOMMENDATION","confirms":"Players value practical unlocks such as moths, rune pouch, fish barrel, Moons, DKs, and transport."},
    {"id":"reddit-modern","title":"r/ironscape — Modern midgame goals","url":"https://www.reddit.com/r/ironscape/comments/1ffrpj7/","kind":"COMMUNITY_RECOMMENDATION","confirms":"Piety, Moons, moths, zombie axe, diaries, Farming, Herblore, and GOTR are recurring priorities."},
    {"id":"youtube-slayer","title":"2026 Ironman Slayer progression guide","url":"https://www.youtube.com/watch?v=ZUe2ngFACHQ","kind":"COMMUNITY_RECOMMENDATION","confirms":"Players organize Slayer around multiple useful bands rather than only levels 87 and 93."},
    {"id":"bruhsailer","title":"BRUHsailer Ironman guide","url":"https://osrsper.github.io/BRUHsailer/","kind":"COMMUNITY_GUIDE","confirms":"Efficient progression dependencies and the value of flexible side objectives."}
    ,{"id":"wiki-knight-waves","title":"OSRS Wiki — Knight Waves Training Grounds","url":"https://oldschool.runescape.wiki/w/Knight_Waves_Training_Grounds","kind":"FACT","confirms":"King's Ransom, 70 Prayer, 70 Defence, and completion of the training ground are distinct Piety gates."}
    ,{"id":"wiki-fairytale-ii","title":"OSRS Wiki — Fairytale II","url":"https://oldschool.runescape.wiki/w/Fairytale_II_-_Cure_a_Queen","kind":"FACT","confirms":"The fairy-ring network unlocks during partial quest progress, before full quest completion."}
    ,{"id":"wiki-rumours","title":"OSRS Wiki — Hunters' Rumours","url":"https://oldschool.runescape.wiki/w/Hunters%27_Rumours","kind":"FACT","confirms":"Rumour tiers, 46 Hunter entry, and the 10/25/50/100/250 completion milestones."}
    ,{"id":"wiki-vale-totems","title":"OSRS Wiki — Vale Totems miniquest","url":"https://oldschool.runescape.wiki/w/Vale_Totems_%28miniquest%29","kind":"FACT","confirms":"The live miniquest requires Children of the Sun and 20 Fletching before the activity is unlocked."}
    ,{"id":"wiki-sailing-levels","title":"OSRS Wiki — Sailing level-up table","url":"https://oldschool.runescape.wiki/w/Sailing/Level_up_table","kind":"FACT","confirms":"The live linen trawling net requires 65 Sailing and 61 Construction."}
    ,{"id":"wiki-salvaging-station","title":"OSRS Wiki — Salvaging station facility","url":"https://oldschool.runescape.wiki/w/Salvaging_station_%28facility%29","kind":"FACT","confirms":"A salvaging station requires 42 Sailing, 34 Construction, and reading the salvaging-station schematic."}
    ,{"id":"community-golem","title":"OSRSIron — Golem Crafting","url":"https://osrsiron.com/crafting/golem-crafting","kind":"COMMUNITY_GUIDE","confirms":"Current Wyrmscraig access, Crafting requirement, fur inputs, and the glass-alternative training loop."}
    ,{"id":"reddit-2026-modern","title":"r/ironscape — modern early-account priorities","url":"https://www.reddit.com/r/ironscape/comments/1t4p4p6/","kind":"COMMUNITY_RECOMMENDATION","confirms":"Current players value transport, food, Prayer, money, and repeatable resource systems over a single rigid route."}
    ,{"id":"reddit-vale","title":"r/ironscape — Vale Totems experience","url":"https://www.reddit.com/r/ironscape/comments/1r6gfil/","kind":"COMMUNITY_RECOMMENDATION","confirms":"Vale Totems can produce useful roots, nests, arrowtips, and logs, but 99 is a long optional grind."}
    ,{"id":"reddit-sailing","title":"r/ironscape — useful Sailing unlocks","url":"https://www.reddit.com/r/ironscape/comments/1vanvsd/","kind":"COMMUNITY_RECOMMENDATION","confirms":"Players highlight salvage, Wyrmscraig, Golem Crafting, trawling, and regional unlocks rather than level for its own sake."}
]

def manual(label):
    return {"type":"MANUAL_ONLY", "label":label}

def skill_condition(skill, level):
    return {"type":"SKILL_AT_LEAST", "label":f"{skill} {level}", "skill":skill, "level":level}

def quest_condition(quest):
    return {"type":"QUEST_STATE", "label":f"{quest} complete", "quest":quest, "state":"FINISHED"}

def item_any(label, item_ids, quantity=1):
    return {"type":"ITEM_ANY", "label":label, "itemIds":item_ids, "quantity":quantity, "source":"ANY"}

def all_condition(label, *children):
    return {"type":"ALL", "label":label, "children":list(children)}

def any_condition(label, *children):
    return {"type":"ANY", "label":label, "children":list(children)}

def add(id, title, category, stage, description, why, unlocks, wiki, tags, *,
        completion=None, requirements=None, dependencies=None, gear=None, impact="HIGH",
        effort="MEDIUM", usefulness=3, popular=False, rng=False, risk="SAFE",
        skills=None, quests=None, activities=None, items=None, accounts=None, sources=None,
        route=None, benefits=None, completion_mode=None, priority=None, community=None, intents=None,
        relationships=None):
    goal = {
        "id":id, "title":title, "description":description, "whyItMatters":why,
        "category":category, "stage":stage, "completion":completion or manual(f"Confirm {title}"),
        "impact":impact, "effort":effort, "usefulness":usefulness, "popular":popular,
        "rng":rng, "riskLevel":risk, "unlocks":unlocks, "benefits":benefits or unlocks,
        "relatedItems":items or [], "relatedSkills":skills or [], "relatedQuests":quests or [],
        "relatedActivities":activities or [], "accountTypes":accounts or [],
        "sourceReferences":sources or ["wiki-ironman"], "wikiPage":wiki, "tags":tags
    }
    if completion_mode: goal["completionMode"] = completion_mode
    if priority: goal["priority"] = priority
    if community: goal["communityWeight"] = community
    if intents: goal["intents"] = intents
    if relationships: goal["relationships"] = relationships
    if requirements is not None: goal["requirements"] = requirements
    if dependencies: goal["dependencyIds"] = dependencies
    if gear:
        goal["gearId"] = gear
        if completion is None: goal.pop("completion", None)
    if route: goal["routeAnchorId"] = route
    goals.append(goal)

def skill(id, title, skill_name, level, stage, why, unlocks, wiki, tags, **kwargs):
    condition = skill_condition(skill_name, level)
    add(id, title, "Skills", stage, f"Reach {level} {skill_name} for this practical Ironman milestone.",
        why, unlocks, wiki, tags + [skill_name.lower(), "skill-unlock"], completion=condition,
        requirements=condition, skills=[skill_name], **kwargs)

def quest(id, title, quest_name, stage, why, unlocks, wiki, tags, **kwargs):
    condition = quest_condition(quest_name)
    add(id, title, "Quests", stage, f"Complete {quest_name} and make its account unlocks available.", why,
        unlocks, wiki, tags + ["quest"], completion=condition, requirements=condition,
        quests=[quest_name], **kwargs)

# Skill unlocks: small and medium milestones deliberately coexist with the headline goals.
SKILL_GOALS = [
 ("goal.skill.prayer-43","43 Prayer","Prayer",43,"VERY_EARLY","Protection prayers transform questing and early combat safety.",["Protect from Melee","Protect from Missiles","Protect from Magic"],"Prayer",["prayer","combat","pvm"],5,True,"wiki-ironman"),
 ("goal.skill.prayer-70","70 Prayer / Piety","Prayer",70,"MID","Piety is a major melee damage and defence upgrade after its quest requirement.",["Piety prayer","Stronger melee PvM","Diary progress"],"Piety",["prayer","melee","pvm"],5,True,"wiki-ironman"),
 ("goal.skill.prayer-74","74 Prayer / Rigour readiness","Prayer",74,"LATE","This prepares the level requirement for Rigour once the optional raid scroll is obtained.",["Rigour level requirement","Late-game ranged prayer progression"],"Rigour",["prayer","ranged","raid","rng"],4,False,"wiki-gear"),
 ("goal.skill.prayer-77","77 Prayer / Augury readiness","Prayer",77,"LATE","This prepares the level requirement for Augury once the optional raid scroll is obtained.",["Augury level requirement","Late-game magic prayer progression"],"Augury",["prayer","magic","raid","rng"],4,False,"wiki-gear"),
 ("goal.skill.herblore-38","38 Herblore / Prayer potions","Herblore",38,"EARLY","Prayer potions establish the classic renewable Prayer supply line.",["Prayer potion production","Ranarr-to-Prayer supply loop"],"Prayer potion",["herblore","prayer-sustain","resources"],5,True,"wiki-herblore"),
 ("goal.skill.herblore-55","55 Herblore / Super strength","Herblore",55,"EARLY_MID","Super strength potions materially improve melee tasks and early bosses.",["Super strength potions","Better melee output"],"Super strength",["herblore","melee","potions"],4,False,"wiki-herblore"),
 ("goal.skill.herblore-63","63 Herblore / Super restores","Herblore",63,"MID","Super restores support harder PvM and use additional herb/secondary supplies.",["Super restore potions","Broader Prayer restoration"],"Super restore",["herblore","prayer-sustain","pvm"],5,True,"wiki-herblore"),
 ("goal.skill.herblore-70","70 Herblore","Herblore",70,"MID","This is a key grandmaster quest requirement and broadens useful potion access.",["Song of the Elves requirement","Improved potion roster"],"Herblore",["herblore","quest","potions"],4,True,"wiki-herblore"),
 ("goal.skill.herblore-72","72 Herblore / Ranging potions","Herblore",72,"MID","Ranging potions improve ranged bossing without relying on rare external supplies.",["Ranging potion production","Better ranged PvM"],"Ranging potion",["herblore","ranged","potions"],4,False,"wiki-herblore"),
 ("goal.skill.herblore-77","77 Herblore / Stamina potions","Herblore",77,"MID_LATE","Staminas improve questing, raids, runecrafting, clue travel, and POH upgrades.",["Stamina potion production","Efficient long-distance activities"],"Stamina potion",["herblore","run-energy","transport"],5,True,"wiki-herblore"),
 ("goal.skill.herblore-81","81 Herblore / Saradomin brews","Herblore",81,"LATE","Brews are a central high-level PvM food and combo-eating resource.",["Saradomin brew production","High-level PvM sustain"],"Saradomin brew",["herblore","food-sustain","pvm"],5,True,"wiki-herblore"),
 ("goal.skill.herblore-87","87 Herblore / Anti-venom","Herblore",87,"LATE","Anti-venom reduces reliance on repeated cures during venom-heavy content.",["Anti-venom production","Venom protection"],"Anti-venom",["herblore","pvm","venom"],4,False,"wiki-herblore"),
 ("goal.skill.herblore-90","90 Herblore / Super combat","Herblore",90,"LATE","One inventory slot can cover all three melee combat boosts.",["Super combat potions","Compact melee preparation"],"Super combat potion",["herblore","melee","pvm"],5,True,"wiki-herblore"),
 ("goal.skill.hunter-9","Natural History Quiz / 9 Hunter","Hunter",9,"VERY_EARLY","The quiz skips the slowest Hunter levels and starts the skill quickly.",["Level 9 Hunter","Faster access to bird houses"],"Natural History Quiz",["hunter","fossil-island"],3,False,"wiki-hunter"),
 ("goal.skill.hunter-46","46 Hunter / Rumours foundation","Hunter",46,"EARLY","This opens broader Hunter methods and the early Hunter Guild progression band.",["Expanded Hunter methods","Hunter Rumours preparation"],"Hunter",["hunter","varlamore","rumours"],3,False,"wiki-hunter"),
 ("goal.skill.hunter-50","50 Hunter / Eclectic implings","Hunter",50,"EARLY_MID","Eclectic implings are a popular renewable source of medium clues.",["Eclectic impling catches","Medium clue farming path"],"Eclectic impling",["hunter","clues","medium-clues"],4,True,"wiki-hunter"),
 ("goal.skill.hunter-67","67 Hunter / Black salamanders","Hunter",67,"MID","Black salamanders satisfy useful clue and ranged utility requirements, with Wilderness risk.",["Black salamander catches","Elite clue requirement coverage"],"Black salamander",["hunter","clues","wilderness"],3,False,"wiki-clues"),
 ("goal.skill.hunter-75","75 Hunter / Moonlight moths","Hunter",75,"MID","Moonlight moths provide inexpensive, renewable Prayer restoration before a deep potion bank.",["Moonlight moth catches","Moonlight moth mixes","Alternative Prayer sustain"],"Moonlight moth",["hunter","varlamore","moonlight-moths","prayer-sustain"],5,True,"wiki-hunter"),
 ("goal.skill.hunter-83","83 Hunter / Dragon implings","Hunter",83,"MID_LATE","Dragon implings support optional high-tier loot and elite clue acquisition.",["Dragon impling catches","Optional elite clue source"],"Dragon impling",["hunter","clues","optional","rng"],3,False,"wiki-hunter"),
 ("goal.skill.farming-32","32 Farming / Ranarrs","Farming",32,"EARLY","Growing ranarrs turns acquired seeds into the core Prayer-potion herb.",["Ranarr herb farming","Prayer potion supply line"],"Ranarr seed",["farming","ranarr","prayer-sustain","herb-runs"],5,True,"wiki-farming"),
 ("goal.unlock.farming-guild","45 Farming / Farming Guild","Farming",45,"EARLY","The first Guild tier begins a contract-driven seed loop.",["Farming Guild access","Easy Farming contracts"],"Farming Guild",["farming","contracts","seeds"],5,True,"wiki-farming"),
 ("goal.skill.farming-65","65 Farming / Medium contracts","Farming",65,"EARLY_MID","Medium contracts improve seed packs and sustain Herblore progression.",["Medium Farming contracts","Better seed packs"],"Farming contract",["farming","contracts","herblore","seeds"],5,True,"wiki-farming"),
 ("goal.skill.farming-85","85 Farming / Hard contracts","Farming",85,"MID_LATE","Hard contracts are a durable high-level source of herb and tree seeds.",["Hard Farming contracts","High-tier seed packs"],"Farming contract",["farming","contracts","herblore","seeds"],5,True,"wiki-farming"),
 ("goal.skill.farming-90","90 Farming / Spirit tree network","Farming",90,"LATE","Additional spirit trees expand a permanent transport network.",["Expanded spirit tree planting","Better transport coverage"],"Spirit tree",["farming","transport","spirit-tree"],4,False,"wiki-farming"),
 ("goal.skill.crafting-27","27 Crafting / Dueling rings","Crafting",27,"VERY_EARLY","Emerald jewellery provides cheap repeatable banking and minigame teleports.",["Ring of dueling crafting","Early teleport jewellery"],"Ring of dueling",["crafting","jewellery","teleport"],4,True,"wiki-crafting"),
 ("goal.skill.crafting-42","42 Crafting / Digsite pendants","Crafting",42,"EARLY","Ruby necklaces become Fossil Island and House-mounted transport tools after enchantment.",["Digsite pendant crafting","Fossil Island teleports"],"Digsite pendant",["crafting","jewellery","teleport","fossil-island"],4,False,"wiki-crafting"),
 ("goal.skill.crafting-70","70 Crafting / Diamond jewellery","Crafting",70,"EARLY_MID","Diamond jewellery adds powerful transport and combat utility.",["Diamond amulets","Power amulets","Useful teleport jewellery"],"Diamond amulet",["crafting","jewellery","teleport"],4,False,"wiki-crafting"),
 ("goal.skill.crafting-80","80 Crafting / Boosted glory","Crafting",80,"MID","A temporary boost can unlock self-made glories and an important POH transport option.",["Boosted amulet of glory crafting","Mounted glory path"],"Amulet of glory",["crafting","jewellery","teleport","boostable"],5,True,"wiki-crafting"),
 ("goal.skill.crafting-85","85 Crafting / Fury","Crafting",85,"MID_LATE","A boosted Fury is a strong all-round amulet before specialized Zenytes.",["Boosted amulet of fury crafting","All-style combat amulet"],"Amulet of fury",["crafting","jewellery","gear","boostable"],5,True,"wiki-crafting"),
 ("goal.skill.crafting-89","89 Crafting / Zenytes","Crafting",89,"LATE","A boost from 89 enables the full Zenyte jewellery progression when shards and onyxes are available.",["Boosted Zenyte crafting","Anguish, suffering, torture, and tormented bracelet paths"],"Zenyte jewellery",["crafting","jewellery","gear","boostable","rng"],5,True,"wiki-crafting"),
 ("goal.skill.construction-30","30 Construction / Early storage","Construction",30,"VERY_EARLY","Early POH storage reduces bank clutter and is especially useful for clue and costume items.",["Early costume-room storage","Basic POH utility"],"Costume room",["construction","poh","storage"],3,False,"wiki-construction"),
 ("goal.skill.construction-50","50 Construction / Portal chamber","Construction",50,"EARLY","A portal chamber begins a permanent, centralized teleport network.",["Portal chamber","Permanent spell teleports"],"Portal chamber",["construction","poh","teleport"],5,True,"wiki-construction"),
 ("goal.skill.construction-65","65 Construction / Superior garden","Construction",65,"EARLY_MID","The superior garden unlocks the room needed for pools and later transport fixtures.",["Superior garden","POH restoration progression"],"Superior garden",["construction","poh","restoration"],4,False,"wiki-construction"),
 ("goal.skill.construction-70","70 Construction / SOTE requirement","Construction",70,"MID","This clears the Construction requirement for Song of the Elves.",["Song of the Elves requirement","Stronger POH furniture"],"Construction",["construction","poh","quest"],4,True,"wiki-construction"),
 ("goal.account.strong-poh","83 Construction / Strong POH","Construction",83,"MID_LATE","With appropriate boosts and materials, 83 supports many high-level house upgrades.",["Boosted restoration pool path","Boosted jewellery box path","High-level POH hub"],"Player-owned house",["construction","poh","teleport","restoration","boostable"],5,True,"wiki-construction"),
 ("goal.skill.construction-90","90 Construction / Core maxed utilities","Construction",90,"LATE","Unboosted access to an ornate pool and occult altar makes repeat PvM preparation smoother.",["Ornate rejuvenation pool","Occult altar","Reliable PvM reset hub"],"Construction/Level_up_table",["construction","poh","restoration","spellbook"],5,True,"wiki-construction"),
 ("goal.skill.agility-60","60 Agility / Graceful foundation","Agility",60,"EARLY","Rooftop training toward this band usually builds marks and improves passive run restoration.",["Better run restoration","Useful shortcuts","Graceful progression"],"Agility",["agility","graceful","run-energy"],4,True,"wiki-agility"),
 ("goal.skill.agility-70","70 Agility / Key shortcuts","Agility",70,"MID","This level covers many practical shortcuts and common quest/diary requirements.",["Broader shortcut network","Quest and diary progress"],"Agility",["agility","shortcuts","transport"],4,True,"wiki-agility"),
 ("goal.skill.agility-72","72 Agility / Sepulchre floor 3","Agility",72,"MID","Floor 3 makes Hallowed Sepulchre a stronger training and reward option.",["Hallowed Sepulchre floor 3","Faster advanced Agility training"],"Hallowed Sepulchre",["agility","sepulchre","minigame"],4,False,"wiki-agility"),
 ("goal.skill.agility-92","92 Agility / Sepulchre floor 5","Agility",92,"LATE","The final floor enables the Grand Hallowed Coffin and ring-of-endurance grind.",["Hallowed Sepulchre floor 5","Grand Hallowed Coffin"],"Hallowed Sepulchre",["agility","sepulchre","rng","optional"],4,False,"wiki-agility"),
 ("goal.skill.thieving-38","38 Thieving / Master Farmers","Thieving",38,"EARLY","Master Farmers offer a direct herb-seed source, though low-level failure rates are high.",["Master Farmer pickpockets","Herb seed acquisition"],"Master Farmer",["thieving","seeds","herblore"],4,False,"wiki-farming"),
 ("goal.skill.thieving-50","50 Thieving / Wealthy citizens","Thieving",50,"EARLY","Varlamore wealthy citizens offer accessible Thieving, GP, and house-key utility.",["Wealthy citizen pickpockets","Varlamore house keys"],"Wealthy citizen",["thieving","varlamore","money"],4,True,"wiki-ironman"),
 ("goal.skill.thieving-82","82 Thieving / Vyres","Thieving",82,"MID_LATE","Vyres are an optional blood-shard and money grind after Darkmeyer access.",["Vyre pickpocketing","Optional blood shard path"],"Vyre",["thieving","darkmeyer","rng","optional"],3,False,"wiki-ironman"),
 ("goal.skill.thieving-85","85 Thieving / Elves","Thieving",85,"LATE","Elf pickpocketing is an optional source of enhanced crystal teleport seeds and GP.",["Elf pickpocketing","Optional crystal shard and GP loop"],"Elf (Prifddinas)",["thieving","prifddinas","money","rng","optional"],3,False,"wiki-ironman"),
 ("goal.skill.thieving-94","94 Thieving / Reliable Master Farmers","Thieving",94,"LATE","With the Hard Ardougne Diary this eliminates Master Farmer failures for a strong seed loop.",["100% Master Farmer success with diary","High-volume herb seeds"],"Master Farmer",["thieving","seeds","herblore","ardougne-diary"],4,False,"wiki-farming"),
 ("goal.skill.fishing-35","35 Fishing / Tempoross","Fishing",35,"VERY_EARLY","Tempoross combines Fishing with useful early supplies and optional utility uniques.",["Tempoross participation","Fish, planks, gems, and jewellery rewards"],"Tempoross",["fishing","minigame","food-sustain"],4,True,"wiki-fishing"),
 ("goal.skill.fishing-65","65 Fishing / Karambwans","Fishing",65,"EARLY_MID","Karambwans are a durable, AFK-friendly food supply once the quest and transport loop are ready.",["Raw karambwan fishing","Reliable combat food"],"Raw karambwan",["fishing","cooking","food-sustain","fairy-rings"],5,True,"wiki-fishing"),
 ("goal.skill.fishing-82","82 Fishing / Anglerfish","Fishing",82,"MID_LATE","Anglerfish offer stackable pre-fight overhealing utility after Piscarilius access.",["Anglerfish catches","Pre-fight overheal food"],"Anglerfish",["fishing","cooking","food-sustain","pvm"],4,False,"wiki-fishing"),
 ("goal.skill.fishing-85","85 Fishing / Dark crabs","Fishing",85,"LATE","Dark crabs are high-healing food with Wilderness exposure; they are optional for HCIM.",["Dark crab catches","High-healing food option"],"Dark crab",["fishing","food-sustain","wilderness","optional"],3,False,"wiki-fishing"),
 ("goal.skill.fishing-87","87 Fishing / Sacred eels","Fishing",87,"LATE","Sacred eels provide an AFK route to Zulrah scales after Regicide.",["Sacred eel fishing","Renewable Zulrah scales"],"Sacred eel",["fishing","zulrah","resources"],4,False,"wiki-fishing"),
 ("goal.skill.runecraft-27","27 Runecraft / Guardians of the Rift","Runecraft",27,"VERY_EARLY","Temple of the Eye leads directly into a rune-producing training loop.",["Guardians of the Rift","Mixed rune supply"],"Guardians of the Rift",["runecraft","gotr","runes","minigame"],5,True,"wiki-runecraft"),
 ("goal.skill.runecraft-44","44 Runecraft / Nature runes","Runecraft",44,"EARLY","Self-made nature runes support High Alchemy and utility spells.",["Nature rune crafting","Alchemy sustain"],"Nature rune",["runecraft","runes","money"],4,False,"wiki-runecraft"),
 ("goal.skill.runecraft-54","54 Runecraft / Law runes","Runecraft",54,"EARLY_MID","Law rune crafting supports a less shop-dependent teleport supply.",["Law rune crafting","Teleport rune sustain"],"Law rune",["runecraft","runes","teleport"],4,False,"wiki-runecraft"),
 ("goal.skill.runecraft-65","65 Runecraft / Death runes","Runecraft",65,"MID","Death runes support Iban's, burst spells, and later combat magic.",["Death rune crafting","Combat rune sustain"],"Death rune",["runecraft","runes","magic"],4,False,"wiki-runecraft"),
 ("goal.skill.runecraft-77","77 Runecraft / Blood runes","Runecraft",77,"MID_LATE","Blood runes become a long-term combat and charged-weapon resource.",["Blood rune crafting","High-level magic supply"],"Blood rune",["runecraft","runes","magic","resources"],5,True,"wiki-runecraft"),
 ("goal.skill.runecraft-95","95 Runecraft / Wrath runes","Runecraft",95,"ENDGAME","Wrath runes support surge spells and high-level offering spells.",["Wrath rune crafting","High-level spell sustain"],"Wrath rune",["runecraft","runes","magic"],3,False,"wiki-runecraft"),
 ("goal.skill.smithing-70","70 Smithing / Quest and diary foundation","Smithing",70,"MID","This clears Song of the Elves and supports Giant's Foundry and diary progress.",["Song of the Elves requirement","Broader smithing utility"],"Smithing",["smithing","quest","diary"],4,True,"wiki-ironman"),
 ("goal.skill.smithing-74","74 Smithing / Adamant darts","Smithing",74,"MID","Adamant dart tips provide a practical early blowpipe ammunition tier after Tourist Trap.",["Adamant dart tip smithing","Blowpipe ammo option"],"Adamant dart tip",["smithing","fletching","ammo"],4,False,"wiki-ironman"),
 ("goal.skill.mining-60","60 Mining / Mining Guild","Mining",60,"EARLY_MID","The Mining Guild improves access to coal, ores, and Amethyst progression later.",["Mining Guild access","Expanded ore training"],"Mining Guild",["mining","resources","guild"],4,False,"wiki-ironman"),
 ("goal.skill.mining-70","70 Mining / SOTE requirement","Mining",70,"MID","This clears the Mining requirement for Song of the Elves.",["Song of the Elves requirement","Adamantite mining"],"Mining",["mining","quest"],4,True,"wiki-ironman"),
 ("goal.skill.mining-92","92 Mining / Amethyst","Mining",92,"LATE","Amethyst supports sustainable high-tier arrows and darts with the matching Crafting/Fletching levels.",["Amethyst mining","High-tier ammunition supply"],"Amethyst",["mining","fletching","ammo","resources"],5,True,"wiki-ironman"),
 ("goal.skill.woodcutting-60","60 Woodcutting / Woodcutting Guild","Woodcutting",60,"EARLY","Guild access centralizes useful trees, axes, and a nearby sawmill.",["Woodcutting Guild","Convenient plank production"],"Woodcutting Guild",["woodcutting","construction","resources"],4,False,"wiki-construction"),
 ("goal.skill.woodcutting-70","70 Woodcutting / SOTE requirement","Woodcutting",70,"MID","This clears the Woodcutting requirement for Song of the Elves.",["Song of the Elves requirement","Stronger log access"],"Woodcutting",["woodcutting","quest"],4,True,"wiki-ironman"),
 ("goal.skill.woodcutting-90","90 Woodcutting / Redwood logs","Woodcutting",90,"LATE","Redwoods support AFK training, high-tier bird houses, and lantern fuel choices.",["Redwood logs","Redwood bird houses","High-tier log supply"],"Redwood tree",["woodcutting","hunter","resources"],4,False,"wiki-ironman"),
 ("goal.skill.fletching-55","55 Fletching / Broad bolts","Fletching",55,"EARLY_MID","After the Slayer unlock, broad bolts provide sustainable rune-crossbow ammunition.",["Broad bolt fletching","Sustainable ranged ammo"],"Broad bolts",["fletching","slayer","ammo","ranged"],5,True,"wiki-ironman"),
 ("goal.skill.fletching-69","69 Fletching / Rune crossbow","Fletching",69,"MID","A boost can allow self-fletching a rune crossbow when limbs are available.",["Boosted rune crossbow fletching","Ranged weapon independence"],"Rune crossbow",["fletching","ranged","gear","boostable"],4,False,"wiki-ironman"),
 ("goal.skill.fletching-90","90 Fletching / Amethyst darts","Fletching",90,"LATE","Combined with Mining and Crafting, this completes a renewable high-tier dart supply.",["Amethyst dart fletching","Sustainable blowpipe ammunition"],"Amethyst dart",["fletching","mining","crafting","ammo"],5,True,"wiki-ironman"),
 ("goal.skill.firemaking-50","50 Firemaking / Wintertodt","Firemaking",50,"VERY_EARLY","Wintertodt offers rapid Firemaking with scaling resource rewards; it is an option, not a mandatory start.",["Wintertodt access","Skilling reward cart"],"Wintertodt",["firemaking","minigame","resources","optional"],4,True,"wiki-ironman"),
 ("goal.skill.cooking-30","30 Cooking / Early food stability","Cooking",30,"VERY_EARLY","Reliable basic cooked food makes early quests and training less fragile.",["Broader cooked-food options","Early combat sustain"],"Cooking",["cooking","food-sustain"],3,False,"wiki-fishing"),
 ("goal.skill.cooking-70","70 Cooking / Recipe and diary foundation","Cooking",70,"MID","This covers many quest, food, and diary needs while reducing burn rates.",["Broader food preparation","Quest and diary progress"],"Cooking",["cooking","food-sustain","diary"],4,False,"wiki-fishing"),
 ("goal.skill.magic-55","55 Magic / High Alchemy","Magic",55,"EARLY","High Alchemy converts drops and crafted items into portable GP.",["High Level Alchemy","Portable GP conversion"],"High Level Alchemy",["magic","money","runes"],5,True,"wiki-ironman"),
 ("goal.skill.magic-78","78 Magic / Barrows portal","Magic",78,"MID","A POH Barrows portal makes repeated runs substantially smoother when Construction is ready.",["Barrows portal spell level","Faster Barrows access"],"Barrows Teleport",["magic","construction","poh","barrows"],4,False,"wiki-ironman"),
 ("goal.skill.attack-strength-130","130 Attack + Strength / Warriors' Guild","Attack",65,"EARLY","Combined Attack and Strength levels open the Warriors' Guild and defender progression.",["Warriors' Guild access","Defender grind"],"Warriors' Guild",["attack","strength","melee","defender"],5,True,"wiki-ironman"),
 ("goal.skill.ranged-61","61 Ranged / Rune crossbow band","Ranged",61,"EARLY_MID","This is the classic rune-crossbow equipment level for broad-bolt progression.",["Rune crossbow equipment","Midgame ranged baseline"],"Rune crossbow",["ranged","gear","ammo"],4,True,"wiki-ironman"),
 ("goal.skill.defence-70","70 Defence / Barrows band","Defence",70,"MID","This supports Barrows and several durable midgame armour options.",["Barrows armour equipment","Improved PvM durability"],"Defence",["defence","barrows","pvm"],4,False,"wiki-ironman"),
 ("goal.skill.hitpoints-70","70 Hitpoints / PvM durability","Hitpoints",70,"MID","A larger health pool makes learning midgame bosses and surviving mistakes more forgiving.",["Improved combat durability","Stronger midgame PvM baseline"],"Hitpoints",["hitpoints","combat","pvm"],4,False,"wiki-ironman"),
 ("goal.skill.sailing-1","Unlock Sailing","Sailing",1,"VERY_EARLY","Starting Sailing opens a modern progression branch with ports, tasks, upgrades, and ocean encounters.",["Sailing training","Courier and bounty tasks","Ocean exploration"],"Sailing",["sailing","transport","world-unlock"],4,True,"wiki-sailing"),
 ("goal.skill.sailing-30","30 Sailing / Tempor Tantrum","Sailing",30,"EARLY","The first Barracuda Trial provides a concrete ship-upgrade and skill checkpoint.",["Tempor Tantrum trial","Early ship facility progression"],"Barracuda Trials",["sailing","trial","ship-upgrade"],3,False,"wiki-sailing"),
 ("goal.skill.sailing-55","55 Sailing / Jubbly Jive","Sailing",55,"EARLY_MID","The second Barracuda Trial advances facilities and active Sailing progression.",["Jubbly Jive trial","Mid-tier ship progression"],"Barracuda Trials",["sailing","trial","ship-upgrade"],3,False,"wiki-sailing"),
 ("goal.skill.sailing-72","72 Sailing / Gwenith Glide","Sailing",72,"MID_LATE","The Gwenith trial is a higher-level Sailing checkpoint tied to advanced ship parts and access.",["Gwenith Glide trial","Advanced Sailing progression"],"Barracuda Trials",["sailing","trial","ship-upgrade"],4,False,"wiki-sailing")
]

for row in SKILL_GOALS:
    id,title,sk,lvl,stage,why,unlocks,wiki,tags,useful,pop,source = row
    risk = "WILDERNESS" if "wilderness" in tags else "SAFE"
    skill(id,title,sk,lvl,stage,why,unlocks,wiki,tags,usefulness=useful,popular=pop,
          risk=risk,sources=[source])

for goal in goals:
    if goal["id"] == "goal.skill.attack-strength-130":
        combined = {"type":"SKILL_SUM_AT_LEAST", "label":"Attack + Strength 130",
                    "skills":["Attack","Strength"], "level":130}
        goal["completion"] = combined
        goal["requirements"] = combined
        goal["relatedSkills"] = ["Attack","Strength"]
    elif goal["id"] == "goal.skill.sailing-1":
        goal["completion"] = quest_condition("Pandemonium")
        goal["requirements"] = quest_condition("Pandemonium")
        goal["relatedQuests"] = ["Pandemonium"]

# Quest and world unlocks. These are milestones, not a duplicate ordered route.
QUEST_GOALS = [
 ("goal.quest.druidic-ritual","Druidic Ritual","Druidic Ritual","VERY_EARLY","Starts Herblore, one of the defining resource skills for an Ironman.",["Herblore skill"],"Druidic Ritual",["herblore","world-unlock"],5,True),
 ("goal.quest.waterfall","Waterfall Quest","Waterfall Quest","VERY_EARLY","A large early Attack and Strength reward accelerates access to useful melee weapons.",["Early melee experience","Faster combat foundation"],"Waterfall Quest",["melee","combat"],4,True),
 ("goal.quest.priest-in-peril","Priest in Peril","Priest in Peril","VERY_EARLY","Opens Morytania and its quests, Slayer tower, Barrows, and later Darkmeyer path.",["Morytania access","Morytania quest line"],"Priest in Peril",["morytania","world-unlock"],5,True),
 ("goal.quest.lost-city","Lost City","Lost City","EARLY","Opens Zanaris, dragon weapon access, and the path toward fairy rings.",["Zanaris","Dragon weapon access","Fairy-ring quest line"],"Lost City",["transport","gear","zanaris"],5,True),
 ("goal.quest.tree-gnome-village","Tree Gnome Village","Tree Gnome Village","VERY_EARLY","Begins the Spirit Tree network and grants useful early Attack experience.",["Spirit Tree network","Early combat experience"],"Tree Gnome Village",["transport","spirit-tree"],5,True),
 ("goal.quest.grand-tree","The Grand Tree","The Grand Tree","EARLY","Expands gnome travel and provides substantial Agility, Attack, and Magic experience.",["Gnome glider network","Expanded Spirit Tree access"],"The Grand Tree",["transport","spirit-tree","glider"],5,True),
 ("goal.quest.fairytale-i","Fairytale I","Fairytale I - Growing Pains","EARLY","Magic secateurs improve herb and allotment yield and lead directly to fairy rings.",["Magic secateurs","Fairytale II access"],"Fairytale I - Growing Pains",["farming","herb-runs","transport"],5,True),
 ("goal.quest.ghosts-ahoy","Ghosts Ahoy","Ghosts Ahoy","EARLY","The Ectophial is a compact, repeatable Morytania teleport and safety escape.",["Ectophial","Port Phasmatys access"],"Ghosts Ahoy",["transport","ectophial","morytania"],5,True),
 ("goal.unlock.fossil-island","Fossil Island","Bone Voyage","EARLY","Bird houses, ammonite crabs, seaweed, fossils, and volcanic mine make this a major account hub.",["Bird house runs","Giant seaweed farming","Ammonite crabs","Fossil Island activities"],"Fossil Island",["transport","hunter","crafting","world-unlock"],5,True),
 ("goal.quest.children-of-the-sun","Children of the Sun","Children of the Sun","VERY_EARLY","Introduces Varlamore and starts several modern skilling and quest branches.",["Varlamore quest line","Access to regional progression"],"Children of the Sun",["varlamore","world-unlock"],5,True),
 ("goal.quest.twilights-promise","Twilight's Promise","Twilight's Promise","EARLY","Advances the Varlamore storyline and access around Civitas illa Fortis.",["Varlamore story progression","Regional activities"],"Twilight's Promise",["varlamore","world-unlock"],3,False),
 ("goal.quest.perilous-moons","Perilous Moons","Perilous Moons","EARLY_MID","Neypotzli is a self-supplied boss loop with three complementary midgame sets.",["Moons of Peril","Lunar Chest","Blood, Eclipse, and Blue Moon equipment"],"Perilous Moons",["varlamore","pvm","bossing","gear"],5,True),
 ("goal.quest.another-slice-ham","Another Slice of H.A.M.","Another Slice of H.A.M.","EARLY","The ancient mace special can support Prayer-point boosting strategies and clue access.",["Ancient mace","Dorgesh-Kaan progression"],"Another Slice of H.A.M.",["prayer-sustain","dorgesh-kaan","clues"],3,False),
 ("goal.quest.sea-slug","Sea Slug","Sea Slug","VERY_EARLY","The Fishing experience skips slow early levels on the way to useful food unlocks.",["Early Fishing experience","Faster food progression"],"Sea Slug",["fishing","food-sustain"],3,False),
 ("goal.quest.tai-bwo-trio","Tai Bwo Wannai Trio","Tai Bwo Wannai Trio","EARLY_MID","Completing the quest establishes the karambwan food and combo-eating path.",["Karambwan fishing path","Karambwan cooking","Karambwan vessel"],"Tai Bwo Wannai Trio",["fishing","cooking","food-sustain"],5,True),
 ("goal.quest.animal-magnetism","Animal Magnetism","Animal Magnetism","EARLY","Ava's devices reduce ammunition loss and form a lasting ranged cape progression.",["Ava's attractor","Ava's accumulator path"],"Animal Magnetism",["ranged","ammo","gear"],5,True),
 ("goal.quest.underground-pass","Underground Pass","Underground Pass","EARLY_MID","Iban's staff is a powerful early magic weapon for Barrows and quest bosses.",["Iban's staff","Tirannwn quest progression"],"Underground Pass",["magic","gear","barrows"],5,True),
 ("goal.quest.monkey-madness-i","Monkey Madness I","Monkey Madness I","EARLY_MID","Unlocks the dragon scimitar and advances major gnome and RFD dependencies.",["Dragon scimitar access","Monkey quest line"],"Monkey Madness I",["melee","gear"],5,True),
 ("goal.quest.fremennik-isles","The Fremennik Isles","The Fremennik Isles","EARLY_MID","The Helm of Neitiznot is a durable all-purpose melee helm and clue item.",["Helm of Neitiznot","Fremennik progression"],"The Fremennik Isles",["melee","gear","clues"],5,True),
 ("goal.quest.desert-treasure-i","Desert Treasure I","Desert Treasure I","MID","Ancient Magicks unlock burst/barrage Slayer and many PvM strategies.",["Ancient Magicks","Burst Slayer","Ancient staff access"],"Desert Treasure I",["magic","slayer","spellbook"],5,True),
 ("goal.quest.lunar-diplomacy","Lunar Diplomacy","Lunar Diplomacy","MID","Lunar spells improve skilling, farming, Runecraft pouch repair, and group utility.",["Lunar spellbook","NPC Contact","Superglass Make path"],"Lunar Diplomacy",["magic","spellbook","runecraft","crafting"],5,True),
 ("goal.quest.throne-miscellania","Throne of Miscellania","Throne of Miscellania","EARLY_MID","Starts a passive resource system once the account can maintain approval and coins.",["Managing Miscellania","Passive resource allocation"],"Throne of Miscellania",["resources","kingdom","money"],4,True),
 ("goal.account.kingdom","Royal Trouble / Full Kingdom","Royal Trouble","MID","Improved workers make Kingdom a meaningful passive herb, log, or coal source.",["Maximum Kingdom workers","Improved passive resources"],"Managing Miscellania",["resources","kingdom","herbs","construction"],5,True),
 ("goal.quest.troll-stronghold","Troll Stronghold","Troll Stronghold","EARLY","Opens Trollheim travel and the path toward a protected disease-free herb patch.",["Trollheim access","God Wars and herb-patch progression"],"Troll Stronghold",["transport","herb-runs","trollheim"],4,True),
 ("goal.quest.my-arms-big-adventure","My Arm's Big Adventure","My Arm's Big Adventure","EARLY_MID","Adds a disease-free herb patch after the relevant regional access.",["Troll Stronghold herb patch","Safer herb runs"],"My Arm's Big Adventure",["farming","herb-runs","resources"],5,True),
 ("goal.quest.making-friends","Making Friends with My Arm","Making Friends with My Arm","MID","Opens Weiss and another protected herb patch for high-value herb runs.",["Weiss access","Weiss herb patch","Salt mine"],"Making Friends with My Arm",["transport","farming","herb-runs"],5,True),
 ("goal.quest.taste-of-hope","A Taste of Hope","A Taste of Hope","MID","Drakan's medallion becomes a core Morytania transport tool and advances Darkmeyer access.",["Drakan's medallion","Morytania teleports"],"A Taste of Hope",["transport","darkmeyer","morytania"],5,True),
 ("goal.quest.sins-of-father","Sins of the Father","Sins of the Father","MID_LATE","Unlocks Darkmeyer, Sepulchre, vyres, and the late Myreque progression area.",["Darkmeyer","Hallowed Sepulchre","Vyre activities"],"Sins of the Father",["darkmeyer","sepulchre","world-unlock"],5,True),
 ("goal.quest.song-of-the-elves","Song of the Elves","Song of the Elves","MID_LATE","Prifddinas unlocks Gauntlet, Zalcano, crystal equipment, elves, and valuable skilling hubs.",["Prifddinas","Gauntlet and Corrupted Gauntlet","Crystal equipment","Zalcano"],"Song of the Elves",["prifddinas","gauntlet","world-unlock"],5,True),
 ("goal.quest.monkey-madness-ii","Monkey Madness II","Monkey Madness II","MID_LATE","Demonic gorillas begin the Zenyte branch and the seed pod adds a safety teleport.",["Demonic gorillas","Zenyte shard path","Royal seed pod"],"Monkey Madness II",["gear","zenyte","transport","pvm"],5,True),
 ("goal.quest.dragon-slayer-ii","Dragon Slayer II","Dragon Slayer II","MID_LATE","Unlocks Vorkath, Ava's assembler progression, and several high-value account systems.",["Vorkath","Ava's assembler path","Myths' Guild"],"Dragon Slayer II",["bossing","ranged","gear"],5,True),
 ("goal.quest.desert-treasure-ii","Desert Treasure II","Desert Treasure II - The Fallen Empire","LATE","Unlocks the Forgotten Four and the vestige/ancient sceptre upgrade branches.",["Vardorvis","Duke Sucellus","The Leviathan","The Whisperer"],"Desert Treasure II - The Fallen Empire",["bossing","pvm","gear"],5,True),
 ("goal.quest.while-guthix-sleeps","While Guthix Sleeps","While Guthix Sleeps","LATE","Unlocks tormented demons and their optional demonbane weapon branch.",["Tormented demons","Tormented synapse weapons","Burning claws path"],"While Guthix Sleeps",["bossing","demonbane","gear"],5,True),
 ("goal.quest.kingdom-divided","A Kingdom Divided","A Kingdom Divided","MID","Completes the major Kourend quest line and enables advanced Arceuus spells and Yama access.",["Advanced Arceuus spellbook","Yama access path"],"A Kingdom Divided",["kourend","spellbook","bossing"],5,True),
 ("goal.raid.toa-entry","Beneath Cursed Sands / ToA Entry","Beneath Cursed Sands","MID_LATE","Unlocks the configurable Tombs of Amascut and an accessible raid-learning path.",["Tombs of Amascut","Entry-mode raid learning","Keris partisan"],"Tombs of Amascut",["raid","toa","pvm"],5,True),
]

for row in QUEST_GOALS:
    id,title,name,stage,why,unlocks,wiki,tags,useful,pop = row
    quest(id,title,name,stage,why,unlocks,wiki,tags,usefulness=useful,popular=pop,
          sources=["wiki-ironman"], effort="LONG" if stage in ("MID_LATE","LATE") else "MEDIUM")

# Benefit-driven infrastructure and resource goals use manual confirmation where RuneLite has no durable signal.
INFRA_GOALS = [
 ("goal.transport.fairy-rings","Unlock Fairy Rings","Transportation","EARLY","Start Fairytale II far enough to use the fairy-ring network.","Fairy rings collapse travel time for quests, clues, Slayer, Farming, and karambwans.",["Global fairy-ring network","Fast access to remote activities"],"Fairytale II - Cure a Queen",["transport","fairy-rings","teleport"],5,True,["wiki-ironman"]),
 ("goal.transport.spirit-trees","Build the Spirit Tree Network","Transportation","EARLY","Complete the gnome quests and learn the permanent Spirit Tree links.","Spirit trees complement jewellery and fairy rings without consuming charges.",["Spirit Tree transport","Gnome Stronghold and Village links"],"Spirit tree",["transport","spirit-tree","teleport"],4,True,["wiki-ironman"]),
 ("goal.transport.ardougne-cloak","Ardougne Cloak Teleport","Transportation","VERY_EARLY","Complete the easy Ardougne tasks and claim the cloak.","The monastery teleport is one of the earliest unlimited teleports and sits near an altar and fairy ring.",["Unlimited monastery teleport","Early altar and fairy-ring access"],"Ardougne cloak 1",["transport","diary","teleport","prayer"],5,True,["wiki-diaries"]),
 ("goal.transport.quetzals","Establish the Quetzal Network","Transportation","EARLY_MID","Unlock and expand Varlamore quetzal destinations as they become available.","Quetzals turn Varlamore's spread-out activities into a practical repeatable circuit.",["Varlamore flight network","Faster Hunter, Farming, and quest travel"],"Quetzal Transport System",["transport","varlamore","quetzal"],4,True,["wiki-ironman"]),
 ("goal.transport.xerics-talisman","Charge a Xeric's Talisman","Transportation","EARLY","Obtain and charge the talisman from Kourend activities.","Its teleports simplify early Kourend quests, clues, herb runs, and Chambers access.",["Kourend teleport network","Faster Hosidius and Xeric travel"],"Xeric's talisman",["transport","kourend","teleport"],5,True,["wiki-ironman"]),
 ("goal.transport.poh-portals","Create a POH Portal Network","Account Infrastructure","EARLY_MID","Build and direct portals toward frequently repeated destinations.","Permanent portals trade initial runes and Construction for lasting route compression.",["Centralized spell teleports","Lower repeat travel friction"],"Portal chamber",["poh","construction","teleport"],5,True,["wiki-construction"]),
 ("goal.transport.mounted-glory","Mount an Amulet of Glory","Account Infrastructure","MID","Combine a quest hall, uncharged glory, and the required Construction level.","Unlimited glory destinations improve banking, clue travel, and early resource loops.",["Unlimited glory destinations from POH","Faster Edgeville and Karamja travel"],"Amulet of glory (mounted)",["poh","construction","crafting","teleport"],5,True,["wiki-construction","wiki-crafting"]),
 ("goal.transport.poh-fairy-ring","Build a POH Fairy Ring","Account Infrastructure","MID_LATE","Reach the Farming and Construction requirements, using boosts if desired.","A house fairy ring connects restoration, jewellery, portals, and the world network in one hub.",["POH fairy ring","Integrated transport hub"],"Fairy ring (Construction)",["poh","construction","farming","fairy-rings"],5,True,["wiki-construction"]),
 ("goal.transport.poh-spirit-tree","Build a POH Spirit Tree","Account Infrastructure","MID_LATE","Plant a Spirit Tree in the superior garden when Farming and Construction permit.","This connects the house directly to the Spirit Tree network.",["POH Spirit Tree","Integrated gnome transport"],"Spirit tree (Construction)",["poh","construction","farming","spirit-tree"],4,False,["wiki-construction"]),
 ("goal.transport.jewellery-box","Build a Useful Jewellery Box","Account Infrastructure","MID_LATE","Install the best jewellery box your current Construction and materials support.","Repeatable jewellery destinations reduce charge management and free bank space.",["Central jewellery teleports","Reduced charge upkeep"],"Jewellery box space",["poh","construction","jewellery","teleport"],5,True,["wiki-construction"]),
 ("goal.transport.occult-altar","Build an Occult Altar","Account Infrastructure","LATE","Acquire the altar components and Construction level, with a boost if appropriate.","Rapid spellbook switching supports Slayer, skilling, clues, and PvM preparation.",["POH spellbook switching","Ancient, Lunar, Arceuus, and standard access"],"Occult altar",["poh","construction","spellbook"],5,True,["wiki-construction"]),
 ("goal.resource.prayer-sustain","Establish Reliable Prayer Sustain","Resources","EARLY_MID","Choose a renewable mix of potions, ranarrs, contracts, moths, and other current methods.","Prayer restoration is a supply problem; the best solution depends on the account's nearby unlocks.",["Repeatable Prayer restoration","Less supply anxiety during quests and PvM"],"Ironman Guide/Prayer",["prayer","prayer-sustain","benefit-prayer","resources"],5,True,["wiki-herblore","wiki-hunter","wiki-farming"]),
 ("goal.resource.ranarr-loop","Establish a Ranarr Seed Loop","Resources","EARLY_MID","Use contracts, Slayer, Master Farmers, or PvM to replenish ranarr seeds.","Prayer potions become sustainable only when the herb and seed inputs are repeatable.",["Renewable ranarr seeds","Stable Prayer-potion production"],"Ironman Guide/Farming",["farming","ranarr","prayer-sustain","seeds"],5,True,["wiki-farming"]),
 ("goal.resource.herb-runs","Build a Repeatable Herb Run","Resources","EARLY","Unlock several patches, compost, teleports, and disease protection.","Short repeatable herb runs compound into Herblore levels and PvM supplies.",["Routine herb production","Herblore supply growth"],"Herb patch",["farming","herblore","herb-runs","resources"],5,True,["wiki-farming"]),
 ("goal.resource.food-karambwans","Bank a Karambwan Food Reserve","Resources","EARLY_MID","Set up karambwanji, fairy-ring travel, fishing, banking, and cooking.","Karambwans are reliable food and a valuable combo-eat for a wide range of combat.",["Repeatable combat food","Combo-eating supply"],"Cooked karambwan",["fishing","cooking","food-sustain","resources","bank-heavy"],5,True,["wiki-fishing"]),
 ("goal.resource.high-heal-food","Establish High-Healing Food","Resources","MID_LATE","Choose sharks, anglers, dark crabs, or other appropriate food for the account.","Harder bosses need a dependable food source rather than one lucky stack of drops.",["Repeatable high-healing food","Better boss preparation"],"Ironman Guide/Fishing",["fishing","cooking","food-sustain","pvm","bank-heavy"],5,True,["wiki-fishing"]),
 ("goal.resource.rune-sustain","Establish a Combat Rune Supply","Resources","EARLY_MID","Combine shops, GOTR, Runecraft, Slayer, and PvM drops for the runes you actually spend.","Sustainable runes keep teleports, alchemy, bursting, and powered staves available.",["Repeatable combat runes","Reliable utility spell casts"],"Ironman guide",["runecraft","runes","magic","resources","bank-heavy"],5,True,["wiki-runecraft","wiki-ironman"]),
 ("goal.resource.gp-early","Build an Early GP Engine","Resources","EARLY","Choose a progression-positive source such as Thieving, Agility Pyramid, shops, crafting, or alchables.","Coins fund runes, Construction, Kingdom, shops, and many quest requirements.",["Repeatable cash flow","Funding for account infrastructure"],"Ironman money making guide",["money","gp","thieving","agility"],5,True,["wiki-ironman"]),
 ("goal.resource.gp-mid","Build a Midgame Alch Pipeline","Resources","MID","Turn Slayer, skilling products, and PvM drops into a steady High Alchemy stack.","A portable alch pipeline funds Kingdom and expensive training without a dead-end cash grind.",["Sustainable alchables","Midgame cash flow"],"Ironman money making guide",["money","gp","alchemy","slayer"],4,True,["wiki-ironman"]),
 ("goal.resource.planks","Establish a Plank Supply","Resources","MID","Connect Kingdom or Woodcutting logs, a sawmill, coins, and a training method.","Construction progress is much smoother when logs, conversion, and cash are planned together.",["Repeatable plank production","Construction training readiness"],"Ironman Guide/Construction",["construction","woodcutting","planks","resources","bank-heavy"],5,True,["wiki-construction"]),
 ("goal.resource.giant-seaweed","Establish Giant Seaweed Runs","Resources","EARLY_MID","Unlock Fossil Island's underwater patches and collect spores while visiting.","Giant seaweed plus mined sand is the classic scalable Crafting supply line.",["Renewable giant seaweed","Scalable molten-glass training"],"Giant seaweed",["crafting","farming","glass","resources","bank-heavy"],5,True,["wiki-crafting"]),
 ("goal.resource.sand","Unlock Sandstone Mining","Resources","MID","Prepare waterskins or desert protection and use the grinder for bulk buckets of sand.","Bulk sand converts giant seaweed into predictable Crafting progress.",["Bulk buckets of sand","Molten-glass supply"],"Sandstorm",["crafting","mining","glass","resources","bank-heavy"],5,True,["wiki-crafting"]),
 ("goal.resource.ammo","Establish Sustainable Ranged Ammo","Resources","EARLY_MID","Link Slayer unlocks, Fletching, ore or shop sources to the weapon currently used.","Ranged upgrades are only useful when ammunition can be replaced reliably.",["Repeatable bolts or arrows","Reliable ranged training"],"Ironman Guide/Ranged",["ranged","ammo","fletching","resources","bank-heavy"],5,True,["wiki-ironman"]),
]

for row in INFRA_GOALS:
    id,title,category,stage,desc,why,unlocks,wiki,tags,useful,pop,sources = row
    add(id,title,category,stage,desc,why,unlocks,wiki,tags,usefulness=useful,popular=pop,
        sources=sources,skills=[s.title() for s in ("construction","crafting","farming","fishing","cooking","hunter","herblore","runecraft","magic","thieving","agility","woodcutting","mining") if s in tags])

# Every objective already understood by the gear roadmap is exposed as a selectable goal.
GEAR_GOALS = [
 ("gear.early.melee-weapon","Dragon Scimitar","EARLY_MID","A dependable early melee weapon after Monkey Madness I.",["Strong general slash weapon","Bridge toward zombie axe and whip"],["melee","weapon"],5,True,False),
 ("gear.early.defender","Dragon Defender","EARLY","A lasting offensive off-hand from the Warriors' Guild.",["Strong melee off-hand","Avernic defender progression"],["melee","defender"],5,True,False),
 ("gear.early.gloves","Barrows Gloves","EARLY_MID","One of the strongest broad early account milestones across all combat styles.",["All-style glove upgrade","Recipe for Disaster completion"],["melee","ranged","magic","quest"],5,True,False),
 ("gear.early.helm","Helm of Neitiznot","EARLY_MID","A useful melee and Prayer helm with a long upgrade path.",["Melee helm upgrade","Neitiznot faceguard path"],["melee","helm"],4,True,False),
 ("gear.early.strength-body","Fighter Torso","EARLY_MID","A deterministic strength body that remains relevant for a long time.",["Melee strength bonus","Barbarian Assault progression"],["melee","minigame"],5,True,False),
 ("gear.early.rune-crossbow","Rune Crossbow","EARLY_MID","A flexible shield-compatible ranged weapon with sustainable bolt options.",["Broad bolt progression","Enchanted bolt utility"],["ranged","weapon","ammo"],5,True,False),
 ("gear.early.sunlight-crossbow","Hunters' Sunlight Crossbow","EARLY_MID","A fast, modern ranged option tied to Varlamore Hunter ammunition.",["Sunlight antler bolt path","Fast ranged training option"],["ranged","hunter","varlamore"],4,False,False),
 ("gear.early.ava","Ava's Accumulator","EARLY","Ammo recovery is a major ranged quality-of-life upgrade.",["Improved ammunition recovery","Assembler progression"],["ranged","ammo","quest"],5,True,False),
 ("gear.early.magic-weapon","Iban's Staff (u)","EARLY_MID","Iban Blast is a strong pre-powered-staff spell for quests and Barrows.",["Iban Blast","Early Barrows magic"],["magic","weapon","barrows"],5,True,False),
 ("gear.early.god-cape","God Cape","EARLY_MID","A useful early magic cape that leads to the imbued version.",["Magic attack cape","Mage Arena II progression"],["magic","wilderness"],4,True,False),
 ("gear.early.fire-cape","Fire Cape","MID","A major first PvM execution milestone and lasting melee cape.",["Melee cape upgrade","Inferno access path"],["melee","pvm","fight-caves"],5,True,False),
 ("gear.mid.slayer-helm","Slayer Helmet (i)","EARLY_MID","Combines protective Slayer headgear and boosts multiple combat styles on task.",["Unified Slayer head protection","Imbued ranged and magic task bonuses"],["slayer","melee","ranged","magic"],5,True,False),
 ("gear.mid.zombie-axe","Zombie Axe","EARLY_MID","A powerful slash/crush bridge between dragon weapons and later drops.",["Strong slash and crush coverage","Midgame bossing weapon"],["melee","weapon","quest"],5,True,True),
 ("gear.mid.barrows-tank","Barrows Tank Armour","MID","Tank body and legs make Moons and several learning encounters more forgiving.",["High melee defence","Perilous Moons preparation"],["melee","barrows","tank"],4,True,True),
 ("gear.mid.blood-moon","Blood Moon Set","MID","A strength-focused set with valuable crush and hybrid applications.",["Blood Moon armour","Dual macuahuitl progression"],["melee","moons","varlamore"],4,False,True),
 ("gear.mid.eclipse-moon","Eclipse Moon Set and Atlatl","MID","A hybrid ranged set that opens an alternative midgame weapon branch.",["Eclipse atlatl","Hybrid ranged armour"],["ranged","moons","varlamore"],4,False,True),
 ("gear.mid.blue-moon","Blue Moon Set","MID","Ahrim-like magic offence with melee strength creates useful hybrid setups.",["Blue Moon spear and armour","Hybrid magic gear"],["magic","moons","varlamore"],4,False,True),
 ("gear.mid.twinflame","Royal Titans Rewards","MID","The staff and prayer scrolls form optional modern midgame upgrades.",["Twinflame staff path","Deadeye and Mystic Vigour paths"],["magic","prayer","bossing"],4,True,True),
 ("gear.mid.warped-sceptre","Warped Sceptre","MID","An accessible powered staff bridges Iban's and the Slayer trident.",["Powered magic attacks","Trident bridge"],["magic","slayer","weapon"],5,True,True),
 ("gear.mid.whip","Abyssal Whip","MID_LATE","The classic Slayer melee upgrade remains a strong general attack weapon.",["Fast accurate slash weapon","Tentacle progression"],["melee","slayer","weapon"],5,True,True),
 ("gear.mid.trident","Trident of the Seas","MID_LATE","A powered staff unlocks practical Zulrah and raid magic progression.",["Powered magic bossing","Toxic trident path"],["magic","slayer","weapon"],5,True,True),
 ("gear.mid.first-zenyte","First Zenyte Jewellery","MID_LATE","The first Zenyte should match the content the account plans to do next.",["Specialized high-level jewellery","Further Zenyte progression"],["crafting","zenyte","jewellery"],5,True,True),
 ("gear.mid.bowfa","Bowfa and Crystal Armour","LATE","This optional long grind creates a broad ranged PvM backbone.",["Bow of faerdhinen","Crystal armour","Strong ranged PvM branch"],["ranged","gauntlet","prifddinas","long-grind"],5,True,True),
 ("gear.late.assembler","Ava's Assembler","MID_LATE","Vorkath upgrades Ava's slot with stronger ranged bonuses and ammo retention.",["Ava's assembler","Improved ranged cape slot"],["ranged","vorkath","ammo"],5,True,True),
 ("gear.late.blowpipe","Toxic Blowpipe","LATE","A fast ranged weapon with a continuing scale and dart supply cost.",["Toxic blowpipe","Fast ranged DPS and healing special"],["ranged","zulrah","ammo","resources"],5,True,True),
 ("gear.late.scorching-bow","Scorching Bow","LATE","A demonbane ranged option for content where its specialization matters.",["Demonbane ranged weapon","Specialized demon PvM"],["ranged","demonbane","tormented-demons"],4,False,True),
 ("gear.late.fang","Osmumten's Fang","LATE","An optional raid unique with exceptional accuracy against durable targets.",["High-accuracy stab weapon","Broader boss and raid setups"],["melee","toa","raid"],5,True,True),
 ("gear.late.demonbane","Tormented Synapse Weapon","LATE","Choose a demonbane branch that solves the account's next content needs.",["Emberlight, scorching bow, or purging staff path","Specialized demon damage"],["demonbane","tormented-demons","weapon"],5,True,True),
 ("gear.late.strength-armour","Late Strength Armour","LATE","Bandos, Blood Moon, or another current path improves melee damage while respecting account choices.",["High-level melee strength armour","Stronger melee bossing"],["melee","gwd","armour"],5,True,True),
 ("gear.late.zenyte-set","Complete Zenyte Set","LATE","Completing specialized Zenytes rounds out melee, ranged, magic, and defensive jewellery.",["Anguish","Tormented bracelet","Torture","Suffering"],["zenyte","jewellery","all-styles"],5,True,True),
 ("gear.late.avernic","Avernic Defender","ENDGAME","The Theatre of Blood hilt upgrades the long-lived defender slot.",["Avernic defender","Endgame melee off-hand"],["melee","tob","raid"],4,False,True),
 ("gear.late.doom","Doom of Mokhaiotl Upgrade","LATE","An optional high-level Slayer/PvM branch with modern upgrade rewards.",["Doom of Mokhaiotl reward path","Advanced Slayer PvM"],["slayer","bossing","optional"],3,False,True),
 ("gear.late.oathplate","Oathplate Armour","LATE","Yama's optional armour path offers a modern late-game melee target.",["Oathplate armour","Late-game melee progression"],["melee","yama","bossing"],4,False,True),
 ("gear.endgame.infernal","Infernal Cape","ENDGAME","The Inferno is an execution milestone; preparation matters more than a fixed gear checklist.",["Infernal cape","Endgame wave-content mastery"],["melee","inferno","pvm"],5,True,False),
 ("gear.endgame.quiver","Dizana's Quiver","ENDGAME","The Fortis Colosseum tests movement, solves, supplies, and build adaptation.",["Dizana's quiver","Endgame ranged cape slot"],["ranged","colosseum","pvm","varlamore"],5,True,False),
 ("gear.endgame.masori","Fortified Masori Set","ENDGAME","An optional raid armour target that also depends on Armadyl components to fortify.",["Fortified Masori","Endgame ranged armour"],["ranged","toa","gwd","raid"],4,False,True),
 ("gear.endgame.ancestral","Ancestral Armour Set","ENDGAME","An optional Chambers armour target for endgame magic damage.",["Ancestral armour","Endgame magic armour"],["magic","cox","raid"],4,False,True),
 ("gear.endgame.twisted-bow","Twisted Bow","ENDGAME","A mega-rare Chambers goal; it is never assumed after any number of raids.",["Twisted bow","Endgame ranged weapon branch"],["ranged","cox","raid","mega-rare"],5,True,True),
 ("gear.endgame.shadow","Tumeken's Shadow","ENDGAME","A mega-rare ToA goal with major magic-gear synergy and no guaranteed timeline.",["Tumeken's shadow","Endgame magic weapon branch"],["magic","toa","raid","mega-rare"],5,True,True),
 ("gear.endgame.scythe","Scythe of Vitur","ENDGAME","A mega-rare Theatre goal with ongoing blood-rune and vial supply considerations.",["Scythe of vitur","Endgame melee weapon branch"],["melee","tob","raid","mega-rare","resource-cost"],5,True,True),
]

for row in GEAR_GOALS:
    id,title,stage,why,unlocks,tags,useful,pop,rng = row
    add(id,title,"Gear",stage,f"Pursue {title} through its current in-game source.",why,unlocks,
        title.replace("Rewards",""),tags + ["gear"],gear=id,usefulness=useful,popular=pop,rng=rng,
        risk="WILDERNESS" if "wilderness" in tags else "SAFE",sources=["wiki-gear","wiki-ironman"],
        skills=[])

SLAYER_GOALS = [
 ("goal.skill.slayer-55","55 Slayer / Broad Fletching",55,"EARLY_MID","Supports the broader-fletching unlock and sustainable broad ammunition.",["Broad bolts and arrows progression","Turoths"],["ammo","ranged"],5,True),
 ("goal.skill.slayer-58","58 Slayer / Cave Horrors",58,"EARLY_MID","Cave horrors begin the optional black-mask and Slayer-helmet branch.",["Black mask drop path","Slayer helmet progression"],["black-mask","gear","rng"],5,True),
 ("goal.skill.slayer-62","62 Slayer / Wyrms",62,"MID","Wyrms add useful alchables and an optional dragon-harpoon branch.",["Wyrm tasks","Optional dragon harpoon path"],["resources","fishing","rng"],3,False),
 ("goal.skill.slayer-65","65 Slayer / Dust Devils",65,"MID","Dust devils become an efficient burst-task and alchable source.",["Dust devil tasks","Burst Slayer progression"],["magic","burst","money"],5,True),
 ("goal.skill.slayer-70","70 Slayer / Kurasks",70,"MID","Kurasks provide herbs, alchables, and leaf-bladed weapon progression.",["Kurask tasks","Leaf-bladed equipment path"],["resources","melee"],4,False),
 ("goal.skill.slayer-72","72 Slayer / Skeletal Wyverns",72,"MID","Skeletal wyverns offer a resource-rich optional task and ranged gear drops.",["Skeletal wyvern tasks","Resource and ranged-drop path"],["resources","ranged","rng"],3,False),
 ("goal.skill.slayer-75","75 Slayer / Gargoyles",75,"MID","Gargoyles provide steady alchables and unlock Grotesque Guardians.",["Gargoyle tasks","Grotesque Guardians","Steady alchables"],["money","bossing"],4,True),
 ("goal.skill.slayer-80","80 Slayer / Nechryaels",80,"MID_LATE","Nechryaels are a valuable burst task with seeds, runes, and alchables.",["Nechryael tasks","Burst Slayer and resources"],["magic","burst","resources"],5,True),
 ("goal.skill.slayer-83","83 Slayer / Spiritual Mages",83,"MID_LATE","Spiritual mages provide the classic dragon-boots path.",["Dragon boots drop path","God Wars Slayer option"],["gear","gwd","rng"],4,False),
 ("goal.skill.slayer-85","85 Slayer / Abyssal Demons",85,"MID_LATE","Abyssal demons unlock the whip and Abyssal Sire branches.",["Abyssal whip path","Abyssal Sire"],["melee","gear","bossing","rng"],5,True),
 ("goal.skill.slayer-87","87 Slayer / Kraken",87,"MID_LATE","Cave krakens unlock the powered-staff transition and Kraken boss.",["Trident of the seas path","Kraken boss"],["magic","gear","bossing","rng"],5,True),
 ("goal.skill.slayer-91","91 Slayer / Cerberus",91,"LATE","Cerberus begins optional boot-crystal and smouldering-stone progression.",["Cerberus","Primordial, eternal, and pegasian crystal paths"],["bossing","gear","rng"],4,True),
 ("goal.skill.slayer-92","92 Slayer / Araxxor",92,"LATE","Araxxor opens the noxious halberd and amulet-of-rancour branches.",["Araxxor","Noxious halberd path","Araxyte fang path"],["bossing","melee","gear","rng"],5,True),
 ("goal.skill.slayer-93","93 Slayer / Smoke Devils",93,"LATE","Smoke devils unlock the Occult necklace drop path and Thermonuclear boss.",["Occult necklace path","Thermonuclear smoke devil"],["magic","gear","bossing","rng"],5,True),
 ("goal.skill.slayer-95","95 Slayer / Alchemical Hydra",95,"LATE","Hydra unlocks the dragon-hunter-lance path and valuable repeatable drops.",["Alchemical Hydra","Hydra claw and leather paths"],["bossing","gear","rng"],5,True),
]

for id,title,level,stage,why,unlocks,tags,useful,pop in SLAYER_GOALS:
    condition = skill_condition("Slayer",level)
    add(id,title,"Slayer",stage,f"Reach {level} Slayer for this current monster or boss unlock.",why,
        unlocks,"Slayer",tags + ["slayer","skill-unlock"],completion=condition,requirements=condition,
        skills=["Slayer"],usefulness=useful,popular=pop,rng="rng" in tags,
        effort="VERY_LONG" if level >= 85 else "LONG",sources=["wiki-slayer","youtube-slayer"])

DIARY_GOALS = [
 ("goal.diary.ardougne-easy","Ardougne Easy Diary","VERY_EARLY","The unlimited monastery teleport is exceptional early transport near an altar and fairy ring.",["Ardougne cloak 1","Monastery teleport","Early lamp"],["ardougne","teleport","prayer"],5,True,"SAFE"),
 ("goal.diary.ardougne-medium","Ardougne Medium Diary","EARLY","Farm teleports and improved local Thieving make routine runs easier.",["Ardougne farm teleports","Ardougne pickpocket bonus"],["ardougne","farming","thieving"],4,False,"SAFE"),
 ("goal.diary.ardougne-hard","Ardougne Hard Diary","MID_LATE","The global pickpocket bonus and five farm teleports strengthen seed and herb loops.",["Global pickpocket success bonus","Five farm teleports"],["ardougne","thieving","farming","seeds"],5,True,"SAFE"),
 ("goal.diary.ardougne-elite","Ardougne Elite Diary","ENDGAME","Unlimited farm teleports, extra marks, and automatic sand delivery reward broad completion.",["Unlimited farm teleports","Automatic sand delivery","More Ardougne marks"],["ardougne","farming","crafting","agility"],4,False,"SAFE"),
 ("goal.diary.lumbridge-hard","Lumbridge Hard Diary","MID","Unlimited cabbage-port access and improved Tears of Guthix are lasting conveniences.",["Explorer's ring 3","Unlimited cabbage teleports","Tears of Guthix bonus"],["lumbridge","teleport","run-energy"],4,False,"SAFE"),
 ("goal.diary.lumbridge-elite","Lumbridge Elite Diary","ENDGAME","Staffless fairy rings and another Slayer block slot are exceptional account-wide utility.",["Staffless fairy rings","Sixth Slayer block","Explorer's ring 4"],["lumbridge","fairy-rings","slayer","teleport"],5,True,"SAFE"),
 ("goal.diary.morytania-hard","Morytania Hard Diary","MID_LATE","Extra Barrows runes, Bonecrusher, fungi yield, and Burgh teleports stack multiple Ironman benefits.",["50% more Barrows runes","Bonecrusher","Double Mort myre fungi","Burgh de Rott teleports"],["morytania","barrows","prayer-sustain","herblore"],5,True,"SAFE"),
 ("goal.diary.fremennik-hard","Fremennik Hard Diary","MID","This tier improves travel and resource conveniences across Fremennik content.",["Fremennik diary teleports and benefits","Dagannoth and resource conveniences"],["fremennik","transport","bossing"],4,False,"SAFE"),
 ("goal.diary.fremennik-elite","Fremennik Elite Diary","LATE","Elite benefits improve noted DK bones and high-level Fremennik travel.",["Noted Dagannoth King bones","Elite Fremennik utilities"],["fremennik","dagannoth-kings","prayer"],4,False,"SAFE"),
 ("goal.diary.karamja-medium","Karamja Medium Diary","EARLY_MID","Gem-mine access and regional travel improve Crafting and karambwan loops.",["Karamja gloves 2","Gem-mine convenience"],["karamja","crafting","transport"],4,False,"SAFE"),
 ("goal.diary.karamja-hard","Karamja Hard Diary","MID","The underground gem-mine teleport is an excellent bank and karambwan deposit route.",["Karamja gloves 3","Unlimited gem-mine teleport"],["karamja","crafting","karambwans","teleport"],5,True,"SAFE"),
 ("goal.diary.karamja-elite","Karamja Elite Diary","ENDGAME","Duradel teleport and elite regional utilities support late Slayer.",["Duradel teleport","Karamja gloves 4"],["karamja","slayer","teleport"],4,False,"SAFE"),
 ("goal.diary.kandarin-hard","Kandarin Hard Diary","MID_LATE","Improved bolt special chance and regional teleports support ranged PvM and Farming.",["Enhanced enchanted-bolt effects","Kandarin diary teleports"],["kandarin","ranged","farming"],4,True,"SAFE"),
 ("goal.diary.western-hard","Western Provinces Hard Diary","MID_LATE","This tier enables elite void progression and the crystal halberd branch.",["Elite Void prerequisite","Crystal halberd access"],["western","void","gear"],5,True,"SAFE"),
 ("goal.diary.kourend-hard","Kourend & Kebos Hard Diary","MID_LATE","Rada's blessing, Slayer utility, and ash sanctification are broad regional upgrades.",["Rada's blessing 3","Ash sanctifier","Kourend utility"],["kourend","slayer","prayer","fishing"],5,True,"SAFE"),
 ("goal.diary.desert-hard","Desert Hard Diary","MID_LATE","Desert travel and regional benefits help Slayer, clue, and boss routes.",["Desert amulet 3","Improved desert transportation"],["desert","transport","slayer"],4,False,"SAFE"),
 ("goal.diary.desert-elite","Desert Elite Diary","ENDGAME","Unlimited Nardah restoration transport is a premier late-game reset option.",["Desert amulet 4","Unlimited Nardah teleport","Nardah statue access"],["desert","transport","restoration","pvm"],5,True,"SAFE"),
 ("goal.diary.varrock-medium","Varrock Medium Diary","EARLY_MID","Daily battlestaves can support Crafting and cash when the orb pipeline is worthwhile.",["Daily discounted battlestaves","Varrock armour 2"],["varrock","crafting","money"],4,False,"SAFE"),
 ("goal.diary.varrock-hard","Varrock Hard Diary","MID_LATE","More battlestaves and improved mining effects strengthen several resource loops.",["Expanded daily battlestaves","Varrock armour 3"],["varrock","crafting","mining","money"],4,False,"SAFE"),
 ("goal.diary.falador-hard","Falador Hard Diary","MID_LATE","This tier improves Mole and Prayer-related utility and advances diary completion.",["Falador shield 3","Giant Mole conveniences"],["falador","prayer","bossing"],4,False,"SAFE"),
 ("goal.diary.wilderness-hard","Wilderness Hard Diary","MID_LATE","Optional Wilderness benefits improve several risky resource and boss routes.",["Wilderness sword 3","Improved Wilderness activities"],["wilderness","bossing","resources","optional"],3,False,"WILDERNESS"),
]

for id,title,stage,why,unlocks,tags,useful,pop,risk in DIARY_GOALS:
    add(id,title,"Achievement Diaries",stage,f"Complete and claim the {title} rewards.",why,unlocks,
        title,"diary " .split() + tags,usefulness=useful,popular=pop,risk=risk,
        sources=["wiki-diaries"],activities=[title])

MINIGAME_GOALS = [
 ("goal.qol.graceful","Complete Graceful Outfit","EARLY","Marks from rooftops turn into an all-purpose questing and skilling set.",["Graceful outfit","Lower carried weight","Faster run-energy restoration"],"Graceful outfit",["agility","run-energy","qol"],5,True,False),
 ("goal.qol.rogue-outfit","Complete Rogue Outfit","EARLY","The full set doubles successful pickpocket loot and strengthens seed and GP methods.",["Double pickpocket loot","Better Master Farmer and money methods"],"Rogue equipment",["thieving","seeds","money","qol"],5,True,False),
 ("goal.minigame.wintertodt-foundation","Use Wintertodt as a Foundation","VERY_EARLY","Wintertodt can provide fast Firemaking and scaling supplies, but it is not mandatory.",["Firemaking progression","Optional early resource crates"],"Wintertodt",["firemaking","resources","optional"],4,True,False),
 ("goal.minigame.tempoross-foundation","Use Tempoross as a Food Foundation","EARLY","Tempoross mixes Fishing progress with fish, planks, jewellery, gems, and optional uniques.",["Fishing progression","Early food and material rewards"],"Tempoross",["fishing","food-sustain","resources"],5,True,False),
 ("goal.qol.fish-barrel","Obtain a Fish Barrel","EARLY_MID","The optional Tempoross unique doubles the useful raw-fish carrying capacity for many methods.",["Longer fishing trips","More AFK karambwan fishing"],"Fish barrel",["fishing","tempoross","qol","rng"],4,True,True),
 ("goal.qol.tackle-box","Obtain a Tackle Box","EARLY_MID","This optional storage reward consolidates fishing tools, especially for space-sensitive accounts.",["Fishing-tool storage","Reduced bank clutter"],"Tackle box",["fishing","tempoross","storage","rng"],3,False,True),
 ("goal.minigame.gotr-foundation","Build a Guardians of the Rift Foundation","EARLY","GOTR creates mixed runes while training Runecraft, Mining, and Crafting.",["Mixed rune bank","Runecraft progression","GOTR reward path"],"Guardians of the Rift",["runecraft","runes","resources"],5,True,False),
 ("goal.qol.raiments","Complete Raiments of the Eye","MID","The full outfit increases rune output, making later resource-focused Runecraft more valuable.",["60% more runes from full set","Improved rune sustainability"],"Raiments of the Eye",["runecraft","gotr","runes","rng"],5,True,True),
 ("goal.qol.colossal-pouch","Create the Colossal Pouch","MID","The abyssal needle consolidates pouches and scales capacity with Runecraft milestones.",["Consolidated essence storage","Higher Runecraft throughput"],"Colossal pouch",["runecraft","gotr","storage","rng"],5,True,True),
 ("goal.minigame.giants-foundry","Train at Giant's Foundry","EARLY_MID","Foundry converts metal items or bars into Smithing experience and cash efficiently.",["Smithing progression","Cash from commissions"],"Giants' Foundry",["smithing","money","resources"],5,True,False),
 ("goal.qol.coal-bag","Obtain a Coal Bag","EARLY_MID","The Motherlode reward improves Blast Furnace and coal-heavy Smithing trips.",["Extra coal storage","Faster Blast Furnace"],"Coal bag",["mining","smithing","storage","qol"],4,True,False),
 ("goal.qol.gem-bag","Obtain a Gem Bag","EARLY_MID","Gem storage improves gem mining, Slayer, and activities that drop uncut gems.",["Portable gem storage","Longer resource trips"],"Gem bag",["mining","crafting","storage","qol"],3,False,False),
 ("goal.minigame.mlm-upper","Unlock Upper Motherlode Mine","MID","The upper level creates a more predictable, lower-friction Motherlode loop.",["Upper-level veins","Improved Motherlode Mine training"],"Motherlode Mine",["mining","resources","qol"],4,True,False),
 ("goal.qol.plank-sack","Obtain a Plank Sack","EARLY_MID","Mahogany Homes points buy a large throughput upgrade for contracts.",["28-plank storage","Faster Mahogany Homes"],"Plank sack",["construction","mahogany-homes","storage","qol"],5,True,False),
 ("goal.qol.seed-box","Obtain a Seed Box","EARLY_MID","Seed storage helps Farming, Slayer, and especially inventory-limited account modes.",["Six seed-type storage","Cleaner seed collection"],"Seed box",["farming","tithe-farm","storage","qol"],4,True,False),
 ("goal.qol.herb-sack","Obtain a Herb Sack","EARLY_MID","Herb storage extends Slayer and gathering trips while protecting useful Herblore inputs.",["Portable grimy-herb storage","Longer Slayer trips"],"Herb sack",["herblore","slayer","storage","qol"],5,True,False),
 ("goal.qol.bottomless-bucket","Obtain Bottomless Compost Bucket","EARLY_MID","This optional Hespori drop doubles stored compost efficiency and simplifies runs.",["Bulk compost storage","Lower Farming-run friction"],"Bottomless compost bucket",["farming","hespori","qol","rng"],5,True,True),
 ("goal.minigame.void","Complete Void Knight Set","MID","Void provides compact all-style setups and is a prerequisite for elite void.",["Void melee, ranged, and magic sets","Elite Void progression"],"Void Knight equipment",["pest-control","gear","all-styles"],4,True,False),
 ("goal.minigame.elite-void","Complete Elite Void","MID_LATE","After Western Hard, elite void improves ranged and magic sets for selected encounters.",["Elite Void ranged and magic sets","Compact raid and boss setups"],"Elite Void Knight equipment",["pest-control","gear","western-diary"],4,True,False),
 ("goal.minigame.bones-peaches","Unlock Bones to Peaches","MID","The Mage Training Arena spell supports certain long trips and diary requirements.",["Bones to Peaches spell","Mage Training Arena progression"],"Bones to Peaches",["magic","mta","spellbook","diary"],3,False,False),
 ("goal.minigame.master-wand","Obtain a Master Wand","MID_LATE","This optional deterministic MTA grind supports later magic weapon upgrades.",["Master wand","Kodai wand upgrade path"],"Master wand",["magic","mta","gear","optional"],3,False,False),
 ("goal.minigame.hallowed-tools","Build a Sepulchre Tool Kit","MID_LATE","Hallowed equipment and supplies improve coffin looting and advanced Agility training.",["Improved Sepulchre runs","Hallowed reward access"],"Hallowed Sepulchre",["agility","sepulchre","qol"],4,False,False),
 ("goal.qol.rune-pouch","Obtain a Rune Pouch","EARLY_MID","Three or four rune slots compress combat, clue, skilling, and teleport inventories.",["Compact rune storage","Cleaner spellcasting loadouts"],"Rune pouch",["magic","storage","qol","slayer"],5,True,False),
 ("goal.qol.log-basket","Obtain a Log Basket","MID","Forestry storage improves Construction log collection and long Woodcutting trips.",["Portable log storage","Better plank-supply trips"],"Log basket",["woodcutting","forestry","construction","storage"],4,False,False),
 ("goal.qol.reagent-pouch","Obtain a Reagent Pouch","MID_LATE","Mastering Mixology storage reduces Herblore secondary clutter and improves processing.",["Secondary-ingredient storage","More compact Herblore preparation"],"Reagent pouch",["herblore","mastering-mixology","storage"],4,False,False),
]

for id,title,stage,why,unlocks,wiki,tags,useful,pop,rng in MINIGAME_GOALS:
    add(id,title,"Minigames",stage,f"Use the relevant minigame until {title.lower()} is complete.",why,
        unlocks,wiki,tags + ["minigame"],usefulness=useful,popular=pop,rng=rng,
        sources=["wiki-ironman"],activities=[wiki])

PVM_GOALS = [
 ("goal.pvm.scurrius","Learn Scurrius","EARLY","A forgiving repeatable boss for movement, prayer, and combat experience.",["Early boss mechanics practice","Rat bone weapon path"],"Scurrius",["bossing","combat-training"],4,True,"SAFE"),
 ("goal.pvm.barrows","Establish Consistent Barrows Runs","MID","Barrows teaches supply planning and offers runes plus optional armour branches.",["Repeatable Barrows clears","Rune and armour reward path"],"Barrows/Strategies",["bossing","barrows","magic","rng"],5,True,"SAFE"),
 ("goal.pvm.perilous-moons-loop","Establish a Perilous Moons Loop","MID","Dungeon-made supplies create an accessible three-style gear and resource loop.",["Repeatable Moons clears","Duplicate-protected set progression"],"Moons of Peril/Strategies",["bossing","moons","varlamore","gear","rng"],5,True,"SAFE"),
 ("goal.pvm.dagannoth-rex","Learn Dagannoth Rex","MID","Rex can be isolated and begins the optional berserker-ring branch.",["Dagannoth Rex clears","Berserker ring path","Dragon axe path"],"Dagannoth Rex/Strategies",["bossing","dagannoth-kings","melee","rng"],4,True,"DANGEROUS"),
 ("goal.pvm.sarachnis","Learn Sarachnis","EARLY_MID","An optional bridge boss for prayer switching, movement, clues, and a crush weapon.",["Sarachnis clears","Hard and elite clue source","Cudgel path"],"Sarachnis/Strategies",["bossing","clues","optional","rng"],3,False,"SAFE"),
 ("goal.pvm.royal-titans","Prepare for Royal Titans","MID","The encounter develops movement and style switching with modern midgame rewards.",["Consistent Royal Titans kills","Twinflame and prayer-scroll paths"],"Royal Titans/Strategies",["bossing","royal-titans","gear","rng"],4,True,"DANGEROUS"),
 ("goal.pvm.gauntlet","Learn the Gauntlet","MID_LATE","The Gauntlet removes bank-supply pressure and teaches movement, switches, and preparation.",["Consistent Gauntlet completions","Crystal shard and seed path"],"The Gauntlet/Strategies",["bossing","gauntlet","prifddinas"],5,True,"DANGEROUS"),
 ("goal.pvm.corrupted-gauntlet","Start Corrupted Gauntlet","LATE","Corrupted Gauntlet is the harder self-contained branch toward Bowfa and crystal armour.",["Corrupted Gauntlet completions","Enhanced and armour seed paths"],"The Gauntlet/Strategies",["bossing","gauntlet","bowfa","rng","long-grind"],5,True,"DANGEROUS"),
 ("goal.pvm.zulrah","Establish Consistent Zulrah Kills","LATE","Zulrah unlocks scales and optional ranged/magic gear while testing rotations and switches.",["Renewable Zulrah scales","Blowpipe, toxic trident, and serpentine paths"],"Zulrah/Strategies",["bossing","zulrah","gear","resources","rng"],5,True,"DANGEROUS"),
 ("goal.pvm.vorkath","Establish Consistent Vorkath Kills","MID_LATE","Vorkath supports assembler progression, Prayer experience, and steady resources.",["Vorkath head path","Dragon bones and resource drops"],"Vorkath/Strategies",["bossing","vorkath","ranged","resources"],5,True,"DANGEROUS"),
 ("goal.pvm.muspah","Learn Phantom Muspah","LATE","Muspah practices movement and prayer switches while offering ancient sceptre and shard paths.",["Phantom Muspah clears","Ancient sceptre path","Venator shard path"],"Phantom Muspah/Strategies",["bossing","muspah","magic","ranged","rng"],4,False,"DANGEROUS"),
 ("goal.pvm.god-wars-entry","Prepare for God Wars Dungeon","MID_LATE","God protection, kill counts, sustain, and escape planning are prerequisites to productive trips.",["Safe God Wars access","Preparation for four general branches"],"God Wars Dungeon",["bossing","gwd","preparation"],5,True,"DANGEROUS"),
 ("goal.pvm.graardor","Learn General Graardor","LATE","Graardor begins the optional Bandos armour and hilt branches.",["Graardor clears","Bandos armour and hilt paths"],"General Graardor/Strategies",["bossing","gwd","melee","rng"],4,True,"DANGEROUS"),
 ("goal.pvm.kril","Learn K'ril Tsutsaroth","MID_LATE","K'ril offers the Zamorakian spear path for hasta and dragon hunter lance progression.",["K'ril clears","Zamorakian spear path"],"K'ril Tsutsaroth/Strategies",["bossing","gwd","melee","rng"],5,True,"DANGEROUS"),
 ("goal.pvm.zilyana","Learn Commander Zilyana","LATE","Zilyana is an optional Armadyl crossbow and Saradomin hilt branch.",["Zilyana clears","Armadyl crossbow and hilt paths"],"Commander Zilyana/Strategies",["bossing","gwd","ranged","rng"],4,False,"DANGEROUS"),
 ("goal.pvm.kree","Learn Kree'arra","LATE","Kree supplies Armadyl components used directly or to fortify Masori.",["Kree'arra clears","Armadyl armour component path"],"Kree'arra/Strategies",["bossing","gwd","ranged","rng"],4,False,"DANGEROUS"),
 ("goal.pvm.tormented-demons","Farm Tormented Demons","LATE","After While Guthix Sleeps, hybrid demon fights lead to optional demonbane upgrades.",["Tormented synapse path","Burning claws path"],"Tormented Demon/Strategies",["bossing","demonbane","gear","rng"],5,True,"DANGEROUS"),
 ("goal.pvm.dt2-bosses","Learn the Forgotten Four","LATE","The post-quest DT2 bosses develop four different mechanical skill sets and gear branches.",["Vardorvis, Duke, Leviathan, and Whisperer clears","Vestige and axe-piece paths"],"Desert Treasure II - The Fallen Empire#The Forgotten Four",["bossing","dt2","gear","rng"],5,True,"DANGEROUS"),
 ("goal.pvm.yama","Prepare for Yama","ENDGAME","Yama is a demanding solo-or-duo encounter beyond the DT2 boss band.",["Yama clears","Oathplate and soulflame paths"],"Yama/Strategies",["bossing","yama","kourend","rng"],4,False,"DANGEROUS"),
 ("goal.raid.cox","Learn Chambers of Xeric","LATE","Chambers is a flexible raid where scouting, rooms, team roles, and supplies matter.",["Chambers completions","Prayer scroll, ancestral, and mega-rare paths"],"Chambers of Xeric/Strategies",["raid","cox","pvm","rng"],5,True,"DANGEROUS"),
 ("goal.raid.toa-normal","Build Consistent Normal ToA Runs","LATE","Configurable invocations let the account scale difficulty while learning every room.",["Normal-mode ToA clears","Fang, Lightbearer, Masori, and Shadow paths"],"Tombs of Amascut/Strategies",["raid","toa","pvm","rng"],5,True,"DANGEROUS"),
 ("goal.raid.toa-expert","Progress to Expert ToA","ENDGAME","Expert raids are a later execution and reward-rate milestone, not an early requirement.",["Expert-mode ToA clears","Improved high-level raid proficiency"],"Tombs of Amascut/Strategies",["raid","toa","pvm","rng"],4,False,"DANGEROUS"),
 ("goal.raid.tob-entry","Complete Theatre of Blood Entry Mode","LATE","Entry mode teaches room order and mechanics and supports quest completion.",["Entry-mode Theatre completion","Theatre room familiarity"],"Theatre of Blood/Entry Mode",["raid","tob","pvm"],4,True,"DANGEROUS"),
 ("goal.raid.tob-normal","Learn Normal Theatre of Blood","ENDGAME","Normal Theatre requires role knowledge, execution, and dependable team preparation.",["Normal Theatre clears","Avernic, armour, and Scythe paths"],"Theatre of Blood/Strategies",["raid","tob","pvm","rng"],5,True,"DANGEROUS"),
 ("goal.pvm.nex","Prepare for Nex","ENDGAME","Nex requires team-aware mechanics, supply planning, and a suitable ranged setup.",["Nex kill participation","Torva and Zaryte paths"],"Nex/Strategies",["bossing","nex","ranged","rng"],5,True,"DANGEROUS"),
 ("goal.pvm.inferno-prep","Build an Inferno Setup","ENDGAME","Separate gear, supply, and wave-practice preparation from the cape RNG-free execution goal.",["Sustainable Inferno attempts","Wave and solve practice"],"Inferno/Strategies",["pvm","inferno","preparation","prayer-sustain"],5,True,"DANGEROUS"),
 ("goal.pvm.colosseum-prep","Build a Colosseum Setup","ENDGAME","Plan gear, supplies, modifiers, and wave solves before pursuing Sol Heredit.",["Repeatable Colosseum attempts","Dizana's quiver preparation"],"Fortis Colosseum/Strategies",["pvm","colosseum","varlamore","preparation"],5,True,"DANGEROUS"),
]

for id,title,stage,why,unlocks,wiki,tags,useful,pop,risk in PVM_GOALS:
    add(id,title,"Raids" if "raid" in tags else "Bossing",stage,
        f"Prepare for and complete {title.lower()} without assuming any unique-drop timeline.",why,
        unlocks,wiki,tags + ["pvm"],usefulness=useful,popular=pop,rng="rng" in tags,
        risk=risk,sources=["wiki-ironman","wiki-gear","reddit-goals"],activities=[wiki])

CLUE_GOALS = [
 ("goal.clue.easy-foundation","Build an Easy Clue Foundation","EARLY","Early teleports, quest access, and simple STASH units increase completion rate.",["More completable easy clues","Early god-page and cosmetic paths"],"Clue scroll (easy)",["easy-clues","teleport"],3,False,False),
 ("goal.clue.eclectic-loop","Build an Eclectic Impling Loop","EARLY_MID","Hunter and Puro-Puro tools create a repeatable medium-clue source.",["Repeatable medium clues","Optional ranger-boots path"],"Eclectic impling",["medium-clues","hunter","implings"],4,True,False),
 ("goal.clue.ranger-boots","Pursue Ranger Boots (Optional)","MID","Ranger boots are a valid optional RNG goal, never a required progression checkpoint.",["Ranger boots drop path","Pegasian boots upgrade path"],"Ranger boots",["medium-clues","ranged","optional","rng"],3,True,True),
 ("goal.clue.hard-ready","Become Hard-Clue Ready","MID","Quests, teleports, combat, and common emote items let more hard clues reach caskets.",["Higher hard-clue completion rate","Access to broad hard-clue rewards"],"Clue scroll (hard)",["hard-clues","quests","teleport"],4,True,False),
 ("goal.clue.hard-ranged-gear","Pursue Hard-Clue Ranged Gear (Optional)","MID","Black or blessed dragonhide and a magic shortbow can arrive from hard clues, but none is guaranteed.",["Black dragonhide path","Blessed dragonhide path","Magic shortbow path"],"Reward casket (hard)",["hard-clues","ranged","gear","optional","rng"],4,True,True),
 ("goal.clue.elite-ready","Become Elite-Clue Ready","LATE","Broader skills, quests, and difficult emote items prevent avoidable elite-clue drops.",["Higher elite-clue completion rate","Elite casket path"],"Clue scroll (elite)",["elite-clues","skills","quests"],3,False,False),
 ("goal.clue.master-ready","Become Master-Clue Ready","ENDGAME","Master clues test broad account completion and many optional item branches.",["Higher master-clue completion rate","Master casket path"],"Clue scroll (master)",["master-clues","skills","quests","collection"],3,False,False),
 ("goal.clue.stash-medium","Build Useful Medium STASH Units","EARLY_MID","Storing recurring emote items reduces clue time and bank clutter.",["Faster medium clues","Stored emote outfits"],"STASH",["medium-clues","construction","storage"],4,False,False),
 ("goal.clue.stash-hard","Build Useful Hard STASH Units","MID_LATE","High-frequency hard emote steps become faster and less bank-intensive.",["Faster hard clues","Reduced clue-item bank pressure"],"STASH",["hard-clues","construction","storage"],4,False,False),
 ("goal.clue.teleport-kit","Build a Clue Teleport Kit","MID","Jewellery, fairy rings, spellbooks, diary items, and scroll storage reduce clue travel.",["Faster clue routing","Reusable teleport coverage"],"Treasure Trails/Full guide/All",["clues","teleport","jewellery","qol"],5,True,False),
]

for id,title,stage,why,unlocks,wiki,tags,useful,pop,rng in CLUE_GOALS:
    add(id,title,"Clue Scrolls",stage,f"Treat {title.lower()} as an optional account-completion branch.",why,
        unlocks,wiki,tags + ["clues","optional"],usefulness=useful,popular=pop,rng=rng,
        sources=["wiki-clues","reddit-goals"],activities=["Treasure Trails"])

# Small account-mode overlays reuse the same catalog and scoring system.
add("goal.account.hcim-safe-boss-ladder","Build a Low-Risk HCIM Boss Ladder","Account Infrastructure","MID",
    "Choose staged encounters and escape plans before adding dangerous or Wilderness content.",
    "Hardcore progression benefits from explicitly valuing survival and practice over nominal efficiency.",
    ["Safer mechanical practice","Risk-aware boss progression"],"Ironman Mode",
    ["hcim","bossing","risk","qol"],accounts=["HARDCORE_IRONMAN","HARDCORE_GROUP_IRONMAN"],
    usefulness=5,popular=False,sources=["wiki-ironman"])
add("goal.account.uim-looting-bag","Set Up UIM Looting-Bag Storage","Account Infrastructure","EARLY",
    "Acquire and understand a looting bag only where the account's death and Wilderness plan makes it appropriate.",
    "The bag is a major UIM storage tool, but using it safely requires account-specific item-management knowledge.",
    ["Expanded carried storage","UIM item-management foundation"],"Ultimate Ironman Guide/Item Management",
    ["uim","storage","wilderness","qol"],accounts=["ULTIMATE_IRONMAN"],risk="WILDERNESS",
    usefulness=5,popular=True,sources=["wiki-ironman"])
add("goal.account.uim-poh-storage","Build UIM POH Storage","Account Infrastructure","EARLY_MID",
    "Prioritize costume room, STASH, and storable activity items that fit the account's current plan.",
    "Permanent storage changes which grinds an Ultimate Ironman can keep without sacrificing the inventory.",
    ["Costume-room storage","Clue and outfit storage","Lower inventory pressure"],"Ultimate Ironman Guide/Item Management",
    ["uim","poh","construction","storage","qol"],accounts=["ULTIMATE_IRONMAN"],
    usefulness=5,popular=True,sources=["wiki-construction"])

# Current systems whose value is broader than a bare skill level.
add("goal.unlock.piety","Unlock Piety","Account Infrastructure","MID",
    "Complete King's Ransom and the Knight Waves Training Grounds after reaching 70 Prayer and 70 Defence.",
    "Piety is a major melee damage, accuracy, and defence upgrade; merely reaching 70 Prayer does not unlock it.",
    ["Piety prayer","Stronger melee bossing and Slayer"],"Piety",
    ["prayer","melee","combat","quest-unlock"],
    completion=manual("Complete the Knight Waves Training Grounds"),
    requirements=all_condition("Piety requirements", skill_condition("Prayer",70),
        skill_condition("Defence",70), quest_condition("King's Ransom")),
    skills=["Prayer","Defence"],quests=["King's Ransom"],usefulness=5,popular=True,
    sources=["wiki-knight-waves","reddit-modern"],completion_mode="MANUAL",
    priority="CORE",community="VERY_COMMON",intents=["MELEE_POWER"])

add("goal.activity.hunter-rumours","Establish Hunter Rumours","Resources","EARLY",
    "Reach 46 Hunter, speak to Verity, and complete a first Hunter's Rumour.",
    "Rumours turn Hunter into a repeatable source of herbs, logs, meats, nests, bones, and later antelope supplies.",
    ["Repeatable Hunter rumours","Hunter loot sacks","Rumour milestone path"],"Hunters' Rumours",
    ["hunter","rumours","resources","varlamore"],completion=manual("Complete a Hunter's Rumour"),
    requirements=all_condition("Hunter Guild access",skill_condition("Hunter",46),
        quest_condition("Children of the Sun")),skills=["Hunter"],quests=["Children of the Sun"],
    activities=["Hunters' Rumours"],
    usefulness=5,popular=True,sources=["wiki-rumours","wiki-hunter","reddit-2026-modern"],
    completion_mode="MANUAL",priority="CORE",community="VERY_COMMON",
    intents=["FOOD_SUSTAIN","HERB_SUPPLY","AMMO_SUPPLY"])

add("goal.activity.hunter-rumours-50","Complete 50 Hunter Rumours","Resources","MID",
    "Complete 50 rumours and claim the milestone benefits before treating further counts as optional collection progress.",
    "Fifty rumours unlock the ability to cook sunlight and moonlight antelope meat and represents a useful stopping point.",
    ["50-rumour milestone","Antelope cooking permission"],"Hunters' Rumours",
    ["hunter","rumours","food-sustain","milestone"],completion=manual("Complete 50 Hunter's Rumours"),
    requirements=all_condition("Hunter Guild access",skill_condition("Hunter",46),
        quest_condition("Children of the Sun")),dependencies=["goal.activity.hunter-rumours"],skills=["Hunter"],
    quests=["Children of the Sun"],
    activities=["Hunters' Rumours"],usefulness=4,popular=True,sources=["wiki-rumours","wiki-hunter"],
    completion_mode="MANUAL",priority="RECOMMENDED",community="COMMON",intents=["FOOD_SUSTAIN"])

skill("goal.skill.hunter-72","72 Hunter / Sunlight antelopes","Hunter",72,"MID",
    "Sunlight antelopes support high-healing food and a renewable ammunition path for the Hunters' sunlight crossbow.",
    ["Sunlight antelope hunting","Sunlight antler bolt supply","High-healing antelope meat"],
    "Sunlight antelope",["hunter","antelope","food-sustain","ammo","varlamore"],usefulness=5,
    popular=True,sources=["wiki-hunter","wiki-rumours"],intents=["FOOD_SUSTAIN","AMMO_SUPPLY"])

skill("goal.skill.hunter-91","91 Hunter / Moonlight antelopes","Hunter",91,"LATE",
    "Moonlight antelopes provide high-healing food and high-tier antler ammunition; master rumours remain a separate unlock.",
    ["Moonlight antelope hunting","Moonlight antler bolt supply","High-healing antelope meat"],
    "Moonlight antelope",["hunter","antelope","food-sustain","ammo","varlamore"],usefulness=4,
    popular=False,sources=["wiki-hunter","wiki-rumours"],intents=["FOOD_SUSTAIN","AMMO_SUPPLY"])

add("goal.activity.vale-totems","Unlock and Try Vale Totems","Resources","EARLY",
    "Complete the Vale Totems miniquest at 20 Fletching and use the activity before choosing how far to train there.",
    "Vale Totems is an active Fletching alternative that returns nests, roots, arrowtips, logs, flax, and ent branches.",
    ["Vale Totems activity","Resource-producing Fletching method"],"Vale Totems",
    ["fletching","vale-totems","resources","varlamore"],completion=quest_condition("Vale Totems"),
    requirements=all_condition("Vale Totems access",skill_condition("Fletching",20),
        quest_condition("Children of the Sun")),skills=["Fletching"],
    quests=["Children of the Sun","Vale Totems"],
    activities=["Vale Totems"],usefulness=4,popular=True,sources=["wiki-vale-totems","reddit-vale"],
    priority="RECOMMENDED",community="COMMON",intents=["AMMO_SUPPLY"])

skill("goal.skill.sailing-15","15 Sailing / Salvaging","Sailing",15,"VERY_EARLY",
    "Salvaging starts a useful material and ship-progression loop shortly after Sailing is unlocked.",
    ["Salvaging","Early ship and material progression"],"Salvaging",
    ["sailing","salvaging","resources"],usefulness=4,popular=True,
    sources=["wiki-sailing","reddit-sailing"],intents=["ACCOUNT_INFRASTRUCTURE"])

skill("goal.skill.sailing-62","62 Sailing / Wyrmscraig","Sailing",62,"MID",
    "Wyrmscraig access connects Sailing to Fallen From Grace, Golem Crafting, and new resource loops.",
    ["Wyrmscraig access","Fallen From Grace path","Golem Crafting region"],"Wyrmscraig",
    ["sailing","wyrmscraig","crafting","world-unlock"],usefulness=5,popular=True,
    sources=["wiki-sailing","reddit-sailing"],intents=["ACCOUNT_INFRASTRUCTURE","CRAFTING_SUPPLY"])

add("goal.activity.golem-crafting","Establish Golem Crafting","Resources","MID",
    "Unlock Wyrmscraig and Fallen From Grace, then test the sunstone-and-fur Golem Crafting loop at 60 Crafting.",
    "Golem Crafting is a modern alternative to glass that turns Hunter fur and sunstones into banked Crafting progress.",
    ["Golem Crafting","Alternative Crafting training","Useful Hunter-fur sink"],"Golem Crafting",
    ["crafting","golem-crafting","wyrmscraig","hunter","alternative"],
    completion=manual("Create a golem through Golem Crafting"),
    requirements=all_condition("Golem Crafting requirements",skill_condition("Crafting",60),
        skill_condition("Sailing",62),skill_condition("Mining",53),skill_condition("Runecraft",47),
        quest_condition("Pandemonium"),quest_condition("Fallen From Grace")),
    dependencies=["goal.skill.sailing-62"],skills=["Crafting","Sailing","Mining","Runecraft"],
    quests=["Pandemonium","Fallen From Grace"],
    activities=["Golem Crafting"],usefulness=4,popular=True,sources=["community-golem","reddit-sailing"],
    completion_mode="MANUAL",priority="RECOMMENDED",community="COMMON",intents=["CRAFTING_SUPPLY"])

add("goal.activity.sailing-salvaging-station","Build a Salvaging Station","Resources","EARLY",
    "At 42 Sailing and 34 Construction, recover the schematic and install a salvaging station on the boat.",
    "Sorting salvage at sea reduces return trips and makes resource-focused salvaging substantially smoother.",
    ["Sort salvage at sea","Longer shipwreck-salvaging trips"],"Shipwreck salvaging",
    ["sailing","salvaging","construction","resources","ship-upgrade"],
    completion=manual("Install and use a salvaging station"),
    requirements=all_condition("Salvaging station requirements",skill_condition("Sailing",42),
        skill_condition("Construction",34),quest_condition("Pandemonium"),
        manual("Read the Salvaging Station schematic")),
    dependencies=["goal.skill.sailing-15"],skills=["Sailing","Construction"],quests=["Pandemonium"],
    activities=["Shipwreck salvaging"],usefulness=5,popular=True,
    sources=["wiki-salvaging-station","wiki-sailing","reddit-sailing"],
    completion_mode="MANUAL",priority="CORE",community="COMMON",
    intents=["ACCOUNT_INFRASTRUCTURE","CRAFTING_SUPPLY"])

add("goal.activity.sailing-trawling","Establish Deep Sea Trawling","Resources","MID",
    "At 65 Sailing and 61 Construction, build a linen trawling net and establish a renewable ocean-food loop.",
    "Deep Sea Trawling links Sailing with useful fish, Cooking, and later Herblore ingredients without becoming mandatory.",
    ["Mid-depth Deep Sea Trawling","Ocean food and fish-resource path"],"Deep Sea Trawling",
    ["sailing","trawling","fishing","construction","food-sustain","optional"],
    completion=manual("Build a linen trawling net and complete a trawling trip"),
    requirements=all_condition("Linen trawling requirements",skill_condition("Sailing",65),
        skill_condition("Construction",61),quest_condition("Pandemonium")),
    dependencies=["goal.skill.sailing-15"],skills=["Sailing","Construction","Fishing"],quests=["Pandemonium"],
    activities=["Deep Sea Trawling"],usefulness=4,popular=False,
    sources=["wiki-sailing-levels","wiki-sailing","reddit-sailing"],
    completion_mode="MANUAL",priority="OPTIONAL",community="NOTABLE",
    intents=["FOOD_SUSTAIN","ACCOUNT_INFRASTRUCTURE"])

# Dependency chains make major goals decompose through the existing GoalPlannerService.
DEPENDENCIES = {
    "goal.transport.fairy-rings":["goal.quest.lost-city","goal.quest.fairytale-i"],
    "goal.transport.mounted-glory":["goal.skill.crafting-80","goal.skill.construction-50"],
    "goal.transport.poh-fairy-ring":["goal.account.strong-poh","goal.transport.fairy-rings"],
    "goal.transport.occult-altar":["goal.account.strong-poh","goal.quest.desert-treasure-i","goal.quest.lunar-diplomacy"],
    "goal.resource.ranarr-loop":["goal.skill.farming-32"],
    "goal.resource.prayer-sustain":["goal.skill.prayer-43","goal.skill.herblore-38","goal.skill.farming-32"],
    "goal.resource.food-karambwans":["goal.quest.tai-bwo-trio","goal.transport.fairy-rings","goal.skill.fishing-65"],
    "goal.resource.giant-seaweed":["goal.unlock.fossil-island"],
    "goal.pvm.perilous-moons-loop":["goal.quest.perilous-moons","gear.mid.barrows-tank"],
    "goal.pvm.gauntlet":["goal.quest.song-of-the-elves"],
    "goal.pvm.corrupted-gauntlet":["goal.pvm.gauntlet"],
    "gear.mid.bowfa":["goal.pvm.corrupted-gauntlet"],
    "gear.mid.trident":["goal.skill.slayer-87"],
    "gear.mid.whip":["goal.skill.slayer-85"],
    "gear.late.assembler":["goal.quest.dragon-slayer-ii","goal.pvm.vorkath"],
    "gear.late.demonbane":["goal.quest.while-guthix-sleeps","goal.pvm.tormented-demons"],
    "goal.raid.toa-normal":["goal.raid.toa-entry"],
    "goal.raid.toa-expert":["goal.raid.toa-normal"],
    "goal.raid.tob-normal":["goal.raid.tob-entry"],
    "gear.endgame.infernal":["goal.pvm.inferno-prep","gear.early.fire-cape"],
    "gear.endgame.quiver":["goal.pvm.colosseum-prep"],
    "goal.clue.eclectic-loop":["goal.skill.hunter-50"],
    "goal.clue.ranger-boots":["goal.clue.eclectic-loop"],
}
for goal in goals:
    if goal["id"] in DEPENDENCIES:
        goal["dependencyIds"] = DEPENDENCIES[goal["id"]]

ROUTE_ANCHORS = {
    "goal.quest.perilous-moons":"efficient-ironman.188.perilous-moons",
    "goal.quest.song-of-the-elves":"efficient-ironman.227.song-of-the-elves",
    "goal.raid.toa-entry":"efficient-ironman.218.beneath-cursed-sands",
    "goal.unlock.fossil-island":"efficient-ironman.095.bone-voyage",
    "goal.account.kingdom":"efficient-ironman.171.royal-trouble",
    "goal.quest.monkey-madness-i":"efficient-ironman.153.monkey-madness-i",
    "goal.quest.desert-treasure-i":"efficient-ironman.172.desert-treasure-i",
    "goal.quest.lunar-diplomacy":"efficient-ironman.194.lunar-diplomacy",
    "goal.quest.monkey-madness-ii":"efficient-ironman.221.monkey-madness-ii",
    "goal.quest.dragon-slayer-ii":"efficient-ironman.224.dragon-slayer-ii",
    "goal.quest.while-guthix-sleeps":"efficient-ironman.226.while-guthix-sleeps",
}
for goal in goals:
    if goal["id"] in ROUTE_ANCHORS:
        goal["routeAnchorId"] = ROUTE_ANCHORS[goal["id"]]
    if goal["id"] == "goal.quest.song-of-the-elves":
        goal["requirements"] = {"type":"ALL","label":"Song of the Elves requirements","children":[
            skill_condition("Agility",70), skill_condition("Construction",70), skill_condition("Farming",70),
            skill_condition("Herblore",70), skill_condition("Hunter",70), skill_condition("Mining",70),
            skill_condition("Smithing",70), skill_condition("Woodcutting",70),
            quest_condition("Mourning's End Part II"), quest_condition("Making History"),
            quest_condition("Druidic Ritual")
        ]}

# Separate readiness from actual completion for compound unlocks.
by_id = {goal["id"]:goal for goal in goals}
prayer_70 = by_id["goal.skill.prayer-70"]
prayer_70["title"] = "70 Prayer / Piety readiness"
prayer_70["whyItMatters"] = "This satisfies Piety's Prayer level, but King's Ransom, 70 Defence, and Knight Waves remain separate gates."
prayer_70["unlocks"] = ["Piety Prayer-level requirement","Diary and combat progression"]
prayer_70["wikiPage"] = "Prayer"
prayer_70["sourceReferences"] = ["wiki-knight-waves","wiki-ironman"]

moonlight_moths = by_id["goal.skill.hunter-75"]
moonlight_moths["description"] = "Reach 75 Hunter and unlock Varlamore through Children of the Sun before treating moonlight moths as accessible."
moonlight_moths["requirements"] = all_condition("Moonlight moth access",skill_condition("Hunter",75),
    quest_condition("Children of the Sun"))
moonlight_moths["completion"] = all_condition("Moonlight moth access",skill_condition("Hunter",75),
    quest_condition("Children of the Sun"))
moonlight_moths["relatedQuests"] = ["Children of the Sun"]

fairy = by_id["goal.transport.fairy-rings"]
fairy["description"] = "Complete Fairytale I, start Fairytale II, then progress through the early conversations until the ring network works."
fairy["completion"] = manual("Use a fairy ring after partial Fairytale II progress")
fairy["requirements"] = all_condition("Fairy-ring readiness",
    quest_condition("Fairytale I - Growing Pains"),
    any_condition("Fairytale II started",
        {"type":"QUEST_STATE","label":"Fairytale II in progress","quest":"Fairytale II - Cure a Queen","state":"IN_PROGRESS"},
        quest_condition("Fairytale II - Cure a Queen")))
fairy["completionMode"] = "MANUAL"
fairy["sourceReferences"] = ["wiki-fairytale-ii","wiki-ironman"]
fairy["relatedQuests"] = ["Fairytale I - Growing Pains","Fairytale II - Cure a Queen"]

# Durable item ownership is auto-detected from inventory, equipment, or an observed bank.
ITEM_COMPLETION = {
    "goal.qol.graceful": (all_condition("Complete Graceful outfit",
        item_any("Graceful hood",[11850]),item_any("Graceful cape",[11852]),
        item_any("Graceful top",[11854]),item_any("Graceful legs",[11856]),
        item_any("Graceful gloves",[11858]),item_any("Graceful boots",[11860])),
        ["Graceful hood","Graceful cape","Graceful top","Graceful legs","Graceful gloves","Graceful boots"]),
    "goal.qol.rogue-outfit": (all_condition("Complete rogue outfit",
        item_any("Rogue mask",[5554]),item_any("Rogue top",[5553]),item_any("Rogue trousers",[5555]),
        item_any("Rogue gloves",[5556]),item_any("Rogue boots",[5557])),
        ["Rogue mask","Rogue top","Rogue trousers","Rogue gloves","Rogue boots"]),
    "goal.qol.fish-barrel": (item_any("Fish barrel",[25582,25584]),["Fish barrel"]),
    "goal.qol.tackle-box": (item_any("Tackle box",[25580]),["Tackle box"]),
    "goal.qol.coal-bag": (item_any("Coal bag",[764,12019,24480,25627]),["Coal bag"]),
    "goal.qol.gem-bag": (item_any("Gem bag",[766,12020,24481,25628]),["Gem bag"]),
    "goal.qol.plank-sack": (item_any("Plank sack",[24882,25629]),["Plank sack"]),
    "goal.qol.seed-box": (item_any("Seed box",[13639,24482]),["Seed box"]),
    "goal.qol.herb-sack": (item_any("Herb sack",[13226,24478]),["Herb sack"]),
    "goal.qol.bottomless-bucket": (item_any("Bottomless compost bucket",[22994,22997]),["Bottomless compost bucket"]),
    "goal.qol.colossal-pouch": (item_any("Colossal pouch",[26784,26786,26906]),["Colossal pouch"]),
    "goal.qol.rune-pouch": (item_any("Rune pouch",[12791,23650,24416,27086,27281,27509]),["Rune pouch"]),
    "goal.qol.log-basket": (item_any("Log basket",[28140,28142]),["Log basket"]),
    "goal.qol.reagent-pouch": (item_any("Reagent pouch",[29996,29998]),["Reagent pouch"]),
    "goal.minigame.master-wand": (item_any("Master wand",[6914]),["Master wand"]),
    "goal.minigame.void": (all_condition("Complete Void Knight set",
        item_any("Void knight top",[8839]),item_any("Void knight robe",[8840]),
        item_any("Void knight gloves",[8842]),item_any("Void knight helm",[11663,11664,11665])),
        ["Void knight top","Void knight robe","Void knight gloves","Void knight helms"]),
    "goal.minigame.elite-void": (all_condition("Complete Elite Void sets",
        item_any("Elite void top",[13072]),item_any("Elite void robe",[13073]),
        item_any("Void knight gloves",[8842]),item_any("Void knight melee helm",[11665]),
        item_any("Void knight ranger helm",[11664]),item_any("Void knight mage helm",[11663])),
        ["Elite void top","Elite void robe","Void knight gloves","Void knight helms"]),
}
for goal_id, (condition, related_items) in ITEM_COMPLETION.items():
    goal = by_id[goal_id]
    goal["completion"] = condition
    goal["completionMode"] = "HYBRID"
    goal["relatedItems"] = related_items

# Source-specific editorial cross-checks; every bundled source has an intentional consumer.
SOURCE_AUGMENTS = {
    "goal.quest.song-of-the-elves":["bruhsailer"],
    "gear.mid.bowfa":["bruhsailer","reddit-stages"],
    "goal.quest.perilous-moons":["wiki-mootrius","reddit-modern"],
    "goal.pvm.royal-titans":["wiki-mootrius"],
    "gear.early.fire-cape":["reddit-stages"],
}
for goal_id, source_ids in SOURCE_AUGMENTS.items():
    by_id[goal_id]["sourceReferences"].extend(source_ids)

# Hard dependencies drive the planner. Softer recommendations and alternatives only affect context/scoring.
by_id["goal.resource.prayer-sustain"].pop("dependencyIds",None)
SOFT_RELATIONSHIPS = {
    "goal.resource.prayer-sustain":[
        ("goal.skill.herblore-38","ALTERNATIVE"),("goal.skill.hunter-75","ALTERNATIVE"),
        ("goal.resource.ranarr-loop","RECOMMENDED_BEFORE")],
    "goal.resource.food-karambwans":[("goal.activity.hunter-rumours-50","ALTERNATIVE"),
        ("goal.skill.hunter-72","ALTERNATIVE")],
    "goal.resource.giant-seaweed":[("goal.activity.golem-crafting","ALTERNATIVE")],
    "goal.activity.golem-crafting":[("goal.resource.giant-seaweed","ALTERNATIVE")],
    "goal.unlock.piety":[("goal.pvm.perilous-moons-loop","LEADS_TO"),("gear.early.fire-cape","LEADS_TO")],
    "goal.skill.hunter-46":[("goal.activity.hunter-rumours","LEADS_TO")],
    "goal.skill.hunter-72":[("gear.early.sunlight-crossbow","LEADS_TO")],
    "goal.skill.sailing-62":[("goal.activity.golem-crafting","LEADS_TO")],
    "goal.skill.sailing-15":[("goal.activity.sailing-salvaging-station","LEADS_TO"),
        ("goal.activity.sailing-trawling","LEADS_TO")],
    "goal.transport.fairy-rings":[("goal.resource.food-karambwans","LEADS_TO"),
        ("goal.resource.herb-runs","LEADS_TO")],
    "gear.early.gloves":[("gear.early.fire-cape","LEADS_TO"),("goal.quest.song-of-the-elves","SYNERGY")],
    "gear.early.fire-cape":[("goal.unlock.piety","RECOMMENDED_BEFORE"),
        ("goal.resource.prayer-sustain","RECOMMENDED_BEFORE")],
    "gear.mid.zombie-axe":[("gear.early.melee-weapon","ALTERNATIVE"),("gear.mid.blood-moon","ALTERNATIVE"),
        ("goal.pvm.perilous-moons-loop","SYNERGY")],
    "goal.account.strong-poh":[("goal.pvm.zulrah","LEADS_TO"),("goal.pvm.god-wars-entry","LEADS_TO"),
        ("goal.pvm.inferno-prep","SYNERGY"),("goal.pvm.colosseum-prep","SYNERGY")],
    "goal.pvm.perilous-moons-loop":[("goal.unlock.piety","RECOMMENDED_BEFORE"),
        ("gear.mid.barrows-tank","RECOMMENDED_BEFORE"),("goal.pvm.barrows","ALTERNATIVE")],
    "gear.mid.whip":[("gear.mid.zombie-axe","ALTERNATIVE"),("goal.raid.cox","SYNERGY"),
        ("goal.raid.tob-normal","SYNERGY")],
    "gear.mid.trident":[("gear.mid.warped-sceptre","ALTERNATIVE"),("goal.pvm.zulrah","LEADS_TO"),
        ("goal.raid.cox","SYNERGY"),("goal.raid.tob-normal","SYNERGY")],
    "goal.quest.song-of-the-elves":[("goal.pvm.gauntlet","LEADS_TO"),("goal.pvm.zulrah","SYNERGY")],
    "goal.pvm.gauntlet":[("goal.pvm.corrupted-gauntlet","LEADS_TO")],
    "goal.pvm.corrupted-gauntlet":[("gear.mid.bowfa","LEADS_TO")],
    "gear.mid.bowfa":[("goal.pvm.zulrah","SYNERGY"),("goal.pvm.god-wars-entry","SYNERGY"),
        ("goal.raid.toa-normal","SYNERGY"),("goal.raid.cox","SYNERGY")],
    "goal.pvm.zulrah":[("gear.mid.trident","RECOMMENDED_BEFORE"),
        ("goal.resource.food-karambwans","RECOMMENDED_BEFORE")],
    "goal.pvm.god-wars-entry":[("goal.unlock.piety","RECOMMENDED_BEFORE"),
        ("goal.account.strong-poh","RECOMMENDED_BEFORE"),("goal.resource.prayer-sustain","RECOMMENDED_BEFORE")],
    "goal.raid.toa-normal":[("goal.unlock.piety","RECOMMENDED_BEFORE"),
        ("goal.resource.prayer-sustain","RECOMMENDED_BEFORE"),("goal.resource.food-karambwans","SYNERGY")],
    "goal.raid.cox":[("gear.mid.trident","RECOMMENDED_BEFORE"),("goal.unlock.piety","RECOMMENDED_BEFORE"),
        ("goal.account.strong-poh","RECOMMENDED_BEFORE")],
    "goal.raid.tob-normal":[("gear.mid.whip","RECOMMENDED_BEFORE"),("gear.mid.trident","RECOMMENDED_BEFORE"),
        ("goal.unlock.piety","RECOMMENDED_BEFORE")],
    "goal.pvm.nex":[("goal.pvm.god-wars-entry","RECOMMENDED_BEFORE"),
        ("goal.resource.prayer-sustain","RECOMMENDED_BEFORE"),("gear.mid.bowfa","SYNERGY")],
    "gear.endgame.infernal":[("goal.resource.prayer-sustain","RECOMMENDED_BEFORE"),
        ("goal.account.strong-poh","RECOMMENDED_BEFORE")],
    "gear.endgame.quiver":[("goal.resource.prayer-sustain","RECOMMENDED_BEFORE"),
        ("goal.account.strong-poh","RECOMMENDED_BEFORE"),("goal.unlock.piety","SYNERGY")],
}
for goal in goals:
    relations = goal.setdefault("relationships",[])
    for dependency in goal.get("dependencyIds",[]):
        relations.append({"goalId":dependency,"type":"REQUIRES"})
    for related_id, relation_type in SOFT_RELATIONSHIPS.get(goal["id"],[]):
        relations.append({"goalId":related_id,"type":relation_type})

for goal in list(goals):
    for relation in list(goal.get("relationships",[])):
        if relation["type"] == "REQUIRES" and relation["goalId"] in by_id:
            by_id[relation["goalId"]].setdefault("relationships",[]).append(
                {"goalId":goal["id"],"type":"LEADS_TO"})

TAG_INTENTS = {
    "prayer-sustain":"PRAYER_SUSTAIN","food-sustain":"FOOD_SUSTAIN","transport":"TRANSPORT_NETWORK",
    "teleport":"TRANSPORT_NETWORK","money":"GP_SUSTAIN","gp":"GP_SUSTAIN","herblore":"HERB_SUPPLY",
    "herb-runs":"HERB_SUPPLY","runes":"RUNE_SUPPLY","ammo":"AMMO_SUPPLY","crafting":"CRAFTING_SUPPLY",
    "poh":"POH_NETWORK","slayer":"SLAYER_PROGRESS","clues":"CLUE_SUPPORT","bossing":"BOSSING_READINESS",
    "raid":"RAID_READINESS","melee":"MELEE_POWER","ranged":"RANGED_POWER","magic":"MAGIC_POWER",
}
ITEM_TAGS = {
    "prayer-sustain":["Prayer potion","Super restore","Moonlight moth"],
    "food-sustain":["Cooked karambwan","High-healing food"],
    "ammo":["Arrows","Bolts","Darts"],"runes":["Combat runes"],
    "jewellery":["Crafted jewellery"],"herb-runs":["Herb seeds","Grimy herbs"],
}

def skill_target(goal):
    condition = goal.get("completion",{})
    return condition.get("level") if condition.get("type") == "SKILL_AT_LEAST" else None

for goal in goals:
    # Canonical, unique tags make search and intent matching stable.
    normalized_tags = []
    for tag in goal.get("tags",[]):
        value = "-".join(str(tag).strip().lower().replace("_","-").split())
        if value and value not in normalized_tags: normalized_tags.append(value)
    goal["tags"] = normalized_tags
    goal["sourceReferences"] = list(dict.fromkeys(goal.get("sourceReferences",[])))

    # Benefits describe player outcomes; unlocks name concrete access/rewards.
    goal["benefits"] = [goal["whyItMatters"]]
    if len(goal["unlocks"]) > 1:
        goal["benefits"].append("Supports: " + ", ".join(goal["unlocks"][:2]))

    if goal.get("gearId") and not goal.get("relatedItems"):
        goal["relatedItems"] = [goal["title"]]
    for tag, item_names in ITEM_TAGS.items():
        if tag in normalized_tags:
            goal["relatedItems"] = list(dict.fromkeys(goal.get("relatedItems",[]) + item_names))

    inferred_intents = list(goal.get("intents",[]))
    for tag in normalized_tags:
        intent = TAG_INTENTS.get(tag)
        if intent and intent not in inferred_intents: inferred_intents.append(intent)
    if goal["category"] == "Account Infrastructure" and "ACCOUNT_INFRASTRUCTURE" not in inferred_intents:
        inferred_intents.append("ACCOUNT_INFRASTRUCTURE")
    goal["intents"] = inferred_intents

    # RNG describes completion itself, not merely a boss's optional reward table.
    if goal["category"] in ("Bossing","Raids") and goal["id"].startswith(("goal.pvm.","goal.raid.")):
        goal["rng"] = False

    tags = set(normalized_tags)
    if "mega-rare" in tags or goal["id"] in ("gear.endgame.infernal","gear.endgame.quiver"):
        goal["priority"] = "PRESTIGE"
    elif goal.get("rng"):
        goal["priority"] = "RNG_GRIND"
    elif "collection" in tags:
        goal["priority"] = "COLLECTION"
    elif "optional" in tags:
        goal["priority"] = "OPTIONAL"
    elif goal.get("usefulness",3) == 5 and goal.get("popular"):
        goal["priority"] = goal.get("priority","CORE")
    else:
        goal["priority"] = goal.get("priority","RECOMMENDED")

    goal["communityWeight"] = goal.get("communityWeight",
        "VERY_COMMON" if goal.get("popular") and goal.get("usefulness",3) == 5 else
        "COMMON" if goal.get("popular") else "NOTABLE" if goal.get("usefulness",3) >= 4 else "NICHE")
    goal["impact"] = "MAJOR" if goal["priority"] == "CORE" else \
        "HIGH" if goal.get("usefulness",3) >= 4 else "MEDIUM"

    level = skill_target(goal)
    if level is not None:
        goal["effort"] = "QUICK" if level <= 30 else "SHORT" if level <= 50 else \
            "MEDIUM" if level <= 70 else "LONG" if level <= 85 else "VERY_LONG"
    elif goal["priority"] in ("PRESTIGE","RNG_GRIND"):
        goal["effort"] = "VERY_LONG" if goal["stage"] in ("LATE","ENDGAME") else "LONG"
    elif goal["stage"] == "VERY_EARLY": goal["effort"] = "QUICK"
    elif goal["stage"] == "EARLY": goal["effort"] = "SHORT"
    elif goal["stage"] in ("LATE","ENDGAME"): goal["effort"] = "LONG"

    completion_type = goal.get("completion",{}).get("type")
    if "completionMode" not in goal:
        goal["completionMode"] = "HYBRID" if goal.get("gearId") or completion_type in ("ITEM_ANY","ITEM_ANY_EXACT","ALL") and goal["id"] in ITEM_COMPLETION \
            else "MANUAL" if completion_type == "MANUAL_ONLY" else "AUTO"

    # Stable unique typed relationships.
    seen_relations = set()
    unique_relations = []
    for relation in goal.get("relationships",[]):
        key = (relation["goalId"],relation["type"])
        if key not in seen_relations:
            seen_relations.add(key); unique_relations.append(relation)
    goal["relationships"] = unique_relations

# Hand-authored outcomes for headline milestones; these must say what changes for the player, not echo unlock names.
BENEFIT_OVERRIDES = {
    "gear.early.gloves":["One durable glove-slot upgrade supports melee, ranged, magic, and many quest setups.",
        "Recipe for Disaster progress also broadens account access and quest completion."],
    "goal.transport.fairy-rings":["Remote Slayer, clue, Farming, quest, and karambwan destinations become repeatable with little inventory cost."],
    "goal.unlock.piety":["Melee accuracy, damage, and defence improve together for Slayer and bossing."],
    "gear.early.fire-cape":["A lasting melee cape upgrade also proves the account can sustain and execute wave content."],
    "gear.mid.zombie-axe":["Strong slash and crush coverage bridges dragon weapons, Moons gear, and the whip without forcing an RNG route."],
    "goal.account.strong-poh":["Restoration and transport are centralized, reducing supply and travel friction before repeated PvM."],
    "goal.pvm.perilous-moons-loop":["Dungeon-made supplies let the account practice bosses while banking deterministic resources and duplicate-protected gear progress."],
    "gear.mid.whip":["A fast, accurate general melee weapon strengthens Slayer and many bosses without consuming charges."],
    "gear.mid.trident":["A powered staff makes movement-heavy magic encounters practical and separates damage casts from the spellbook."],
    "goal.quest.song-of-the-elves":["Prifddinas consolidates Gauntlet, Zalcano, crystal gear, elves, and high-level skilling services."],
    "goal.pvm.gauntlet":["Self-contained preparation teaches movement and switches without consuming banked food or potions."],
    "goal.pvm.corrupted-gauntlet":["The harder self-contained encounter advances the crystal armour and enhanced-seed branch without promising drop timing."],
    "gear.mid.bowfa":["Bowfa with crystal armour provides a powerful, broadly useful ranged branch across bosses, raids, and Slayer."],
    "goal.pvm.zulrah":["Consistent kills establish renewable scales and access to several optional ranged and magic upgrades."],
    "goal.pvm.god-wars-entry":["A safe entry plan turns protection, kill count, sustain, and escape tools into productive trips."],
    "goal.raid.toa-normal":["Configurable invocations let the player grow raid execution while pursuing useful uniques without a guaranteed timeline."],
    "goal.raid.cox":["Chambers develops scouting, room knowledge, team roles, and all-style PvM preparation."],
    "goal.raid.tob-normal":["Normal Theatre develops precise team roles and execution after entry-mode familiarity."],
    "goal.pvm.nex":["Team-aware mechanics and sustained ranged preparation open the Torva and Zaryte reward branches."],
    "gear.endgame.infernal":["The cape is an execution milestone; the path emphasizes sustainable attempts and wave mastery rather than RNG."],
    "gear.endgame.quiver":["Colosseum mastery upgrades the ranged cape slot while testing movement, modifiers, supplies, and solves."],
    "goal.activity.hunter-rumours":["One repeatable Hunter loop supplies herbs, logs, meats, nests, bones, and access to useful milestones."],
    "goal.activity.vale-totems":["Active Fletching can return nests, roots, arrowtips, logs, flax, and ent branches instead of only consuming inputs."],
    "goal.activity.golem-crafting":["Hunter furs and sunstones become a bankable alternative to the traditional giant-seaweed and sand loop."],
    "goal.skill.sailing-62":["Wyrmscraig connects Sailing progress to quests, Golem Crafting, and new resource systems."],
    "goal.activity.sailing-salvaging-station":["Sorting salvage while still at sea extends low-intensity trips and keeps useful seeds, relics, teleports, and ship materials flowing."],
    "goal.activity.sailing-trawling":["A linen trawling setup adds a renewable Sailing-and-Fishing food branch while remaining an optional alternative to land methods."],
}
for goal_id, benefits in BENEFIT_OVERRIDES.items():
    by_id[goal_id]["benefits"] = benefits

catalog = {"version":5,"auditedAt":"2026-08-29","sources":SOURCES,"goals":goals}
OUTPUT.write_text(json.dumps(catalog,indent=2,ensure_ascii=False) + "\n",encoding="utf-8")
print(f"Wrote {len(goals)} goals to {OUTPUT}")
