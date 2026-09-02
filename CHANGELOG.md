# Changelog

All notable Iron Compass changes are recorded here. The project follows semantic versioning once a release is published.

## [1.1.5] - 2026-09-02

### Added

- Expanded the Skill Planner data from five pilot guides to researched 1–99 coverage for all 24 skills, including Sailing.
- Added Bank-to-Goal projections for nine bankable skills using the current character's exact XP and locally observed bank: recognized XP, target coverage, estimated reachable level, remaining XP, and contributing conversions.
- Added honest `UNKNOWN`, `NOT BANKABLE`, `ESTIMATE`, `READY`, and `COMPLETE` states, including secondary-limited Herblore calculations and current-level recipe gating.

### Changed

- Updated Plugin Hub discovery metadata so searches for bank, banked progress, and XP can find Iron Compass.
- Preserved the existing UI system while adding one compact Bank-to-Goal card inside the Skill Planner.

## [1.1.0] - 2026-08-30

### Added

- Added the account-aware Ironman Skill Planner with multi-segment target plans and complete pilot guides for Hunter, Crafting, Herblore, Construction, and Slayer.
- Added 44 local methods/support chains, 46 important milestones, structured sources, resource chains, broad XP-rate/time estimates, method search, locked alternatives, and goal-aware deterministic scoring.
- Added a premium compact Skill Planner dialog plus shared integration for Primary Goal skill gates and canonical route training steps.

### Changed

- Replaced the route UI's separate hard-coded training advice with the same structured planner used by the Goal Planner.
- Preserved unknown-bank safety: unobserved resources remain neutral and never make a method impossible.
- Improved the Plugin Hub description, search tags, and README first screen so players can discover the goal, gear, route, and skill-planning features more easily.

## [1.0.5] - 2026-08-29

### Added

- Expanded the researched Ironman goal library from 290 to 558 goals across all 24 skills, quests, resources, transportation, diaries, clues, minigames, boss preparation, raids, Varlamore, and Sailing.
- Added 21 factual and community sources, 187 typed relationships, and 87 dependency links for richer Personal Goal paths.
- Added catalog coverage tests for every skill, major categories, modern flagship goals, effort/impact balance, normalized duplicate titles, and valid related quests.

### Changed

- Strengthened catalog validation for duplicate normalized titles, duplicate related metadata, and RuneLite quest references.
- Bumped the generated goal catalog schema version to 6 while preserving every existing goal ID.

## [1.0.4] - 2026-08-29

### Added

- Added explainable Account Health, goal-based Quick Wins, objective proximity Unlock Radar, typed Primary Goal blockers, intent-driven alternatives, and a dependency-backed Path to My Goal view.
- Added persistent goal-level Mark Complete, Mark Incomplete, and Clear Manual actions without colliding with route or Gear overrides.
- Added current Hunter Rumours, antelope, Vale Totems, Sailing, Wyrmscraig, Golem Crafting, Piety, and fairy-ring milestones to the curated catalog.

### Fixed

- Missing requirement metadata now produces `UNKNOWN`, never a false `READY` claim.
- Separated Piety and fairy-ring readiness from their actual unlock completion.
- Replaced goal-specific recommendation exceptions with one shared GoalIntent/AccountNeed evaluator that preserves an unobserved bank as unknown.

### Changed

- Expanded and recured the catalog to 290 goals, 28 cited sources, 17 intents, and typed hard, recommended, synergy, alternative, and leads-to relationships.
- Centralized major ranking weights and capped multi-goal synergy.
- Consolidated the public release version and user-facing UI as 1.0.4.

### Catalog expansion included in 1.0.4

#### Added

- Expanded the Goal Picker from its initial compact set into a broad researched catalog spanning seven stages, every skill including Sailing, quests, transport, gear, resources, clues, diaries, minigames, Slayer, bosses, raids, and account infrastructure.
- Added rich goal metadata for why a goal matters, benefits, related items/skills/quests/activities, usefulness, risk, account types, RNG status, and source references.
- Added a static source registry and a categorized research report distinguishing game facts from community recommendations.
- Added stage and Popular filters plus full-text search across descriptions, benefits, tags, skills, quests, items, and activities.
- Added compact WHAT / WHY / REQUIREMENTS / UNLOCKS / WHY SUGGESTED details to the RuneLite goal dialog.

#### Changed

- Reworked Suggested for you into deterministic account-aware scoring using current stage, route progress, skill proximity, observed gear, active goals, account type, risk, and observed Prayer supplies.
- Confirmed completed goals are excluded, while manual or unobserved completion remains UNKNOWN and eligible.
- Added explicit RNG wording and preserved every previously persisted goal ID and the existing multi-goal planner.
- Made the loaded catalog and its metadata lists immutable after deserialization.

