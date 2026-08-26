# IronPath research record

Audited: **2026-08-25**. These findings drove the V1 architecture. Dates matter: maintainers should repeat this audit before changing an integration contract or regenerating the route.

## RuneLite and Plugin Hub

Primary sources:

- RuneLite example plugin: https://github.com/runelite/example-plugin
- Example-plugin rules for agents: https://github.com/runelite/example-plugin/blob/master/AGENTS.md
- Plugin Hub submission/build guidance: https://github.com/runelite/plugin-hub/blob/master/README.md
- Current rejected/rolled-back features: https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features
- Current RuneLite quest API: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Quest.java

Decisions:

- Main code targets Java 11 (`options.release = 11`) and `latest.release`, matching the current external-plugin template.
- `runelite-plugin.properties` uses `build=standard`; there are no non-RuneLite runtime dependencies.
- Runtime code uses no reflection, JNI/JNA, `Unsafe`, external process execution, code downloading, dynamic loading, Java serialization, input automation, or player-action automation.
- All route JSON and images are classpath resources read through streams. The plugin does not assume Plugin Hub JAR resources are unpacked.
- The root `icon.png` is 48×48, within the current 48×72 Plugin Hub limit.
- RuneLite state is read on the client thread; Swing changes are handed to the EDT.
- Current `Quest#getState(Client)`, `Skill`, container events, `InventoryID`, `PluginMessage`, `ConfigManager` RS-profile configuration, `LinkBrowser`, and normal plugin-manager availability checks were verified against the resolved RuneLite client/API.

## Current route

Primary sources:

- OSRS Wiki Optimal quest guide/Ironman: https://oldschool.runescape.wiki/w/Optimal_quest_guide/Ironman
- OSRS Wiki parse API, invoked explicitly by the development generator: https://oldschool.runescape.wiki/api.php
- RuneLite `Quest` enum used for exact detectable names: https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Quest.java

The live Wiki data contained **230 route rows** and **111 inline training templates** at audit time. Two current rows have malformed/unclosed `data-rowid` quoting; the generator deliberately parses row IDs only to the line boundary so `Nature Spirit` and `Death Plateau` are not swallowed. The build validates every quest name against the current RuneLite enum. Recipe for Disaster display titles are explicitly mapped to RuneLite's subquest names, including the current final name `Recipe for Disaster - Culinaromancer`.

The route includes modern Sailing-era quest rows such as `Pandemonium`, `Prying Times`, `Current Affairs`, `Troubled Tortugans`, `The Red Reef`, `Fallen From Grace`, and `The Blood Moon Rises`. Source ordering is factual route data; IronPath's short instructions, reasons, preparations, and nearby errands are original wording.

Community context reviewed:

- BRUHsailer: https://umkyzn.github.io/BRUHsailer/
- B0aty/BRUHsailer support visible in Guide Overlay: https://github.com/RunelitePlugin/guide-overlay

Community guides informed the product vocabulary around batching, nearby errands, and early travel efficiency. No community prose, guide file, code, or step-by-step wording is bundled. Older TheFX V2 material was not treated as a current authoritative route.

## Competitive audit

### Optimal Quest Guide

Source: https://github.com/cesoun/optimal-quest-guide

It mirrors the Wiki optimal quest order, auto-updates quest completion, opens Wiki guides, and displays requirements. Its README says the maintainer no longer plays and has limited maintenance time. IronPath keeps the good account-sync idea but differs by presenting one decision with reason/readiness/preparation, supporting typed non-quest milestones, three-valued bank truth, manual overrides, migration, and a richer route state model.

### Guide Overlay 2.0

Source: https://github.com/RunelitePlugin/guide-overlay

It is a broad checklist/import system with B0aty HCIM and BRUHsailer guides, completion tracking, banking assistance, locations/markers, Shortest Path hand-off, and Quest Helper awareness. IronPath intentionally avoids guide importing, long checklists as the primary view, its own scene/path overlay, and broad UI customization. The differentiation is a deterministic account-adaptive projection with one next decision and a concise explanation.

### Iron Hub

Source: https://github.com/ellismosss/iron-hub

Iron Hub is an all-in-one progression suite covering many modules: quests, gear, farming, dailies, loadouts, diaries, rumours, and more. IronPath deliberately does not copy that scope. V1 owns only progression routing, state-based projection, preparation, and lightweight hand-offs.

No implementation code was copied from these competitors.

## Quest Helper

Primary sources:

- Quest Helper repository: https://github.com/Zoinkwiz/quest-helper
- Proposed inbound launch message: https://github.com/Zoinkwiz/quest-helper/pull/2756
- Related open API request: https://github.com/Zoinkwiz/quest-helper/issues/2768

At audit time PR #2756, `feat: allow other plugins to start any helper via PluginMessage`, was still **open** and awaiting review. Its proposed payload is therefore not a released public contract. IronPath does not post that message, does not import implementation classes, and does not claim launch support. `QuestHelperBridge` reports `PARTIAL`: authored route milestones may use verified quest-state boundaries from the same server variables Quest Helper consumes, while the player opens the named helper from Quest Helper's own sidebar. The UI provides no fake launch button.

