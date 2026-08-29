# Goal Planner V2

Audited: **2026-08-28**.

The Goal Planner turns a deliberately small queue of Ironman objectives into one deterministic next action. It does not replace the canonical Efficient Ironman route or the Gear graph: it references both through stable IDs, detects shared requirements, and explains the strongest currently useful boundary.

## Player contract

The Overview shows:

- **Active Goals** — one Primary Goal and up to three Secondary Goals;
- **Next best move** — one concrete route, Gear, quest, skill, confirmation, or completion action;
- **Why this?** — deterministic reasons such as Primary Goal relevance, goal synergy, impact, or session fit;
- **Good fit for this account** — a structured Ironman method when the action is a supported skill gate;
- **Progress** — concise true, false, and unknown requirement evidence;
- **After this** — the next known boundary without pretending to estimate completion time;
- **What this unlocks** — authored outcomes;
- **Resources** — `UNKNOWN`, observed empty, partial, or an authored useful starting threshold, never invented banked XP;
- **Take a Useful Break** — distinct optional actions that still advance an active goal, unlock, supply path, or canonical route.

Unknown bank evidence remains `UNKNOWN`. It produces a confirmation action, never an ownership, availability, or recommendation claim. Completion takes precedence over inconsistent lower-level inputs so a completed quest does not display contradictory missing requirements.

## Goal Queue and migration

The queue is stored in RuneLite's per-character RS-profile configuration as `primaryGoal` and ordered `secondaryGoals`. The old `selectedGearGoal` value is copied to `primaryGoal` exactly once when the new key is absent. Migration is idempotent, keeps the old key readable, and starts secondaries empty. Selecting the Primary Goal removes it from secondaries; duplicates are rejected; skipping a goal removes it from every active role; and profile switches clear the in-memory cache before another character is read.

## Bundled Goal catalog

`src/main/resources/goals/ironman-goals-2026.json` is versioned; its goal count is read from the catalog rather than duplicated in UI strings. It covers every skill (including Sailing), Gear/PvM, resources, transport, account infrastructure, quests, clues, diaries, minigames, Slayer, bosses, and raids. The searchable Goal Picker exposes Suggested, Popular, Active, Completed, broad category, and stage filters. Suggested goals are never selected automatically.

Each goal carries player-facing what/why/unlock metadata plus completion mode, editorial priority, community weight, GoalIntents, typed relationships, related skills, quests, items, activities, usefulness, risk, account types, RNG status, and source references. Search uses those fields. The detail panel displays both the authored reason and the current scoring explanation.

Suggested scoring is deterministic. It combines usefulness, impact, stage relevance blended from account skills and route progress, requirement proximity, active-goal preference, observed missing Gear, the shared GoalIntent/AccountNeed evaluation, account-mode constraints, and risk. A confirmed `TRUE` completion is excluded; `UNKNOWN` remains eligible and is explained. Missing requirements are `UNKNOWN`, never implicitly ready. See `docs/GOAL_CATALOG_RESEARCH.md` for research and evidence policy.

Each definition may contain:

- stable `id`, `title`, `description`, `whyItMatters`, `category`, and `stage`;
- `completion` and direct `requirements` conditions;
- `dependencyIds` for other Goal definitions;
- one `routeAnchorId` or `gearId` where existing engines already own the progression facts;
- `impact`, `effort`, `usefulness`, `unlocks`, `benefits`, related metadata, account types, risk, RNG status, source references, `wikiPage`, and `tags`.

Do not duplicate a Gear objective's completion/readiness conditions in Goal JSON. Set `gearId` and reuse the Gear evaluation. This prevents the Overview and Gear views from disagreeing.

## Deterministic planning

Goal dependencies are walked before direct requirements. The planner chooses the closest missing skill requirement by numeric gap and uses stable catalog order as the final tie-break. Route anchors resolve through the canonical route projection; Gear anchors resolve through the existing recursive `GoalDependencyResolver`.

Saved pre-update Gear goal IDs continue to work. Curated Gear goals deliberately reuse their existing Gear IDs, and a legacy selected Gear ID outside the curated catalog is adapted rather than deleted. A saved ID absent from both catalogs fails safely and can be cleared in the UI.

