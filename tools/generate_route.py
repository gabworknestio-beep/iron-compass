#!/usr/bin/env python3
"""Regenerate Iron Compass's bundled route from reviewed public source facts.

This is a development-only tool. Iron Compass never downloads route data at runtime.
The generated instructions and reasons are original concise summaries; the tool
does not copy Wiki or community-guide prose.
"""

import json
import re
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src" / "main" / "resources" / "routes" / "efficient-ironman.json"
AUDIT_DATE = "2026-08-26"
WIKI_PAGE = "Optimal quest guide/Ironman"
WIKI_URL = "https://oldschool.runescape.wiki/w/Optimal_quest_guide/Ironman"
RUNELITE_QUEST_URL = (
    "https://raw.githubusercontent.com/runelite/runelite/master/"
    "runelite-api/src/main/java/net/runelite/api/Quest.java"
)


RFD_NAMES = {
    "Recipe for Disaster/Another Cook's Quest": "Recipe for Disaster - Another Cook's Quest",
    "Recipe for Disaster/Freeing the Goblin generals": "Recipe for Disaster - Wartface & Bentnoze",
    "Recipe for Disaster/Freeing the Mountain Dwarf": "Recipe for Disaster - Mountain Dwarf",
    "Recipe for Disaster/Freeing Evil Dave": "Recipe for Disaster - Evil Dave",
    "Recipe for Disaster/Freeing Pirate Pete": "Recipe for Disaster - Pirate Pete",
    "Recipe for Disaster/Freeing the Lumbridge Guide": "Recipe for Disaster - Lumbridge Guide",
    "Recipe for Disaster/Freeing Skrach Uglogwee": "Recipe for Disaster - Skrach Uglogwee",
    "Recipe for Disaster/Freeing Sir Amik Varze": "Recipe for Disaster - Sir Amik Varze",
    "Recipe for Disaster/Freeing King Awowogei": "Recipe for Disaster - King Awowogei",
    "Recipe for Disaster#Defeating the Culinaromancer": "Recipe for Disaster - Culinaromancer",
}


MANUAL_CONFIRMATIONS = {
    "Stronghold of Security": "the fourth-level treasure has been claimed",
    "Natural history quiz": "Orlando Smith's quiz is complete",
    "Start Barcrawl": "the Barcrawl card has been received",
    "Anti-dragon shield": "Duke Horacio has given you an anti-dragon shield",
    "Varrock Museum": "at least 100 Varrock Museum Kudos are displayed",
    "Crafting Guild Balloon": "the Crafting Guild balloon route is unlocked",
    "Varrock Balloon": "the Varrock balloon route is unlocked",
    "Knight Waves Training Grounds": "the Knight Waves challenge is complete",
}


