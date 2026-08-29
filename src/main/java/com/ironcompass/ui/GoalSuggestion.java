package com.ironcompass.ui;

import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalCompletionEvaluation;
import com.ironcompass.requirement.TruthValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GoalSuggestion
{
    private final GoalDefinition goal;
    private final int score;
    private final GoalCompletionEvaluation evaluation;
    private final List<String> reasons;

    GoalSuggestion(GoalDefinition goal, int score, GoalCompletionEvaluation evaluation, List<String> reasons)
    {
        this.goal = goal;
        this.score = score;
        this.evaluation = evaluation;
        this.reasons = Collections.unmodifiableList(new ArrayList<>(reasons));
    }

    public GoalDefinition getGoal() { return goal; }
    public int getScore() { return score; }
    public TruthValue getCompletion() { return evaluation.getCompletion(); }
    public GoalCompletionEvaluation getEvaluation() { return evaluation; }
    public List<String> getReasons() { return reasons; }

    public String getPrimaryReason()
    {
        return reasons.isEmpty() ? "A useful milestone for this part of progression." : reasons.get(0);
    }
}
