# Goal Planner V1

Audited: **2026-08-27**.

The Goal Planner turns a chosen Ironman objective into one deterministic next action. It does not replace the canonical Efficient Ironman route or the Gear graph: it references both through stable IDs and explains the shortest currently provable requirement boundary.

## Player contract

The Overview shows:

- **Your goal** — the selected long-term outcome;
- **Next step** — one concrete route, Gear, quest, skill, confirmation, or completion action;
- **Why now?** — the requirement that makes this action the nearest useful boundary;
- **Progress** — concise true, false, and unknown requirement evidence;
- **After this** — the next known boundary without pretending to estimate completion time;
- **What this unlocks** — authored outcomes;
- **Resources** — readiness only when an existing Gear objective has reviewed supply data.

Unknown bank evidence remains `UNKNOWN`. It produces a confirmation action, never an ownership, availability, or recommendation claim. Completion takes precedence over inconsistent lower-level inputs so a completed quest does not display contradictory missing requirements.

## Bundled catalog

`src/main/resources/goals/ironman-goals-2026.json` is versioned and contains 11 curated goals: Dragon Defender, Barrows Gloves, Fire Cape, Fighter Torso, Zombie Axe, Perilous Moons, Royal Titans, Song of the Elves, Bowfa and Crystal Armour, Trident of the Seas, and Tombs of Amascut entry.

Each definition may contain:

- stable `id`, `title`, `description`, and `category`;
- `completion` and direct `requirements` conditions;
- `dependencyIds` for other Goal definitions;
- one `routeAnchorId` or `gearId` where existing engines already own the progression facts;
- `impact`, `effort`, `unlocks`, `wikiPage`, and `tags`.

Do not duplicate a Gear objective's completion/readiness conditions in Goal JSON. Set `gearId` and reuse the Gear evaluation. This prevents the Overview and Gear views from disagreeing.

## Deterministic planning

Goal dependencies are walked before direct requirements. The planner chooses the closest missing skill requirement by numeric gap and uses stable catalog order as the final tie-break. Route anchors resolve through the canonical route projection; Gear anchors resolve through the existing recursive `GoalDependencyResolver`.

Saved pre-update Gear goal IDs continue to work. Curated Gear goals deliberately reuse their existing Gear IDs, and a legacy selected Gear ID outside the 11-goal catalog is adapted rather than deleted. A saved ID absent from both catalogs fails safely and can be cleared in the UI.

## Recommendations and preferences

`ProgressionRecommendationService` produces three deduplicated roles:

- **Recommended** — best current route or reachable Gear action;
- **Quick Win** — a distinct short action that fits the selected session filter;
- **Long-Term** — the selected Goal's next boundary.

Balanced, Efficient, PvM, and Skilling playstyles change ranking only. **Avoid Wilderness** penalizes Wilderness candidates but does not rewrite factual access or completion. Session length filters Quick Wins only and is not an ETA promise.

All preferences use RuneLite's per-character RS-profile configuration. Logout and both profile-change events clear the in-memory cache before the next profile is read.

## Unlock Radar

The Unlock Radar compares consecutive projections in the same character session. It emits at most one opportunity when a route/Gear objective becomes available or a selected Goal requirement changes to true. Stable IDs deduplicate repeated ticks. Its baseline resets on logout, shutdown, and profile change, preventing old-character notifications.

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
