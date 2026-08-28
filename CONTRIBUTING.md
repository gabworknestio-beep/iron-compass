# Contributing to Iron Compass

Thanks for helping improve Iron Compass. Keep changes focused, explain the player-facing outcome, and preserve the plugin's decision-first scope.

## Before opening a pull request

1. Use a Java 11 JDK.
2. Read [docs/ROUTE_SCHEMA.md](docs/ROUTE_SCHEMA.md) before changing route data and [docs/GEAR_ROADMAP.md](docs/GEAR_ROADMAP.md) before changing gear progression.
3. Keep instructions and reasons concise and original. Cite factual sources in the relevant research or roadmap document; do not copy guide prose.
4. Give route steps stable IDs. Wording changes must not rename IDs; true replacements require an explicit migration.
5. Use a specific `MANUAL_ONLY` label that states exactly what the player must verify.
6. Do not add automation, game input, reflection, native code, external processes, runtime code/data downloads, telemetry, or non-RuneLite runtime dependencies.
7. Run `gradlew.bat clean test build` on Windows or `./gradlew clean test build` elsewhere.

Pull requests should describe the change, why it improves an Ironman decision, the account states tested, and any Wiki/Jagex/community facts consulted. Include a default-width sidebar screenshot for visible UI changes when practical.

Bug reports should include the affected route or gear objective, the expected result, the observed result, account mode, whether the bank had been opened that session, and reproducible steps. Never include login credentials or other sensitive account information.