## Recommendation V2 and synergy

`ProgressionRecommendationService` generates candidates from the Primary and Secondary Goal actions, the current and next ten canonical route boundaries, and every reachable Gear objective. Identical skill/target, route-step, and Gear actions share a stable action key. One merged candidate can therefore explain that it advances two or more active goals.

Internal scores combine explicit Primary/Secondary relevance, hard requirements, impact, synergy, current effort, session fit, playstyle, risk, and canonical position. The numeric score never enters the UI. Primary hard requirements have a larger base weight than low-value secondary synergy, so a broadly useful action cannot casually displace the player's explicit critical boundary.

The service produces four deduplicated roles:

- **Recommended** — best immediate action from the broader pool;
- **Quick Win** — a distinct short action that fits the selected session filter;
- **Long-Term** — the Primary Goal context or another distinct long boundary;
- **Useful Break** — up to three different actions that still advance the account.

Balanced, Efficient, PvM, and Skilling playstyles change ranking only. **Avoid Wilderness** excludes unsafe Method suggestions and strongly penalizes Wilderness candidates without rewriting factual access or completion. Hardcore accounts never receive a Wilderness Method recommendation. Session effort remains qualitative and is not an ETA promise.

## Structured Method Planner

`src/main/resources/methods/ironman-methods-2026.json` contains a focused set of important methods for goal skill gates. Definitions include stable ID, skill/range, verified requirements, risk, attention, qualitative effort/speed/resource efficiency, starting resource groups, useful outputs, acquisition sources, tags, playstyles, account types, related goals, and Wiki title.

The Method Planner filters locked or account-incompatible methods, applies the Wilderness/Hardcore preference, evaluates observed carried/banked starting inputs, and returns one recommended good fit plus at most two alternatives. A sufficient state means only that an authored **starting threshold** is observed; it is explicitly not a target-level or banked-XP claim. When the bank is unknown, the exact unconfirmed-bank message takes precedence.

All preferences use RuneLite's per-character RS-profile configuration. Logout and both profile-change events clear the in-memory cache before the next profile is read.

## Unlock Radar

The Unlock Radar compares consecutive projections in the same character session. It emits at most one opportunity when a route/Gear objective becomes available or a selected Goal requirement changes to true. Stable IDs deduplicate repeated ticks. Its baseline resets on logout, shutdown, and profile change, preventing old-character notifications.

The Overview also exposes a non-notifying goal proximity view. **Quick Wins** rank high account value against known remaining effort; **Unlock Radar** sorts objective closeness; unknown or unmodelled requirements remain visibly unknown. **Account Health**, alternatives, blockers, and Suggested all consume the same AccountNeed evaluator. All of these views resolve effective requirements through the same goal-or-linked-Gear policy. **Path to My Goal** is a read-only dependency view and never creates a second route database.

Live-content corrections in v1.0.4 distinguish skill levels from actual world/activity access: Children of the Sun gates Vale Totems, Hunter Guild rumours, and moonlight moth access; the linen trawling net uses 61 Construction; and a Salvaging Station schematic requires manual confirmation when it cannot be observed locally.

## Validation and contributions

`GoalValidator` rejects malformed stable IDs, duplicate IDs, missing/dangling dependencies, dependency cycles, invalid Gear references, invalid route anchors, unsupported or malformed conditions, missing unlock text, and malformed Wiki titles. `GearValidator` performs the corresponding route-anchor and condition checks for Gear data.

When adding a goal:

1. verify volatile game facts against the current OSRS Wiki;
2. reuse a Gear ID or route anchor whenever that engine already models the boundary;
3. write the narrowest provable condition and preserve unknown state;
4. add original, concise explanation/unlock text;
5. keep the stable ID unchanged for wording-only edits;
6. update validation, planner, recommendation, and 242 px render tests;
7. increment the Goal catalog version and audit date.

The planner is local decision support. It does not automate input, predict combat, launch unavailable Quest Helper APIs, or estimate grind completion times.
