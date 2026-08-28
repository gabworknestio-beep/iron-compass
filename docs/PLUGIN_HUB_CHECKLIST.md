# Plugin Hub release checklist

Status as of **2026-08-28**: the Iron Compass rename remains an update candidate until the renamed repository and manifest are pinned, reviewed, and merged in RuneLite's Plugin Hub. This checklist gates that rename and later updates.

## Complete in this repository

- [x] Java-only main implementation with Java 11 release compatibility.
- [x] Current RuneLite `latest.release` external-plugin layout and official Gradle wrapper.
- [x] `runelite-plugin.properties` uses the expedited `standard` build and declares one plugin class.
- [x] Root icon is a transparent 48×48 PNG; resource copy is bundled for the sidebar.
- [x] BSD 2-Clause project license and third-party audit notes.
- [x] No non-RuneLite runtime dependencies.
- [x] No reflection, JNI/JNA, `Unsafe`, native memory, process execution, dynamic code loading, Java serialization, or runtime executable/source download.
- [x] No automation, menu/input injection, player-action execution, combat prediction, or duplicate path overlay.
- [x] Bundled route, dynamic 40-objective Gear catalog, and 11-goal catalog load via classpath streams and perform no runtime scrape/download.
- [x] Per-character progress uses RuneLite RS-profile configuration; settings use normal plugin configuration.
- [x] Local-only player state with no analytics, telemetry, backend, or username collection.
- [x] Optional integrations degrade safely. Quest Helper support is not falsely claimed.
- [x] Automated domain, route/Gear/Goal validation, goal-resolution, dose-aware supply, training-advice, integration-contract, persistence/lifecycle, and Swing render tests.
- [x] Gear graph validation rejects self/missing/duplicate/conflicting references, cycles, reverse alternatives, and tier regressions.
- [x] Fifteen synthetic 242 px Overview/Path/Gear/Goal/search/bank/manual/detail captures pass visual smoke tests; full projection and account assembly have repeatable performance guards.
- [x] Logout and both RuneLite profile-change events clear character/session projections, observed bank data, and notification state before another profile can render.
- [x] Unknown bank ownership is `UNCONFIRMED`, skipped goals cannot drive dependencies/supplies, and exact imbued-item checks bypass variation collapse where required.
- [x] Explainable local gear scoring never automates actions or supplies high-end encounter mechanics.
- [x] README documents features, limitations, account types, privacy, development, integrations, route contributions, attribution, and release status.
- [x] Public changelog, contribution guide, security policy, and exact Plugin Hub submission handoff.
- [x] GitHub Actions Java 11 build and automated metadata/icon/documentation consistency checks.
- [x] Every manual route condition states the exact player-visible milestone; generic confirmation labels fail validation.

## Required before each update submission

- [ ] Run the developer client with a real logged-in Ironman and capture reviewer-facing screenshots at default sidebar width.
- [ ] Verify event behaviour in game: login/logout, quest completion, skill level-up, equipment/inventory changes, opening/closing bank, profile switch, manual override/reset, and notification timing.
- [ ] Verify selected gear goals redirect Overview to the correct route/skill prerequisite, and confirm gear search, filters, alternatives, supply states, and bank-scan age at default sidebar width.
- [ ] Test alongside the current Plugin Hub releases of Shortest Path and Quest Helper. Confirm only non-quest authored locations receive a **Path** action and no duplicate guidance appears.
- [ ] Review every curated coordinate and item ID in live game; automated tests validate structure, not world accuracy.
- [ ] Re-run the route generator/audit immediately before release and review the resulting diff rather than accepting it blindly.
- [ ] Re-check the official Plugin Hub README, Plugin Hub review page, rejected-features page, Jagex third-party-client guidance, and Quest Helper PR #2756 at submission time.
- [ ] Run `gradlew.bat clean test` and `gradlew.bat clean build` on Java 11 from a clean checkout.
- [ ] Push the exact reviewed source to `https://github.com/gabworknestio-beep/iron-compass`, copy its full 40-character SHA, rename the existing manifest to `plugins/iron-compass`, and update its repository and commit fields. Keep `authors=Gab`.
- [ ] Respond to Plugin Hub CI/reviewer feedback and do not represent acceptance until the manifest is merged.

The exact manifest template and update workflow are in [PLUGIN_HUB_SUBMISSION.md](PLUGIN_HUB_SUBMISSION.md).

## Review notes

The development-only `tools/generate_route.py` performs explicit network reads when a maintainer runs it. It is not compiled, packaged as executable plugin logic, or called at runtime. Generated JSON is reviewed and bundled.

The Shortest Path bridge posts only a normal in-client `PluginMessage`. The Quest Helper bridge intentionally posts nothing while the proposed inbound API remains unmerged.

The official requirements were rechecked on 2026-08-27 against the [RuneLite example plugin](https://github.com/runelite/example-plugin), [Plugin Hub repository](https://github.com/runelite/plugin-hub), [Plugin Hub review process](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review), and [rejected or rolled-back features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features). Iron Compass's public source repository exists; only a tested, pushed update commit can supply the next manifest SHA.