REASONS = {
    "Learning the Ropes": "Finishes account onboarding and unlocks the Sailing progression used by the modern route.",
    "Druidic Ritual": "Unlocks Herblore so future lamps and gathered herbs can advance a slow Ironman skill.",
    "Tree Gnome Village": "Large early Attack experience and spirit-tree access accelerate the next quest cluster.",
    "Fight Arena": "Efficient early combat experience avoids slow low-level melee training.",
    "Waterfall Quest": "A major early Attack and Strength boost that opens safer, faster combat progression.",
    "The Grand Tree": "Adds gnome gliders and more early combat experience while advancing the gnome quest line.",
    "Lost City": "Unlocks Zanaris, dragon weapons, and the path toward fairy rings.",
    "Fairytale I - Growing Pains": "Sets up fairy-ring access, one of the most valuable travel networks for an Ironman.",
    "Partial completion of Fairytale II - Cure a Queen": "Unlocks fairy rings early; finishing the full quest can wait.",
    "Bone Voyage": "Unlocks Fossil Island, bird houses, ammonite crabs, and several long-term skilling loops.",
    "The Feud": "Provides a large Thieving reward and avoids a substantial low-level training grind.",
    "Dragon Slayer I": "Unlocks rune platebody access and advances many later quest requirements.",
    "Animal Magnetism": "Unlocks Ava's devices, a lasting Ranged quality-of-life upgrade.",
    "Ghosts Ahoy": "The ectophial gives a permanent one-click teleport to Morytania.",
    "Temple of the Eye": "Unlocks Guardians of the Rift for practical Ironman Runecraft training and runes.",
    "Monkey Madness I": "Unlocks the dragon scimitar and advances the core gnome storyline.",
    "Underground Pass": "Unlocks Iban's staff and the route into western Tirannwn.",
    "Heroes' Quest": "Unlocks the Heroes' Guild and is required for major late-game quest chains.",
    "Desert Treasure I": "Unlocks Ancient Magicks and several important follow-up quests.",
    "Recipe for Disaster#Defeating the Culinaromancer": "Finishes Barrows gloves, a foundational all-round equipment unlock.",
    "A Kingdom Divided": "Completes the Kourend storyline and unlocks the full Arceuus spellbook.",
    "Perilous Moons": "Unlocks repeatable mid-game bosses with useful equipment and supply rewards.",
    "Sins of the Father": "Unlocks Darkmeyer and advances the account toward endgame Morytania content.",
    "Monkey Madness II": "Unlocks demonic gorillas and completes a major weapon-upgrade quest line.",
    "Dragon Slayer II": "Unlocks Vorkath, the Myths' Guild, and key ranged equipment progression.",
    "While Guthix Sleeps": "Unlocks tormented demons and a major late-game upgrade path.",
    "Song of the Elves": "Unlocks Prifddinas and its dense set of skilling, PvM, and equipment upgrades.",
    "Desert Treasure II - The Fallen Empire": "Unlocks four repeatable bosses and completes a central late-game quest chain.",
    "The Blood Moon Rises": "Advances the current Varlamore storyline and modern quest-cape requirements.",
}


WHILE_HERE = {
    "Learning the Ropes": [
        ("Start X Marks the Spot", "Speak to Veos before leaving Lumbridge so its clues fit the early travel loop."),
        ("Bank the tutorial supplies", "Keep the pot and bucket; both recur throughout early quests."),
        ("Knock out two diary tasks", "Pickpocket a man and ask Hans your age while you are already by the castle."),
    ],
    "Cook's Assistant": [
        ("Gather useful duplicates", "Take an extra pot and bucket while collecting the quest ingredients."),
        ("Start Sheep Shearer", "Speak to Fred and collect wool during the same western Lumbridge loop."),
    ],
    "The Restless Ghost": [
        ("Keep the leather gloves", "Pick them up near Father Urhney; several future steps need hand protection."),
        ("Advance Rune Mysteries", "Bring the air talisman to the Wizards' Tower while you are there."),
    ],
    "X Marks the Spot": [
        ("Start Vampyre Slayer", "Visit Morgan in Draynor and bank spare garlic for later quests."),
        ("Run one rooftop lap", "Complete the nearby easy Lumbridge diary task."),
    ],
    "Witch's Potion": [
        ("Shop once in Port Sarim", "Buy an eye of newt, sardine, bait, cheese, and raw beef for the next quest cluster."),
        ("Mine clay in Rimmington", "Bank clay for Doric's Quest and later soft-clay requirements."),
    ],
    "Client of Kourend": [
        ("Use the minecarts", "Batch the five house visits through the low-cost minecart network."),
        ("Start Pirate's Treasure", "Speak to Redbeard Frank when the route returns to Port Sarim."),
    ],
    "Tree Gnome Village": [
        ("Collect snape grass", "Pick up several pieces on the Rimmington coast for upcoming quests."),
        ("Unlock the BA teleport", "Complete the Barbarian Assault tutorial while passing the outpost."),
        ("Advance the barcrawl", "Visit the nearby bars now to reduce future travel."),
    ],
    "Plague City": [
        ("Stock up on ropes", "Buy a few ropes in Ardougne; several near-term quests consume them."),
        ("Start Biohazard", "Keep the west-Ardougne storyline moving before leaving the area."),
    ],
    "Below Ice Mountain": [
        ("Start Black Knights' Fortress", "Speak to Sir Amik Varze during the Falador portion."),
        ("Mine spare iron", "Bank ore in Rimmington for near-term quest bars."),
    ],
    "Elemental Workshop I": [
        ("Mine extra elemental ore", "Bank at least two pieces for Elemental Workshop II."),
    ],
    "Fairytale I - Growing Pains": [
        ("Start the sequel immediately", "Progress Fairytale II only far enough to unlock fairy rings."),
    ],
    "Bone Voyage": [
        ("Set up bird houses", "Begin passive Hunter runs as soon as Fossil Island becomes available."),
    ],
    "Ghosts Ahoy": [
        ("Keep the ectophial charged", "Refill it before leaving for a dependable Morytania teleport."),
    ],
    "Monkey Madness I": [
        ("Buy a dragon scimitar", "After completion, purchase the weapon on Ape Atoll when your Attack level permits."),
    ],
}