Re-audit rule: implement launch only after the contract is merged, released, documented/stable, and remains Plugin Hub compliant. Until then users open Quest Helper normally and IronPath owns only high-level route choice.

## Shortest Path

Primary sources:

- Repository: https://github.com/Skretzo/shortest-path
- Changelog documenting cross-plugin messages: https://github.com/Skretzo/shortest-path/wiki/Changelog
- Current Quest Helper/Guide Overlay usage context: https://github.com/Zoinkwiz/quest-helper and https://github.com/RunelitePlugin/guide-overlay

The current public event contract accepts namespace `shortestpath`, message `path`, and data key `target` containing a RuneLite `WorldPoint`; `clear` is supported. `ShortestPathBridge` implements exactly that small contract. A unit test captures the posted event and verifies namespace, name, key, and value type.

IronPath offers the action only when Shortest Path is active and a **non-quest** step has a reviewed destination. Quest navigation remains Quest Helper's responsibility, and IronPath draws no competing tile path.

## Wiki and WikiSync

Primary sources:

- OSRS Wiki: https://oldschool.runescape.wiki/
- WikiSync plugin/service repository: https://github.com/runelite/wiki-sync

Wiki actions use RuneLite `LinkBrowser` to open the authored page. Page fragments are preserved; there is no runtime scrape or embedded browser.

WikiSync sends selected player facts to personalize Wiki experiences, but it adds no stable capability needed by V1's local route engine. `WikiSyncBridge` is intentionally a future/optional boundary only. IronPath does not require WikiSync, invoke its internals, or call its network API.

## Product implications

- Canonical ordering is deterministic; account adaptation removes satisfied work rather than inventing a random route.
- Unknown knowledge is a first-class state, especially for the bank.
- Detailed quest execution belongs to Quest Helper/Wiki; IronPath owns high-level route selection and explanation.
- The sidebar is primary; no overlay was added because it did not materially improve V1 enough to justify more game-screen surface.
- Bundled reviewed data and local state keep privacy, reviewability, and offline behaviour straightforward.

## 2026 Ironman progression and gear audit

Audited: **2026-08-26**. Game facts came primarily from current OSRS Wiki pages, with official Jagex polls/news used for release intent and recent community discussions used only to understand practical route choices.

Primary gear and encounter references:

- https://oldschool.runescape.wiki/w/Ironman_Guide/Ranged
- https://oldschool.runescape.wiki/w/Ironman_Guide/Magic
- https://oldschool.runescape.wiki/w/Ironman_Guide/Slayer
- https://oldschool.runescape.wiki/w/TzHaar_Fight_Cave/Strategies
- https://oldschool.runescape.wiki/w/Moons_of_Peril/Strategies
- https://oldschool.runescape.wiki/w/Royal_Titans/Strategies
- https://oldschool.runescape.wiki/w/Update:Royal_Titans
- https://oldschool.runescape.wiki/w/Doom_of_Mokhaiotl/Strategies
- https://oldschool.runescape.com/polls/2025/1702
- https://oldschool.runescape.com/polls/2025/1705

Current community cross-checks:

- https://www.reddit.com/r/ironscape/comments/1t6r3b5/ironman_range_gear_progression/
- https://www.reddit.com/r/ironscape/comments/1ro30s9/bowfaskip_progress_atlatlonly_ironman/
- https://www.reddit.com/r/ironscape/comments/1v833g0/the_year_is_2026/
- https://www.reddit.com/r/ironscape/comments/1vy2t8h/first_ironman_grind_done/
- https://www.reddit.com/r/ironscape/comments/1vxuzey/if_you_were_to_make_an_ironman_today_what_would/

Decision notes:

1. **Bowfa remains high value, not mandatory.** Its current 80 Ranged/70 Agility use requirements and Song of the Elves gate prevent early recommendations. Eclipse atlatl, Hunters' sunlight crossbow, Scorching bow, Blowpipe and crossbow routes are explicit alternatives. A player's chosen alternative receives a scoring boost.
2. **The modern midgame chain is real but branchable.** Barrows tank pieces can smooth Moons; Blood Moon/Eclipse/Blue Moon feed Royal Titans; Royal Titans offer Twinflame and the Deadeye/Mystic Vigour tier. Neither full Barrows nor every Moons set is a universal gate.
3. **Doom is late progression.** Current strategy guidance recommends The Final Dawn, 90+ Ranged, Rigour-level Prayer, 75 Agility and especially Scorching bow. IronPath therefore classifies Doom rewards as long-term and never recommends them to an unfinished midgame account.
4. **Supply thresholds are labelled estimates.** Fight Caves and high-end encounters vary sharply with experience and stats. IronPath counts exact potion doses from raw item IDs, but presents researched comfort thresholds as estimates. Moons receives no invented bank requirement because its dungeon supplies food and potions.
5. **Consumed unlocks are not faked.** Deadeye/Mystic Vigour and collection-log history do not have a stable public RuneLite state contract suitable for this plugin. They inform sequencing, but are not falsely marked owned from an absent scroll.

