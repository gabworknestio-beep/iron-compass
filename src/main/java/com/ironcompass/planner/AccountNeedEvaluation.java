package com.ironcompass.planner;

import com.ironcompass.goal.GoalIntent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AccountNeedEvaluation
{
    private final GoalIntent intent;
    private final AccountNeedLevel level;
    private final List<String> explanations;

    public AccountNeedEvaluation(GoalIntent intent, AccountNeedLevel level, List<String> explanations)
    {
        this.intent = intent;
        this.level = level;
        this.explanations = Collections.unmodifiableList(new ArrayList<>(explanations));
    }

    public GoalIntent getIntent() { return intent; }
    public AccountNeedLevel getLevel() { return level; }
    public List<String> getExplanations() { return explanations; }
    public String getPrimaryExplanation()
    {
        return explanations.isEmpty() ? "Not enough observable information is available." : explanations.get(0);
    }

    public int recommendationBonus()
    {
        return level == AccountNeedLevel.WEAK ? ScoringWeights.ACCOUNT_NEED_WEAK
            : level == AccountNeedLevel.DEVELOPING ? ScoringWeights.ACCOUNT_NEED_DEVELOPING : 0;
    }
}