PREPARATION = {
    "Learning the Ropes": [("ITEM", "Spade", 952, 1, "ANY", False)],
    "Witch's Potion": [("ITEM", "Coins", 995, 100, "ANY", True)],
    "Tree Gnome Village": [("ITEM", "Food", 379, 8, "ANY", True)],
    "Plague City": [("ITEM", "Rope", 954, 1, "ANY", True)],
    "Waterfall Quest": [
        ("ITEM", "Rope", 954, 1, "ANY", True),
        ("ITEM", "Food", 379, 8, "ANY", True),
    ],
    "The Grand Tree": [("ITEM", "Food", 379, 10, "ANY", True)],
    "Lost City": [("SKILL", "Woodcutting", 0, 1, "ANY", False)],
    "Monkey Madness I": [
        ("SKILL", "Prayer", 0, 43, "ANY", False),
        ("ITEM", "Food", 379, 12, "ANY", True),
    ],
}


# Stable quest-state boundaries verified against the corresponding Quest Helper
# loadSteps() state map. These let Iron Compass hand a partial quest to Quest Helper
# while still advancing automatically at the route's intended stopping point.
PIRATES_TREASURE_MILESTONE = {
    "quest": "Pirate's Treasure",
    "instruction": (
        "Open Pirate's Treasure in Quest Helper. Complete its Musa Point and Port Sarim steps through "
        "receiving the chest key from Redbeard Frank, then stop before opening the Varrock chest."
    ),
    "reason": (
        "Quest Helper can guide this quest segment now; the Varrock chest and Falador finale stay grouped "
        "with later travel."
    ),
    "completion": {
        "type": "VARP_AT_LEAST",
        "label": "Quest Helper milestone reached",
        "id": 71,
        "value": 2,
    },
}

PARTIAL_QUEST_MILESTONES = {
    "Partial completion of Demon Slayer": {
        "title": "Demon Slayer — collect all three keys",
        "quest": "Demon Slayer",
        "instruction": (
            "Use Quest Helper to start Demon Slayer, record the incantation, retrieve the two Varrock Palace "
            "keys, bring 25 bones to Traiborn after Rune Mysteries, and stop once all three keys are secured."
        ),
        "reason": "Front-loads the scattered key collection while the route already visits Varrock and the Wizards' Tower.",
        "completion": {"type": "MANUAL_ONLY", "label": "all three Silverlight keys are secured"},
    },
    "Partial completion of Enter the Abyss": {
        "title": "Enter the Abyss — charge the scrying orb",
        "quest": "Enter the Abyss",
        "instruction": (
            "Start with the Mage of Zamorak north of Edgeville, obtain his scrying orb at Varrock's chaos temple, "
            "visit Aubury's essence mine to charge it, then bank the orb and stop."
        ),
        "reason": "Completes the Varrock-side setup now; the other essence-mine visits stay with later travel.",
        "completion": {"type": "MANUAL_ONLY", "label": "the scrying orb is charged and banked"},
    },
    "Partial completion of Dwarf Cannon": {
        "title": "Dwarf Cannon — repair the railings",
        "quest": "Dwarf Cannon",
        "instruction": (
            "Use Quest Helper to repair the railings, inspect the watchtower, rescue Lollk, and return to Captain "
            "Lawgof. Stop when he asks you to visit Nulodion."
        ),
        "reason": "Finishes the western quest work during this travel loop and postpones only the Dwarven Mine visit.",
        "completion": {"type": "MANUAL_ONLY", "label": "Captain Lawgof asks you to visit Nulodion"},
    },
    "Partially complete Rag and Bone Man I": {
        "title": "Rag and Bone Man I — start the bone list",
        "quest": "Rag and Bone Man I",
        "instruction": "Start Rag and Bone Man I with the Odd Old Man, then keep its listed bones as upcoming quests provide them.",
        "reason": "Turns future quest kills into passive collection instead of a separate cleanup trip.",
        "completion": {"type": "MANUAL_ONLY", "label": "the Rag and Bone Man I bone list has been received"},
    },
    "Partially complete Rag and Bone Man II": {
        "title": "Rag and Bone Man II — start the extended list",
        "quest": "Rag and Bone Man II",
        "instruction": "Start Rag and Bone Man II after the first quest and save each qualifying bone during the remaining route.",
        "reason": "Begins the long collection pass early so normal progression supplies most bones automatically.",
        "completion": {"type": "MANUAL_ONLY", "label": "the extended Rag and Bone Man II list has been received"},
    },
    "Partial completion of Fairytale II - Cure a Queen": {
        "title": "Fairytale II — unlock fairy rings",
        "quest": "Fairytale II - Cure a Queen",
        "instruction": "Use Quest Helper to begin Fairytale II and continue only until the fairy-ring network becomes usable, then stop.",
        "reason": "Unlocks the account's most valuable travel network without forcing the full sequel into this travel cluster.",
        "completion": {"type": "MANUAL_ONLY", "label": "fairy rings are unlocked"},
    },
}

