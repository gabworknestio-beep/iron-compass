package com.ironcompass.planner;

import com.ironcompass.goal.GoalIntent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AccountHealthProjection
{
    private final Map<GoalIntent, AccountNeedEvaluation> evaluations;

    public AccountHealthProjection(List<AccountNeedEvaluation> values)
    {
        Map<GoalIntent, AccountNeedEvaluation> indexed = new EnumMap<>(GoalIntent.class);
        for (AccountNeedEvaluation value : values) indexed.put(value.getIntent(), value);
        evaluations = Collections.unmodifiableMap(indexed);
    }

    public AccountNeedEvaluation get(GoalIntent intent) { return evaluations.get(intent); }
    public List<AccountNeedEvaluation> getEvaluations()
    {
        return Collections.unmodifiableList(new ArrayList<>(evaluations.values()));
    }
}
