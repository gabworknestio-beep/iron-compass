# Publishing Iron Compass to the RuneLite Plugin Hub

This is the maintainer handoff for the Iron Compass rename and later updates: RuneLite must review and merge each new commit-pinned manifest change before normal clients receive it.

## 1. Finish an Iron Compass update

1. Confirm that the public author name `Gaby100amis` in `runelite-plugin.properties` and `LICENSE` is still the identity you want reviewers and users to see.
2. Do not commit `.gradle/`, `.gradle-user-home/`, `build/`, IDE files, logs, or packaged JARs.
3. Keep the default branch stable and make sure the GitHub Actions build passes on Java 11.
4. Complete the live-game items in [PLUGIN_HUB_CHECKLIST.md](PLUGIN_HUB_CHECKLIST.md).
5. From a clean checkout, run `./gradlew clean test build` (or `gradlew.bat` on Windows).
6. Commit and push the exact reviewed source to `https://github.com/gabworknestio-beep/iron-compass`.
7. Copy the full 40-character commit SHA with `git rev-parse HEAD`.

`runelite-plugin.properties` intentionally omits `version`: RuneLite documents it as optional, while the Plugin Hub manifest pins the exact source commit. Future releases update the manifest commit after review.

## 2. Update the existing Plugin Hub manifest

Sync your `runelite/plugin-hub` fork, create a branch, and edit the existing extensionless file `plugins/ironpath`. Keep this historical manifest filename so RuneLite updates the existing installation instead of treating Iron Compass as a separate new plugin. The filename is not the display name or ConfigGroup. Update its contents to:

```properties
repository=https://github.com/gabworknestio-beep/iron-compass.git
commit=FULL_40_CHARACTER_COMMIT_SHA
authors=Gab
```

Keep `authors=Gab`: Plugin Hub authors are GitHub usernames authorized to submit updates, while `author=Gaby100amis` in `runelite-plugin.properties` is the public in-client attribution. The commit must be the full 40-character hash, not GitHub's abbreviated display.

## 3. Open the update review pull request

Open one pull request against `runelite/plugin-hub` describing the Iron Compass update. Link the changelog and summarize any new account state, UI, or integration behaviour reviewers should test. Do not reuse or reopen an already merged initial-addition pull request.

RuneLite performs automated and human review for security, policy, and build compliance. The installed version remains the prior pinned commit until the update manifest is merged.

## 4. Publish later updates

For each update:

1. add a changelog entry and test the exact commit;
2. push that commit to the public repository;
3. change only the `commit=` value in the existing `plugins/ironpath` manifest after the rename release;
4. open a Plugin Hub update pull request and respond to review feedback.

Official references: [Plugin Hub repository](https://github.com/runelite/plugin-hub), [Plugin Hub review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review), and [rejected or rolled-back features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features).