QUEST_HELPER_MILESTONES = {
    "Partial completion of Pirate's Treasure": PIRATES_TREASURE_MILESTONE,
    "Partially complete: Pirate's Treasure": PIRATES_TREASURE_MILESTONE,
    **PARTIAL_QUEST_MILESTONES,
}

ACTIVITY_OVERRIDES = {
    "Stronghold of Security": {
        "instruction": "Reach the treasure room on the fourth level of the Stronghold of Security and claim its 10,000-coin reward.",
        "reason": "Solves early cash pressure while unlocking the stronghold rewards and account-security emotes.",
    },
    "Natural history quiz": {
        "instruction": "Complete Orlando Smith's Natural History Quiz in the Varrock Museum basement for 1,000 Hunter and Slayer XP.",
        "reason": "Skips the slowest first Hunter and Slayer levels while the route is already in Varrock.",
    },
    "Start Barcrawl": {
        "instruction": "Speak to the Barbarian guard outside Barbarian Outpost and start Alfred Grimhand's Barcrawl.",
        "reason": "Starts a travel-heavy requirement early so later pub visits can advance it passively.",
    },
    "Anti-dragon shield": {
        "instruction": "Ask Duke Horacio in Lumbridge Castle for an anti-dragon shield and bank it for Dragon Slayer I.",
        "reason": "Secures the quest shield while the route is already in Lumbridge and avoids a return trip.",
    },
    "Varrock Museum": {
        "instruction": "Reach at least 100 Kudos at Varrock Museum so Bone Voyage can begin.",
        "reason": "Opens Fossil Island on the next step and consolidates the museum work into the Varrock route.",
    },
    "Crafting Guild Balloon": {
        "instruction": "Fly the balloon from Entrana to the Crafting Guild once to unlock that transport route.",
        "reason": "Adds a permanent transport link while Enlightened Journey and its balloon network are already active.",
    },
    "Varrock Balloon": {
        "instruction": "Fly the balloon from Entrana to Varrock once to unlock that transport route.",
        "reason": "Completes another permanent balloon link while the required Firemaking level is newly available.",
    },
    "Knight Waves Training Grounds": {
        "instruction": "Complete the Knight Waves Training Grounds after King's Ransom to unlock Chivalry and Piety.",
        "reason": "Turns the completed quest and Defence level into two permanent melee prayers before late combat progression.",
    },
}


DEFAULT_REASONS = {
    "QUEST": "Clears a prerequisite in the efficient quest chain so its rewards and follow-up quests become available on schedule.",
    "DIARY": "Claims diary rewards and travel conveniences at a point where the route has already assembled most requirements.",
    "UNLOCK": "Stops at the useful unlock boundary now and postpones the remaining work until it fits a later quest cluster.",
    "ACTIVITY": "Takes a durable account unlock or experience reward while the surrounding progression makes the detour efficient.",
    "MANUAL": "Closes a cross-category milestone that the client cannot prove safely from one quest, skill, or item signal.",
}


