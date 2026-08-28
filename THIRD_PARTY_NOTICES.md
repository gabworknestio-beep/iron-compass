# Third-party notices and source audit

Iron Compass source code is BSD 2-Clause. This file records what was reviewed, what was redistributed, and what was only used as a factual or architectural reference.

## Redistributed build tooling

The Gradle wrapper files were copied from the current RuneLite example-plugin repository on 2026-08-25:

- https://github.com/runelite/example-plugin
- https://gradle.org/

Gradle is licensed under Apache License 2.0. The wrapper downloads the pinned Gradle distribution during a developer build; it is development tooling, not Iron Compass runtime code. RuneLite's example plugin and RuneLite are BSD 2-Clause projects. Iron Compass follows their external-plugin build conventions.

## OSRS Wiki route facts

Source page: https://oldschool.runescape.wiki/w/Optimal_quest_guide/Ironman

Iron Compass's bundled route was generated from the current factual row ordering, quest titles, and skill targets, then validated against RuneLite. Iron Compass does not bundle Wiki page prose, HTML, images, or a runtime scrape. Its instructions, reasons, preparation notes, and nearby errands are original concise text. The route embeds source URL and audit-date metadata so future changes can be reviewed transparently.

RuneScape and Old School RuneScape are trademarks of Jagex Limited. This project is unofficial and is not endorsed by Jagex or the OSRS Wiki.

## Reviewed plugins and guides — no code/prose redistributed

- Optimal Quest Guide: https://github.com/cesoun/optimal-quest-guide — BSD 2-Clause. Reviewed for current feature/maintenance context.
- Guide Overlay: https://github.com/RunelitePlugin/guide-overlay — BSD 2-Clause. Reviewed for competitive features and optional-integration behaviour.
- Iron Hub: https://github.com/ellismosss/iron-hub — reviewed to keep Iron Compass's scope deliberately narrow.
- Quest Helper: https://github.com/Zoinkwiz/quest-helper — reviewed only for the current public integration boundary and open inbound-message proposal.
- Shortest Path: https://github.com/Skretzo/shortest-path — BSD 2-Clause. Its public `PluginMessage` contract is implemented independently; no implementation classes or code are copied.
- WikiSync: https://github.com/runelite/wiki-sync — reviewed for capability boundaries; no integration code is copied or invoked.
- BRUHsailer: https://umkyzn.github.io/BRUHsailer/ — reviewed as community context. No guide prose, guide data file, or code is bundled.

## Gear-roadmap factual references — original synthesis

- Yazi's Ironman Gear Progression 2025: https://oldschool.runescape.wiki/w/Guide:Yazi%27s_Ironman_Gear_Progression_2025
- OSRS Wiki Ironman guide: https://oldschool.runescape.wiki/w/Ironman_guide
- ironman.guide gear progression: https://ironman.guide/gear
- community checklist discussion: https://www.reddit.com/r/osrs/comments/1tgh158/ironman_gear_progression_checklist_not_a_strict/
- Ladlor's Interactive Gear Progression Chart: https://ladlorchart.com and https://github.com/Madssb/InteractiveGearProg
- Current Wiki combat/encounter guides: https://oldschool.runescape.wiki/w/Ironman_Guide/Ranged, https://oldschool.runescape.wiki/w/Ironman_Guide/Magic, https://oldschool.runescape.wiki/w/Ironman_Guide/Slayer, https://oldschool.runescape.wiki/w/Moons_of_Peril/Strategies, https://oldschool.runescape.wiki/w/Royal_Titans/Strategies, and https://oldschool.runescape.wiki/w/Doom_of_Mokhaiotl/Strategies
- Current Wiki skilling guides and activities: https://oldschool.runescape.wiki/w/Ironman_Guide, https://oldschool.runescape.wiki/w/Mastering_Mixology, https://oldschool.runescape.wiki/w/Guardians_of_the_Rift, https://oldschool.runescape.wiki/w/Mahogany_Homes, and https://oldschool.runescape.wiki/w/Gemstone_Crab/Strategies
- Official Jagex release/poll facts: https://oldschool.runescape.com/polls/2025/1702, https://oldschool.runescape.com/polls/2025/1705, https://oldschool.runescape.com/polls/2026/1717, and https://oldschool.runescape.com/polls/2026/1757
- Recent Ironman route cross-checks: https://www.reddit.com/r/ironscape/comments/1t6r3b5/ironman_range_gear_progression/, https://www.reddit.com/r/ironscape/comments/1ro30s9/bowfaskip_progress_atlatlonly_ironman/, and https://www.reddit.com/r/ironscape/comments/1vxuzey/if_you_were_to_make_an_ironman_today_what_would/

Ladlor's repository was inspected before use and is MIT licensed (Copyright 2025 Mads S. Balto). Iron Compass does **not** copy its chart sequence, JSON, UI, images, or source code. It uses the chart only as one efficiency reference and independently represents factual OSRS item relationships. Wiki, Jagex, ironman.guide, and Reddit prose is likewise not redistributed. Iron Compass's objective graph, scoring, roles, branches, conditions, notes, supply estimates, instructions, and explanations are original synthesis. Item names, item IDs, sources, prerequisites, and upgrade relationships are game facts validated against the current RuneLite API and current pages during the 2026-08-26 audit.

The older TheFX V2 route was not used as the current route authority.

## Icon

The pickaxe-and-path icon was generated specifically for Iron Compass with OpenAI ImageGen from an original prompt and then downscaled to 48×48. It does not reproduce a RuneLite, Jagex, community-guide, or third-party logo.
