# Route schema and contribution guide

Bundled routes live under `src/main/resources/routes/`. The Java model and validator are the executable schema; every public JSON route is parsed and validated during `test`.

## Route document

```json
{
  "routeId": "efficient-ironman",
  "version": 3,
  "name": "Efficient Ironman",
  "description": "...",
  "auditedAt": "2026-08-26",
  "sources": [],
  "chapters": [
    {
      "id": "foundations",
      "name": "Account Foundations",
      "description": "...",
      "startStepId": "efficient-ironman.001.learning-the-ropes"
    }
  ],
  "migrations": [],
  "sections": []
}
```

- `routeId` is a stable machine ID.
- `version` is positive and increments for shipped semantic/data changes.
- `auditedAt` tells maintainers when volatile OSRS facts were last reviewed.
- `sources` record factual provenance and whether a source was authoritative or only community context.
- `chapters` project the canonical flat sequence into player-facing goals. IDs and start-step references are unique, starts follow canonical order, and the first chapter begins at the first route step.
- `migrations` map retired step IDs to their replacements.
- each section needs a unique `id`, display `name`, and ordered `steps`.

## Step

```json
{
  "id": "efficient-ironman.train.magic-37",
  "type": "TRAIN",
  "title": "Train Magic to 37",
  "category": "Skill milestone",
  "instruction": "Raise Magic to level 37 before continuing the quest route.",
  "reason": "Meets a near-term quest requirement without hiding the grind inside another step.",
  "completion": {
    "type": "SKILL_AT_LEAST",
    "skill": "Magic",
    "level": 37
  },
  "readiness": null,
  "requires": [],
  "preparation": [],
  "whileHere": [],
  "tags": ["training", "magic"],
  "optional": false,
  "risk": "SAFE",
  "importance": "NORMAL"
}
```

Supported step types are `QUEST`, `TRAIN`, `COLLECT`, `BUY`, `DIARY`, `UNLOCK`, `TRAVEL`, `ACTIVITY`, `EQUIP`, `PREPARE`, and `MANUAL`. Use the narrowest truthful type. A `MANUAL` step must directly declare `MANUAL_ONLY` completion; other non-detectable milestone types should do the same rather than guessing.

`requires` contains stable step IDs and forms a directed acyclic graph. It is for real prerequisites, not a restatement of the preceding row. Canonical list position preserves normal order.

`risk` is `SAFE`, `HCIM_CAUTION`, `WILDERNESS`, or `DANGEROUS`. Risk metadata warns; it does not imply an alternative exists. To replace a step for a hardcore account, set the canonical step's `hcimAlternativeStepId`, create a separately ordered step with `alternativeForStepId`, and ensure that alternative actually satisfies the intended route relationship. The engine never synthesizes one.

## Conditions

Conditions return `TRUE`, `FALSE`, or `UNKNOWN`.

Combinators:

- `ALL` with non-empty `children`;
- `ANY` with non-empty `children`;
- `NOT` with one `child`.

Leaf types:

- `SKILL_AT_LEAST`: `skill`, `level` (1–99);
- `SKILL_SUM_AT_LEAST`: non-empty `skills` and a positive aggregate `level`; used when access depends on a sum such as Attack + Strength 130;
- `QUEST_STATE`: exact current RuneLite `quest`, `state` (`NOT_STARTED`, `IN_PROGRESS`, `FINISHED`);
- `ITEM_PRESENT` / `ITEM_QUANTITY`: positive `itemId`, positive `quantity`, and `source` (`INVENTORY`, `EQUIPMENT`, `CARRIED`, `BANK`, or `ANY`);
- `ITEM_ANY`: non-empty positive `itemIds`, positive `quantity`, and a normal item `source`; succeeds when any listed equivalent is present;
- `ITEM_ANY_EXACT`: the same fields as `ITEM_ANY`, but compares raw RuneLite item IDs without `ItemVariationMapping`; use only when canonicalization would merge semantically different states;
- `EQUIPMENT_CONTAINS`: positive `itemId` and quantity;
- `BANK_KNOWN_ITEM_QUANTITY`: positive item ID and quantity; unknown until bank observation;
- `VARBIT_EQUALS`, `VARBIT_AT_LEAST`, `VARP_EQUALS`, `VARP_AT_LEAST`: integer `id` and `value`;
- `LOCATION_REACHED`: `x`, `y`, `plane`, and non-negative `radius`;
- `ACCOUNT_TYPE`: `accountTypes` matching the domain enum;
- `MANUAL_ONLY`: explicit player confirmation.

For `ANY` item source, carried quantity can prove presence immediately. If carried quantity is insufficient and the bank has not been observed, the answer is `UNKNOWN`, never “missing.” Once observed, carried and bank quantities are summed. RuneLite item-variation mapping normalizes charged, degraded, and ornament variants. Use `ITEM_ANY` only for authored functional equivalents or real upgrade descendants; do not use it to hide materially different gear behind a vague family. Use `ITEM_ANY_EXACT` narrowly—for example, the audited Slayer helmet (i) family—when RuneLite's canonical mapping would otherwise make an unimbued item satisfy an imbued goal.

The Warriors' Guild gate is represented as `ANY(SKILL_SUM_AT_LEAST(Attack, Strength, 130), SKILL_AT_LEAST(Attack, 99), SKILL_AT_LEAST(Strength, 99))`. Keep alternative access rules explicit instead of approximating each skill as 65.

## Preparation and nearby work

```json
"preparation": [
  {
    "kind": "ITEM",
    "name": "Rope",
    "itemId": 954,
    "quantity": 1,
    "source": "ANY",
    "consumable": true
  }
],
"whileHere": [
  {
    "title": "Stock up on ropes",
    "detail": "Buy a few ropes during this Ardougne visit for near-term quest use."
  }
]
```

Preparation is aggregated over the configured number of meaningful pending steps. Consumables add; reusable requirements take the maximum. `whileHere` is authored route data, not a runtime location guess, and the UI deliberately caps the current display.

An optional `location` has `x`, `y`, `plane`, `radius`, and `label`. Only add reviewed destinations. V1 hands locations to Shortest Path only for non-quest steps.

## Adding a step safely

1. Choose a stable ID based on identity, not display position. Do not renumber old IDs when inserting a step.
2. Write original concise text. Do not copy community-guide prose.
3. Add the most conservative provable completion condition. If the client cannot prove it, use `MANUAL_ONLY`.
4. Add readiness only when it determines whether the user can act now.
5. Reference only existing stable IDs and keep the prerequisite graph acyclic.
6. Use exact RuneLite quest names and valid skill levels/item IDs.
7. Add source/audit notes when introducing volatile facts.
8. Increment the route version. If an old ID must be retired, add `{ "fromStepId": "old", "toStepId": "new" }` to `migrations`.
9. Add or update evaluator/validation tests and run `./gradlew clean test`.

The build must reject bad data instead of allowing the runtime panel to fail silently.
