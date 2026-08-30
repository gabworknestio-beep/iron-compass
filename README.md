# Iron Compass

![Iron Compass icon](icon.png)

[![Build](https://github.com/gabworknestio-beep/iron-compass/actions/workflows/build.yml/badge.svg)](https://github.com/gabworknestio-beep/iron-compass/actions/workflows/build.yml)

**Your Ironman progression companion.** Iron Compass is a decision-first RuneLite sidebar plugin that answers one question: *what is the smartest useful thing for this account to do next?*

The unique `ironcompass` configuration group keeps Iron Compass fully separate from unrelated plugins. On first load, Iron Compass copies only its explicitly known former settings into the new namespace when the corresponding new value is absent; it never scans, deletes, or bulk-copies the former shared namespace.

Iron Compass projects a canonical Efficient Ironman route, a dynamic gear catalog, and a curated multi-goal planner against the character's actual skills, quest log, carried equipment, locally observed bank, account type, active goals, playstyle, session length, and manual choices. The overview follows one decision chain: **You are here → Next best move → Why this → How → What comes next**. Persistent **Overview**, **Path**, and **Gear** navigation provides detail without turning the sidebar into a giant checklist.

The plugin is available from RuneLite's Plugin Hub. The Iron Compass name becomes live after this source update is commit-pinned, reviewed, and merged in the Plugin Hub. This repository contains the installed source and later update candidates; every update still requires that review.

## What it does

- Automatically skips satisfied quest and skill milestones without moving the remaining route into an opaque order.
- Explains **why now**, not just what to click next.
- Uses composable, three-valued conditions (`TRUE`, `FALSE`, `UNKNOWN`) for completion and readiness.
- Never treats an unopened bank as an empty bank. Carried items are known immediately; bank-dependent answers remain unknown until the bank is observed in the current session.
- Clears cached character, bank, planner, and notification state immediately on logout or either RuneLite profile-change event, then reloads the new character's RS-profile preferences.
- Stores manual complete, incomplete, and skip overrides in RuneLite's per-character profile configuration.
- Preserves manual progress through stable step IDs and explicit route migrations.
- Marks risk on authored steps and supports explicit HCIM alternatives when a route supplies one.
- Opens contextual OSRS Wiki pages and can hand authored non-quest destinations to Shortest Path.
- Recommends one explainable gear objective using access, usefulness, effort, difficulty, account state, and player preference rather than a fixed shopping list.
- Lets the player select a gear goal; Iron Compass recursively walks its gear dependencies and returns the first unfinished canonical route step on the way to the linked milestone.
- Adds a Goal Queue with one Primary and up to three Secondary Goals, profile-specific migration from the former single selected goal, and duplicate/skip protection.
- Detects when one skill, quest, route step, or Gear action advances multiple active goals and shows deterministic reasons instead of an internal score.
- Produces distinct **Recommended**, **Quick Win**, **Long-Term**, and opt-in **Useful Break** suggestions from a broader candidate pool. Balanced, Efficient, PvM, and Skilling playstyles influence priority without changing factual requirements.
- Builds an account-aware Ironman Skill Plan from current level to target, with multi-band recommendations, relevant alternatives, locked methods, resource chains, milestones, broad XP-rate/time estimates, and full 1–99 pilot guides for Hunter, Crafting, Herblore, Construction, and Slayer.
- Searches bundled method titles, tags, resources, outputs, and styles. Unknown banks stay neutral; observed resources are described qualitatively and are never presented as an exact banked-XP calculation.
- Provides a compact searchable Goal Picker backed by the bundled researched catalog, with Suggested/Popular/Active/Completed views, broad categories, seven progression stages, and rich goal details.
- Summarizes Account Health from the same GoalIntent evaluator used by recommendations, with honest Weak/Developing/Good/Strong/Unknown explanations.
- Shows goal-based Quick Wins, nearby unlocks, typed Primary Goal blockers, intent-compatible alternatives, and a dependency-backed Path to My Goal view.
- Searches goal titles, descriptions, benefits, tags, related skills, quests, items, and activities; `prayer` and `teleport` therefore reveal whole solution families rather than title-only matches.
- Ranks explainable account-aware suggestions from skill proximity, account/route stage, observed gear and bank, account type, risk, active goals, usefulness, and resource problems without treating an unopened bank as empty.
- Tracks `OWNED`, `UNCONFIRMED`, `AVAILABLE`, `LOCKED`, `RECOMMENDED`, `OPTIONAL`, and `SKIPPED` gear states with search and style/state filters. Unknown bank ownership is never promoted as availability.
- Detects meaningful newly available route, gear, and selected-goal opportunities once per transition through a local Unlock Radar.
- Counts reviewed encounter supplies from real carried items and the last bank observation, including exact potion doses, while labelling variable thresholds as estimates.
- Adds concise account-aware training advice to route skill milestones, including safe Hardcore alternatives and real recognized bank-material counts.
- Operates with bundled data and local RuneLite state: no Iron Compass server, analytics, telemetry, account-name collection, or runtime route download.

## What it does not do

Iron Compass does not perform game actions, send input, start quests, duplicate detailed Quest Helper walkthroughs, predict combat, calculate DPS, or generate a route with an AI at runtime. Its Gear Path is a focused progression companion, not a loadout optimizer, collection-log dashboard, encounter helper, or giant PvM hub.

## Current route coverage

The bundled `Efficient Ironman` route was audited on **2026-08-26** against the live OSRS Wiki `Optimal quest guide/Ironman` data and the current RuneLite `Quest` enum. Goal and access facts received a focused follow-up audit on **2026-08-28**. Route version 3 contains **341 ordered steps projected into 12 player-facing chapters**:

- **230 current high-level Wiki route rows**, from `Learning the Ropes` through `The Blood Moon Rises`;
- **199 RuneLite-detectable quest steps** within those rows;
- **31 explicit manual milestones**: 15 diaries, 11 unlock/partial-quest milestones, 4 activities, and 1 aggregate achievement step;
- **111 inline skill-training milestones** extracted from the route's current training requirements;
- **121 preparation requirements**, including every skill target and curated early item preparation;
- **25 original “while you're here” errands** across the early and foundational route;
- authored navigation locations, with Shortest Path actions exposed only for non-quest activities.

The order is a deterministic baseline. Completed quests and over-levelled skill grinds disappear for established accounts, while unsatisfied steps keep their canonical relationship. Path defaults to the current chapter instead of exposing a raw 341-row checklist; full-route search remains available. The route has richer micro-routing early on and high-level milestone coverage later; it is not a verbatim copy of any community guide.

The separate dynamic Gear Path contains **40 reviewed objectives** across early, mid, late, and endgame progression:

- deterministic foundations, Slayer upgrades, Barrows, all three Moons sets, Royal Titans, Zulrah, raids, Doom/Yama, and selected endgame destinations;
- explicit RCB, sunlight crossbow, Eclipse atlatl, Bowfa, Blowpipe, and Scorching-bow branches, so Bowfa remains valuable without becoming a mandatory early recommendation;
- recursive prerequisite and route-step links that turn a chosen item into a concrete next account action;
- functional descendants and RuneLite item variations, so stronger equivalents satisfy earlier foundations;
- authored supply estimates only where a reviewed threshold adds value.

The engine does not claim one strict universal gear order. It synthesizes current Wiki facts, official Jagex releases, established gear guides, and recent community practice into explainable choices; player goals and alternatives remain first-class inputs. See [docs/GEAR_ROADMAP.md](docs/GEAR_ROADMAP.md).

## Goal Planner

The bundled Goal Planner loads its current objective count directly from the versioned catalog across seven flexible stages. It covers every skill including Sailing, resource sustainability, transport, modern Varlamore, Gear/PvM, quests, clues, all Achievement Diary regions, minigames, the full useful Slayer ladder, bosses, raids, and endgame preparation. Goal definitions keep stable IDs and use validated dependencies, typed relationships, Gear references, rich what/why/when metadata, account-mode constraints, risk, RNG status, and source references.

The queue deliberately stays small: one Primary Goal and no more than three Secondary Goals. Iron Compass identifies the nearest provable requirement for each, merges identical actions, and gives the Primary Goal enough weight that low-value synergy cannot displace a critical hard requirement. For example, **70 Herblore** can simultaneously advance Song of the Elves and a potion-readiness milestone. Existing single and Gear goals remain backward compatible.

The goal catalog is intentionally broader than the canonical route. It includes small transformative unlocks (for example 75 Hunter for moonlight moths), benefit-led intentions, optional clue/RNG branches, and endgame targets without turning stages into mandatory rules. See [docs/GOAL_CATALOG_RESEARCH.md](docs/GOAL_CATALOG_RESEARCH.md).

Iron Compass v1.1.0 keeps readiness honest: observed skills, quest access, gear requirements, and manual-only unlock facts are evaluated through one shared requirement policy before anything is labelled ready.

For supported skill gates, the bundled Skill Planner suggests a **good fit for this account**, never an unjustified universal “best method.” It considers verified unlocks, Wilderness/Hardcore constraints, observed starting resources, active-goal synergy, playstyle, session length, outputs, costs, and Ironman resource value. The same projection powers Primary Goals and route training details; **View Skill Plan** opens the compact target path, while **Full 1–99 Guide** exposes the researched bands and important milestones. Useful supply chains are shown when authored inputs are missing; an unopened bank always produces the explicit unconfirmed-bank message.

See [docs/GOAL_PLANNER.md](docs/GOAL_PLANNER.md) for the data contract, preference semantics, and extension rules.

## Account types

- **Ironman / Group Ironman:** primary supported path.
- **Hardcore Ironman / Hardcore Group Ironman:** risk labels are shown; the engine can substitute a safe alternative only when one is explicitly authored. The route must never silently invent a safer strategy.
- **Ultimate Ironman:** route preview is supported, but the sidebar clearly warns that preparation and banking are not UIM-optimized.
- **Regular:** preview is available with a clear warning that recommendations are tuned for Ironman progression.

## Integrations

| Integration | Status | Behaviour |
|---|---|---|
| OSRS Wiki | **WORKING** | Opens the route step's contextual Wiki page with RuneLite's normal browser utility. No Wiki page is scraped at runtime. |
| Shortest Path | **WORKING** | When its plugin is active, non-quest steps with an authored location can post the public `shortestpath/path` `PluginMessage` containing a `WorldPoint`. Iron Compass draws no competing path overlay. |
| Quest Helper | **PARTIAL** | Iron Compass names the matching helper and can reuse verified quest-state boundaries for automatic partial-milestone completion. Rechecked 2026-08-26: Quest Helper's proposed inbound launch message is still an open pull request, so the player must currently open the named quest from Quest Helper's own sidebar. Iron Compass exposes no fake launch action and imports no Quest Helper internals. |
| WikiSync | **PARTIAL** | A documented optional boundary exists, but Iron Compass does not call WikiSync or its network service because local RuneLite state already provides the required facts. |

Iron Compass remains fully usable when every optional companion plugin is absent.

## How progress is detected

`AccountStateService` captures a small immutable snapshot on RuneLite's client thread after relevant events. It reads real skill levels, RuneLite quest states, inventory, worn equipment, selected route varbits/varps, account type, and world location. Bank contents are cached only after an actual bank-container event, timestamped, and cleared on logout or profile switch. The Gear view therefore reports bank-dependent ownership as `UNCONFIRMED` until the bank has been opened during the current character session and shows the age of the observation. Canonical item IDs support equivalent gear families; exact raw IDs are used where variation collapse would change meaning, including imbued Slayer helmets and dose-aware supplies.

Gear detection proves current possession, not lifetime collection-log history. If an upgrade was consumed, dismantled, lost, or discarded and no accepted descendant is currently present, use **Gear → Details → Manage → Mark owned manually**. Goals, skips, optional marks, alternatives, filters, and manual ownership overrides use per-character RS-profile storage.

The domain evaluator is independent of Swing and RuneLite events. A route step can combine conditions such as skills, quests, items, equipment, known-bank quantities, variables, locations, account type, and explicit manual confirmation with `ALL`, `ANY`, and `NOT`. Expensive rebuilding is event-driven and coalesced to at most once every two game ticks.

Manual controls live under **Manage** on a step:

- **Mark complete** overrides automatic detection;
- **Mark incomplete** keeps a normally detected step open;
- **Skip / Unskip** excludes or restores a step without pretending it was completed;
- **Clear manual override** returns the step to automatic detection;
- **Reset all overrides** requires confirmation and affects only the current character profile.

Gear detail keeps **Set as goal**, **Wiki**, and **Manage** visible. Skip, optional priority, manual ownership, and reset actions live in the Manage overflow; authored alternatives remain directly visible. Account refreshes preserve the open Path/Gear card.

## Privacy and fair play

Iron Compass reads the local character state listed above solely to evaluate the route. It does not upload username, skills, quests, inventory, equipment, bank, location, or progress. It has no analytics and no backend. Clicking **Wiki** opens a normal browser page; using **Path** posts an in-client event to an installed Shortest Path plugin. No player action is automated.

## Development

Requirements: a Java 11 JDK and internet access for Gradle to resolve the current RuneLite release during development.

```text
./gradlew clean test
./gradlew clean build
./gradlew run
```

On Windows, use `gradlew.bat`. `runClient` is an alias for the developer-client `run` task. Only a user with a real RuneLite/OSRS session can complete the final in-game behaviour and layout check.

If Windows resolves `java` to an old JRE rather than a JDK, run `run-client.bat`. It locates a JDK 11+ and sets `JAVA_HOME` only for the developer-client process. To select one explicitly, set `IRONCOMPASS_JAVA_HOME` to its installation directory before running the launcher.

The project intentionally uses only dependencies already supplied by RuneLite plus JUnit for tests. Main source compatibility is fixed at Java 11. Route, Gear, Goal, and Method data load once from classpath resources, so they work from the Plugin Hub JAR rather than assuming resources are unpacked files. The automated suite includes 21 synthetic 242 px profile/view renders, staged full-projection and account-assembly performance guards, profile-isolation and lifecycle coverage, catalog validators, and publication checks for Plugin Hub metadata, icon limits, documentation, and descriptor consistency. GitHub Actions runs the complete Java 11 build for every push and pull request.

## Release process

The initial `1.0.0` release is already available through the Plugin Hub. For later versions, complete [docs/PLUGIN_HUB_CHECKLIST.md](docs/PLUGIN_HUB_CHECKLIST.md), then follow the commit-pinned update workflow in [docs/PLUGIN_HUB_SUBMISSION.md](docs/PLUGIN_HUB_SUBMISSION.md). A successful local build validates an update candidate only; RuneLite users receive it after the corresponding Plugin Hub manifest change is reviewed and merged.

## Route-data contributions

Read [docs/ROUTE_SCHEMA.md](docs/ROUTE_SCHEMA.md) before editing route data. In short:

1. give the step a stable, namespaced ID that will never change for wording-only edits;
2. select a typed step kind and an explicit completion condition;
3. add original, concise `instruction` and `reason` text;
4. use `MANUAL_ONLY` when RuneLite state cannot prove completion;
5. add preparation/location/risk metadata only when it is known and authored;
6. add a migration if an existing stable ID truly must be replaced;
7. run the complete test suite. Bundled route validation fails on malformed data, duplicate IDs, bad quest names, invalid references, impossible cycles, and invalid requirements.

`tools/generate_route.py` is a development-only audited generator. It fetches source facts when a maintainer deliberately runs it; the shipped plugin never invokes the script or downloads route data.

## Research, attribution, and release status

- [docs/RESEARCH.md](docs/RESEARCH.md) records the current API, policy, route, integration, and competitive audit.
- [docs/GOAL_PLANNER.md](docs/GOAL_PLANNER.md) defines the Goal Planner, recommendation, preference, resource, and Unlock Radar contracts.
- [docs/PROGRESSION_UX_RESEARCH.md](docs/PROGRESSION_UX_RESEARCH.md) records the 2026 Wiki, guide, Plugin Hub, GitHub, and Reddit progression-UX comparison and the decisions applied here.
- [docs/METHOD_PLANNER_RESEARCH.md](docs/METHOD_PLANNER_RESEARCH.md) records the audited skill-method and resource-planner boundaries used by the bundled JSON.
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) distinguishes source facts and inspiration from redistributed code.
- [docs/PLUGIN_HUB_CHECKLIST.md](docs/PLUGIN_HUB_CHECKLIST.md) records release-readiness checks and remaining human verification.
- [docs/PLUGIN_HUB_SUBMISSION.md](docs/PLUGIN_HUB_SUBMISSION.md) gives the exact future-update manifest workflow.

Iron Compass is licensed under the [BSD 2-Clause License](LICENSE).
