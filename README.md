# IronPath

![IronPath icon](icon.png)

[![Build](https://github.com/gabworknestio-beep/ironpath/actions/workflows/build.yml/badge.svg)](https://github.com/gabworknestio-beep/ironpath/actions/workflows/build.yml)

**Your Ironman progression companion.** IronPath is a decision-first RuneLite sidebar plugin that answers one question: *what is the smartest useful thing for this account to do next?*

IronPath projects a canonical Efficient Ironman route and a dynamic gear catalog against the character's actual skills, quest log, carried equipment, locally observed bank, account type, selected goal, and manual choices. The overview follows one decision chain: **You are here → Do this now → Why this now → What this unlocks → What comes next**. Chaptered **Path** and grouped **Gear** views provide the detail without turning the sidebar into a giant checklist.

This repository is a release candidate, not an installed Plugin Hub listing. In-game validation and RuneLite review are still required before publication.

## What it does

- Automatically skips satisfied quest and skill milestones without moving the remaining route into an opaque order.
- Explains **why now**, not just what to click next.
- Uses composable, three-valued conditions (`TRUE`, `FALSE`, `UNKNOWN`) for completion and readiness.
- Never treats an unopened bank as an empty bank. Carried items are known immediately; bank-dependent answers remain unknown until the bank is observed in the current session.
- Stores manual complete, incomplete, and skip overrides in RuneLite's per-character profile configuration.
- Preserves manual progress through stable step IDs and explicit route migrations.
- Marks risk on authored steps and supports explicit HCIM alternatives when a route supplies one.
- Opens contextual OSRS Wiki pages and can hand authored non-quest destinations to Shortest Path.
- Recommends one explainable gear objective using access, usefulness, effort, difficulty, account state, and player preference rather than a fixed shopping list.
- Lets the player select a gear goal; IronPath recursively walks its gear dependencies and returns the first unfinished canonical route step on the way to the linked milestone.
- Tracks `OWNED`, `AVAILABLE`, `LOCKED`, `RECOMMENDED`, `OPTIONAL`, and `SKIPPED` gear states with search and style/state filters.
- Counts reviewed encounter supplies from real carried items and the last bank observation, including exact potion doses, while labelling variable thresholds as estimates.
- Adds concise account-aware training advice to route skill milestones, including safe Hardcore alternatives and real recognized bank-material counts.
- Operates with bundled data and local RuneLite state: no IronPath server, analytics, telemetry, account-name collection, or runtime route download.

## What it does not do

IronPath does not perform game actions, send input, start quests, duplicate detailed Quest Helper walkthroughs, predict combat, calculate DPS, or generate a route with an AI at runtime. Its Gear Path is a focused progression companion, not a loadout optimizer, collection-log dashboard, encounter helper, or giant PvM hub.

## Current route coverage

The bundled `Efficient Ironman` route was audited on **2026-08-26** against the live OSRS Wiki `Optimal quest guide/Ironman` data and the current RuneLite `Quest` enum. Route version 3 contains **341 ordered steps projected into 12 player-facing chapters**:

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

## Account types

- **Ironman / Group Ironman:** primary supported path.
- **Hardcore Ironman / Hardcore Group Ironman:** risk labels are shown; the engine can substitute a safe alternative only when one is explicitly authored. The route must never silently invent a safer strategy.
- **Ultimate Ironman:** route preview is supported, but the sidebar clearly warns that preparation and banking are not UIM-optimized.
- **Regular:** preview is available with a clear warning that recommendations are tuned for Ironman progression.

## Integrations

| Integration | Status | Behaviour |
|---|---|---|
| OSRS Wiki | **WORKING** | Opens the route step's contextual Wiki page with RuneLite's normal browser utility. No Wiki page is scraped at runtime. |
| Shortest Path | **WORKING** | When its plugin is active, non-quest steps with an authored location can post the public `shortestpath/path` `PluginMessage` containing a `WorldPoint`. IronPath draws no competing path overlay. |
| Quest Helper | **PARTIAL** | IronPath names the matching helper and can reuse verified quest-state boundaries for automatic partial-milestone completion. Rechecked 2026-08-26: Quest Helper's proposed inbound launch message is still an open pull request, so the player must currently open the named quest from Quest Helper's own sidebar. IronPath exposes no fake launch action and imports no Quest Helper internals. |
| WikiSync | **PARTIAL** | A documented optional boundary exists, but IronPath does not call WikiSync or its network service because local RuneLite state already provides the required facts. |

IronPath remains fully usable when every optional companion plugin is absent.

## How progress is detected

`AccountStateService` captures a small immutable snapshot on RuneLite's client thread after relevant events. It reads real skill levels, RuneLite quest states, inventory, worn equipment, selected route varbits/varps, account type, and world location. Bank contents are cached only after an actual bank-container event, timestamped, and cleared on logout. The Gear view therefore reports stored gear as unknown until the bank has been opened during the session and shows the age of the observation. Canonical item IDs support equivalent gear families; exact raw IDs remain available for dose-aware supply counts.

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

IronPath reads the local character state listed above solely to evaluate the route. It does not upload username, skills, quests, inventory, equipment, bank, location, or progress. It has no analytics and no backend. Clicking **Wiki** opens a normal browser page; using **Path** posts an in-client event to an installed Shortest Path plugin. No player action is automated.

## Development

Requirements: a Java 11 JDK and internet access for Gradle to resolve the current RuneLite release during development.

```text
./gradlew clean test
./gradlew clean build
./gradlew run
```

On Windows, use `gradlew.bat`. `runClient` is an alias for the developer-client `run` task. Only a user with a real RuneLite/OSRS session can complete the final in-game behaviour and layout check.

If Windows resolves `java` to an old JRE rather than a JDK, run `run-client.bat`. It locates a JDK 11+ and sets `JAVA_HOME` only for the developer-client process. To select one explicitly, set `IRONPATH_JAVA_HOME` to its installation directory before running the launcher.

The project intentionally uses only dependencies already supplied by RuneLite plus JUnit for tests. Main source compatibility is fixed at Java 11. The route is loaded with `getResourceAsStream`, so it works from the Plugin Hub JAR rather than assuming resources are unpacked files. The automated suite includes nine 242 px synthetic-profile renders, a 200-iteration full-projection performance guard, and publication checks for Plugin Hub metadata, icon limits, documentation, and descriptor consistency. GitHub Actions runs the complete Java 11 build for every push and pull request.

## Release process

The first Plugin Hub release is prepared as `1.0.0` in [CHANGELOG.md](CHANGELOG.md). Maintainers should complete [docs/PLUGIN_HUB_CHECKLIST.md](docs/PLUGIN_HUB_CHECKLIST.md), then follow the exact repository and manifest handoff in [docs/PLUGIN_HUB_SUBMISSION.md](docs/PLUGIN_HUB_SUBMISSION.md). A successful local build does not mean RuneLite has accepted the plugin; availability begins only after the manifest pull request is reviewed, merged, and visible in a normal client.

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
- [docs/PROGRESSION_UX_RESEARCH.md](docs/PROGRESSION_UX_RESEARCH.md) records the 2026 Wiki, guide, Plugin Hub, GitHub, and Reddit progression-UX comparison and the decisions applied here.
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) distinguishes source facts and inspiration from redistributed code.
- [docs/PLUGIN_HUB_CHECKLIST.md](docs/PLUGIN_HUB_CHECKLIST.md) records release-readiness checks and remaining human verification.
- [docs/PLUGIN_HUB_SUBMISSION.md](docs/PLUGIN_HUB_SUBMISSION.md) gives the exact first-release and future-update manifest workflow.

IronPath is licensed under the [BSD 2-Clause License](LICENSE).