### Goal Planner expansion included in 1.0.4

#### Added

- A profile-specific Goal Queue with one Primary Goal and up to three deduplicated Secondary Goals.
- Goal Synergy detection that identifies one action advancing multiple active goals and explains the benefit without exposing an opaque score.
- A structured, bundled Ironman Method catalog for important Herblore, Crafting, Smithing, Prayer, Hunter, Construction, Farming, Agility, Mining, Woodcutting, and Slayer gates.
- Honest method-resource states for unknown, observed-empty, partial, and authored starting thresholds, plus useful acquisition sources without pretending to calculate exact banked XP.
- A compact searchable Goal Picker with category, Suggested, Active, and Completed filters.
- **Take a Useful Break**, offering distinct account-progress alternatives without making psychological claims about the player.

#### Changed

- Recommendation V2 now compares active-goal requirements, upcoming route milestones, and all reachable Gear candidates; explicit primary-goal requirements outrank generic old route steps.
- The Goal catalog grows from 11 to 26 curated objectives across infrastructure, quests, skill milestones, and PvM.
- Session fit now evaluates the immediate skill gap and method effort rather than treating every long-term goal as one indivisible grind.
- The 242 px Overview uses denser progressive disclosure and deterministic **Why this?** reasons.

#### Migration

- Existing `selectedGearGoal` profile data is copied idempotently to `primaryGoal`; `secondaryGoals` starts empty and old data remains readable.

## [1.2.0] - 2026-08-28

### Changed

- Adopted the Iron Compass identity throughout the visible metadata, Java package, branded classes, sidebar labels, launcher identifiers, tests, and documentation.
- Moved normal settings to the unique `ironcompass` ConfigGroup and per-character progress to `ironcompass-progress`.

### Migration

- Added a one-time, per-namespace migration that copies only Iron Compass's known settings when the new value is absent.
- The former shared namespace is never enumerated, modified, or deleted, and unknown keys belonging to unrelated plugins are never copied.

## [1.1.0] - 2026-08-27

### Added

- Goal Planner V1 with 11 curated account goals, validated dependencies, route anchors, Gear links, nearest-missing-requirement planning, resource readiness, and clear next/after/unlock explanations.
- Recommended, Quick Win, and Long-Term projections with Balanced, Efficient, PvM, and Skilling preferences, an Avoid Wilderness preference, and session-length filtering for Quick Wins.
- Unlock Radar notifications for meaningful newly available route, Gear, and selected-goal opportunities.
- Persistent, keyboard-focusable Overview / Path / Gear navigation and explicit Path/Gear search labels.

### Fixed

- Clear all character and observed-bank state on logout, `ProfileChanged`, and `RuneScapeProfileChanged` so profiles cannot leak into one another.
- Treat unknown bank-dependent Gear ownership as `UNCONFIRMED`, never `AVAILABLE` or `RECOMMENDED`.
- Clear a selected Gear goal when it is skipped and prevent skipped goals from reappearing in dependency or supply projections.
- Model Warriors' Guild access as Attack + Strength 130, or 99 in either skill.
- Detect Slayer helmet (i) only from exact imbued helmet IDs, excluding Black mask and unimbued Slayer helmet variants.

### Quality

- Added profile-isolation, persistence-corruption, lifecycle, exact-item, requirement-boundary, Goal Planner, recommendation, Unlock Radar, validator, performance, and 242 px UI coverage.
- Updated public authorship to `Gaby100amis`.

## [1.0.0] - 2026-08-26

### Added

- Account-aware Efficient Ironman route with 341 ordered steps and 12 focused chapters.
- One explainable next action based on skills, quests, carried gear, equipment, observed bank state, account mode, and manual choices.
- Forty-objective gear progression graph with melee, ranged, magic, and utility branches.
- Selected gear goals that resolve back to their first unfinished route or equipment dependency.
- Supply forecasts, training advice, risk labels, explicit HCIM alternatives, preparation lookahead, and nearby errands.
- Per-character manual completion, incomplete, skip, optional, goal, and ownership controls.
- Contextual OSRS Wiki and supported Shortest Path actions, plus honest Quest Helper handoff guidance.
- Local-only processing with no Iron Compass backend, telemetry, analytics, or runtime route download.

### Quality

- Route, gear graph, migration, integration-contract, lifecycle, performance, and 242 px Swing render coverage.
- Publication-readiness checks for Plugin Hub metadata, icon limits, documentation, and PluginDescriptor consistency.
