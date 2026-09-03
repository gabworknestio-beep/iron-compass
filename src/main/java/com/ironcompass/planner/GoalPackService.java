package com.ironcompass.planner;

import com.ironcompass.gear.GearProjection;
import com.ironcompass.goal.GoalCatalog;
import com.ironcompass.goal.GoalCompletionEvaluation;
import com.ironcompass.goal.GoalCompletionService;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalRequirementResolver;
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.requirement.RequirementResult;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Curated views over existing goals and dependencies; this does not define separate requirements. */
public final class GoalPackService
{
    private static final int MAX_PACKS = 4;
    private static final List<PackDefinition> PACKS = Arrays.asList(
        pack("pack.early-essentials", "Early Ironman Essentials",
            "Core travel, prayers, food, GP, and starter gear.",
            "goal.skill.prayer-43", "goal.quest.druidic-ritual", "goal.transport.fairy-rings",
            "goal.resource.food-karambwans", "gear.early.ava", "gear.early.melee-weapon"),
        pack("pack.transport", "Transportation Unlocks",
            "Travel systems that make later questing and clues smoother.",
            "goal.transport.fairy-rings", "goal.transport.ardougne-cloak",
            "goal.transport.poh-portals", "goal.transport.xerics-talisman",
            "goal.account.strong-poh"),
        pack("pack.slayer-foundation", "Slayer Foundation",
            "Slayer gear and unlocks that start paying off across combat styles.",
            "gear.mid.slayer-helm", "goal.skill.slayer-55", "goal.skill.slayer-65",
            "goal.qol.slayer-rings", "goal.skill.slayer-75"),
        pack("pack.barrows-ready", "Barrows Ready",
            "The practical prayer, magic, and access baseline for repeatable Barrows.",
            "goal.pvm.barrows-prep", "goal.skill.prayer-43", "gear.early.magic-weapon",
            "goal.pvm.barrows"),
        pack("pack.moons-ready", "Moons Ready",
            "Quest, defensive, and melee foundations for Perilous Moons.",
            "goal.quest.perilous-moons", "goal.unlock.piety", "gear.early.defender",
            "gear.mid.barrows-tank", "goal.pvm.perilous-moons-loop"),
        pack("pack.questing-infrastructure", "Questing Infrastructure",
            "High-value quest and account systems that remove many future blockers.",
            "goal.quest.recipe-disaster", "gear.early.gloves", "goal.account.kingdom",
            "goal.quest.desert-treasure-i", "goal.quest.lunar-diplomacy",
            "goal.quest.song-of-the-elves"),
        pack("pack.midgame-pvm", "Midgame PvM",
            "Gear, supply, and quest milestones for broader bossing.",
            "gear.early.fire-cape", "goal.pvm.perilous-moons-loop", "goal.pvm.gauntlet",
            "gear.mid.bowfa", "goal.pvm.zulrah", "goal.quest.dragon-slayer-ii")
    );

    private final ConditionEvaluator conditions;
    private final GoalCompletionService completion;

    public GoalPackService(ConditionEvaluator conditions)
    {
        this.conditions = conditions;
        this.completion = new GoalCompletionService(conditions);
    }

    public List<GoalPackProjection> evaluate(GoalCatalog catalog, AccountState state, GearProjection gear,
                                             ManualOverrideStore overrides)
    {
        List<GoalPackProjection> result = new ArrayList<>();
        for (PackDefinition definition : PACKS)
        {
            GoalPackProjection pack = evaluate(definition, catalog, state, gear, overrides);
            if (pack != null && pack.getCompleteCount() < pack.getTotalCount()) result.add(pack);
        }
        result.sort(Comparator.comparingInt(GoalPackService::statusRank)
            .thenComparing(Comparator.comparingInt(GoalPackProjection::getCompleteCount).reversed())
            .thenComparing(GoalPackProjection::getId));
        return result.size() <= MAX_PACKS ? result : new ArrayList<>(result.subList(0, MAX_PACKS));
    }

    private GoalPackProjection evaluate(PackDefinition definition, GoalCatalog catalog, AccountState state,
                                        GearProjection gear, ManualOverrideStore overrides)
    {
        List<GoalPathNode> nodes = new ArrayList<>();
        List<GoalBlocker> blockers = new ArrayList<>();
        int complete = 0;
        int missing = 0;
        int unknown = 0;
        for (String goalId : definition.goalIds)
        {
            GoalDefinition goal = catalog.find(goalId);
            if (goal == null) continue;
            GoalCompletionEvaluation evaluation = completion.evaluate(goal, state, gear, overrides);
            nodes.add(new GoalPathNode(goal, evaluation.getStatus()));
            if (evaluation.getCompletion() == TruthValue.TRUE) complete++;
            else
            {
                if (evaluation.getReadiness() == TruthValue.UNKNOWN) unknown++;
                else missing++;
                addBlockers(blockers, catalog, goal, state, gear, overrides);
            }
        }
        if (nodes.isEmpty()) return null;
        GoalPackProjection.Status status = unknown > missing && complete == 0 ? GoalPackProjection.Status.UNKNOWN
            : missing + unknown == 0 ? GoalPackProjection.Status.READY
            : missing + unknown <= 2 ? GoalPackProjection.Status.CLOSE : GoalPackProjection.Status.BUILDING;
        String summary = complete + "/" + nodes.size() + " complete"
            + (missing + unknown == 0 ? "; ready to use."
            : "; " + (missing + unknown) + " remaining.");
        return new GoalPackProjection(definition.id, definition.title, definition.summary + " " + summary,
            status, complete, nodes.size(), nodes, blockers);
    }

    private void addBlockers(List<GoalBlocker> target, GoalCatalog catalog, GoalDefinition goal, AccountState state,
                             GearProjection gear, ManualOverrideStore overrides)
    {
        for (String dependencyId : goal.getDependencyIds())
        {
            GoalDefinition dependency = catalog.find(dependencyId);
            if (dependency != null && completion.evaluate(dependency, state, gear, overrides).getCompletion()
                != TruthValue.TRUE)
            {
                target.add(new GoalBlocker(GoalBlocker.Kind.HARD_REQUIREMENT, dependency.getTitle(),
                    "Required before " + goal.getTitle() + "."));
                if (target.size() >= 3) return;
            }
        }
        for (RequirementResult result : conditions.explain(GoalRequirementResolver.effectiveRequirements(goal, gear),
            state))
        {
            if (result.getValue() == TruthValue.TRUE) continue;
            target.add(new GoalBlocker(result.getValue() == TruthValue.FALSE
                ? GoalBlocker.Kind.HARD_REQUIREMENT : GoalBlocker.Kind.UNKNOWN_OR_MANUAL,
                result.getLabel(), result.getDetail()));
            if (target.size() >= 3) return;
        }
    }

    private static int statusRank(GoalPackProjection pack)
    {
        switch (pack.getStatus())
        {
            case READY: return 0;
            case CLOSE: return 1;
            case BUILDING: return 2;
            default: return 3;
        }
    }

    private static PackDefinition pack(String id, String title, String summary, String... goalIds)
    {
        return new PackDefinition(id, title, summary, Arrays.asList(goalIds));
    }

    private static final class PackDefinition
    {
        private final String id;
        private final String title;
        private final String summary;
        private final List<String> goalIds;

        private PackDefinition(String id, String title, String summary, List<String> goalIds)
        {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.goalIds = goalIds;
        }
    }
}
