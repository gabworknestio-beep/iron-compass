# Ironman Skill Planner research

Audited: **2026-08-30**.

Iron Compass bundles an original, structured synthesis of current training facts. The shipped plugin performs no web requests. Method definitions, level bands, source links, scoring metadata, resource chains, and milestones are loaded once from local JSON in the Plugin Hub JAR.

## Phase-one coverage

The complete guide catalog now covers all 24 RuneLite skills, including Sailing. It includes passive routines, modern unlocks, competing methods, bank-dependent inputs, GP costs, supply chains, combat progression, quest locks, Hardcore/Wilderness risk boundaries, and account-useful alternatives.

The catalog currently contains 115 methods/support chains and 136 Ironman-relevant milestones. A loader validator rejects duplicate IDs, unknown skills, invalid levels, malformed references, invalid styles, and any level gap in a guide that claims full 1–99 coverage.

## Primary factual references

- [Ironman guide](https://oldschool.runescape.wiki/w/Ironman_guide): account-wide Ironman priorities and cross-skill resource loops.
- [Ironman Guide/Hunter](https://oldschool.runescape.wiki/w/Ironman_Guide/Hunter), [Hunters' Rumours](https://oldschool.runescape.wiki/w/Hunters%27_Rumours), and [Rumour strategies](https://oldschool.runescape.wiki/w/Hunters%27_Rumours/Strategies): the 46/57/72/91 rumour tiers, bird houses, chinchompas, antelopes, moonlight moths, rewards, and access requirements.
- [Ironman Guide/Crafting](https://oldschool.runescape.wiki/w/Ironman_Guide/Crafting): early quests and jewellery, pre-Lunar glass, giant seaweed, sandstone, and Superglass Make.
- [Golem Crafting](https://oldschool.runescape.wiki/w/Golem_crafting) and the official [Wyrmscraig poll/release facts](https://oldschool.runescape.com/polls/2026/1758): level-60 Crafting access after Fallen From Grace, Wyrmscraig prerequisites, sunstone, Hunter-fur inputs, and active/relaxed execution.
- [Ironman Guide/Herblore](https://oldschool.runescape.wiki/w/Ironman_Guide/Herblore) and [Mastering Mixology](https://oldschool.runescape.wiki/w/Mastering_Mixology): early reward XP, level-60 Mixology, useful potion processing, farming contracts, herb runs, Master Farmers, Slayer/PvM supplies, and secondary sourcing.
- [Ironman Guide/Construction](https://oldschool.runescape.wiki/w/Ironman_Guide/Construction), [Mahogany Homes](https://oldschool.runescape.wiki/w/Mahogany_Homes), and the [Construction level table](https://oldschool.runescape.wiki/w/Construction/Level_up_table): plank sourcing, contract tiers, furniture alternatives, costs, and high-value POH milestones.
- [Ironman Guide/Slayer](https://oldschool.runescape.wiki/w/Ironman_Guide/Slayer), [Slayer training](https://oldschool.runescape.wiki/w/Slayer_training), [Slayer masters](https://oldschool.runescape.wiki/w/Slayer_Master), and the [Slayer level table](https://oldschool.runescape.wiki/w/Slayer/Level_up_table): useful task selection, master access, bursting/barraging, resource tasks, and exact monster milestones.

The Wyrmscraig Golem Crafting rate band was conservatively cross-checked against the current community calculator at [OSRS Iron](https://osrsiron.com/crafting/golem-crafting). Community observations affect only the broad estimated rate and method preference; Wiki/Jagex pages remain authoritative for requirements and mechanics.

## Planner policy

- A plan is generated from current level, target level, account requirements, active goals, observed resources, playstyle, session length, and method metadata.
- Candidate scores reward useful outputs, low cost, resource efficiency, goal synergy, session fit, and preference fit. XP rate is one factor, not the answer.
- A false requirement keeps the method visible as a **locked option**; an unknown requirement may remain recommendable but is labelled unconfirmed.
- An unopened bank is neutral. `UNKNOWN` never becomes zero, empty, impossible, or unavailable.
- Resource thresholds mean “enough to begin a useful session,” never “enough XP to reach the target.”
- XP/hour values are broad ranges. Time estimates use level-boundary XP because RuneLite's current snapshot stores levels, not exact skill XP in this domain model.
- The same planner projection serves active Goal skill gates, route training details, target plans, full guides, search, milestones, and alternatives. The older hard-coded route advice is no longer used by the UI.

## Deliberate limitations

- All 24 skills claim complete 1–99 coverage through at least one researched account-aware method band, with richer alternative bands for high-impact Ironman skills.
- Combat level is not stored in `AccountState`; Slayer methods that require a combat threshold expose it as recommended setup instead of pretending it was verified.
- The planner does not calculate exact banked XP, exact material totals, drop times, or RNG completion dates.
- Golem Crafting rates are still execution- and fur-dependent; the UI presents a range and labels total time as rough.
