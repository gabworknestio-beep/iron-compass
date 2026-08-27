# Dynamic Gear Path

Audited: **2026-08-27**.

IronPath's Gear view is an account-aware objective engine. It answers **“what is my next useful gear upgrade, why is it timely, and what unlocks it?”** while allowing the player to choose a different path.

## Catalog

The bundled `gear/ironman-gear-2026.json` catalog contains 40 reviewed objectives. Every objective can carry:

- a stable ID, name, equipment slot, combat styles, tier, role, importance, difficulty, effort and usefulness;
- previous, alternative and prerequisite gear IDs;
- links to stable steps in the 341-step progression route;
- automatic completion and readiness conditions;
- unlock method, activity, region, Wiki page, tags, notes and supply estimates.

The catalog deliberately stops at 40 integrated decisions instead of becoming a giant item dump. It covers deterministic early foundations, Slayer, Varlamore/Moons/Royal Titans, Bowfa and Bowfa-skip branches, Zulrah, raids, Doom/Yama and selected endgame destinations.

## States

- **OWNED** — an accepted equivalent is carried, equipped, observed in the bank, or manually confirmed.
- **UNCONFIRMED** — carried state does not prove ownership and the current character's bank has not yet been observed; manual ownership remains available.
- **AVAILABLE** — direct requirements and gear prerequisites are met.
- **LOCKED** — a quest, skill, item or prerequisite is missing; the detail view says which.
- **RECOMMENDED** — the highest-scoring reachable objective after player choices.
- **OPTIONAL** — optional, niche, long-term, or manually deprioritised.
- **SKIPPED** — explicitly postponed by the player.

An unopened bank produces unconfirmed ownership, never a false “missing” or “available” claim. The bank card says **Bank not scanned yet** until it is observed during the current character session. Logout and both RuneLite profile-change events clear that observation immediately.

## Explainable scoring

The internal score is deterministic:

`importance + role + accessibility + usefulness + player preference`

`− missing distance − effort − difficulty − bank uncertainty`

Only reachable non-optional objectives normally compete for the automatic recommendation. This prevents Bowfa, Doom, raid armour, or megarares from outranking unfinished foundational gear. Selecting a goal or choosing one of its alternatives applies a preference boost. The sidebar never exposes the equation: it translates the result into **High priority now**, **Ready now**, **Build toward this**, **Optional detour**, or a concrete **Not now** reason.

## Goal and route communication

**Set as goal** turns a gear item into an account objective. The dependency resolver walks prerequisite gear recursively, finds a linked route target, then returns the first unfinished canonical step through that target before checking direct account requirements.

The Overview presents one dominant chain:

- **YOU ARE HERE** — current chapter and nearby chapter timeline;
- **DO THIS NOW** — one route or selected-goal action;
- **WHY THIS NOW**;
- **WHAT THIS UNLOCKS**;
- **WHAT COMES NEXT**;
- one secondary route preview and one gear opportunity/detour.

A selected Bowfa goal can therefore point to Song of the Elves; a selected Zombie axe can point to Defender of Varrock; a missing skill becomes a concise training action. The original route order remains available under **Path** and is not rewritten.

## Player controls

The Gear detail view keeps Set/Clear goal, Wiki, and Manage visible. Manage contains:

- Skip/Unskip
- Mark optional/Restore priority
- Mark owned/return to automatic detection
- Reset gear choices

Authored alternatives remain directly visible because choosing a branch is a primary progression decision.

The selected goal, skips, optional marks, chosen alternatives, style/status filters and manual ownership overrides use RuneLite's per-character RS-profile configuration.

## Detection and equivalent families

Completion checks inventory, equipment, and the locally observed bank. RuneLite's `ItemVariationMapping` canonicalizes charged, degraded, imbued, poisoned and ornament variants. Authored `ITEM_ANY` families then accept functional descendants. `ITEM_ANY_EXACT` deliberately bypasses canonicalization when collapsing variants would alter semantics. Slayer helmet (i) uses an audited exact-ID family so neither Black mask nor an unimbued Slayer helmet can satisfy it. Dragon defender is the prerequisite for Avernic defender; Avernic also satisfies the earlier Dragon-defender ownership family, but is never modelled as an alternative to its own base item.

Catalog validation rejects missing, self, duplicate and conflicting references; prerequisite and previous-item cycles; reverse alternatives; and previous-tier regressions.

Exact raw item IDs are retained alongside canonical IDs for supply forecasting. This lets IronPath count potion doses without weakening gear-family matching.

## Supplies

Supply specs are attached only where a reviewed threshold adds value. The UI labels variable comfort thresholds as **estimated**. It counts actual potion doses and recognized food in carried items plus the observed bank.

No threshold is invented for self-contained or highly variable content. Moons, for example, supplies its own food and potions, so the plugin explains that instead of requiring a fake bank inventory.

## UI

The sidebar keeps three destinations: **Overview**, **Path**, and **Gear**. Gear has explicitly labelled Style and Status filters and groups the journey by combat style and early/midgame/late tier. Rows open a compact detail drill-down; account refreshes preserve the currently open Path/Gear card.

Skipping a Gear goal also clears it if selected. Dependency resolution and supply forecasting independently reject skipped goals; unskipping does not silently reselect one.

IronPath remains planning-only. It does not calculate live DPS, automate actions, guide boss mechanics, read collection-log history, or manipulate RuneLite windows.
