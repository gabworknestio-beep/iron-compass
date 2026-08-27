# Changelog

All notable IronPath changes are recorded here. The project follows semantic versioning once a release is published.

## [Unreleased]

- No unreleased changes yet.

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
- Local-only processing with no IronPath backend, telemetry, analytics, or runtime route download.

### Quality

- Route, gear graph, migration, integration-contract, lifecycle, performance, and 242 px Swing render coverage.
- Publication-readiness checks for Plugin Hub metadata, icon limits, documentation, and PluginDescriptor consistency.
