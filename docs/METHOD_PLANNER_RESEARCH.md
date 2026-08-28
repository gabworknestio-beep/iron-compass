# Method Planner research

Audited: **2026-08-28**.

This note records the factual boundaries used by the bundled Method Planner. The shipped plugin performs no web requests: the reviewed definitions are local JSON loaded once from the JAR.

## Primary references

- [Ironman Guide/Herblore](https://oldschool.runescape.wiki/w/Ironman_Guide/Herblore): quest-led early levels, Farming Contracts, herb runs, Slayer/PvM herb sources, and potion processing.
- [Mastering Mixology](https://oldschool.runescape.wiki/w/Mastering_Mixology): level 60 Herblore access, paste inputs, and the resource-efficiency trade-off.
- [Ironman Guide/Crafting](https://oldschool.runescape.wiki/w/Ironman_Guide/Crafting): gems/jewellery, giant seaweed, sandstone, Lunar Diplomacy, and Superglass Make.
- [Ironman Guide/Smithing](https://oldschool.runescape.wiki/w/Ironman_Guide/Smithing) and [Giants' Foundry](https://oldschool.runescape.wiki/w/Giants%27_Foundry): quest XP, Foundry access/material use, and the Blast Furnace alternative.
- [Ironman Guide/Prayer](https://oldschool.runescape.wiki/w/Ironman_Guide/Prayer) and [Pay-to-play Prayer training](https://oldschool.runescape.wiki/w/Pay-to-play_Prayer_training): blessed bone shards/libation bowl, gilded altar, and explicit Wilderness risk at the Chaos Altar.
- [Ironman Guide/Hunter](https://oldschool.runescape.wiki/w/Ironman_Guide/Hunter) and [Hunters' Rumours](https://oldschool.runescape.wiki/w/Hunters%27_Rumours): Bone Voyage bird houses, Hunter Guild rumours, useful supplies, and Wilderness alternatives.
- [Ironman Guide/Farming](https://oldschool.runescape.wiki/w/Ironman_Guide/Farming): contract-based seed sustain and the relationship between Farming and Herblore.
- [Player-owned house](https://oldschool.runescape.wiki/w/Player-owned_house): high-level transport/restoration utility used to justify the strong-POH goal without claiming one mandatory layout.
- [Slayer level-up table](https://oldschool.runescape.wiki/w/Slayer/Level_up_table): audited 87 and 93 Slayer milestone unlocks.

## Deliberate product limits

- Method speed, attention, and resource efficiency are qualitative.
- Resource thresholds mean “enough to begin a useful session,” not “enough XP to reach the target.”
- The planner does not estimate potion conversions, XP rates, completion time, drop chance, or dryness.
- Wilderness methods are removed when Avoid Wilderness is active and for detected Hardcore accounts.
- The old text `SkillTrainingAdvisor` remains for canonical route detail; the structured Method Planner owns active Goal skill gates. It can replace more legacy advice gradually after equivalent data is audited.
- RuneLite `ItemID` constants from the resolved 1.12.37 API were used to verify the small set of resource-family IDs represented in JSON.