LOCATIONS = {
    "Stronghold of Security": (3081, 3421, 0, 6, "Stronghold of Security entrance"),
    "Natural history quiz": (3254, 3448, 0, 6, "Varrock Museum"),
    "Varrock Museum": (3254, 3448, 0, 6, "Varrock Museum"),
    "Tree Gnome Village": (2542, 3170, 0, 8, "Tree Gnome Village"),
    "Plague City": (2568, 3334, 0, 8, "East Ardougne"),
    "Waterfall Quest": (2519, 3495, 0, 8, "Baxtorian Falls"),
    "The Grand Tree": (2466, 3496, 0, 8, "The Grand Tree"),
    "Bone Voyage": (3362, 3445, 0, 8, "Digsite barge"),
    "Monkey Madness I": (2465, 3494, 0, 8, "The Grand Tree"),
}


def fetch_text(url):
    request = urllib.request.Request(url, headers={"User-Agent": "Iron Compass route generator"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def current_sources():
    params = urllib.parse.urlencode(
        {"action": "parse", "page": WIKI_PAGE, "prop": "wikitext", "format": "json", "origin": "*"}
    )
    wiki_payload = json.loads(fetch_text("https://oldschool.runescape.wiki/api.php?" + params))
    wiki_text = wiki_payload["parse"]["wikitext"]["*"]
    quest_java = fetch_text(RUNELITE_QUEST_URL)
    quest_names = set(re.findall(r'^\s*[A-Z0-9_]+\(\d+,\s*"([^"]+)"\)', quest_java, re.MULTILINE))
    return wiki_text, quest_names


def slug(value):
    value = value.lower().replace("&", " and ").replace("'", "")
    value = re.sub(r"[^a-z0-9]+", "-", value).strip("-")
    return value


def manual_completion(label):
    return {"type": "MANUAL_ONLY", "label": label}


def quest_completion(name):
    return {"type": "QUEST_STATE", "label": "Quest complete", "quest": name, "state": "FINISHED"}


def player_instruction(step_type, title):
    if step_type == "QUEST":
        return f"Complete {title}."
    if step_type == "DIARY":
        return f"Finish every {title} task and claim the reward."
    if step_type == "UNLOCK":
        return f"Reach and verify the {title} stopping point before continuing."
    if step_type == "ACTIVITY":
        return f"Complete {title} and claim its reward or permanent unlock."
    return f"Complete {title} before continuing the route."


def train_step(skill, level, occurrence):
    skill_name = skill.strip().title()
    suffix = "" if occurrence == 1 else f"-{occurrence}"
    return {
        "id": f"efficient-ironman.train.{slug(skill_name)}-{level}{suffix}",
        "type": "TRAIN",
        "title": f"Train {skill_name} to {level}",
        "category": "Skill milestone",
        "instruction": f"Raise {skill_name} to level {level} before continuing the quest route.",
        "reason": "Meets a near-term quest requirement without obscuring the grind inside another step.",
        "completion": {"type": "SKILL_AT_LEAST", "label": f"{skill_name} {level}", "skill": skill_name, "level": level},
        "readiness": None,
        "preparation": [
            {"kind": "SKILL", "name": skill_name, "skill": skill_name, "level": level, "quantity": 0, "source": "ANY", "consumable": False}
        ],
        "risk": "SAFE",
        "importance": "NORMAL",
        "optional": False,
        "tags": ["training", skill_name.lower()],
        "wikiPage": f"Ironman Guide/{skill_name}",
    }


def classify(title, quest_names):
    exact_quest = title if title in quest_names else RFD_NAMES.get(title)
    if exact_quest:
        return "QUEST", quest_completion(exact_quest), exact_quest
    lower = title.lower()
    if "diary" in lower:
        return "DIARY", manual_completion(f"the {title} reward is claimed"), None
    if "partial" in lower or "start " in lower or title in {"Anti-dragon shield", "Crafting Guild Balloon", "Varrock Balloon"}:
        return "UNLOCK", manual_completion(MANUAL_CONFIRMATIONS.get(title, f"{title} is complete")), None
    if title in {"Stronghold of Security", "Natural history quiz", "Varrock Museum", "Knight Waves Training Grounds", "Alfred Grimhand's Barcrawl"}:
        return "ACTIVITY", manual_completion(MANUAL_CONFIRMATIONS.get(title, f"{title} is complete")), None
    return "MANUAL", manual_completion(f"{title} is complete"), None


def route_step(title, quest_names, index):
    title = title.splitlines()[0].strip()
    step_type, completion, quest_name = classify(title, quest_names)
    display_title = title.replace("Recipe for Disaster/", "RFD: ").replace("Recipe for Disaster#", "RFD: ")
    instruction = player_instruction(step_type, display_title)
    step = {
        "id": f"efficient-ironman.{index:03d}.{slug(title)}",
        "type": step_type,
        "title": display_title,
        "category": "Quest" if step_type == "QUEST" else ("Achievement diary" if step_type == "DIARY" else "Route milestone"),
        "instruction": instruction,
        "reason": REASONS.get(title, DEFAULT_REASONS[step_type]),
        "completion": completion,
        "readiness": None,
        "requires": [],
        "preparation": [],
        "whileHere": [],
        "wikiPage": title,
        "questHelperKey": quest_name,
        "tags": [step_type.lower(), "efficient-ironman"],
        "optional": False,
        "risk": "HCIM_CAUTION" if title in {"Waterfall Quest", "Witch's House", "Underground Pass", "Desert Treasure I", "Dragon Slayer II"} else "SAFE",
        "importance": "MAJOR" if title in REASONS else "NORMAL",
    }
    if step_type == "DIARY":
        step["instruction"] = f"Complete every {display_title} task and claim the tier reward."
    if title == "All Easy Achievement Diaries":
        step["instruction"] = (
            "Finish and claim every remaining Easy Achievement Diary reward before beginning the medium-diary block."
        )
    if title == "Medium Wilderness Diary":
        step["instruction"] = (
            "Complete every Medium Wilderness Diary task and claim the tier reward; use appropriate Wilderness risk precautions."
        )
    for kind, name, item_id, amount, source, consumable in PREPARATION.get(title, []):
        spec = {"kind": kind, "name": name, "quantity": amount, "source": source, "consumable": consumable}
        if kind == "SKILL":
            spec.update({"skill": name, "level": amount, "quantity": 0})
        else:
            spec["itemId"] = item_id
        step["preparation"].append(spec)
    for item_title, detail in WHILE_HERE.get(title, []):
        step["whileHere"].append({"title": item_title, "detail": detail})
    if title in LOCATIONS:
        x, y, plane, radius, label = LOCATIONS[title]
        step["location"] = {"x": x, "y": y, "plane": plane, "radius": radius, "label": label}
    milestone = QUEST_HELPER_MILESTONES.get(title)
    if milestone:
        step["title"] = milestone.get("title", step["title"])
        step["instruction"] = milestone["instruction"]
        step["reason"] = milestone["reason"]
        step["completion"] = milestone["completion"]
        step["wikiPage"] = milestone["quest"]
        step["questHelperKey"] = milestone["quest"]
    activity = ACTIVITY_OVERRIDES.get(title)
    if activity:
        step["instruction"] = activity["instruction"]
        step["reason"] = activity["reason"]
    return step


def generate():
    wiki_text, quest_names = current_sources()
    pattern = re.compile(
        r'^\|- data-rowid="([^\r\n"]+)"?|\{\{Optimal quest/train\|([^|}]+)\|(\d+)(?:\|[^}]*)?\}\}',
        re.MULTILINE,
    )
    sequence = []
    train_occurrences = {}
    route_row_index = 0
    for match in pattern.finditer(wiki_text):
        if match.group(1) is not None:
            route_row_index += 1
            sequence.append((route_row_index, route_step(match.group(1), quest_names, route_row_index)))
        else:
            skill, level = match.group(2), int(match.group(3))
            key = (skill.lower(), level)
            train_occurrences[key] = train_occurrences.get(key, 0) + 1
            sequence.append((route_row_index, train_step(skill, level, train_occurrences[key])))

    if route_row_index != 230:
        raise RuntimeError(f"Expected 230 Wiki route rows, found {route_row_index}; audit the parser")

    section_specs = [
        ("foundations", "Foundations", 56, "Fast early unlocks, travel setup, and quest-reward chaining."),
        ("early-game", "Early game", 114, "Core transport, skilling, and regional quest lines."),
        ("mid-game", "Established Ironman", 176, "Barrows gloves preparation and broad account systems."),
        ("quest-cape", "Quest cape path", 10_000, "Modern high-requirement quests and current quest-cape finish."),
    ]
    sections = [{"id": sid, "name": name, "description": desc, "steps": []} for sid, name, _, desc in section_specs]
    for row_number, step in sequence:
        for target, (_, _, maximum, _) in zip(sections, section_specs):
            if row_number <= maximum:
                target["steps"].append(step)
                break

    route = {
        "routeId": "efficient-ironman",
        "version": 3,
        "name": "Efficient Ironman",
        "description": "A deterministic, account-adaptive path through the current Wiki Ironman quest order, with explicit training milestones and curated early routing.",
        "auditedAt": AUDIT_DATE,
        "sources": [
            {
                "type": "AUTHORITATIVE_ROUTE",
                "title": "OSRS Wiki — Optimal quest guide/Ironman",
                "url": WIKI_URL,
                "auditedAt": AUDIT_DATE,
                "notes": "Quest/action ordering and training milestone facts. Iron Compass wording is original and concise.",
            },
            {
                "type": "COMMUNITY_CONTEXT",
                "title": "BRUHsailer",
                "url": "https://umkyzn.github.io/BRUHsailer/",
                "auditedAt": AUDIT_DATE,
                "notes": "Reviewed for modern efficiency concepts only; no guide prose is bundled.",
            },
        ],
        "chapters": [
            {"id": "foundations", "name": "Account Foundations", "description": "Secure early cash, transport, and quest-reward combat levels.", "startStepId": "efficient-ironman.001.learning-the-ropes"},
            {"id": "travel", "name": "Early Travel & Questing", "description": "Batch Varrock, Falador, Karamja, and western travel while starting long collections.", "startStepId": "efficient-ironman.017.partial-completion-of-demon-slayer"},
            {"id": "fairy-rings", "name": "Core Skills & Fairy Rings", "description": "Build the skill floor and unlock the transport network that compresses every later chapter.", "startStepId": "efficient-ironman.train.cooking-15"},
            {"id": "regions", "name": "Regional Unlocks", "description": "Open Fossil Island, Morytania, Kourend, and the first repeatable account systems.", "startStepId": "efficient-ironman.train.magic-33"},
            {"id": "rfd-fremennik", "name": "RFD & Fremennik", "description": "Advance Recipe for Disaster and the Fremennik quest chains toward durable combat rewards.", "startStepId": "efficient-ironman.train.cooking-22"},
            {"id": "spellbooks", "name": "Combat & Spellbooks", "description": "Unlock Iban's staff, Ancient Magicks, and broader midgame combat access.", "startStepId": "efficient-ironman.139.haunted-mine"},
            {"id": "systems", "name": "Midgame Systems & Kingdom", "description": "Establish Slayer, diaries, kingdom, and the supporting skill economy.", "startStepId": "efficient-ironman.train.slayer-42"},
            {"id": "barrows-gloves", "name": "Barrows Gloves & Piety", "description": "Convert accumulated quest requirements into the first major all-style combat baseline.", "startStepId": "efficient-ironman.180.medium-ardougne-diary"},
            {"id": "varlamore", "name": "Varlamore & Midgame Gear", "description": "Use modern Varlamore unlocks and Moons equipment as flexible progression branches.", "startStepId": "efficient-ironman.train.woodcutting-71"},
            {"id": "sailing-slayer", "name": "Sailing & Slayer Branches", "description": "Raise modern traversal and combat skills without committing to one mandatory gear prison.", "startStepId": "efficient-ironman.train.sailing-45"},
            {"id": "grandmaster", "name": "Grandmaster Foundations", "description": "Complete the quest lines that open raids, Prifddinas, Darkmeyer, and late PvM.", "startStepId": "efficient-ironman.217.sins-of-the-father"},
            {"id": "quest-cape", "name": "Quest Cape & Late Unlocks", "description": "Finish the remaining grandmaster and current-release quest requirements.", "startStepId": "efficient-ironman.225.secrets-of-the-north"},
        ],
        "migrations": [],
        "sections": sections,
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(route, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote {OUTPUT} with {len(sequence)} steps ({route_row_index} route rows)")


if __name__ == "__main__":
    generate()
