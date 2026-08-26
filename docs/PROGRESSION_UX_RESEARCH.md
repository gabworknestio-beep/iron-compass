# IronPath progression UX research — 2026 audit

Audited: 2026-08-26

## Research question

How can IronPath give an Ironman one dependable next action without turning a flexible account journey into a rigid efficiency checklist?

IronPath treats the OSRS Wiki as the source for game facts and the canonical quest order. Community guides and discussions are context for player expectations, pain points, and useful presentation patterns. Their prose is not bundled or copied.

## Sources reviewed

### Authoritative and current game facts

- [OSRS Wiki — Optimal quest guide/Ironman](https://oldschool.runescape.wiki/w/Optimal_quest_guide/Ironman): current quest/training order, including Varlamore and Sailing. The guide itself recommends following the early order more closely than the later order.
- [OSRS Wiki — Mootrius Ironman Guide](https://oldschool.runescape.wiki/w/Guide:Mootrius_Ironman_Guide): 2026 chapter-like progression that balances fun and efficiency and explicitly encourages alternating long grinds.
- Individual OSRS Wiki quest pages were checked for the exact partial boundaries bundled for Demon Slayer, Enter the Abyss, Dwarf Cannon, Rag and Bone Man I/II, and Fairytale II.

### Guides and progression tools

- [BRUHsailer](https://github.com/umkyzn/BRUHsailer): dense, efficient linear route with filters, progress, and resumption controls.
- [ironman.guide](https://ironman.guide/): 576+ actions organized into seven sections and an optional Sailing track; explicitly describes itself as a checklist rather than a script.
- [Ladlor's Ironman Progression Chart](https://ladlorchart.com/): visual milestone graph with direct Wiki actions and visible alternate paths.
- [Wizard-Fish/Ironman-Progression](https://github.com/Wizard-Fish/Ironman-Progression): phases, current focus, collapsible completed sections, and bundled data.
- [RuneLite Guide Overlay](https://github.com/RunelitePlugin/guide-overlay): one-action rows, section/overall progress, jump-to-current, and automatic completed-section collapse.
- [Yazi's Ironman Gear Progression 2025](https://oldschool.runescape.wiki/w/Guide:Yazi%27s_Ironman_Gear_Progression_2025), [ironman.guide gear](https://ironman.guide/gear), and [Ladlor](https://ladlorchart.com/) were compared with IronPath's 40-objective catalog for broad style and tier coverage.
- [RuneLite Plugin Hub](https://runelite.net/plugin-hub/) was checked for adjacent progression plugins and the constraints of a normal 242 px sidebar.

No authoritative current project was found for the names “Solo Ironman Strategist” or “Gustav's Helper”; IronPath does not invent comparisons to unverified projects.

### 2026 community pain points

- [Midgame milestone guide discussion](https://www.reddit.com/r/ironscape/comments/1t8a9cm/): players ask for early/mid/late goals and useful unlocks because the number of possible tasks becomes overwhelming. A recurring recommendation is to choose one desired item, then let its skills and quests generate the next tasks.
- [Modern guide recommendations](https://www.reddit.com/r/ironscape/comments/1rwn9yh/): BRUHsailer is valued for efficiency but described as dense; many players prefer a flexible guide or PvM-oriented path.
- [Guide detail discussion](https://www.reddit.com/r/ironscape/comments/1uu9m9m/): an efficient route can assume too much prior game knowledge and leave a newer player unsure what the row actually means.
- [Partial-quest frustration](https://www.reddit.com/r/ironscape/comments/1rf5681/): chopping quests into unexplained fragments confuses players even when it saves travel.
- [Direction without an efficiency prison](https://www.reddit.com/r/ironscape/comments/1s1eemx/) and [next-step uncertainty](https://www.reddit.com/r/ironscape/comments/1tfwl5t/) reinforce the same requirement: give a confident recommendation while leaving room to branch.
- [Bowfa/red-prison reluctance](https://www.reddit.com/r/ironscape/comments/1t8z68a/) and [a long dry CG grind hurting enjoyment](https://www.reddit.com/r/ironscape/comments/1v9n4rk/) support presenting Bowfa as a high-value branch, not a universal gate.
- [2026 interactive gear chart](https://www.reddit.com/r/ironscape/comments/1u3d0d5/) shows demand for visual order, Wiki links, and account toggles while explicitly warning that the order is approximate.

## Patterns worth using

1. One dominant action. The first card must answer “What do I do now?” before showing broader planning.
2. Chapter and overall progress. A player needs both a nearby finish line and long-term position.
3. Explain split milestones. “Partial completion” is not an instruction; each stop needs a concrete boundary and Quest Helper handoff.
4. Resume at the first unresolved prerequisite. Selecting Barrows gloves should never jump a fresh account directly to the final RFD fight.
5. Show the benefit chain. Each action should say why now, what it unlocks, and what comes next.
6. Preserve agency. Alternatives and optional detours should be visible, especially for long RNG grinds.
7. Conservative state language. An unopened bank is “not scanned yet,” not “missing.” Manual confirmation describes exactly what the player must confirm.
8. Progressive disclosure. Scores and edge-case controls are internal or behind Manage; the normal view uses human priority labels.

## Patterns deliberately rejected

- A raw 341-row home checklist: technically complete, cognitively unusable in a RuneLite sidebar.
- Blind copying of a guide's prose or exact route: hard to maintain, inappropriate for attribution, and contrary to account adaptation.
- Full-screen map overlays, arrows, or bank-tab automation: useful in Guide Overlay, but outside IronPath's progression-planning scope.
- Treating Bowfa, torso, Barrows, or a long Slayer drop as a mandatory gate when a modern alternate branch exists.
- Displaying the recommendation equation as player-facing justification. It is an implementation tool, not a reason a person can act on.
- Claiming an item is absent before the player has opened their bank during the session.

## Product decisions applied to IronPath

- The canonical 341-step route keeps stable step IDs but is projected into 12 data-driven chapters.
- Overview uses the sequence: **YOU ARE HERE → DO THIS NOW → WHY → WHAT THIS UNLOCKS → WHAT COMES NEXT**.
- Path defaults to the current chapter and a vertical timeline; search remains available for the full route.
- Gear is a journey by combat style and slot/tier, with explicit Owned, Next, Available, Locked, Optional, and Skipped states.
- A selected gear goal walks its graph and canonical route to the first unresolved action.
- Dragon defender is a prerequisite of Avernic defender, never an alternative to it.
- Graph validation rejects missing/self/duplicate references, relation conflicts, cycles, reverse alternatives, and previous-tier regressions.
- Partial quests use concrete stop labels and Quest Helper metadata. Where no stable client signal is proven, IronPath asks for a specific manual confirmation instead of pretending to detect it.
- Bowfa remains a high-value ranged branch. Eclipse atlatl, Scorching bow, Blowpipe, crossbows, and other catalogued routes remain visible alternatives.
