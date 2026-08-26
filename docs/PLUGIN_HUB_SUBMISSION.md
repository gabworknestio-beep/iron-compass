# Publishing IronPath to the RuneLite Plugin Hub

This is the maintainer handoff for the first public release. RuneLite must review and merge the Plugin Hub manifest before IronPath appears in the client.

## 1. Finish the repository release

1. Confirm that the public author name `Gab` in `runelite-plugin.properties` and `LICENSE` is still the identity you want reviewers and users to see.
2. Create a public GitHub repository containing this project. Do not commit `.gradle/`, `.gradle-user-home/`, `build/`, IDE files, logs, or packaged JARs.
3. Keep the default branch stable and make sure the GitHub Actions build passes on Java 11.
4. Complete the live-game items in [PLUGIN_HUB_CHECKLIST.md](PLUGIN_HUB_CHECKLIST.md).
5. From a clean checkout, run `./gradlew clean test build` (or `gradlew.bat` on Windows).
6. Commit the exact reviewed source. Optionally tag the first accepted candidate `v1.0.0`.
7. Copy the full 40-character commit SHA with `git rev-parse HEAD`.

`runelite-plugin.properties` intentionally omits `version`: RuneLite documents it as optional, while the Plugin Hub manifest pins the exact source commit. Future releases update the manifest commit after review.

## 2. Add the Plugin Hub manifest

Fork `runelite/plugin-hub`, create a branch, and add one extensionless file named `plugins/ironpath` with exactly:

```properties
repository=https://github.com/GITHUB_OWNER/GITHUB_REPOSITORY.git
commit=FULL_40_CHARACTER_COMMIT_SHA
authors=PUBLIC_AUTHOR_NAME
```

Use the final public values, no spaces around `=`, and a lowercase filename containing only letters, numbers, and dashes. The manifest author should match the public project identity.

## 3. Open the review pull request

Open a pull request against `runelite/plugin-hub` titled `Add IronPath`. Link the repository README, summarize that the plugin is local-only and decision-support only, and mention the standard build, Java 11, absence of runtime dependencies, and optional integrations.

RuneLite performs automated and human review for security, policy, and build compliance. Do not announce Plugin Hub availability until the manifest is merged and IronPath is visible in a normal RuneLite client.

## 4. Publish later updates

For each update:

1. add a changelog entry and test the exact commit;
2. push that commit to the public repository;
3. change only the `commit=` value in `plugins/ironpath` unless repository or authorship changed;
4. open a Plugin Hub update pull request and respond to review feedback.

Official references: [Plugin Hub repository](https://github.com/runelite/plugin-hub), [Plugin Hub review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review), and [rejected or rolled-back features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features).