## 2026 skilling and resource audit

Primary references:

- Prayer and Varlamore shards: https://oldschool.runescape.wiki/w/Ironman_Guide/Prayer
- Farming contracts/herb runs: https://oldschool.runescape.wiki/w/Ironman_Guide/Farming
- Crafting glass route: https://oldschool.runescape.wiki/w/Ironman_Guide/Crafting
- Hunter rumours: https://oldschool.runescape.wiki/w/Ironman_Guide/Hunter
- Herblore/Mastering Mixology: https://oldschool.runescape.wiki/w/Mastering_Mixology
- Smithing: https://oldschool.runescape.wiki/w/Ironman_Guide/Smithing
- Guardians of the Rift: https://oldschool.runescape.wiki/w/Guardians_of_the_Rift
- Mahogany Homes: https://oldschool.runescape.wiki/w/Mahogany_Homes
- Gemstone Crab: https://oldschool.runescape.wiki/w/Gemstone_Crab/Strategies
- Ironman money: https://oldschool.runescape.wiki/w/Ironman_money_making_guide

Implemented conclusions:

- Training cards give one concise primary method and one alternative instead of a full guide.
- Gemstone Crab is the current low-attention early melee baseline after Children of the Sun; Scurrius remains the active learning alternative.
- Hunter Rumours are recommended at their 46/57/72/91 tiers because their mixed loot advances herbs, logs, nests, meat and Prayer.
- The libation bowl is the safe Varlamore Prayer route; the Chaos Altar is presented as an explicit Wilderness trade-off and not a Hardcore default.
- Mastering Mixology is described accurately as higher XP per herb but slower than ordinary potion production.
- Farming contracts and herb runs, giant seaweed/sand, Giants' Foundry versus Blast Furnace, Guardians of the Rift, and Mahogany Homes are chosen according to resource/cash constraints.
- Bank material lines report real recognized counts only. IronPath does not convert mixed bank contents into a speculative XP total.

## Sailing decision

Primary references:

- https://oldschool.runescape.wiki/w/Sailing
- https://oldschool.runescape.wiki/w/Ironman_Guide/Sailing
- https://oldschool.runescape.com/polls/2026/1757
- https://oldschool.runescape.com/polls/2026/1717

Sailing released on 19 November 2025 and is now real account state through RuneLite's current Skill API. IronPath retains the route's authored Sailing requirements and gives concise courier/port-task guidance. Deep Sea Trawling, ship combat, sea Slayer and higher boat upgrades are optional branches unless a selected goal explicitly needs them. Sailing is never added as an arbitrary global gear gate.

## Dynamic recommendation architecture

The bundled catalog now contains **40** reviewed objectives with stable IDs, slot, styles, tier, previous/alternative/prerequisite links, route-step links, completion and readiness conditions, source/method/region, role, importance, difficulty, effort, usefulness, tags, notes and optional supply specs.

The recommender is deterministic and explainable:

- value = importance + role + accessibility + usefulness + player preference;
- penalties = missing-distance + effort + difficulty + bank uncertainty;
- only reachable non-optional objectives normally compete for the recommendation;
- locked high-value objectives remain visible with their missing reason;
- selected goals and chosen alternatives receive explicit preference boosts;
- prerequisite resolution is recursive and can redirect **NEXT ACCOUNT STEP** to a real bundled route step.

Gear states are OWNED, AVAILABLE, LOCKED, RECOMMENDED, OPTIONAL, and SKIPPED. Item possession uses exact current inventory/equipment plus the bank only after a real bank event. Canonical RuneLite variation mapping handles functional item families, while raw IDs are preserved separately for dose-aware supply counts.

## Quest Helper re-audit

Rechecked on **2026-08-26**:

- https://github.com/Zoinkwiz/quest-helper/pull/2756
- https://github.com/Zoinkwiz/quest-helper/issues/2768

The proposed inbound questhelper/start PluginMessage contract is still open and awaiting review. IronPath therefore does not post it, import Quest Helper internals, or manipulate RuneLite windows to fake a deep link. Quest steps retain exact questHelperKey metadata and Quest Helper-aware boundaries; until the public contract is merged and released, the player opens that helper from Quest Helper's sidebar.

## Plugin Hub re-audit

Rechecked on **2026-08-26**:

- https://github.com/runelite/plugin-hub/blob/master/README.md
- https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features

The implementation remains Java 11, latest.release, build=standard, classpath-resource based, and dependency-free beyond RuneLite. It does not manipulate client windows, automate input, add high-end boss assistance, expose player data over HTTP, use reflection/JNI/processes, or download runtime code/data. The dynamic engine is local planning UI, not a gameplay-action or encounter-helper system.
