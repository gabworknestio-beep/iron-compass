package com.ironcompass.planner;

import com.ironcompass.goal.GoalDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GoalInsightsProjection
{
    private final AccountHealthProjection health;
    private final List<GoalProximityCandidate> quickWins;
    private final List<GoalProximityCandidate> nearbyUnlocks;
    private final List<GoalBlocker> blockers;
    private final List<GoalDefinition> alternatives;
    private final boolean explicitAlternatives;
    private final List<GoalPathNode> personalPath;

    public GoalInsightsProjection(AccountHealthProjection health, List<GoalProximityCandidate> quickWins,
                                  List<GoalProximityCandidate> nearbyUnlocks, List<GoalBlocker> blockers,
                                  List<GoalDefinition> alternatives, boolean explicitAlternatives,
                                  List<GoalPathNode> personalPath)
    {
        this.health = health;
        this.quickWins = immutable(quickWins);
        this.nearbyUnlocks = immutable(nearbyUnlocks);
        this.blockers = immutable(blockers);
        this.alternatives = immutable(alternatives);
        this.explicitAlternatives = explicitAlternatives;
        this.personalPath = immutable(personalPath);
    }

    public AccountHealthProjection getHealth() { return health; }
    public List<GoalProximityCandidate> getQuickWins() { return quickWins; }
    public List<GoalProximityCandidate> getNearbyUnlocks() { return nearbyUnlocks; }
    public List<GoalBlocker> getBlockers() { return blockers; }
    public List<GoalDefinition> getAlternatives() { return alternatives; }
    public boolean hasExplicitAlternatives() { return explicitAlternatives; }
    public String getAlternativeHeading() { return explicitAlternatives ? "ALTERNATIVE PATHS" : "WAYS TO IMPROVE"; }
    public List<GoalPathNode> getPersonalPath() { return personalPath; }

    private static <T> List<T> immutable(List<T> values)
    {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
